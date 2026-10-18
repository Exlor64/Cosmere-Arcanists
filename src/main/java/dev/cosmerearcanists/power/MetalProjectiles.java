package dev.cosmerearcanists.power;

import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MetalProjectiles {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,CosmereArcanists.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<SteelProjectile>> STEEL=ENTITIES.register("steel_projectile",
        ()->EntityType.Builder.<SteelProjectile>of(SteelProjectile::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(8).updateInterval(1).build("cosmerearcanists:steel_projectile"));
    public static void register(net.neoforged.bus.api.IEventBus bus){ENTITIES.register(bus);}
    /** Null leaves the normal drop intact; a recoil vector means the projectile replaced it successfully. */
    public static Vec3 launch(ServerPlayer player,ItemEntity dropped) {
        if(!PowerConfig.STEEL_PROJECTILES.get() || !PowerConfig.IRON_STEEL.get() || !player.isAlive() || player.isSpectator()
            || !PowerService.holdingAbility(player,"a:steel") || !PowerData.available(player).contains("a:steel") || PowerData.state(player,"a:steel")==0)return null;
        double units=MetalAmounts.units(player.server,dropped.getItem()); if(units<=0)return null;
        double cost=PowerConfig.PROJECTILE_RESERVE_COST.get();
        if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(player,"steel")<cost)return null;
        if(!PowerConfig.FLARING.get() && PowerData.state(player,"a:steel")==3)PowerData.state(player,"a:steel",2);
        double power=PowerService.powerForce(player,"a:steel")/7.0;
        double speed=MetalProjectilePhysics.speed(units,power,PowerConfig.NUGGET_SPEED.get(),PowerConfig.PROJECTILE_MAX_SPEED.get());
        if(speed<=0)return null;
        var projectile=new SteelProjectile(STEEL.get(),player.level());
        projectile.setOwner(player); projectile.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
        projectile.cargo(dropped.getItem(),units); Vec3 direction=player.getLookAngle().normalize();
        projectile.setDeltaMovement(direction.scale(speed));
        if(!player.serverLevel().addFreshEntity(projectile))return null;
        if(PowerConfig.CONSUMPTION.get())PowerData.reserve(player,"steel",PowerData.reserve(player,"steel")-cost);
        return PowerConfig.RECOIL.get() ? direction.scale(-MetalProjectilePhysics.recoil(units,speed,PowerConfig.NUGGET_MASS.get(),PowerConfig.PLAYER_MASS.get())) : Vec3.ZERO;
    }
    private MetalProjectiles() { }
}
