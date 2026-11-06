package dev.cosmerearcanists.aondor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.*;

/** Air-only light: temporary invisible states and permanent visible motes share one registry ID. */
public final class AonLightBlock extends Block {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("cosmerearcanists");
    public static final DeferredBlock<AonLightBlock> LIGHT=BLOCKS.register("aon_light",()->new AonLightBlock());
    private AonLightBlock(){super(BlockBehaviour.Properties.of().replaceable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).noCollission().noOcclusion().noLootTable().lightLevel(s->s.getValue(BlockStateProperties.LEVEL)));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LEVEL,12).setValue(BlockStateProperties.PERSISTENT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BlockStateProperties.LEVEL,BlockStateProperties.PERSISTENT);}
    // Keep this a real (non-air) state so empty chunk sections store it. Its geometry is still empty.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return state.getValue(BlockStateProperties.PERSISTENT)?Block.box(7,7,7,9,9,9):net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override protected RenderShape getRenderShape(BlockState state){return state.getValue(BlockStateProperties.PERSISTENT)?RenderShape.MODEL:RenderShape.INVISIBLE;}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!state.getValue(BlockStateProperties.PERSISTENT))level.removeBlock(pos,false);}
    public static boolean place(ServerLevel level,BlockPos pos,int brightness,int duration){if(duration<1)return false;return store(level,pos,brightness,duration,false);}
    public static boolean placePermanent(ServerLevel level,BlockPos pos,int brightness){return store(level,pos,brightness,0,true);}
    private static boolean store(ServerLevel level,BlockPos pos,int brightness,int duration,boolean permanent){
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.isInWorldBounds(pos))return false;
        var before=level.getBlockState(pos);boolean upgrade=permanent&&before.is(LIGHT.get())&&!before.getValue(BlockStateProperties.PERSISTENT);
        if(!before.isAir()&&!upgrade)return false;
        var state=LIGHT.get().defaultBlockState().setValue(BlockStateProperties.LEVEL,Math.max(1,Math.min(15,brightness))).setValue(BlockStateProperties.PERSISTENT,permanent);
        if(!level.setBlock(pos,state,3))return false;
        if(!permanent)level.scheduleTick(pos,LIGHT.get(),duration);return true;
    }
}
