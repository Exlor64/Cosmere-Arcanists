package dev.cosmerearcanists.investiture;

import java.util.*;
import dev.cosmerearcanists.power.EnhancementService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Validated server contact. Integrations queue their own resource changes, applied only after cancellation checks.
 * Nicrosil integrations must commit a finite fuel snapshot and prevent refresh/stacking themselves. */
public final class InvestitureContactEvent extends Event implements ICancellableEvent {
    public enum Kind { CHROMIUM, NICROSIL }
    private final ServerPlayer source;private final LivingEntity target;private final Kind kind;private final double strength;
    private final EnhancementService.Profile profile;private final List<Runnable> effects=new ArrayList<>();
    public InvestitureContactEvent(ServerPlayer source,LivingEntity target,Kind kind,double strength,EnhancementService.Profile profile){this.source=Objects.requireNonNull(source);this.target=Objects.requireNonNull(target);this.kind=Objects.requireNonNull(kind);this.strength=strength;this.profile=Objects.requireNonNull(profile);}
    public ServerPlayer source(){return source;}public LivingEntity target(){return target;}public Kind kind(){return kind;}public double strength(){return strength;}public EnhancementService.Profile profile(){return profile;}
    public void addEffect(Runnable effect){effects.add(Objects.requireNonNull(effect));}
    public boolean hasEffects(){return !effects.isEmpty();}
    public void applyEffects(){if(isCanceled())return;var pending=List.copyOf(effects);effects.clear();pending.forEach(Runnable::run);}
}
