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
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BendalloyFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Subsumer"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:bendalloy",1);return p;}
    private static ItemStack mind(FakePlayer p){var s=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"bendalloy_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,s);return s;}
    private static void ticks(FakePlayer p,int count){for(int i=0;i<count;i++)BendalloyFeruchemy.tick(p);}
    private static void close(GameTestHelper h,double got,double expected,String message){h.assertTrue(Math.abs(got-expected)<1e-4,message+": "+got+" expected "+expected);}
    @GameTest(template="empty") public static void bendalloyConservesNutritionAndUsesSaturationBeforeWholeFood(GameTestHelper h){
        var p=player(h);var s=mind(p);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);PowerData.state(p,"f:bendalloy",-3);ticks(p,20);
        close(h,Metalminds.charge(s),.3,"One second stores30percent nutrition");close(h,p.getFoodData().getSaturationLevel(),4.7,"Saturation pays exact storage");h.assertTrue(p.getFoodData().getFoodLevel()==20,"Hunger remains untouched while saturation exists");
        p.getFoodData().setSaturation(0);ticks(p,66);h.assertTrue(p.getFoodData().getFoodLevel()==20,"Fractional hunger time grants no charge or food loss");ticks(p,1);h.assertTrue(p.getFoodData().getFoodLevel()==19,"Whole hunger point is taken only at its transfer time");close(h,Metalminds.charge(s),1.3,"Whole hunger is conserved in the same nutrition pool");BendalloyFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void bendalloyLowTapLevelsRestoreFoodAndSaturationSlowly(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"bendalloy",10);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);PowerData.state(p,"f:bendalloy",1);ticks(p,799);
        h.assertTrue(p.getFoodData().getFoodLevel()==10,"Tap1 cannot restore a food point before40seconds");close(h,Metalminds.charge(s),10,"Waiting never spends an unpaid fractional hunger point");ticks(p,1);h.assertTrue(p.getFoodData().getFoodLevel()==11,"Tap1 restores one food point after40seconds");close(h,Metalminds.charge(s),9,"One restored hunger point costs one stored point at low tap");
        p.getFoodData().setFoodLevel(20);PowerData.state(p,"f:bendalloy",2);ticks(p,200);close(h,p.getFoodData().getSaturationLevel(),1,"Tap2 restores one saturation point in10seconds");close(h,Metalminds.charge(s),8,"Saturation and hunger consume the same conserved pool");close(h,BendalloyFeruchemy.tapRate(3),.225,"Low tap curve is gradual");BendalloyFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void bendalloyHigherTappingUsesCompressionAndNeverSpendsAtFullNutrition(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"bendalloy",10);p.getFoodData().setFoodLevel(19);p.getFoodData().setSaturation(0);PowerData.state(p,"f:bendalloy",10);ticks(p,8);
        h.assertTrue(p.getFoodData().getFoodLevel()==20,"Tap10 restores a hunger point in0.4seconds");close(h,Metalminds.charge(s),10-Math.pow(2.5,.25),"Higher nutrition output pays compression loss");
        p.getFoodData().setSaturation(20);double before=Metalminds.charge(s);ticks(p,200);close(h,Metalminds.charge(s),before,"Full food and saturation spend no charge and bank no timer credit");p.getFoodData().setFoodLevel(19);p.getFoodData().setSaturation(0);ticks(p,1);h.assertTrue(p.getFoodData().getFoodLevel()==19,"Full nutrition cannot bank a later instant hunger refill");BendalloyFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void bendalloyCapacityDepletionFloorAndFeatureGatesPreserveData(GameTestHelper h){
        var p=player(h);var s=mind(p);Metalminds.transfer(s,p.getUUID(),"bendalloy",Metalminds.capacity(s)-.005);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(1);PowerData.state(p,"f:bendalloy",-3);ticks(p,1);
        close(h,Metalminds.charge(s),Metalminds.capacity(s),"Final saturation storage cannot exceed capacity");close(h,p.getFoodData().getSaturationLevel(),.995,"Only accepted nutrition is removed");h.assertTrue(PowerData.state(p,"f:bendalloy")==0,"Partial final storage tick stops");
        s=mind(p);p.getFoodData().setFoodLevel(FeruchemyConfig.BENDALLOY_FOOD_FLOOR.get());p.getFoodData().setSaturation(0);PowerData.state(p,"f:bendalloy",-3);ticks(p,200);close(h,Metalminds.charge(s),0,"Storage floor avoids starvation");
        Metalminds.transfer(s,p.getUUID(),"bendalloy",.0005);p.getFoodData().setFoodLevel(20);PowerData.state(p,"f:bendalloy",1);ticks(p,1);close(h,p.getFoodData().getSaturationLevel(),.0005,"Last fractional charge restores only funded saturation");h.assertTrue(!Metalminds.bound(s)&&PowerData.state(p,"f:bendalloy")==0,"Depleted pieces unkey and stop");
        Metalminds.transfer(s,p.getUUID(),"bendalloy",2);boolean enabled=FeruchemyConfig.BENDALLOY.get();try{FeruchemyConfig.BENDALLOY.set(false);PowerData.state(p,"f:bendalloy",10);ticks(p,20);close(h,Metalminds.charge(s),2,"Disabling preserves saved nutrition");h.assertTrue(PowerAttributes.strength(p,"f:bendalloy")==1&&SeekingService.activeStrength(p,"f:bendalloy")==0,"Disabling preserves power but removes its paid signal");}finally{FeruchemyConfig.BENDALLOY.set(enabled);BendalloyFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void bendalloyContactOwnershipAndLevelLimitsAreServerEnforced(GameTestHelper h){
        var p=player(h);var s=mind(p);PowerAttributes.set(p,"f:bendalloy",1.5);h.assertTrue(PowerData.storeLevels(p,"f:bendalloy")==5&&PowerData.tapLevels(p,"f:bendalloy")==15,"Fractional attributes bound both selectors");
        Metalminds.transfer(s,UUID.randomUUID(),"bendalloy",5);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);PowerData.state(p,"f:bendalloy",10);ticks(p,200);h.assertTrue(p.getFoodData().getFoodLevel()==10,"Foreign metalmind restores nothing");close(h,Metalminds.charge(s),5,"Foreign nutrition remains intact");
        s=mind(p);s.setCount(2);PowerData.state(p,"f:bendalloy",-3);p.getFoodData().setSaturation(5);ticks(p,20);close(h,Metalminds.charge(s),0,"Stacked items cannot receive nutrition");close(h,p.getFoodData().getSaturationLevel(),5,"No nutrition is removed without valid contact");
        s=mind(p);Metalminds.transfer(s,p.getUUID(),"bendalloy",5);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);PowerData.state(p,"f:bendalloy",1);ticks(p,799);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);ticks(p,1);p.setItemInHand(InteractionHand.OFF_HAND,s);PowerData.state(p,"f:bendalloy",1);ticks(p,1);h.assertTrue(p.getFoodData().getFoodLevel()==10,"Lost contact removes scheduling credit");close(h,Metalminds.charge(s),5,"No hidden nutrition is spent by lost contact");BendalloyFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void bendalloyCadmiumStorageAndNativeFoodHealingUseFundedRates(GameTestHelper h){
        var caster=player(h);PowerAttributes.set(caster,"a:cadmium",1);PowerData.state(caster,"a:cadmium",2);PowerData.reserve(caster,"cadmium",100);PowerData.assignSlot(caster,"a:cadmium",0);PowerService.control(caster,new PowerNetwork.Control(2,"",1));var p=player(h);var s=mind(p);boolean pvp=BubbleConfig.PVP.get();
        try{BubbleConfig.PVP.set(false);BubbleService.create(caster,"cadmium");p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);PowerData.state(p,"f:bendalloy",-3);ticks(p,20);close(h,Metalminds.charge(s),.6,"Other caster's cadmium doubles nutrition storage");close(h,p.getFoodData().getSaturationLevel(),4.7,"Cadmium preserves the selected storage penalty");}finally{BubbleConfig.PVP.set(pvp);BubbleService.clearPlayer(caster);BubbleService.clearPlayer(p);BendalloyFeruchemy.clear(p);}
        var low=player(h);var high=player(h);var lowMind=mind(low);var highMind=mind(high);Metalminds.transfer(lowMind,low.getUUID(),"bendalloy",100);Metalminds.transfer(highMind,high.getUUID(),"bendalloy",100);
        for(var user:java.util.List.of(low,high)){user.setHealth(10);user.getFoodData().setFoodLevel(20);user.getFoodData().setSaturation(0);}
        PowerData.state(low,"f:bendalloy",1);PowerData.state(high,"f:bendalloy",10);for(int i=0;i<200;i++){BendalloyFeruchemy.tick(low);BendalloyFeruchemy.tick(high);low.getFoodData().tick(low);high.getFoodData().tick(high);}
        h.assertTrue(low.getHealth()<11&&high.getHealth()>low.getHealth()+.5,"Native food healing stays weak at tap1 and becomes stronger at high output");BendalloyFeruchemy.clear(low);BendalloyFeruchemy.clear(high);h.succeed();
    }
}
