package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BronzeFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BronzeFerring"));
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 20, 1))));
        PowerAttributes.set(p, "f:bronze", 1);
        return p;
    }
    private static ItemStack mind(FakePlayer p) {
        var item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "bronze_metalmind_ring")));
        p.setItemInHand(InteractionHand.OFF_HAND, item);
        return item;
    }
    private static int rest(FakePlayer p) { return p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)); }
    private static void rest(FakePlayer p, int value) { p.getStats().setValue(p, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), value); }
    private static void close(GameTestHelper h, double actual, double expected, String message) { h.assertTrue(Math.abs(actual - expected) < 1e-7, message + ": " + actual + " expected " + expected); }

    @GameTest(template="empty") public static void bronzeStorageConservesWakefulnessAndSlowsPhysicalActions(GameTestHelper h) {
        var p = player(h); var item = mind(p);
        double movement = p.getAttributeValue(Attributes.MOVEMENT_SPEED), attack = p.getAttributeValue(Attributes.ATTACK_SPEED), mining = p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        rest(p, 1000); PowerData.state(p, "f:bronze", -100);
        for (int i = 0; i < 100; i++) BronzeFeruchemy.tick(p);
        close(h, Metalminds.charge(item), 1.5, "Five seconds at30percent stores1.5wakefulness-seconds");
        h.assertTrue(rest(p) == 1030, "Native insomnia receives exactly30additional ticks, preserving fractional correction");
        close(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), movement * .85, "Drowsiness movement penalty");
        close(h, p.getAttributeValue(Attributes.ATTACK_SPEED), attack * .85, "Drowsiness attack recovery penalty");
        close(h, p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), mining * .85, "Drowsiness mining penalty");
        h.assertTrue(SeekingService.activeStrength(p, "f:bronze") > 0 && PowerRules.implemented("f:bronze"), "Paid bronze emits a real Feruchemical signal and claims implementation");
        PowerData.state(p, "f:bronze", 0); BronzeFeruchemy.tick(p);
        close(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), movement, "Off clears only the drowsiness modifier");
        h.assertTrue(SeekingService.activeStrength(p, "f:bronze") == 0, "Off emits no wakefulness signal"); h.succeed();
    }
    @GameTest(template="empty") public static void bronzeTappingOffsetsNativeInsomniaWithCompressionAndNoUnusedRestBank(GameTestHelper h) {
        var p = player(h); var item = mind(p); Metalminds.transfer(item, p.getUUID(), "bronze", 100);
        rest(p, 1000); PowerData.state(p, "f:bronze", 10);
        for (int i = 0; i < 20; i++) { rest(p, rest(p) + 1); BronzeFeruchemy.tick(p); }
        h.assertTrue(rest(p) == 990, "Tap10 offsets20natural ticks and restores10existing insomnia ticks");
        close(h, Metalminds.charge(item), 100 - 1.5 * Math.pow(1.5, .25), "High output pays compression from the item");
        PowerData.state(p, "f:bronze", 1);
        for (int i = 0; i < 20; i++) { rest(p, rest(p) + 1); BronzeFeruchemy.tick(p); }
        h.assertTrue(rest(p) == 1007, "Tap1 slows ordinary insomnia growth by15percent");
        rest(p, 0); PowerData.state(p, "f:bronze", 10);
        for (int i = 0; i < 40; i++) BronzeFeruchemy.tick(p);
        h.assertTrue(rest(p) == 0 && PowerData.data(p).getDouble("bronze_rest_remainder") == 0, "Unused tapping at zero cannot bank future free rest");
        rest(p, Integer.MAX_VALUE); PowerData.state(p, "f:bronze", -3);
        for (int i = 0; i < 20; i++) BronzeFeruchemy.tick(p);
        h.assertTrue(rest(p) == Integer.MAX_VALUE, "Insomnia saturates without integer overflow");
        BronzeFeruchemy.clear(p); h.succeed();
    }
    @GameTest(template="empty") public static void bronzeFractionalLimitsPartialCapacityAndDepletionNeverGrantFreeEffects(GameTestHelper h) {
        var p = player(h); var item = mind(p); PowerAttributes.set(p, "f:bronze", 1.5);
        h.assertTrue(PowerData.storeLevels(p, "f:bronze") == 5 && PowerData.tapLevels(p, "f:bronze") == 15, "Attribute1.5 grants45percent storage and15taplevels");
        PowerData.state(p, "f:bronze", -100); BronzeFeruchemy.tick(p);
        close(h, Metalminds.charge(item), .45 / 20, "Partial fifth storelevel stores45percent");
        Metalminds.transfer(item, p.getUUID(), "bronze", -1000); Metalminds.transfer(item, p.getUUID(), "bronze", .00375);
        PowerData.state(p, "f:bronze", 1); BronzeFeruchemy.tick(p);
        close(h, Metalminds.charge(item), 0, "Half a tick of fuel is spent exactly");
        close(h, PowerData.data(p).getDouble("bronze_rest_remainder"), .45 - .075, "Partial reserve produces only half the selected output");
        h.assertTrue(PowerData.state(p, "f:bronze") == 0 && !Metalminds.bound(item), "Depletion stops and unkeys the metalmind");
        int before = rest(p); BronzeFeruchemy.tick(p); h.assertTrue(rest(p) == before, "Exhausted bronze gives no additional rest");
        Metalminds.transfer(item, p.getUUID(), "bronze", Metalminds.capacity(item) - .001);
        PowerData.state(p, "f:bronze", -3); BronzeFeruchemy.tick(p);
        close(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED) / p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), .99, "Only actual remaining capacity causes drowsiness");
        PowerAttributes.set(p, "f:bronze", 4); item = mind(p); PowerData.state(p, "f:bronze", -100); BronzeFeruchemy.tick(p);
        close(h, Metalminds.charge(item), .95 / 20, "High attributes still obey95percent storage ceiling");
        BronzeFeruchemy.clear(p); h.succeed();
    }
    @GameTest(template="empty") public static void bronzeRespectsOwnersStacksContactIdleAndIndependentConfiguration(GameTestHelper h) {
        var p = player(h); var item = mind(p); Metalminds.transfer(item, UUID.randomUUID(), "bronze", 10);
        PowerData.state(p, "f:bronze", 10); rest(p, 1000); BronzeFeruchemy.tick(p);
        h.assertTrue(rest(p) == 1000 && Metalminds.charge(item) == 10 && PowerData.state(p, "f:bronze") == 0, "Foreign ownership grants no rest");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "bronze_ingot")), 2));
        PowerData.state(p, "f:bronze", -3); BronzeFeruchemy.tick(p); h.assertTrue(PowerData.state(p, "f:bronze") == 0, "Stacks cannot store");
        item = mind(p); long time = p.level().getGameTime();
        for (int i = 1; i <= 6001; i++) StorageActivity.observe(p, time + i);
        PowerData.state(p, "f:bronze", -3); BronzeFeruchemy.tick(p); h.assertTrue(Metalminds.charge(item) == 0, "Shared idle gate prevents bronze AFK storage");
        p.setPos(p.position().add(.1, 0, 0)); StorageActivity.tick(p);
        boolean drowsy = FeruchemyConfig.BRONZE_DROWSY.get(), insomnia = FeruchemyConfig.BRONZE_REST.get(), enabled = FeruchemyConfig.BRONZE.get();
        try {
            FeruchemyConfig.BRONZE_DROWSY.set(false); PowerData.state(p, "f:bronze", -3);
            for (int i = 0; i < 20; i++) BronzeFeruchemy.tick(p);
            h.assertTrue(rest(p) == 1006 && p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().isEmpty(), "Drowsiness toggle leaves paid insomnia enabled");
            FeruchemyConfig.BRONZE_REST.set(false); FeruchemyConfig.BRONZE_DROWSY.set(true);
            for (int i = 0; i < 20; i++) BronzeFeruchemy.tick(p);
            h.assertTrue(rest(p) == 1006 && p.getAttributeValue(Attributes.MOVEMENT_SPEED) < p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), "Insomnia toggle leaves drowsiness enabled");
            double saved = Metalminds.charge(item); FeruchemyConfig.BRONZE.set(false); BronzeFeruchemy.tick(p);
            h.assertTrue(Metalminds.charge(item) == saved && PowerAttributes.strength(p, "f:bronze") == 1 && SeekingService.activeStrength(p, "f:bronze") == 0, "Disabling preserves charge and power and stops all effects");
            FeruchemyConfig.BRONZE.set(true); p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); BronzeFeruchemy.tick(p);
            h.assertTrue(PowerData.state(p, "f:bronze") == 0 && Metalminds.charge(item) == saved, "Lost contact stops without discarding charge");
        } finally { FeruchemyConfig.BRONZE_DROWSY.set(drowsy); FeruchemyConfig.BRONZE_REST.set(insomnia); FeruchemyConfig.BRONZE.set(enabled); BronzeFeruchemy.clear(p); }
        h.succeed();
    }
    @GameTest(template="empty") public static void bronzeFractionalRestSurvivesTogglingAndLifecyclePreservesOtherModifiers(GameTestHelper h) {
        var p = player(h); var item = mind(p); rest(p, 1000);
        var other = ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "bronze_test_other");
        p.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(other, .2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        double normal = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (int i = 0; i < 10; i++) {
            PowerData.state(p, "f:bronze", -3); BronzeFeruchemy.tick(p);
            PowerData.state(p, "f:bronze", 0); BronzeFeruchemy.clear(p);
        }
        h.assertTrue(rest(p) == 1003, "Toggling cannot discard fractional insomnia payments");
        close(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), normal, "Unrelated modifier survives cleanup");
        PowerData.state(p, "f:bronze", -3); BronzeFeruchemy.tick(p); double saved = Metalminds.charge(item);
        new BronzeFeruchemy().logout(new PlayerEvent.PlayerLoggedOutEvent(p));
        h.assertTrue(PowerData.state(p, "f:bronze") == 0 && Metalminds.charge(item) == saved, "Logout stops state and preserves item reserve");
        close(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), normal, "Logout cleans drowsiness");
        var replacement = player(h); PowerData.clonePlayer(p, replacement);
        close(h, PowerData.data(replacement).getDouble("bronze_rest_remainder"), .3, "Saved correction survives PowerData clone without creating a power grant");
        h.assertTrue(PowerData.state(replacement, "f:bronze") == 0, "Clone requires deliberate reactivation");
        p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(other); h.succeed();
    }
    @GameTest(template="empty") public static void bronzeDispatchCorrectsTheNativeServerPlayerRestClock(GameTestHelper h) {
        // Restore awardStat, which FakePlayer normally suppresses, then exercise native doTick and PlayerTickEvent.Post.
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BronzeNative")) {
            @Override public void awardStat(Stat<?> stat, int amount) { getStats().increment(this, stat, amount); }
        };
        p.setGameMode(GameType.SURVIVAL); p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 30, 1))));
        PowerAttributes.set(p, "f:bronze", 1); var item = mind(p); Metalminds.transfer(item, p.getUUID(), "bronze", 100);
        rest(p, 72020); PowerData.state(p, "f:bronze", 10);
        for (int i = 0; i < 60; i++) p.doTick();
        h.assertTrue(rest(p) == 71990, "Actual server player ticks restore insomnia below the vanilla three-day phantom threshold");
        close(h, Metalminds.charge(item), 100 - 3 * 1.5 * Math.pow(1.5, .25), "Native dispatch funds exactly one bronze update per tick");
        BronzeFeruchemy.clear(p); h.succeed();
    }

    @GameTest(template="empty") public static void bronzeShortensOnlyFatigueAndNauseaAndPreservesHiddenEffects(GameTestHelper h) {
        var p=player(h); var item=mind(p); Metalminds.transfer(item,p.getUUID(),"bronze",100);
        var fatigue=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,200,0);
        fatigue.update(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,100,2));
        p.addEffect(fatigue);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,200,0));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200,0));
        PowerData.state(p,"f:bronze",10);
        for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);
        var active=p.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        var hidden=((dev.cosmerearcanists.mixin.BronzeEffectAccess)active).cosmerearcanists$hiddenEffect();
        h.assertTrue(active.getDuration()==80&&active.getAmplifier()==2&&hidden!=null&&hidden.getDuration()==180,"Visible and hidden fatigue age faster without losing their amplifier or chain");
        h.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION).getDuration()==180&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON).getDuration()==200,"Only the two agreed debuffs are shortened");
        boolean gate=FeruchemyConfig.BRONZE_DEBUFFS.get();
        try{FeruchemyConfig.BRONZE_DEBUFFS.set(false);for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);h.assertTrue(active.getDuration()==80,"Independent gate preserves existing debuffs");}finally{FeruchemyConfig.BRONZE_DEBUFFS.set(gate);BronzeFeruchemy.clear(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void bronzeDebuffScalingRequiresFuelAndNeverBanksMissingEffectTicks(GameTestHelper h) {
        var p=player(h); var item=mind(p); Metalminds.transfer(item,p.getUUID(),"bronze",100);
        PowerAttributes.set(p,"f:bronze",2);PowerData.state(p,"f:bronze",20);
        for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);
        var fatigue=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,200,0);p.addEffect(fatigue);BronzeFeruchemy.tick(p);
        h.assertTrue(fatigue.getDuration()==198,"Absent effects do not bank virtual debuff ticks; tap20 removes2extra ticks per tick");
        PowerData.state(p,"f:bronze",1);for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);
        h.assertTrue(fatigue.getDuration()==196,"Tap1 removes only2extra ticks in20ticks");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);h.assertTrue(fatigue.getDuration()==196,"Lost contact grants no free shortening");
        p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:bronze",-3);for(int i=0;i<20;i++)BronzeFeruchemy.tick(p);h.assertTrue(fatigue.getDuration()==196,"Storage never shortens debuffs");
        p.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,-1,0));PowerData.state(p,"f:bronze",20);BronzeFeruchemy.tick(p);
        h.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION).isInfiniteDuration(),"Infinite-duration effects are not rewritten into finite effects");BronzeFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void bronzeWarningsStartAtTenAndRejectWallsRecedingAndOwnProjectiles(GameTestHelper h) {
        var p=player(h);var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,h.getLevel());
        arrow.setPos(p.getBoundingBox().getCenter().add(4,0,0));arrow.setDeltaMovement(-1,0,0);h.getLevel().addFreshEntity(arrow);
        h.assertTrue(BronzeAlertness.warnings(p,9).isEmpty()&&BronzeAlertness.warnings(p,10).size()==1,"Warning threshold includes level10 and excludes9");
        var base=BronzeAlertness.warnings(p,10).getFirst();var strong=BronzeAlertness.warnings(p,20).getFirst();h.assertTrue(strong.intensity()>base.intensity()&&base.direction().x>.9,"Higher tapping strengthens the correctly directed warning");
        arrow.setDeltaMovement(1,0,0);h.assertTrue(BronzeAlertness.warnings(p,20).isEmpty(),"Receding projectile is not an approaching threat");
        arrow.setDeltaMovement(-1,0,0);arrow.setOwner(p);h.assertTrue(BronzeAlertness.warnings(p,20).isEmpty(),"Own projectile never warns");arrow.setOwner(null);
        var block=BlockPos.containing(p.getEyePosition().add(2,0,0));
        for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(block.offset(0,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(BronzeAlertness.warnings(p,20).isEmpty(),"Opaque wall prevents danger sensing");
        for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(block.offset(0,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        arrow.setInvisible(true);h.assertTrue(BronzeAlertness.warnings(p,20).isEmpty(),"Invisible threats are not revealed");arrow.discard();h.succeed();
    }
    @GameTest(template="empty") public static void bronzeWarnsForEngagedAttacksAndCodecIsPrivateBoundedAndFinite(GameTestHelper h) {
        var p=player(h);var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());zombie.setNoAi(true);zombie.setPos(p.position().add(0,0,2));zombie.setYRot(180);zombie.setTarget(p);zombie.setAggressive(true);h.getLevel().addFreshEntity(zombie);
        h.assertTrue(BronzeAlertness.warnings(p,10).size()==1,"Nearby engaged enemy facing the player warns");
        zombie.setTarget(null);h.assertTrue(BronzeAlertness.warnings(p,10).isEmpty(),"Idle mob does not warn merely for existing nearby");zombie.discard();
        var packet=new BronzeWarningPacket(p.level().dimension().location().toString(),6,java.util.List.of(new BronzeWarningPacket.Warning(new Vec3(1,0,0),.4f)));
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{BronzeWarningPacket.CODEC.encode(b,packet);h.assertTrue(BronzeWarningPacket.CODEC.decode(b).equals(packet),"Directions and intensity round-trip without entity identities or future paths");}finally{b.release();}
        boolean rejected=false;try{new BronzeWarningPacket.Warning(new Vec3(Double.NaN,0,0),.5f);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Nonfinite warning vectors are rejected");
        rejected=false;try{new BronzeWarningPacket("",6,java.util.Collections.nCopies(9,packet.warnings().getFirst()));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Warning budget enforced on construction");h.succeed();
    }
}
