package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.OriginData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    @GameTest(template="empty") public static void reserveUnitsMigrateOnceAndPreserveBurnDuration(GameTestHelper h){
        var level=h.getLevel();var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"ReserveMigration"),ClientInformation.createDefault());
        var old=new net.minecraft.nbt.CompoundTag();var reserves=new net.minecraft.nbt.CompoundTag();reserves.putDouble("iron",150);reserves.putDouble("unimplemented_metal",300);old.put("reserves",reserves);p.getPersistentData().put(PowerData.KEY,old);
        h.assertTrue(PowerData.reserve(p,"iron")==50&&PowerData.reserve(p,"unimplemented_metal")==100,"Existing reserves convert proportionally including disabled/unknown metals");
        h.assertTrue(PowerData.reserve(p,"iron")==50,"Repeated access never converts twice");
        PowerData.reserve(p,"iron",1000);h.assertTrue(PowerData.reserve(p,"iron")==100,"Refills clamp to the new cap");
        EnhancementService.consume(p,"iron",1);h.assertTrue(Math.abs(PowerData.reserve(p,"iron")-(100-1.0/3))<1e-7,"A normal one-second burn consumes one third of a reserve unit");
        h.assertTrue(Math.abs(100/PowerConfig.reserveCost(1)-300)<1e-7,"Full-reserve normal burn duration remains300 seconds");
        var clone=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"ReserveClone"),ClientInformation.createDefault());PowerData.clonePlayer(p,clone);h.assertTrue(PowerData.reserve(clone,"iron")==PowerData.reserve(p,"iron"),"Clone retains conversion marker and current reserves");
        h.succeed();
    }
    @GameTest(template="empty") public static void metalRangeUsesIndependentAttributesBurnLevelsAndResistance(GameTestHelper h){
        var level=h.getLevel();MetalDetector.rebuild(level.getServer());
        var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"RangeScaling"),ClientInformation.createDefault());
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(1,20,1))));
        PowerAttributes.set(p,"a:iron",1);PowerAttributes.set(p,"a:steel",2);
        PowerData.state(p,"a:iron",1);PowerData.state(p,"a:steel",3);
        h.assertTrue(Math.abs(PowerService.metalRange(p,"a:iron")-9.6)<1e-6&&Math.abs(PowerService.metalRange(p,"a:steel")-20.8)<1e-6,"Each power uses its own attribute and burn tier");
        var block=net.minecraft.core.BlockPos.containing(PowerService.chest(p).add(18,0,0));var target=MetalTarget.block(block);
        h.assertTrue(PowerService.withinRange(p,target,PowerService.metalRange(p,"a:steel"))&&!PowerService.withinRange(p,target,PowerService.metalRange(p,"a:iron")),"A shared visible anchor cannot extend the weaker power's activation range");
        var wall=block.offset(-1,0,0);level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
        try{h.assertTrue(!PowerService.withinRange(p,target,PowerService.metalRange(p,"a:steel")),"Hardness still consumes scaled reach");}finally{level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
        var enabled=dev.cosmerearcanists.ServerConfig.ALLOMANCY.metals().get(dev.cosmerearcanists.Metal.STEEL);boolean old=enabled.get();
        try{enabled.set(false);h.assertTrue(Math.abs(PowerService.detectionRange(p)-9.6)<1e-6,"A disabled stronger metal cannot extend detection");}finally{enabled.set(old);}
        PowerData.state(p,"a:steel",0);h.assertTrue(Math.abs(PowerService.detectionRange(p)-9.6)<1e-6,"Stopping the stronger metal shrinks shared detection immediately");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void godMetalFormsConvertWithoutMaterialLoss(GameTestHelper helper) {
        var level=helper.getLevel();MetalDetector.rebuild(level.getServer());
        for(String metal:dev.cosmerearcanists.ModMetals.GOD_METALS) {
            var nugget=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_nugget"));
            var ingot=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_ingot"));
            var block=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_block"));
            helper.assertTrue(nugget!=Items.AIR && ingot!=Items.AIR && block instanceof net.minecraft.world.item.BlockItem,"Every god metal must register nugget, ingot and placeable block");
            for(var pair:java.util.List.of(new net.minecraft.world.item.Item[]{nugget,ingot},new net.minecraft.world.item.Item[]{ingot,block})) {
                var packed=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.Collections.nCopies(9,new ItemStack(pair[0])));
                var compact=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,packed,level).orElseThrow().value().assemble(packed,level.registryAccess());
                helper.assertTrue(compact.is(pair[1]) && compact.getCount()==1,"Nine lower units must compact into one higher unit for "+metal);
                var single=net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(new ItemStack(pair[1])));
                var unpacked=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,single,level).orElseThrow().value().assemble(single,level.registryAccess());
                helper.assertTrue(unpacked.is(pair[0]) && unpacked.getCount()==9,"Every compaction must reverse exactly for "+metal);
            }
            helper.assertTrue(MetalDetector.detects(level.getServer(),new ItemStack(block)) && MetalAmounts.units(level.getServer(),new ItemStack(nugget))==1 && MetalAmounts.units(level.getServer(),new ItemStack(ingot))==9 && MetalAmounts.units(level.getServer(),new ItemStack(block))==81,"God-metal forms must have consistent detection and 1/9/81 metal mass");
            var condition=new dev.cosmerearcanists.GodMetalEnabledCondition(metal);boolean enabled=dev.cosmerearcanists.MetallurgyConfig.GOD_CONVERSIONS.get();
            try {dev.cosmerearcanists.MetallurgyConfig.GOD_CONVERSIONS.set(false);helper.assertTrue(!condition.test(null) && BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_block"))==block,"Disabling conversion must preserve registered god-metal blocks");}
            finally{dev.cosmerearcanists.MetallurgyConfig.GOD_CONVERSIONS.set(enabled);}
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void godMetalFilingVialsAndBlockDrops(GameTestHelper helper) {
        var level=helper.getLevel();var p=filingPlayer(helper,"GodMetalFiling");
        for(String metal:dev.cosmerearcanists.ModMetals.GOD_METALS) {
            for(String form:java.util.List.of("nugget","ingot","block")) {
                var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_"+form));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(item));
                helper.assertTrue(dev.cosmerearcanists.filing.Filings.file(p),"All god-metal forms must support filing");
                int expected=form.equals("nugget")?4:form.equals("ingot")?36:324;
                helper.assertTrue(dev.cosmerearcanists.filing.Filings.capacity(p.getOffhandItem())==expected && dev.cosmerearcanists.filing.Filings.remaining(p.getOffhandItem())==expected-1,"God-metal filing must preserve standard material ratios");
            }
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,metal+"_block"));var state=block.defaultBlockState();
            helper.assertTrue(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) && state.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL) && new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state),"God-metal blocks must use their registered pickaxe/tool tags");
            var drops=net.minecraft.world.level.block.Block.getDrops(state,level,helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1)),null,p,new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size()==1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount()==1,"Mined storage blocks must drop exactly themselves");
        }
        var mixture=dev.cosmerearcanists.filing.Filings.vial(java.util.Map.of("atium",2,"lerasium",3));
        var bottle=((MetalVialItem)mixture.getItem()).finishUsingItem(mixture,level,p);
        helper.assertTrue(PowerData.reserve(p,"atium")==100 && PowerData.reserve(p,"lerasium")==100 && dev.cosmerearcanists.filing.Filings.contents(bottle).equals(java.util.Map.of("lerasium",1)),"Mixed god-metal filings must restore independent reserves at fifty per filing");
        helper.assertTrue(!PowerData.available(p).contains("a:atium") && !PowerData.available(p).contains("a:lerasium"),"Material ingestion must not accidentally grant unfinished god-metal powers");
        helper.succeed();
    }
    private static ServerPlayer tinPlayer(GameTestHelper helper,String name) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),name));
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,20,1));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        PowerAttributes.set(p,"a:tin",1);PowerData.state(p,"a:tin",2);PowerData.reserve(p,"tin",300);return p;
    }
    @GameTest(template = "empty")
    public static void tinBurnFuelVisionAndHeldZoom(GameTestHelper helper) {
        var p=tinPlayer(helper,"TinBurn");TinService.tick(p);
        helper.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION) && TinService.nightVision(p)>0 && Math.abs(PowerData.reserve(p,"tin")-(PowerConfig.RESERVE_CAP.get()-PowerConfig.reserveCost(.05)))<1e-6,"Passive tin provides variable vision without a potion and consumes normal burn fuel");
        PowerData.state(p,"a:tin",0);TinService.tick(p);
        helper.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Stopping leaves no tin potion effect");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1000));
        PowerData.state(p,"a:tin",3);TinService.tick(p);
        helper.assertTrue(TinService.strength(p)==2,"Tin Flaring must scale effective strength");
        PowerData.state(p,"a:tin",0);TinService.tick(p);
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION) && p.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION).getDuration()==1000,"Tin must never remove an independent night-vision potion");
        PowerData.state(p,"a:tin",2);PowerData.assignSlot(p,"a:tin",0);PowerService.control(p,new PowerNetwork.Control(2,"",1));
        helper.assertTrue(PowerService.holdingAbility(p,"a:tin") && TinMath.zoom(TinService.strength(p),1.5,8)==2.5,"Tin uses the shared primary activation channel for held zoom");
        PowerService.control(p,new PowerNetwork.Control(2,"",0));PowerData.assignSlot(p,"a:tin",1);PowerService.control(p,new PowerNetwork.Control(7,"",1));
        helper.assertTrue(PowerService.holdingAbility(p,"a:tin"),"Secondary activation must also support tin zoom");
        PowerService.control(p,new PowerNetwork.Control(7,"",0));PowerData.reserve(p,"tin",.001);TinService.tick(p);
        helper.assertTrue(PowerData.state(p,"a:tin")==0 && TinService.strength(p)==0,"Depletion must disable tin immediately");
        helper.succeed();
    }
    @GameTest(template="empty") public static void tinVisionScalingMigrationAndFeatureGates(GameTestHelper h){
        var p=tinPlayer(h,"TinVision");PowerData.state(p,"a:tin",1);double low=TinService.nightVision(p);PowerData.state(p,"a:tin",2);double normal=TinService.nightVision(p);PowerData.state(p,"a:tin",3);double flare=TinService.nightVision(p);
        h.assertTrue(low>0 && low<normal && normal<flare,"All three burns have distinct graded vision");PowerAttributes.set(p,"a:tin",2);h.assertTrue(TinService.nightVision(p)>flare,"Higher effective attribute improves vision beyond baseline flare");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,320,0,true,false,false));PowerData.data(p).putBoolean("tin_night_vision",true);TinService.tick(p);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION) && !PowerData.data(p).contains("tin_night_vision"),"Old tin-owned hidden potion is removed on migration");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1000));TinService.tick(p);h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Independent night vision remains intact while burning");
        boolean enabled=TinConfig.NIGHT_VISION.get();try{TinConfig.NIGHT_VISION.set(false);h.assertTrue(TinService.nightVision(p)==0 && TinService.strength(p)>0,"Vision toggle leaves other tin senses enabled");}finally{TinConfig.NIGHT_VISION.set(enabled);}
        PowerData.reserve(p,"tin",0);h.assertTrue(TinService.nightVision(p)==0,"Empty fuel grants no vision");PowerData.reserve(p,"tin",100);PowerData.state(p,"a:tin",0);h.assertTrue(TinService.nightVision(p)==0,"Off grants no vision");
        var packet=new PowerNetwork.TinStatus(p.level().dimension().location().toString(),2,1,true,(float)flare);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{PowerNetwork.TinStatus.CODEC.encode(buffer,packet);h.assertTrue(PowerNetwork.TinStatus.CODEC.decode(buffer).equals(packet),"Graded server-approved vision is transmitted exactly");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template = "empty")
    public static void tinWallsMuffleAndBlockRealEchoes(GameTestHelper helper) {
        var p=tinPlayer(helper,"TinWalls");PowerData.state(p,"a:tin",3);var level=helper.getLevel();
        var from=p.getEyePosition();var target=from.add(8,0,0);var first=net.minecraft.core.BlockPos.containing(from);
        for(int x=0;x<=8;x++)level.setBlock(first.offset(x,0,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var clear=TinService.echo(p,target,null,1);helper.assertTrue(clear!=null,"Unblocked nearby sound must be heard");
        var wall=first.offset(4,0,0);level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        double stone=TinService.obstruction(level,from,target);var muted=TinService.echo(p,target,null,1);
        helper.assertTrue(stone>0 && muted!=null && muted.audible()<clear.audible() && muted.ticks()<=clear.ticks(),"One wall must mute and shorten the sound reveal");
        level.setBlock(wall,net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),3);
        helper.assertTrue(TinService.obstruction(level,from,target)>stone,"Harder blocks must attenuate more");
        level.setBlock(wall,net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState(),3);
        helper.assertTrue(TinService.obstruction(level,from,target)>stone,"Wool must strongly muffle sound");
        for(int x=1;x<=7;x++)level.setBlock(first.offset(x,0,0),net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState(),3);
        helper.assertTrue(TinService.echo(p,target,null,1)==null,"A thick muffling barrier must entirely suppress wireframes");
        helper.assertTrue(TinService.echo(p,target,null,0)==null,"Zero-volume sound must reveal nothing");
        PowerData.state(p,"a:tin",0);helper.assertTrue(TinService.echo(p,target,null,1)==null,"Inactive players must receive no sound geometry");helper.succeed();
    }
    public static final class TinSoundProbe {
        net.minecraft.world.entity.Entity heard;
        @net.neoforged.bus.api.SubscribeEvent public void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent event) {
            heard=event instanceof net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity at?at.getEntity():TinService.soundSource();
        }
    }
    @GameTest(template = "empty")
    public static void tinEntitySoundsIdentifyOnlyTheActualEmitter(GameTestHelper helper) {
        var p=tinPlayer(helper,"TinSource");PowerData.state(p,"a:tin",3);var level=helper.getLevel();
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);cow.setPos(p.position().add(4,0,0));cow.setNoAi(true);level.addFreshEntity(cow);
        var probe=new TinSoundProbe();net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(probe);
        try {
            cow.playSound(net.minecraft.sounds.SoundEvents.COW_AMBIENT,1,1);
            helper.assertTrue(probe.heard==cow && TinService.soundSource()==null,"Real entity sound calls must scope source identity and restore it afterward");
            var echo=TinService.echo(p,cow.position(),probe.heard,1);
            helper.assertTrue(echo!=null && echo.entityId()==cow.getId(),"A noisy mob must be included in its sound reveal");
            var blockEcho=TinService.echo(p,cow.position(),null,1);
            helper.assertTrue(blockEcho!=null && blockEcho.entityId()==-1,"Position-only noise must not guess and expose a nearby silent mob");
            cow.setSilent(true);probe.heard=null;cow.playSound(net.minecraft.sounds.SoundEvents.COW_AMBIENT,1,1);
            helper.assertTrue(probe.heard==null,"Silent entities must emit no audible reveal events");
        } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(probe);cow.discard();}
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void tinWireframesRequireActualFlaringEvenAtExtremeStrength(GameTestHelper helper){
        var p=tinPlayer(helper,"TinFlareGate");PowerAttributes.set(p,"a:tin",1024);var sound=p.getEyePosition().add(2,0,0);
        for(int state:new int[]{1,2}){PowerData.state(p,"a:tin",state);helper.assertTrue(TinService.revealStrength(p)==0 && TinService.echo(p,sound,null,1)==null,"Attributes cannot bypass the Flaring-only wireframe rule");}
        PowerData.state(p,"a:tin",3);helper.assertTrue(TinService.echo(p,sound,null,1)!=null,"Flaring enables eligible high-strength echoes");
        boolean flaring=PowerConfig.FLARING.get();try{PowerConfig.FLARING.set(false);helper.assertTrue(TinService.echo(p,sound,null,1)==null,"Disabled flaring cannot unlock echoes through a stored flare state");}finally{PowerConfig.FLARING.set(flaring);}
        double range=TinConfig.ECHO_RANGE.get(),base=TinConfig.RANGE.get(),cap=TinConfig.MAX_RANGE.get();try{TinConfig.RANGE.set(1.0);TinConfig.MAX_RANGE.set(2.0);TinConfig.ECHO_RANGE.set(1.0);var distant=p.getEyePosition().add(3,0,0);helper.assertTrue(TinService.echo(p,distant,null,1)==null,"Original capped hearing range excludes this loaded sound");TinConfig.ECHO_RANGE.set(2.0);helper.assertTrue(TinService.echo(p,distant,null,1)!=null,"Doubled capped range includes the same loaded unobstructed sound");}finally{TinConfig.ECHO_RANGE.set(range);TinConfig.RANGE.set(base);TinConfig.MAX_RANGE.set(cap);}
        var packet=new PowerNetwork.TinStatus(p.level().dimension().location().toString(),TinService.strength(p),1,true);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());try{PowerNetwork.TinStatus.CODEC.encode(buffer,packet);helper.assertTrue(PowerNetwork.TinStatus.CODEC.decode(buffer).equals(packet),"Tin status transmits server-authoritative wireframe eligibility");}finally{buffer.release();}helper.succeed();
    }
    @GameTest(template = "empty")
    public static void tinPersistenceExtendsEchoesWithoutBypassingPowerGateOrPacketBounds(GameTestHelper helper){
        var p=tinPlayer(helper,"TinPersistence");PowerAttributes.set(p,"a:tin",6);PowerData.state(p,"a:tin",3);var sound=p.getEyePosition().add(2,0,0);
        double persistence=TinConfig.PERSISTENCE.get();int fade=TinConfig.FADE.get(),cap=TinConfig.MAX_FADE.get();
        try{TinConfig.PERSISTENCE.set(1.0);var shorter=TinService.echo(p,sound,null,1);TinConfig.PERSISTENCE.set(1.4);var longer=TinService.echo(p,sound,null,1);
            helper.assertTrue(shorter!=null && longer!=null && longer.ticks()>shorter.ticks() && longer.audible()==shorter.audible(),"Persistence extends the same approved echo without weakening obstruction or changing its audibility");
            TinConfig.PERSISTENCE.set(4.0);TinConfig.FADE.set(100);TinConfig.MAX_FADE.set(200);PowerAttributes.set(p,"a:tin",1024);helper.assertTrue(TinService.echo(p,sound,null,1).ticks()<=200,"Long echoes remain within packet lifetime bounds");
            PowerAttributes.set(p,"a:tin",1);PowerData.state(p,"a:tin",2);helper.assertTrue(TinService.echo(p,sound,null,1)==null,"Longer persistence cannot unlock wireframes below the Tin threshold");
        }finally{TinConfig.PERSISTENCE.set(persistence);TinConfig.FADE.set(fade);TinConfig.MAX_FADE.set(cap);}helper.succeed();
    }
    @GameTest(template = "empty")
    public static void highTinIdentifiesANoisyMobBehindAStoneWall(GameTestHelper helper){
        var p=tinPlayer(helper,"TinWallMob");PowerAttributes.set(p,"a:tin",6);PowerData.state(p,"a:tin",3);var level=helper.getLevel();var origin=net.minecraft.core.BlockPos.containing(p.position());
        for(int x=1;x<=5;x++)for(int y=0;y<4;y++)level.setBlock(origin.offset(x,y,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        for(int y=0;y<3;y++)level.setBlock(origin.offset(2,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);cow.setPos(p.position().add(4,0,0));cow.setNoAi(true);level.addFreshEntity(cow);
        var probe=new TinSoundProbe();net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(probe);
        try{cow.playSound(net.minecraft.sounds.SoundEvents.COW_AMBIENT,1,1);var echo=TinService.echo(p,cow.position(),probe.heard,1);
            helper.assertTrue(TinService.obstruction(level,p.getEyePosition(),cow.position())>0,"Fixture must put real blocking geometry between listener and mob");
            helper.assertTrue(echo!=null && echo.entityId()==cow.getId() && echo.sourceBox().equals(cow.getBoundingBox()) && echo.audible()>.1 && !echo.surfaces().isEmpty(),"High tin must send an identified mob and nearby geometry through a modest wall when it actually makes noise");
            PowerData.state(p,"a:tin",0);helper.assertTrue(TinService.echo(p,cow.position(),cow,1)==null,"Disabling tin removes through-wall sound reveals");
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(probe);cow.discard();}helper.succeed();
    }
    @GameTest(template = "empty")
    public static void tinSurfaceShapesAndPacketsStayBounded(GameTestHelper helper) {
        var p=tinPlayer(helper,"TinShapes");var level=helper.getLevel();var center=p.position().add(3,0,0);var floor=net.minecraft.core.BlockPos.containing(center).below();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlock(floor.offset(x,0,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var boxes=TinService.surfaces(level,center,3,8);
        helper.assertTrue(!boxes.isEmpty() && boxes.size()<=8,"Sound wireframes must contain nearby collision surfaces within the shape budget");
        for(var box:boxes)helper.assertTrue(box.getCenter().distanceTo(center)<4,"Echo cannot reveal distant terrain");
        var packet=new PowerNetwork.TinEcho(level.dimension().location().toString(),center,-1,new net.minecraft.world.phys.AABB(center,center),24,.5f,boxes);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {PowerNetwork.TinEcho.CODEC.encode(buffer,packet);helper.assertTrue(PowerNetwork.TinEcho.CODEC.decode(buffer).equals(packet),"Echo packet must preserve source, lifetime, obstruction intensity and shapes");}finally{buffer.release();}
        helper.assertTrue(TinService.echo(p,center,null,1)==null,"Baseline Burning must not unlock wireframes");
        PowerData.state(p,"a:tin",1);helper.assertTrue(TinService.echo(p,center,null,1)==null,"Baseline Low burn must not unlock wireframes");
        PowerData.state(p,"a:tin",3);var normal=TinService.echo(p,center,null,1);PowerAttributes.set(p,"a:tin",3);var stronger=TinService.echo(p,center,null,1);
        helper.assertTrue(normal!=null && stronger!=null && stronger.audible()>normal.audible()*2 && stronger.ticks()>normal.ticks() && stronger.surfaces().size()>=normal.surfaces().size(),"Greater attributes must improve hearing and persistence");
        helper.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer filingPlayer(GameTestHelper helper,String name) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),name));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().instabuild=false;return p;
    }
    @GameTest(template = "empty")
    public static void filingPiecesRespectCapacityAndStackSplitting(GameTestHelper helper) {
        var p=filingPlayer(helper,"Filer");var file=dev.cosmerearcanists.ModMetals.METAL_FILE.get();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,file.getDefaultInstance());
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.IRON_NUGGET,2));
        file.use(helper.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        var partial=p.getOffhandItem();
        helper.assertTrue(partial.getCount()==1 && dev.cosmerearcanists.filing.Filings.remaining(partial)==3 && partial.getDamageValue()==1 && partial.getMaxDamage()==4 && partial.getMaxStackSize()==1,"First filing must isolate one nugget with a persistent 3/4 remaining bar");
        helper.assertTrue(p.getMainHandItem().getDamageValue()==1,"Using the file must apply tool wear");
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,1,java.util.List.of(partial,ItemStack.EMPTY,ItemStack.EMPTY));
        helper.assertTrue(!net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_NUGGET).test(partial),"Filed pieces must fail ordinary and tag ingredients");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,helper.getLevel()).isEmpty(),"Recipe manager must exclude filed input");
        for(int i=0;i<3;i++)helper.assertTrue(dev.cosmerearcanists.filing.Filings.file(p),"All remaining nugget filings must be accessible");
        helper.assertTrue(p.getOffhandItem().isEmpty(),"Fourth filing must consume the exhausted nugget");
        int loose=0,whole=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(dev.cosmerearcanists.filing.Filings.metal(stack).equals("iron"))loose+=stack.getCount();if(stack.is(Items.IRON_NUGGET))whole+=stack.getCount();}
        helper.assertTrue(loose==4 && whole==1,"Stack splitting must yield exactly four filings and preserve the untouched nugget");
        for(var test:java.util.Map.of(Items.IRON_INGOT,36,Items.IRON_BLOCK,324).entrySet()) {
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,test.getKey().getDefaultInstance());
            helper.assertTrue(dev.cosmerearcanists.filing.Filings.file(p),"Ingots and blocks must be fileable");
            helper.assertTrue(dev.cosmerearcanists.filing.Filings.capacity(p.getOffhandItem())==test.getValue(),"Filing capacity must follow nugget equivalents");
            if(test.getKey()==Items.IRON_BLOCK){
                var pos=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
                var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.OFF_HAND,pos,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
                new dev.cosmerearcanists.filing.FilingEvents().useBlock(event);
                helper.assertTrue(event.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE && !event.isCanceled(),"Filed block placement must be blocked while chest/block interaction stays available");
            }
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void filedMetalCannotBypassRecipeMatchingOrSaveProgress(GameTestHelper helper) {
        var p=filingPlayer(helper,"FiledRecipes");p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.IRON_INGOT));
        dev.cosmerearcanists.filing.Filings.file(p);var partial=p.getOffhandItem();
        var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(partial,new ItemStack(Items.STICK)));
        var ingredients=net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT),net.minecraft.world.item.crafting.Ingredient.of(Items.STICK));
        var recipe=new net.minecraft.world.item.crafting.ShapelessRecipe("",net.minecraft.world.item.crafting.CraftingBookCategory.MISC,new ItemStack(Items.IRON_NUGGET),ingredients);
        helper.assertTrue(!recipe.matches(input,helper.getLevel()),"Optimized simple shapeless recipes must not ignore filing data");
        var clean=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.STICK)));
        helper.assertTrue(recipe.matches(clean,helper.getLevel()),"Unfiled ingredients must keep normal recipe behavior");
        var cached=net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        helper.assertTrue(cached.getRecipeFor(net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(partial)),helper.getLevel()).isEmpty(),"Cached machine lookup must not recycle filed material");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
        try {ItemStack.STREAM_CODEC.encode(buffer,partial);var restored=ItemStack.STREAM_CODEC.decode(buffer);helper.assertTrue(dev.cosmerearcanists.filing.Filings.filed(restored) && dev.cosmerearcanists.filing.Filings.remaining(restored)==35 && restored.getDamageValue()==1,"Filing progress and wear must survive component serialization");}finally{buffer.release();}
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void mixedVialsHoldSixteenAndDrinkPerMetal(GameTestHelper helper) {
        var p=filingPlayer(helper,"MixedVial");p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
        for(var entry:java.util.Map.of("iron",3,"steel",13).entrySet()) {
            // Start from a single bottle to keep the growing vial in the other hand.
            if(!(p.getOffhandItem().getItem() instanceof MetalVialItem))p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.GLASS_BOTTLE));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,dev.cosmerearcanists.filing.Filings.filing(entry.getKey(),entry.getValue()));
            for(int i=0;i<entry.getValue();i++)helper.assertTrue(dev.cosmerearcanists.filing.Filings.load(p,net.minecraft.world.InteractionHand.MAIN_HAND),"Every filing up to capacity must load");
        }
        var vial=p.getOffhandItem();
        helper.assertTrue(dev.cosmerearcanists.filing.Filings.contents(vial).equals(java.util.Map.of("iron",3,"steel",13)),"Vial must retain exact mixed counts");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,dev.cosmerearcanists.filing.Filings.filing("iron",1));
        helper.assertTrue(!dev.cosmerearcanists.filing.Filings.load(p,net.minecraft.world.InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount()==1,"Seventeenth filing must be rejected without consumption");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var item=(MetalVialItem)vial.getItem();item.use(helper.getLevel(),p,net.minecraft.world.InteractionHand.OFF_HAND);
        helper.assertTrue(PowerData.reserve(p,"iron")==0 && PowerData.reserve(p,"steel")==0 && item.getUseAnimation(vial)==net.minecraft.world.item.UseAnim.DRINK && item.getUseDuration(vial,p)==32,"Beginning potion-style use cannot grant reserve before completion");
        double cap=PowerConfig.RESERVE_CAP.get();
        try {PowerConfig.RESERVE_CAP.set(1000.0);var returned=item.finishUsingItem(vial,helper.getLevel(),p);
            helper.assertTrue(PowerData.reserve(p,"iron")==150 && PowerData.reserve(p,"steel")==650 && returned.is(Items.GLASS_BOTTLE),"3 iron + 13 steel must restore 150/650 and return one empty bottle");
        }finally{PowerConfig.RESERVE_CAP.set(cap);}
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void vialOverflowLegacyAndAluminumArePreserved(GameTestHelper helper) {
        var p=filingPlayer(helper,"VialOverflow");var vial=dev.cosmerearcanists.filing.Filings.vial(java.util.Map.of("iron",3,"steel",13));
        var result=((MetalVialItem)vial.getItem()).finishUsingItem(vial,helper.getLevel(),p);
        helper.assertTrue(PowerData.reserve(p,"iron")==100 && PowerData.reserve(p,"steel")==100 && dev.cosmerearcanists.filing.Filings.contents(result).equals(java.util.Map.of("iron",1,"steel",11)),"Overflow must retain one iron and eleven steel filings instead of silently wasting metal");
        MetalDetector.rebuild(helper.getLevel().getServer());
        helper.assertTrue(MetalDetector.detects(p.server,result) && Math.abs(MetalAmounts.units(p.server,result)-3.0)<1e-9,"Vial detection and projectile mass must use actual contents");
        var aluminum=dev.cosmerearcanists.filing.Filings.vial(java.util.Map.of("iron",15,"aluminum",1));
        helper.assertTrue(!MetalDetector.detects(p.server,aluminum) && !MetalDetector.detects(p.server,dev.cosmerearcanists.filing.Filings.filing("duralumin",1)),"Any aluminum-bearing filing must exclude the whole item");
        var legacy=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"iron_vial")));
        helper.assertTrue(dev.cosmerearcanists.filing.Filings.contents(legacy).equals(java.util.Map.of("iron",1)),"Old saved vial IDs must remain usable as one filing");
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.IRON_NUGGET));dev.cosmerearcanists.filing.Filings.file(p);
        helper.assertTrue(Math.abs(MetalAmounts.units(p.server,p.getOffhandItem())-.75)<1e-9,"Partially filed metal must have reduced projectile mass");
        helper.succeed();
    }
    private static ServerPlayer pewterPlayer(GameTestHelper helper,String name) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),name)) {
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1))));
        p.getAbilities().invulnerable=false;
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(20);
        // Damage tests use out_of_world to bypass ServerPlayer spawn protection, which FakePlayer.tick never advances.
        PowerAttributes.set(p,"a:pewter",1);PowerData.state(p,"a:pewter",2);PowerData.reserve(p,"pewter",300);
        return p;
    }
    @GameTest(template = "empty")
    public static void pewterDamageDebtAndStopping(GameTestHelper helper) {
        var p=pewterPlayer(helper,"PewterDamage");PewterService.tick(p);
        double speed=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double attack=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        helper.assertTrue(speed>.1 && attack>=4,"Burning pewter must enhance movement and melee attacks without holding activation");
        p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().fellOutOfWorld(),10);
        helper.assertTrue(Math.abs(p.getHealth()-13.25)<.01 && Math.abs(PewterService.debt(p)-.75)<.01,"10 damage must become 6.75 immediate + .75 deferred after 25% mitigation; health="+p.getHealth()+", debt="+PewterService.debt(p));
        p.setAbsorptionAmount(4);p.invulnerableTime=20;
        PowerData.state(p,"a:pewter",0);PewterService.tick(p);
        helper.assertTrue(Math.abs(p.getHealth()-12.5)<.01 && PewterService.debt(p)==0 && p.getAbsorptionAmount()==4,"Stopping must settle once without absorption or hurt-immunity discounts; health="+p.getHealth()+", absorption="+p.getAbsorptionAmount()+", debt="+PewterService.debt(p));
        helper.assertTrue(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.1)<1e-6 && p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==1,"Pewter modifiers must be removed on stop");
        PewterService.tick(p);helper.assertTrue(Math.abs(p.getHealth()-12.5)<.01,"Settled debt cannot be charged twice");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void pewterBurnScalingExhaustionAndAbsorption(GameTestHelper helper) {
        var p=pewterPlayer(helper,"PewterScaling");
        double[] immediate={7.875,6.75,4.5};
        for(int level=1;level<=3;level++) {
            PowerData.state(p,"a:pewter",level);PewterService.debt(p,0);p.setHealth(20);p.invulnerableTime=0;
            p.hurt(p.damageSources().fellOutOfWorld(),10);
            helper.assertTrue(Math.abs((20-p.getHealth())-immediate[level-1])<.01,"Pewter health damage must scale by burn level "+level);
        }
        PowerAttributes.set(p,"a:pewter",2);PowerData.state(p,"a:pewter",3);p.setHealth(20);p.invulnerableTime=0;PewterService.debt(p,0);
        p.hurt(p.damageSources().fellOutOfWorld(),10);
        helper.assertTrue(Math.abs(p.getHealth()-17.3)<.01 && Math.abs(PewterService.debt(p)-.3)<.01,"Stronger Flaring must cap mitigation at 70%");
        PowerAttributes.set(p,"a:pewter",1);PowerData.state(p,"a:pewter",2);p.setHealth(20);p.setAbsorptionAmount(10);p.invulnerableTime=0;PewterService.debt(p,0);
        p.hurt(p.damageSources().fellOutOfWorld(),10);
        helper.assertTrue(PewterService.debt(p)==0 && p.getHealth()==20,"Absorbed hits cannot create deferred injury; health="+p.getHealth()+", absorption="+p.getAbsorptionAmount()+", debt="+PewterService.debt(p));
        p.getFoodData().setExhaustion(0);p.causeFoodExhaustion(4);
        helper.assertTrue(Math.abs(p.getFoodData().getExhaustionLevel()-2)<.01,"Actual exhaustion entry point must apply pewter endurance before hunger thresholds");
        PowerData.state(p,"a:pewter",0);p.getFoodData().setExhaustion(0);p.causeFoodExhaustion(4);
        helper.assertTrue(Math.abs(p.getFoodData().getExhaustionLevel()-4)<.01,"Inactive pewter must leave vanilla exhaustion unchanged");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void pewterRestSaturationAndHealing(GameTestHelper helper) {
        var rest=pewterPlayer(helper,"PewterRest");rest.setOnGround(true);rest.getFoodData().setSaturation(0);PewterService.debt(rest,10);
        for(int i=0;i<101;i++)PewterService.tick(rest);
        helper.assertTrue(Math.abs(PewterService.debt(rest)-9.9)<.001,"Five seconds standing still must clear 1% of current debt");
        var moving=pewterPlayer(helper,"PewterMoving");moving.setOnGround(true);moving.getFoodData().setSaturation(0);PewterService.debt(moving,10);
        for(int i=0;i<101;i++){moving.setPos(moving.position().add(.1,0,0));PewterService.tick(moving);}
        helper.assertTrue(PewterService.debt(moving)==10,"Continuous movement must not earn rest recovery");
        var fed=pewterPlayer(helper,"PewterFed");fed.getFoodData().setSaturation(2);PewterService.debt(fed,2);
        for(int i=0;i<20;i++)PewterService.tick(fed);
        helper.assertTrue(Math.abs(PewterService.debt(fed)-1.8)<.001 && Math.abs(fed.getFoodData().getSaturationLevel()-1.8)<.001,"Passive drag recovery must consume saturation proportionally");
        var healing=pewterPlayer(helper,"PewterHealing");healing.setHealth(10);PowerData.state(healing,"a:pewter",3);
        for(int i=0;i<200;i++)PewterService.tick(healing);
        helper.assertTrue(Math.abs(healing.getHealth()-12)<.01,"Slight healing must scale with Flaring strength");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void pewterDepletionPersistenceAndConfigGates(GameTestHelper helper) {
        var p=pewterPlayer(helper,"PewterPersistence");PewterService.debt(p,2);PewterService.tick(p);
        double configured=PewterConfig.ENABLED.get()?1:0;
        try {
            PewterConfig.ENABLED.set(false);PewterService.tick(p);
            helper.assertTrue(PewterService.debt(p)==2 && p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==1,"Disabled pewter must remove effects while preserving debt");
        } finally {PewterConfig.ENABLED.set(configured==1);}
        var restored=pewterPlayer(helper,"PewterRestored");restored.getPersistentData().put(PowerData.KEY,PowerData.data(p).copy());
        helper.assertTrue(PewterService.debt(restored)==2,"Persistent power data must retain debt");
        PowerData.reserve(restored,"pewter",.001);restored.setHealth(20);PewterService.tick(restored);
        helper.assertTrue(PowerData.state(restored,"a:pewter")==0 && Math.abs(restored.getHealth()-18)<.01 && PewterService.debt(restored)==0,"Empty reserve must turn off pewter and settle injury immediately");
        var status=new PowerNetwork.PewterStatus(helper.getLevel().dimension().location().toString(),2.75,true,2);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
        try {PowerNetwork.PewterStatus.CODEC.encode(buffer,status);helper.assertTrue(PowerNetwork.PewterStatus.CODEC.decode(buffer).equals(status),"Gray-heart status must round-trip dimension, debt and strength");}finally{buffer.release();}
        var vial=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"pewter_vial"));
        helper.assertTrue(vial instanceof MetalVialItem,"Pewter must have a refill vial");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void equipmentRecipesAndCrossbowAmmo(GameTestHelper helper) {
        var level=helper.getLevel(); MetalDetector.rebuild(level.getServer());
        var diagonal=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT)));
        var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,diagonal,level).orElseThrow();
        var result=recipe.value().assemble(diagonal,level.registryAccess());
        helper.assertTrue(result.is(dev.cosmerearcanists.equipment.EquipmentContent.SPIKES.get("iron").get()) && result.getCount()==2,"Two diagonal ingots must craft two metal spikes");
        var crossbow=dev.cosmerearcanists.equipment.EquipmentContent.CROSSBOW.get();
        helper.assertTrue(crossbow.getAllSupportedProjectiles().test(result) && !crossbow.getAllSupportedProjectiles().test(new ItemStack(Items.ARROW)),"Spike crossbow must accept spikes instead of arrows");
        for(String metal:java.util.List.of("iron","copper","gold","steel","aluminum","duralumin")){
            String namespace=java.util.List.of("iron","copper","gold").contains(metal)?"minecraft":CosmereArcanists.MOD_ID;
            var ingot=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace,metal+"_ingot")));
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(ingot,new ItemStack(Items.STRING),new ItemStack(Items.STRING),new ItemStack(Items.STRING)));
            var matched=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level);
            boolean ball=matched.isPresent() && matched.get().value().getResultItem(level.registryAccess()).is(dev.cosmerearcanists.equipment.EquipmentContent.BALL.get());
            helper.assertTrue(ball != (metal.equals("aluminum")||metal.equals("duralumin")),"Ball recipe metal policy failed for "+metal);
        }
        helper.assertTrue(MetalDetector.detects(level.getServer(),dev.cosmerearcanists.equipment.EquipmentContent.BALL.get().getDefaultInstance()),"Ball must be detectable metal");
        helper.assertTrue(!MetalDetector.detects(level.getServer(),dev.cosmerearcanists.equipment.EquipmentContent.SPIKES.get("aluminum").get().getDefaultInstance()),"Aluminum spikes must be undetectable");
        var owner=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"SpikeShooter"));
        var spike=crossbow.createProjectile(level,owner,crossbow.getDefaultInstance(),result.copyWithCount(1),false);
        crossbow.shootProjectile(owner,spike,0,3.15f,0,0,null);
        helper.assertTrue(spike instanceof dev.cosmerearcanists.equipment.EquipmentProjectile && spike.getDeltaMovement().length()>3,"Crossbow shooting must create a moving spike projectile");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void embeddedSpikesAreMetalAnchors(GameTestHelper helper) {
        var level=helper.getLevel(); MetalDetector.rebuild(level.getServer());
        var anchor=helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1)); level.setBlock(anchor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var spike=new dev.cosmerearcanists.equipment.EquipmentProjectile(dev.cosmerearcanists.equipment.EquipmentContent.PROJECTILE.get(),level);
        spike.configure(dev.cosmerearcanists.equipment.EquipmentProjectile.SPIKE,dev.cosmerearcanists.equipment.EquipmentContent.SPIKES.get("iron").get().getDefaultInstance(),9);
        spike.setPos(net.minecraft.world.phys.Vec3.atCenterOf(anchor).add(0,0,-.6));
        level.addFreshEntity(spike); spike.onHit(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(anchor).add(0,0,-.5),net.minecraft.core.Direction.NORTH,anchor,false));
        helper.assertTrue(spike.latched() && spike.fixedMetalAnchor() && spike.allomanticallyDetectable(level.getServer()),"Embedded iron spike must be a detectable fixed anchor");
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"SpikePuller"));
        player.setPos(spike.getX(),spike.getY()-1,spike.getZ()-3);
        var before=spike.position(); var target=MetalTarget.entity(spike,7);
        var direction=PowerService.anchorDirection(PowerService.chest(player),target.position(level,1),"a:iron");
        var impulse=PowerService.targetImpulse(player,target,direction,14);
        helper.assertTrue(impulse.dot(direction)>0 && spike.position().equals(before) && spike.getDeltaMovement().lengthSqr()==0,"Pulling an embedded spike must move the user toward the anchor without moving the embedded spike");
        var saved=new net.minecraft.nbt.CompoundTag();spike.addAdditionalSaveData(saved);
        var restored=new dev.cosmerearcanists.equipment.EquipmentProjectile(dev.cosmerearcanists.equipment.EquipmentContent.PROJECTILE.get(),level);restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.latched() && restored.fixedMetalAnchor(),"Anchored spike must survive save/load");
        level.setBlock(anchor,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);spike.tick();
        helper.assertTrue(!spike.latched(),"Breaking the support must release an embedded spike");spike.discard();helper.succeed();
    }
    @GameTest(template = "empty")
    public static void grappleTensionSpaceAndRecall(GameTestHelper helper) {
        var level=helper.getLevel();MetalDetector.rebuild(level.getServer());
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"Grappler"));
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,4,1));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var item=dev.cosmerearcanists.equipment.EquipmentContent.GRAPPLE.get(); var stack=item.getDefaultInstance();player.getInventory().setItem(0,stack);
        helper.assertTrue(item.launch(player,stack,1),"Normal grappling must work without Allomancy");
        var hook=(dev.cosmerearcanists.equipment.EquipmentProjectile)level.getEntity(dev.cosmerearcanists.equipment.TetherItem.active(stack));
        var anchor=pos.above(6);level.setBlock(anchor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        hook.onHit(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(anchor),net.minecraft.core.Direction.DOWN,anchor,false));
        helper.assertTrue(hook.latched() && hook.fixedMetalAnchor(),"Grapple must latch to a solid block as an iron anchor");
        double length=hook.ropeLength();player.setDeltaMovement(.25,0,0);
        PowerService.control(player,new PowerNetwork.Control(9,"",1));hook.tick();
        helper.assertTrue(hook.ropeLength()<length && player.getDeltaMovement().y>0,"Space must shorten rope and pull upward slowly");
        helper.assertTrue(Math.abs(player.getDeltaMovement().x-.25)<1e-6,"Rope tension must retain tangential swing momentum");
        hook.reel();helper.assertTrue(hook.returning() && !hook.fixedMetalAnchor(),"Recall must unlatch the hook");
        hook.setPos(player.getEyePosition());hook.tick();
        helper.assertTrue(hook.isRemoved() && dev.cosmerearcanists.equipment.TetherItem.active(stack)==null && player.getInventory().getItem(0)==stack,"Recall must return use of the original item without duplicating it");
        PowerService.control(player,new PowerNetwork.Control(9,"",0));helper.succeed();
    }
    @GameTest(template = "empty")
    public static void reusableBallUsesSteelStrengthAndSingleItem(GameTestHelper helper) {
        var level=helper.getLevel();MetalDetector.rebuild(level.getServer());
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"BallUser"));
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var item=dev.cosmerearcanists.equipment.EquipmentContent.BALL.get();var stack=item.getDefaultInstance();player.getInventory().setItem(0,stack);
        helper.assertTrue(!item.launch(player,stack,1),"Ball launch must require active steel");
        PowerAttributes.set(player,"a:steel",1);PowerData.state(player,"a:steel",2);PowerData.reserve(player,"steel",10);PowerService.control(player,new PowerNetwork.Control(2,"",1));
        helper.assertTrue(item.launch(player,stack,1),"Active steel must launch the reusable ball");
        var ball=(dev.cosmerearcanists.equipment.EquipmentProjectile)level.getEntity(dev.cosmerearcanists.equipment.TetherItem.active(stack));
        double speed=ball.getDeltaMovement().length();ball.reel();ball.setPos(player.getEyePosition());ball.tick();
        PowerAttributes.set(player,"a:steel",2);
        helper.assertTrue(item.launch(player,stack,1),"Reeled ball must be reusable");
        var stronger=(dev.cosmerearcanists.equipment.EquipmentProjectile)level.getEntity(dev.cosmerearcanists.equipment.TetherItem.active(stack));
        helper.assertTrue(stronger.getDeltaMovement().length()>speed*1.9 && player.getInventory().getItem(0)==stack,"Higher steel attribute must strengthen the same reusable weapon");
        stronger.reel();stronger.setPos(player.getEyePosition());stronger.tick();PowerService.control(player,new PowerNetwork.Control(2,"",0));helper.succeed();
    }
    @GameTest(template = "empty")
    public static void secondaryAssignmentsAndIndependentHolds(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"DualPowers"),ClientInformation.createDefault());
        PowerAttributes.set(player,"a:steel",1); PowerAttributes.set(player,"a:iron",1);
        helper.assertTrue(PowerData.assignSlot(player,"a:steel",0) && PowerData.assignSlot(player,"a:iron",1),"Available powers must assign to both slots");
        helper.assertTrue(PowerData.selected(player).equals("a:steel") && PowerData.secondary(player).equals("a:iron"),"Slots must be distinct");
        helper.assertTrue(!PowerData.assignSlot(player,"a:pewter",1),"Unavailable powers cannot be assigned");
        PowerData.state(player,"a:steel",2); PowerData.state(player,"a:iron",2); PowerData.reserve(player,"steel",10);
        PowerService.control(player,new PowerNetwork.Control(2,"",1));
        PowerService.control(player,new PowerNetwork.Control(7,"",3));
        helper.assertTrue(PowerService.holdingAbility(player,"a:steel") && PowerService.holdingAbility(player,"a:iron"),"Both activation keys must be held independently");
        helper.assertTrue(!PowerService.holdingAlternateAbility(player,"a:steel") && PowerService.holdingAlternateAbility(player,"a:iron"),"Alt modes must be independent per key");
        PowerService.control(player,new PowerNetwork.Control(2,"",0));
        helper.assertTrue(!PowerService.holdingAbility(player,"a:steel") && PowerService.holdingAbility(player,"a:iron"),"Primary release must not stop secondary");
        PowerData.assignSlot(player,"a:steel",1);
        helper.assertTrue(PowerData.selected(player).equals("a:iron") && PowerData.secondary(player).equals("a:steel"),"Moving an assigned power must swap the two slots");
        PowerService.control(player,new PowerNetwork.Control(7,"",0)); PowerService.control(player,new PowerNetwork.Control(7,"",1));
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.IRON_NUGGET));
        helper.assertTrue(MetalProjectiles.launch(player,drop)!=null,"Steel assigned to secondary must launch projectiles with its own held key");
        level.getEntitiesOfClass(SteelProjectile.class,player.getBoundingBox().inflate(3),e->e.getOwner()==player).forEach(net.minecraft.world.entity.Entity::discard);
        var clone=new ServerPlayer(level.getServer(),level,new GameProfile(player.getUUID(),"DualPowers"),ClientInformation.createDefault());
        PowerData.clonePlayer(player,clone);
        helper.assertTrue(PowerData.selected(clone).equals("a:iron") && PowerData.secondary(clone).equals("a:steel"),"Both assignments must survive respawn");
        helper.assertTrue(PowerData.state(clone,"a:steel")==0 && !PowerService.holdingAbility(clone,"a:iron"),"Respawn must reset states; primary hold is released");
        var snapshot=new PowerNetwork.Snapshot(level.dimension().location().toString(),"a:iron","a:steel",5,java.util.List.of(),java.util.List.of());
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            PowerNetwork.Snapshot.CODEC.encode(buffer,snapshot);
            helper.assertTrue(PowerNetwork.Snapshot.CODEC.decode(buffer).equals(snapshot),"Snapshot must transmit both assignments");
        } finally {buffer.release();}
        PowerService.control(player,new PowerNetwork.Control(7,"",0)); helper.succeed();
    }
    @GameTest(template = "empty")
    public static void twoActivationChannelsDispatchIndependently(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"DualDispatch"),ClientInformation.createDefault());
        var events=new java.util.ArrayList<String>();
        PowerService.ABILITIES.register("test:primary",(p,key,phase,targets)->events.add("p"+phase));
        PowerService.ABILITIES.register("test:secondary",(p,key,phase,targets)->events.add("s"+phase));
        PowerData.state(player,"test:primary",1); PowerData.state(player,"test:secondary",1);
        var session=new PowerService.Session(); session.applying=true; session.secondary.applying=true;
        session.lastApply=session.secondary.lastApply=level.getGameTime();
        var available=java.util.List.of("test:primary","test:secondary");
        PowerService.advanceActivation(player,session,session,"test:primary",available);
        PowerService.advanceActivation(player,session,session.secondary,"test:secondary",available);
        session.applying=false;
        PowerService.advanceActivation(player,session,session,"test:primary",available);
        PowerService.advanceActivation(player,session,session.secondary,"test:secondary",available);
        session.secondary.applying=false;
        PowerService.advanceActivation(player,session,session.secondary,"test:secondary",available);
        helper.assertTrue(events.equals(java.util.List.of("pSTART","sSTART","pSTOP","sTICK","sSTOP")),"Releasing one activation channel must not interrupt the other handler");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void projectileLaunchUsesBurnLevelAndAttribute(GameTestHelper helper) {
        var level=helper.getLevel(); MetalDetector.rebuild(level.getServer());
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"ScaledShooter"),ClientInformation.createDefault());
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1)); player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        PowerData.reserve(player,"steel",10); PowerService.control(player,new PowerNetwork.Control(2,"",1));
        int[][] cases={{1,1},{1,2},{2,2},{1,3},{2,1}};
        double[] expected={3.5,7,8,8,7};
        for(int i=0;i<cases.length;i++) {
            PowerAttributes.set(player,"a:steel",cases[i][0]); PowerData.state(player,"a:steel",cases[i][1]);
            var drop=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.IRON_NUGGET));
            helper.assertTrue(MetalProjectiles.launch(player,drop)!=null,"Valid held steel must launch at each tested strength");
            var shots=level.getEntitiesOfClass(SteelProjectile.class,player.getBoundingBox().inflate(3),e->e.getOwner()==player);
            helper.assertTrue(shots.size()==1 && Math.abs(shots.getFirst().getDeltaMovement().length()-expected[i])<1e-6,"Actual shot speed must follow attribute and burn level: case "+i);
            shots.forEach(net.minecraft.world.entity.Entity::discard);
        }
        PowerService.control(player,new PowerNetwork.Control(2,"",0)); helper.succeed();
    }
    @GameTest(template = "empty")
    public static void otherAllomancerCanPushAndPullProjectiles(GameTestHelper helper) {
        var level=helper.getLevel(); MetalDetector.rebuild(level.getServer());
        var defender=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"Deflector"),ClientInformation.createDefault());
        var shooter=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"Shooter"),ClientInformation.createDefault());
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1));
        defender.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        PowerAttributes.set(defender,"a:iron",1); PowerAttributes.set(defender,"a:steel",1);
        PowerData.state(defender,"a:iron",2); PowerData.state(defender,"a:steel",2);
        var shot=new SteelProjectile(MetalProjectiles.STEEL.get(),level); shot.setOwner(shooter);
        var point=PowerService.chest(defender).add(0,0,4);
        shot.setPos(point.x,point.y-shot.getBbHeight()*.5,point.z); shot.cargo(new ItemStack(Items.IRON_NUGGET),1);
        shot.setDeltaMovement(0,0,-3.5); level.addFreshEntity(shot);
        var target=MetalTarget.entity(shot,7);
        helper.assertTrue(PowerService.targets(defender,new PowerService.Session()).contains(target),"Another player's fired metal must be included in blue-line targets");
        var push=PowerService.anchorDirection(PowerService.chest(defender),target.position(level,1),"a:steel");
        PowerService.targetImpulse(defender,target,push,PowerService.powerForce(defender,"a:steel"));
        double lightChange=shot.getDeltaMovement().z+3.5;
        helper.assertTrue(lightChange>0 && Math.abs(shot.getDeltaMovement().y)<1e-6,"Steel must deflect the incoming shot away along the real anchor line");
        helper.assertTrue(shot.hurtMarked && shot.getOwner()==shooter,"Trajectory updates must sync without replacing the original shooter");
        var heavy=new SteelProjectile(MetalProjectiles.STEEL.get(),level); heavy.cargo(new ItemStack(Items.ANVIL),279);
        heavy.setPos(shot.position()); heavy.setDeltaMovement(0,0,-3.5); level.addFreshEntity(heavy);
        PowerService.targetImpulse(defender,MetalTarget.entity(heavy,7),push,PowerService.powerForce(defender,"a:steel"));
        helper.assertTrue(heavy.getDeltaMovement().z+3.5<lightChange,"Heavy shots must resist deflection more than nuggets");
        shot.setDeltaMovement(0,0,3.5);
        var pull=PowerService.anchorDirection(PowerService.chest(defender),target.position(level,1),"a:iron");
        PowerService.targetImpulse(defender,target,pull,PowerService.powerForce(defender,"a:iron"));
        helper.assertTrue(shot.getDeltaMovement().z<3.5,"Iron must pull the shot toward the Allomancer");
        var aluminum=new SteelProjectile(MetalProjectiles.STEEL.get(),level);
        aluminum.cargo(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"duralumin_ingot"))),9);
        aluminum.setPos(shot.position()); level.addFreshEntity(aluminum);
        helper.assertTrue(!PowerService.targets(defender,new PowerService.Session()).contains(MetalTarget.entity(aluminum,7)),"Aluminum exclusion must still apply to projectile cargo");
        shot.discard(); heavy.discard(); aluminum.discard(); helper.succeed();
    }
    @GameTest(template = "empty")
    public static void alternateSteelDropsMetalDownward(GameTestHelper helper) {
        var level=helper.getLevel(); MetalDetector.rebuild(level.getServer());
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"DownDropper"),ClientInformation.createDefault());
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,4,1));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); player.setYRot(90); player.setXRot(-45);
        PowerAttributes.set(player,"a:steel",1); PowerData.state(player,"a:steel",2); PowerData.reserve(player,"steel",10);
        PowerService.control(player,new PowerNetwork.Control(2,"",3)); // Held activation with alternate latched.
        var stack=new ItemStack(Items.IRON_NUGGET,16);
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,player.getX()+1,player.getEyeY(),player.getZ(),stack);
        drop.setDeltaMovement(1,.3,1); drop.setPickUpDelay(40);
        helper.assertTrue(MetalProjectiles.launch(player,drop)==null,"Alternate activation must retain a regular item instead of launching a projectile");
        helper.assertTrue(drop.getX()==player.getX() && drop.getZ()==player.getZ() && drop.getY()<player.getY()+.3,"Drop must start at the feet directly below the player");
        helper.assertTrue(drop.getDeltaMovement().x==0 && drop.getDeltaMovement().z==0 && drop.getDeltaMovement().y<0,"Drop must fall straight down regardless of aim or original toss velocity");
        helper.assertTrue(drop.getItem().getCount()==16 && drop.hasPickUpDelay(),"Stack and pickup delay must remain intact");
        helper.assertTrue(PowerData.reserve(player,"steel")==10,"Downward drops must not spend projectile reserves");
        PowerService.control(player,new PowerNetwork.Control(2,"",0));
        PowerService.control(player,new PowerNetwork.Control(2,"",1));
        var recoil=MetalProjectiles.launch(player,drop);
        helper.assertTrue(recoil!=null && Math.abs(PowerData.reserve(player,"steel")-(10-PowerConfig.reserveCost(1)))<1e-7,"Normal steel activation must still launch and spend shot fuel");
        level.getEntitiesOfClass(SteelProjectile.class,player.getBoundingBox().inflate(3),e->e.getOwner()==player).forEach(net.minecraft.world.entity.Entity::discard);
        PowerService.control(player,new PowerNetwork.Control(2,"",0)); helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void projectileActivationHeartbeatExpires(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"StaleThrower"),ClientInformation.createDefault());
        PowerAttributes.set(player,"a:steel",1); PowerData.state(player,"a:steel",2); PowerData.reserve(player,"steel",10);
        PowerService.control(player,new PowerNetwork.Control(2,"",1));
        helper.assertTrue(PowerService.holdingAbility(player,"a:steel"),"Fresh hold must authorize a steel activation");
        helper.runAfterDelay(16,()->{
            var dropped=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.IRON_NUGGET));
            helper.assertTrue(!PowerService.holdingAbility(player,"a:steel") && MetalProjectiles.launch(player,dropped)==null,"Missing hold heartbeats must disable launches");
            helper.assertTrue(PowerData.reserve(player,"steel")==10,"Expired holds must not spend reserves");
            PowerService.control(player,new PowerNetwork.Control(2,"",0)); helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void thrownMetalLaunchAndRecoil(GameTestHelper helper) {
        var level=helper.getLevel(); var server=level.getServer(); MetalDetector.rebuild(server);
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"MetalThrower"),ClientInformation.createDefault());
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,6,1));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        PowerAttributes.set(player,"a:steel",1); PowerData.state(player,"a:steel",2); PowerData.reserve(player,"steel",10);
        var nugget=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.IRON_NUGGET));
        var anvil=new net.minecraft.world.entity.item.ItemEntity(level,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.ANVIL));
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"Burning without holding activate must leave an ordinary drop");
        helper.assertTrue(PowerData.reserve(player,"steel")==10,"Ordinary drops must not consume shot reserves");
        PowerService.control(player,new PowerNetwork.Control(5,"",0));
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"A brief activation tap must not count as holding G");
        PowerAttributes.set(player,"a:iron",1); PowerData.data(player).putString("selected","a:iron");
        PowerService.control(player,new PowerNetwork.Control(2,"",1));
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"Holding activate for iron must not launch steel shots");
        PowerData.data(player).putString("selected","a:steel");
        var light=MetalProjectiles.launch(player,nugget); var heavy=MetalProjectiles.launch(player,anvil);
        helper.assertTrue(light!=null && heavy!=null && heavy.length()>light.length()*50,"Anvil launch must apply much greater recoil than nugget launch");
        helper.assertTrue(Math.abs(PowerData.reserve(player,"steel")-(10-PowerConfig.reserveCost(2)))<1e-7,"Each successful launch must deduct its extra reserve cost");
        var shots=level.getEntitiesOfClass(SteelProjectile.class,player.getBoundingBox().inflate(3),e->e.getOwner()==player);
        helper.assertTrue(shots.size()==2,"Each thrown stack must launch exactly one shot");
        var speeds=shots.stream().mapToDouble(e->e.getDeltaMovement().length()).sorted().toArray();
        helper.assertTrue(speeds[0]<speeds[1]*.3,"Anvil must fly much slower than nugget");
        PowerService.control(player,new PowerNetwork.Control(2,"",0));
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"Releasing G must immediately restore ordinary drops");
        helper.assertTrue(Math.abs(PowerData.reserve(player,"steel")-(10-PowerConfig.reserveCost(2)))<1e-7,"Rejected launches must not deduct fuel");
        PowerService.control(player,new PowerNetwork.Control(2,"",1));
        PowerData.state(player,"a:steel",0);
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"Throwing without burning must remain an ordinary drop");
        shots.forEach(net.minecraft.world.entity.Entity::discard); helper.succeed();
    }
    @GameTest(template = "empty")
    public static void metalProjectileContentAndRecovery(GameTestHelper helper) {
        var level=helper.getLevel(); var server=level.getServer(); MetalDetector.rebuild(server);
        helper.assertTrue(MetalAmounts.units(server,new ItemStack(Items.IRON_NUGGET))==1,"Nugget must contain one unit");
        helper.assertTrue(MetalAmounts.units(server,new ItemStack(Items.IRON_INGOT))==9,"Ingot must contain nine nugget units");
        helper.assertTrue(MetalAmounts.units(server,new ItemStack(Items.ANVIL))==279,"Anvil must include three iron blocks and four ingots");
        helper.assertTrue(MetalAmounts.units(server,new ItemStack(Items.IRON_NUGGET,16))==16,"Stack count must affect total metal");
        var aluminum=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"duralumin_ingot")));
        helper.assertTrue(MetalAmounts.units(server,aluminum)==0,"Aluminum-containing objects must not launch");
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,3,1));
        var projectile=new SteelProjectile(MetalProjectiles.STEEL.get(),level);
        projectile.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        projectile.cargo(new ItemStack(Items.IRON_NUGGET,16),16);
        var saved=new net.minecraft.nbt.CompoundTag(); projectile.addAdditionalSaveData(saved);
        var restored=new SteelProjectile(MetalProjectiles.STEEL.get(),level); restored.readAdditionalSaveData(saved);
        var savedAgain=new net.minecraft.nbt.CompoundTag(); restored.addAdditionalSaveData(savedAgain);
        helper.assertTrue(ItemStack.parseOptional(level.registryAccess(),savedAgain.getCompound("Cargo")).getCount()==16,"Projectile must retain the exact carried stack across save/load");
        projectile.onHit(new net.minecraft.world.phys.BlockHitResult(projectile.position(),net.minecraft.core.Direction.UP,pos,false));
        helper.assertTrue(projectile.isRemoved(),"Impact must end the projectile");
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->e.getItem().is(Items.IRON_NUGGET)&&e.getItem().getCount()==16);
        helper.assertTrue(drops.size()==1,"Impact must recover exactly one original stack");
        projectile.onHit(new net.minecraft.world.phys.BlockHitResult(projectile.position(),net.minecraft.core.Direction.UP,pos,false));
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->e.getItem().is(Items.IRON_NUGGET)&&e.getItem().getCount()==16).size()==1,"Repeated impact must not duplicate cargo");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void metalProjectileSweptCollision(GameTestHelper helper) {
        var level=helper.getLevel(); var start=helper.absolutePos(new net.minecraft.core.BlockPos(1,4,1));
        // The small empty template is encased by the harness; clear a real airborne corridor.
        for(var block:net.minecraft.core.BlockPos.betweenClosed(start.offset(0,-1,-1),start.offset(5,3,1)))
            level.setBlock(block,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);
        cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(start.getX()+3.5,start.getY(),start.getZ()+.5); level.addFreshEntity(cow);
        float health=cow.getHealth();
        var projectile=new SteelProjectile(MetalProjectiles.STEEL.get(),level);
        projectile.setPos(start.getX()+.5,start.getY()+.5,start.getZ()+.5);
        projectile.cargo(new ItemStack(Items.IRON_NUGGET),1); projectile.setDeltaMovement(new net.minecraft.world.phys.Vec3(3.5,0,0)); level.addFreshEntity(projectile);
        helper.runAfterDelay(5,()->{
            helper.assertTrue(projectile.isRemoved(),"Fast shot must collide instead of tunneling through a mob");
            helper.assertTrue(Math.abs((health-cow.getHealth())-2)<.2,"Reference-speed nugget must deal about two damage before armor; before="+health+", after="+cow.getHealth()+", impact="+projectile.position());
            cow.discard(); helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void threeBurnLevelsAndLegacyMigration(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "BurnLevels"), ClientInformation.createDefault());
        var legacy = new net.minecraft.nbt.CompoundTag(); var states = new net.minecraft.nbt.CompoundTag();
        states.putInt("a:iron",1); states.putInt("a:steel",2); states.putInt("f:iron",3);
        legacy.put("states",states); player.getPersistentData().put(PowerData.KEY,legacy);
        helper.assertTrue(PowerData.state(player,"a:iron")==2 && PowerData.state(player,"a:steel")==3, "Old burning/flaring states must retain their meanings");
        helper.assertTrue(PowerData.state(player,"f:iron")==3, "Feruchemy levels must be preserved");
        PowerAttributes.set(player,"a:iron",1);
        double burning = PowerService.powerForce(player,"a:iron");
        PowerData.state(player,"a:iron",1);
        helper.assertTrue(PowerData.state(player,"a:iron")==1, "Low burn must not be migrated a second time");
        helper.assertTrue(PowerService.powerForce(player,"a:iron")==burning*.5, "Low burn must apply half strength");
        PowerData.state(player,"a:iron",3);
        helper.assertTrue(PowerService.powerForce(player,"a:iron")==burning*2, "Flaring must apply twice burning strength");
        PowerData.state(player,"a:iron",0);
        helper.assertTrue(PowerService.powerForce(player,"a:iron")==0, "Off must apply no force");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void ironAlternateHoldsAndChangesDirection(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"IronMovement"));
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(1,10,1));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); player.setYRot(0);
        PowerAttributes.set(player,"a:iron",1); PowerData.state(player,"a:iron",2); PowerData.reserve(player,"iron",100);
        var session=new PowerService.Session();
        // Route each slot through the real control handler and registered ability.
        PowerData.assignSlot(player,"a:iron",0);
        var above=MetalTarget.block(pos.above(4));
        var ahead=MetalTarget.block(pos.offset(0,1,4));
        int[] elapsed={0};
        helper.onEachTick(()->{
            int tick=elapsed[0]++;
            if(tick>=100)return;
            boolean secondary=tick>=50;
            if(tick==50) {
                PowerService.control(player,new PowerNetwork.Control(2,"",0));
                PowerData.assignSlot(player,"a:iron",1);
            }
            int direction=tick%20<10?64:4;
            // Keep activation held for five seconds, including direction changes and heartbeat refresh.
            PowerService.control(player,new PowerNetwork.Control(secondary?7:2,"",1|2|direction));
            session.targets=java.util.List.of(above,ahead);
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            PowerService.ABILITIES.activate(player,"a:iron",tick==0?AbilityDispatcher.Phase.START:AbilityDispatcher.Phase.TICK,session.targets);
            var motion=player.getDeltaMovement();
            helper.assertTrue(direction==64?motion.y>0 && Math.abs(motion.z)<1e-6:motion.z>0,
                "Held iron alternate must pull toward the requested anchor in either slot, tick "+tick);
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            PowerService.control(player,new PowerNetwork.Control(secondary?7:2,"",1|2|8));
            PowerService.ABILITIES.activate(player,"a:iron",AbilityDispatcher.Phase.TICK,session.targets);
            helper.assertTrue(player.getDeltaMovement().lengthSqr()<1e-12,"Iron must not invent backward thrust without metal behind");
            if(tick==99) {
                PowerService.control(player,new PowerNetwork.Control(7,"",0));
                helper.succeed();
            }
        });
    }
    @GameTest(template = "empty")
    public static void ironStrengthAndPullAssistance(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "IronAssist"), ClientInformation.createDefault());
        PowerAttributes.set(player,"a:iron",1); PowerAttributes.set(player,"a:steel",1);
        PowerData.state(player,"a:iron",2); PowerData.state(player,"a:steel",2);
        double steel = PowerService.powerForce(player,"a:steel");
        helper.assertTrue(steel == PowerConfig.FORCE.get() * 2, "Default steel Burning must match the previous attribute 2 force");
        PowerData.state(player,"a:steel",1);
        helper.assertTrue(PowerService.powerForce(player,"a:steel") == steel * .5, "Steel Low burn must retain half Burning force");
        PowerData.state(player,"a:steel",3);
        helper.assertTrue(PowerService.powerForce(player,"a:steel") == steel * 2, "Steel Flaring must retain twice Burning force");
        PowerData.state(player,"a:steel",2); PowerAttributes.set(player,"a:steel",2);
        helper.assertTrue(PowerService.powerForce(player,"a:steel") == steel * 2, "Steel attributes must stack with the base multiplier");
        PowerAttributes.set(player,"a:steel",1);
        helper.assertTrue(PowerService.powerForce(player,"a:iron") == steel, "Default iron and steel must have matching doubled base force");
        PowerAttributes.set(player,"a:iron",2);
        helper.assertTrue(PowerService.powerForce(player,"a:iron") == steel * 2, "Iron attribute strength must continue to scale");
        var chest = net.minecraft.world.phys.Vec3.ZERO;
        var above = new net.minecraft.world.phys.Vec3(0,4,0); var below = above.scale(-1);
        var ironAbove = PowerService.anchorDirection(chest,above,"a:iron");
        var ironBelow = PowerService.anchorDirection(chest,below,"a:iron");
        var steelAbove = PowerService.anchorDirection(chest,above,"a:steel");
        helper.assertTrue(ironAbove.y == 1 && ironBelow.y == -1 && steelAbove.y == -1, "Iron must pull toward anchors while steel pushes away");
        double[] weights = SteelMovement.weights(new double[]{0,1,0},new double[][]{
            {ironAbove.x,ironAbove.y,ironAbove.z},{ironBelow.x,ironBelow.y,ironBelow.z}},new double[]{1,1},.5);
        helper.assertTrue(weights[0] == 1 && weights[1] == 0, "Jump assistance must pull on overhead metal only");
        var right = PowerService.anchorDirection(chest,new net.minecraft.world.phys.Vec3(4,0,0),"a:iron");
        helper.assertTrue(right.x == 1 && right.y == 0, "Lateral iron assistance must pull toward the lateral anchor");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void fallingDiscoversNewAnchorsImmediately(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "FallingScan"), ClientInformation.createDefault());
        var start = helper.absolutePos(new net.minecraft.core.BlockPos(1, 20, 1));
        player.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
        PowerAttributes.set(player,"a:steel",1);PowerData.state(player,"a:steel",3);
        var session = new PowerService.Session();
        PowerService.scan(player,session);
        var lower = start.offset(0,-12,0);
        var nearby = lower.offset(1,-2,0); var distant = lower.offset(0,-10,0);
        level.setBlock(nearby,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(),3);
        level.setBlock(distant,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(),3);
        player.setPos(lower.getX()+.5,lower.getY(),lower.getZ()+.5);
        session.observedMovement = new net.minecraft.world.phys.Vec3(0,-2,0);
        PowerService.scan(player,session);
        helper.assertTrue(session.blocks.contains(nearby), "Current nearby anchors must appear on the first falling tick");
        helper.assertTrue(session.blocks.contains(distant), "Fall-priority scan must find range anchors before the old background pass finishes");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void hardnessBlocksDetection(GameTestHelper helper) {
        var level = helper.getLevel(); var server = level.getServer();
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "Resistance"), ClientInformation.createDefault());
        var start = helper.absolutePos(new net.minecraft.core.BlockPos(1, 3, 1));
        var end = start.offset(4, 0, 0);
        var from = net.minecraft.world.phys.Vec3.atCenterOf(start); var to = net.minecraft.world.phys.Vec3.atCenterOf(end);
        for (int x=0;x<=4;x++) level.setBlock(start.offset(x,0,0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(end, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(MetalResistance.cost(player,from,to)==0, "Target metal must not resist itself");
        level.setBlock(start.offset(2,0,0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        double stone=MetalResistance.cost(player,from,to);
        helper.assertTrue(stone>0, "Solid stone must resist detection");
        level.setBlock(start.offset(2,0,0), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
        double obsidian=MetalResistance.cost(player,from,to);
        helper.assertTrue(obsidian>stone, "Harder blocks must provide more resistance");
        helper.assertTrue(from.distanceTo(to)+obsidian>PowerConfig.RANGE.get(), "Obsidian must hide a distant target at default range");
        level.setBlock(start.offset(1,0,0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(MetalResistance.cost(player,from,to)>obsidian, "Multiple blockers must accumulate");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void nearFirstBlockScanning(GameTestHelper helper) {
        var offsets = PowerService.scanOffsets(16);
        helper.assertTrue(offsets.getFirst().equals(net.minecraft.core.BlockPos.ZERO), "Nearby blocks must be scanned first");
        helper.assertTrue(new java.util.HashSet<>(offsets).size() == offsets.size(), "Scan positions must be unique");
        helper.assertTrue(offsets.size() < 18000, "Scan budget must not be wasted on cube corners");
        double previous = -1;
        for (var offset : offsets) {
            double distance = offset.distSqr(net.minecraft.core.BlockPos.ZERO);
            helper.assertTrue(distance >= previous && distance <= 256, "Scan must progress outward inside the configured sphere");
            previous = distance;
        }
        helper.assertTrue(offsets.contains(new net.minecraft.core.BlockPos(16, 0, 0)) && offsets.contains(new net.minecraft.core.BlockPos(-16, 0, 0)), "Both range edges must be scanned");
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "ScanMovement"), ClientInformation.createDefault());
        var start = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        player.setPos(start.getX() + .5, start.getY(), start.getZ() + .5);
        var metal = start.offset(1, 0, 0);
        helper.getLevel().setBlock(metal, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), 3);
        PowerAttributes.set(player,"a:steel",1);PowerData.state(player,"a:steel",3);
        var session = new PowerService.Session();
        PowerService.scan(player, session);
        helper.assertTrue(session.blocks.contains(metal), "Nearby discoveries must publish before a pass finishes");
        int cursor = session.cursor;
        player.setPos(player.getX() + 5, player.getY(), player.getZ());
        PowerService.scan(player, session);
        helper.assertTrue(session.cursor > cursor, "Moving five blocks must not restart the scan");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void recipeDetection(GameTestHelper helper) {
        var server = helper.getLevel().getServer(); MetalDetector.rebuild(server);
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.IRON_INGOT)), "Iron must be detectable");
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.IRON_SWORD)), "Crafted metal equipment must be detectable");
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.ANVIL)), "Crafted metal blocks must be detectable");
        helper.assertTrue(!MetalDetector.detects(server, new ItemStack(Items.COAL)), "Coal is not metal");
        helper.assertTrue(!MetalDetector.detects(server, new ItemStack(Items.DIAMOND_ORE)), "Nonmetal ores must not become metal seeds");
        for (String id : new String[]{"aluminum_ingot", "duralumin_ingot", "aluminum_ore"})
            helper.assertTrue(!MetalDetector.detects(server, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, id)))), "Aluminum-containing " + id + " must be excluded");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void playerStatePersistence(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var original = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "TestOriginal"), ClientInformation.createDefault());
        var clone = new ServerPlayer(server, helper.getLevel(), new GameProfile(original.getUUID(), "TestOriginal"), ClientInformation.createDefault());
        new OriginData("scadrial", "human", "iron", "steel").save(original);
        PowerData.grant(original, "a:steel", true);
        PowerData.data(original).putString("selected", "a:steel");
        PowerData.state(original, "a:steel", 3); PowerData.reserve(original, "steel", 42);
        OriginData.copy(original, clone); PowerData.clonePlayer(original, clone);
        helper.assertTrue(PowerData.available(clone).containsAll(java.util.List.of("a:iron", "a:steel", "f:steel")), "Natural and granted powers must survive cloning");
        helper.assertTrue(PowerData.selected(clone).equals("a:steel"), "Selection must survive cloning");
        helper.assertTrue(PowerData.reserve(clone, "steel") == 42, "Reserves must survive cloning");
        helper.assertTrue(PowerData.state(clone, "a:steel") == 0, "States must be off after respawn");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void persistentPowerAttributes(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var original = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "Attributes"), ClientInformation.createDefault());
        var clone = new ServerPlayer(server, helper.getLevel(), new GameProfile(original.getUUID(), "Attributes"), ClientInformation.createDefault());
        new OriginData("scadrial", "human", "steel", "iron").save(original);
        var legacy = new net.minecraft.nbt.CompoundTag(); legacy.putBoolean("a:iron", true);
        PowerData.data(original).put("grants", legacy);
        helper.assertTrue(PowerData.available(original).containsAll(java.util.List.of("a:steel", "a:iron", "f:iron")), "Legacy powers must migrate");
        var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "allomancy_steel")).orElseThrow();
        var instance = original.getAttribute(attribute);
        instance.setBaseValue(2);
        instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "test_strength"), 0.5,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        helper.assertTrue(PowerAttributes.strength(original, "a:steel") == 2.5, "Effective strength must include modifiers");
        PowerData.clonePlayer(original, clone);
        helper.assertTrue(PowerAttributes.strength(clone, "a:steel") == 2.5, "Base and permanent modifiers must survive respawn/save-load");
        instance.removeModifiers(); instance.setBaseValue(0);
        helper.assertTrue(!PowerData.available(original).contains("a:steel"), "Origin metadata must not restore a zero attribute");
        PowerData.clonePlayer(original, clone);
        helper.assertTrue(!PowerData.available(clone).contains("a:steel"), "Revocation must survive respawn");
        helper.assertTrue(PowerAttributes.keys().size() == 33, "All Metallic Art attributes must be registered");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void commandsParseNamespacedPowerIds(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        for (String command : new String[]{"cosmere power grant TestPlayer a:steel", "cosmere power revoke TestPlayer f:iron", "attribute @s cosmerearcanists:allomancy_steel base set 2"}) {
            var parsed = dispatcher.parse(command, server.createCommandSourceStack());
            helper.assertTrue(parsed.getExceptions().isEmpty() && !parsed.getReader().canRead(), "Command must consume the full namespaced power ID: " + command);
        }
        helper.succeed();
    }
}