package dev.cosmerearcanists.client;

import dev.cosmerearcanists.mist.MistMath;
import dev.cosmerearcanists.mist.MistPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class MistClient {
    private static ClientLevel world;
    private static long received;
    private static float target, density, previous, distance = 24;
    public static void receive(MistPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !packet.dimension().equals(level.dimension().location().toString())) return;
        if (world != level) { density = previous = 0; world = level; }
        target = packet.density(); distance = packet.distance(); received = level.getGameTime();
    }
    private static boolean activeWorld() {
        var level = Minecraft.getInstance().level;
        return level != null && level == world;
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        if (!activeWorld()) { world = null; density = previous = target = 0; return; }
        previous = density;
        long age = world.getGameTime() - received;
        float desired = age >= 0 && age < 40 ? target : 0;
        density += (desired - density) * .15f;
        if (Math.abs(density - desired) < .0001f) density = desired;
    }
    private static float amount(double partial) {
        return activeWorld() ? previous + (density - previous) * (float)Math.max(0, Math.min(1, partial)) : 0;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void fog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float mist = amount(event.getPartialTick());
        if (mist <= .0001f) return;
        float far = MistMath.farPlane(event.getFarPlaneDistance(), distance, mist);
        if (far >= event.getFarPlaneDistance()) return;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * .15f));
        event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void color(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        var player = Minecraft.getInstance().player;
        if (player == null || player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
            || player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) return;
        float blend = amount(event.getPartialTick()) * .65f;
        event.setRed(event.getRed() + (.35f - event.getRed()) * blend);
        event.setGreen(event.getGreen() + (.39f - event.getGreen()) * blend);
        event.setBlue(event.getBlue() + (.44f - event.getBlue()) * blend);
    }
}
