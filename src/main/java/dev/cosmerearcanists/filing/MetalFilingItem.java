package dev.cosmerearcanists.filing;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class MetalFilingItem extends Item {
    public MetalFilingItem(){super(new Properties());}
    @Override public Component getName(ItemStack stack){var metal=Filings.metal(stack);return Component.literal(metal.isEmpty()?"Metal Filing":Character.toUpperCase(metal.charAt(0))+metal.substring(1)+" Filing");}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.literal("Right-click with a vial or glass bottle in the other hand to load one filing."));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!FilingConfig.ENABLED.get())return InteractionResultHolder.fail(stack);
        if(player instanceof ServerPlayer server && !Filings.load(server,hand))return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
