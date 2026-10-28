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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteelFluidGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"SteelRunner"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);PowerAttributes.set(p,"f:steel",2);var mind=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:steel_metalmind_ring")));Metalminds.transfer(mind,p.getUUID(),"steel",100);p.setItemInHand(InteractionHand.OFF_HAND,mind);return p;}
    private static BlockPos fluid(GameTestHelper h,Block block){var pos=h.absolutePos(new BlockPos(2,20,2));h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());return pos;}
    private static VoxelShape shape(GameTestHelper h,FakePlayer p,BlockPos pos){return h.getLevel().getBlockState(pos).getCollisionShape(h.getLevel(),pos,CollisionContext.of(p));}
    private static void tap(FakePlayer p,int level){PowerData.state(p,"f:steel",level);SteelFeruchemy.tick(p);p.setDeltaMovement(.25,0,0);}
    @GameTest(template="empty") public static void onlyPaidOutputStrictlyAboveTenSupportsWaterAndLava(GameTestHelper h){
        var p=player(h);for(var block:List.of(Blocks.WATER,Blocks.LAVA)){var pos=fluid(h,block);p.setPos(pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5);tap(p,10);h.assertTrue(shape(h,p,pos).isEmpty(),"Exactly tap10 cannot run on fluids");tap(p,11);h.assertTrue(!shape(h,p,pos).isEmpty(),"Tap11 supports exposed water and lava");
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(shape(h,p,pos).isEmpty(),"Contact loss removes support before another steel tick");p=player(h);}
        var pos=fluid(h,Blocks.WATER);p.setPos(pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5);var mind=p.getOffhandItem();Metalminds.transfer(mind,p.getUUID(),"steel",-10000);Metalminds.transfer(mind,p.getUUID(),"steel",.001);tap(p,11);h.assertTrue(shape(h,p,pos).isEmpty(),"Partial fuel below effective threshold gives no free support");h.succeed();
    }
    @GameTest(template="empty") public static void nativeMovementLandsAboveFluidAndDoesNotBlockOtherPlayers(GameTestHelper h){
        var p=player(h);for(var block:List.of(Blocks.WATER,Blocks.LAVA)){var pos=fluid(h,block);p.setPos(pos.getX()+.3,pos.getY()+1.1,pos.getZ()+.5);tap(p,11);p.setDeltaMovement(Vec3.ZERO);
            // Native server movement argument, with zero stored velocity, exercises the movement scope.
            p.move(MoverType.SELF,new Vec3(.2,-.4,0));double top=pos.getY()+h.getLevel().getFluidState(pos).getHeight(h.getLevel(),pos)+1.0/64;
            h.assertTrue(Math.abs(p.getY()-top)<1e-6&&p.getX()>pos.getX()+.49&&p.onGround(),"Native collision permits forward running and stops descent above the fluid");
            var other=player(h);other.setPos(p.position());other.setDeltaMovement(.2,0,0);h.assertTrue(shape(h,other,pos).isEmpty(),"Untapping players retain normal fluid collision");
        }h.succeed();
    }
    @GameTest(template="empty") public static void crouchingStoppingSubmersionAndGatesRemoveSupport(GameTestHelper h){
        var p=player(h);var pos=fluid(h,Blocks.WATER);p.setPos(pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5);tap(p,11);p.setShiftKeyDown(true);h.assertTrue(shape(h,p,pos).isEmpty(),"Crouch to sink");p.setShiftKeyDown(false);p.setDeltaMovement(Vec3.ZERO);h.assertTrue(shape(h,p,pos).isEmpty(),"Standing still is not running");p.setDeltaMovement(.2,0,0);p.setPos(pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5);h.assertTrue(shape(h,p,pos).isEmpty(),"Cannot rise out of fluid by enabling steel underwater");p.setPos(pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5);
        boolean old=FeruchemyConfig.STEEL_WATER_RUNNING.get();try{FeruchemyConfig.STEEL_WATER_RUNNING.set(false);h.assertTrue(shape(h,p,pos).isEmpty(),"Independent water toggle");}finally{FeruchemyConfig.STEEL_WATER_RUNNING.set(old);}
        h.getLevel().setBlockAndUpdate(pos.above(),Blocks.WATER.defaultBlockState());h.assertTrue(shape(h,p,pos).isEmpty(),"No submerged collision ledges");h.getLevel().setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());PowerData.state(p,"f:steel",0);h.assertTrue(shape(h,p,pos).isEmpty(),"Turning off removes support immediately");h.succeed();
    }
}
