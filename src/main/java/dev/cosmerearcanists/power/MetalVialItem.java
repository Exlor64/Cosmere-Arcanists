package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.filing.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** One dynamic vial can contain several metals. Old vial IDs remain valid saved items. */
public final class MetalVialItem extends Item {
    private final String legacyMetal;
    public MetalVialItem(){this(null);}
    public MetalVialItem(String metal){super(new Properties().stacksTo(metal==null?1:16));this.legacyMetal=metal;}
    public String legacyMetal(){return legacyMetal;}
    @Override public Component getName(ItemStack stack){return Component.literal("Metal Vial ("+Filings.count(Filings.contents(stack))+" filings)");}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        Filings.contents(stack).forEach((metal,count)->lines.add(Component.literal(Character.toUpperCase(metal.charAt(0))+metal.substring(1)+": "+count+" filing"+(count==1?"":"s"))));
        lines.add(Component.literal("Hold right-click to drink. Filings that cannot fit remain in the vial."));
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return FilingConfig.DRINK_TICKS.get();}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    public static int fits(ServerPlayer p,String metal,int count) {
        return Math.min(count,(int)Math.max(0,Math.floor((PowerConfig.RESERVE_CAP.get()-PowerData.reserve(p,metal)+1e-7)/FilingConfig.RESERVE.get())));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!FilingConfig.ENABLED.get() || !PowerConfig.ENABLED.get())return InteractionResultHolder.fail(stack);
        var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        if(player.getItemInHand(other).getItem() instanceof MetalFilingItem) {
            if(player instanceof ServerPlayer server && !Filings.load(server,other))return InteractionResultHolder.fail(stack);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        var contents=Filings.contents(stack);
        if(contents.isEmpty() || player instanceof ServerPlayer server && contents.entrySet().stream().noneMatch(e->fits(server,e.getKey(),e.getValue())>0))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user) {
        if(!(user instanceof ServerPlayer player) || !FilingConfig.ENABLED.get() || !PowerConfig.ENABLED.get())return stack;
        var contents=new TreeMap<>(Filings.contents(stack));int drank=0;
        for(var entry:new ArrayList<>(contents.entrySet())) {
            int count=fits(player,entry.getKey(),entry.getValue());if(count==0)continue;
            PowerData.reserve(player,entry.getKey(),PowerData.reserve(player,entry.getKey())+count*FilingConfig.RESERVE.get());drank+=count;
            if(count==entry.getValue())contents.remove(entry.getKey());else contents.put(entry.getKey(),entry.getValue()-count);
        }
        if(drank==0)return stack;
        CriteriaTriggers.CONSUME_ITEM.trigger(player,stack);player.awardStat(Stats.ITEM_USED.get(this));PowerNetwork.sync(player,List.of());
        if(player.getAbilities().instabuild)return stack;
        if(!contents.isEmpty()){Filings.contents(stack,contents);return stack;}
        stack.shrink(1);
        if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);
        Filings.give(player,new ItemStack(Items.GLASS_BOTTLE));return stack;
    }
}
