package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PewterService {
    private static final ResourceLocation SPEED=id("pewter_speed"), ATTACK=id("pewter_attack");
    public static final ResourceKey<DamageType> DRAG_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,id("pewter_drag"));
    private static final Map<UUID, Recovery> RECOVERY=new HashMap<>();
    private static final class Recovery { Vec3 position; int rest,heal,food; }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,path);}
    public static boolean configured() {
        return PowerConfig.ENABLED.get() && PewterConfig.ENABLED.get() && ServerConfig.ALLOMANCY.enabled().get()
            && ServerConfig.ALLOMANCY.metals().get(Metal.PEWTER).get();
    }
    public static double strength(ServerPlayer p) {
        if(!configured() || !p.isAlive() || p.isSpectator() || PowerAttributes.strength(p,"a:pewter")<=0
            || PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,"pewter")<=0)return 0;
        int state=PowerData.state(p,"a:pewter");
        if(state==3 && !PowerConfig.FLARING.get())state=2;
        return PowerAttributes.strength(p,"a:pewter")*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());
    }
    public static double debt(ServerPlayer p){return Math.max(0,PowerData.data(p).getDouble("pewter_drag"));}
    static void debt(ServerPlayer p,double amount){PowerData.data(p).putDouble("pewter_drag",Double.isFinite(amount)?Math.max(0,amount):0);}
    public static float exhaustion(Player p,float amount) {
        return p instanceof ServerPlayer server?(float)PewterMath.exhaustion(amount,strength(server),PewterConfig.EXHAUSTION.get()):amount;
    }
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,ResourceLocation id,double amount,AttributeModifier.Operation operation) {
        var instance=p.getAttribute(attribute); if(instance==null)return;
        var old=instance.getModifier(id);
        if(old!=null && old.amount()==amount)return;
        instance.removeModifier(id);
        if(amount>0)instance.addTransientModifier(new AttributeModifier(id,amount,operation));
    }
    static void clearModifiers(ServerPlayer p) {
        modifier(p,Attributes.MOVEMENT_SPEED,SPEED,0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ATTACK_DAMAGE,ATTACK,0,AttributeModifier.Operation.ADD_VALUE);
    }
    /** Called before snapshots and the other power handlers, independent of which slots are selected. */
    public static void tick(ServerPlayer p) {
        if(!p.isAlive()){clearModifiers(p);return;}
        if(configured() && PowerConfig.CONSUMPTION.get() && PowerData.state(p,"a:pewter")>0 && PowerData.reserve(p,"pewter")<=0)PowerData.state(p,"a:pewter",0);
        double power=strength(p);
        var recovery=RECOVERY.computeIfAbsent(p.getUUID(),key->new Recovery());
        if(power>0 && PowerConfig.CONSUMPTION.get()) {
            int state=PowerData.state(p,"a:pewter"); if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,"a:pewter",2);}
            double drain=PewterConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get());
            PowerData.reserve(p,"pewter",PowerData.reserve(p,"pewter")-drain);
            if(PowerData.reserve(p,"pewter")<=0){PowerData.state(p,"a:pewter",0);power=0;}
        }
        modifier(p,Attributes.MOVEMENT_SPEED,SPEED,Math.min(PewterConfig.MAX_SPEED_BONUS.get(),power*PewterConfig.SPEED.get()),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ATTACK_DAMAGE,ATTACK,Math.min(PewterConfig.MAX_ATTACK_BONUS.get(),power*PewterConfig.ATTACK.get()),AttributeModifier.Operation.ADD_VALUE);
        if(power<=0) {
            recovery.rest=recovery.heal=recovery.food=0;recovery.position=p.position();
            // Configuration disables freeze debt; genuine cessation or lost power settles it.
            if(configured() && PewterConfig.DRAG.get() && !p.isSpectator())settle(p);
            return;
        }
        boolean resting=(p.onGround() || p.isSleeping()) && recovery.position!=null && p.position().distanceToSqr(recovery.position)<.0004 && !p.isPassenger();
        recovery.position=p.position();
        if(PewterConfig.DRAG.get() && debt(p)>0) {
            if(PewterConfig.REST.get() && resting) {
                if(++recovery.rest>=PewterConfig.REST_TICKS.get()){debt(p,PewterMath.rested(debt(p),power,PewterConfig.REST_FRACTION.get()));recovery.rest=0;}
            } else recovery.rest=0;
            if(++recovery.food>=20) {
                recovery.food=0;
                if(PewterConfig.SATURATION.get() && PewterConfig.SATURATION_RECOVERY.get()>0) {
                    var food=p.getFoodData();
                    double spent=Math.min(food.getSaturationLevel(),Math.min(PewterConfig.SATURATION_RATE.get()*power,debt(p)/PewterConfig.SATURATION_RECOVERY.get()));
                    food.setSaturation((float)(food.getSaturationLevel()-spent));
                    debt(p,debt(p)-spent*PewterConfig.SATURATION_RECOVERY.get());
                }
            }
        } else recovery.rest=recovery.food=0;
        if(++recovery.heal>=PewterConfig.HEAL_TICKS.get()) {
            recovery.heal=0;if(PewterConfig.HEAL.get()>0 && p.getHealth()<p.getMaxHealth())p.heal((float)(PewterConfig.HEAL.get()*power));
        }
    }
    /** Debt is already mitigated injury. Charging health directly avoids armor/absorption/i-frame discounts. */
    static void settle(ServerPlayer p) {
        double owed=debt(p);if(owed<=0 || !p.isAlive())return;
        debt(p,0);
        var source=new DamageSource(p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DRAG_DAMAGE));
        p.getCombatTracker().recordDamage(source,(float)owed);
        p.setHealth((float)Math.max(0,p.getHealth()-owed));
        if(p.isDeadOrDying())p.die(source);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void mitigate(LivingDamageEvent.Pre event) {
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        double power=strength(p); if(power<=0)return;
        double mitigation=PewterMath.mitigation(PowerAttributes.strength(p,"a:pewter"),power,PewterConfig.MITIGATION.get(),PewterConfig.BASE_CAP.get(),PewterConfig.STRONG_CAP.get());
        double damage=Math.max(0,event.getNewDamage()*(1-mitigation));
        double deferred=PewterConfig.DRAG.get()?PewterMath.deferred(damage,p.getAbsorptionAmount(),PewterConfig.DRAG_FRACTION.get()):0;
        event.setNewDamage((float)(damage-deferred));
    }
    @SubscribeEvent public void accumulate(LivingDamageEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || strength(p)<=0 || !PewterConfig.DRAG.get())return;
        double fraction=PewterConfig.DRAG_FRACTION.get();
        if(fraction>0 && event.getNewDamage()>0)debt(p,debt(p)+event.getNewDamage()*fraction/(1-fraction));
    }
    @SubscribeEvent public void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer p){debt(p,0);clearModifiers(p);RECOVERY.remove(p.getUUID());}
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){RECOVERY.remove(event.getEntity().getUUID());}
    public PewterService() { }
}
