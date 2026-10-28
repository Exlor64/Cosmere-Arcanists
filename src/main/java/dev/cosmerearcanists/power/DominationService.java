package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.investiture.EmotionalInfluenceEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Temporary ownership is separate from short emotional linger and never survives logout/restart. */
public final class DominationService {
    private record Bond(LivingEntity target,ServerPlayer owner,String power,double strength,double unresistedStrength,double allomanticStrength,long expires,long[] nextAttack) { }
    private static final Map<UUID,Bond> BONDS=new HashMap<>();
    public static boolean naturalKolossControl(LivingEntity target){return target instanceof dev.cosmerearcanists.koloss.KolossEntity&&dev.cosmerearcanists.koloss.KolossConfig.EMOTIONAL_CONTROL.get();}
    private static boolean integrityEligible(LivingEntity target){return naturalKolossControl(target)||SpiritualIntegrity.get(target)<=EmotionConfig.MAX_INTEGRITY.get();}
    private static double threshold(LivingEntity target){return naturalKolossControl(target)?Math.max(EmotionConfig.DOMINATION_STRENGTH.get(),dev.cosmerearcanists.koloss.KolossConfig.CONTROL_STRENGTH.get()):EmotionConfig.DOMINATION_STRENGTH.get();}
    public static void attempt(ServerPlayer source,LivingEntity target,String power,double compliance,double allomanticStrength){
        if(!EmotionalInfluenceEvent.allowed(target,allomanticStrength) || !EmotionConfig.DOMINATION.get() || compliance*SpiritualIntegrity.takeoverMultiplier(target)<threshold(target) || !integrityEligible(target))return;
        var old=BONDS.get(target.getUUID());if(old!=null && old.owner!=source && old.strength>=compliance)return;
        if(old==null && BONDS.size()>=EmotionConfig.AFFECTED.get())return;
        long duration=Math.min(EmotionConfig.MINION_CAP.get(),Math.round(EmotionConfig.MINION_TICKS.get()*compliance/EmotionConfig.DOMINATION_STRENGTH.get()));
        double multiplier=EmotionService.influenceMultiplier(target);if(multiplier<=0)return;
        BONDS.put(target.getUUID(),new Bond(target,source,power,compliance,compliance/multiplier,allomanticStrength,target.level().getGameTime()+duration,old==null?new long[]{0}:old.nextAttack));
        if(target instanceof Mob mob){mob.setTarget(null);mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);}
        if(old==null || old.owner!=source){source.displayClientMessage(Component.literal(target.getName().getString()+" is your minion for "+duration/20+" seconds."),true);if(target instanceof ServerPlayer player)player.displayClientMessage(Component.literal("Your damaged spirit has been seized by "+source.getName().getString()+"."),false);}
    }
    private static boolean valid(Bond b){return b.unresistedStrength*EmotionService.influenceMultiplier(b.target)*SpiritualIntegrity.takeoverMultiplier(b.target)+1e-9>=threshold(b.target) && EmotionConfig.DOMINATION.get() && EmotionConfig.COMPLIANCE.get() && EmotionService.configured(b.power) && EmotionService.eligible(b.target) && !b.target.isRemoved() && b.owner.isAlive() && !b.owner.isRemoved() && b.owner.level()==b.target.level() && b.target.level().getGameTime()<b.expires && integrityEligible(b.target) && !EmotionService.shielded(b.target) && EmotionalInfluenceEvent.allowed(b.target,b.allomanticStrength);}
    public static ServerPlayer owner(LivingEntity target){var b=BONDS.get(target.getUUID());return b!=null && valid(b)?b.owner:null;}
    public static long remaining(LivingEntity target){var b=BONDS.get(target.getUUID());return b!=null && valid(b)?b.expires-target.level().getGameTime():0;}
    private static LivingEntity enemy(Bond b){
        LivingEntity enemy=b.owner.getLastHurtMob();if(enemy==null || b.owner.tickCount-b.owner.getLastHurtMobTimestamp()>100){enemy=b.owner.getLastHurtByMob();if(b.owner.tickCount-b.owner.getLastHurtByMobTimestamp()>100)enemy=null;}
        return enemy!=null && enemy.isAlive() && enemy!=b.target && enemy!=b.owner && !enemy.isAlliedTo(b.owner) && owner(enemy)!=b.owner && enemy.level()==b.target.level() && enemy.distanceToSqr(b.target)<32*32 ?enemy:null;
    }
    public static boolean permits(LivingEntity target,LivingEntity victim){var b=BONDS.get(target.getUUID());return b==null || !valid(b) || victim==null || victim==enemy(b);}
    public static boolean behave(Mob mob){var b=BONDS.get(mob.getUUID());if(b==null || !valid(b))return false;if(mob.isNoAi())return true;
        var enemy=enemy(b);mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);mob.setTarget(enemy);
        if(enemy!=null){mob.getLookControl().setLookAt(enemy,30,30);if(mob.distanceToSqr(enemy)>4)EmotionService.navigate(mob,enemy.position(),1);else if(mob.level().getGameTime()>=b.nextAttack[0]){b.nextAttack[0]=mob.level().getGameTime()+20;if(mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)!=null)mob.doHurtTarget(enemy);else enemy.hurt(mob.damageSources().mobAttack(mob),2);}}
        else if(mob.distanceToSqr(b.owner)>9)EmotionService.navigate(mob,b.owner.position(),1);else mob.getNavigation().stop();return true;
    }
    public static void tick(){
        for(var it=BONDS.values().iterator();it.hasNext();){var b=it.next();if(!valid(b)){if(b.target instanceof Mob mob){mob.setTarget(null);mob.getNavigation().stop();mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);}if(b.target instanceof ServerPlayer p)p.displayClientMessage(Component.literal("You are free of emotional domination."),false);it.remove();continue;}
            if(b.target instanceof ServerPlayer p && p.level().getGameTime()%4==0){var enemy=enemy(b);var destination=enemy==null?b.owner:enemy;if(p.distanceToSqr(destination)>(enemy==null?9:4)){var impulse=destination.position().subtract(p.position()).multiply(1,0,1).normalize().scale(.12);p.setDeltaMovement(p.getDeltaMovement().add(impulse));PacketDistributor.sendToPlayer(p,new PowerNetwork.Impulse(p.level().dimension().location().toString(),impulse.x,0,impulse.z,100,false));}if(enemy!=null && p.distanceToSqr(enemy)<=9 && p.getAttackStrengthScale(.5f)>=1 && p.hasLineOfSight(enemy) && (!(enemy instanceof ServerPlayer other)||p.canHarmPlayer(other)))p.attack(enemy);}
        }
    }
    public static void remove(LivingEntity entity){BONDS.remove(entity.getUUID());BONDS.values().removeIf(b->b.owner==entity);}
    public static void clear(){BONDS.clear();}
    private DominationService() { }
}
