package dev.cosmerearcanists.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class MetalVialItem extends Item {
    private final String metal;
    public MetalVialItem(String metal) { super(new Properties().stacksTo(16)); this.metal = metal; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) return InteractionResultHolder.success(stack);
        if (!PowerConfig.ENABLED.get() || (metal.equals("pewter") ? !PewterService.configured() : !PowerConfig.IRON_STEEL.get()) || PowerData.reserve(server, metal) >= PowerConfig.RESERVE_CAP.get()) return InteractionResultHolder.fail(stack);
        PowerData.reserve(server, metal, PowerData.reserve(server, metal) + PowerConfig.VIAL_RESERVE.get());
        if (!player.getAbilities().instabuild) {
            stack.shrink(1); var bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        PowerNetwork.sync(server, java.util.List.of());
        return InteractionResultHolder.success(stack);
    }
}
