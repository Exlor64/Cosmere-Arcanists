package dev.cosmerearcanists.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cosmerearcanists.power.TemporalPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class TemporalClient {
    private static TemporalPacket forecast=new TemporalPacket("",1,java.util.List.of());
    private static long received;
    public static void receive(TemporalPacket packet){var mc=Minecraft.getInstance();if(mc.level==null || !packet.dimension().equals(mc.level.dimension().location().toString()))return;forecast=packet;received=mc.level.getGameTime();}
    private static boolean active(){var mc=Minecraft.getInstance();return mc.level!=null && mc.player!=null && mc.player.isAlive() && !mc.player.isSpectator() && forecast.dimension().equals(mc.level.dimension().location().toString()) && mc.level.getGameTime()-received<forecast.lifetime();}
    @SubscribeEvent public void tick(ClientTickEvent.Post event){if(!active())forecast=new TemporalPacket("",1,java.util.List.of());}
    private static int warning(TemporalPacket.Shadow shadow){int flags=0;for(var p:shadow.points())flags|=p.warning();return flags&15;}
    private static final VertexConsumer DISCARD=new VertexConsumer(){
        public VertexConsumer addVertex(float x,float y,float z){return this;}public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}public VertexConsumer setNormal(float x,float y,float z){return this;}
    };
    @SubscribeEvent public void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active() || !PowerClientConfig.FUTURES.get())return;
        var mc=Minecraft.getInstance();var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float fade=(float)Math.clamp((forecast.lifetime()-(mc.level.getGameTime()-received+partial))/3,0,1);
        for(var shadow:forecast.shadows()) {
            var entity=mc.level.getEntity(shadow.entity());boolean own=shadow.kind()==TemporalPacket.SELF || shadow.kind()==TemporalPacket.JUMP;
            float r=own?1:.65f,g=own?.78f:.8f,b=own?.35f:1;
            if(shadow.kind()==TemporalPacket.BRANCH){r=.8f;g=.65f;b=1;}
            var previous=entity==null?shadow.points().getFirst().position():entity.position();
            for(int i=0;i<shadow.points().size();i++) {
                var point=shadow.points().get(i);boolean danger=(point.warning()&15)!=0;
                float alpha=(float)(PowerClientConfig.FUTURE_OPACITY.get()*fade*(.4+.6*(i+1)/shadow.points().size()));
                float red=danger?1:r,green=danger?.25f:g,blue=danger?.2f:b;
                var vertices=buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines());
                var feet=point.position();var box=new AABB(feet.x-shadow.width()/2,feet.y,feet.z-shadow.width()/2,feet.x+shadow.width()/2,feet.y+shadow.height(),feet.z+shadow.width()/2);
                // Two thin rails make the projectile's predicted corridor readable without an opaque beam.
                Vec3 mid=feet.add(0,shadow.height()*.5,0),from=previous.add(0,shadow.height()*.5,0);
                Vec3 a=from.subtract(camera),c=mid.subtract(camera),delta=c.subtract(a);if(delta.lengthSqr()>1e-8) {
                    var normal=delta.normalize();vertices.addVertex(pose.last().pose(),(float)a.x,(float)a.y,(float)a.z).setColor(red,green,blue,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
                    vertices.addVertex(pose.last().pose(),(float)c.x,(float)c.y,(float)c.z).setColor(red,green,blue,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
                }
                LevelRenderer.renderLineBox(pose,vertices,box.move(camera.scale(-1)),red,green,blue,alpha);buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
                previous=feet;
                if(entity==null || !PowerClientConfig.FUTURE_MODELS.get() || shadow.kind()==TemporalPacket.PROJECTILE || i!=shadow.points().size()-1 && i!=shadow.points().size()/2)continue;
                var type=TemporalRenderTypes.ghost();MultiBufferSource ghost=original->original.format().equals(DefaultVertexFormat.NEW_ENTITY)?buffers.getBuffer(type):DISCARD;
                pose.pushPose();pose.translate(feet.x-camera.x,feet.y-camera.y,feet.z-camera.z);
                try {
                    RenderSystem.setShaderColor(red,green,blue,alpha*.45f);
                    mc.getEntityRenderDispatcher().getRenderer(entity).render(entity,entity.getYRot(),partial,pose,ghost,LightTexture.FULL_BRIGHT);
                    buffers.endBatch(type);
                } finally {RenderSystem.setShaderColor(1,1,1,1);pose.popPose();}
            }
        }
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();if(!active() || !PowerClientConfig.FUTURE_WARNINGS.get() || mc.options.hideGui || mc.screen!=null)return;
        int row=0;
        for(var shadow:forecast.shadows()) {
            int flags=warning(shadow);if(shadow.kind()!=TemporalPacket.SELF && shadow.kind()!=TemporalPacket.JUMP)flags&=TemporalPacket.IMPACT|TemporalPacket.ATTACK;if(flags==0 || shadow.kind()==TemporalPacket.JUMP)continue;
            boolean own=shadow.kind()==TemporalPacket.SELF;
            String message;
            if(own)message="Electrum: "+((flags&TemporalPacket.FALL)!=0?"unsafe landing":(flags&TemporalPacket.HAZARD)!=0?"hazard ahead":(flags&TemporalPacket.IMPACT)!=0?"likely projectile hit":"possible attack");
            else {
                var source=mc.level.getEntity(shadow.entity());Vec3 offset=(source==null?shadow.points().getFirst().position():source.position()).subtract(mc.player.position());
                double relative=Math.toDegrees(Math.atan2(-offset.x,offset.z))-mc.player.getYRot();relative=net.minecraft.util.Mth.wrapDegrees((float)relative);
                String side=Math.abs(relative)>135?"behind":Math.abs(relative)<45?"ahead":relative>0?"right":"left";
                message="Atium: "+((flags&TemporalPacket.IMPACT)!=0?"incoming projectile":"possible attack")+" ("+side+")";
            }
            event.getGuiGraphics().drawCenteredString(mc.font,message,event.getGuiGraphics().guiWidth()/2,event.getGuiGraphics().guiHeight()/2+26+row*11,0xFFFFAA88);
            if(++row>=3)break;
        }
    }
}
