package dev.cosmerearcanists.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cosmerearcanists.power.PowerNetwork;
import dev.cosmerearcanists.power.TinMath;
import dev.cosmerearcanists.power.TinConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;

public final class TinClient {
    private static PowerNetwork.TinStatus status=new PowerNetwork.TinStatus("",0,1);
    private record Echo(PowerNetwork.TinEcho packet,long started) { }
    private static final List<Echo> ECHOES=new ArrayList<>();
    public static void status(PowerNetwork.TinStatus packet){status=packet;if(TinMath.reveal(packet.strength(),TinConfig.REVEAL_THRESHOLD.get(),TinConfig.FULL_REVEAL.get())<=0)ECHOES.clear();}
    public static void echo(PowerNetwork.TinEcho packet){
        var mc=Minecraft.getInstance();if(mc.level==null || !PowerClientConfig.TIN_ECHOES.get() || !packet.dimension().equals(mc.level.dimension().location().toString()))return;
        // New sound updates replace the same entity's old echo, never leave a continuous tracking trail.
        if(packet.entityId()>=0)ECHOES.removeIf(e->e.packet.entityId()==packet.entityId());
        ECHOES.add(new Echo(packet,mc.level.getGameTime()));
        while(ECHOES.size()>PowerClientConfig.TIN_ECHO_LIMIT.get())ECHOES.removeFirst();
    }
    private static boolean active(){var mc=Minecraft.getInstance();return mc.level!=null && mc.player!=null && status.strength()>0 && status.dimension().equals(mc.level.dimension().location().toString());}
    private static boolean wireActive(){return active() && TinMath.reveal(status.strength(),TinConfig.REVEAL_THRESHOLD.get(),TinConfig.FULL_REVEAL.get())>0;}
    @SubscribeEvent public void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!active()){ECHOES.clear();if(mc.level==null)status=new PowerNetwork.TinStatus("",0,1);return;}
        if(!wireActive())ECHOES.clear();
        ECHOES.removeIf(e->!e.packet.dimension().equals(status.dimension()) || mc.level.getGameTime()-e.started>=e.packet.ticks());
    }
    @SubscribeEvent public void fov(ViewportEvent.ComputeFov event){if(active() && PowerClientConfig.TIN_ZOOM.get() && event.usedConfiguredFov())event.setFOV(Math.max(5,event.getFOV()/Math.max(1,status.zoom())));}
    private static final VertexConsumer DISCARD=new VertexConsumer() {
        public VertexConsumer addVertex(float x,float y,float z){return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    };
    /** NO_DEPTH_TEST does not guarantee that a previously enabled world depth test is disabled.
     * Flush this pass explicitly without depth testing, then restore the caller's state. */
    private static void flushThroughWalls(MultiBufferSource.BufferSource buffers,RenderType type){
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try{RenderSystem.disableDepthTest();buffers.endBatch(type);}
        finally{if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();}
    }
    private static void sourceBox(com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource.BufferSource buffers,PowerNetwork.TinEcho packet,Vec3 camera,float alpha){
        var lines=TinRenderTypes.lines();LevelRenderer.renderLineBox(pose,buffers.getBuffer(lines),packet.sourceBox().move(camera.scale(-1)),.65f,.8f,.85f,alpha);flushThroughWalls(buffers,lines);
    }
    @SubscribeEvent public void render(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !wireActive() || !PowerClientConfig.TIN_ECHOES.get() || ECHOES.isEmpty())return;
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(var echo:ECHOES) {
            float fade=(float)Math.clamp(1-(mc.level.getGameTime()-echo.started+partial)/echo.packet.ticks(),0,1);
            float alpha=(float)(fade*echo.packet.audible()*PowerClientConfig.TIN_OPACITY.get());if(alpha<.01)continue;
            var lines=TinRenderTypes.lines();var vertices=buffers.getBuffer(lines);
            for(var box:echo.packet.surfaces())LevelRenderer.renderLineBox(pose,vertices,box.move(camera.scale(-1)),.55f,.72f,.78f,alpha);
            var entity=echo.packet.entityId()<0?null:mc.level.getEntity(echo.packet.entityId());
            if(echo.packet.entityId()>=0 && (entity==null || !PowerClientConfig.TIN_MODELS.get()))LevelRenderer.renderLineBox(pose,vertices,echo.packet.sourceBox().move(camera.scale(-1)),.65f,.8f,.85f,alpha);
            flushThroughWalls(buffers,lines);
            if(entity==null || entity==mc.player || !PowerClientConfig.TIN_MODELS.get())continue;
            var type=TinRenderTypes.models();
            boolean[] modelBuffer={false};
            MultiBufferSource wire=original->{if(original.format().equals(DefaultVertexFormat.NEW_ENTITY)){modelBuffer[0]=true;return buffers.getBuffer(type);}return DISCARD;};
            pose.pushPose();
            var box=echo.packet.sourceBox();pose.translate((box.minX+box.maxX)*.5-camera.x,box.minY-camera.y,(box.minZ+box.maxZ)*.5-camera.z);
            try {
                RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_LINE);
                RenderSystem.setShaderColor(.55f,.72f,.78f,alpha);
                mc.getEntityRenderDispatcher().getRenderer(entity).render(entity,entity.getYRot(),partial,pose,wire,LightTexture.FULL_BRIGHT);
                flushThroughWalls(buffers,type);
            } finally {
                RenderSystem.setShaderColor(1,1,1,1);RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_FILL);pose.popPose();
            }
            if(!modelBuffer[0])sourceBox(pose,buffers,echo.packet,camera,alpha);
        }
    }
}
