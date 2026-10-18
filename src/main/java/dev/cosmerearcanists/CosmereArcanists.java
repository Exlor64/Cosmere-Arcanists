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
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.equipment.EquipmentConfig.SPEC, "cosmerearcanists-equipment-server.toml");
        dev.cosmerearcanists.power.MetalProjectiles.register(bus);
        dev.cosmerearcanists.power.PowerAttributes.register(bus);
        MetalOreFeature.register(bus);
        AlloyEnabledCondition.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, MetallurgyConfig.SPEC);
        NeoForge.EVENT_BUS.register(new OriginEvents());
        container.registerConfig(ModConfig.Type.SERVER, dev.cosmerearcanists.power.PowerConfig.SPEC, "cosmerearcanists-powers-server.toml");
        bus.addListener(dev.cosmerearcanists.power.PowerNetwork::register);
        NeoForge.EVENT_BUS.register(new dev.cosmerearcanists.power.PowerService());
    }
}
