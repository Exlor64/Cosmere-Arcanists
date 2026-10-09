package dev.cosmerearcanists;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMetals {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CosmereArcanists.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CosmereArcanists.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CosmereArcanists.MOD_ID);
    public static final String[] BASE = {"tin", "zinc", "aluminum", "chromium", "cadmium", "lead", "silver", "nickel"};
    static {
        for (String metal : BASE) {
            ITEMS.registerSimpleItem("raw_" + metal, new Item.Properties());
            ore(metal + "_ore", Blocks.IRON_ORE);
            ore("deepslate_" + metal + "_ore", Blocks.DEEPSLATE_IRON_ORE);
        }
        for (String metal : new String[]{"tin", "zinc", "aluminum", "chromium", "cadmium", "lead", "silver", "nickel",
                "steel", "pewter", "brass", "bronze", "duralumin", "electrum", "bendalloy", "nicrosil"}) {
            ITEMS.registerSimpleItem(metal + "_ingot", new Item.Properties());
            ITEMS.registerSimpleItem(metal + "_nugget", new Item.Properties());
        }
        ITEMS.registerSimpleItem("copper_nugget", new Item.Properties());
        ITEMS.register("iron_vial", () -> new dev.cosmerearcanists.power.MetalVialItem("iron"));
        ITEMS.register("steel_vial", () -> new dev.cosmerearcanists.power.MetalVialItem("steel"));
        TABS.register("metals", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cosmerearcanists.metals"))
            .icon(() -> Items.IRON_INGOT.getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
    }
    private static void ore(String name, Block source) {
        var block = BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.ofFullCopy(source));
        ITEMS.registerSimpleBlockItem(name, block);
    }
    public static void register(IEventBus bus) { ITEMS.register(bus); BLOCKS.register(bus); TABS.register(bus); }
    private ModMetals() { }
}
