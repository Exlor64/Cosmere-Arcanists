package dev.cosmerearcanists;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CosmereArcanists.MOD_ID)
public final class CosmereArcanists {
    public static final String MOD_ID = "cosmerearcanists";
    public CosmereArcanists(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        dev.cosmerearcanists.equipment.EquipmentContent.register(bus);
        ModMetals.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.filing.FilingConfig.SPEC, "cosmerearcanists-filings-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.filing.FilingEvents());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.equipment.EquipmentConfig.SPEC, "cosmerearcanists-equipment-server.toml");
        dev.cosmerearcanists.power.MetalProjectiles.register(bus);
        dev.cosmerearcanists.power.PowerAttributes.register(bus);
        MetalOreFeature.register(bus);
        AlloyEnabledCondition.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, MetallurgyConfig.SPEC);
        NeoForge.EVENT_BUS.register(new OriginEvents());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.PowerConfig.SPEC, "cosmerearcanists-powers-server.toml");
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.PewterConfig.SPEC, "cosmerearcanists-pewter-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.PewterService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.TinConfig.SPEC, "cosmerearcanists-tin-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.TinService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.TemporalConfig.SPEC, "cosmerearcanists-temporal-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.TemporalService());
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.ScoutService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.SeekingConfig.SPEC, "cosmerearcanists-seeking-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.SeekingService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.EmotionConfig.SPEC, "cosmerearcanists-emotions-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.EmotionService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.EnhancementConfig.SPEC, "cosmerearcanists-enhancement-server.toml");
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.ContactConfig.SPEC, "cosmerearcanists-contact-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.ContactService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.GoldConfig.SPEC, "cosmerearcanists-gold-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.GoldService());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.BubbleConfig.SPEC, "cosmerearcanists-bubbles-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.BubbleService());
        container.registerConfig(ModConfig.Type.SERVER,dev.cosmerearcanists.feruchemy.FeruchemyConfig.SPEC,"cosmerearcanists-feruchemy-server.toml");
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.feruchemy.IronFeruchemy());
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.feruchemy.SteelFeruchemy());
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.feruchemy.PewterFeruchemy());
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.feruchemy.MetalmindEvents());
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->{if(net.neoforged.fml.ModList.get().isLoaded("curios"))event.enqueueWork(dev.cosmerearcanists.feruchemy.CuriosMetalminds::register);});
        bus.addListener(dev.cosmerearcanists.power.PowerNetwork::register);
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.PowerService());
    }
}
