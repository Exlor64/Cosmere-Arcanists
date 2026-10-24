package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.copper.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AluminumFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Trueself"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:aluminum",1);return p;}
    private static ItemStack mind(String metal){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_metalmind_ring")));}
    private static ItemStack suppress(FakePlayer p){var aluminum=mind("aluminum");p.setItemInHand(InteractionHand.OFF_HAND,aluminum);PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);return aluminum;}
    @GameTest(template="empty") public static void fullyStoredIdentityMakesNewChargeSharedButDoesNotGrantPowers(GameTestHelper h){
        var p=player(h);var aluminum=suppress(p);var iron=mind("iron");p.setItemInHand(InteractionHand.MAIN_HAND,iron);PowerAttributes.set(p,"f:iron",1);PowerData.state(p,"f:iron",-3);IronFeruchemy.tick(p);
        h.assertTrue(p.getAttributeValue(PowerAttributes.IDENTITY)==0&&Metalminds.owner(aluminum).equals(p.getUUID())&&!Metalminds.unkeyed(aluminum),"Identity suppression is fully funded into a keyed aluminum mind");
        h.assertTrue(Metalminds.unkeyed(iron)&&Metalminds.owner(iron)==null&&Math.abs(Metalminds.charge(iron)-.015)<1e-7,"New iron charge is explicitly ownerless only at full suppression");
        var recipient=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);recipient.setItemInHand(InteractionHand.MAIN_HAND,iron);PowerData.state(recipient,"f:iron",1);double before=Metalminds.charge(iron);IronFeruchemy.tick(recipient);h.assertTrue(Metalminds.charge(iron)==before,"Shared charge does not grant a missing Feruchemical power");
        PowerAttributes.set(recipient,"f:iron",1);PowerData.state(recipient,"f:iron",1);IronFeruchemy.tick(recipient);h.assertTrue(Metalminds.charge(iron)<before,"A different Iron Ferring can tap shared iron");AluminumFeruchemy.clear(p);AluminumFeruchemy.clear(recipient);h.succeed();
    }
    @GameTest(template="empty") public static void partialIdentityAndLostFundingNeverAuthorizeUnkeyedStorage(GameTestHelper h){
        var p=player(h);var aluminum=mind("aluminum");p.setItemInHand(InteractionHand.OFF_HAND,aluminum);PowerData.state(p,"f:aluminum",-9);AluminumFeruchemy.tick(p);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p)&&Math.abs(p.getAttributeValue(PowerAttributes.IDENTITY)-.1)<1e-7,"Ninety percent is insufficient");
        PowerAttributes.set(p,"f:aluminum",.5);PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p)&&AluminumFeruchemy.storeLevels(p)==5,"Fractional power limits full suppression");
        PowerAttributes.set(p,"f:aluminum",1);PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);h.assertTrue(AluminumFeruchemy.unkeyedStorage(p),"Full paid suppression enables neutral storage");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p),"Lost aluminum contact revokes authorization within the same tick");
        p.setItemInHand(InteractionHand.OFF_HAND,aluminum);PowerData.state(p,"f:aluminum",0);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p),"Changing mode revokes the previous paid output");
        Metalminds.transfer(aluminum,p.getUUID(),"aluminum",10000);Metalminds.transfer(aluminum,p.getUUID(),"aluminum",-.001);PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p)&&PowerData.state(p,"f:aluminum")==0,"Nearly full aluminum cannot claim complete Identity suppression");AluminumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void existingBindingsNeverConvertOrMixAndEmptyPiecesCanBeReused(GameTestHelper h){
        var p=player(h);var owned=mind("iron");Metalminds.transfer(owned,p.getUUID(),"iron",1);p.setItemInHand(InteractionHand.MAIN_HAND,owned);suppress(p);h.assertTrue(Metalminds.transferAccessible(p,"iron",1)==0&&Metalminds.charge(owned)==1&&p.getUUID().equals(Metalminds.owner(owned)),"Full suppression cannot convert or top up existing owned reserves");
        var shared=mind("iron");p.setItemInHand(InteractionHand.MAIN_HAND,shared);Metalminds.transferAccessible(p,"iron",1);h.assertTrue(Metalminds.unkeyed(shared),"Fresh shared reserve created");var copy=shared.copy();h.assertTrue(Metalminds.unkeyed(copy)&&Metalminds.usable(copy,UUID.randomUUID(),"iron"),"Item serialization preserves explicit shared binding");
        PowerData.state(p,"f:aluminum",0);AluminumFeruchemy.tick(p);h.assertTrue(Metalminds.transferAccessible(p,"iron",1)==0&&Metalminds.charge(shared)==1,"Normal Identity cannot mix keyed deposits into unkeyed reserves");
        Metalminds.transfer(shared,p.getUUID(),"iron",-1);h.assertTrue(!Metalminds.unkeyed(shared)&&!Metalminds.bound(shared),"Complete drain removes shared binding");Metalminds.transferAccessible(p,"iron",1);h.assertTrue(p.getUUID().equals(Metalminds.owner(shared))&&!Metalminds.unkeyed(shared),"An empty piece can be reused for a fresh keyed reserve");AluminumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void sharedCopperRequiresCopperPowerAndStillErasesOnDrain(GameTestHelper h){
        var p=player(h);suppress(p);PowerAttributes.set(p,"f:copper",1);var copper=mind("copper");p.setItemInHand(InteractionHand.MAIN_HAND,copper);PowerData.state(p,"f:copper",-1);var session=CopperService.open(p,false);h.assertTrue(session!=null&&Metalminds.unkeyed(copper),"Full Identity suppression creates a shared notebook");
        CopperService.request(p,new CopperNetwork.Request(CopperNetwork.SAVE,session.token,CopperNetwork.ZERO,"Shared route","Meet beside the river",0,new byte[0]));var book=session.book;h.assertTrue(CopperBooks.get(p.server).books.get(book).size()==1,"Shared notes saved server-side");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);CopperService.clear(p);var recipient=player(h);recipient.setItemInHand(InteractionHand.MAIN_HAND,copper);PowerData.state(recipient,"f:copper",1);h.assertTrue(CopperService.open(recipient,false)==null,"An unkeyed notebook does not grant copper Feruchemy");PowerAttributes.set(recipient,"f:copper",1);PowerData.state(recipient,"f:copper",1);h.assertTrue(CopperService.open(recipient,false)!=null,"A Copper Ferring can read another player's unkeyed notebook");
        PowerData.state(recipient,"f:copper",-1);h.assertTrue(CopperService.open(recipient,false)==null,"Editing shared memories requires continued full Identity suppression");Metalminds.transfer(copper,recipient.getUUID(),"copper",-1);h.assertTrue(!CopperBooks.get(p.server).books.containsKey(book)&&CopperService.identity(copper).equals(CopperNetwork.ZERO),"Final shared copper drain erases world memories and item identity");CopperService.clear(recipient);AluminumFeruchemy.clear(p);AluminumFeruchemy.clear(recipient);h.succeed();
    }
    @GameTest(template="empty") public static void cancelledHealingRefundRestoresExhaustedUnkeyedGoldBinding(GameTestHelper h){
        var p=player(h);var gold=mind("gold");Metalminds.transferWithBinding(gold,null,"gold",.001,true);p.setItemInHand(InteractionHand.MAIN_HAND,gold);PowerAttributes.set(p,"f:gold",1);PowerData.state(p,"f:gold",10);p.setHealth(10);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);try{GoldFeruchemy.tick(p);h.assertTrue(Math.abs(Metalminds.charge(gold)-.001)<1e-8&&Metalminds.unkeyed(gold)&&Metalminds.owner(gold)==null&&p.getHealth()==10,"Cancelled healing restores exact charge and neutral binding after exhausting the piece");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}AluminumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void identityTappingAndFeatureGatesPreserveOtherAttributes(GameTestHelper h){
        var p=player(h);var aluminum=suppress(p);h.assertTrue(PowerData.storeLevels(p,"f:aluminum")==10&&FeruchemyLevels.maximumStored(100,.3,.95)==.95,"Identity alone can reach full storage");PowerData.state(p,"f:aluminum",1);AluminumFeruchemy.tick(p);h.assertTrue(p.getAttributeValue(PowerAttributes.IDENTITY)>1&&!AluminumFeruchemy.unkeyedStorage(p),"Tapping consumes Identity charge and never enables shared storage");
        PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);boolean enabled=FeruchemyConfig.ALUMINUM.get();try{FeruchemyConfig.ALUMINUM.set(false);h.assertTrue(!AluminumFeruchemy.unkeyedStorage(p),"Config disables authorization immediately");AluminumFeruchemy.tick(p);h.assertTrue(p.getAttributeValue(PowerAttributes.IDENTITY)==1&&Metalminds.charge(aluminum)>0,"Disabled effect cleans transient Identity while preserving reserve");}finally{FeruchemyConfig.ALUMINUM.set(enabled);AluminumFeruchemy.clear(p);}h.succeed();
    }
    private static FakePlayer emotionalCaster(GameTestHelper h){var p=player(h);for(String metal:new String[]{"zinc","brass"}){PowerAttributes.set(p,"a:"+metal,1);PowerData.state(p,"a:"+metal,2);PowerData.reserve(p,metal,100);}return p;}
    private static ItemStack tapIdentity(FakePlayer p,int level){var aluminum=mind("aluminum");Metalminds.transfer(aluminum,p.getUUID(),"aluminum",10);p.setItemInHand(InteractionHand.OFF_HAND,aluminum);PowerData.state(p,"f:aluminum",level);AluminumFeruchemy.tick(p);return aluminum;}
    @GameTest(template="empty") public static void aluminumAttenuatesRiotingAndSoothingIncludingExistingInfluences(GameTestHelper h){
        var source=emotionalCaster(h);var target=player(h);target.setPos(source.position());PowerAttributes.set(source,"a:brass",.4);
        EmotionService.apply(source,target,"a:zinc",Emotion.ANGER);EmotionService.apply(source,target,"a:brass",Emotion.ANGER);double baseline=EmotionService.intensity(target);h.assertTrue(baseline>0,"Opposing Rioting and Soothing leave measurable anger");
        tapIdentity(target,10);h.assertTrue(Math.abs(AluminumFeruchemy.emotionalResistance(target)-.5)<1e-8&&Math.abs(EmotionService.intensity(target)-baseline*.5)<1e-8,"Funded tap10 halves already lingering Rioting and Soothing without another cast");
        PowerData.state(target,"f:aluminum",2);AluminumFeruchemy.tick(target);h.assertTrue(Math.abs(EmotionService.intensity(target)-baseline*.9)<1e-8,"Lower tapping gives proportionally less resistance");
        PowerAttributes.set(target,"f:aluminum",2);PowerData.state(target,"f:aluminum",20);AluminumFeruchemy.tick(target);h.assertTrue(Math.abs(EmotionService.intensity(target)-baseline*.25)<1e-8&&Math.abs(EmotionService.intensity(target)-baseline*.25)<1e-8,"High tapping respects75percent cap and repeated refresh never applies resistance twice");
        PowerData.state(target,"f:aluminum",0);AluminumFeruchemy.tick(target);h.assertTrue(Math.abs(EmotionService.intensity(target)-baseline)<1e-8,"Stopping tapping restores normal remaining influence without amplifying it");EmotionService.clear(target);AluminumFeruchemy.clear(target);h.succeed();
    }
    @GameTest(template="empty") public static void emotionalResistanceRequiresFundedTappingContactAndConfiguration(GameTestHelper h){
        var p=player(h);var aluminum=tapIdentity(p,10);h.assertTrue(AluminumFeruchemy.emotionalResistance(p)==.5,"Paid tapping provides resistance");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(AluminumFeruchemy.emotionalResistance(p)==0,"Loss of physical contact immediately removes resistance");p.setItemInHand(InteractionHand.OFF_HAND,aluminum);
        boolean enabled=FeruchemyConfig.ALUMINUM_EMOTIONS.get();double rate=FeruchemyConfig.ALUMINUM_EMOTION_STEP.get(),cap=FeruchemyConfig.ALUMINUM_EMOTION_CAP.get();try{FeruchemyConfig.ALUMINUM_EMOTIONS.set(false);h.assertTrue(AluminumFeruchemy.emotionalResistance(p)==0,"Independent bonus toggle takes effect immediately");FeruchemyConfig.ALUMINUM_EMOTIONS.set(true);FeruchemyConfig.ALUMINUM_EMOTION_STEP.set(.02);FeruchemyConfig.ALUMINUM_EMOTION_CAP.set(.15);h.assertTrue(Math.abs(AluminumFeruchemy.emotionalResistance(p)-.15)<1e-8,"Rate and cap are server configurable");}finally{FeruchemyConfig.ALUMINUM_EMOTIONS.set(enabled);FeruchemyConfig.ALUMINUM_EMOTION_STEP.set(rate);FeruchemyConfig.ALUMINUM_EMOTION_CAP.set(cap);}
        Metalminds.transfer(aluminum,p.getUUID(),"aluminum",-10000);AluminumFeruchemy.tick(p);h.assertTrue(AluminumFeruchemy.emotionalResistance(p)==0,"An unfunded tap selection offers no resistance");PowerData.state(p,"f:aluminum",-10);AluminumFeruchemy.tick(p);h.assertTrue(AluminumFeruchemy.emotionalResistance(p)==0,"Storing Identity never supplies emotional resistance");AluminumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void aluminumResistsNewAndExistingDominationWithoutChangingCopperPiercing(GameTestHelper h){
        var source=emotionalCaster(h);var target=player(h);target.setPos(source.position());PowerAttributes.set(source,"a:zinc",4);SpiritualIntegrity.set(target,.25);tapIdentity(target,10);
        EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==null,"Halved compliance cannot reach the domination threshold");PowerAttributes.set(source,"a:zinc",8);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==source,"A stronger attacker can overcome resistance without double attenuation");DominationService.remove(target);PowerAttributes.set(source,"a:zinc",4);
        PowerData.state(target,"f:aluminum",0);AluminumFeruchemy.tick(target);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==source,"The same attacker can dominate without resistance");PowerData.state(target,"f:aluminum",10);AluminumFeruchemy.tick(target);DominationService.tick();h.assertTrue(DominationService.owner(target)==null,"Funded Identity tapping breaks a bond whose resisted strength falls below threshold");
        PowerAttributes.set(target,"a:copper",3.2);PowerData.reserve(target,"copper",100);PowerData.state(target,"a:copper",2);h.assertTrue(EmotionService.apply(source,target,"a:zinc",Emotion.ANGER),"Exactly25percent stronger still pierces copper before Identity attenuation");PowerAttributes.set(target,"a:copper",3.200001);h.assertTrue(!EmotionService.apply(source,target,"a:zinc",Emotion.ANGER),"Aluminum does not alter copper's independent strength contest");
        DominationService.remove(target);EmotionService.clear(target);AluminumFeruchemy.clear(target);h.succeed();
    }

}
