package dev.cosmerearcanists.aondor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.*;

/** Air-only temporary light. Vanilla scheduled block ticks persist with unloaded chunks and saves. */
public final class AonLightBlock extends Block {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("cosmerearcanists");
    public static final DeferredBlock<AonLightBlock> LIGHT=BLOCKS.register("aon_light",()->new AonLightBlock());
    private AonLightBlock(){super(BlockBehaviour.Properties.of().replaceable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).noCollission().noOcclusion().noLootTable().lightLevel(s->s.getValue(BlockStateProperties.LEVEL)));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LEVEL,12));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BlockStateProperties.LEVEL);}
    // Keep this a real (non-air) state so empty chunk sections store it. Its geometry is still empty.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){level.removeBlock(pos,false);}
    public static boolean place(ServerLevel level,BlockPos pos,int brightness,int duration){
        if(duration<1||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.isInWorldBounds(pos)||!level.getBlockState(pos).isAir()||level.getBlockState(pos).is(LIGHT.get()))return false;
        if(!level.setBlock(pos,LIGHT.get().defaultBlockState().setValue(BlockStateProperties.LEVEL,Math.max(1,Math.min(15,brightness))),3))return false;
        level.scheduleTick(pos,LIGHT.get(),duration);return true;
    }
}
