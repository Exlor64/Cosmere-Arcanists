package dev.cosmerearcanists.mobs;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.living.*;
/** Ownership only: real personal attributes and provenance for theft, without inventing NPC fuel/AI. */
public final class NaturalMobPowers {
    public static final String DATA="cosmerearcanists_natural_powers";
    public static final TagKey<EntityType<?>> HUMANOIDS=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("cosmerearcanists:natural_power_humanoids"));
    public static boolean eligible(Mob mob){return mob.isAlive()&&mob.getType().is(HUMANOIDS)&&!mob.getType().is(EntityTypeTags.UNDEAD);}
    public static UUID identity(LivingEntity entity){var data=entity.getPersistentData().getCompound(DATA);return data.hasUUID("identity")?data.getUUID("identity"):entity.getUUID();}
    public static boolean checked(Mob mob){return mob.getPersistentData().getCompound(DATA).getBoolean("rolled");}
    public static void initialize(Mob mob){
        if(!eligible(mob)||checked(mob))return;
        var data=new CompoundTag();data.putBoolean("rolled",true);data.putUUID("identity",mob.getUUID());mob.getPersistentData().put(DATA,data);
        if(!NaturalMobConfig.ENABLED.get()||!PowerConfig.ENABLED.get())return;
        var dimensions=NaturalMobConfig.DIMENSIONS.get();if(!dimensions.isEmpty()&&!dimensions.contains(mob.level().dimension().location().toString()))return;
        // Respect pre-existing/admin-provided powers. Never overwrite or add another birth roll to them.
        for(String key:PowerAttributes.keys()){var attr=mob.getAttribute(PowerAttributes.holder(key));if(attr!=null&&attr.getBaseValue()>0)return;}
        long ticks=NaturalMobConfig.CHUNK_COOLDOWN_SECONDS.get()*20L;
        var server=mob.getServer();if(server==null)return;
        long now=server.overworld().getGameTime();
        var chunk=new ChunkPowerLedger.Key(mob.level().dimension().location().toString(),mob.chunkPosition().toLong());
        var cooldown=ticks>0?NaturalPowerChunks.get(server):null;
        if(cooldown!=null&&!cooldown.available(chunk,now))return;
        var a=new ArrayList<String>();var f=new ArrayList<String>();
        for(var metal:Metal.values())for(String art:List.of("a:","f:")){
            String key=art+metal.id();if(mob.getAttribute(PowerAttributes.holder(key))!=null&&dev.cosmerearcanists.hemalurgy.HemalurgySources.enabled(key))(art.equals("a:")?a:f).add(key);
        }
        var powers=NaturalPowerRoll.roll(NaturalMobConfig.ONE_IN.get(),NaturalMobConfig.BOTH.get(),NaturalMobConfig.SETS.get(),NaturalMobConfig.SET_ONE_IN.get(),a,f,mob.getRandom()::nextInt);
        // Claim only a successful grant, atomically rechecking the chunk for concurrent spawn attempts.
        if(powers.isEmpty()||cooldown!=null&&!cooldown.claim(chunk,now,ticks))return;
        for(String key:powers)mob.getAttribute(PowerAttributes.holder(key)).setBaseValue(NaturalMobConfig.STRENGTH.get());
    }
    @SubscribeEvent public void worldLoaded(net.neoforged.neoforge.event.level.LevelEvent.Load event){
        if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))NaturalPowerChunks.install(level);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void spawn(FinalizeSpawnEvent event){
        if(!event.isSpawnCancelled()&&event.getSpawnType()!=MobSpawnType.CONVERSION)initialize(event.getEntity());
    }
    @SubscribeEvent public void converted(LivingConversionEvent.Post event){
        if(!(event.getEntity() instanceof Mob from)||!(event.getOutcome() instanceof Mob to)||!checked(from))return;
        to.getPersistentData().put(DATA,from.getPersistentData().getCompound(DATA).copy());
        for(String key:PowerAttributes.keys()){
            var source=from.getAttribute(PowerAttributes.holder(key));var target=to.getAttribute(PowerAttributes.holder(key));
            if(source!=null&&target!=null)target.setBaseValue(source.getBaseValue());
        }
    }
}
