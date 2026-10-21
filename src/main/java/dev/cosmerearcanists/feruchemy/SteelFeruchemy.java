package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Physical speed-seconds live in steelminds; temporal clocks are not changed. */
public final class SteelFeruchemy {
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_steel_speed");
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.STEEL.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.STEEL).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:steel"),FeruchemyConfig.STEEL_STORE_STEP.get(),FeruchemyConfig.STEEL_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:steel"),FeruchemyConfig.STEEL_TAP_LEVELS.get());}
    public static double factor(Player p){var a=p.getAttribute(Attributes.MOVEMENT_SPEED);var m=a==null?null:a.getModifier(MODIFIER);return m==null?1:1+m.amount();}
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,"f:steel")!=0?PowerAttributes.strength(p,"f:steel")*Math.abs(factor(p)-1):0;}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,double factor){var a=p.getAttribute(attribute);if(a==null)return;double value=factor-1;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-value)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(value)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void apply(ServerPlayer p,double factor){step(p,FeruchemyConfig.STEEL_STEP.get()?Math.min(FeruchemyConfig.STEEL_STEP_CAP.get(),Math.max(0,factor-1)*FeruchemyConfig.STEEL_STEP_RATE.get()):0);modifier(p,Attributes.MOVEMENT_SPEED,factor);modifier(p,Attributes.ATTACK_SPEED,FeruchemyConfig.STEEL_ATTACK.get()?factor:1);modifier(p,Attributes.BLOCK_BREAK_SPEED,FeruchemyConfig.STEEL_MINING.get()?factor:1);}
    private static void step(ServerPlayer p,double extra){var a=p.getAttribute(Attributes.STEP_HEIGHT);if(a==null)return;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-extra)<1e-10)return;a.removeModifier(MODIFIER);if(extra>0)a.addTransientModifier(new AttributeModifier(MODIFIER,extra,AttributeModifier.Operation.ADD_VALUE));}
    private static boolean consumable(net.minecraft.world.item.ItemStack item){var animation=item.getUseAnimation();return animation==net.minecraft.world.item.UseAnim.EAT||animation==net.minecraft.world.item.UseAnim.DRINK;}
    @SubscribeEvent public void startUse(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start event){if(event.getEntity() instanceof Player p)p.getPersistentData().remove("steel_consuming_remainder");}
    @SubscribeEvent public void consume(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick event){
        if(!(event.getEntity() instanceof Player p)||!consumable(event.getItem())||!FeruchemyConfig.STEEL_CONSUMING.get()||event.getDuration()<=0)return;
        double rate=factor(p);var data=p.getPersistentData();double clock=data.getDouble("steel_consuming_remainder")+rate;int steps=(int)Math.floor(clock+1e-9);data.putDouble("steel_consuming_remainder",Math.max(0,clock-steps));
        // NeoForge applies this value before the vanilla decrement; keep >=1 so completion is not cancelled.
        event.setDuration(Math.max(1,event.getDuration()+1-steps));
    }
    public static void clear(ServerPlayer p){apply(p,1);p.getPersistentData().remove("steel_consuming_remainder");}
    public static void tick(ServerPlayer p){
        double factor=1;int state=PowerData.state(p,"f:steel");double attribute=PowerAttributes.strength(p,"f:steel");
        if(configured()&&p.isAlive()&&!p.isSpectator()&&attribute>0&&state!=0){
            boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.STEEL_STORE_STEP.get(),FeruchemyConfig.STEEL_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.STEEL_TAP_STEP.get(),FeruchemyConfig.STEEL_CAP.get());
            double cost=storing?magnitude/20:magnitude*Math.pow(Math.max(1,magnitude),FeruchemyConfig.STEEL_LOSS.get())/20;
            double moved=(storing?Metalminds.storePerTick(p,"steel",cost):Metalminds.transferAccessible(p,"steel",-cost));
            if(cost>0&&Math.abs(moved)>0)factor=storing?1-Math.abs(moved)*20:1+magnitude*Math.min(1,Math.abs(moved)/cost);
            if(cost<=0||Math.abs(moved)<cost-1e-9)PowerData.state(p,"f:steel",0);
        }apply(p,factor);
    }
    @SubscribeEvent public void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:steel",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:steel",0);}}
}
