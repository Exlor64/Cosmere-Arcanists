package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.ServerConfig;
import dev.cosmerearcanists.compounding.IdentityMath;
import dev.cosmerearcanists.equipment.SpikeItem;
import dev.cosmerearcanists.filing.Filings;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Capture the lethal melee hit, then settle after death cancellation handlers have all finished. */
public final class HemalurgyService {
    private record Pending(LivingDeathEvent death,ServerPlayer attacker,ItemStack weapon,HemalurgySources.Source source,HemalurgicSpikes.Charge charge,String metal){}
    private static final Map<UUID,Pending> PENDING=new LinkedHashMap<>();
    private static final String CLAIM="cosmerearcanists_hemalurgic_claim";
    public static boolean enabled(String metal){var config=HemalurgyConfig.METALS.get(metal);return dev.cosmerearcanists.power.PowerConfig.ENABLED.get()&&ServerConfig.HEMALURGY_ENABLED.get()&&HemalurgyConfig.ENABLED.get()&&config!=null&&config.get();}
    private static boolean allowed(LivingEntity victim){return victim instanceof ServerPlayer?HemalurgyConfig.PLAYERS.get():victim instanceof net.minecraft.world.entity.Mob&&HemalurgyConfig.MOBS.get();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void death(LivingDeathEvent event){capture(event);}
    public static void capture(LivingDeathEvent e){if(e.isCanceled()||e.getEntity().level().isClientSide||!e.getSource().is(DamageTypes.PLAYER_ATTACK)||!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getSource().getDirectEntity()!=p||p==e.getEntity()||!p.isAlive()||p.isSpectator()||!allowed(e.getEntity())||e.getEntity().getPersistentData().hasUUID(CLAIM)||PENDING.containsKey(e.getEntity().getUUID()))return;
        var weapon=p.getMainHandItem();if(!(weapon.getItem() instanceof SpikeItem spike)||weapon.getCount()<1||HemalurgicSpikes.marked(weapon)||!enabled(spike.metal))return;if(spike.metal.equals("aluminum")||spike.metal.equals("duralumin")){if(e.getEntity() instanceof ServerPlayer)PENDING.put(e.getEntity().getUUID(),new Pending(e,p,weapon,null,null,spike.metal));return;}var source=HemalurgySources.select(e.getEntity(),spike.metal);if(source==null||source.identities().size()>64)return;double fraction=source.trait()?1:HemalurgyConfig.RETENTION.get();var parts=new ArrayList<IdentityMath.Portion>();for(var part:source.identities()){double retained=source.trait()?part.strength():HemalurgyRules.retained(part.strength(),fraction);if(retained>0)parts.add(new IdentityMath.Portion(part.identity(),retained));}if(parts.isEmpty())return;var charge=new HemalurgicSpikes.Charge(UUID.randomUUID(),e.getEntity().getUUID(),spike.metal,source.power(),parts,source.senses(),source.cognition());PENDING.put(e.getEntity().getUUID(),new Pending(e,p,weapon,source,charge,spike.metal));
    }
    private static void settle(Pending pending){var event=pending.death();var victim=event.getEntity();var p=pending.attacker();var weapon=pending.weapon();if(event.isCanceled()||!victim.isDeadOrDying()||victim.getPersistentData().hasUUID(CLAIM)||!enabled(pending.metal())||!allowed(victim)||pending.source()!=null&&!HemalurgySources.enabled(pending.source().power())||!p.isAlive()||p.isRemoved()||p.getMainHandItem()!=weapon||weapon.isEmpty()||HemalurgicSpikes.marked(weapon)||pending.source()!=null&&!HemalurgySources.unchanged(victim,pending.source()))return;
        if(pending.metal().equals("aluminum")){if(!(victim instanceof ServerPlayer target))return;AluminumSuppression.apply(target);victim.getPersistentData().putUUID(CLAIM,UUID.randomUUID());p.displayClientMessage(net.minecraft.network.chat.Component.literal("Aluminum spike suppressed the victim's powers."),true);return;}
        var charge=pending.charge();if(pending.metal().equals("duralumin")){if(!(victim instanceof ServerPlayer target))return;var contents=DuraluminTheft.capture(target);charge=new HemalurgicSpikes.Charge(UUID.randomUUID(),target.getUUID(),"duralumin",DuraluminTheft.POWER,List.of(new IdentityMath.Portion(contents.identity(),1)),HemalurgicTraits.NONE,HemalurgicTraits.NO_COGNITION,contents);}
        var charged=weapon.copyWithCount(1);if(!HemalurgicSpikes.store(charged,charge))return;
        if(charge.connection()!=null)DuraluminTheft.remove((ServerPlayer)victim,charge.connection());else HemalurgySources.remove(victim,pending.source());victim.getPersistentData().putUUID(CLAIM,charge.id());
        if(weapon.getCount()==1){ // Keep the exact stack object in hand, so ordinary inventory synchronization remains authoritative.
            if(!HemalurgicSpikes.store(weapon,charge))throw new IllegalStateException("Spike changed during synchronous theft");
        }else{weapon.shrink(1);p.setItemInHand(InteractionHand.MAIN_HAND,charged);Filings.give(p,weapon);}
        dev.cosmerearcanists.investiture.InvestitureSignals.emit(p.serverLevel(),new dev.cosmerearcanists.investiture.InvestitureSignals.Signal(dev.cosmerearcanists.investiture.InvestitureSignals.HEMALURGY,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cosmerearcanists",charge.metal()),p.getUUID(),victim.position(),Math.max(.01,Math.min(4096,charge.strength())),1,40,1,true),10);
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();p.displayClientMessage(net.minecraft.network.chat.Component.literal("Spike charged: "+HemalurgicSpikes.label(charge)+String.format(Locale.ROOT," (strength %.3f)",charge.strength())),true);
    }
    /** End-of-tick seam also used by synchronous engine tests. */
    public static void flush(){var work=List.copyOf(PENDING.values());PENDING.clear();for(var pending:work)settle(pending);}
    private static void flushFor(LivingEntity entity){var work=new ArrayList<Pending>();for(var it=PENDING.entrySet().iterator();it.hasNext();){var p=it.next().getValue();if(p.death().getEntity()==entity||p.attacker()==entity){work.add(p);it.remove();}}for(var p:work)settle(p);}
    @SubscribeEvent public void tick(ServerTickEvent.Post e){flush();}
    // Flush before the normal power-data clone/save so stolen personal strength cannot reappear on respawn.
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void clone(PlayerEvent.Clone e){flushFor(e.getOriginal());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void logout(PlayerEvent.PlayerLoggedOutEvent e){flushFor(e.getEntity());}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){PENDING.clear();}
}
