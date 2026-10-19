package dev.cosmerearcanists.power;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.investiture.InvestitureCleanseEvent;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnhancementGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"EnhanceTest"));p.setPos(Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(1,20,1))));for(String m:List.of("duralumin","aluminum","iron","steel","tin","pewter","electrum","atium","bronze","copper","zinc","brass")){PowerAttributes.set(p,"a:"+m,1);PowerData.reserve(p,m,PowerConfig.RESERVE_CAP.get());}return p;}
    @GameTest(template="empty") public static void duraluminBoostsAllImplementedMetalsWithoutChangingAttributes(GameTestHelper h){
        var p=player(h);for(String m:List.of("iron","steel","tin","pewter","electrum","atium","bronze","copper","zinc","brass"))PowerData.state(p,"a:"+m,2);
        PowerData.assignSlot(p,"a:atium",0);PowerData.assignSlot(p,"a:electrum",1);PowerService.control(p,new PowerNetwork.Control(2,"",1));PowerService.control(p,new PowerNetwork.Control(7,"",1));
        var metals=List.of("iron","steel","tin","pewter","electrum","atium","bronze","copper","zinc","brass");var before=new HashMap<String,Double>();for(String m:metals)before.put(m,SeekingService.activeStrength(p,"a:"+m));
        for(int state:List.of(1,2)){PowerData.state(p,"a:duralumin",state);double expected=state==1?1.75:2.5;for(String m:metals)h.assertTrue(Math.abs(SeekingService.activeStrength(p,"a:"+m)-before.get(m)*expected)<1e-7 && PowerAttributes.strength(p,"a:"+m)==1,"Duralumin multiplies real service strength for "+m+" without modifying saved attributes");double fuel=PowerData.reserve(p,"zinc");EmotionService.tickFuel(p);h.assertTrue(Math.abs(fuel-PowerData.reserve(p,"zinc")-EmotionConfig.DRAIN.get()/20*(state==1?5:10))<1e-7,"Actual zinc burn drain uses requested multiplier");}
        PowerAttributes.set(p,"a:duralumin",2);PowerData.state(p,"a:duralumin",1);var profile=EnhancementService.profile(p);h.assertTrue(Math.abs(profile.strengthMultiplier()-1.825)<1e-7 && Math.abs(profile.consumptionMultiplier()-5/1.5)<1e-7,"Higher duralumin attributes primarily improve efficiency, including the resource-independent API");PowerData.state(p,"a:duralumin",3);profile=EnhancementService.profile(p);h.assertTrue(Math.abs(profile.strengthMultiplier()-5.4)<1e-7 && profile.durationTicks(500,1000)==75,"Higher attributes sustain captured fuel longer with a modest peak boost");
        PowerData.state(p,"a:duralumin",0);h.assertTrue(EmotionService.strength(p,"a:zinc")==1,"Turning duralumin off restores base strength");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=130) public static void flareHasIndependentReserveScaledWindowsAndConsumesCapturedFuel(GameTestHelper h){
        var p=player(h);double cap=PowerConfig.RESERVE_CAP.get();PowerData.reserve(p,"zinc",cap/2);PowerData.state(p,"a:zinc",2);PowerData.state(p,"a:copper",2);PowerData.state(p,"a:duralumin",3);EnhancementService.begin(p);long start=h.getLevel().getGameTime();
        h.assertTrue(EmotionService.strength(p,"a:zinc")==5 && SeekingService.strength(p,"copper")==5,"Flare boosts active metals5x");
        h.onEachTick(()->{long age=h.getLevel().getGameTime()-start;EnhancementService.begin(p);EnhancementService.finish(p);if(age==49)h.assertTrue(PowerData.reserve(p,"zinc")==0 && PowerData.state(p,"a:zinc")==0 && PowerData.reserve(p,"copper")>0,"Half-full zinc finishes in50ticks while full copper continues");if(age>=100){h.assertTrue(PowerData.reserve(p,"copper")==0 && PowerData.reserve(p,"duralumin")==0 && PowerData.state(p,"a:duralumin")==0,"Full reserve flare consumes captured fuel and ends after100ticks");h.succeed();}});
    }
    @GameTest(template="empty") public static void aluminumPurgesReservesAndOnlyInvokesForeignCleansingHooks(GameTestHelper h){
        var p=player(h);PowerData.reserve(p,"lerasium",123);PowerData.state(p,"a:zinc",2);PowerData.state(p,"a:aluminum",2);p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,0));SpiritualIntegrity.set(p,.4);
        var count=new int[]{0};java.util.function.Consumer<InvestitureCleanseEvent> listener=e->{if(e.player()==p)count[0]++;};NeoForge.EVENT_BUS.addListener(listener);
        try{EnhancementService.begin(p);h.assertTrue(count[0]==1 && PowerData.reserve(p,"zinc")==0 && PowerData.reserve(p,"aluminum")==0 && PowerData.reserve(p,"lerasium")==0,"Aluminum purges all saved metals and delivers one targeted cleansing event");h.assertTrue(PowerData.state(p,"a:zinc")==0 && PowerAttributes.strength(p,"a:zinc")==1 && p.hasEffect(MobEffects.MOVEMENT_SPEED) && SpiritualIntegrity.get(p)==.4,"Purging stops burns without deleting powers, spirit damage or ordinary effects");EnhancementService.begin(p);h.assertTrue(count[0]==1,"One burn cannot repeatedly cleanse after purge");}finally{NeoForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
    @GameTest(template="empty") public static void duraluminCanPierceCopperAndDisabledEnhancementPreservesData(GameTestHelper h){
        var p=player(h);var target=player(h);target.setPos(p.position());PowerData.state(p,"a:zinc",2);PowerData.state(target,"a:copper",2);h.assertTrue(!EmotionService.apply(p,target,"a:zinc",Emotion.COMPLIANCE),"Equal ordinary strengths cannot pierce copper");
        PowerData.state(p,"a:duralumin",1);h.assertTrue(EmotionService.apply(p,target,"a:zinc",Emotion.COMPLIANCE),"Low duralumin boost participates in copper piercing");
        boolean enabled=EnhancementConfig.ENABLED.get();double fuel=PowerData.reserve(p,"duralumin");try{EnhancementConfig.ENABLED.set(false);EnhancementService.begin(p);EnhancementService.finish(p);h.assertTrue(EmotionService.strength(p,"a:zinc")==1 && PowerData.reserve(p,"duralumin")==fuel,"Disabled enhancement removes effects and preserves its fuel/data");}finally{EnhancementConfig.ENABLED.set(enabled);}
        PowerData.state(p,"a:duralumin",3);PowerData.state(p,"a:tin",2);EnhancementService.begin(p);boolean tinEnabled=TinConfig.ENABLED.get();double tin=PowerData.reserve(p,"tin");try{TinConfig.ENABLED.set(false);EnhancementService.begin(p);EnhancementService.finish(p);h.assertTrue(PowerData.reserve(p,"tin")==tin,"Disabling a captured metal preserves its reserves rather than continuing its burst drain");}finally{TinConfig.ENABLED.set(tinEnabled);EnhancementService.clear(p);}h.succeed();
    }
}
