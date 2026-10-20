package dev.cosmerearcanists.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cosmerearcanists.power.BronzeWarningPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Copies the scene before the HUD, then samples that copy for a directional peripheral blur. */
public final class BronzeDangerClient {
    private static ShaderInstance shader;
    private static TextureTarget scene;
    private static BronzeWarningPacket packet;
    private static ClientLevel world;
    private static long expires;

    public static void registerShaders(RegisterShadersEvent event) {
        try { event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("cosmerearcanists", "bronze_danger"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException("Unable to load bronze danger blur", e); }
    }
    public static void receive(BronzeWarningPacket next) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !next.dimension().equals(mc.level.dimension().location().toString())) return;
        world = mc.level; expires = world.getGameTime() + next.lifetime();
        packet = next.warnings().isEmpty() ? null : next;
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator() || mc.level.getGameTime() >= expires) packet = null;
        if ((mc.level != world || mc.level == null) && scene != null) { scene.destroyBuffers(); scene = null; world = mc.level; }
    }
    @SubscribeEvent public void render(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (shader == null || packet == null || mc.level != world || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()
            || !PowerClientConfig.BRONZE_BLUR.get() || mc.options.hideGui || mc.screen != null || mc.getCameraEntity() != mc.player || mc.level.getGameTime() >= expires) return;
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 forward = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        Vec3 right = Vec3.directionFromRotation(0, camera.getYRot() + 90), up = right.cross(forward);
        float left = 0, r = 0, top = 0, bottom = 0;
        for (var warning : packet.warnings()) {
            double x = warning.direction().dot(right), y = warning.direction().dot(up), z = warning.direction().dot(forward);
            float strength = (float)Math.min(1, warning.intensity() * PowerClientConfig.BRONZE_BLUR_STRENGTH.get());
            left = Math.max(left, (float)Math.max(0, -x) * strength); r = Math.max(r, (float)Math.max(0, x) * strength);
            top = Math.max(top, (float)Math.min(1, Math.max(0, y) + Math.max(0, z) * .7) * strength);
            bottom = Math.max(bottom, (float)Math.min(1, Math.max(0, -y) + Math.max(0, -z) * .8) * strength);
        }
        var main = mc.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) return;
        event.getGuiGraphics().flush();
        if (scene == null) scene = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        else if (scene.width != main.width || scene.height != main.height) scene.resize(main.width, main.height, Minecraft.ON_OSX);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        var previousShader = RenderSystem.getShader(); int previousTexture = RenderSystem.getShaderTexture(0);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, scene.width, scene.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(true);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, scene.getColorTextureId());
            shader.setSampler("Sampler0", scene.getColorTextureId());
            shader.safeGetUniform("ScreenSize").set((float)main.width, (float)main.height);
            shader.safeGetUniform("DangerEdges").set(left, r, top, bottom);
            shader.safeGetUniform("EdgeWidth").set(PowerClientConfig.BRONZE_EDGE_WIDTH.get().floatValue());
            shader.safeGetUniform("BlurRadius").set(PowerClientConfig.BRONZE_BLUR_RADIUS.get().floatValue());
            float w = event.getGuiGraphics().guiWidth(), h = event.getGuiGraphics().guiHeight();
            var pose = event.getGuiGraphics().pose().last().pose();
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.addVertex(pose, 0, h, 0).setUv(0, 0); vertices.addVertex(pose, w, h, 0).setUv(1, 0);
            vertices.addVertex(pose, w, 0, 0).setUv(1, 1); vertices.addVertex(pose, 0, 0, 0).setUv(0, 1);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            main.bindWrite(true); RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
}
