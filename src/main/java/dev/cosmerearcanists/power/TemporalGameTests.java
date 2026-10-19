package dev.cosmerearcanists.power;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TemporalGameTests {
    private static FakePlayer player(GameTestHelper h,String name,Vec3 pos,String metal) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),name));p.setPos(pos);p.setOnGround(true);
        PowerAttributes.set(p,"a:"+metal,1);PowerData.reserve(p,metal,100);PowerData.state(p,"a:"+metal,2);PowerData.assignSlot(p,"a:"+metal,0);PowerService.control(p,new PowerNetwork.Control(2,"",1));return p;
    }
    private static Vec3 origin(GameTestHelper h){return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1,20,1)));}
    private static void floor(GameTestHelper h){
        // The tiny shared template does not clear a full flight arena. Stay above test infrastructure.
        for(int x=0;x<7;x++)for(int z=0;z<5;z++) {h.setBlock(new BlockPos(x,19,z),Blocks.STONE);for(int y=20;y<29;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);}
    }
    @GameTest(template="empty") public static void alloyPowerPersistsAndFuelAndConfigGateEffects(GameTestHelper h) {
        var p=player(h,"AlloySeer",origin(h),"atium");
        h.assertTrue(PowerData.available(p).contains("a:atium") && !PowerAttributes.keys().contains("f:atium"),"Alloy gets its own Allomantic attribute without inventing god-metal Feruchemy");
        h.assertTrue(TemporalService.strength(p,"atium")==1,"Baseline alloy burn");
        var clone=player(h,"AlloyCopy",origin(h),"electrum");PowerData.clonePlayer(p,clone);
        h.assertTrue(PowerAttributes.strength(clone,"a:atium")==1 && PowerData.reserve(clone,"atium")==100 && PowerData.state(clone,"a:atium")==0,"Respawn preserves alloy attribute/fuel but stops burning");
        boolean enabled=TemporalConfig.ATIUM.get();
        try {TemporalConfig.ATIUM.set(false);TemporalService.tick(p);h.assertTrue(TemporalService.strength(p,"atium")==0 && PowerData.reserve(p,"atium")==100,"Disabled powers preserve their saved fuel");}
        finally{TemporalConfig.ATIUM.set(enabled);}
        PowerData.reserve(p,"atium",.001);TemporalService.tick(p);
        h.assertTrue(PowerData.state(p,"a:atium")==0 && TemporalService.forecast(p).shadows().isEmpty(),"Exhaustion clears future sight immediately");
        PowerAttributes.set(p,"a:atium",0);h.assertTrue(!PowerData.available(p).contains("a:atium"),"Revocation is persistent, never restored by metadata");h.succeed();
    }
    @GameTest(template="empty") public static void flaredAtiumDodgesPhysicalHitsWithFuelHoldAndCounterGates(GameTestHelper h){
        var p=player(h,"Dodger",origin(h),"atium");var attacker=h.spawn(EntityType.ZOMBIE,new BlockPos(4,20,1));attacker.setNoAi(true);var source=p.damageSources().mobAttack(attacker);
        h.assertTrue(TemporalService.dodgeChance(p,source)==0,"Normal burning grants no dodge");
        PowerData.state(p,"a:atium",3);h.assertTrue(Math.abs(TemporalService.dodgeChance(p,source)-.1)<1e-9,"Baseline flare grants ten percent dodge");
        PowerService.control(p,new PowerNetwork.Control(2,"",0));h.assertTrue(TemporalService.dodgeChance(p,source)==0,"Released future sight grants no dodge");PowerService.control(p,new PowerNetwork.Control(2,"",1));
        PowerData.reserve(p,"atium",0);h.assertTrue(TemporalService.dodgeChance(p,source)==0,"Empty reserves grant no dodge");PowerData.reserve(p,"atium",100);
        var oracle=player(h,"CounterOracle",origin(h).add(3,0,0),"electrum");h.assertTrue(TemporalService.dodgeChance(p,p.damageSources().playerAttack(oracle))==0,"Active electrum counters automatic dodge");
        var arrow=new Arrow(EntityType.ARROW,h.getLevel());h.assertTrue(TemporalService.dodgeChance(p,p.damageSources().arrow(arrow,attacker))>0,"Physical projectiles are dodgeable");
        h.assertTrue(TemporalService.dodgeChance(p,p.damageSources().fall())==0 && TemporalService.dodgeChance(p,p.damageSources().lava())==0,"Falling and environmental hazards cannot be dodged");
        double base=TemporalConfig.DODGE_CHANCE.get(),cap=TemporalConfig.DODGE_CAP.get();boolean enabled=TemporalConfig.DODGE.get();
        try{TemporalConfig.DODGE_CHANCE.set(1.0);TemporalConfig.DODGE_CAP.set(1.0);float health=p.getHealth();p.hurt(source,4);h.assertTrue(p.getHealth()==health,"Successful dodge cancels the real incoming damage event");
            TemporalConfig.DODGE.set(false);p.invulnerableTime=0;p.hurt(source,4);h.assertTrue(p.getHealth()<health,"Disabling dodge restores incoming attack damage");
        }finally{TemporalConfig.DODGE_CHANCE.set(base);TemporalConfig.DODGE_CAP.set(cap);TemporalConfig.DODGE.set(enabled);attacker.discard();}
        h.succeed();
    }
    @GameTest(template="empty") public static void trajectoriesRespectWallsAndWarnAboutFallsAndLava(GameTestHelper h) {
        floor(h);var p=player(h,"OraclePath",origin(h),"electrum");
        h.setBlock(new BlockPos(3,20,1),Blocks.STONE);h.setBlock(new BlockPos(3,21,1),Blocks.STONE);
        var wall=TemporalService.trace(p,new Vec3(.5,0,0),20);double maximum=h.absolutePos(new BlockPos(3,20,1)).getX()-.29;
        h.assertTrue(wall.stream().allMatch(f->f.position().x<maximum),"Future self must not pass through a wall");
        p.setPos(origin(h).add(0,5,0));p.setOnGround(false);
        var fall=TemporalService.trace(p,new Vec3(0,-.5,0),30);
        h.assertTrue(fall.stream().anyMatch(f->(f.warning()&TemporalPacket.FALL)!=0),"Forecast accumulates fall distance and marks dangerous landing: "+fall+" ground="+p.onGround()+" fly="+p.getAbilities().flying+" noGravity="+p.isNoGravity());
        h.setBlock(new BlockPos(1,20,1),Blocks.WATER);
        var water=TemporalService.trace(p,new Vec3(0,-.5,0),30);
        h.assertTrue(water.stream().noneMatch(f->(f.warning()&TemporalPacket.FALL)!=0),"A water landing cancels projected fall injury");
        h.setBlock(new BlockPos(1,20,1),Blocks.AIR);
        p.setPos(origin(h));h.setBlock(new BlockPos(2,20,1),Blocks.LAVA);
        var lava=TemporalService.trace(p,new Vec3(.2,0,0),12);
        h.assertTrue(lava.stream().anyMatch(f->(f.warning()&TemporalPacket.HAZARD)!=0),"Own future must warn about lava");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,200));
        h.assertTrue(TemporalService.trace(p,new Vec3(.2,0,0),12).stream().noneMatch(f->(f.warning()&TemporalPacket.HAZARD)!=0),"Fire resistance prevents false lava injury warnings");h.succeed();
    }
    @GameTest(template="empty") public static void electrumPredictsHitsWithoutRevealingAttackers(GameTestHelper h) {
        floor(h);var p=player(h,"OracleHit",origin(h),"electrum");
        var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setPos(p.position().add(4,1,0));arrow.setDeltaMovement(-2,0,0);h.getLevel().addFreshEntity(arrow);
        var forecast=TemporalService.forecast(p);
        h.assertTrue(forecast.shadows().stream().allMatch(s->s.entity()==p.getId()),"Electrum sends only the user's future, never the attacker's identity/path");
        h.assertTrue(forecast.shadows().stream().flatMap(s->s.points().stream()).anyMatch(f->(f.warning()&TemporalPacket.IMPACT)!=0),"Crossing projectile must warn on future self: "+forecast+" arrow="+arrow.position()+" visible="+TemporalService.visible(p,arrow)+" velocity="+arrow.getDeltaMovement());
        PowerData.assignSlot(p,"a:electrum",0);PowerService.control(p,new PowerNetwork.Control(2,"",1));
        h.assertTrue(TemporalService.forecast(p).shadows().stream().anyMatch(s->s.kind()==TemporalPacket.JUMP),"Holding electrum's assigned activation previews a hypothetical grounded jump");
        PowerService.control(p,new PowerNetwork.Control(2,"",0));
        h.assertTrue(TemporalService.forecast(p).shadows().stream().noneMatch(s->s.kind()==TemporalPacket.JUMP),"Releasing activation removes the optional jump future");
        PowerData.state(p,"a:electrum",0);h.assertTrue(TemporalService.forecast(p).shadows().isEmpty(),"Off grants no forecasts");h.succeed();
    }
    @GameTest(template="empty") public static void futureSightBranchesAndHiddenTargetsStayPrivate(GameTestHelper h) {
        floor(h);var p=player(h,"Seer",origin(h),"atium");var other=player(h,"Oracle",origin(h).add(3,0,0),"electrum");
        h.assertTrue(TemporalService.interferes(other),"Active electrum splits an atium forecast");PowerData.state(other,"a:electrum",0);
        h.assertTrue(!TemporalService.interferes(other),"Merely possessing electrum does not counter atium");PowerAttributes.set(other,"a:atium",1);PowerData.reserve(other,"atium",10);PowerData.state(other,"a:atium",2);
        PowerData.assignSlot(other,"a:atium",0);PowerService.control(other,new PowerNetwork.Control(2,"",1));h.assertTrue(TemporalService.interferes(other),"Two actively held atium powers interfere too");
        var cow=h.spawn(EntityType.COW,new BlockPos(4,20,1));cow.setNoAi(true);
        h.assertTrue(TemporalService.forecast(p).shadows().stream().anyMatch(s->s.entity()==cow.getId()),"Visible entities have atium shadows: "+TemporalService.forecast(p)+" visible="+TemporalService.visible(p,cow)+" eye="+p.getEyePosition()+" cow="+cow.position()+" sight="+p.hasLineOfSight(cow));
        cow.setInvisible(true);h.assertTrue(!TemporalService.visible(p,cow),"Foresight must not reveal invisible targets");cow.setInvisible(false);
        h.setBlock(new BlockPos(3,20,1),Blocks.STONE);h.setBlock(new BlockPos(3,21,1),Blocks.STONE);h.setBlock(new BlockPos(3,22,1),Blocks.STONE);
        h.assertTrue(!TemporalService.visible(p,cow),"Foresight must not become a wallhack");h.succeed();
    }
    @GameTest(template="empty") public static void forecastsAndInterferenceRequireTheCorrectHeldSlot(GameTestHelper h){
        floor(h);var p=player(h,"HeldSight",origin(h),"electrum");PowerService.control(p,new PowerNetwork.Control(2,"",0));double reserve=PowerData.reserve(p,"electrum");TemporalService.tick(p);
        h.assertTrue(TemporalService.forecast(p).shadows().isEmpty() && !TemporalService.interferes(p) && PowerData.reserve(p,"electrum")==reserve,"Burning alone gives no forecasts, interference or normal drain");
        PowerData.assignSlot(p,"a:electrum",1);PowerService.control(p,new PowerNetwork.Control(2,"",1));h.assertTrue(TemporalService.forecast(p).shadows().isEmpty(),"Holding a different slot does not activate electrum");
        PowerService.control(p,new PowerNetwork.Control(7,"",1));h.assertTrue(!TemporalService.forecast(p).shadows().isEmpty() && TemporalService.interferes(p),"Secondary activation enables the assigned future sight");PowerService.control(p,new PowerNetwork.Control(7,"",0));h.assertTrue(TemporalService.forecast(p).shadows().isEmpty(),"Release removes all futures immediately");h.succeed();
    }
    @GameTest(template="empty") public static void electrumWarnsAboutFutureAggroWithoutRevealingTheMob(GameTestHelper h){
        floor(h);var p=player(h,"AggroOracle",origin(h),"electrum");p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setDeltaMovement(.2,0,0);
        var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setPos(origin(h).add(5,0,0));zombie.setNoGravity(true);zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(2);h.getLevel().addFreshEntity(zombie);
        var forecast=TemporalService.forecast(p);h.assertTrue(forecast.shadows().stream().allMatch(s->s.entity()==p.getId()),"Electrum aggro warnings must not reveal the hostile mob identity or path");
        h.assertTrue(forecast.shadows().stream().flatMap(s->s.points().stream()).anyMatch(f->(f.warning()&TemporalPacket.AGGRO)!=0),"Walking into an idle hostile mob's follow range warns on the future self");
        zombie.setNoAi(true);h.assertTrue(TemporalService.forecast(p).shadows().stream().flatMap(s->s.points().stream()).noneMatch(f->(f.warning()&TemporalPacket.AGGRO)!=0),"A mob with disabled AI cannot acquire aggro");
        zombie.setNoAi(false);zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(8);for(int y=20;y<24;y++)h.setBlock(new BlockPos(4,y,1),Blocks.STONE);p.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(TemporalService.forecast(p).shadows().stream().flatMap(s->s.points().stream()).noneMatch(f->(f.warning()&TemporalPacket.AGGRO)!=0),"A solid barrier blocks prospective aggro even within follow range");zombie.discard();
        var packet=new TemporalPacket(h.getLevel().dimension().location().toString(),5,List.of(new TemporalPacket.Shadow(p.getId(),TemporalPacket.SELF,.6f,1.8f,List.of(new TemporalPacket.Point(p.position(),TemporalPacket.AGGRO)))));
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{TemporalPacket.CODEC.encode(buffer,packet);h.assertTrue(TemporalPacket.CODEC.decode(buffer).equals(packet),"New aggro warning flag round trips safely");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty") public static void temporalCodecBoundsAndThirtyThreePowerSnapshot(GameTestHelper h) {
        var level=h.getLevel();var p=player(h,"ForecastCodec",origin(h),"electrum");
        var packet=TemporalService.forecast(p);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{TemporalPacket.CODEC.encode(buffer,packet);h.assertTrue(TemporalPacket.CODEC.decode(buffer).equals(packet),"Forecast packet preserves all samples/flags");}finally{buffer.release();}
        var list=PowerAttributes.keys().stream().map(key->new PowerNetwork.Entry(key,0,0)).toList();
        var snapshot=new PowerNetwork.Snapshot(level.dimension().location().toString(),"a:atium","a:electrum",5,list,List.of());
        buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{PowerNetwork.Snapshot.CODEC.encode(buffer,snapshot);h.assertTrue(PowerNetwork.Snapshot.CODEC.decode(buffer).equals(snapshot),"All 33 powers fit the selector protocol");}finally{buffer.release();}
        buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{buffer.writeUtf("minecraft:overworld",128);buffer.writeVarInt(5);buffer.writeVarInt(1000);boolean rejected=false;try{TemporalPacket.CODEC.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Oversized forecasts must be rejected before allocating shadows");}finally{buffer.release();}h.succeed();
    }
}
