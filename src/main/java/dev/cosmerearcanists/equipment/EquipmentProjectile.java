package dev.cosmerearcanists.equipment;

import java.util.UUID;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class EquipmentProjectile extends ThrowableItemProjectile implements AllomanticProjectile {
    public static final int SPIKE=0,BALL=1,HOOK=2;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(EquipmentProjectile.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LATCHED=SynchedEntityData.defineId(EquipmentProjectile.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ROPE_LENGTH=SynchedEntityData.defineId(EquipmentProjectile.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETURNING=SynchedEntityData.defineId(EquipmentProjectile.class,EntityDataSerializers.BOOLEAN);
    private ItemStack cargo=ItemStack.EMPTY;
    private double units=9,ropeLength=1;
    private BlockPos anchor=BlockPos.ZERO;
    private int age;
    private boolean spent;
    public boolean recoverable=true;
    public EquipmentProjectile(EntityType<? extends EquipmentProjectile> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(KIND,SPIKE);builder.define(LATCHED,false);builder.define(RETURNING,false);builder.define(ROPE_LENGTH,1f);}
    @Override protected Item getDefaultItem(){return Items.IRON_NUGGET;}
    public int kind(){return entityData.get(KIND);}
    public boolean latched(){return entityData.get(LATCHED);}
    public boolean returning(){return entityData.get(RETURNING);}
    public double ropeLength(){return entityData.get(ROPE_LENGTH);}
    public void configure(int kind,ItemStack stack,double units){entityData.set(KIND,kind);cargo=stack.copy();setItem(stack.copyWithCount(1));this.units=Math.max(.001,units);}
    @Override public boolean allomanticallyDetectable(MinecraftServer server){return !isRemoved() && PowerConfig.DETECTION_PROJECTILES.get() && MetalDetector.detects(server,cargo);}
    @Override public double allomanticMass(){return Math.max(.001,units*PowerConfig.NUGGET_MASS.get());}
    @Override public boolean fixedMetalAnchor(){return latched() && kind()!=BALL && !returning();}
    @Override public void beforeAllomanticForce(){if(kind()==BALL && latched()){entityData.set(LATCHED,false);setNoGravity(false);spent=false;}}
    public void reel(){if(kind()==SPIKE)return;entityData.set(RETURNING,true);entityData.set(LATCHED,false);setNoGravity(true);}
    private ItemStack tetherStack(ServerPlayer owner){
        for(int i=0;i<owner.getInventory().getContainerSize();i++){
            var stack=owner.getInventory().getItem(i);
            if(stack.getItem() instanceof TetherItem && getUUID().equals(TetherItem.active(stack)))return stack;
        }
        return ItemStack.EMPTY;
    }
    private void finish(){
        if(getOwner() instanceof ServerPlayer player){var stack=tetherStack(player);if(!stack.isEmpty())TetherItem.clear(stack);}
        discard();
    }
    @Override public void tick(){
        if(!level().isClientSide){
            age++;
            if(kind()!=SPIKE){
                if(!(getOwner() instanceof ServerPlayer player) || !player.isAlive() || player.isRemoved() || tetherStack(player).isEmpty()){finish();return;}
                double distance=position().distanceTo(PowerService.chest(player));
                if(distance>EquipmentConfig.RANGE.get() || age>12000)reel();
                if(returning()){
                    Vec3 delta=player.getEyePosition().subtract(position());
                    if(delta.length()<1){finish();return;}
                    setDeltaMovement(delta.normalize().scale(Math.min(delta.length(),EquipmentConfig.REEL_SPEED.get())));
                } else if(latched() && kind()==HOOK){
                    if(PowerService.ropeJump(player))ropeLength=Math.max(1,ropeLength-EquipmentConfig.SHORTEN_RATE.get());
                    Vec3 offset=PowerService.chest(player).subtract(position());
                    double length=offset.length();
                    if(length>1e-6){
                        Vec3 radial=offset.scale(1/length);
                        double tension=TetherPhysics.tension(length,ropeLength,PowerService.movementEstimate(player).dot(radial));
                        if(tension>0)PowerService.externalImpulse(player,radial.scale(-tension));
                    }
                }
            } else if(age>EquipmentConfig.SPIKE_LIFETIME.get()){discard();return;}
            entityData.set(ROPE_LENGTH,(float)ropeLength);
            if(latched() && (level().getBlockState(anchor).isAir() || level().getBlockState(anchor).getCollisionShape(level(),anchor).isEmpty())){
                entityData.set(LATCHED,false);setNoGravity(false);
            }
        }
        if(latched())setDeltaMovement(Vec3.ZERO);
        super.tick();
    }
    @Override protected boolean canHitEntity(Entity entity){return !latched() && !returning() && super.canHitEntity(entity);}
    @Override public void onHit(HitResult hit){
        if(level().isClientSide || returning() || latched() || isRemoved())return;
        if(hit instanceof EntityHitResult entityHit){
            if(kind()!=HOOK && !spent){
                var target=entityHit.getEntity();
                boolean allowed=!(target instanceof Player victim) || PowerConfig.PLAYER_TARGETS.get() && (!(getOwner() instanceof Player owner)||owner.canHarmPlayer(victim));
                double speed=getDeltaMovement().length();
                float damage=(float)(kind()==SPIKE?Math.min(100,speed*EquipmentConfig.SPIKE_DAMAGE.get()):MetalProjectilePhysics.damage(units,speed,PowerConfig.NUGGET_SPEED.get(),PowerConfig.NUGGET_DAMAGE.get(),PowerConfig.PROJECTILE_DAMAGE_CAP.get()));
                if(allowed)target.hurt(damageSources().thrown(this,getOwner()),damage);
                spent=true;
            }
            if(kind()==SPIKE){dropSpike(hit.getLocation());return;}
            if(kind()==HOOK){reel();return;}
            setPos(hit.getLocation());setDeltaMovement(Vec3.ZERO);return;
        }
        if(hit instanceof BlockHitResult blockHit){
            anchor=blockHit.getBlockPos().immutable();
            Vec3 point=blockHit.getLocation().add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(.04));
            setPos(point);setDeltaMovement(Vec3.ZERO);setNoGravity(true);entityData.set(LATCHED,true);
            if(getOwner()!=null)ropeLength=Math.max(1,position().distanceTo(PowerService.chest(getOwner())));
            entityData.set(ROPE_LENGTH,(float)ropeLength);
        }
    }
    private void dropSpike(Vec3 at){
        if(recoverable && !cargo.isEmpty()){var drop=new ItemEntity(level(),at.x,at.y,at.z,cargo.copy());drop.setDefaultPickUpDelay();level().addFreshEntity(drop);}
        cargo=ItemStack.EMPTY;discard();
    }
    @Override public void playerTouch(Player player){
        if(!level().isClientSide && kind()==SPIKE && latched() && age>10 && recoverable && !cargo.isEmpty() && player.getInventory().add(cargo.copy())){cargo=ItemStack.EMPTY;discard();}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);if(!cargo.isEmpty())tag.put("Cargo",cargo.save(registryAccess()));
        tag.putInt("Kind",kind());tag.putBoolean("Latched",latched());tag.putBoolean("Returning",returning());
        tag.putLong("Anchor",anchor.asLong());tag.putDouble("Units",units);tag.putDouble("RopeLength",ropeLength);
        tag.putInt("Age",age);tag.putBoolean("Recoverable",recoverable);tag.putBoolean("Spent",spent);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);cargo=ItemStack.parseOptional(registryAccess(),tag.getCompound("Cargo"));
        entityData.set(KIND,Math.clamp(tag.getInt("Kind"),SPIKE,HOOK));entityData.set(LATCHED,tag.getBoolean("Latched"));entityData.set(RETURNING,tag.getBoolean("Returning"));
        anchor=BlockPos.of(tag.getLong("Anchor"));units=Math.max(.001,tag.getDouble("Units"));ropeLength=Math.max(1,tag.getDouble("RopeLength"));
        entityData.set(ROPE_LENGTH,(float)ropeLength);
        age=tag.getInt("Age");recoverable=tag.getBoolean("Recoverable");spent=tag.getBoolean("Spent");setNoGravity(latched()||returning());
    }
}
