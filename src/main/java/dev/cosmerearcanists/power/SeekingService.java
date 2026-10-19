package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.investiture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SeekingService {
    public record Cloud(UUID owner,Vec3 position,double radius,double strength) { }
    private static final Set<UUID> SENT=new HashSet<>();
    static {InvestitureSignals.register(InvestitureSignals.ALLOMANCY,SeekingService::allomancy);}
    public static double strength(ServerPlayer p,String metal) {
        if(!SeekingConfig.ENABLED.get() || !(metal.equals("bronze")?SeekingConfig.BRONZE.get():metal.equals("copper") && SeekingConfig.COPPER.get()))return 0;
        return ordinaryStrength(p,metal);
    }
    private static double ordinaryStrength(ServerPlayer p,String metal) {
        if(!PowerConfig.ENABLED.get() || !ServerConfig.ALLOMANCY.enabled().get() || !p.isAlive() || p.isSpectator() || !ServerConfig.ALLOMANCY.metals().get(Metal.valueOf(metal.toUpperCase(Locale.ROOT))).get()
            || PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,metal)<=0)return 0;
        int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get())state=2;
        return PowerAttributes.strength(p,"a:"+metal)*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());
    }
    public static double activeStrength(ServerPlayer p,String power) {
        return switch(power) {
            case "a:iron","a:steel" -> PowerConfig.IRON_STEEL.get()?ordinaryStrength(p,power.substring(2)):0;
            case "a:tin" -> TinService.strength(p);
            case "a:pewter" -> PewterService.strength(p);
            case "a:electrum","a:atium" -> TemporalService.strength(p,power.substring(2));
            case "a:bronze","a:copper" -> strength(p,power.substring(2));
            default -> 0; // Placeholder states, passive possessions and reserve inventories are not kinetic effects.
        };
    }
    public static List<InvestitureSignals.Signal> signalsFor(ServerPlayer p) {
        var list=new ArrayList<InvestitureSignals.Signal>();
        for(String power:PowerAttributes.keys()) {
            double value=activeStrength(p,power);if(value<=0)continue;
            String metal=power.substring(2);int state=PowerData.state(p,power);if(state==3&&!PowerConfig.FLARING.get())state=2;
            int index=metal.equals("atium")?16:Metal.valueOf(metal.toUpperCase(Locale.ROOT)).ordinal();
            int period=(int)Math.clamp((20+index*2)*(state==1?1.25:state==3?.75:1),4,200);
            list.add(new InvestitureSignals.Signal(InvestitureSignals.ALLOMANCY,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal),p.getUUID(),p.position(),Math.min(4096,value),1,period,state,true));
        }return List.copyOf(list);
    }
    private static void allomancy(ServerLevel level,Consumer<InvestitureSignals.Signal> sink){for(var player:level.players())signalsFor(player).forEach(sink);}
    public static double cloudRadius(double strength){return strength<=0?0:Math.min(SeekingConfig.CLOUD_CAP.get(),SeekingConfig.CLOUD_RADIUS.get()*Math.sqrt(strength));}
    public static Cloud cloud(ServerPlayer p){double power=strength(p,"copper");return power>0 && SeekingConfig.MASKING.get()?new Cloud(p.getUUID(),p.position(),cloudRadius(power),power):null;}
    public static List<Cloud> clouds(ServerLevel level){return level.players().stream().map(SeekingService::cloud).filter(Objects::nonNull).toList();}
    /** Endpoint fields: each cloud counts once even when it contains both the Seeker and the emission. */
    public static double shielding(Vec3 listener,Vec3 source,List<Cloud> clouds) {
        double total=0;var seen=new HashSet<UUID>();
        for(var cloud:clouds)if(seen.add(cloud.owner) && (cloud.position.distanceToSqr(listener)<=cloud.radius*cloud.radius || cloud.position.distanceToSqr(source)<=cloud.radius*cloud.radius))total+=cloud.strength;
        return total;
    }
    public static SeekingPacket detect(ServerPlayer listener,List<InvestitureSignals.Signal> signals,List<Cloud> clouds) {
        double bronze=strength(listener,"bronze"),copper=strength(listener,"copper");var pulses=new ArrayList<SeekingPacket.Pulse>();
        if(bronze>0 && SeekingConfig.DETECTION.get()) {
            var ordered=new ArrayList<>(signals);ordered.sort(Comparator.comparingDouble(s->s.position().distanceToSqr(listener.position())));
            for(var signal:ordered) {
                if(signal.source().equals(listener.getUUID()) || !SeekingConfig.OTHER_ARTS.get() && !signal.art().equals(InvestitureSignals.ALLOMANCY))continue;
                if(!listener.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(signal.position())))continue;
                double range=SeekingMath.range(bronze,signal.intensity(),SeekingConfig.RANGE.get(),SeekingConfig.RANGE_CAP.get());double distance=listener.position().distanceTo(signal.position());if(distance>=range)continue;
                double shield=SeekingConfig.MASKING.get() && signal.copperMaskable()?shielding(listener.position(),signal.position(),clouds):0;
                double clarity=SeekingMath.clarity(distance,range,bronze,signal.difficulty(),SeekingMath.transmission(bronze,shield,SeekingConfig.PIERCING.get(),SeekingConfig.PIERCE_RATIO.get()));
                if(clarity<SeekingConfig.MIN_SIGNAL.get())continue;
                double skill=bronze/signal.difficulty();boolean identify=SeekingConfig.IDENTIFY.get() && skill>=SeekingConfig.ID_STRENGTH.get();
                // Unrecognized rhythms are stable but do not disclose the hidden art/effect through a packet field.
                boolean detailed=identify && skill>=SeekingConfig.DETAIL_STRENGTH.get();
                double grid=identify?.5:2;var p=signal.position();var position=new Vec3(Math.floor(p.x/grid)*grid+grid/2,Math.floor(p.y/grid)*grid+grid/2,Math.floor(p.z/grid)*grid+grid/2);
                pulses.add(new SeekingPacket.Pulse(position,identify?signal.art().toString():"",identify?signal.effect().toString():"",(float)clarity,signal.period(),detailed?signal.burnState():0));
                if(pulses.size()>=SeekingConfig.MAX_SIGNALS.get())break;
            }
        }
        boolean focused=bronze>0 && SeekingConfig.FOCUS.get() && PowerService.holdingAbility(listener,"a:bronze");
        return new SeekingPacket(listener.level().dimension().location().toString(),SeekingConfig.INTERVAL.get()+3,bronze>0,focused,copper>0 && SeekingConfig.MASKING.get()?cloudRadius(copper):0,List.copyOf(pulses));
    }
    public static void tick(ServerPlayer p) {
        for(String metal:List.of("bronze","copper")) {
            if(strength(p,metal)>0 && PowerConfig.CONSUMPTION.get()) {
                int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,"a:"+metal,2);}
                double rate=metal.equals("bronze")?SeekingConfig.BRONZE_DRAIN.get():SeekingConfig.COPPER_DRAIN.get();
                PowerData.reserve(p,metal,PowerData.reserve(p,metal)-rate/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
            }
            if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,metal)<=0 && PowerData.state(p,"a:"+metal)>0)PowerData.state(p,"a:"+metal,0);
        }
    }
    @SubscribeEvent public void scan(ServerTickEvent.Post event) {
        for(var level:event.getServer().getAllLevels()) {
            if(level.getGameTime()%SeekingConfig.INTERVAL.get()!=0)continue;
            boolean listeners=level.players().stream().anyMatch(p->strength(p,"bronze")>0);
            var signals=listeners && SeekingConfig.DETECTION.get()?InvestitureSignals.collect(level):List.<InvestitureSignals.Signal>of();var clouds=clouds(level);
            for(var player:level.players()) {
                boolean active=strength(player,"bronze")>0 || strength(player,"copper")>0;
                if(active || SENT.remove(player.getUUID()))PacketDistributor.sendToPlayer(player,detect(player,signals,clouds));
                if(active)SENT.add(player.getUUID());
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void emotions(EmotionalInfluenceEvent event) {
        if(event.target() instanceof ServerPlayer p && SeekingConfig.EMOTIONAL.get() && SeekingMath.emotionBlocked(strength(p,"copper"),event.strength(),SeekingConfig.EMOTION_PIERCING.get(),SeekingConfig.EMOTION_RATIO.get()))event.setCanceled(true);
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){SENT.remove(event.getEntity().getUUID());InvestitureSignals.removeSource(event.getEntity().getUUID());}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){SENT.remove(event.getEntity().getUUID());InvestitureSignals.removeSource(event.getEntity().getUUID());}
    @SubscribeEvent public void stopped(ServerStoppedEvent event){SENT.clear();InvestitureSignals.clearTransient();}
}
