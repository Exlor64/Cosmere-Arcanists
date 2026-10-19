package dev.cosmerearcanists;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Conversion gates never remove registered god-metal content or existing world blocks. */
public record GodMetalEnabledCondition(String metal) implements ICondition {
    public static final MapCodec<GodMetalEnabledCondition> CODEC=Codec.STRING.fieldOf("metal").xmap(GodMetalEnabledCondition::new,GodMetalEnabledCondition::metal);
    @Override public boolean test(IContext context){var setting=MetallurgyConfig.GOD_METALS.get(metal);return MetallurgyConfig.GOD_CONVERSIONS.get() && setting!=null && setting.get();}
    @Override public MapCodec<? extends ICondition> codec(){return CODEC;}
}
