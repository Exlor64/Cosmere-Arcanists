package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElectrumFeruchemyGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Determined")){
        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source,amount));try{actuallyHurt(source,amount);return true;}finally{damageContainers.pop();}}
    };p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:electrum",1);return p;}
    private static ItemStack piece(String metal){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_metalmind_ring")));}
    private static ItemStack mind(FakePlayer p,double charge){var item=piece("electrum");if(charge>0)Metalminds.transfer(item,p.getUUID(),"electrum",charge);p.setItemInHand(InteractionHand.OFF_HAND,item);return item;}
    private static void tap(FakePlayer p,int level){PowerData.state(p,"f:electrum",level);ElectrumFeruchemy.tick(p);}
    private static void close(GameTestHelper h,double value,double expected,String message){h.assertTrue(Math.abs(value-expected)<1e-5,message+": "+value+" expected "+expected);}
    @GameTest(template="empty") public static void electrumDamageReductionScalesAndNeverExceedsHalf(GameTestHelper h){
        var p=player(h);mind(p,100);tap(p,1);p.hurt(p.damageSources().generic(),10);close(h,p.getHealth(),10.5,"Tap1 reduces native damage by5percent");p.setHealth(20);tap(p,10);p.hurt(p.damageSources().generic(),10);close(h,p.getHealth(),15,"Tap10 halves native damage");
        PowerAttributes.set(p,"f:electrum",4);tap(p,40);p.setHealth(20);p.hurt(p.damageSources().generic(),10);close(h,p.getHealth(),15,"Higher tapping cannot exceed50percent mitigation");p.setHealth(20);p.hurt(p.damageSources().fellOutOfWorld(),10);close(h,p.getHealth(),10,"Invulnerability-bypassing damage retains its native behavior");ElectrumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void electrumComposesWithArmorResistanceAndGoldRescue(GameTestHelper h){
        var plain=player(h);var protectedPlayer=player(h);for(var p:new FakePlayer[]{plain,protectedPlayer}){p.getAttribute(Attributes.ARMOR).setBaseValue(8);p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,0));}mind(protectedPlayer,100);tap(protectedPlayer,10);plain.hurt(plain.damageSources().generic(),10);protectedPlayer.hurt(protectedPlayer.damageSources().generic(),10);close(h,20-protectedPlayer.getHealth(),(20-plain.getHealth())*.5,"Electrum halves damage remaining after native armor and Resistance");
        var p=player(h);mind(p,100);tap(p,10);var gold=piece("gold");Metalminds.transfer(gold,p.getUUID(),"gold",100);p.setItemInHand(InteractionHand.MAIN_HAND,gold);PowerAttributes.set(p,"f:gold",2);PowerData.state(p,"f:gold",11);p.setHealth(5);p.hurt(p.damageSources().generic(),20);close(h,p.getHealth(),1,"Gold rescue evaluates reduced lethal damage");close(h,Metalminds.charge(gold),70,"Rescue pays5times remaining overkill after electrum mitigation");ElectrumFeruchemy.clear(p);ElectrumFeruchemy.clear(protectedPlayer);h.succeed();
    }
    @GameTest(template="empty") public static void electrumStorageAndPartialFuelRespectOwnershipAndContact(GameTestHelper h){
        var p=player(h);var item=mind(p,0);PowerData.state(p,"f:electrum",-3);for(int i=0;i<20;i++)ElectrumFeruchemy.tick(p);close(h,Metalminds.charge(item),.3,"Storing conserves determination seconds");close(h,ElectrumFeruchemy.damageReduction(p),0,"Storing grants no mitigation");
        Metalminds.transfer(item,p.getUUID(),"electrum",-10000);Metalminds.transfer(item,p.getUUID(),"electrum",.001);tap(p,10);h.assertTrue(ElectrumFeruchemy.damageReduction(p)>0&&ElectrumFeruchemy.damageReduction(p)<.05,"Final fractional fuel grants proportional mitigation");ElectrumFeruchemy.tick(p);close(h,ElectrumFeruchemy.damageReduction(p),0,"Unfunded selection stops protecting");
        Metalminds.transfer(item,UUID.randomUUID(),"electrum",10);tap(p,10);close(h,ElectrumFeruchemy.damageReduction(p),0,"Foreign keyed charge cannot fund protection");item=mind(p,10);tap(p,10);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);close(h,ElectrumFeruchemy.damageReduction(p),0,"Lost contact removes protection immediately");p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:electrum",0);close(h,ElectrumFeruchemy.damageReduction(p),0,"Turning off invalidates cached paid protection");ElectrumFeruchemy.clear(p);h.succeed();
    }
    private static double knockback(FakePlayer p){p.setDeltaMovement(Vec3.ZERO);p.knockback(1,1,0);return p.getDeltaMovement().horizontalDistance();}
    @GameTest(template="empty") public static void electrumKnockbackAndMiningComposeWithIron(GameTestHelper h){
        var p=player(h);var item=mind(p,100);double baseline=knockback(p);PowerData.state(p,"f:electrum",-3);ElectrumFeruchemy.tick(p);close(h,knockback(p),baseline*1.15,"Storing increases knockback susceptibility");close(h,p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)/p.getAttribute(Attributes.BLOCK_BREAK_SPEED).getBaseValue(),.85,"Storing modestly reduces mining speed");tap(p,10);close(h,knockback(p),baseline*.75,"Tap10 reduces native knockback by25percent");
        var iron=new ItemStack(Items.IRON_INGOT);Metalminds.transfer(iron,p.getUUID(),"iron",100);p.setItemInHand(InteractionHand.MAIN_HAND,iron);PowerAttributes.set(p,"f:iron",1);PowerData.state(p,"f:iron",10);IronFeruchemy.tick(p);close(h,knockback(p),baseline*.75*.8,"Electrum and iron reduce knockback independently");ElectrumFeruchemy.clear(p);close(h,knockback(p),baseline*.8,"Electrum cleanup preserves iron's effect");IronFeruchemy.mass(p,1);h.succeed();
    }
    @GameTest(template="empty") public static void electrumShortensOnlySlownessWithoutRemovingHiddenOrInfiniteEffects(GameTestHelper h){
        var p=player(h);mind(p,100);var slow=new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,0);slow.update(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,2));p.addEffect(slow);p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,200,0));PowerData.state(p,"f:electrum",10);for(int i=0;i<20;i++)ElectrumFeruchemy.tick(p);var visible=p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var hidden=((dev.cosmerearcanists.mixin.BronzeEffectAccess)visible).cosmerearcanists$hiddenEffect();h.assertTrue(visible.getDuration()==80&&visible.getAmplifier()==2&&hidden!=null&&hidden.getDuration()==180,"Tap10 adds one extra aging tick to visible and hidden Slowness per funded tick");h.assertTrue(p.getEffect(MobEffects.DIG_SLOWDOWN).getDuration()==200,"Other debuffs remain unchanged");
        PowerData.state(p,"f:electrum",0);ElectrumFeruchemy.tick(p);p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,-1,0));tap(p,10);h.assertTrue(p.getEffect(MobEffects.MOVEMENT_SLOWDOWN).isInfiniteDuration(),"Infinite Slowness remains infinite");ElectrumFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void electrumSecondWindRequiresLowHealthAndGatesPreserveReserves(GameTestHelper h){
        var p=player(h);var item=mind(p,100);tap(p,10);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED)/p.getAttribute(Attributes.ATTACK_SPEED).getBaseValue(),1,"Full health gives no second wind");p.setHealth(6);tap(p,9);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED)/p.getAttribute(Attributes.ATTACK_SPEED).getBaseValue(),1,"Low tapping gives no second wind");tap(p,10);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED)/p.getAttribute(Attributes.ATTACK_SPEED).getBaseValue(),1.2,"Tap10 at30percent health gives20percent faster attack recovery");close(h,p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)/p.getAttribute(Attributes.BLOCK_BREAK_SPEED).getBaseValue(),1.2,"Second wind also aids mining");
        boolean enabled=FeruchemyConfig.ELECTRUM.get(),damage=FeruchemyConfig.ELECTRUM_DAMAGE.get();try{FeruchemyConfig.ELECTRUM_DAMAGE.set(false);close(h,ElectrumFeruchemy.damageReduction(p),0,"Damage bonus has an independent toggle");FeruchemyConfig.ELECTRUM.set(false);ElectrumFeruchemy.tick(p);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED)/p.getAttribute(Attributes.ATTACK_SPEED).getBaseValue(),1,"Disabling electrum cleans transient bonuses");h.assertTrue(Metalminds.charge(item)>0,"Config changes preserve saved charge");}finally{FeruchemyConfig.ELECTRUM.set(enabled);FeruchemyConfig.ELECTRUM_DAMAGE.set(damage);ElectrumFeruchemy.clear(p);}PowerAttributes.set(p,"f:electrum",1.5);h.assertTrue(ElectrumFeruchemy.storeLevels(p)==5&&ElectrumFeruchemy.tapLevels(p)==15,"Fractional attributes determine electrum levels");h.succeed();
    }
}
