package dev.cosmerearcanists;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OriginTravelGameTests {
    private static FakePlayer player(GameTestHelper h) {
        return new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "OriginTraveler"));
    }

    @GameTest(template = "empty")
    public static void arrivalSafetyRejectsFluidsHazardsAndObstructions(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        var level = h.getLevel();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.assertTrue(OriginTravel.safe(level, pos), "Dry solid ground with clearance is safe");
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.WATER, Blocks.LAVA,
            Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE}) {
            level.setBlockAndUpdate(pos.below(), block.defaultBlockState());
            h.assertTrue(!OriginTravel.safe(level, pos), "Hazardous/fluid floor must be rejected: " + block);
        }
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(!OriginTravel.safe(level, pos), "Head obstruction must be rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void arrivalSetsPersistentGroundRespawnAndClearsFall(GameTestHelper h) {
        var p = player(h);
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        p.fallDistance = 80;
        p.setDeltaMovement(new Vec3(0, -2, 0));
        OriginTravel.arrive(p, h.getLevel(), pos);
        h.assertTrue(p.getRespawnDimension().equals(h.getLevel().dimension())
            && pos.equals(p.getRespawnPosition()) && p.isRespawnForced(),
            "Arrival creates a forced ground respawn in the destination dimension");
        h.assertTrue(p.fallDistance == 0 && p.getDeltaMovement().equals(Vec3.ZERO),
            "Travel does not carry falling damage or velocity into arrival");
        var saved = p.saveWithoutId(new CompoundTag());
        var restored = player(h);
        restored.load(saved);
        h.assertTrue(restored.getRespawnDimension().equals(h.getLevel().dimension())
            && pos.equals(restored.getRespawnPosition()) && restored.isRespawnForced(),
            "Vanilla player save/load preserves the dimension, position and forced flag");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void originDataAloneDoesNotRelocateOrOverwriteBeds(GameTestHelper h) {
        var p = player(h);
        var bed = h.absolutePos(new BlockPos(3, 3, 3));
        p.setRespawnPosition(h.getLevel().dimension(), bed, 0, false, false);
        var before = p.position();
        new OriginData("scadrial", "human", "steel", "iron").save(p);
        h.assertTrue(before.equals(p.position()) && bed.equals(p.getRespawnPosition())
            && !p.isRespawnForced(), "Reading/saving ancestry cannot repeat travel or overwrite later beds");
        h.succeed();
    }
}
