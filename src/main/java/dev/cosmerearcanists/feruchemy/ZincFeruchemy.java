package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Mental-speed seconds; actual funded output drives actions, slight movement and reaction assistance. */
public final class ZincFeruchemy {
    private static final String POWER="f:zinc",CLOCK="zinc_weapon_remainder";
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_zinc_mind");
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.ZINC.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.ZINC).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.ZINC_STORE_STEP.get(),FeruchemyConfig.ZINC_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.ZINC_TAP_LEVELS.get());}
    public static double factor(Player p){var a=p.getAttribute(PowerAttributes.MENTAL_SPEED);var m=a==null?null:a.getModifier(MODIFIER);return m==null?1:1+m.amount();}
    public static double fundedLevels(Player p){return Math.max(0,factor(p)-1)/FeruchemyConfig.ZINC_TAP_STEP.get();}
    public static double weaponRate(Player p){var a=p.getAttribute(PowerAttributes.WEAPON_PREPARATION);var m=a==null?null:a.getModifier(MODIFIER);return !FeruchemyConfig.ZINC_WEAPONS.get()||m==null?1:1+m.amount();}
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,POWER)!=0?PowerAttributes.strength(p,POWER)*Math.abs(factor(p)-1):0;}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,double factor){var a=p.getAttribute(attribute);if(a==null)return;double amount=factor-1;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-amount)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(amount)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void apply(ServerPlayer p,double mental){modifier(p,PowerAttributes.MENTAL_SPEED,mental);double levels=Math.max(0,mental-1)/FeruchemyConfig.ZINC_TAP_STEP.get();double action=mental<1?mental:1+Math.min(FeruchemicalEnhancement.cap(p,"zinc",FeruchemyConfig.ZINC_ACTION_CAP.get()),levels*FeruchemyConfig.ZINC_ACTION_STEP.get());modifier(p,PowerAttributes.WEAPON_PREPARATION,FeruchemyConfig.ZINC_WEAPONS.get()?action:1);modifier(p,Attributes.ATTACK_SPEED,FeruchemyConfig.ZINC_ATTACK.get()?action:1);modifier(p,Attributes.MOVEMENT_SPEED,FeruchemyConfig.ZINC_MOVEMENT.get()?1+Math.min(FeruchemicalEnhancement.cap(p,"zinc",FeruchemyConfig.ZINC_MOVEMENT_CAP.get()),levels*FeruchemyConfig.ZINC_MOVEMENT_STEP.get()):1);}
    public static void clear(ServerPlayer p){apply(p,1);p.getPersistentData().remove(CLOCK);ZincReactions.clear(p);}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);double attribute=PowerAttributes.strength(p,POWER),mental=1;
        if(configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&attribute>0&&state!=0){
            boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.ZINC_STORE_STEP.get(),FeruchemyConfig.ZINC_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.ZINC_TAP_STEP.get(),FeruchemyConfig.ZINC_CAP.get());
            double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),FeruchemyConfig.ZINC_LOSS.get()))/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"zinc",magnitude);
            double moved=storing?Metalminds.storePerTick(p,"zinc",cost):FeruchemicalEnhancement.tap(p,"zinc",cost);
            double actual=cost>0?magnitude*Math.min(1,Math.abs(moved)/cost):0;mental=storing?1-actual:1+actual;
            if(cost<=0||Math.abs(moved)<cost-1e-9)PowerData.state(p,POWER,0);
        }
        apply(p,mental);ZincReactions.tick(p,Math.max(0,mental-1)/FeruchemyConfig.ZINC_TAP_STEP.get());if(mental==1)p.getPersistentData().remove(CLOCK);
    }
    @SubscribeEvent public void startUse(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p)p.getPersistentData().remove(CLOCK);}
    @SubscribeEvent public void weaponUse(LivingEntityUseItemEvent.Tick e){
        if(!(e.getEntity() instanceof Player p)||e.getDuration()<=0)return;var animation=e.getItem().getUseAnimation();if(animation!=UseAnim.BOW&&animation!=UseAnim.CROSSBOW)return;
        double rate=weaponRate(p);var data=p.getPersistentData();if(Math.abs(rate-1)<1e-10){data.remove(CLOCK);return;}double clock=data.getDouble(CLOCK)+rate;int steps=(int)Math.floor(clock+1e-9);data.putDouble(CLOCK,Math.max(0,clock-steps));e.setDuration(Math.max(1,e.getDuration()+1-steps));
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
}
