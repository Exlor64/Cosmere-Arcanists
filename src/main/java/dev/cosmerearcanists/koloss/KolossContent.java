package dev.cosmerearcanists.koloss;
import dev.cosmerearcanists.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;
public final class KolossContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,CosmereArcanists.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<KolossEntity>> KOLOSS=ENTITIES.register("koloss",()->EntityType.Builder.of(KolossEntity::new,MobCategory.MONSTER).sized(1.1f,2.6875f).clientTrackingRange(10).build("cosmerearcanists:koloss"));
    public static final DeferredItem<SwordItem> SWORD=ModMetals.ITEMS.register("koloss_sword",()->new SwordItem(Tiers.IRON,new Item.Properties().durability(750).attributes(SwordItem.createAttributes(Tiers.IRON,8,-3.2f))));
    public static final DeferredItem<SpawnEggItem> EGG=ModMetals.ITEMS.register("koloss_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(KOLOSS,0x586889,0x8A2929,new Item.Properties()));
    public static void register(IEventBus bus){
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(KOLOSS.get(),KolossEntity.attributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent e)->e.register(KOLOSS.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,KolossEntity::canSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
    private KolossContent(){}
}
