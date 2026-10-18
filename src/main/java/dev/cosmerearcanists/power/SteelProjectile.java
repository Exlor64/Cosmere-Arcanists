package dev.cosmerearcanists.power;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** One physical projectile carries the exact thrown stack, preventing merging and duplicate hits. */
public final class SteelProjectile extends ThrowableItemProjectile {
    private ItemStack cargo=ItemStack.EMPTY;
    private double metalUnits=1, referenceSpeed=3.5, nuggetDamage=2, damageCap=100;
    private int flightAge;
    public SteelProjectile(EntityType<? extends SteelProjectile> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return Items.IRON_NUGGET;}
    public void cargo(ItemStack stack,double units) {
        cargo=stack.copy(); setItem(stack.copyWithCount(1)); metalUnits=units;
        referenceSpeed=PowerConfig.NUGGET_SPEED.get(); nuggetDamage=PowerConfig.NUGGET_DAMAGE.get(); damageCap=PowerConfig.PROJECTILE_DAMAGE_CAP.get();
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && !isRemoved() && ++flightAge>=PowerConfig.PROJECTILE_LIFETIME.get()) recover();
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if(level().isClientSide)return;
        var target=hit.getEntity();
        if(target instanceof Player victim && (!PowerConfig.PLAYER_TARGETS.get() || getOwner() instanceof Player owner && !owner.canHarmPlayer(victim)))return;
        double speed=getDeltaMovement().length();
        float damage=(float)MetalProjectilePhysics.damage(metalUnits,speed,referenceSpeed,nuggetDamage,damageCap);
        if(target.hurt(damageSources().thrown(this,getOwner()),damage) && target instanceof LivingEntity living) {
            double knockback=Math.min(2,Math.sqrt(metalUnits)*speed*.05);
            var motion=getDeltaMovement(); living.knockback(knockback,-motion.x,-motion.z);
        }
    }
    @Override protected void onHit(HitResult hit) {
        if(isRemoved())return;
        super.onHit(hit);
        if(!level().isClientSide && hit.getType()!=HitResult.Type.MISS) {
            setPos(hit.getLocation().subtract(getDeltaMovement().normalize().scale(.05)));
            recover();
        }
    }
    private void recover() {
        if(isRemoved())return;
        if(!cargo.isEmpty()) {
            var drop=new ItemEntity(level(),getX(),getY(),getZ(),cargo.copy());
            drop.setDefaultPickUpDelay(); level().addFreshEntity(drop); cargo=ItemStack.EMPTY;
        }
        discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(!cargo.isEmpty())tag.put("Cargo",cargo.save(registryAccess()));
        tag.putDouble("MetalUnits",metalUnits); tag.putDouble("ReferenceSpeed",referenceSpeed);
        tag.putDouble("NuggetDamage",nuggetDamage); tag.putDouble("DamageCap",damageCap); tag.putInt("FlightAge",flightAge);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); cargo=ItemStack.parseOptional(registryAccess(),tag.getCompound("Cargo"));
        metalUnits=Math.clamp(tag.getDouble("MetalUnits"),.001,1e12); referenceSpeed=Math.max(.001,tag.getDouble("ReferenceSpeed"));
        nuggetDamage=Math.max(0,tag.getDouble("NuggetDamage")); damageCap=Math.clamp(tag.getDouble("DamageCap"),.001,100);
        flightAge=Math.max(0,tag.getInt("FlightAge"));
    }
}
