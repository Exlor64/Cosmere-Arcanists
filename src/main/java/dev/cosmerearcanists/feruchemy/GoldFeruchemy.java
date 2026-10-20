package dev.cosmerearcanists.feruchemy;
import java.util.*;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Gold charge represents stored health; storage sacrifices the current health ceiling. */
public final class GoldFeruchemy {
    private static final net.minecraft.resources.ResourceLocation CAP=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_gold_health");
    private static final String ACTIVE="feruchemical_gold_transfer";
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.GOLD.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.GOLD).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:gold"),FeruchemyConfig.GOLD_STORE_STEP.get(),FeruchemyConfig.GOLD_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:gold"),FeruchemyConfig.GOLD_TAP_LEVELS.get());}
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,"f:gold")!=0?PowerAttributes.strength(p,"f:gold")*Math.abs(p.getPersistentData().getDouble(ACTIVE))*20:0;}
    private static void ceiling(ServerPlayer p,double fraction){var a=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);if(a==null)return;var old=a.getModifier(CAP);double baseline=old==null?a.getValue():a.getValue()/(1+old.amount());double value=Math.max(-fraction,Math.min(0,FeruchemyConfig.GOLD_FLOOR.get()/baseline-1));if(old!=null&&Math.abs(old.amount()-value)<1e-10)return;a.removeModifier(CAP);if(value<0)a.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(CAP,value,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());}
    public static void clear(ServerPlayer p){p.getPersistentData().remove(ACTIVE);ceiling(p,0);}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public void blockHealing(net.neoforged.neoforge.event.entity.living.LivingHealEvent event){if(event.getEntity() instanceof ServerPlayer p&&configured()&&PowerData.state(p,"f:gold")<0&&PowerAttributes.strength(p,"f:gold")>0)event.setCanceled(true);}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public void rescue(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!configured()||!FeruchemyConfig.GOLD_RESCUE.get()||!p.isAlive()||p.isSpectator()||PowerAttributes.strength(p,"f:gold")<=0||PowerData.state(p,"f:gold")<=FeruchemyConfig.GOLD_RESCUE_LEVEL.get()||event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        double damage=event.getNewDamage(),absorbed=p.getAbsorptionAmount(),health=p.getHealth();if(!Double.isFinite(damage)||damage-absorbed<health)return;
        double cost=(damage-absorbed-health+1)*FeruchemyConfig.GOLD_RESCUE_COST.get();if(Metalminds.accessibleCharge(p,"gold")+1e-9<cost)return;
        double spent=-Metalminds.transferAccessible(p,"gold",-cost);if(spent<cost-1e-8){Metalminds.transferAccessible(p,"gold",spent);return;}
        if(health<1)p.setHealth(1);event.setNewDamage((float)(absorbed+Math.max(0,health-1)));p.getPersistentData().putDouble(ACTIVE,-spent);
    }
    public static void tick(ServerPlayer p){
        p.getPersistentData().remove(ACTIVE);int state=PowerData.state(p,"f:gold");double attribute=PowerAttributes.strength(p,"f:gold");
        if(!configured()||!p.isAlive()||p.isSpectator()||state==0||attribute<=0){ceiling(p,0);return;}
        if(state<0){
            double fraction=FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.GOLD_STORE_STEP.get(),FeruchemyConfig.GOLD_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());
            var maximum=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);var own=maximum.getModifier(CAP);double baseline=own==null?maximum.getValue():maximum.getValue()/(1+own.amount());fraction=Math.min(fraction,Math.max(0,1-FeruchemyConfig.GOLD_FLOOR.get()/baseline));
            double requested=fraction*FeruchemyConfig.GOLD_RATE.get()/20;
            double stored=Metalminds.transferAccessible(p,"gold",requested);
            ceiling(p,requested>0?fraction*Math.min(1,stored/requested):0);
            if(stored>0)p.getPersistentData().putDouble(ACTIVE,stored);
            if(requested<=0||stored<requested-1e-9)PowerData.state(p,"f:gold",0);
            return;
        }
        ceiling(p,0);
        if(Metalminds.accessibleCharge(p,"gold")<=0){PowerData.state(p,"f:gold",0);return;}
        double missing=p.getMaxHealth()-p.getHealth();if(missing<=0)return;
        double rate=Math.min(FeruchemyConfig.GOLD_CAP.get(),state*FeruchemyConfig.GOLD_TAP_STEP.get()*FeruchemyConfig.GOLD_RATE.get());
        double compression=Math.pow(Math.max(1,rate/FeruchemyConfig.GOLD_RATE.get()),FeruchemyConfig.GOLD_LOSS.get());double planned=Math.min(missing,rate/20),needed=planned*compression;
        var seen=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.world.item.ItemStack,Boolean>());var paid=new ArrayList<Payment>();double spent=0;
        for(var access:Metalminds.touching(p)){if(!seen.add(access.stack()))continue;double took=-Metalminds.transfer(access.stack(),p.getUUID(),"gold",-(needed-spent));if(took>0){paid.add(new Payment(access,took));access.changed().run();spent+=took;}if(spent>=needed-1e-10)break;}
        if(spent<=0){PowerData.state(p,"f:gold",0);return;}
        float before=p.getHealth();p.heal((float)(spent/compression));double healed=Math.max(0,p.getHealth()-before);double used=Math.min(spent,healed*compression),refund=spent-used;
        // Refund cancelled/reduced healing to the exact pieces charged, even if contact changed in a heal listener.
        for(var payment:paid){double back=Math.min(refund,payment.amount());if(back>0){double returned=Metalminds.transfer(payment.access().stack(),p.getUUID(),"gold",back);payment.access().changed().run();refund-=returned;}}
        if(used>0)p.getPersistentData().putDouble(ACTIVE,-used);
        if(spent<needed-1e-8)PowerData.state(p,"f:gold",0);
    }
    private record Payment(Metalminds.Access access,double amount){}
    @SubscribeEvent public void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:gold",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:gold",0);}}
}
