package dev.cosmerearcanists.filing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class MetalFileItem extends Item {
    public MetalFileItem(){super(new Properties().durability(256));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND || !FilingConfig.ENABLED.get() || Filings.source(player.getOffhandItem())==null)return InteractionResultHolder.fail(stack);
        if(player instanceof ServerPlayer server && Filings.file(server) && !server.getAbilities().instabuild)
            stack.hurtAndBreak(FilingConfig.FILE_DAMAGE.get(),server,EquipmentSlot.MAINHAND);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer()==null?InteractionResult.PASS:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();
    }
}
