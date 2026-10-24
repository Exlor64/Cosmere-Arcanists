package dev.cosmerearcanists.power;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

/** Persistent spiritual damage integration point for future Hemalurgy. Intact spirits default to1. */
public final class SpiritualIntegrity {
    private static final String KEY="cosmerearcanists_spiritual_integrity";
    private static CompoundTag data(LivingEntity target){return target instanceof ServerPlayer p?PowerData.data(p):target.getPersistentData();}
    public static double base(LivingEntity target){var tag=data(target);double value=tag.contains(KEY)?tag.getDouble(KEY):1;return Double.isFinite(value)?Math.min(value,1):1;}
    public static double get(LivingEntity target){return base(target)-(target instanceof ServerPlayer p?dev.cosmerearcanists.hemalurgy.ImplantService.integrityCost(p):0);}
    public static double takeoverMultiplier(LivingEntity target){return dev.cosmerearcanists.hemalurgy.ImplantRules.takeover(get(target));}
    public static void set(LivingEntity target,double integrity){if(!Double.isFinite(integrity))throw new IllegalArgumentException("Integrity must be finite");data(target).putDouble(KEY,Math.min(integrity,1));}
    public static void damage(LivingEntity target,double amount){if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Damage must be nonnegative");set(target,base(target)-amount);}
    private SpiritualIntegrity() { }
}
