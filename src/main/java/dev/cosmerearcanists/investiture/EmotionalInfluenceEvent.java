package dev.cosmerearcanists.investiture;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Future zinc/brass and compatible mods must call allowed immediately before applying emotional influence.
 * This event is exclusively for emotional Investiture, not general damage, potions or AI effects.
 */
public final class EmotionalInfluenceEvent extends Event implements ICancellableEvent {
    private final LivingEntity target;
    private final double strength;
    public EmotionalInfluenceEvent(LivingEntity target,double strength){if(!Double.isFinite(strength)||strength<=0)throw new IllegalArgumentException("Invalid emotional strength");this.target=java.util.Objects.requireNonNull(target);this.strength=strength;}
    public LivingEntity target(){return target;}
    public double strength(){return strength;}
    public static boolean allowed(LivingEntity target,double strength){if(target.level().isClientSide)return false;return !NeoForge.EVENT_BUS.post(new EmotionalInfluenceEvent(target,strength)).isCanceled();}
}
