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
        helper.assertTrue(recoil!=null && PowerData.reserve(player,"steel")==9,"Normal steel activation must still launch and spend shot fuel");
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
        helper.assertTrue(PowerData.reserve(player,"steel")==8,"Each successful launch must deduct its extra reserve cost");
        var shots=level.getEntitiesOfClass(SteelProjectile.class,player.getBoundingBox().inflate(3),e->e.getOwner()==player);
        helper.assertTrue(shots.size()==2,"Each thrown stack must launch exactly one shot");
        var speeds=shots.stream().mapToDouble(e->e.getDeltaMovement().length()).sorted().toArray();
        helper.assertTrue(speeds[0]<speeds[1]*.3,"Anvil must fly much slower than nugget");
        PowerService.control(player,new PowerNetwork.Control(2,"",0));
        helper.assertTrue(MetalProjectiles.launch(player,nugget)==null,"Releasing G must immediately restore ordinary drops");
        helper.assertTrue(PowerData.reserve(player,"steel")==8,"Rejected launches must not deduct fuel");
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
            helper.assertTrue(Math.abs((health-cow.getHealth())-2)<.2,"Baseline nugget must deal about two damage before armor; before="+health+", after="+cow.getHealth()+", impact="+projectile.position());
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
    @GameTest(template = "empty")
    public static void ironStrengthAndPullAssistance(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "IronAssist"), ClientInformation.createDefault());
        PowerAttributes.set(player,"a:iron",1); PowerAttributes.set(player,"a:steel",1);
        PowerData.state(player,"a:iron",2); PowerData.state(player,"a:steel",2);
        double steel = PowerService.powerForce(player,"a:steel");
        helper.assertTrue(PowerService.powerForce(player,"a:iron") == steel * 2, "Default iron force must be doubled without increasing steel force");
        PowerAttributes.set(player,"a:iron",2);
        helper.assertTrue(PowerService.powerForce(player,"a:iron") == steel * 4, "Iron attribute strength must continue to scale");
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
        helper.assertTrue(PowerAttributes.keys().size() == 32, "All Metallic Art attributes must be registered");
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