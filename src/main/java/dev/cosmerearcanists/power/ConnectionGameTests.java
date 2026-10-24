package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.connection.*;
import dev.cosmerearcanists.feruchemy.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConnectionGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Connector")){@Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}};p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));PowerAttributes.set(p,"f:duralumin",1);return p;}
    private static ItemStack mind(FakePlayer p,double charge){var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"duralumin_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,item);if(charge>0)Metalminds.transfer(item,p.getUUID(),"duralumin",charge);return item;}
    private static void tap(FakePlayer p,int level){PowerData.state(p,"f:duralumin",level);DuraluminFeruchemy.tick(p);}
    private static void close(GameTestHelper h,double a,double b,String message){h.assertTrue(Math.abs(a-b)<1e-6,message+": "+a+" != "+b);}
    @GameTest(template="empty") public static void connectionChargeLevelsPartialPaymentAndOwnership(GameTestHelper h){
        var p=player(h);var item=mind(p,0);PowerData.state(p,"f:duralumin",-3);for(int i=0;i<20;i++)DuraluminFeruchemy.tick(p);close(h,Metalminds.charge(item),.3,"Storage conserves Connection-seconds");close(h,DuraluminFeruchemy.connectionMultiplier(p),.7,"Storing suppresses expressed Connection");
        Metalminds.transfer(item,p.getUUID(),"duralumin",-10000);Metalminds.transfer(item,p.getUUID(),"duralumin",.001);tap(p,10);h.assertTrue(DuraluminFeruchemy.fundedLevels(p)>0&&DuraluminFeruchemy.fundedLevels(p)<1,"Fractional last payment scales the output");DuraluminFeruchemy.tick(p);close(h,DuraluminFeruchemy.fundedLevels(p),0,"Empty selection gives no effect");
        Metalminds.transfer(item,UUID.randomUUID(),"duralumin",10);tap(p,10);close(h,DuraluminFeruchemy.fundedLevels(p),0,"Foreign keyed reserves cannot fund Connection");item=mind(p,100);tap(p,10);close(h,DuraluminFeruchemy.connectionMultiplier(p),2.5,"Tap10 gives150percent extra Connection");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);close(h,DuraluminFeruchemy.connectionMultiplier(p),1,"Lost contact invalidates paid output");
        PowerAttributes.set(p,"f:duralumin",1.5);h.assertTrue(DuraluminFeruchemy.storeLevels(p)==5&&DuraluminFeruchemy.tapLevels(p)==15,"Fractional attributes set selector limits");DuraluminFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void organicConnectionsAreSymmetricPersistentAndRateLimited(GameTestHelper h){
        var a=player(h);var b=player(h);PowerAttributes.set(a,"f:duralumin",0);PowerAttributes.set(b,"f:duralumin",0);h.assertTrue(ConnectionService.interaction(a,b),"Players form a relationship without either owning a power or accepting an invitation");var data=ConnectionBonds.get(a.server);close(h,data.ledger.strength(a.getUUID(),b.getUUID()),2,"First interaction contributes points");h.assertTrue(!ConnectionService.interaction(b,a),"Reversing participants cannot bypass the shared cooldown");h.assertTrue(ConnectionService.nearbyPair(a,b)&&!ConnectionService.nearbyPair(b,a),"Nearby growth occurs once per pair per server second");
        double strength=data.ledger.strength(a.getUUID(),b.getUUID());close(h,data.ledger.strength(b.getUUID(),a.getUUID()),strength,"Both directions share the same bond");var loaded=ConnectionBonds.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());close(h,loaded.ledger.strength(a.getUUID(),b.getUUID()),strength,"Relationship strength survives world SavedData reload");h.assertTrue(!loaded.ledger.grow(a.getUUID(),b.getUUID(),2,a.server.overworld().getGameTime(),true,600,64,16384),"The interaction cooldown also survives reload");
        boolean enabled=ConnectionConfig.BONDS.get();try{ConnectionConfig.BONDS.set(false);h.assertTrue(!ConnectionService.interaction(a,b)&&data.ledger.strength(a.getUUID(),b.getUUID())==strength,"Disabling accumulation retains history");}finally{ConnectionConfig.BONDS.set(enabled);ConnectionService.forgetActivity(a);ConnectionService.forgetActivity(b);}h.succeed();
    }
    @GameTest(template="empty") public static void nativeCombatCreatesConnectionsForRivalsAndHelpers(GameTestHelper h){
        var attacker=player(h);var victim=player(h);PowerAttributes.set(attacker,"f:duralumin",0);PowerAttributes.set(victim,"f:duralumin",0);victim.hurt(victim.damageSources().playerAttack(attacker),2);var data=ConnectionBonds.get(victim.server);h.assertTrue(data.ledger.strength(attacker.getUUID(),victim.getUUID())>0,"Real damage creates an organic Connection between rivals without either having the power");
        var helper=player(h);var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setPos(victim.position().add(1,0,0));zombie.setTarget(victim);zombie.hurt(zombie.damageSources().playerAttack(helper),2);h.assertTrue(data.ledger.strength(helper.getUUID(),victim.getUUID())>0,"Helping against a mob targeting another player also forms Connection");ConnectionService.forgetActivity(attacker);ConnectionService.forgetActivity(victim);ConnectionService.forgetActivity(helper);h.succeed();
    }
    @GameTest(template="empty") public static void sensingRequiresPaidTappingBondStrengthAndTargetPresence(GameTestHelper h){
        var p=player(h);var target=player(h);target.setPos(p.position().add(50,0,0));var item=mind(p,100);var data=ConnectionBonds.get(p.server);var pair=new ConnectionLedger.Pair(p.getUUID(),target.getUUID());data.ledger.restore(pair,new ConnectionLedger.Bond(40,0,0));data.setDirty();java.util.function.Function<UUID,net.minecraft.server.level.ServerPlayer> online=id->id.equals(target.getUUID())?target:null;
        tap(p,1);h.assertTrue(ConnectionService.sensed(p,online).isEmpty(),"Low tapping cannot sense a weak bond");tap(p,10);var sensed=ConnectionService.sensed(p,online);h.assertTrue(sensed.size()==1&&Math.abs(sensed.getFirst().distance()-50)<1e-6,"Higher tapping detects the existing bond without target approval");h.assertTrue(ConnectionService.sensed(p,id->null).isEmpty(),"Offline players cannot be sensed");
        mind(target,0);PowerData.state(target,"f:duralumin",-3);DuraluminFeruchemy.tick(target);h.assertTrue(ConnectionService.sensed(p,online).isEmpty(),"Paid target storage suppresses the expressed bond");close(h,data.ledger.strength(p.getUUID(),target.getUUID()),40,"Suppression never erases relationship history");PowerData.state(target,"f:duralumin",0);h.assertTrue(ConnectionService.sensed(p,online).size()==1,"Turning suppression off restores sensing eligibility");
        target.setGameMode(GameType.SPECTATOR);h.assertTrue(ConnectionService.sensed(p,online).isEmpty(),"Spectators cannot be tracked");target.setGameMode(GameType.SURVIVAL);target.setPos(p.position().add(5000,0,0));h.assertTrue(ConnectionService.sensed(p,online).isEmpty(),"Sensing obeys its range");target.setPos(p.position().add(50,0,0));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(ConnectionService.sensed(p,online).isEmpty(),"Lost metalmind contact invalidates sensing immediately");p.setItemInHand(InteractionHand.OFF_HAND,item);DuraluminFeruchemy.clear(p);DuraluminFeruchemy.clear(target);h.succeed();
    }
    @GameTest(template="empty") public static void connectionTradingUsesNativeSessionsAndRevertsWithoutCompounding(GameTestHelper h){
        var p=player(h);mind(p,100);tap(p,10);var v=EntityType.VILLAGER.create(h.getLevel());v.setPos(p.position());v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.FARMER));v.getOffers().clear();var offer=new MerchantOffer(new ItemCost(Items.EMERALD,20),new ItemStack(Items.BREAD),16,1,0f);var exchange=new MerchantOffer(new ItemCost(ModMetals.CLIP.get(),20),new ItemStack(ModMetals.BOXING.get()),16,1,0f);v.getOffers().add(offer);v.getOffers().add(exchange);v.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(offer.getCostA().getCount()==16&&exchange.getSpecialPriceDiff()==0,"Native trade opening applies a20percent discount and excludes currency exchanges");int before=offer.getSpecialPriceDiff();ConnectionTrades.tick();ConnectionTrades.tick();h.assertTrue(offer.getSpecialPriceDiff()==before,"Refresh never compounds its own discount");PowerData.state(p,"f:duralumin",0);ConnectionTrades.tick();h.assertTrue(offer.getCostA().getCount()==20,"Turning off removes only the Connection adjustment during the same trade");PowerData.state(p,"f:duralumin",-3);DuraluminFeruchemy.tick(p);ConnectionTrades.tick();h.assertTrue(offer.getCostA().getCount()==23,"Storing30percent adds a15percent session price penalty");
        v.setTradingPlayer(null);ConnectionTrades.tick();DuraluminFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void connectionChangesInitialDetectionAndAvoidanceWithoutPacifyingAggression(GameTestHelper h){
        var p=player(h);mind(p,100);PowerData.state(p,"f:duralumin",-3);DuraluminFeruchemy.tick(p);var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);zombie.setPos(p.position().add(30,0,0));zombie.setTarget(p);h.assertTrue(zombie.getTarget()==null,"Storing reduces initial hostile acquisition distance");zombie.setPos(p.position().add(5,0,0));zombie.setTarget(p);h.assertTrue(zombie.getTarget()==p,"Close hostiles can still acquire the player");zombie.setPos(p.position().add(30,0,0));zombie.setTarget(p);h.assertTrue(zombie.getTarget()==p,"Storing never erases existing aggression");zombie.setTarget(null);zombie.setLastHurtByMob(p);zombie.setTarget(p);h.assertTrue(zombie.getTarget()==p,"Direct retaliation is retained");
        tap(p,10);var rabbit=EntityType.RABBIT.create(h.getLevel());rabbit.setPos(p.position().add(6,0,0));h.assertTrue(ConnectionService.ignoreAvoidance(rabbit,p,8),"Tap10 reduces an8block avoidance radius to4blocks");rabbit.setPos(p.position().add(2,0,0));h.assertTrue(!ConnectionService.ignoreAvoidance(rabbit,p,8),"The animal may still flee if approached too closely");rabbit.setPos(p.position().add(6,0,0));rabbit.setLastHurtByMob(p);h.assertTrue(!ConnectionService.ignoreAvoidance(rabbit,p,8),"Recent damage retains natural avoidance");DuraluminFeruchemy.clear(p);h.succeed();
    }
    @GameTest(template="empty") public static void connectionEnhancementConfigurationAndPacketBounds(GameTestHelper h){
        var p=player(h);var item=mind(p,100);PowerAttributes.set(p,"a:duralumin",1);PowerData.reserve(p,"duralumin",100);PowerData.state(p,"a:duralumin",1);EnhancementService.begin(p);double before=Metalminds.charge(item);tap(p,10);close(h,DuraluminFeruchemy.fundedLevels(p),17.5,"Allomantic low duralumin amplifies Connection output");close(h,before-Metalminds.charge(item),1.5*Math.pow(1.5,.25)/20*5,"Amplified tapping pays the shared fivefold consumption profile");
        boolean enabled=ConnectionConfig.ENABLED.get();try{ConnectionConfig.ENABLED.set(false);DuraluminFeruchemy.tick(p);close(h,DuraluminFeruchemy.connectionMultiplier(p),1,"Disabling Connection clears paid output");h.assertTrue(Metalminds.charge(item)>0,"Disabling preserves charge");}finally{ConnectionConfig.ENABLED.set(enabled);EnhancementService.clear(p);DuraluminFeruchemy.clear(p);}
        var packet=new ConnectionPacket(p.level().dimension().location().toString(),List.of(new ConnectionPacket.Sensed("Friend",new Vec3(1,0,0),50)));var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{ConnectionPacket.CODEC.encode(buf,packet);h.assertTrue(ConnectionPacket.CODEC.decode(buf).equals(packet),"Connection compass packet round trips");}finally{buf.release();}boolean rejected=false;try{new ConnectionPacket.Sensed("Invalid",new Vec3(Double.NaN,0,0),5);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Malformed directions are rejected");rejected=false;try{new ConnectionPacket("minecraft:overworld",Collections.nCopies(9,packet.players().getFirst()));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Packet player counts remain bounded");h.succeed();
    }
}
