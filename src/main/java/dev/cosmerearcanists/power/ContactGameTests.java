package dev.cosmerearcanists.power;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.investiture.InvestitureContactEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContactGameTests {
    private static FakePlayer player(GameTestHelper h,boolean friendly){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ContactTest")){@Override public boolean canHarmPlayer(Player other){return friendly;}};
        p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));p.setYRot(-90);p.setXRot(0);
        for(String m:List.of("chromium","nicrosil","duralumin","aluminum","tin","pewter","copper")){PowerAttributes.set(p,"a:"+m,1);PowerData.reserve(p,m,PowerConfig.RESERVE_CAP.get());}return p;
    }
    /** GameTestServer starts with PvP disabled. Scope this override to a single synchronous call. */
    private static boolean touch(ServerPlayer source,net.minecraft.world.entity.LivingEntity target,String power){boolean pvp=source.server.isPvpAllowed();try{source.server.setPvpAllowed(true);return ContactService.contact(source,target,power);}finally{source.server.setPvpAllowed(pvp);}}
    private static FakePlayer target(GameTestHelper h,ServerPlayer source){var p=player(h,true);p.setPos(source.position().add(2,0,0));h.getLevel().addNewPlayer(p);return p;}
    private static void remove(GameTestHelper h,ServerPlayer... players){for(var p:players){ContactService.clear(p);EnhancementService.clear(p);h.getLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}}
    @GameTest(template="empty") public static void chromiumLeechesReservesWithoutDeletingPowersOrOrdinaryEffects(GameTestHelper h){
        var source=player(h,true);var target=target(h,source);try{
            PowerData.state(source,"a:chromium",2);PowerData.state(target,"a:tin",2);PowerData.state(target,"f:iron",2);PowerData.reserve(target,"lerasium",123);SpiritualIntegrity.set(target,.4);target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,0));
            double fuel=PowerData.reserve(source,"chromium");h.assertTrue(ContactService.target(source)==target,"Server ray resolves the touched body");h.assertTrue(touch(source,target,"a:chromium"),"Chromium contact succeeds");
            h.assertTrue(PowerData.reserve(target,"tin")==0 && PowerData.reserve(target,"lerasium")==0 && PowerData.state(target,"a:tin")==0,"All saved reserves are removed and burns stop");
            h.assertTrue(PowerAttributes.strength(target,"a:tin")==1 && PowerData.state(target,"f:iron")==2 && target.hasEffect(MobEffects.MOVEMENT_SPEED) && SpiritualIntegrity.get(target)==.4,"Powers, feruchemy, potion effects and spiritual integrity survive");
            h.assertTrue(Math.abs(fuel-PowerData.reserve(source,"chromium")-ContactConfig.COST.get())<1e-7,"Successful touch pays contact cost");PowerData.reserve(target,"tin",100);h.assertTrue(!touch(source,target,"a:chromium") && PowerData.reserve(target,"tin")==100,"Cooldown prevents repeated free contacts");
        }finally{remove(h,source,target);}h.succeed();
    }
    @GameTest(template="empty") public static void contactRequiresBurnFuelRangeSightAndPlayerPermission(GameTestHelper h){
        var source=player(h,false);var target=target(h,source);try{
            PowerData.state(source,"a:chromium",2);h.assertTrue(!touch(source,target,"a:chromium"),"Friendly fire permission is respected");
            boolean serverPvp=source.server.isPvpAllowed();try{source.server.setPvpAllowed(false);h.assertTrue(!ContactService.contact(source,target,"a:chromium"),"Server PvP-disabled protection rejects contact");}finally{source.server.setPvpAllowed(serverPvp);}
            boolean pvp=ContactConfig.PVP.get();try{ContactConfig.PVP.set(false);PowerData.state(source,"a:chromium",0);h.assertTrue(!touch(source,target,"a:chromium"),"Inactive power cannot leech");PowerData.state(source,"a:chromium",2);PowerData.reserve(source,"chromium",0);h.assertTrue(!touch(source,target,"a:chromium"),"Empty source cannot leech");PowerData.reserve(source,"chromium",1000);target.setPos(source.position().add(5,0,0));h.assertTrue(ContactService.target(source)==null,"No long range contact");target.setPos(source.position().add(2,0,0));BlockPos wall=BlockPos.containing(source.getEyePosition().add(1,0,0));h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());h.assertTrue(ContactService.target(source)==null && !touch(source,target,"a:chromium"),"Solid blocks prevent contact");h.getLevel().setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());target.getAbilities().invulnerable=true;h.assertTrue(!touch(source,target,"a:chromium"),"Creative targets are protected even when PvP override is allowed");}finally{ContactConfig.PVP.set(pvp);}
        }finally{remove(h,source,target);}h.succeed();
    }
    @GameTest(template="empty") public static void nicrosilBoostsOnlyActiveMetalsWithoutAttributeEditsOrStacking(GameTestHelper h){
        var source=player(h,true);var target=target(h,source);try{
            PowerData.state(source,"a:nicrosil",3);PowerData.state(target,"a:tin",2);PowerData.state(target,"a:copper",2);PowerData.state(target,"a:duralumin",1);
            h.assertTrue(touch(source,target,"a:nicrosil"),"Nicroburst succeeds on active metals");h.assertTrue(TinService.strength(target)==5 && PowerAttributes.strength(target,"a:tin")==1,"Effective tin strength is boosted without changing attributes");
            EnhancementService.begin(target);h.assertTrue(TinService.strength(target)==5,"Lack of a self duralumin burst does not clear incoming nicrosil");h.assertTrue(!ContactService.committed(target,"pewter")&&!ContactService.committed(target,"duralumin"),"Inactive metals and enhancers are not captured");
            double before=PowerData.reserve(target,"tin");EnhancementService.consume(target,"tin",10);h.assertTrue(PowerData.reserve(target,"tin")==before,"Normal passive drain does not double spend the captured pool");
            PowerData.state(target,"a:duralumin",3);EnhancementService.begin(target);h.assertTrue(!EnhancementService.committed(target,"tin"),"Self duralumin cannot commit the same fuel again");
        }finally{remove(h,source,target);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=130) public static void nicrosilCapturesFiniteIndependentFuelBudgetsAndCannotBeRefreshed(GameTestHelper h){
        var source=player(h,true);var target=target(h,source);double cap=PowerConfig.RESERVE_CAP.get(),refill=cap/10;PowerData.state(source,"a:nicrosil",3);PowerData.state(target,"a:tin",2);PowerData.state(target,"a:copper",2);PowerData.reserve(target,"tin",cap/2);h.assertTrue(touch(source,target,"a:nicrosil"),"Initial surge succeeds");long start=h.getLevel().getGameTime();
        h.onEachTick(()->{long age=h.getLevel().getGameTime()-start;ContactService.tick(target);if(age==20){PowerData.reserve(target,"tin",PowerData.reserve(target,"tin")+refill);PowerData.state(target,"a:tin",0);h.assertTrue(!touch(source,target,"a:nicrosil"),"Repeated contact cannot refresh existing commitments");}if(age==49)h.assertTrue(Math.abs(PowerData.reserve(target,"tin")-refill)<1e-7 && !ContactService.committed(target,"tin") && ContactService.committed(target,"copper"),"Half reserve finishes in50ticks, spends only captured half-reserve despite stopping, and preserves later refill: tin="+PowerData.reserve(target,"tin")+", tinCommitted="+ContactService.committed(target,"tin")+", copperCommitted="+ContactService.committed(target,"copper")+", age="+age);if(age>=100){h.assertTrue(PowerData.reserve(target,"copper")==0 && PowerData.state(target,"a:copper")==0,"Full reserve finishes in100ticks");remove(h,source,target);h.succeed();}});
    }
    @GameTest(template="empty") public static void disabledContactAndAluminumClearSurgesWithoutDeletingSavedData(GameTestHelper h){
        var source=player(h,true);var target=target(h,source);try{
            PowerData.state(source,"a:nicrosil",3);PowerData.state(target,"a:tin",2);h.assertTrue(touch(source,target,"a:nicrosil"),"Surge succeeds");double reserve=PowerData.reserve(target,"tin");boolean enabled=ContactConfig.NICROSIL.get();try{ContactConfig.NICROSIL.set(false);ContactService.tick(target);h.assertTrue(TinService.strength(target)==1 && PowerData.reserve(target,"tin")==reserve && PowerAttributes.strength(source,"a:nicrosil")==1,"Disabling removes boost and preserves remaining fuel/attributes");}finally{ContactConfig.NICROSIL.set(enabled);}
            ContactService.clear(source);h.assertTrue(touch(source,target,"a:nicrosil"),"Fresh surge can be created after disabled cleanup");PowerData.state(target,"a:aluminum",2);EnhancementService.begin(target);h.assertTrue(!ContactService.committed(target,"tin") && PowerData.reserve(target,"tin")==0,"Aluminum purges incoming burst and fuel");
        }finally{remove(h,source,target);}h.succeed();
    }
    @GameTest(template="empty") public static void contactEventsCanCancelOrQueueForeignResourceEffects(GameTestHelper h){
        var source=player(h,true);var target=target(h,source);PowerData.state(source,"a:chromium",2);int[] applied={0};boolean[] cancel={true};java.util.function.Consumer<InvestitureContactEvent> listener=e->{if(e.source()==source){e.addEffect(()->applied[0]++);if(cancel[0])e.setCanceled(true);}};NeoForge.EVENT_BUS.addListener(listener);
        try{double fuel=PowerData.reserve(source,"chromium");h.assertTrue(!touch(source,target,"a:chromium") && applied[0]==0 && PowerData.reserve(source,"chromium")==fuel && PowerData.reserve(target,"tin")>0,"Cancelled contacts spend nothing and execute no queued effects");cancel[0]=false;h.assertTrue(touch(source,target,"a:chromium") && applied[0]==1,"Accepted contact executes foreign effect once");}finally{NeoForge.EVENT_BUS.unregister(listener);remove(h,source,target);}h.succeed();
    }
    private ContactGameTests(){}
}
