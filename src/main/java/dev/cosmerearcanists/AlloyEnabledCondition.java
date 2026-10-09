package dev.cosmerearcanists;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record AlloyEnabledCondition(String metal) implements ICondition {
    public static final MapCodec<AlloyEnabledCondition> CODEC = Codec.STRING.fieldOf("metal").xmap(AlloyEnabledCondition::new, AlloyEnabledCondition::metal);
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_SERIALIZERS, CosmereArcanists.MOD_ID);
    static { SERIALIZERS.register("alloy_enabled", () -> CODEC); }
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
    @Override public boolean test(IContext context) {
        var setting = MetallurgyConfig.ALLOYS.get(metal);
        return MetallurgyConfig.ALLOYING.get() && setting != null && setting.get();
    }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
