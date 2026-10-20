package dev.cosmerearcanists.power;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EmotionGameTests {
    private static FakePlayer caster(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"EmotionTest"));p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));for(String m:List.of("zinc","brass")){PowerAttributes.set(p,"a:"+m,1);PowerData.state(p,"a:"+m,2);PowerData.reserve(p,m,100);}return p;}
    private static <T extends Mob> T mob(GameTestHelper h,EntityType<T> type,Vec3 pos){var m=type.create(h.getLevel());m.setPos(pos);h.getLevel().addFreshEntity(m);return m;}
    @GameTest(template="empty") public static void copperUsesMetalStrengthAndExactQuarterAdvantageForAllEmotionsAndMinions(GameTestHelper h){
        var source=caster(h);var target=caster(h);target.setPos(source.position().add(8,0,0));PowerAttributes.set(target,"a:copper",1);PowerData.reserve(target,"copper",100);PowerData.state(target,"a:copper",2);
        for(String power:List.of("a:zinc","a:brass"))for(var emotion:Emotion.values()){
            EmotionService.clear(target);PowerAttributes.set(source,power,1.249999);h.assertTrue(!EmotionService.apply(source,target,power,emotion),"Less than25percent stronger is blocked for both metals and every emotion");
            PowerAttributes.set(source,power,1.25);h.assertTrue(EmotionService.apply(source,target,power,emotion),"Exactly25percent stronger pierces independently of distance-reduced effect size");
        }
        EmotionService.clear(target);PowerAttributes.set(source,"a:zinc",1.25);EmotionService.apply(source,target,"a:zinc",Emotion.ANGER);PowerData.state(target,"a:copper",3);
        h.assertTrue(EmotionService.dominant(target)==null,"Flaring copper can remove an already active weaker influence");
        PowerAttributes.set(source,"a:zinc",2.5);h.assertTrue(EmotionService.apply(source,target,"a:zinc",Emotion.ANGER),"Burn multiplier participates on both sides of the strength comparison");
        EmotionService.clear(target);target.setPos(source.position());PowerData.state(target,"a:copper",2);PowerAttributes.set(target,"a:copper",3.2);PowerAttributes.set(source,"a:zinc",4);SpiritualIntegrity.set(target,.25);
        h.assertTrue(EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE) && DominationService.owner(target)==source,"Exact25percent edge can establish minion control");
        PowerAttributes.set(target,"a:copper",3.200001);DominationService.tick();h.assertTrue(DominationService.owner(target)==null,"Copper becoming strong enough releases existing control");
        PowerData.state(target,"a:copper",0);PowerAttributes.set(source,"a:zinc",1);h.assertTrue(EmotionService.apply(source,target,"a:zinc",Emotion.FEAR),"Inactive copper gives no emotional immunity");h.assertTrue(DominationService.owner(target)==null,"Applying fear cannot refresh old lingering compliance into a new minion bond");h.succeed();
    }
    @GameTest(template="empty") public static void dominationRequiresSpiritualDamageAndOverwhelmingCompliance(GameTestHelper h){
        var source=caster(h);var target=mob(h,EntityType.ZOMBIE,source.position());PowerAttributes.set(source,"a:zinc",4);
        EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==null,"Intact spirits resist even strong compliance");
        SpiritualIntegrity.set(target,.5);PowerAttributes.set(source,"a:zinc",1);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==null,"Spiritual damage alone is insufficient");
        PowerAttributes.set(source,"a:zinc",4);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==source && DominationService.remaining(target)==1200,"Threshold strength seizes a damaged spirit for one minute");
        target.setTarget(source);h.assertTrue(target.getTarget()==null,"Minions cannot target their controller");
        PowerAttributes.set(source,"a:zinc",8);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.remaining(target)==2400,"Duration scales with effective power");
        var enemy=mob(h,EntityType.SHEEP,source.position().add(1,0,0));source.setLastHurtMob(enemy);float health=enemy.getHealth();DominationService.behave(target);h.assertTrue(target.getTarget()==enemy && enemy.getHealth()<health,"Controlled mobs assist their owner in combat");
        PowerAttributes.set(source,"a:zinc",64);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.remaining(target)==EmotionConfig.MINION_CAP.get(),"Extreme strength reaches the configured duration cap independently of HUD intensity limits");
        SpiritualIntegrity.set(target,1);DominationService.tick();h.assertTrue(DominationService.owner(target)==null,"Restoring integrity releases a minion");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=120) public static void dominationExpiresAndPlayerIntegritySurvivesClone(GameTestHelper h){
        var source=caster(h);var target=caster(h);target.setPos(source.position());var copy=caster(h);SpiritualIntegrity.set(target,.25);PowerData.clonePlayer(target,copy);h.assertTrue(SpiritualIntegrity.get(copy)==.25,"Player spiritual damage persists through respawn copying");
        int duration=EmotionConfig.MINION_TICKS.get();try{EmotionConfig.MINION_TICKS.set(20);PowerAttributes.set(source,"a:zinc",4);EmotionService.apply(source,target,"a:zinc",Emotion.COMPLIANCE);h.assertTrue(DominationService.owner(target)==source && DominationService.remaining(target)==20,"Players can be controlled under the same thresholds");}finally{EmotionConfig.MINION_TICKS.set(duration);}
        h.runAfterDelay(22,()->{DominationService.tick();h.assertTrue(DominationService.owner(target)==null,"Control expires independently of short emotional linger");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=120) public static void soothingHostileAngerClearsEveryVictimAndAllowsRecovery(GameTestHelper h){
        var source=caster(h);var victim=caster(h);var stranger=caster(h);var zombie=mob(h,EntityType.ZOMBIE,source.position().add(1,0,0));
        zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));zombie.setTarget(victim);
        PowerAttributes.set(source,"a:brass",.01);EmotionService.apply(source,zombie,"a:brass",Emotion.ANGER);
        h.assertTrue(!EmotionService.pacified(zombie) && zombie.getTarget()==victim && !EmotionService.forcesBehavior(zombie),"Insufficient soothing must preserve native aggression, not turn it into magically forced mob infighting");
        PowerAttributes.set(source,"a:brass",1);EmotionService.apply(source,zombie,"a:brass",Emotion.ANGER);
        h.assertTrue(EmotionService.pacified(zombie) && zombie.getTarget()==null,"Enough anger soothing immediately calms a hostile mob even when its victim is not the Allomancer");
        zombie.setTarget(victim);h.assertTrue(zombie.getTarget()==null,"Native AI may not reacquire the old victim while pacified");
        zombie.setTarget(stranger);h.assertTrue(zombie.getTarget()==null,"Pacification also blocks choosing a new victim");
        h.runAfterDelay(EmotionConfig.DURATION.get()+2,()->{h.assertTrue(!EmotionService.pacified(zombie),"Pacification expires instead of permanently taming the mob");zombie.setTarget(victim);h.assertTrue(zombie.getTarget()==victim,"Vanilla target acquisition resumes after the influence fades");h.succeed();});
    }
    @GameTest(template="empty") public static void soothingTrustCanExposeAngerAndSoothingLoveCancelsBreeding(GameTestHelper h){
        var source=caster(h);var cow=mob(h,EntityType.COW,source.position().add(1,0,0));var sheep=mob(h,EntityType.SHEEP,cow.position().add(1,0,0));
        EmotionService.apply(source,cow,"a:brass",Emotion.TRUST);cow.tickCount=20;EmotionService.behave(cow);
        h.assertTrue(EmotionService.dominant(cow)==Emotion.ANGER && cow.getTarget()==sheep,"Inverse effects follow the strongest remaining emotion: soothing trust can expose anger in a passive mob");
        EmotionService.clear(cow);cow.setTarget(null);EmotionService.apply(source,cow,"a:zinc",Emotion.LOVE);EmotionService.behave(cow);h.assertTrue(cow.isInLove(),"Rioting love prepares breeding");
        EmotionService.apply(source,cow,"a:brass",Emotion.LOVE);h.assertTrue(!cow.isInLove(),"Counter-soothing removes induced breeding readiness once love loses dominance");
        EmotionService.clear(cow);cow.setInLove(source);EmotionService.apply(source,cow,"a:brass",Emotion.LOVE);h.assertTrue(!cow.isInLove(),"Sufficient Soothing also reduces naturally present love rather than only countering a Rioter");h.succeed();
    }
    @GameTest(template="empty") public static void angerRiotingOverridesSoothingAndCalmStopsAutonomousCreeperSwelling(GameTestHelper h){
        var source=caster(h);var rioter=caster(h);var victim=caster(h);var creeper=mob(h,EntityType.CREEPER,source.position().add(1,0,0));creeper.setNoGravity(true);creeper.setTarget(victim);creeper.setSwellDir(1);
        EmotionService.apply(source,creeper,"a:brass",Emotion.ANGER);var swelling=new net.minecraft.world.entity.ai.goal.SwellGoal(creeper);
        h.assertTrue(creeper.getTarget()==null && creeper.getSwellDir()== -1 && !swelling.canUse(),"Soothing anger stops an autonomous creeper attack and its swelling goal");
        PowerAttributes.set(rioter,"a:zinc",4);EmotionService.apply(rioter,creeper,"a:zinc",Emotion.ANGER);
        h.assertTrue(!EmotionService.pacified(creeper),"A stronger Rioter can make anger dominant again");var nearest=mob(h,EntityType.COW,creeper.position().add(1,0,0));creeper.setTarget(victim);h.assertTrue(creeper.getTarget()==nearest,"A winning anger signal redirects native aggression to the nearest mob");nearest.discard();
        EmotionService.clear(creeper);EmotionService.apply(source,creeper,"a:brass",Emotion.ANGER);creeper.ignite();creeper.setSwellDir(1);h.assertTrue(swelling.canUse(),"Emotions do not reverse deliberate ignition");h.succeed();
    }
    @GameTest(template="empty") public static void angerOnlyRedirectsNativeCombatAndRetargetsNearest(GameTestHelper h){
        var source=caster(h);var skeleton=mob(h,EntityType.SKELETON,source.position().add(1,0,0));skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        var near=mob(h,EntityType.COW,skeleton.position().add(1,0,0));var far=mob(h,EntityType.COW,skeleton.position().add(4,0,0));
        try{
            h.assertTrue(EmotionService.apply(source,skeleton,"a:zinc",Emotion.ANGER),"Anger applies to a native ranged attacker");skeleton.tickCount=20;float health=near.getHealth();EmotionService.behave(skeleton);
            h.assertTrue(skeleton.getTarget()==near && near.getHealth()==health,"Anger switches target but never applies a forced melee hit");
            var emotional=new EmotionalGoal(skeleton);var targetGoal=new AngerTargetGoal(skeleton);
            h.assertTrue(!emotional.canUse() && targetGoal.canUse() && targetGoal.getFlags().equals(java.util.EnumSet.of(net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET)),"Native anger takes only TARGET, leaving movement/look free for bow AI");
            targetGoal.tick();skeleton.setTarget(source);h.assertTrue(skeleton.getTarget()==near,"Native target acquisition cannot replace emotional nearest-mob targeting with a player");
            near.setPos(skeleton.position().add(10,0,0));targetGoal.tick();h.assertTrue(skeleton.getTarget()==far,"Changed distances select the new nearest mob");far.discard();targetGoal.tick();h.assertTrue(skeleton.getTarget()==near,"Dead or removed targets are replaced");
            EmotionService.clear(skeleton);targetGoal.stop();h.assertTrue(skeleton.getTarget()==null,"Temporary anger target clears when goal ends");
            var cow=mob(h,EntityType.COW,source.position().add(0,0,2));EmotionService.apply(source,cow,"a:zinc",Emotion.ANGER);h.assertTrue(!EmotionService.hasNativeCombat(cow) && EmotionService.forcesBehavior(cow),"A passive animal keeps fallback anger behavior");EmotionService.clear(cow);cow.discard();
            EmotionService.apply(source,skeleton,"a:zinc",Emotion.ANGER);boolean preserve=EmotionConfig.NATIVE_ANGER.get();try{EmotionConfig.NATIVE_ANGER.set(false);h.assertTrue(!EmotionService.nativeAnger(skeleton) && EmotionService.forcesBehavior(skeleton),"Config can restore legacy forced combat");}finally{EmotionConfig.NATIVE_ANGER.set(preserve);}
        }finally{EmotionService.clear(skeleton);skeleton.discard();near.discard();far.discard();}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void angeredSkeletonActuallyShootsAnotherSkeleton(GameTestHelper h){
        var source=caster(h);var origin=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1,20,1)));source.setPos(origin);
        for(int x=0;x<7;x++)for(int z=0;z<7;z++){h.setBlock(new BlockPos(x,19,z),net.minecraft.world.level.block.Blocks.STONE);for(int y=20;y<25;y++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);}
        var skeleton=mob(h,EntityType.SKELETON,origin.add(1,0,1));var victim=mob(h,EntityType.SKELETON,skeleton.position().add(1.5,0,0));victim.setNoAi(true);victim.setNoGravity(true);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));skeleton.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));victim.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));skeleton.reassessWeaponGoal();
        float health=victim.getHealth();EmotionService.apply(source,skeleton,"a:zinc",Emotion.ANGER);EmotionService.behave(skeleton);
        h.runAfterDelay(55,()->{try{h.assertTrue(victim.getHealth()<health && victim.getLastDamageSource()!=null && victim.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE),"Native bow AI fires a real damaging arrow at the other skeleton");}finally{EmotionService.clear(skeleton);skeleton.discard();victim.discard();}h.succeed();});
    }
    @GameTest(template="empty") public static void brainAngerRedirectsAttackMemoryWithoutForcingMovement(GameTestHelper h){
        var source=caster(h);var piglin=mob(h,EntityType.PIGLIN,source.position().add(1,0,0));var cow=mob(h,EntityType.COW,piglin.position().add(1,0,0));
        try{piglin.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));EmotionService.apply(source,piglin,"a:zinc",Emotion.ANGER);EmotionService.behave(piglin);
            h.assertTrue(EmotionService.nativeAnger(piglin) && !EmotionService.forcesBehavior(piglin) && piglin.getTarget()==cow && piglin.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null)==cow,"Brain attacker receives nearest target memory with its native combat behavior intact");
            piglin.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);h.assertTrue(piglin.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null)==cow,"Native brain invalid-target clearing cannot discard the forced anger target");
            piglin.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET,source);h.assertTrue(piglin.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null)==cow,"Native brain target replacement remains bound to nearest mob");
            var goal=new AngerTargetGoal(piglin);goal.tick();EmotionService.clear(piglin);goal.stop();h.assertTrue(!piglin.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET),"Ending anger removes its assigned brain target");
        }finally{EmotionService.clear(piglin);piglin.discard();cow.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void fiveEmotionsCompeteAnimalsAreWeakerAndBrassSuppresses(GameTestHelper h){
        var source=caster(h);var p=caster(h);p.setPos(source.position());var cow=mob(h,EntityType.COW,source.position());
        h.assertTrue(EmotionService.apply(source,p,"a:zinc",Emotion.ANGER),"Zinc can riot anger");
        h.assertTrue(EmotionService.dominant(p)==Emotion.ANGER && Math.abs(EmotionService.intensity(p)-1)<.001,"Highest emotion dictates state");
        EmotionService.apply(source,cow,"a:zinc",Emotion.ANGER);h.assertTrue(EmotionService.intensity(cow)<EmotionService.intensity(p),"Animals have a lesser response");
        EmotionService.apply(source,p,"a:brass",Emotion.ANGER);h.assertTrue(EmotionService.dominant(p)==Emotion.TRUST,"Equal Soothing counters Rioting and changes the dominant emotion");
        for(var emotion:Emotion.values()){EmotionService.clear(p);EmotionService.apply(source,p,"a:zinc",emotion);h.assertTrue(EmotionService.dominant(p)==emotion,"Each requested emotion can dominate: "+emotion);}
        EmotionService.clear(p);for(var emotion:Emotion.values())EmotionService.apply(source,p,"a:brass",emotion);
        h.assertTrue(EmotionService.dominant(p)==null,"Suppressing every emotion to zero must not create spurious anger");EmotionService.clear(p);EmotionService.clear(cow);h.succeed();
    }
    @GameTest(template="empty") public static void emotionalCombatModifiersApplyAndCleanUp(GameTestHelper h){
        var source=caster(h);var p=caster(h);p.setPos(source.position());double attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE),speed=p.getAttributeValue(Attributes.ATTACK_SPEED),movement=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        EmotionService.apply(source,p,"a:zinc",Emotion.ANGER);EmotionService.updatePlayer(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE)-attack*1.25)<.001,"Anger gives25percent melee attack damage at unit strength");
        EmotionService.clear(p);EmotionService.apply(source,p,"a:zinc",Emotion.FEAR);EmotionService.updatePlayer(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE)-attack*.7)<.001 && Math.abs(p.getAttributeValue(Attributes.ATTACK_SPEED)-speed*.7)<.001 && p.getAttributeValue(Attributes.MOVEMENT_SPEED)>movement,"Fear grants speed burst and30percent damage/attack-speed reduction");
        EmotionService.clear(p);h.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE)==attack && p.getAttributeValue(Attributes.ATTACK_SPEED)==speed && p.getAttributeValue(Attributes.MOVEMENT_SPEED)==movement,"Cleanup restores only our transient modifiers");h.succeed();
    }
    @GameTest(template="empty") public static void copperBlocksEmotionsAndLoveProtectsOnlyItsSource(GameTestHelper h){
        var source=caster(h);var p=caster(h);p.setPos(source.position());PowerAttributes.set(p,"a:copper",1);PowerData.reserve(p,"copper",100);PowerData.state(p,"a:copper",2);
        h.assertTrue(!EmotionService.apply(source,p,"a:zinc",Emotion.LOVE),"Real zinc/brass respects copper emotional protection gateway");
        PowerData.state(p,"a:copper",0);PowerData.state(source,"a:zinc",3);EmotionService.apply(source,p,"a:zinc",Emotion.LOVE);
        h.assertTrue(Math.abs(EmotionService.loveReduction(p,source)-.25)<.001,"Baseline Flaring love reduces damage to its user25percent");
        var stranger=caster(h);h.assertTrue(EmotionService.loveReduction(p,stranger)==0,"Love does not grant blanket damage reduction against others");
        EmotionService.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void angryPassiveMobsAttackAndLoveBreedsOnlyEligibleAdults(GameTestHelper h){
        var source=caster(h);var cow=mob(h,EntityType.COW,source.position().add(1,0,0));var other=mob(h,EntityType.SHEEP,cow.position().add(1,0,0));float health=other.getHealth();
        EmotionService.apply(source,cow,"a:zinc",Emotion.ANGER);cow.tickCount=20;EmotionService.behave(cow);
        h.assertTrue(cow.getTarget()==other && other.getHealth()<health,"Even passive angry mobs can attack nearby mobs");
        EmotionService.clear(cow);EmotionService.apply(source,cow,"a:zinc",Emotion.LOVE);EmotionService.behave(cow);h.assertTrue(cow.isInLove(),"Love triggers vanilla adult breeding readiness");
        var baby=mob(h,EntityType.COW,source.position());baby.setAge(-1000);EmotionService.apply(source,baby,"a:zinc",Emotion.LOVE);EmotionService.behave(baby);h.assertTrue(!baby.isInLove(),"Babies cannot be forced to breed");
        var cooling=mob(h,EntityType.COW,source.position());cooling.setAge(6000);EmotionService.apply(source,cooling,"a:zinc",Emotion.LOVE);EmotionService.behave(cooling);h.assertTrue(!cooling.isInLove(),"Vanilla breeding cooldown remains authoritative");h.succeed();
    }
    @GameTest(template="empty") public static void trustAffectsAvoidanceAndVillagerSessionPrices(GameTestHelper h){
        var source=caster(h);var cat=mob(h,EntityType.OCELOT,source.position().add(1,0,0));EmotionService.apply(source,cat,"a:zinc",Emotion.TRUST);
        h.assertTrue(EmotionService.trusts(cat,source) && EmotionService.calmPanic(cat),"Trust suppresses avoidance and injury panic around the influencing player");
        var v=mob(h,EntityType.VILLAGER,source.position());v.setVillagerData(v.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.FARMER));v.getOffers().clear();var offer=new MerchantOffer(new ItemCost(Items.EMERALD,20),new ItemStack(Items.BREAD),16,1,.05f);v.getOffers().add(offer);
        EmotionService.apply(source,v,"a:zinc",Emotion.TRUST);int before=offer.getCostA().getCount();
        v.mobInteract(source,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(offer.getCostA().getCount()<before,"Real vanilla trading entry invokes session-only trust price mixin");h.succeed();
    }
    @GameTest(template="empty") public static void nearbyInspectionRequiresBurningAndRespectsWallsAndAccess(GameTestHelper h){
        var p=caster(h);p.setYRot(-90);p.setXRot(15);var cow=mob(h,EntityType.COW,p.position().add(4,0,0));
        var view=EmotionService.inspect(p);h.assertTrue(view.stream().anyMatch(t->t.entity()==cow.getId() && t.emotion()==Emotion.TRUST.ordinal()),"Nearby unmodified passive mob shows its current natural state");
        EmotionService.apply(p,cow,"a:zinc",Emotion.ANGER);h.assertTrue(EmotionService.inspect(p).stream().anyMatch(t->t.entity()==cow.getId() && t.emotion()==Emotion.ANGER.ordinal()),"Inspection updates after actual emotional manipulation");
        var packet=new EmotionPacket(p.level().dimension().location().toString(),0,1,-1,0,EmotionService.inspect(p));var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{EmotionPacket.CODEC.encode(buffer,packet);h.assertTrue(EmotionPacket.CODEC.decode(buffer).equals(packet),"Private target status safely round trips");}finally{buffer.release();}
        var wall=net.minecraft.core.BlockPos.containing(p.position().add(2,0,0));for(int y=0;y<3;y++)h.getLevel().setBlock(wall.above(y),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);h.assertTrue(EmotionService.inspect(p).isEmpty(),"Inspection cannot reveal mobs through walls");for(int y=0;y<3;y++)h.getLevel().setBlock(wall.above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        p.setYRot(90);h.assertTrue(!EmotionService.inspect(p).isEmpty(),"Looking away keeps nearby indicators visible");PowerData.state(p,"a:zinc",0);PowerData.state(p,"a:brass",0);h.assertTrue(EmotionService.inspect(p).isEmpty(),"Owning powers without burning grants no readout");PowerData.state(p,"a:zinc",2);p.setYRot(-90);boolean enabled=EmotionConfig.INDICATOR.get();try{EmotionConfig.INDICATOR.set(false);h.assertTrue(EmotionService.inspect(p).isEmpty(),"Server can disable emotion inspection");}finally{EmotionConfig.INDICATOR.set(enabled);}
        PowerAttributes.set(p,"a:zinc",0);PowerAttributes.set(p,"a:brass",0);h.assertTrue(EmotionService.inspect(p).isEmpty(),"No owned emotional power grants no target readout");cow.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nearbyInspectionIncludesMultipleMobsAndPlayersButNotSelf(GameTestHelper h){
        var p=caster(h);var first=mob(h,EntityType.COW,p.position().add(4,0,0));var second=mob(h,EntityType.COW,p.position().add(-4,0,0));
        var player=caster(h);player.setPos(p.position().add(0,0,4));h.getLevel().addNewPlayer(player);
        try{
            var view=EmotionService.inspect(p);
            h.assertTrue(view.stream().anyMatch(t->t.entity()==first.getId()) && view.stream().anyMatch(t->t.entity()==second.getId()) && view.stream().anyMatch(t->t.entity()==player.getId()),"All nearby mobs and players are reported together without aiming");
            h.assertTrue(view.stream().noneMatch(t->t.entity()==p.getId()),"Viewer never receives their own overhead label");
            PowerAttributes.set(player,"a:copper",1);PowerData.reserve(player,"copper",100);PowerData.state(player,"a:copper",2);
            h.assertTrue(EmotionService.inspect(p).stream().noneMatch(t->t.entity()==player.getId()),"Copper protection also hides player readout");
            PowerData.reserve(p,"zinc",0);PowerData.reserve(p,"brass",0);h.assertTrue(EmotionService.inspect(p).isEmpty(),"Empty reserves disable all overhead readouts");
        }finally{first.discard();second.discard();h.getLevel().removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);}
        h.succeed();
    }
    @GameTest(template="empty") public static void modesFuelBoundsDisablementAndPacketPersistCorrectly(GameTestHelper h){
        var p=caster(h);PowerData.assignSlot(p,"a:zinc",0);PowerService.control(p,new PowerNetwork.Control(10,"a:zinc",4));h.assertTrue(EmotionService.mode(p,"a:zinc")==4,"Mode control uses existing owned power controls");
        PowerService.control(p,new PowerNetwork.Control(10,"a:zinc",99));h.assertTrue(EmotionService.mode(p,"a:zinc")==4,"Invalid mode cannot change saved selection");
        var copy=caster(h);PowerData.clonePlayer(p,copy);h.assertTrue(EmotionService.mode(copy,"a:zinc")==4 && EmotionService.strength(copy,"a:zinc")==0,"Mode/attributes/fuel survive respawn but burning stops");
        boolean enabled=EmotionConfig.ZINC.get();try{EmotionConfig.ZINC.set(false);EmotionService.tickFuel(p);h.assertTrue(PowerData.reserve(p,"zinc")==100,"Disabled power preserves reserves");}finally{EmotionConfig.ZINC.set(enabled);}
        PowerData.reserve(p,"zinc",.001);EmotionService.tickFuel(p);h.assertTrue(PowerData.state(p,"a:zinc")==0,"Depletion stops zinc");
        var packet=new EmotionPacket(p.level().dimension().location().toString(),4,2,3,2);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{EmotionPacket.CODEC.encode(buffer,packet);h.assertTrue(EmotionPacket.CODEC.decode(buffer).equals(packet),"Five-mode status packet round trips");}finally{buffer.release();}h.succeed();
    }
}
