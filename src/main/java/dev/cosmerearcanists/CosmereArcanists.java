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
        ModMetals.register(bus);
        MetalOreFeature.register(bus);
        AlloyEnabledCondition.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, MetallurgyConfig.SPEC);
        NeoForge.EVENT_BUS.register(new OriginEvents());
    }
}
