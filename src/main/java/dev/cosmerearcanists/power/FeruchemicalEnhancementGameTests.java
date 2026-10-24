package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeruchemicalEnhancementGameTests {
    private static FakePlayer player(GameTestHelper h,String metal,int duralumin){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"EnhancedFerring"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:"+metal,1);PowerAttributes.set(p,"a:duralumin",1);PowerData.reserve(p,"duralumin",100);PowerData.state(p,"a:duralumin",duralumin);return p;}
    private static ItemStack mind(FakePlayer p,String metal,double charge){var s=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_metalmind_ring")));if(charge>0)Metalminds.transfer(s,p.getUUID(),metal,charge);p.setItemInHand(InteractionHand.OFF_HAND,s);return s;}
    private static void close(GameTestHelper h,double actual,double expected,String label){h.assertTrue(Double.isFinite(actual)&&Math.abs(actual-expected)<1e-5,label+": "+actual+" expected "+expected);}
    @GameTest(template="empty") public static void duraluminUsesTheSameContinuousStrengthAndConsumptionForFeruchemy(GameTestHelper h){
        var p=player(h,"steel",1);var mind=mind(p,"steel",100);PowerData.state(p,"f:steel",10);double cost=1.5*Math.pow(1.5,.25)/20;SteelFeruchemy.tick(p);close(h,SteelFeruchemy.factor(p),1+1.5*1.75,"Low burn amplifies paid extra speed1.75times");close(h,100-Metalminds.charge(mind),cost*5,"Low burn consumes five times the ordinary charge");
        PowerData.state(p,"a:duralumin",2);double before=Metalminds.charge(mind);SteelFeruchemy.tick(p);close(h,SteelFeruchemy.factor(p),1+1.5*2.5,"Normal burn amplifies2.5times");close(h,before-Metalminds.charge(mind),cost*10,"Normal burn consumes ten times charge");close(h,PowerAttributes.strength(p,"f:steel"),1,"Amplification never changes the saved power attribute");
        var empty=mind(p,"steel",0);PowerData.state(p,"f:steel",-3);for(int i=0;i<20;i++)SteelFeruchemy.tick(p);close(h,Metalminds.charge(empty),.3,"Storing is neither amplified nor charged extra");SteelFeruchemy.clear(p);EnhancementService.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void duraluminExceedsPhysicalActionAndMitigationCapsWithoutNegativeDamage(GameTestHelper h){
        var p=player(h,"steel",1);mind(p,"steel",500);PowerAttributes.set(p,"f:steel",10);PowerData.state(p,"f:steel",100);SteelFeruchemy.tick(p);h.assertTrue(SteelFeruchemy.factor(p)>FeruchemyConfig.STEEL_CAP.get(),"Duralumin exceeds the normal steel multiplier cap");SteelFeruchemy.clear(p);
        PowerAttributes.set(p,"f:zinc",10);mind(p,"zinc",500);PowerData.state(p,"f:zinc",100);ZincFeruchemy.tick(p);h.assertTrue(ZincFeruchemy.weaponRate(p)>2&&p.getAttributeValue(Attributes.MOVEMENT_SPEED)/p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()>1.05,"Zinc's synchronized preparation and movement caps rise with amplification");ZincFeruchemy.clear(p);
        PowerAttributes.set(p,"f:electrum",1);mind(p,"electrum",100);PowerData.state(p,"f:electrum",10);ElectrumFeruchemy.tick(p);h.assertTrue(ElectrumFeruchemy.damageReduction(p)>.5&&ElectrumFeruchemy.damageReduction(p)<=.95,"Amplified electrum exceeds its normal50percent cap but never produces negative damage");ElectrumFeruchemy.clear(p);EnhancementService.clear(p);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80) public static void duraluminFlareCommitsCapturedMetalmindFuelOnceAndExpiresByFullness(GameTestHelper h){
        var p=player(h,"steel",3);var mind=mind(p,"steel",300);PowerData.state(p,"f:steel",10);EnhancementService.begin(p);close(h,Metalminds.charge(mind),0,"Half-full ring's captured charge leaves the physical item immediately");h.assertTrue(FeruchemicalEnhancement.flaring(p,"steel"),"The original physical metalmind authorizes its private captured burst");var snapshot=new PowerNetwork.Snapshot(p.level().dimension().location().toString(),"f:steel","a:duralumin",5,java.util.List.of(new PowerNetwork.Entry("f:steel",10,0,3,10,300,50)),java.util.List.of());var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{PowerNetwork.Snapshot.CODEC.encode(buffer,snapshot);h.assertTrue(PowerNetwork.Snapshot.CODEC.decode(buffer).equals(snapshot),"HUD snapshot preserves committed fuel and remaining burst ticks");}finally{buffer.release();}SteelFeruchemy.tick(p);close(h,SteelFeruchemy.factor(p),8.5,"Flare gives five times normal extra speed");
        EnhancementService.finish(p);double committed=FeruchemicalEnhancement.emergencyAvailable(p,"steel");EnhancementService.finish(p);close(h,FeruchemicalEnhancement.emergencyAvailable(p,"steel"),committed,"Same-tick finish never spends captured fuel twice");Metalminds.transfer(mind,p.getUUID(),"steel",10);EnhancementService.begin(p);close(h,Metalminds.charge(mind),10,"New deposits cannot be folded into a committed burst");
        PowerData.state(p,"f:steel",0);h.assertTrue(!FeruchemicalEnhancement.flaring(p,"steel"),"Off revokes the effect without refunding the commitment");PowerData.state(p,"f:steel",10);h.assertTrue(FeruchemicalEnhancement.flaring(p,"steel"),"Reselecting may use only the original remaining interval");
        h.runAfterDelay(51,()->{EnhancementService.finish(p);h.assertTrue(!FeruchemicalEnhancement.committed(p,"steel")&&PowerData.state(p,"f:steel")==0,"Half-full ring expires after50ticks, not100");close(h,Metalminds.charge(mind),10,"Expiry preserves post-capture deposits");close(h,PowerData.reserve(p,"duralumin"),0,"Duralumin's own captured reserve is consumed on the same interval");SteelFeruchemy.clear(p);EnhancementService.clear(p);h.succeed();});
    }
    @GameTest(template="empty") public static void duraluminRespectsOwnershipContactAndTheCopperNotebookExclusion(GameTestHelper h){
        var p=player(h,"steel",3);var item=mind(p,"steel",0);Metalminds.transfer(item,UUID.randomUUID(),"steel",100);PowerData.state(p,"f:steel",10);EnhancementService.begin(p);h.assertTrue(!FeruchemicalEnhancement.committed(p,"steel")&&Metalminds.charge(item)==100,"A foreign keyed mind cannot be captured");
        item=mind(p,"steel",100);EnhancementService.begin(p);h.assertTrue(FeruchemicalEnhancement.flaring(p,"steel"),"Owned fuel can commit");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(!FeruchemicalEnhancement.flaring(p,"steel"),"Lost physical contact cannot retain boosted effects");p.setItemInHand(InteractionHand.OFF_HAND,item);Metalminds.wipe(item);h.assertTrue(!FeruchemicalEnhancement.flaring(p,"steel"),"Wiping removes the physical commitment marker");EnhancementService.clear(p);
        var archivist=player(h,"copper",3);var copper=mind(archivist,"copper",1);PowerData.state(archivist,"f:copper",1);EnhancementService.begin(archivist);h.assertTrue(!FeruchemicalEnhancement.committed(archivist,"copper")&&Metalminds.charge(copper)==1,"Duralumin never consumes a copper notebook's data binding");EnhancementService.clear(archivist);h.succeed();
    }
    @GameTest(template="empty") public static void duraluminGoldRefundsCancelledHealingAndNativeNutritionAndAirStayBounded(GameTestHelper h){
        var p=player(h,"gold",1);var gold=mind(p,"gold",100);p.setHealth(10);PowerData.state(p,"f:gold",10);java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);try{GoldFeruchemy.tick(p);close(h,Metalminds.charge(gold),100,"Cancelled amplified healing refunds its full physical cost");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}GoldFeruchemy.tick(p);h.assertTrue(p.getHealth()>10.1&&Metalminds.charge(gold)<100,"Funded amplified healing gains more health and pays additional fuel");
        var gasper=player(h,"cadmium",2);var air=mind(gasper,"cadmium",100);gasper.setAirSupply(gasper.getMaxAirSupply());PowerData.state(gasper,"f:cadmium",10);CadmiumFeruchemy.tick(gasper);h.assertTrue(gasper.getAirSupply()==gasper.getMaxAirSupply()&&Metalminds.charge(air)<100,"Amplified oxygen pays fuel while respecting native maximum air");
        var eater=player(h,"bendalloy",1);var food=mind(eater,"bendalloy",100);eater.getFoodData().setFoodLevel(19);eater.getFoodData().setSaturation(19);PowerData.state(eater,"f:bendalloy",10);for(int i=0;i<40;i++)BendalloyFeruchemy.tick(eater);h.assertTrue(eater.getFoodData().getFoodLevel()==20&&eater.getFoodData().getSaturationLevel()<=20&&Metalminds.charge(food)<100,"Amplified nutrition never overfills hunger or saturation");GoldFeruchemy.clear(p);CadmiumFeruchemy.clear(gasper);BendalloyFeruchemy.clear(eater);EnhancementService.clear(p);EnhancementService.clear(gasper);EnhancementService.clear(eater);h.succeed();
    }
    @GameTest(template="empty") public static void duraluminRaisesAllomanticCapsAndFeatureGatesRestoreOrdinaryBehavior(GameTestHelper h){
        var p=player(h,"steel",1);PowerAttributes.set(p,"a:pewter",64);PowerData.reserve(p,"pewter",100);PowerData.state(p,"a:pewter",2);PewterService.tick(p);h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED)/p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()>4,"Allomantic pewter exceeds its normal movement bonus cap");
        boolean enabled=EnhancementConfig.FERUCHEMY.get(),caps=EnhancementConfig.OVERCAPS.get();try{EnhancementConfig.FERUCHEMY.set(false);mind(p,"steel",100);PowerData.state(p,"f:steel",10);SteelFeruchemy.tick(p);close(h,SteelFeruchemy.factor(p),2.5,"Feruchemy integration toggle restores ordinary tapping");EnhancementConfig.FERUCHEMY.set(true);EnhancementConfig.OVERCAPS.set(false);PowerAttributes.set(p,"f:steel",10);PowerData.state(p,"f:steel",100);SteelFeruchemy.tick(p);close(h,SteelFeruchemy.factor(p),FeruchemyConfig.STEEL_CAP.get(),"Overcap toggle preserves the ordinary core multiplier ceiling");}finally{EnhancementConfig.FERUCHEMY.set(enabled);EnhancementConfig.OVERCAPS.set(caps);SteelFeruchemy.clear(p);EnhancementService.clear(p);}h.succeed();
    }
}
