package dev.cosmerearcanists.equipment;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SpikeCrossbowItem extends CrossbowItem {
    public SpikeCrossbowItem(){super(new Properties().durability(465).component(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,net.minecraft.world.item.component.ChargedProjectiles.EMPTY));}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,Player player,net.minecraft.world.InteractionHand hand){
        return EquipmentConfig.SPIKES.get()?super.use(level,player,hand):net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
    }
    @Override public Predicate<ItemStack> getAllSupportedProjectiles(){return stack->EquipmentConfig.SPIKES.get() && stack.getItem() instanceof SpikeItem;}
    @Override public Predicate<ItemStack> getSupportedHeldProjectiles(){return getAllSupportedProjectiles();}
    @Override public ItemStack getDefaultCreativeAmmo(Player player,ItemStack weapon){return EquipmentContent.SPIKES.get("iron").get().getDefaultInstance();}
    @Override public Projectile createProjectile(Level level,LivingEntity shooter,ItemStack weapon,ItemStack ammo,boolean critical){
        var spike=new EquipmentProjectile(EquipmentContent.PROJECTILE.get(),level);
        spike.configure(EquipmentProjectile.SPIKE,ammo.copyWithCount(1),18.0/2);
        spike.setOwner(shooter); spike.setPos(shooter.getX(),shooter.getEyeY()-.1,shooter.getZ());
        return spike;
    }
    @Override public void shootProjectile(LivingEntity shooter,Projectile projectile,int index,float speed,float inaccuracy,float angle,LivingEntity target){
        if(projectile instanceof EquipmentProjectile spike)spike.recoverable=index==0 && !(shooter instanceof Player p && p.getAbilities().instabuild);
        super.shootProjectile(shooter,projectile,index,speed,inaccuracy,angle,target);
    }
}
