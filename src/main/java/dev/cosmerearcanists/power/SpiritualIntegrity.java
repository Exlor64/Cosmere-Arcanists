package dev.cosmerearcanists.power;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

/** Persistent spiritual damage integration point for future Hemalurgy. Intact spirits default to1. */
public final class SpiritualIntegrity {
    private static final String KEY="cosmerearcanists_spiritual_integrity";
    private static CompoundTag data(LivingEntity target){return target instanceof ServerPlayer p?PowerData.data(p):target.getPersistentData();}
    public static double get(LivingEntity target){var tag=data(target);return tag.contains(KEY)?Math.clamp(tag.getDouble(KEY),0,1):1;}
    public static void set(LivingEntity target,double integrity){if(!Double.isFinite(integrity))throw new IllegalArgumentException("Integrity must be finite");data(target).putDouble(KEY,Math.clamp(integrity,0,1));}
    public static void damage(LivingEntity target,double amount){if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Damage must be nonnegative");set(target,get(target)-amount);}
    private SpiritualIntegrity() { }
}
