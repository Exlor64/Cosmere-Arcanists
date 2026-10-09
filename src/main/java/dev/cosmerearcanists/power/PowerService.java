package dev.cosmerearcanists.power;

import java.util.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import dev.cosmerearcanists.Metal;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class PowerService {
    public static final AbilityDispatcher<ServerPlayer, MetalTarget> ABILITIES = new AbilityDispatcher<>();
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<Integer, List<BlockPos>> SCAN_OFFSETS = new HashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    static final class Session {
        BlockPos scanCenter, urgentCenter; boolean urgentApplying; int cursor, scanRadius; final Set<BlockPos> blocks = new HashSet<>();
        List<MetalTarget> targets = List.of(); boolean applying, pulse, alternate; int movementInputs; long lastApply, packetTick = -1; int packets;
        String dimension = "", activePower = "";
        Vec3 previousPosition, observedMovement;
        long sampledTick = -1, impulseTick = -1;
    }
    public PowerService() {
        AbilityDispatcher.Handler<ServerPlayer, MetalTarget> ironSteel = (player, power, phase, targets) -> {
            if (phase == AbilityDispatcher.Phase.STOP || !PowerConfig.IRON_STEEL.get()) return;
            if (PowerConfig.CONSUMPTION.get() && PowerData.reserve(player, power.substring(2)) <= 0) return;
            if (power.equals("a:steel") && session(player).alternate) {
                if (PowerConfig.STEEL_MOVEMENT.get()) assistMovement(player, power, targets);
            } else apply(player, power, targets);
        };
        ABILITIES.register("a:iron", ironSteel);
        ABILITIES.register("a:steel", ironSteel);
    }
    private static Session session(ServerPlayer p) { return SESSIONS.computeIfAbsent(p.getUUID(), id -> new Session()); }
    public static void control(ServerPlayer p, PowerNetwork.Control packet) {
        var s = session(p); long now = p.level().getGameTime();
        if (s.packetTick != now) { s.packetTick = now; s.packets = 0; }
        if (++s.packets > 12) return;
        if (packet.action() == 2) {
            boolean held = (packet.value() & 1) != 0 && p.isAlive() && !p.isSpectator();
            if (!s.applying && held) s.alternate = (packet.value() & 2) != 0;
            if (!held) s.alternate = false;
            s.movementInputs = packet.value() & 252; s.applying = held; s.lastApply = now; return;
        }
        if (packet.action() == 5) { s.pulse = p.isAlive() && !p.isSpectator(); s.alternate = (packet.value() & 2) != 0; s.movementInputs = packet.value() & 252; s.lastApply = now; return; }
        if (packet.action() == 4) { PowerNetwork.sync(p, s.targets); return; }
        if (!p.isAlive() || p.isSpectator() || !PowerData.available(p).contains(packet.power())) return;
        switch (packet.action()) {
            case 0 -> PowerData.data(p).putString("selected", packet.power());
            case 1 -> PowerData.state(p, packet.power(), packet.value());
            case 3 -> PowerData.state(p, packet.power(), PowerRules.next(packet.power(), PowerData.state(p, packet.power()), PowerConfig.FERUCHEMY_LEVELS.get()));
            default -> { return; }
        }
        PowerNetwork.sync(p, s.targets);
    }
    @SubscribeEvent public void syncDatapack(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) { event.getPlayerList().getPlayers().forEach(PowerService::stopActivation); MetalDetector.rebuild(event.getPlayerList().getServer()); SESSIONS.clear(); }
        event.getRelevantPlayers().forEach(p -> PowerNetwork.sync(p, List.of()));
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { stopActivation(p); SESSIONS.remove(p.getUUID()); PowerNetwork.sync(p, List.of()); }
    }
    private static void stopActivation(ServerPlayer p) {
        var s = SESSIONS.get(p.getUUID());
        if (s != null && !s.activePower.isEmpty()) {
            ABILITIES.activate(p, s.activePower, AbilityDispatcher.Phase.STOP, s.targets);
            s.activePower = "";
        }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) stopActivation(p);
        SESSIONS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer old && event.getEntity() instanceof ServerPlayer p) {
            stopActivation(old); PowerData.clonePlayer(old, p); SESSIONS.remove(p.getUUID());
        }
    }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer p) PowerNetwork.sync(p, List.of()); }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { stopActivation(p); SESSIONS.remove(p.getUUID()); PowerNetwork.sync(p, List.of()); }
    }
    @SubscribeEvent public void stopped(ServerStoppedEvent event) { event.getServer().getPlayerList().getPlayers().forEach(PowerService::stopActivation); SESSIONS.clear(); MetalDetector.clear(); }
    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var s = session(p); var available = PowerData.available(p);
        Vec3 displacement = s.previousPosition == null ? null : p.position().subtract(s.previousPosition);
        s.observedMovement = usableMovement(displacement) ? displacement : null;
        s.previousPosition = p.position(); s.sampledTick = p.level().getGameTime();
        boolean active = false;
        for (String power : List.of("a:iron", "a:steel")) {
            if (!PowerConfig.IRON_STEEL.get() || !p.isAlive() || p.isSpectator() || !available.contains(power)) continue;
            int state = PowerData.state(p, power); if (state == 0) continue;
            if (state == 2 && !PowerConfig.FLARING.get()) { state = 1; PowerData.state(p, power, state); }
            String metal = power.substring(2); double fuel = PowerData.reserve(p, metal);
            if (PowerConfig.CONSUMPTION.get() && fuel <= 0) { PowerData.state(p, power, 0); continue; }
            active = true;
            if (PowerConfig.CONSUMPTION.get()) {
                double drain = PowerConfig.BURN_DRAIN.get() / 20.0 * (state == 2 ? PowerConfig.FLARE_DRAIN.get() : 1);
                PowerData.reserve(p, metal, fuel - drain);
            }
        }
        var previousTargets = s.targets;
        if (active) { scan(p, s); s.targets = targets(p, s); }
        else { s.targets = List.of(); s.scanCenter = s.urgentCenter = null; s.urgentApplying = false; s.blocks.clear(); }
        String selected = PowerData.selected(p);
        String activated = (s.applying || s.pulse) && p.isAlive() && !p.isSpectator() && p.level().getGameTime() - s.lastApply <= 15
                && available.contains(selected) && PowerData.state(p, selected) != 0 ? selected : "";
        if (!activated.equals(s.activePower)) {
            if (!s.activePower.isEmpty()) ABILITIES.activate(p, s.activePower, AbilityDispatcher.Phase.STOP, s.targets);
            s.activePower = activated;
            if (!activated.isEmpty()) ABILITIES.activate(p, activated, AbilityDispatcher.Phase.START, s.targets);
        } else if (!activated.isEmpty()) ABILITIES.activate(p, activated, AbilityDispatcher.Phase.TICK, s.targets);
        s.pulse = false;
        if (!s.targets.equals(previousTargets) || p.tickCount % PowerConfig.SYNC_INTERVAL.get() == 0) PowerNetwork.sync(p, s.targets);
    }
    static void scan(ServerPlayer p, Session s) {
        int radius = PowerConfig.RANGE.get(); var center = p.blockPosition();
        String dimension = p.level().dimension().location().toString();
        if (!dimension.equals(s.dimension)) { s.dimension = dimension; s.blocks.clear(); s.scanCenter = null; }
        if (!PowerConfig.DETECTION_BLOCKS.get()) { s.blocks.clear(); return; }
        // Keep a pass alive while walking; restarting it on every few blocks starves discovery.
        if (s.scanCenter == null || s.scanRadius != radius || s.scanCenter.distSqr(center) > 4 * radius * radius) {
            s.scanCenter = center; s.scanRadius = radius; s.cursor = 0;
            s.blocks.clear();
        }
        // Always refresh the immediate neighborhood at the player's CURRENT position.
        // The background pass can otherwise still be centered high above a falling player.
        scanPositions(p, s, center, scanOffsets(Math.min(radius, PowerConfig.NEAR_SCAN_RADIUS.get())), Integer.MAX_VALUE);
        Vec3 movement = s.observedMovement;
        boolean fast = movement != null && (movement.y < -.15 || movement.horizontalDistanceSqr() > .25);
        boolean urgent = (fast && !center.equals(s.urgentCenter)) || ((s.applying || s.pulse) && !s.urgentApplying);
        if (urgent && PowerConfig.URGENT_SCAN_BUDGET.get() > 0) {
            scanPositions(p, s, center, scanOffsets(radius), PowerConfig.URGENT_SCAN_BUDGET.get());
            s.urgentCenter = center;
        }
        s.urgentApplying = s.applying || s.pulse;
        var offsets = scanOffsets(radius);
        s.blocks.removeIf(pos -> pos.distSqr(center) > (radius + 2) * (radius + 2));
        int budget = PowerConfig.SCAN_BUDGET.get();
        for (int i = 0; i < budget && s.cursor < offsets.size(); i++, s.cursor++) {
            var pos = s.scanCenter.offset(offsets.get(s.cursor));
            if (!p.serverLevel().hasChunkAt(pos)) continue;
            // Publish discoveries immediately rather than waiting for the complete volume.
            if (MetalDetector.detects(p.server, new ItemStack(p.level().getBlockState(pos).getBlock().asItem()))) s.blocks.add(pos);
            else s.blocks.remove(pos);
        }
        int capacity = PowerConfig.MAX_TARGETS.get() * 4;
        if (s.blocks.size() > capacity) {
            var nearest = s.blocks.stream().sorted(Comparator.comparingDouble(pos -> pos.distSqr(center))).limit(capacity).toList();
            s.blocks.clear(); s.blocks.addAll(nearest);
        }
        if (s.cursor >= offsets.size()) { s.scanCenter = center; s.cursor = 0; }
    }
    private static void scanPositions(ServerPlayer p, Session s, BlockPos center, List<BlockPos> offsets, int budget) {
        for (int i = 0; i < Math.min(budget, offsets.size()); i++) {
            var pos = center.offset(offsets.get(i));
            if (!p.serverLevel().hasChunkAt(pos)) continue;
            if (MetalDetector.detects(p.server, new ItemStack(p.level().getBlockState(pos).getBlock().asItem()))) s.blocks.add(pos);
            else s.blocks.remove(pos);
        }
    }
    static List<BlockPos> scanOffsets(int radius) {
        return SCAN_OFFSETS.computeIfAbsent(radius, r -> {
            var offsets = new ArrayList<BlockPos>();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++)
                if (x * x + y * y + z * z <= r * r) offsets.add(new BlockPos(x, y, z));
            offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
            return List.copyOf(offsets);
        });
    }

    private List<MetalTarget> targets(ServerPlayer p, Session s) {
        int range = PowerConfig.RANGE.get(); double rangeSq = range * range; var chest = chest(p);
        var result = new ArrayList<MetalTarget>();
        // Gear/items are prioritized over dense collections of metal blocks.
        for (var e : p.level().getEntities(p, p.getBoundingBox().inflate(range))) {
            if (e instanceof ItemEntity item && PowerConfig.DETECTION_ITEMS.get() && MetalDetector.detects(p.server, item.getItem())) result.add(MetalTarget.entity(e, 6));
            else if (e instanceof LivingEntity living && PowerConfig.DETECTION_GEAR.get()) {
                if (e instanceof ServerPlayer && !PowerConfig.PLAYER_TARGETS.get()) continue;
                for (int i = 0; i < SLOTS.length; i++) if (MetalDetector.detects(p.server, living.getItemBySlot(SLOTS[i]))) result.add(MetalTarget.entity(e, i));
            }
        }
        for (var pos : s.blocks) if (p.serverLevel().hasChunkAt(pos) && MetalDetector.detects(p.server, new ItemStack(p.level().getBlockState(pos).getBlock().asItem()))) result.add(MetalTarget.block(pos));
        // A directly aimed block is available immediately, before a background scan finishes.
        var hit = p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(range)), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (PowerConfig.DETECTION_BLOCKS.get() && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
            && MetalDetector.detects(p.server, new ItemStack(p.level().getBlockState(hit.getBlockPos()).getBlock().asItem()))) {
            var target = MetalTarget.block(hit.getBlockPos()); if (!result.contains(target)) result.add(target);
        }
        return result.stream().filter(t -> { var pos = t.position(p.level(), 1); return pos != null && pos.distanceToSqr(chest) <= rangeSq && chest.distanceTo(pos) + MetalResistance.cost(p, chest, pos) <= range; })
            .sorted(Comparator.comparingDouble(t -> t.position(p.level(), 1).distanceToSqr(chest))).limit(PowerConfig.MAX_TARGETS.get()).toList();
    }
    public static Vec3 chest(Entity p) { return p.position().add(0, p.getBbHeight() * .65, 0); }
    private void apply(ServerPlayer p, String power, List<MetalTarget> targets) {
        MetalTarget best = null; double score = -1, threshold = Math.cos(Math.toRadians(PowerConfig.AIM_ANGLE.get()));
        var hit = p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(PowerConfig.RANGE.get())), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        BlockPos aimedBlock = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.getBlockPos() : null;
        for (var target : targets) {
            Vec3 point = target.position(p.level(), 1); if (point == null) continue;
            Vec3 offset = point.subtract(p.getEyePosition()); double distance = offset.length(); if (distance < .1) continue;
            double dot = offset.normalize().dot(p.getLookAngle());
            if (dot < threshold && !(target.entityId() < 0 && target.block().equals(aimedBlock))) continue;
            double candidate = dot - distance * .0001;
            // Prefer the block actually under the crosshair over a neighboring center in the cone.
            if (target.entityId() < 0 && target.block().equals(aimedBlock)) candidate += 2;
            if (candidate > score) { score = candidate; best = target; }
        }
        if (best == null) return;
        var point = best.position(p.level(), 1); var offset = point.subtract(chest(p)); double distance = offset.length();
        if (distance < .3) return;
        Vec3 direction = offset.normalize().scale(power.equals("a:iron") ? 1 : -1);
        accelerate(p, targetImpulse(p, best, direction, powerForce(p, power)));
    }
    private static double powerForce(ServerPlayer p, String power) {
        return PowerConfig.FORCE.get() * PowerAttributes.strength(p, power) * (PowerData.state(p, power) == 2 ? PowerConfig.FLARE_STRENGTH.get() : 1);
    }
    private Vec3 targetImpulse(ServerPlayer p, MetalTarget best, Vec3 direction, double force) {
        var point = best.position(p.level(), 1); if (point == null) return Vec3.ZERO;
        double distance = point.distanceTo(chest(p));
        double accel = AllomanticPhysics.acceleration(force, distance, PowerConfig.RANGE.get(), PowerConfig.PLAYER_MASS.get());
        // Fixed blocks anchor the force: iron draws the player in; steel pushes them away.
        if (best.entityId() < 0) return direction.scale(accel);
        Entity entity = p.level().getEntity(best.entityId()); if (entity == null) return Vec3.ZERO;
        if (entity instanceof ItemEntity item && !MetalDetector.detects(p.server, item.getItem())) return Vec3.ZERO;
        if (entity instanceof LivingEntity living && (best.slot() < 0 || best.slot() >= SLOTS.length || !MetalDetector.detects(p.server, living.getItemBySlot(SLOTS[best.slot()])))) return Vec3.ZERO;
        double mass = entity instanceof ItemEntity item ? Math.max(.2, item.getItem().getCount() * .2)
            : entity instanceof ServerPlayer ? PowerConfig.PLAYER_MASS.get() : Math.max(10, entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() * 100);
        accelerate(entity, direction.scale(-AllomanticPhysics.acceleration(force, distance, PowerConfig.RANGE.get(), mass)));
        return PowerConfig.RECOIL.get() ? direction.scale(accel) : Vec3.ZERO;
    }
    private void assistMovement(ServerPlayer p, String power, List<MetalTarget> targets) {
        int input = session(p).movementInputs;
        double forward = ((input & 4) != 0 ? 1 : 0) - ((input & 8) != 0 ? 1 : 0);
        double left = ((input & 16) != 0 ? 1 : 0) - ((input & 32) != 0 ? 1 : 0);
        double vertical = ((input & 64) != 0 ? 1 : 0) - ((input & 128) != 0 ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        Vec3 desired = new Vec3(-Math.sin(yaw)*forward + Math.cos(yaw)*left, vertical, Math.cos(yaw)*forward + Math.sin(yaw)*left);
        if (desired.lengthSqr() < 1e-8) return;
        desired = desired.normalize();
        var anchors = new ArrayList<MetalTarget>(); var directions = new ArrayList<Vec3>(); var strengths = new ArrayList<Double>();
        var usedEntities = new HashSet<Integer>();
        for (var target : targets) {
            // Multiple pieces of equipment on one entity are one physical anchor.
            if (target.entityId() >= 0 && (!PowerConfig.RECOIL.get() || !usedEntities.add(target.entityId()))) continue;
            var point = target.position(p.level(), 1); if (point == null || point.distanceTo(chest(p)) < .3) continue;
            anchors.add(target); directions.add(chest(p).subtract(point).normalize());
            strengths.add(AllomanticPhysics.acceleration(1, point.distanceTo(chest(p)), PowerConfig.RANGE.get(), PowerConfig.PLAYER_MASS.get()));
        }
        double[][] vectors = directions.stream().map(v -> new double[]{v.x,v.y,v.z}).toArray(double[][]::new);
        double[] weights = SteelMovement.weights(new double[]{desired.x,desired.y,desired.z}, vectors,
            strengths.stream().mapToDouble(Double::doubleValue).toArray(), Math.cos(Math.toRadians(PowerConfig.MOVEMENT_ANGLE.get())));
        Vec3 impulse = Vec3.ZERO;
        for (int i=0;i<weights.length;i++) if (weights[i]>1e-6)
            impulse = impulse.add(targetImpulse(p, anchors.get(i), directions.get(i), powerForce(p,power)*weights[i]));
        if (impulse.lengthSqr()>1e-12) accelerate(p,impulse);
    }
    private static boolean usableMovement(Vec3 movement) {
        return movement != null && movement.lengthSqr() > 1e-8 && movement.lengthSqr() < 64;
    }
    private void accelerate(Entity e, Vec3 impulse) {
        if (impulse.lengthSqr() < 1e-12) return;
        Vec3 previous = e.getDeltaMovement();
        if (e instanceof ServerPlayer player) {
            var s = session(player); long now = player.level().getGameTime();
            if (s.impulseTick != now) {
                // Player movement packets report positions, not velocity. Use observed motion on ALL axes.
                // Server-side horizontal delta is reset during player movement processing.
                Vec3 observed = s.sampledTick == now ? s.observedMovement
                        : s.previousPosition == null ? null : player.position().subtract(s.previousPosition);
                if (usableMovement(observed)) previous = observed;

                s.impulseTick = now;
            }
        }
        Vec3 velocity = previous.add(impulse);
        // Existing fast falls are not instantly slowed to the ability's speed cap.
        double speed = velocity.length(), cap = Math.max(PowerConfig.MAX_SPEED.get(), previous.length());
        if (speed > cap) velocity = velocity.scale(cap / speed);
        if (PowerConfig.FALL_MITIGATION.get()) e.fallDistance = AllomanticPhysics.brakedFallDistance(
                e.fallDistance, previous.y, velocity.y, impulse.y, PowerConfig.FALL_MITIGATION_STRENGTH.get());
        e.setDeltaMovement(velocity);
        if (e instanceof ServerPlayer player) {
            player.connection.aboveGroundTickCount = 0;
            // Replacing client velocity with measured displacement re-injects walking every tick.
            // Send only the new force; the client retains its real horizontal and vertical momentum.
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PowerNetwork.Impulse(
                player.level().dimension().location().toString(), impulse.x, impulse.y, impulse.z,
                PowerConfig.MAX_SPEED.get(), PowerConfig.RESIST_WALKING.get()));
        } else e.hurtMarked = true;
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cosmere").then(Commands.literal("power").requires(s -> s.hasPermission(2))
            .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("power", ResourceLocationArgument.id())
                .suggests((ctx, b) -> { for (var m : Metal.values()) { b.suggest("a:" + m.id()); b.suggest("f:" + m.id()); } return b.buildFuture(); })
                .executes(ctx -> {
                    var p = EntityArgument.getPlayer(ctx, "player"); var key = ResourceLocationArgument.getId(ctx, "power").toString();
                    if (!validPower(key)) return 0; PowerData.grant(p, key, true); PowerNetwork.sync(p, List.of());
                    ctx.getSource().sendSuccess(() -> Component.literal("Granted " + key + " to " + p.getScoreboardName()), true); return 1;
                }))))
            .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("power", ResourceLocationArgument.id()).executes(ctx -> {
                var p = EntityArgument.getPlayer(ctx, "player"); var key = ResourceLocationArgument.getId(ctx, "power").toString();
                if (!validPower(key)) return 0; PowerData.grant(p, key, false); PowerData.state(p, key, 0); PowerNetwork.sync(p, List.of()); return 1;
            }))))
            .then(Commands.literal("refill").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                var p = EntityArgument.getPlayer(ctx, "player");
                PowerData.reserve(p, "iron", PowerConfig.RESERVE_CAP.get()); PowerData.reserve(p, "steel", PowerConfig.RESERVE_CAP.get()); PowerNetwork.sync(p, List.of()); return 1;
            })))));
    }
    private boolean validPower(String key) {
        if (!(key.startsWith("a:") || key.startsWith("f:"))) return false;
        return Arrays.stream(Metal.values()).anyMatch(m -> m.id().equals(key.substring(2)));
    }
}
