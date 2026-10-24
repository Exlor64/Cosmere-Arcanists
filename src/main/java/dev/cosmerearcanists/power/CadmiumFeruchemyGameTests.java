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
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CadmiumFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Gasper"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:cadmium",1);return p;}
    private static ItemStack mind(FakePlayer p){var s=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"cadmium_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,s);return s;}
    private static void ticks(FakePlayer p,int count){for(int i=0;i<count;i++)CadmiumFeruchemy.tick(p);}
    private static void close(GameTestHelper h,double got,double expected,String message){h.assertTrue(Math.abs(got-expected)<1e-5,message+": "+got+" expected "+expected);}
    private static void tap(FakePlayer p,int level){PowerData.state(p,"f:cadmium",level);CadmiumFeruchemy.tick(p);}
    @GameTest(template="empty") public static void cadmiumStoresActualAirAndSuppressesNativeRefillUntilFloor(GameTestHelper h){
        var p=player(h);var s=mind(p);p.setAirSupply(100);PowerData.state(p,"f:cadmium",-3);
        for(int i=0;i<20;i++){p.baseTick();CadmiumFeruchemy.tick(p);}h.assertTrue(p.getAirSupply()==94,"Native ambient refill cannot erase six funded stored air ticks");close(h,Metalminds.charge(s),.3,"Twenty native air ticks equal one stored breath-second");
        p.setAirSupply(31);for(int i=0;i<4;i++){p.baseTick();CadmiumFeruchemy.tick(p);}h.assertTrue(PowerData.state(p,"f:cadmium")==0,"Reaching the storage floor stops rather than cycling refill into free breath");h.assertTrue(p.getAirSupply()>=30,"Storage itself preserves the configured minimum air");int before=p.getAirSupply();p.baseTick();h.assertTrue(p.getAirSupply()>before,"Native ambient refill resumes after storage stops");CadmiumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void cadmiumBreathingThresholdOffsetsNativeUnderwaterAirUse(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"cadmium",100);var head=BlockPos.containing(p.getEyePosition());h.getLevel().setBlockAndUpdate(head,Blocks.WATER.defaultBlockState());h.getLevel().setBlockAndUpdate(head.below(),Blocks.WATER.defaultBlockState());p.setAirSupply(100);PowerData.state(p,"f:cadmium",5);
        try{for(int i=0;i<100;i++){p.baseTick();CadmiumFeruchemy.tick(p);}h.assertTrue(p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"Fixture is actually submerged");h.assertTrue(p.getAirSupply()==100,"Tap5 offsets normal native air consumption exactly");close(h,Metalminds.charge(s),95,"Sustained breathing pays one breath-second per real second");
            PowerData.state(p,"f:cadmium",1);for(int i=0;i<100;i++){p.baseTick();CadmiumFeruchemy.tick(p);}h.assertTrue(p.getAirSupply()==20,"Low tapping extends a dive without sustaining it");close(h,CadmiumFeruchemy.excess(p),0,"Below threshold grants no extra perks");
        }finally{h.getLevel().setBlockAndUpdate(head,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(head.below(),Blocks.AIR.defaultBlockState());CadmiumFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void cadmiumExcessOutputGrantsBoundedBonusesAndRespectsAquaAffinityScale(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"cadmium",100);double movement=p.getAttributeValue(Attributes.MOVEMENT_SPEED),swim=p.getAttributeValue(NeoForgeMod.SWIM_SPEED);
        tap(p,5);close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),movement,"Threshold grants no movement bonus");close(h,p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED),.2,"Threshold retains normal submerged mining penalty");
        tap(p,10);close(h,p.getAttributeValue(NeoForgeMod.SWIM_SPEED),swim*1.15,"Five excess levels give15percent swimming");close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),movement*1.01,"Walking gets only a slight1percent bonus");close(h,p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED),1,"Five excess levels remove the submerged penalty");close(h,CadmiumFeruchemy.naturalHealing(p,1),1.025,"Food healing gets only2.5percent at tap10");close(h,CadmiumFeruchemy.activityExhaustion(p,1),.75,"Tap10 reduces only activity exhaustion by25percent");
        var mining=p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);PowerData.state(p,"f:cadmium",0);CadmiumFeruchemy.clear(p);var external=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"cadmium_test_affinity");mining.addTransientModifier(new AttributeModifier(external,4,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));tap(p,10);close(h,mining.getValue(),1,"Existing Aqua-Affinity-like multiplier cannot stack cadmium into super mining");
        PowerAttributes.set(p,"f:cadmium",10);tap(p,100);close(h,CadmiumFeruchemy.activityExhaustion(p,1),.25,"High output caps endurance at75percent");h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED)<=movement*1.05+1e-8&&CadmiumFeruchemy.naturalHealing(p,1)<=1.100001,"Movement and food healing stay under slight caps");CadmiumFeruchemy.clear(p);close(h,mining.getValue(),1,"Cleanup preserves foreign mining modifiers");h.succeed();
    }
    @GameTest(template="empty") public static void cadmiumNativeLocomotionGetsEnduranceButHealingExhaustionDoesNot(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"cadmium",100);tap(p,10);p.getFoodData().setExhaustion(0);p.setOnGround(true);p.setSprinting(true);p.checkMovementStatistics(1,0,0);close(h,p.getFoodData().getExhaustionLevel(),.075,"Actual sprint statistics apply25percent endurance");
        p.getFoodData().setExhaustion(0);p.jumpFromGround();close(h,p.getFoodData().getExhaustionLevel(),.15,"Actual sprint jump applies activity endurance");p.getFoodData().setExhaustion(0);p.causeFoodExhaustion(1);close(h,p.getFoodData().getExhaustionLevel(),1,"Unrelated exhaustion is unchanged");
        p.getFoodData().setExhaustion(0);p.setHealth(10);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(6);for(int i=0;i<10;i++)p.getFoodData().tick(p);close(h,p.getHealth(),11.025,"Actual native saturation healing gets only the slight2.5percent bonus");close(h,p.getFoodData().getExhaustionLevel(),6,"Native regeneration exhaustion is not reduced by cadmium endurance");CadmiumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void cadmiumPartialFuelConfigAndLostContactRemovePerks(GameTestHelper h){
        var p=player(h);var s=mind(p);double movement=p.getAttributeValue(Attributes.MOVEMENT_SPEED);Metalminds.transfer(s,p.getUUID(),"cadmium",.001);p.setAirSupply(100);tap(p,10);h.assertTrue(p.getAirSupply()==100&&PowerData.state(p,"f:cadmium")==0,"Partial final output cannot grant a full air tick or excess perks");close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),movement,"Depletion removes movement modifier immediately");
        Metalminds.transfer(s,p.getUUID(),"cadmium",10);tap(p,10);boolean enabled=FeruchemyConfig.CADMIUM.get();try{FeruchemyConfig.CADMIUM.set(false);ticks(p,1);close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),movement,"Disabling removes attributes");h.assertTrue(PowerAttributes.strength(p,"f:cadmium")==1&&Metalminds.charge(s)>9,"Disabling preserves powers and saved charge");}finally{FeruchemyConfig.CADMIUM.set(enabled);}
        tap(p,10);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);ticks(p,1);close(h,p.getAttributeValue(NeoForgeMod.SWIM_SPEED),1,"Lost contact clears swimming immediately");close(h,CadmiumFeruchemy.excess(p),0,"Lost contact clears paid output");CadmiumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void cadmiumStorageRespectsCapacityOwnershipAndOtherCastersTimeBonus(GameTestHelper h){
        var caster=player(h);PowerAttributes.set(caster,"a:cadmium",1);PowerData.state(caster,"a:cadmium",2);PowerData.reserve(caster,"cadmium",100);PowerData.assignSlot(caster,"a:cadmium",0);PowerService.control(caster,new PowerNetwork.Control(2,"",1));var p=player(h);var s=mind(p);boolean pvp=BubbleConfig.PVP.get();
        try{BubbleConfig.PVP.set(false);BubbleService.create(caster,"cadmium");p.setAirSupply(100);PowerData.state(p,"f:cadmium",-3);ticks(p,20);h.assertTrue(p.getAirSupply()==94,"Accelerated storage preserves selected air debit");close(h,Metalminds.charge(s),.6,"Another caster's cadmium doubles breath storage");
            CadmiumFeruchemy.clear(p);s=mind(p);Metalminds.transfer(s,UUID.randomUUID(),"cadmium",5);p.setAirSupply(100);tap(p,10);h.assertTrue(p.getAirSupply()==100&&PowerData.state(p,"f:cadmium")==0,"Foreign breath cannot fund oxygen or perks");
            s=mind(p);Metalminds.transfer(s,p.getUUID(),"cadmium",Metalminds.capacity(s)-.01);PowerData.state(p,"f:cadmium",-3);ticks(p,20);h.assertTrue(p.getAirSupply()==100,"Insufficient capacity for a full native air tick removes no breath");PowerAttributes.set(p,"f:cadmium",1.5);h.assertTrue(PowerData.storeLevels(p,"f:cadmium")==5&&PowerData.tapLevels(p,"f:cadmium")==15,"Fractional attributes bound both selectors");
        }finally{BubbleConfig.PVP.set(pvp);BubbleService.clearPlayer(caster);BubbleService.clearPlayer(p);CadmiumFeruchemy.clear(p);}h.succeed();
    }
}
