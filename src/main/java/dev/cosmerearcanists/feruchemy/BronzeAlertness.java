package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.mixin.BronzeEffectAccess;
import dev.cosmerearcanists.power.BronzeWarningPacket;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Effects require this tick's actual funded output, including the final partial tick. */
public final class BronzeAlertness {
    private record Clock(MobEffectInstance effect, double remainder) {}
    private static final Map<ServerPlayer, Map<String, Clock>> CLOCKS = new WeakHashMap<>();
    private static final Set<ServerPlayer> WARNED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void clear(ServerPlayer p) {
        CLOCKS.remove(p); clearWarnings(p);
    }
    private static void clearWarnings(ServerPlayer p) {
        if (WARNED.remove(p)) PacketDistributor.sendToPlayer(p, new BronzeWarningPacket(p.level().dimension().location().toString(), 1, List.of()));
    }
    private static void shorten(ServerPlayer p, String key, MobEffectInstance effect, double extra) {
        var clocks = CLOCKS.computeIfAbsent(p, unused -> new HashMap<>());
        if (effect == null || effect.isInfiniteDuration()) { clocks.remove(key); return; }
        var previous = clocks.get(key);
        double clock = extra + (previous != null && previous.effect == effect ? previous.remainder : 0);
        int steps = (int)Math.floor(clock + 1e-9);
        clocks.put(key, new Clock(effect, Math.max(0, clock - steps)));
        if (steps <= 0) return;
        boolean changed = false;
        var seen = Collections.newSetFromMap(new IdentityHashMap<MobEffectInstance, Boolean>());
        // Hidden weaker effects age with the visible effect, and retain their native promotion behavior.
        for (var current = effect; current != null && seen.size() < 32 && seen.add(current); current = ((BronzeEffectAccess)current).cosmerearcanists$hiddenEffect()) {
            if (current.isInfiniteDuration()) continue;
            int duration = Math.max(1, current.getDuration() - steps);
            if (duration != current.getDuration()) { ((BronzeEffectAccess)current).cosmerearcanists$duration(duration); changed = true; }
        }
        // Leave the last tick for vanilla expiration/removal hooks. Refresh the client periodically, or at expiry.
        if (changed && (p.tickCount % 20 == 0 || effect.getDuration() <= 1)) p.connection.send(new ClientboundUpdateMobEffectPacket(p.getId(), effect, false));
    }
    public static boolean perceptible(ServerPlayer p, Entity entity) {
        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return false;
        Vec3 from = p.getEyePosition(), to = entity.getBoundingBox().getCenter();
        if (!p.level().hasChunksAt(BlockPos.containing(from), BlockPos.containing(to))) return false;
        return p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }
    private static boolean attack(ServerPlayer p, Entity entity) {
        if (!(entity instanceof LivingEntity living) || living.isAlliedTo(p)) return false;
        double distance = living.distanceToSqr(p);
        if (living instanceof Mob mob) {
            if (mob.getTarget() != p) return false;
            if (mob instanceof Creeper creeper) return distance <= 36 && (creeper.getSwellDir() > 0 || creeper.isIgnited());
            if (living.isUsingItem()) return true;
            if (!mob.isAggressive() && !living.swinging) return false;
        } else if (!(living instanceof Player player) || player.isSpectator() || !p.server.isPvpAllowed() || !p.canHarmPlayer(player) || !living.swinging) return false;
        return distance <= 16 && living.getLookAngle().dot(p.getBoundingBox().getCenter().subtract(living.getEyePosition()).normalize()) > .35;
    }
    public static List<BronzeWarningPacket.Warning> warnings(ServerPlayer p, double level) {
        double threshold = FeruchemyConfig.BRONZE_WARNING_LEVEL.get();
        if (!FeruchemyConfig.BRONZE_WARNINGS.get() || level + 1e-9 < threshold) return List.of();
        double ratio = Math.max(1, level / threshold);
        double range = Math.min(FeruchemyConfig.BRONZE_WARNING_CAP.get(), FeruchemyConfig.BRONZE_WARNING_RANGE.get() * Math.sqrt(ratio));
        double horizon = Math.min(12, 6 * Math.sqrt(ratio));
        var warnings = new ArrayList<BronzeWarningPacket.Warning>();
        var entities = p.serverLevel().getEntities(p, p.getBoundingBox().inflate(range), e -> e instanceof Projectile || e instanceof LivingEntity);
        entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int scanned = 0;
        for (var entity : entities) {
            if (++scanned > 128 || warnings.size() >= 8) break;
            if (entity.distanceToSqr(p) > range * range || !perceptible(p, entity)) continue;
            boolean danger;
            if (entity instanceof Projectile projectile) {
                if (projectile.getOwner() == p || projectile.getOwner() != null && projectile.getOwner().isAlliedTo(p)) continue;
                Vec3 velocity = projectile.getDeltaMovement().subtract(p.getDeltaMovement());
                Vec3 start = projectile.position(), relative = start.subtract(p.getBoundingBox().getCenter());
                danger = velocity.lengthSqr() > .0025 && relative.dot(velocity) < 0
                    && p.getBoundingBox().inflate(.35 + projectile.getBbWidth() * .5).clip(start, start.add(velocity.scale(horizon))).isPresent();
            } else danger = attack(p, entity);
            if (!danger) continue;
            Vec3 direction = entity.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
            if (direction.lengthSqr() < .9) continue;
            float intensity = (float)Math.min(1, (.4 + .15 * Math.log(ratio)) * (.7 + .3 * (1 - Math.min(1, entity.distanceTo(p) / range))));
            warnings.add(new BronzeWarningPacket.Warning(direction, intensity));
        }
        return List.copyOf(warnings);
    }
    public static void tick(ServerPlayer p, double fundedLevel) {
        if (fundedLevel <= 0) { clear(p); return; }
        if (FeruchemyConfig.BRONZE_DEBUFFS.get()) {
            double extra = Math.min(FeruchemyConfig.BRONZE_DEBUFF_CAP.get(), fundedLevel * FeruchemyConfig.BRONZE_DEBUFF_RATE.get());
            shorten(p, "fatigue", p.getEffect(MobEffects.DIG_SLOWDOWN), extra);
            shorten(p, "nausea", p.getEffect(MobEffects.CONFUSION), extra);
        } else CLOCKS.remove(p);
        if (!FeruchemyConfig.BRONZE_WARNINGS.get() || fundedLevel + 1e-9 < FeruchemyConfig.BRONZE_WARNING_LEVEL.get()) { clearWarnings(p); return; }
        if (p.tickCount % 2 != 0 && WARNED.contains(p)) return;
        WARNED.add(p);
        PacketDistributor.sendToPlayer(p, new BronzeWarningPacket(p.level().dimension().location().toString(), 6, warnings(p, fundedLevel)));
    }
    private BronzeAlertness() {}
}
