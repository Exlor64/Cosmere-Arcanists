package dev.cosmerearcanists.power;

import net.minecraft.server.MinecraftServer;

/** Metal-bearing projectiles share detection, mass and fixed-anchor behavior. */
public interface AllomanticProjectile {
    boolean allomanticallyDetectable(MinecraftServer server);
    double allomanticMass();
    default boolean fixedMetalAnchor(){return false;}
    default void beforeAllomanticForce(){ }
}
