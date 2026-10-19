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
    @GameTest(template="empty") public static void fiveEmotionsCompeteAnimalsAreWeakerAndBrassSuppresses(GameTestHelper h){
        var source=caster(h);var p=caster(h);p.setPos(source.position());var cow=mob(h,EntityType.COW,source.position());
        h.assertTrue(EmotionService.apply(source,p,"a:zinc",Emotion.ANGER),"Zinc can riot anger");
        h.assertTrue(EmotionService.dominant(p)==Emotion.ANGER && Math.abs(EmotionService.intensity(p)-1)<.001,"Highest emotion dictates state");
        EmotionService.apply(source,cow,"a:zinc",Emotion.ANGER);h.assertTrue(EmotionService.intensity(cow)<EmotionService.intensity(p),"Animals have a lesser response");
        EmotionService.apply(source,p,"a:brass",Emotion.ANGER);h.assertTrue(EmotionService.dominant(p)==Emotion.TRUST,"Equal Soothing counters Rioting and changes the dominant emotion");
        for(var emotion:Emotion.values()){EmotionService.clear(p);EmotionService.apply(source,p,"a:zinc",emotion);h.assertTrue(EmotionService.dominant(p)==emotion,"Each requested emotion can dominate: "+emotion);}
        EmotionService.clear(p);EmotionService.clear(cow);h.succeed();
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
    @GameTest(template="empty") public static void modesFuelBoundsDisablementAndPacketPersistCorrectly(GameTestHelper h){
        var p=caster(h);PowerData.assignSlot(p,"a:zinc",0);PowerService.control(p,new PowerNetwork.Control(10,"a:zinc",4));h.assertTrue(EmotionService.mode(p,"a:zinc")==4,"Mode control uses existing owned power controls");
        PowerService.control(p,new PowerNetwork.Control(10,"a:zinc",99));h.assertTrue(EmotionService.mode(p,"a:zinc")==4,"Invalid mode cannot change saved selection");
        var copy=caster(h);PowerData.clonePlayer(p,copy);h.assertTrue(EmotionService.mode(copy,"a:zinc")==4 && EmotionService.strength(copy,"a:zinc")==0,"Mode/attributes/fuel survive respawn but burning stops");
        boolean enabled=EmotionConfig.ZINC.get();try{EmotionConfig.ZINC.set(false);EmotionService.tickFuel(p);h.assertTrue(PowerData.reserve(p,"zinc")==100,"Disabled power preserves reserves");}finally{EmotionConfig.ZINC.set(enabled);}
        PowerData.reserve(p,"zinc",.001);EmotionService.tickFuel(p);h.assertTrue(PowerData.state(p,"a:zinc")==0,"Depletion stops zinc");
        var packet=new EmotionPacket(p.level().dimension().location().toString(),4,2,3,2);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{EmotionPacket.CODEC.encode(buffer,packet);h.assertTrue(EmotionPacket.CODEC.decode(buffer).equals(packet),"Five-mode status packet round trips");}finally{buffer.release();}h.succeed();
    }
}
