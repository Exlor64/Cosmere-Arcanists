package dev.cosmerearcanists.power;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.cosmerearcanists.currency.*;
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
public final class SteelProjectile extends ThrowableItemProjectile implements AllomanticProjectile {
    private static final EntityDataAccessor<Boolean> EMBEDDED=SynchedEntityData.defineId(SteelProjectile.class,EntityDataSerializers.BOOLEAN);
    private BlockPos anchor=BlockPos.ZERO;
    private ItemStack cargo=ItemStack.EMPTY;
    private double metalUnits=1, referenceSpeed=3.5, nuggetDamage=2, damageCap=100;
    private int flightAge;
    public SteelProjectile(EntityType<? extends SteelProjectile> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(EMBEDDED,false);}
    public boolean embedded(){return entityData.get(EMBEDDED);}
    public boolean coin(){return Coins.coin(cargo);}
    @Override public boolean fixedMetalAnchor(){return embedded();}
    /** Transfer the entire remaining physical cargo once; do not destroy it if entity insertion fails. */
    public ItemEntity pullAsItem(ServerPlayer player,double force){
        if(level().isClientSide||isRemoved()||!coin()||!CurrencyConfig.RECALL.get()||force<=0)return null;
        var item=new ItemEntity(level(),getX(),getY(),getZ(),cargo.copy());
        double speed=Math.min(CurrencyConfig.RETURN_SPEED.get(),AllomanticPhysics.acceleration(force,position().distanceTo(PowerService.chest(player)),PowerConfig.RANGE.get(),allomanticMass()));
        CoinRecall.start(item,player,Math.max(.05,speed));if(!level().addFreshEntity(item))return null;cargo=ItemStack.EMPTY;discard();return item;
    }
    @Override public void playerTouch(Player player){
        if(level().isClientSide||isRemoved()||!embedded()||!CurrencyConfig.PICKUP.get()||flightAge<CurrencyConfig.PICKUP_DELAY.get()||player.isSpectator()||cargo.isEmpty())return;
        var remainder=cargo.copy();int before=remainder.getCount();player.getInventory().add(remainder);int received=before-remainder.getCount();
        if(received>0){metalUnits*=remainder.getCount()/(double)before;cargo=remainder;player.take(this,received);player.getInventory().setChanged();if(cargo.isEmpty())discard();}
    }
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity){return !embedded()&&super.canHitEntity(entity);}
    @Override protected Item getDefaultItem(){return Items.IRON_NUGGET;}
    public void cargo(ItemStack stack,double units) {
        cargo=stack.copy(); setItem(stack.copyWithCount(1)); metalUnits=units;
        referenceSpeed=PowerConfig.NUGGET_SPEED.get(); nuggetDamage=PowerConfig.NUGGET_DAMAGE.get(); damageCap=PowerConfig.PROJECTILE_DAMAGE_CAP.get();
    }
    public boolean allomanticallyDetectable(net.minecraft.server.MinecraftServer server) {
        return !isRemoved() && PowerConfig.DETECTION_PROJECTILES.get() && MetalDetector.detects(server,cargo);
    }
    public double allomanticMass() {
        return Math.max(.001,metalUnits*PowerConfig.NUGGET_MASS.get());
    }
    @Override public void tick() {
        if(embedded()){
            if(!level().isClientSide&&level().hasChunkAt(anchor)&&level().getBlockState(anchor).getCollisionShape(level(),anchor).isEmpty()){entityData.set(EMBEDDED,false);setNoGravity(false);}else setDeltaMovement(Vec3.ZERO);
        }
        super.tick();
        int lifetime=coin()?CurrencyConfig.LIFETIME.get():PowerConfig.PROJECTILE_LIFETIME.get();
        if(!level().isClientSide&&!isRemoved()&&++flightAge>=lifetime)recover();
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if(level().isClientSide)return;
        var target=hit.getEntity();
        if(target instanceof Player victim && (!PowerConfig.PLAYER_TARGETS.get() || getOwner() instanceof Player owner && !owner.canHarmPlayer(victim)))return;
        double speed=BubbleService.intrinsicVelocity(this).length();
        float damage=(float)MetalProjectilePhysics.damage(metalUnits,speed,referenceSpeed,nuggetDamage,damageCap);
        boolean accepted=target.hurt(damageSources().thrown(this,getOwner()),damage);
        if(accepted && target instanceof LivingEntity living) {
            double knockback=Math.min(2,Math.sqrt(metalUnits)*speed*.05);
            var motion=getDeltaMovement(); living.knockback(knockback,-motion.x,-motion.z);
        }
    }
    @Override protected void onHit(HitResult hit) {
        if(isRemoved()||embedded())return;
        if(!level().isClientSide&&hit instanceof BlockHitResult block&&coin()&&CurrencyConfig.EMBED.get()){
            anchor=block.getBlockPos().immutable();setPos(block.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.04)));setDeltaMovement(Vec3.ZERO);setNoGravity(true);entityData.set(EMBEDDED,true);return;
        }
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
        tag.putBoolean("CoinEmbedded",embedded());tag.putLong("CoinAnchor",anchor.asLong());
        tag.putDouble("MetalUnits",metalUnits); tag.putDouble("ReferenceSpeed",referenceSpeed);
        tag.putDouble("NuggetDamage",nuggetDamage); tag.putDouble("DamageCap",damageCap); tag.putInt("FlightAge",flightAge);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); cargo=ItemStack.parseOptional(registryAccess(),tag.getCompound("Cargo"));
        metalUnits=Math.clamp(tag.getDouble("MetalUnits"),.001,1e12); referenceSpeed=Math.max(.001,tag.getDouble("ReferenceSpeed"));
        nuggetDamage=Math.max(0,tag.getDouble("NuggetDamage")); damageCap=Math.clamp(tag.getDouble("DamageCap"),.001,100);
        flightAge=Math.max(0,tag.getInt("FlightAge"));entityData.set(EMBEDDED,coin()&&tag.getBoolean("CoinEmbedded"));anchor=BlockPos.of(tag.getLong("CoinAnchor"));setNoGravity(embedded());
    }
}
