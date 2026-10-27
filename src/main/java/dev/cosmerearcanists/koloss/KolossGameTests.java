package dev.cosmerearcanists.koloss;
import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KolossGameTests {
    private static KolossEntity create(GameTestHelper h,SpawnGroupData data){
        var mob=KolossContent.KOLOSS.get().create(h.getLevel());
        mob.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());mob.setNoAi(true);mob.setNoGravity(true);
        mob.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.NATURAL,data);
        // Synthetic fixture lives in the single-world Overworld harness; dimension enforcement has its own native spawn test.
        mob.setSpawnCancelled(false);return mob;
    }
    @GameTest(template="empty") public static void packHasExactlyOneLargestSwordLeaderAndVariedFollowers(GameTestHelper h){
        var first=KolossContent.KOLOSS.get().create(h.getLevel());
        var pack=first.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(BlockPos.ZERO),MobSpawnType.NATURAL,null);
        double leaderScale=first.getAttributeValue(Attributes.SCALE);var sizes=new HashSet<Double>();
        h.assertTrue(first.isPackLeader()&&first.getMainHandItem().is(KolossContent.SWORD.get()),"First successful pack member is the armed leader");
        for(int i=0;i<6;i++){var follower=create(h,pack);sizes.add(follower.getAttributeValue(Attributes.SCALE));h.assertTrue(!follower.isPackLeader()&&follower.getMainHandItem().isEmpty()&&follower.getAttributeValue(Attributes.SCALE)<leaderScale,"Only largest pack member receives a sword");h.assertTrue(Objects.equals(first.packId(),follower.packId()),"Members share persistent pack identity");h.assertTrue(follower.getMaxHealth()>=32&&follower.getAttributeValue(Attributes.ATTACK_DAMAGE)>=6.3,"Even small koloss are substantial melee threats");}
        h.assertTrue(sizes.size()>=4&&first.getMaxHealth()>=128&&first.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)>=.63,"Pack has varied sizes and a durable resistant leader");h.succeed();
    }
    @GameTest(template="empty") public static void savedPackKeepsSizeLeaderEquipmentAndHealth(GameTestHelper h){
        var first=create(h,null);first.setHealth(first.getMaxHealth()/2);var tag=new CompoundTag();first.saveWithoutId(tag);
        var copy=KolossContent.KOLOSS.get().create(h.getLevel());copy.load(tag);
        h.assertTrue(copy.isPackLeader()&&Objects.equals(first.packId(),copy.packId())&&copy.getMainHandItem().is(KolossContent.SWORD.get()),"Pack role and actual sword survive save/load");
        h.assertTrue(copy.getAttributeValue(Attributes.SCALE)==first.getAttributeValue(Attributes.SCALE)&&Math.abs(copy.getHealth()-first.getHealth())<.001,"Size and current health are not rerolled or healed on reload");h.succeed();
    }
    @GameTest(template="empty") public static void leaderDeathDropsOneSwordFollowerDropsNone(GameTestHelper h){
        var leader=create(h,null);
        // Seed a follower in the same native group instead of starting another group.
        var shared=new KolossEntity.PackData(leader.getUUID(),1.7f);create(h,shared);var follower=create(h,shared);
        var box=leader.getBoundingBox().inflate(4);
        var before=h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(KolossContent.SWORD.get())).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.getLevel().addFreshEntity(leader);h.getLevel().addFreshEntity(follower);
        leader.hurt(leader.damageSources().generic(),100000);follower.hurt(follower.damageSources().generic(),100000);
        int after=h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(KolossContent.SWORD.get())).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(leader.isDeadOrDying()&&follower.isDeadOrDying()&&after-before==1,"Native death equipment path drops exactly the leader's one sword");h.succeed();
    }
    @GameTest(template="empty") public static void spawningOutsideScadrialRejectedIncludingEggs(GameTestHelper h){
        h.assertTrue(!KolossEntity.spawnDimension(h.getLevel()),"Overworld harness is not Scadrial");
        h.assertTrue(!KolossEntity.canSpawn(KolossContent.KOLOSS.get(),h.getLevel(),MobSpawnType.NATURAL,h.absolutePos(new BlockPos(2,1,2)),h.getLevel().random),"Natural spawn predicate rejects shared Overworld biomes");
        var spawned=KolossContent.KOLOSS.get().spawn(h.getLevel(),h.absolutePos(new BlockPos(2,20,2)),MobSpawnType.SPAWN_EGG);
        h.assertTrue(spawned==null||spawned.isRemoved()||h.getLevel().getEntity(spawned.getUUID())==null,"FinalizeSpawnEvent prevents egg/spawner-style insertion outside Scadrial");h.succeed();
    }
}
