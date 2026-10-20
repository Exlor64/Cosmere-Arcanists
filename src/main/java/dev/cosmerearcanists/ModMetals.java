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
    public static final net.neoforged.neoforge.registries.DeferredItem<dev.cosmerearcanists.power.MetalVialItem> METAL_VIAL=ITEMS.register("metal_vial",()->new dev.cosmerearcanists.power.MetalVialItem());
    public static final net.neoforged.neoforge.registries.DeferredItem<dev.cosmerearcanists.filing.MetalFilingItem> METAL_FILING=ITEMS.register("metal_filing",dev.cosmerearcanists.filing.MetalFilingItem::new);
    public static final net.neoforged.neoforge.registries.DeferredItem<dev.cosmerearcanists.filing.MetalFileItem> METAL_FILE=ITEMS.register("metal_file",dev.cosmerearcanists.filing.MetalFileItem::new);
    public static final String[] GOD_METALS = {"atium", "lerasium"};
    public static final String[] BASE = {"tin", "zinc", "aluminum", "chromium", "cadmium", "lead", "silver", "nickel"};
    static {
        for(var metal:Metal.values())for(String slot:new String[]{"ring","bracelet","necklace"}){
            String id=metal.id()+"_metalmind_"+slot;int units=slot.equals("ring")?1:9;
            ITEMS.register(id,()->new dev.cosmerearcanists.feruchemy.MetalmindItem(metal.id(),slot,units));
        }

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
        for(String metal:GOD_METALS) {
            ITEMS.registerSimpleItem(metal+"_ingot",new Item.Properties());
            ITEMS.registerSimpleItem(metal+"_nugget",new Item.Properties());
            var block=BLOCKS.registerSimpleBlock(metal+"_block",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
            ITEMS.registerSimpleBlockItem(metal+"_block",block);
        }
        ITEMS.registerSimpleItem("copper_nugget", new Item.Properties());
        ITEMS.register("iron_vial", () -> new dev.cosmerearcanists.power.MetalVialItem("iron"));
        ITEMS.register("pewter_vial", () -> new dev.cosmerearcanists.power.MetalVialItem("pewter"));
        ITEMS.register("steel_vial", () -> new dev.cosmerearcanists.power.MetalVialItem("steel"));
        TABS.register("metals", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cosmerearcanists.metals"))
            .icon(() -> Items.IRON_INGOT.getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> {
                if(item.get()==METAL_FILING.get())dev.cosmerearcanists.filing.Filings.METALS.stream().sorted().forEach(metal->output.accept(dev.cosmerearcanists.filing.Filings.filing(metal,1)));
                else if(!(item.get() instanceof dev.cosmerearcanists.power.MetalVialItem vial) || vial.legacyMetal()==null)output.accept(item.get());
            }))
            .build());
    }
    private static void ore(String name, Block source) {
        var block = BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.ofFullCopy(source));
        ITEMS.registerSimpleBlockItem(name, block);
    }
    public static void register(IEventBus bus) { ITEMS.register(bus); BLOCKS.register(bus); TABS.register(bus); }
    private ModMetals() { }
}
