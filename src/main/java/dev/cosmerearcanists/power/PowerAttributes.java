package dev.cosmerearcanists.power;

import java.util.LinkedHashMap;
import java.util.Map;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.Metal;
import dev.cosmerearcanists.OriginData;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent ability strength: zero is absent, one is normal strength. */
public final class PowerAttributes {
    private static final DeferredRegister<Attribute> REGISTER = DeferredRegister.create(Registries.ATTRIBUTE, CosmereArcanists.MOD_ID);
    private static final Map<String, DeferredHolder<Attribute, Attribute>> POWERS = new LinkedHashMap<>();
    public static final DeferredHolder<Attribute,Attribute> IMPLANT_VISION=REGISTER.register("hemalurgic_vision",()->new RangedAttribute("attribute.cosmerearcanists.hemalurgic_vision",1,.01,1).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> MASS=REGISTER.register("physical_mass_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.physical_mass_multiplier",1,.01,1000000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> PHYSICAL_STRENGTH=REGISTER.register("physical_strength_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.physical_strength_multiplier",1,.01,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> SIGHT=REGISTER.register("physical_sight_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.physical_sight_multiplier",1,.01,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> HEARING=REGISTER.register("physical_hearing_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.physical_hearing_multiplier",1,.01,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> TOUCH=REGISTER.register("physical_touch_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.physical_touch_multiplier",1,.01,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> IDENTITY=REGISTER.register("spiritual_identity_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.spiritual_identity_multiplier",1,0,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> MENTAL_SPEED=REGISTER.register("mental_speed_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.mental_speed_multiplier",1,.01,1000).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> WEAPON_PREPARATION=REGISTER.register("weapon_preparation_multiplier",()->new RangedAttribute("attribute.cosmerearcanists.weapon_preparation_multiplier",1,.01,1000).setSyncable(true));
    private static final String MIGRATED = "attributes_migrated";
    static {
        for (String art : new String[]{"a", "f"}) for (Metal metal : Metal.values()) {
            String name = (art.equals("a") ? "allomancy_" : "feruchemy_") + metal.id();
            POWERS.put(art + ":" + metal.id(), REGISTER.register(name,
                () -> new RangedAttribute("attribute.cosmerearcanists." + name, 0, 0, 1024).setSyncable(true)));
        }
        POWERS.put("a:atium", REGISTER.register("allomancy_atium",
            () -> new RangedAttribute("attribute.cosmerearcanists.allomancy_atium",0,0,1024).setSyncable(true)));
    }
    public static void register(IEventBus bus) {
        REGISTER.register(bus);
        bus.addListener(PowerAttributes::addToPlayers);
    }
    private static void addToPlayers(EntityAttributeModificationEvent event) {
        POWERS.values().forEach(attribute -> event.add(EntityType.PLAYER, attribute));
        event.add(EntityType.PLAYER,IMPLANT_VISION);event.add(EntityType.PLAYER,WEAPON_PREPARATION);event.add(EntityType.PLAYER,MENTAL_SPEED);event.add(EntityType.PLAYER,IDENTITY);event.add(EntityType.PLAYER,MASS);event.add(EntityType.PLAYER,PHYSICAL_STRENGTH);event.add(EntityType.PLAYER,SIGHT);event.add(EntityType.PLAYER,HEARING);event.add(EntityType.PLAYER,TOUCH);
    }
    public static net.minecraft.core.Holder<Attribute> holder(String key){return POWERS.get(key);}
    public static java.util.Set<String> keys() { return java.util.Collections.unmodifiableSet(POWERS.keySet()); }
    public static double strength(ServerPlayer player, String power) {
        if(dev.cosmerearcanists.hemalurgy.AluminumSuppression.active(player))return 0;
        migrate(player);
        var holder = POWERS.get(power);
        return holder == null ? 0 : player.getAttributeValue(holder);
    }
    /** Personal permanent strength; external/equipment/Hemalurgic modifiers never become stored powers. */
    public static double baseStrength(ServerPlayer player,String power){migrate(player);var holder=POWERS.get(power);var instance=holder==null?null:player.getAttribute(holder);double value=instance==null?0:instance.getBaseValue();return Double.isFinite(value)?Math.clamp(value,0,1024):0;}
    public static net.minecraft.world.entity.ai.attributes.AttributeInstance instance(ServerPlayer p,String power){var holder=POWERS.get(power);return holder==null?null:p.getAttribute(holder);}
    public static double effectiveStrength(ServerPlayer player,String power){return strength(player,power)*EnhancementService.multiplier(player,power);}
    public static void set(ServerPlayer player, String power, double strength) {
        migrate(player);
        setBase(player, power, strength);
    }
    private static void setBase(ServerPlayer player, String power, double strength) {
        var holder = POWERS.get(power);
        if (holder == null) return;
        var instance = player.getAttribute(holder);
        if (instance != null) {double value=Double.isFinite(strength)?Math.clamp(strength,0,1024):0;dev.cosmerearcanists.compounding.PowerSources.baseChanged(player,power,instance.getBaseValue(),value);instance.setBaseValue(value);}
    }
    /** Convert legacy origin/grant data once, including when powers are disabled. */
    public static void migrate(ServerPlayer player) {
        var data = PowerData.data(player);
        if (data.getBoolean(MIGRATED)) return;
        data.putBoolean(MIGRATED, true);
        if (OriginData.assigned(player)) {
            var origin = OriginData.read(player);
            seed(player, "a:" + origin.allomancy());
            seed(player, "f:" + origin.feruchemy());
        }
        var grants = data.getCompound("grants");
        for (String key : grants.getAllKeys()) if (grants.getBoolean(key)) seed(player, key);
        data.remove("grants");
    }
    private static void seed(ServerPlayer player, String power) {
        var holder = POWERS.get(power);
        if (holder != null) {
            var instance = player.getAttribute(holder);
            if (instance != null && instance.getBaseValue() <= 0) setBase(player,power,1);
        }
    }
    public static void assignOrigin(ServerPlayer player, OriginData previous, OriginData next) {
        migrate(player);
        if (previous != null) {
            setBase(player, "a:" + previous.allomancy(), 0);
            setBase(player, "f:" + previous.feruchemy(), 0);
        }
        seed(player, "a:" + next.allomancy());
        seed(player, "f:" + next.feruchemy());
    }
    public static void reset(ServerPlayer player) {
        dev.cosmerearcanists.compounding.CompoundingService.clear(player);dev.cosmerearcanists.nicrosil.TemporaryPowers.stop(player);
        dev.cosmerearcanists.compounding.PowerSources.reset(player);
        for (var holder : POWERS.values()) {
            var instance = player.getAttribute(holder);
            if (instance != null) { instance.removeModifiers(); instance.setBaseValue(0); }
        }
    }
    public static void copy(ServerPlayer original, ServerPlayer replacement) {
        migrate(original);
        for (var holder : POWERS.values()) {
            var source = original.getAttribute(holder);
            var target = replacement.getAttribute(holder);
            // save/load copies the base and permanent modifiers, not temporary equipment effects.
            if (source != null && target != null) { target.removeModifiers(); target.load(source.save()); }
        }
    }
    private PowerAttributes() { }
}
