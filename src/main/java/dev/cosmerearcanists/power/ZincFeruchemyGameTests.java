package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZincFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Sparker"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:zinc",1);return p;}
    private static ItemStack mind(FakePlayer p){var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"zinc_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,item);return item;}
    private static void close(GameTestHelper h,double value,double expected,String text){h.assertTrue(Math.abs(value-expected)<1e-6,text+": "+value+" expected "+expected);}
    private static double ratio(FakePlayer p,net.minecraft.core.Holder<Attribute> attribute){return p.getAttributeValue(attribute)/p.getAttribute(attribute).getBaseValue();}
    @GameTest(template="empty") public static void zincConservesMentalSpeedAndKeepsMovementBonusSlight(GameTestHelper h){
        var p=player(h);var item=mind(p);PowerData.state(p,"f:zinc",-3);for(int i=0;i<20;i++)ZincFeruchemy.tick(p);close(h,Metalminds.charge(item),.3,"Twenty ticks store30percent mental-speed seconds");close(h,ZincFeruchemy.factor(p),.7,"Storing slows reactions");close(h,ratio(p,Attributes.ATTACK_SPEED),.7,"Attack recovery follows storing penalty");close(h,ratio(p,Attributes.MOVEMENT_SPEED),1,"Storing mental speed does not reduce walking speed");
        Metalminds.transfer(item,p.getUUID(),"zinc",20);double before=Metalminds.charge(item);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);close(h,before-Metalminds.charge(item),1.5*Math.pow(1.5,.25)/20,"Fast tapping pays compression");close(h,ratio(p,Attributes.ATTACK_SPEED),1.5,"Tap10 gives modest action recovery");close(h,ratio(p,Attributes.MOVEMENT_SPEED),1.02,"Tap10 gives only2percent movement");
        PowerAttributes.set(p,"f:zinc",10);PowerData.state(p,"f:zinc",100);ZincFeruchemy.tick(p);close(h,ratio(p,Attributes.MOVEMENT_SPEED),1.05,"Movement bonus caps at5percent");close(h,ratio(p,Attributes.ATTACK_SPEED),2,"Action bonus has an independent cap");ZincFeruchemy.clear(p);h.succeed();
    }
    private static int preparationTicks(FakePlayer p,ItemStack weapon){var service=new ZincFeruchemy();service.startUse(new LivingEntityUseItemEvent.Start(p,weapon,100));int remaining=100,ticks=0;while(100-remaining<20&&ticks<100){var event=new LivingEntityUseItemEvent.Tick(p,weapon,remaining);service.weaponUse(event);remaining=event.getDuration()-1;ticks++;}return ticks;}
    @GameTest(template="empty") public static void zincChangesBowAndCrossbowPreparationWithoutChangingFoodOrShields(GameTestHelper h){
        var p=player(h);var item=mind(p);PowerData.state(p,"f:zinc",-3);ZincFeruchemy.tick(p);h.assertTrue(preparationTicks(p,new ItemStack(Items.BOW))==29&&preparationTicks(p,new ItemStack(Items.CROSSBOW))==29,"Storing makes20ticks of bow and crossbow preparation take29ticks");Metalminds.transfer(item,p.getUUID(),"zinc",10);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);h.assertTrue(preparationTicks(p,new ItemStack(Items.BOW))==14&&preparationTicks(p,new ItemStack(Items.CROSSBOW))==14,"Tap10 prepares weapons in14ticks instead of20");
        var service=new ZincFeruchemy();for(var stack:List.of(new ItemStack(Items.APPLE),new ItemStack(Items.SHIELD),new ItemStack(Items.TRIDENT))){var e=new LivingEntityUseItemEvent.Tick(p,stack,100);service.weaponUse(e);h.assertTrue(e.getDuration()==100,"Only bow/crossbow preparations use zinc's clock");}PowerData.state(p,"f:zinc",0);ZincFeruchemy.tick(p);h.assertTrue(preparationTicks(p,new ItemStack(Items.BOW))==20,"No preparation time is banked after turning off");ZincFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void zincPartialFuelOwnershipContactAndConfigurationBoundEveryBenefit(GameTestHelper h){
        var p=player(h);var item=mind(p);Metalminds.transfer(item,p.getUUID(),"zinc",.001);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);h.assertTrue(ratio(p,Attributes.MOVEMENT_SPEED)>1&&ratio(p,Attributes.MOVEMENT_SPEED)<1.02&&ZincFeruchemy.fundedLevels(p)<1,"Final partial fuel grants only proportional benefits, below reaction threshold");ZincFeruchemy.tick(p);close(h,ratio(p,Attributes.MOVEMENT_SPEED),1,"Unfunded tapping clears movement");
        Metalminds.transfer(item,UUID.randomUUID(),"zinc",10);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);close(h,ZincFeruchemy.factor(p),1,"Foreign keyed charge gives no benefits");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);close(h,ratio(p,Attributes.ATTACK_SPEED),1,"No physical contact grants no recovery bonus");
        item=mind(p);Metalminds.transfer(item,p.getUUID(),"zinc",10);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);boolean enabled=FeruchemyConfig.ZINC.get(),movement=FeruchemyConfig.ZINC_MOVEMENT.get();try{FeruchemyConfig.ZINC_MOVEMENT.set(false);ZincFeruchemy.tick(p);close(h,ratio(p,Attributes.MOVEMENT_SPEED),1,"Movement toggle preserves mental benefits");FeruchemyConfig.ZINC.set(false);ZincFeruchemy.tick(p);close(h,ZincFeruchemy.factor(p),1,"Disabling zinc clears mental effects");h.assertTrue(Metalminds.charge(item)>0,"Disabling preserves saved reserves");}finally{FeruchemyConfig.ZINC.set(enabled);FeruchemyConfig.ZINC_MOVEMENT.set(movement);ZincFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void zincProjectileCuesAndPathsRespectThresholdsVisibilityAndOwnership(GameTestHelper h){
        var p=player(h);var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setPos(p.getBoundingBox().getCenter().add(4,0,0));arrow.setDeltaMovement(-1,0,0);h.getLevel().addFreshEntity(arrow);var before=arrow.position();var low=ZincReactions.threats(p,1);h.assertTrue(low.size()==1&&low.getFirst().path().isEmpty()&&low.getFirst().direction().x>.9,"Low tapping gives a correctly directed cue without a path");var high=ZincReactions.threats(p,10);h.assertTrue(high.size()==1&&high.getFirst().path().size()>1&&high.getFirst().path().size()<=17,"High tapping gives a short bounded path");
        h.assertTrue(arrow.position().equals(before)&&arrow.getDeltaMovement().equals(new Vec3(-1,0,0)),"Prediction never moves or accelerates the projectile");arrow.setOwner(p);h.assertTrue(ZincReactions.threats(p,10).isEmpty(),"Own arrows never warn");arrow.setOwner(null);arrow.setDeltaMovement(1,0,0);h.assertTrue(ZincReactions.threats(p,10).isEmpty(),"Receding arrows never warn");arrow.setDeltaMovement(-1,0,0);arrow.setInvisible(true);h.assertTrue(ZincReactions.threats(p,10).isEmpty(),"Invisible projectiles are not revealed");arrow.setInvisible(false);
        var wall=p.blockPosition().offset(2,0,0);h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(wall.above(),Blocks.STONE.defaultBlockState());h.assertTrue(ZincReactions.threats(p,10).isEmpty(),"Opaque walls block reaction perception");h.getLevel().removeBlock(wall,false);h.getLevel().removeBlock(wall.above(),false);arrow.discard();h.succeed();
    }
    @GameTest(template="empty") public static void zincComposesWithSteelAndCleansOnlyItsOwnModifiers(GameTestHelper h){
        var p=player(h);var item=mind(p);Metalminds.transfer(item,p.getUUID(),"zinc",100);var steel=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"steel_metalmind_ring")));Metalminds.transfer(steel,p.getUUID(),"steel",100);p.setItemInHand(InteractionHand.MAIN_HAND,steel);PowerAttributes.set(p,"f:steel",1);PowerData.state(p,"f:steel",10);SteelFeruchemy.tick(p);PowerData.state(p,"f:zinc",10);ZincFeruchemy.tick(p);close(h,ratio(p,Attributes.MOVEMENT_SPEED),2.5*1.02,"Slight zinc speed composes with physical steel speed");close(h,ratio(p,Attributes.ATTACK_SPEED),2.5*1.5,"Independent attack modifiers compose");ZincFeruchemy.clear(p);close(h,ratio(p,Attributes.MOVEMENT_SPEED),2.5,"Zinc cleanup preserves steel movement");close(h,ratio(p,Attributes.ATTACK_SPEED),2.5,"Zinc cleanup preserves steel attacks");PowerAttributes.set(p,"f:zinc",1.5);h.assertTrue(ZincFeruchemy.storeLevels(p)==5&&ZincFeruchemy.tapLevels(p)==15,"Fractional strength controls zinc limits");SteelFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void zincPacketsRoundTripAndRejectInvalidOrUnboundedPaths(GameTestHelper h){
        var threat=new ZincReactionPacket.Threat(new Vec3(1,0,0),List.of(new Vec3(1,20,1),new Vec3(0,20,1)));var packet=new ZincReactionPacket("minecraft:overworld",6,List.of(threat));var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{ZincReactionPacket.CODEC.encode(buffer,packet);h.assertTrue(ZincReactionPacket.CODEC.decode(buffer).equals(packet),"Private reaction codec preserves bounded directions and paths");}finally{buffer.release();}
        boolean rejected=false;try{new ZincReactionPacket.Threat(new Vec3(1,0,0),Collections.nCopies(18,Vec3.ZERO));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Path budget enforced");rejected=false;try{new ZincReactionPacket.Threat(new Vec3(Double.NaN,0,0),List.of());}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Nonfinite directions rejected");rejected=false;try{new ZincReactionPacket("",6,Collections.nCopies(9,threat));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Threat budget enforced");h.succeed();
    }
}
