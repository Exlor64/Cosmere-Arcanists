package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoinshotGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"CoinshotCheck"));
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,6,2))));
        MetalDetector.rebuild(p.server);
        PowerAttributes.set(p,"a:steel",1);PowerData.state(p,"a:steel",2);PowerData.reserve(p,"steel",100);
        PowerData.data(p).putString("selected","a:steel");PowerService.control(p,new PowerNetwork.Control(2,"",1));
        return p;
    }
    private static SteelProjectile launch(FakePlayer p,ItemStack item) {
        var dropped=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),item);
        if(MetalProjectiles.launch(p,dropped)==null)throw new AssertionError("Shot did not launch");
        return p.serverLevel().getEntitiesOfClass(SteelProjectile.class,p.getBoundingBox().inflate(5),e->e.getOwner()==p).stream().findFirst().orElseThrow();
    }
    private static CompoundTag saved(SteelProjectile projectile) {var tag=new CompoundTag();projectile.addAdditionalSaveData(tag);return tag;}
    private static void impact(SteelProjectile projectile) {
        projectile.onHit(new BlockHitResult(projectile.position(),Direction.UP,projectile.blockPosition(),false));
    }
    @GameTest(template="empty") public static void normalSteelShotUsesBaseFormulaAndOwnedPower(GameTestHelper h) {
        var p=player(h);
        for(int strength:new int[]{1,2}) {
            PowerAttributes.set(p,"a:steel",strength);
            var shot=launch(p,new ItemStack(Items.IRON_INGOT));
            var cow=EntityType.COW.create(h.getLevel());cow.setNoAi(true);cow.setNoGravity(true);
            float before=cow.getHealth();
            shot.onHitEntity(new EntityHitResult(cow));
            h.assertTrue(Math.abs(before-cow.getHealth()-3.8*strength)<.001,
                "Normal ingot shots deal3.8 per power strength, independent of movement boost and launch speed cap");
            shot.discard();cow.discard();
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void anvilWearPreservesStackDataAndFinalShotBreaksAfterHitting(GameTestHelper h) {
        var p=player(h);boolean wear=PowerConfig.ANVIL_SHOT_WEAR.get();
        ItemStack cargo=new ItemStack(Items.ANVIL,2);cargo.set(DataComponents.CUSTOM_NAME,Component.literal("WearCheck"));
        try {
            PowerConfig.ANVIL_SHOT_WEAR.set(true);
            for(int stage=0;stage<3;stage++) {
                h.assertTrue(MetalAmounts.units(p.server,cargo)==558,"All anvil wear stages retain279metal units each");
                var shot=launch(p,cargo.copy());
                var state=saved(shot);var worn=ItemStack.parseOptional(h.getLevel().registryAccess(),state.getCompound("Cargo"));
                if(stage<2)h.assertTrue(worn.is(stage==0?Items.CHIPPED_ANVIL:Items.DAMAGED_ANVIL)
                    && worn.getCount()==2 && worn.getHoverName().getString().equals("WearCheck"),"Successful launch advances one stage for every carried anvil without losing components");
                else {
                    h.assertTrue(state.getBoolean("AnvilBreaksAfterFlight"),"Damaged anvil is marked spent but still has flight cargo");
                    var restored=new SteelProjectile(MetalProjectiles.STEEL.get(),h.getLevel());
                    restored.readAdditionalSaveData(state);restored.setPos(shot.position());restored.setDeltaMovement(shot.getDeltaMovement());
                    shot.discard();shot=restored;
                    var cow=EntityType.COW.create(h.getLevel());float before=cow.getHealth();
                    shot.onHitEntity(new EntityHitResult(cow));
                    h.assertTrue(cow.getHealth()<before,"The final shot still deals impact damage after save/load");cow.discard();
                }
                impact(shot);
                var recovered=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(8),
                    e->e.getItem().getHoverName().getString().equals("WearCheck"));
                if(stage==2)h.assertTrue(recovered.isEmpty(),"Final shot recovers no anvil or reusable fragment");
                else {
                    h.assertTrue(recovered.size()==1,"One worn anvil stack is recovered");
                    cargo=recovered.getFirst().getItem().copy();recovered.getFirst().discard();
                }
            }
        } finally {PowerConfig.ANVIL_SHOT_WEAR.set(wear);}
        h.succeed();
    }
    @GameTest(template="empty") public static void failedLaunchDoesNotWearAndWearCanBeDisabled(GameTestHelper h) {
        var p=player(h);boolean wear=PowerConfig.ANVIL_SHOT_WEAR.get();
        var cargo=new ItemStack(Items.ANVIL);
        PowerData.state(p,"a:steel",0);
        h.assertTrue(MetalProjectiles.launch(p,new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),cargo))==null
            && cargo.is(Items.ANVIL),"An ordinary failed/unpowered drop never damages the anvil");
        try {
            PowerConfig.ANVIL_SHOT_WEAR.set(false);PowerData.state(p,"a:steel",2);
            var shot=launch(p,cargo);
            h.assertTrue(ItemStack.parseOptional(h.getLevel().registryAccess(),saved(shot).getCompound("Cargo")).is(Items.ANVIL),
                "Disabling wear keeps the original anvil");shot.discard();
        } finally {PowerConfig.ANVIL_SHOT_WEAR.set(wear);}
        h.succeed();
    }
    @GameTest(template="empty") public static void savedShotDamageProfileAndLegacyIncrementAreStable(GameTestHelper h) {
        var p=player(h);double increment=PowerConfig.NUGGET_INCREMENT.get();
        try {
            PowerConfig.NUGGET_INCREMENT.set(.2);
            var shot=launch(p,new ItemStack(Items.IRON_INGOT));var tag=saved(shot);shot.discard();
            PowerConfig.NUGGET_INCREMENT.set(.4);
            var restored=new SteelProjectile(MetalProjectiles.STEEL.get(),h.getLevel());restored.readAdditionalSaveData(tag);
            h.assertTrue(saved(restored).getDouble("DamagePerNugget")==.2 && saved(restored).getDouble("ShotPower")==1
                && saved(restored).getDouble("LaunchSpeed")>0,"New shots retain their captured power and tuning across save/load");
            tag.remove("DamagePerNugget");tag.remove("ShotPower");tag.remove("LaunchSpeed");
            restored.readAdditionalSaveData(tag);
            h.assertTrue(saved(restored).getDouble("DamagePerNugget")==.4 && saved(restored).getDouble("LaunchSpeed")==0,
                "Old saved shots adopt the current increment and retain a safe velocity-based fallback");
        } finally {PowerConfig.NUGGET_INCREMENT.set(increment);}
        h.succeed();
    }
}
