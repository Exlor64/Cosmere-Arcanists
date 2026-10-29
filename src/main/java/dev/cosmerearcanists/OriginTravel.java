package dev.cosmerearcanists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Origin relocation happens once, when the command is accepted, never on login. */
public final class OriginTravel {
    public static ServerLevel scadrial(ServerPlayer player) {
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse(ScadrialWorld.ID)));
    }

    public static BlockPos findSpawn(ServerLevel level) {
        var center = level.getSharedSpawnPos();
        // Bounded outward search for a solid, dry surface with two air blocks above.
        // Destination generation is normal teleport preparation; no forced chunk tickets.
        for (int radius = 0; radius <= 64; radius += 8) {
            for (int dx = -radius; dx <= radius; dx += 8) {
                for (int dz = -radius; dz <= radius; dz += 8) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(center.getX() + dx, 0, center.getZ() + dz));
                    if (safe(level, pos)) return pos;
                }
            }
        }
        return null;
    }

    public static boolean safe(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinBuildHeight()
            || pos.getY() + 1 >= level.getMaxBuildHeight()) return false;
        var floor = level.getBlockState(pos.below());
        return floor.isFaceSturdy(level, pos.below(), Direction.UP)
            && floor.getFluidState().isEmpty()
            && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS)
            && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE)
            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    public static void arrive(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.stopRiding();
        player.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5,
            java.util.Set.of(), player.getYRot(), player.getXRot());
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.fallDistance = 0;
        // Forced respawn points support ordinary ground without requiring a bed.
        player.setRespawnPosition(level.dimension(), pos, player.getYRot(), true, false);
        dev.cosmerearcanists.hemalurgy.ScadrialVisits.observe(player);
    }

    private OriginTravel() {}
}
