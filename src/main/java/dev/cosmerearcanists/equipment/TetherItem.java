package dev.cosmerearcanists.equipment;

import java.util.UUID;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class TetherItem extends Item {
    public final boolean hook;
    private static final String ACTIVE="cosmerearcanists_tether";
    private static final String RECALL="cosmerearcanists_recall_press";
    public TetherItem(boolean hook){super(new Properties().stacksTo(1));this.hook=hook;}
    public static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static UUID active(ItemStack stack){var tag=data(stack);return tag.hasUUID(ACTIVE)?tag.getUUID(ACTIVE):null;}
    private static void write(ItemStack stack,CompoundTag tag){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static void bind(ItemStack stack,UUID id){var tag=data(stack);tag.putUUID(ACTIVE,id);write(stack,tag);}
    public static void clear(ItemStack stack){var tag=data(stack);tag.remove(ACTIVE);write(stack,tag);}
    private static void recallPress(ItemStack stack,boolean value){var tag=data(stack);tag.putBoolean(RECALL,value);write(stack,tag);}
    public static boolean hasActiveHook(Player player){
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(stack.getItem() instanceof TetherItem item && item.hook && active(stack)!=null)return true;
        }
        return false;
    }
    @Override public UseAnim getUseAnimation(ItemStack stack){return hook?UseAnim.BOW:UseAnim.NONE;}
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!(hook?EquipmentConfig.GRAPPLE:EquipmentConfig.BALL).get())return InteractionResultHolder.fail(stack);
        if(!level.isClientSide && player instanceof ServerPlayer server){
            UUID id=active(stack); var entity=id==null?null:server.serverLevel().getEntity(id);
            if(entity instanceof EquipmentProjectile tether && tether.getOwner()==player){
                tether.reel();recallPress(stack,true);player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
            }
            clear(stack);recallPress(stack,false);
            if(!hook && !launch(server,stack,1))return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int timeLeft){
        if(!hook || level.isClientSide || !(user instanceof ServerPlayer player))return;
        if(data(stack).getBoolean(RECALL)){recallPress(stack,false);return;}
        if(active(stack)!=null)return;
        double charge=TetherPhysics.charge(getUseDuration(stack,user)-timeLeft,EquipmentConfig.HOOK_CHARGE.get());
        if(charge>=.1)launch(player,stack,charge);
    }
    public boolean launch(ServerPlayer player,ItemStack stack,double charge){
        if(active(stack)!=null)return false;
        double units=MetalAmounts.units(player.server,stack);
        if(units<=0){if(!hook)return false;units=9;}
        double speed=EquipmentConfig.HOOK_SPEED.get()*charge;
        if(!hook){
            if(!PowerService.holdingAbility(player,"a:steel") || !PowerData.available(player).contains("a:steel") || PowerData.state(player,"a:steel")==0
                || PowerConfig.CONSUMPTION.get() && PowerData.reserve(player,"steel")<PowerConfig.PROJECTILE_RESERVE_COST.get()){
                player.displayClientMessage(Component.literal("Hold steel's activation key while burning steel to launch the ball."),true);return false;
            }
            speed=MetalProjectilePhysics.speed(units,PowerService.powerForce(player,"a:steel")/7,PowerConfig.NUGGET_SPEED.get(),PowerConfig.PROJECTILE_MAX_SPEED.get());
        }
        var entity=new EquipmentProjectile(EquipmentContent.PROJECTILE.get(),player.level());
        entity.configure(hook?EquipmentProjectile.HOOK:EquipmentProjectile.BALL,stack.copyWithCount(1),units);
        entity.setOwner(player);entity.setPos(player.getX(),player.getEyeY()-.1,player.getZ());entity.setDeltaMovement(player.getLookAngle().scale(speed));
        if(!player.serverLevel().addFreshEntity(entity))return false;
        bind(stack,entity.getUUID());
        if(!hook){
            if(PowerConfig.CONSUMPTION.get())PowerData.reserve(player,"steel",PowerData.reserve(player,"steel")-PowerConfig.PROJECTILE_RESERVE_COST.get());
            if(PowerConfig.RECOIL.get())PowerService.externalImpulse(player,player.getLookAngle().scale(-MetalProjectilePhysics.recoil(units,speed,PowerConfig.NUGGET_MASS.get(),PowerConfig.PLAYER_MASS.get())));
        }
        return true;
    }
}
