package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.fortune.*;
import dev.cosmerearcanists.mixin.FortuneExplosionAccess;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FortuneProtectionGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"NearMiss"));p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:chromium",1);var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"chromium_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,item);Metalminds.transfer(item,p.getUUID(),"chromium",100);tap(p,10);return p;}
    private static void tap(FakePlayer p,int level){PowerData.state(p,"f:chromium",level);ChromiumFeruchemy.tick(p);}
    private static Snowball incoming(GameTestHelper h,FakePlayer p){var projectile=EntityType.SNOWBALL.create(h.getLevel());projectile.setNoGravity(true);projectile.setPos(p.position().add(4,.9,0));projectile.setDeltaMovement(-1,0,0);return projectile;}
    private static int tntItems(GameTestHelper h,PrimedTnt tnt){return h.getLevel().getEntitiesOfClass(ItemEntity.class,tnt.getBoundingBox().inflate(1),e->e.getItem().is(Items.TNT)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static PrimedTnt tnt(GameTestHelper h,FakePlayer p){var tnt=new PrimedTnt(EntityType.TNT,h.getLevel());tnt.setPos(p.position().add(4,0,0));tnt.setNoGravity(true);tnt.setFuse(1);return tnt;}
    /** Late probe prevents failed test explosions from mutating other fixtures; observes Fortune's earlier decision. */
    public static final class ExplosionProbe {
        final PrimedTnt source;Explosion seen;boolean fortuneCanceled;
        ExplosionProbe(PrimedTnt source){this.source=source;}
        @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void start(ExplosionEvent.Start e){if(((FortuneExplosionAccess)e.getExplosion()).cosmerearcanists$source()!=source)return;seen=e.getExplosion();fortuneCanceled=e.isCanceled();e.setCanceled(true);}
    }
    @GameTest(template="empty") public static void fortuneDeflectsIncomingFlightSlightlyWithoutChangingSpeedOrPosition(GameTestHelper h){
        var p=player(h);var projectile=incoming(h,p);double rate=FortuneConfig.DEFLECT_STEP.get(),cap=FortuneConfig.DEFLECT_CAP.get();try{FortuneConfig.DEFLECT_STEP.set(1.0);FortuneConfig.DEFLECT_CAP.set(.95);p.getRandom().setSeed(0);var position=projectile.position();var before=projectile.getDeltaMovement();h.assertTrue(FortuneProtection.tryDeflect(projectile,p),"An eligible successful luck roll changes an imminent hit into a near miss");var after=projectile.getDeltaMovement();h.assertTrue(projectile.position().equals(position)&&Math.abs(after.length()-before.length())<1e-8,"Deflection changes neither position nor speed");double angle=Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,before.normalize().dot(after.normalize())))));h.assertTrue(angle>0&&angle<=FortuneConfig.DEFLECT_ANGLE.get()+1e-8,"The trajectory turn stays inside the slight-deflection angle cap");var body=p.getBoundingBox().inflate(.08+projectile.getBbWidth()*.5);h.assertTrue(body.clip(position,position.add(after.scale(6))).isEmpty(),"The independently checked straight-flight segment misses the body");h.assertTrue(!FortuneProtection.tryDeflect(projectile,p),"A successful deflection cannot happen repeatedly on the same projectile");}finally{FortuneConfig.DEFLECT_STEP.set(rate);FortuneConfig.DEFLECT_CAP.set(cap);ChromiumFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void fortuneDoesNotRerollFailedProjectileEncountersOrAlterHarmlessShots(GameTestHelper h){
        var p=player(h);var projectile=incoming(h,p);double rate=FortuneConfig.DEFLECT_STEP.get(),cap=FortuneConfig.DEFLECT_CAP.get();try{FortuneConfig.DEFLECT_STEP.set(.001);FortuneConfig.DEFLECT_CAP.set(.01);p.getRandom().setSeed(0);var before=projectile.getDeltaMovement();h.assertTrue(!FortuneProtection.tryDeflect(projectile,p),"A failed roll leaves the incoming projectile unchanged");FortuneConfig.DEFLECT_STEP.set(1.0);FortuneConfig.DEFLECT_CAP.set(.95);p.getRandom().setSeed(0);h.assertTrue(!FortuneProtection.tryDeflect(projectile,p)&&projectile.getDeltaMovement().equals(before),"Higher subsequent tapping cannot reroll the same encounter");
            var away=incoming(h,p);away.setDeltaMovement(1,0,0);h.assertTrue(!FortuneProtection.tryDeflect(away,p),"Receding projectiles are not deflected");var own=incoming(h,p);own.setOwner(p);h.assertTrue(!FortuneProtection.tryDeflect(own,p),"The player's own projectile is not altered");PowerData.state(p,"f:chromium",0);h.assertTrue(!FortuneProtection.tryDeflect(incoming(h,p),p),"Off grants no cached protection");}finally{FortuneConfig.DEFLECT_STEP.set(rate);FortuneConfig.DEFLECT_CAP.set(cap);ChromiumFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void nativeTntKeepsItsFuseThenBecomesExactlyOneItemAtDetonation(GameTestHelper h){
        var p=player(h);var tnt=tnt(h,p);tnt.setFuse(2);var probe=new ExplosionProbe(tnt);double rate=FortuneConfig.TNT_STEP.get(),cap=FortuneConfig.TNT_CAP.get();var level=h.getLevel();level.players().add(p);NeoForge.EVENT_BUS.register(probe);try{FortuneConfig.TNT_STEP.set(1.0);FortuneConfig.TNT_CAP.set(.95);p.getRandom().setSeed(0);tnt.tick();h.assertTrue(!tnt.isRemoved()&&tnt.getFuse()==1&&tntItems(h,tnt)==0,"The TNT keeps its normal fuse until the last tick");tnt.tick();h.assertTrue(probe.seen!=null&&probe.fortuneCanceled&&tnt.isRemoved()&&tntItems(h,tnt)==1,"Native detonation is canceled and the primed TNT becomes one item");h.assertTrue(FortuneProtection.tryDud(probe.seen,List.of(p))&&tntItems(h,tnt)==1,"Repeated callbacks cannot duplicate a recovered TNT item");}finally{FortuneConfig.TNT_STEP.set(rate);FortuneConfig.TNT_CAP.set(cap);level.players().remove(p);NeoForge.EVENT_BUS.unregister(probe);level.getEntitiesOfClass(ItemEntity.class,tnt.getBoundingBox().inflate(2),e->e.getItem().is(Items.TNT)).forEach(Entity::discard);ChromiumFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void tntProtectionRequiresBlastExposureAndCurrentPaidTapping(GameTestHelper h){
        var p=player(h);var level=h.getLevel();double rate=FortuneConfig.TNT_STEP.get(),cap=FortuneConfig.TNT_CAP.get();var wall=p.blockPosition().east(2);level.players().add(p);try{FortuneConfig.TNT_STEP.set(1.0);FortuneConfig.TNT_CAP.set(.95);
            for(int phase=0;phase<3;phase++){var tnt=tnt(h,p);var probe=new ExplosionProbe(tnt);NeoForge.EVENT_BUS.register(probe);try{tap(p,10);if(phase==0)PowerData.state(p,"f:chromium",0);if(phase==1)tnt.setPos(p.position().add(20,0,0));if(phase==2)for(int y=-1;y<=3;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(wall.offset(0,y,z),Blocks.OBSIDIAN.defaultBlockState());p.getRandom().setSeed(0);tnt.tick();h.assertTrue(probe.seen!=null&&!probe.fortuneCanceled&&tntItems(h,tnt)==0,"Off, out-of-range and fully occluded players cannot cause TNT duds");}finally{NeoForge.EVENT_BUS.unregister(probe);}}
        }finally{for(int y=-1;y<=3;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(wall.offset(0,y,z),Blocks.AIR.defaultBlockState());level.players().remove(p);FortuneConfig.TNT_STEP.set(rate);FortuneConfig.TNT_CAP.set(cap);ChromiumFeruchemy.clear(p);}h.succeed();
    }
    @GameTest(template="empty") public static void multipleLuckyPlayersUseOnlyTheStrongestTntChance(GameTestHelper h){
        var low=player(h);tap(low,1);var high=player(h);high.setPos(low.position().add(0,0,1));PowerAttributes.set(high,"f:chromium",10);tap(high,100);var tnt=tnt(h,low);var probe=new ExplosionProbe(tnt);double cap=FortuneConfig.TNT_CAP.get();var level=h.getLevel();level.players().add(low);level.players().add(high);NeoForge.EVENT_BUS.register(probe);try{FortuneConfig.TNT_CAP.set(.95);low.getRandom().setSeed(0);high.getRandom().setSeed(0);tnt.tick();h.assertTrue(probe.fortuneCanceled&&tntItems(h,tnt)==1,"The strongest exposed tapper can save the group with one TNT result");h.assertTrue(low.getRandom().nextDouble()==net.minecraft.util.RandomSource.create(0).nextDouble(),"A weaker player's RNG is not rolled as an additional chance");}finally{level.players().remove(low);level.players().remove(high);NeoForge.EVENT_BUS.unregister(probe);FortuneConfig.TNT_CAP.set(cap);level.getEntitiesOfClass(ItemEntity.class,tnt.getBoundingBox().inflate(2),e->e.getItem().is(Items.TNT)).forEach(Entity::discard);ChromiumFeruchemy.clear(low);ChromiumFeruchemy.clear(high);}h.succeed();
    }
    @GameTest(template="empty") public static void protectionConfigOwnershipContactAndAmplificationFollowPaidFortune(GameTestHelper h){
        var p=player(h);h.assertTrue(Math.abs(FortuneProtection.projectileChance(p)-.2)<1e-8&&Math.abs(FortuneProtection.tntChance(p)-.1)<1e-8,"Tap10 grants20percent projectile and10percent TNT chances");PowerAttributes.set(p,"a:duralumin",1);PowerData.reserve(p,"duralumin",100);PowerData.state(p,"a:duralumin",1);EnhancementService.begin(p);tap(p,10);h.assertTrue(FortuneProtection.projectileChance(p)>.2&&FortuneProtection.tntChance(p)>.1,"Allomantic amplification increases funded protection");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(FortuneProtection.projectileChance(p)==0&&FortuneProtection.tntChance(p)==0,"Contact loss invalidates both protections immediately");boolean enabled=FortuneConfig.DEFLECTION.get();try{FortuneConfig.DEFLECTION.set(false);h.assertTrue(FortuneProtection.projectileChance(p)==0,"Projectile protection has an independent toggle");}finally{FortuneConfig.DEFLECTION.set(enabled);EnhancementService.clear(p);ChromiumFeruchemy.clear(p);}h.succeed();
    }
}
