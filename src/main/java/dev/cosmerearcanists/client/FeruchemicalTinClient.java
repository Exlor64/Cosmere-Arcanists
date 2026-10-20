package dev.cosmerearcanists.client;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.TinMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
/** Sight reveals only normally tracked, unobstructed entities to the local viewer. */
public final class FeruchemicalTinClient {
    public static double invisibleOpacity(Entity entity){var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.isAlive()||mc.player.isSpectator()||entity==mc.player||!entity.isInvisible()||!FeruchemyConfig.TIN_INVISIBLE.get())return 0;double level=Math.max(0,(TinFeruchemy.factor(mc.player,true)-1)/FeruchemyConfig.TIN_TAP_STEP.get());double opacity=TinMath.invisibleClarity(level,FeruchemyConfig.TIN_INVISIBLE_THRESHOLD.get(),FeruchemyConfig.TIN_INVISIBLE_FULL.get(),FeruchemyConfig.TIN_INVISIBLE_OPACITY.get());return opacity>0&&mc.player.distanceToSqr(entity)<=Math.pow(FeruchemyConfig.TIN_INVISIBLE_RANGE.get(),2)&&mc.player.hasLineOfSight(entity)?opacity:0;}
    private FeruchemicalTinClient(){}
}
