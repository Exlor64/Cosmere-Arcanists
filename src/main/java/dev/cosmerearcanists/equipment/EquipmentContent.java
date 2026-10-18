package dev.cosmerearcanists.equipment;

import java.util.LinkedHashMap;
import java.util.Map;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.ModMetals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EquipmentContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,CosmereArcanists.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<EquipmentProjectile>> PROJECTILE=ENTITIES.register("equipment_projectile",
        ()->EntityType.Builder.<EquipmentProjectile>of(EquipmentProjectile::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(10).updateInterval(1).build("cosmerearcanists:equipment_projectile"));
    public static final Map<String,DeferredHolder<Item,SpikeItem>> SPIKES=new LinkedHashMap<>();
    public static final DeferredHolder<Item,SpikeCrossbowItem> CROSSBOW=ModMetals.ITEMS.register("spike_crossbow",SpikeCrossbowItem::new);
    public static final DeferredHolder<Item,TetherItem> BALL=ModMetals.ITEMS.register("ball_and_rope",()->new TetherItem(false));
    public static final DeferredHolder<Item,TetherItem> GRAPPLE=ModMetals.ITEMS.register("grappling_hook",()->new TetherItem(true));
    static {
        for(String metal:new String[]{"iron","copper","gold","tin","zinc","aluminum","chromium","cadmium","lead","silver","nickel","steel","pewter","brass","bronze","duralumin","electrum","bendalloy","nicrosil"})
            SPIKES.put(metal,ModMetals.ITEMS.register(metal+"_spike",()->new SpikeItem(metal)));
    }
    public static void register(IEventBus bus){ENTITIES.register(bus);}
    private EquipmentContent(){ }
}
