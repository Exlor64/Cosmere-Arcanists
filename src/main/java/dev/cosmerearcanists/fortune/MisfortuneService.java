package dev.cosmerearcanists.fortune;

import dev.cosmerearcanists.feruchemy.ChromiumFeruchemy;
import dev.cosmerearcanists.power.PowerService;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Contextual accidents only while Fortune storage is actually paid; cooldowns survive mode switches and relogs. */
public final class MisfortuneService {
    public static final String KEY="cosmerearcanists_misfortune";
    private static final String PROJECTILE_CHECKED="cosmerearcanists_fortune_checked";
    private static long now(ServerPlayer p){return p.server.overworld().getGameTime();}
    private static boolean elapsed(net.minecraft.nbt.CompoundTag data,String key,long now,int duration){return !data.contains(key)||now<data.getLong(key)||now-data.getLong(key)>=duration;}
    public static boolean ready(ServerPlayer p,Misfortune effect){double stored=ChromiumFeruchemy.storedFraction(p);var settings=FortuneConfig.EVENTS.get(effect);if(!FortuneConfig.MISHAPS.get()||!settings.enabled().get()||stored+1e-9<settings.minimum().get())return false;var data=p.getPersistentData().getCompound(KEY);return elapsed(data,"last_event",now(p),FortuneConfig.GLOBAL_COOLDOWN.get())&&elapsed(data,"last_"+effect.key(),now(p),settings.cooldown().get());}
    private static void happened(ServerPlayer p,Misfortune effect){var data=p.getPersistentData().getCompound(KEY);data.putLong("last_event",now(p));data.putLong("last_"+effect.key(),now(p));p.getPersistentData().put(KEY,data);if(FortuneConfig.MESSAGES.get())p.displayClientMessage(Component.literal("Bad luck: "+effect.message+"."),true);}
    private static boolean actionRoll(ServerPlayer p,Misfortune effect){return ready(p,effect)&&p.getRandom().nextDouble()<FortuneMath.probability(ChromiumFeruchemy.storedFraction(p),FortuneConfig.ACTION_CHANCE.get());}
    private static Mob hiddenHostile(ServerPlayer p){double range=FortuneConfig.DETECTION_RANGE.get();return p.serverLevel().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(range),mob->mob instanceof Enemy&&mob.isAlive()&&mob.getTarget()!=p&&!mob.isAlliedTo(p)&&mob.distanceToSqr(p)<=range*range&&!mob.hasLineOfSight(p)).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}
    private static EquipmentSlot armor(ServerPlayer p){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(p.getItemBySlot(slot).isDamageableItem())return slot;return null;}
    private static net.minecraft.core.Holder<MobEffect> debuff(Misfortune effect){return switch(effect){case CRAMP->MobEffects.MOVEMENT_SLOWDOWN;case WEAK_GRIP->MobEffects.WEAKNESS;case DIZZY->MobEffects.CONFUSION;case DUST_IN_EYES->MobEffects.BLINDNESS;case FATIGUE->MobEffects.DIG_SLOWDOWN;default->null;};}
    public static boolean contextAllows(ServerPlayer p,Misfortune effect){
        var debuff=debuff(effect);if(debuff!=null)return !p.hasEffect(debuff);
        return switch(effect){
            case WALL_DETECTION->hiddenHostile(p)!=null;
            case LIGHTNING->p.serverLevel().isThundering()&&p.serverLevel().isRainingAt(p.blockPosition())&&p.serverLevel().canSeeSky(p.blockPosition());
            case TOOL_CHIP->p.getMainHandItem().isDamageableItem();case ARMOR_WEAR->armor(p)!=null;
            case STUMBLE->p.onGround()&&!p.isPassenger()&&p.getDeltaMovement().horizontalDistanceSqr()>.0001;
            case HUNGER_PANG->p.getFoodData().getFoodLevel()>0;
            case LOST_BREATH->p.isUnderWater()&&p.getAirSupply()>0;
            case FIRE_FLARE->p.isOnFire()&&!p.fireImmune();
            case CHILL->p.canFreeze()&&p.getTicksFrozen()>0&&p.getTicksFrozen()<p.getTicksRequiredToFreeze();
            case SHIELD_SLIP->p.isUsingItem()&&p.getUseItem().getUseAnimation()==UseAnim.BLOCK;
            default->false;
        };
    }
    /** Deterministic server entry point; still enforces paid storage, environmental requirements and every cooldown. */
    public static boolean apply(ServerPlayer p,Misfortune effect){
        if(effect.action||!ready(p,effect)||!contextAllows(p,effect))return false;double severity=ChromiumFeruchemy.storedFraction(p);var debuff=debuff(effect);
        if(debuff!=null){if(!p.addEffect(new MobEffectInstance(debuff,effect==Misfortune.DUST_IN_EYES?40:80+(int)(severity*80),0)))return false;}
        else switch(effect){
            case WALL_DETECTION->{var mob=hiddenHostile(p);if(mob==null)return false;mob.setTarget(p);if(mob.getTarget()!=p)return false;}
            case LIGHTNING->{var bolt=EntityType.LIGHTNING_BOLT.create(p.serverLevel());if(bolt==null)return false;bolt.setPos(p.position());if(!p.serverLevel().addFreshEntity(bolt))return false;}
            case TOOL_CHIP->p.getMainHandItem().hurtAndBreak(1+(int)(severity*3),p,EquipmentSlot.MAINHAND);
            case ARMOR_WEAR->{var slot=armor(p);if(slot==null)return false;p.getItemBySlot(slot).hurtAndBreak(1+(int)(severity*3),p,slot);}
            case STUMBLE->{double angle=p.getRandom().nextDouble()*Math.PI*2;PowerService.externalImpulse(p,new Vec3(Math.cos(angle)*.12,0,Math.sin(angle)*.12));}
            case HUNGER_PANG->p.causeFoodExhaustion((float)(2+4*severity));
            case LOST_BREATH->p.setAirSupply(Math.max(0,p.getAirSupply()-20-(int)(severity*60)));
            case FIRE_FLARE->p.setRemainingFireTicks(p.getRemainingFireTicks()+20+(int)(severity*60));
            case CHILL->p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze(),p.getTicksFrozen()+10+(int)(severity*40)));
            case SHIELD_SLIP->{var item=p.getUseItem().getItem();p.stopUsingItem();p.getCooldowns().addCooldown(item,20+(int)(severity*40));}
            default->{return false;}
        }
        happened(p,effect);return true;
    }
    public static void tick(ServerPlayer p){
        long time=now(p);if(time%20!=0||ChromiumFeruchemy.storedFraction(p)<=0||!FortuneConfig.MISHAPS.get())return;var data=p.getPersistentData().getCompound(KEY);if(data.contains("last_check")&&data.getLong("last_check")==time)return;data.putLong("last_check",time);p.getPersistentData().put(KEY,data);
        if(!elapsed(data,"last_event",time,FortuneConfig.GLOBAL_COOLDOWN.get())||p.getRandom().nextDouble()>=FortuneMath.probability(ChromiumFeruchemy.storedFraction(p),FortuneConfig.PERIODIC_CHANCE.get()))return;
        var eligible=new ArrayList<Misfortune>();int total=0;for(var effect:Misfortune.values())if(!effect.action&&ready(p,effect)&&contextAllows(p,effect)){eligible.add(effect);total+=FortuneConfig.EVENTS.get(effect).weight().get();}if(total==0)return;int choice=p.getRandom().nextInt(total);for(var effect:eligible){choice-=FortuneConfig.EVENTS.get(effect).weight().get();if(choice<0){apply(p,effect);break;}}
    }
    private static Vec3 swerve(ServerPlayer p,Vec3 velocity){double turn=(p.getRandom().nextBoolean()?1:-1)*Math.toRadians(45+p.getRandom().nextDouble()*75);var rotated=FortuneMath.rotate(velocity.x,velocity.z,turn);if(velocity.horizontalDistanceSqr()<1e-6){double angle=p.getRandom().nextDouble()*Math.PI*2;double sideways=Math.max(.1,velocity.length()*.35);rotated[0]=Math.cos(angle)*sideways;rotated[1]=Math.sin(angle)*sideways;}return new Vec3(rotated[0],velocity.y,rotated[1]);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void toss(ItemTossEvent e){if(e.getPlayer() instanceof ServerPlayer p&&!e.getEntity().isRemoved()&&actionRoll(p,Misfortune.DROPPED_ITEM_VEER)){e.getEntity().setDeltaMovement(swerve(p,e.getEntity().getDeltaMovement()));e.getEntity().hasImpulse=true;happened(p,Misfortune.DROPPED_ITEM_VEER);}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void projectile(EntityJoinLevelEvent e){if(e.loadedFromDisk()||!(e.getEntity() instanceof Projectile projectile)||projectile.getPersistentData().getBoolean(PROJECTILE_CHECKED)||!(projectile.getOwner() instanceof ServerPlayer p))return;projectile.getPersistentData().putBoolean(PROJECTILE_CHECKED,true);if(projectile.getDeltaMovement().lengthSqr()>1e-6&&actionRoll(p,Misfortune.PROJECTILE_VEER)){projectile.setDeltaMovement(swerve(p,projectile.getDeltaMovement()));projectile.hasImpulse=true;happened(p,Misfortune.PROJECTILE_VEER);}}
    public static boolean lostCatch(ServerPlayer p){if(!actionRoll(p,Misfortune.LOST_CATCH))return false;happened(p,Misfortune.LOST_CATCH);return true;}
    @SubscribeEvent public void clonePlayer(PlayerEvent.Clone e){if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());}
}
