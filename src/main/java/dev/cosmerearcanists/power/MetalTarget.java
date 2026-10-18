package dev.cosmerearcanists.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** slot: 0 main hand, 1 offhand, 2 head, 3 chest, 4 legs, 5 feet, 6 dropped item, 7 fired metal projectile. */
public record MetalTarget(int entityId, int slot, BlockPos block) {
    public static MetalTarget block(BlockPos pos) { return new MetalTarget(-1, -1, pos.immutable()); }
    public static MetalTarget entity(Entity e, int slot) { return new MetalTarget(e.getId(), slot, BlockPos.ZERO); }
    public Vec3 position(Level level, float partial) {
        if (entityId < 0) return Vec3.atCenterOf(block);
        var entity = level.getEntity(entityId); if (entity == null) return null;
        Vec3 base = entity.getPosition(partial); double h = entity.getBbHeight();
        if(entity instanceof AllomanticProjectile)return base.add(0,h*.5,0);
        double height = switch (slot) { case 0, 1 -> h * 0.60; case 2 -> h * .90; case 3 -> h * .65; case 4 -> h * .35; case 5 -> h * .10; default -> .10; };
        double side = slot == 0 ? -.35 : slot == 1 ? .35 : 0;
        double angle = entity instanceof LivingEntity living ? Math.toRadians(living.yBodyRot) : 0;
        return base.add(Math.cos(angle) * side, height, Math.sin(angle) * side);
    }
}
