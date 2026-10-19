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
        PowerAttributes.set(p,"a:"+metal,1);PowerData.reserve(p,metal,100);PowerData.state(p,"a:"+metal,2);return p;
    }
    private static Vec3 origin(GameTestHelper h){return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1,2,1)));}
    private static void floor(GameTestHelper h){for(int x=0;x<7;x++)for(int z=0;z<5;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);}
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
    @GameTest(template="empty") public static void trajectoriesRespectWallsAndWarnAboutFallsAndLava(GameTestHelper h) {
        floor(h);var p=player(h,"OraclePath",origin(h),"electrum");
        h.setBlock(new BlockPos(3,2,1),Blocks.STONE);h.setBlock(new BlockPos(3,3,1),Blocks.STONE);
        var wall=TemporalService.trace(p,new Vec3(.5,0,0),20);double maximum=h.absolutePos(new BlockPos(3,2,1)).getX()-.29;
        h.assertTrue(wall.stream().allMatch(f->f.position().x<maximum),"Future self must not pass through a wall");
        p.setPos(origin(h).add(0,5,0));p.setOnGround(false);
        var fall=TemporalService.trace(p,new Vec3(0,-.5,0),30);
        h.assertTrue(fall.stream().anyMatch(f->(f.warning()&TemporalPacket.FALL)!=0),"Forecast accumulates fall distance and marks dangerous landing");
        p.setPos(origin(h));h.setBlock(new BlockPos(2,2,1),Blocks.LAVA);
        var lava=TemporalService.trace(p,new Vec3(.2,0,0),12);
        h.assertTrue(lava.stream().anyMatch(f->(f.warning()&TemporalPacket.HAZARD)!=0),"Own future must warn about lava");h.succeed();
    }
    @GameTest(template="empty") public static void electrumPredictsHitsWithoutRevealingAttackers(GameTestHelper h) {
        floor(h);var p=player(h,"OracleHit",origin(h),"electrum");
        var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setPos(p.position().add(4,1,0));arrow.setDeltaMovement(-2,0,0);h.getLevel().addFreshEntity(arrow);
        var forecast=TemporalService.forecast(p);
        h.assertTrue(forecast.shadows().stream().allMatch(s->s.entity()==p.getId()),"Electrum sends only the user's future, never the attacker's identity/path");
        h.assertTrue(forecast.shadows().stream().flatMap(s->s.points().stream()).anyMatch(f->(f.warning()&TemporalPacket.IMPACT)!=0),"Crossing projectile must warn on future self");
        PowerData.state(p,"a:electrum",0);h.assertTrue(TemporalService.forecast(p).shadows().isEmpty(),"Off grants no forecasts");h.succeed();
    }
    @GameTest(template="empty") public static void futureSightBranchesAndHiddenTargetsStayPrivate(GameTestHelper h) {
        floor(h);var p=player(h,"Seer",origin(h),"atium");var other=player(h,"Oracle",origin(h).add(3,0,0),"electrum");
        h.assertTrue(TemporalService.interferes(other),"Active electrum splits an atium forecast");PowerData.state(other,"a:electrum",0);
        h.assertTrue(!TemporalService.interferes(other),"Merely possessing electrum does not counter atium");PowerAttributes.set(other,"a:atium",1);PowerData.reserve(other,"atium",10);PowerData.state(other,"a:atium",2);
        h.assertTrue(TemporalService.interferes(other),"Two active atium burners interfere too");
        var cow=h.spawn(EntityType.COW,new BlockPos(4,2,1));cow.setNoAi(true);
        h.assertTrue(TemporalService.forecast(p).shadows().stream().anyMatch(s->s.entity()==cow.getId()),"Visible entities have atium shadows");
        cow.setInvisible(true);h.assertTrue(!TemporalService.visible(p,cow),"Foresight must not reveal invisible targets");cow.setInvisible(false);
        h.setBlock(new BlockPos(3,2,1),Blocks.STONE);h.setBlock(new BlockPos(3,3,1),Blocks.STONE);h.setBlock(new BlockPos(3,4,1),Blocks.STONE);
        h.assertTrue(!TemporalService.visible(p,cow),"Foresight must not become a wallhack");h.succeed();
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
