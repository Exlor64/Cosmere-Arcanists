package dev.cosmerearcanists.treasure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cosmerearcanists.CosmereArcanists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.*;
import net.neoforged.neoforge.registries.*;

/** One rare additive roll on designated chest tables, after Fortune's raw-table sampling. */
public final class TreasureLootModifier extends LootModifier {
    public static final MapCodec<TreasureLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        instance -> codecStart(instance).apply(instance, TreasureLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CosmereArcanists.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<TreasureLootModifier>> SERIALIZER =
        REGISTER.register("rare_treasure", () -> CODEC);
    public TreasureLootModifier(LootItemCondition[] conditions) { super(conditions); }
    public static void register(IEventBus bus) { REGISTER.register(bus); }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!TreasureConfig.ENABLED.get() || !context.getLevel().getServer().isSameThread()) return loot;
        var id = context.getQueriedLootTableId();
        if (id == null || !TreasureConfig.TABLES.get().contains(id.toString())) return loot;
        var dimensions = TreasureConfig.DIMENSIONS.get();
        if (!dimensions.isEmpty() && !dimensions.contains(context.getLevel().dimension().location().toString())) return loot;
        var table = context.getLevel().getServer().reloadableRegistries()
            .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        if (table.getParamSet() != LootContextParamSets.CHEST
            || context.getRandom().nextInt(TreasureConfig.ONE_IN.get()) != 0) return loot;
        var treasure = TreasureItems.generate(context.getLevel(), context.getRandom());
        if (!treasure.isEmpty()) loot.add(treasure);
        return loot;
    }
}
