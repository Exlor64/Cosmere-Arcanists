package dev.cosmerearcanists;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MetalOreFeature extends OreFeature {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CosmereArcanists.MOD_ID);
    static { FEATURES.register("metal_ore", () -> new MetalOreFeature()); }
    private MetalOreFeature() { super(OreConfiguration.CODEC); }
    public static void register(IEventBus bus) { FEATURES.register(bus); }
    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        if (!MetallurgyConfig.GENERATION.get() || context.config().targetStates.isEmpty()) return false;
        String name = BuiltInRegistries.BLOCK.getKey(context.config().targetStates.getFirst().state.getBlock()).getPath();
        String metal = name.replace("deepslate_", "").replace("_ore", "");
        var enabled = MetallurgyConfig.ORES.get(metal);
        return enabled != null && enabled.get() && context.random().nextDouble() < MetallurgyConfig.CHANCES.get(metal).get()
                && super.place(context);
    }
}
