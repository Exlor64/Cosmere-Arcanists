package dev.cosmerearcanists.filing;

import java.util.*;
import dev.cosmerearcanists.ModMetals;
import dev.cosmerearcanists.power.MetalVialItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeInput;

/** Persistent per-piece filing progress and per-metal vial counts. No registry or recipe mutation. */
public final class Filings {
    private static final String FILED="cosmerearcanists_filed", CONTENTS="cosmerearcanists_filings", METAL="cosmerearcanists_filing_metal";
    public static final Set<String> METALS=Set.of("iron","steel","tin","pewter","zinc","brass","copper","bronze","aluminum","duralumin","chromium","nicrosil","gold","electrum","cadmium","bendalloy","lead","silver","nickel","atium","lerasium");
    public record Source(String metal,int nuggetUnits) { }
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean filed(ItemStack stack){return !stack.isEmpty() && data(stack).getCompound(FILED).getInt("used")>0;}
    public static boolean hasFiled(RecipeInput input){for(int i=0;i<input.size();i++)if(filed(input.getItem(i)))return true;return false;}
    public static int capacity(ItemStack stack){return Math.max(0,data(stack).getCompound(FILED).getInt("capacity"));}
    public static int remaining(ItemStack stack){var tag=data(stack).getCompound(FILED);return Math.max(0,tag.getInt("capacity")-tag.getInt("used"));}
    public static Source source(ItemStack stack) {
        if(stack.isEmpty())return null;
        String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        var tags=stack.getTags().map(t->t.location()).filter(t->t.getNamespace().equals("c")).map(t->t.getPath()).toList();
        for(String metal:METALS) {
            String alias=metal.equals("aluminum")?"aluminium":metal;
            if(path.equals(metal+"_nugget") || tags.contains("nuggets/"+metal) || tags.contains("nuggets/"+alias))return new Source(metal,1);
            if(path.equals(metal+"_ingot") || tags.contains("ingots/"+metal) || tags.contains("ingots/"+alias))return new Source(metal,9);
            if(path.equals(metal+"_block") || tags.contains("storage_blocks/"+metal) || tags.contains("storage_blocks/"+alias))return new Source(metal,81);
        }
        return null;
    }
    public static ItemStack filing(String metal,int count) {
        var result=new ItemStack(ModMetals.METAL_FILING.get(),count);
        var tag=data(result);tag.putString(METAL,metal);result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return result;
    }
    public static String metal(ItemStack stack){return stack.getItem() instanceof MetalFilingItem?data(stack).getString(METAL):"";}
    public static Map<String,Integer> contents(ItemStack stack) {
        if(!(stack.getItem() instanceof MetalVialItem vial))return Map.of();
        var root=data(stack);var tag=root.getCompound(CONTENTS);var result=new TreeMap<String,Integer>();
        for(String metal:tag.getAllKeys())if(METALS.contains(metal) && tag.getInt(metal)>0)result.put(metal,Math.min(1000000,tag.getInt(metal)));
        // Old registered vial IDs remain usable, equivalent to one filing until explicitly rewritten.
        if(!root.contains(CONTENTS) && vial.legacyMetal()!=null)result.put(vial.legacyMetal(),1);
        return result;
    }
    public static int count(Map<String,Integer> contents){return contents.values().stream().mapToInt(Integer::intValue).sum();}
    public static void contents(ItemStack stack,Map<String,Integer> contents) {
        var root=data(stack);var tag=new CompoundTag();contents.forEach((metal,count)->{if(METALS.contains(metal)&&count>0)tag.putInt(metal,count);});
        root.put(CONTENTS,tag);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
    }
    public static ItemStack vial(Map<String,Integer> contents){var stack=new ItemStack(ModMetals.METAL_VIAL.get());contents(stack,contents);return stack;}
    public static boolean aluminum(String metal){return metal.equals("aluminum") || metal.equals("duralumin");}
    public static boolean dynamic(ItemStack stack){return stack.getItem() instanceof MetalFilingItem || stack.getItem() instanceof MetalVialItem;}
    public static boolean detectable(ItemStack stack) {
        if(stack.getItem() instanceof MetalFilingItem)return METALS.contains(metal(stack)) && !aluminum(metal(stack));
        var contents=contents(stack);return count(contents)>0 && contents.keySet().stream().noneMatch(Filings::aluminum);
    }
    public static double units(ItemStack stack) {
        if(stack.getItem() instanceof MetalFilingItem)return stack.getCount()/(double)FilingConfig.PER_NUGGET.get();
        if(stack.getItem() instanceof MetalVialItem)return (double)count(contents(stack))*stack.getCount()/FilingConfig.PER_NUGGET.get();
        var source=source(stack);return source==null?0:source.nuggetUnits()*stack.getCount()*(double)remaining(stack)/Math.max(1,capacity(stack));
    }
    public static void give(ServerPlayer player,ItemStack stack){if(!stack.isEmpty() && !player.getInventory().add(stack))player.drop(stack,false);}
    /** Takes exactly one physical piece, leaving the rest of an unfiled stack untouched. */
    public static boolean file(ServerPlayer player) {
        if(!FilingConfig.ENABLED.get())return false;
        var sourceStack=player.getOffhandItem();var source=source(sourceStack);if(source==null)return false;
        var piece=sourceStack.copyWithCount(1);var root=data(piece);var progress=root.getCompound(FILED);
        int capacity=filed(piece)?capacity(piece):source.nuggetUnits()*FilingConfig.PER_NUGGET.get();
        int used=filed(piece)?progress.getInt("used"):0;if(used>=capacity)return false;
        used++;progress.putInt("used",used);progress.putInt("capacity",capacity);root.put(FILED,progress);
        sourceStack.shrink(1);
        if(used<capacity) {
            piece.set(DataComponents.MAX_STACK_SIZE,1);piece.set(DataComponents.MAX_DAMAGE,capacity);piece.set(DataComponents.DAMAGE,used);
            piece.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            player.setItemInHand(InteractionHand.OFF_HAND,piece);
            if(!sourceStack.isEmpty())give(player,sourceStack);
        }
        give(player,filing(source.metal(),1));return true;
    }
    /** Source is loose filings; the other hand contains a vial or glass bottle. */
    public static boolean load(ServerPlayer player,InteractionHand filingsHand) {
        if(!FilingConfig.ENABLED.get())return false;
        var loose=player.getItemInHand(filingsHand);String metal=metal(loose);if(!METALS.contains(metal))return false;
        var vialHand=filingsHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        var target=player.getItemInHand(vialHand);if(!target.is(Items.GLASS_BOTTLE) && !(target.getItem() instanceof MetalVialItem))return false;
        var contents=new TreeMap<>(contents(target));if(count(contents)>=FilingConfig.VIAL_CAPACITY.get())return false;
        contents.merge(metal,1,Integer::sum);var filled=vial(contents);
        target.shrink(1);if(target.isEmpty())player.setItemInHand(vialHand,filled);else give(player,filled);
        loose.shrink(1);return true;
    }
    private Filings() { }
}
