package dev.cosmerearcanists.power;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.investiture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeekingGameTests {
    private static FakePlayer player(GameTestHelper h,String metal) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"SeekingTest"));p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1))));
        grant(p,metal);return p;
    }
    private static void grant(FakePlayer p,String metal){PowerAttributes.set(p,"a:"+metal,1);PowerData.reserve(p,metal,100);PowerData.state(p,"a:"+metal,2);}
    private static InvestitureSignals.Signal signal(Vec3 p){return new InvestitureSignals.Signal(InvestitureSignals.ALLOMANCY,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"steel"),UUID.randomUUID(),p,1,1,24,2,true);}
    @GameTest(template="empty") public static void onlyActiveImplementedEffectsEmitAndBronzeSeesThroughWalls(GameTestHelper h) {
        var source=player(h,"steel");
        for(String m:List.of("iron","tin","pewter","electrum","atium","bronze","copper","gold"))grant(source,m);
        var emissions=SeekingService.signalsFor(source);
        h.assertTrue(emissions.size()==8 && emissions.stream().noneMatch(s->s.effect().getPath().equals("gold")),"Only eight implemented actively burning effects emit, not placeholder gold");
        PowerData.state(source,"a:steel",0);h.assertTrue(SeekingService.signalsFor(source).stream().noneMatch(s->s.effect().getPath().equals("steel")),"Possessing the attribute and reserves does not emit while off");
        var seeker=player(h,"bronze");var pulse=signal(seeker.position().add(4,0,0));
        var wall=BlockPos.containing(seeker.position().add(2,1,0));h.getLevel().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var result=SeekingService.detect(seeker,List.of(pulse),List.of());
        h.assertTrue(result.pulses().size()==1 && result.pulses().getFirst().effect().endsWith(":steel"),"Bronze senses identifiable active Investiture through walls");
        PowerData.state(seeker,"a:bronze",0);h.assertTrue(SeekingService.detect(seeker,List.of(pulse),List.of()).pulses().isEmpty(),"Inactive bronze reveals nothing");h.succeed();
    }
    @GameTest(template="empty") public static void copperMasksBothEndpointsStacksAndCanBePierced(GameTestHelper h) {
        var seeker=player(h,"bronze");var smoker=player(h,"copper");smoker.setPos(seeker.position().add(16,0,0));var cloud=SeekingService.cloud(smoker);var pulse=signal(smoker.position());
        h.assertTrue(cloud.radius()==12 && SeekingService.detect(seeker,List.of(pulse),List.of(cloud)).pulses().isEmpty(),"Normal copper hides an external source inside its radius");
        h.assertTrue(SeekingService.shielding(smoker.position(),pulse.position(),List.of(cloud,cloud))==1,"One cloud cannot be counted twice for both endpoints or duplicate entries");
        var inner=new SeekingService.Cloud(UUID.randomUUID(),seeker.position(),12,1);
        h.assertTrue(SeekingService.detect(seeker,List.of(pulse),List.of(inner)).pulses().isEmpty(),"A Seeker inside a cloud cannot hear even an external source");
        PowerAttributes.set(seeker,"a:bronze",4);
        h.assertTrue(!SeekingService.detect(seeker,List.of(pulse),List.of(cloud)).pulses().isEmpty(),"Stronger bronze can pierce normal copper");
        var extra=new SeekingService.Cloud(UUID.randomUUID(),smoker.position(),12,1);
        h.assertTrue(SeekingService.detect(seeker,List.of(pulse),List.of(cloud,extra)).pulses().isEmpty(),"Overlapping Smokers strengthen copperclouds");
        PowerData.state(smoker,"a:copper",0);h.assertTrue(SeekingService.cloud(smoker)==null,"Stopping removes the field");h.succeed();
    }
    @GameTest(template="empty") public static void otherArtsUseSharedProvidersAndPrivateUnknownRhythms(GameTestHelper h) {
        var seeker=player(h,"bronze");var art=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"test_surgebinding");var effect=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"test_lashing");
        var emitted=new InvestitureSignals.Signal(art,effect,UUID.randomUUID(),seeker.position().add(4,0,0),1,4,35,0,true);
        var key=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"test_provider_"+UUID.randomUUID().toString().replace("-",""));
        InvestitureSignals.register(key,(level,sink)->{if(level==h.getLevel())sink.accept(emitted);});
        try {
            var signals=InvestitureSignals.collect(h.getLevel());h.assertTrue(signals.contains(emitted),"Other arts register real active emissions without modifying bronze");
            var unknown=SeekingService.detect(seeker,List.of(emitted),List.of()).pulses();
            h.assertTrue(unknown.size()==1 && unknown.getFirst().effect().isEmpty() && unknown.getFirst().art().isEmpty(),"Weak bronze hears a difficult signal without leaking its identity in the packet");
            PowerAttributes.set(seeker,"a:bronze",4);var identified=SeekingService.detect(seeker,List.of(emitted),List.of()).pulses();
            h.assertTrue(identified.size()==1 && identified.getFirst().effect().equals(effect.toString()),"Stronger bronze identifies harder foreign Invested Arts");
            boolean previous=SeekingConfig.OTHER_ARTS.get();try{SeekingConfig.OTHER_ARTS.set(false);h.assertTrue(SeekingService.detect(seeker,List.of(emitted),List.of()).pulses().isEmpty(),"Cross-art detection is independently configurable");}finally{SeekingConfig.OTHER_ARTS.set(previous);}
            var cloud=new SeekingService.Cloud(UUID.randomUUID(),seeker.position(),12,100);
            h.assertTrue(SeekingService.detect(seeker,List.of(emitted),List.of(cloud)).pulses().isEmpty(),"Compatible foreign Investiture is concealed too");
            var unmaskable=new InvestitureSignals.Signal(art,effect,UUID.randomUUID(),emitted.position(),1,4,35,0,false);
            h.assertTrue(!SeekingService.detect(seeker,List.of(unmaskable),List.of(cloud)).pulses().isEmpty(),"Providers may opt out of copper where their lore requires it");
        }finally{InvestitureSignals.unregister(key);}h.succeed();
    }
    @GameTest(template="empty") public static void copperEmotionalProtectionIsPersonalAndFuelAndDisablementAreSafe(GameTestHelper h) {
        var smoker=player(h,"copper");var neighbor=player(h,"bronze");
        h.assertTrue(!EmotionalInfluenceEvent.allowed(smoker,1) && EmotionalInfluenceEvent.allowed(neighbor,1),"Emotional influence event protects only the burner, not everyone in the cloud");
        h.assertTrue(EmotionalInfluenceEvent.allowed(smoker,5),"Extreme emotional strength may pierce baseline protection");
        boolean enabled=SeekingConfig.COPPER.get();try{SeekingConfig.COPPER.set(false);SeekingService.tick(smoker);h.assertTrue(PowerData.reserve(smoker,"copper")==100 && SeekingService.cloud(smoker)==null && EmotionalInfluenceEvent.allowed(smoker,1),"Disabling preserves saved fuel but removes field and emotional protection");}finally{SeekingConfig.COPPER.set(enabled);}
        var replacement=player(h,"bronze");PowerData.clonePlayer(smoker,replacement);
        h.assertTrue(PowerAttributes.strength(replacement,"a:copper")==1 && PowerData.reserve(replacement,"copper")==100 && SeekingService.cloud(replacement)==null,"Respawn copies attributes/fuel but stops the cloud");
        PowerData.reserve(smoker,"copper",.001);SeekingService.tick(smoker);
        h.assertTrue(PowerData.state(smoker,"a:copper")==0 && EmotionalInfluenceEvent.allowed(smoker,1),"Empty fuel stops copper immediately");
        PowerData.reserve(neighbor,"bronze",.001);SeekingService.tick(neighbor);h.assertTrue(PowerData.state(neighbor,"a:bronze")==0,"Empty fuel stops bronze immediately");h.succeed();
    }
    @GameTest(template="empty") public static void pulseLifetimesBoundsCodecAndFocus(GameTestHelper h) {
        var p=player(h,"bronze");var signal=signal(p.position().add(3,0,0));
        h.assertTrue(InvestitureSignals.emit(h.getLevel(),signal,1),"Short kinetic pulses may be emitted by one-shot powers");
        h.assertTrue(InvestitureSignals.collect(h.getLevel()).contains(signal),"One-shot event appears while live");
        PowerData.assignSlot(p,"a:bronze",0);PowerService.control(p,new PowerNetwork.Control(2,"",1));
        var packet=SeekingService.detect(p,List.of(signal),List.of());h.assertTrue(packet.focused(),"Bronze uses either existing assigned activation channel for detail focus");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{SeekingPacket.CODEC.encode(buffer,packet);h.assertTrue(SeekingPacket.CODEC.decode(buffer).equals(packet),"Filtered signal packet round trips");}finally{buffer.release();}
        var many=new ArrayList<InvestitureSignals.Signal>();for(int i=0;i<100;i++)many.add(signal(p.position().add(3,0,0)));
        h.assertTrue(SeekingService.detect(p,many,List.of()).pulses().size()==SeekingConfig.MAX_SIGNALS.get(),"Output stays within configured signal budget");
        InvestitureSignals.removeSource(signal.source());h.assertTrue(!InvestitureSignals.collect(h.getLevel()).contains(signal),"Source removal clears lingering event pulses");
        h.assertTrue(InvestitureSignals.emit(h.getLevel(),signal,1),"Event can restart after cleanup");
        h.runAfterDelay(2,()->{h.assertTrue(!InvestitureSignals.collect(h.getLevel()).contains(signal),"Expired effects no longer emit");h.succeed();});
    }
}
