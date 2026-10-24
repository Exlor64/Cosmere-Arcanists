package dev.cosmerearcanists.client;

import com.mojang.math.Axis;
import dev.cosmerearcanists.power.ZincReactionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;

/** Clear directional ticks and thin, depth-tested projectile estimates. */
public final class ZincReactionClient {
    private static ClientLevel world;
    private static ZincReactionPacket packet;
    private static long expires;
    public static void receive(ZincReactionPacket next){var mc=Minecraft.getInstance();if(mc.level==null||!next.dimension().equals(mc.level.dimension().location().toString()))return;world=mc.level;expires=world.getGameTime()+next.lifetime();packet=next.threats().isEmpty()?null:next;}
    private static boolean active(){var mc=Minecraft.getInstance();return packet!=null&&mc.level==world&&mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&mc.level.getGameTime()<expires;}
    private static boolean visible(){var mc=Minecraft.getInstance();return active()&&!mc.options.hideGui&&mc.screen==null&&mc.getCameraEntity()==mc.player;}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){if(!active())packet=null;}
    @SubscribeEvent public void hud(RenderGuiEvent.Post e){
        if(!visible()||!PowerClientConfig.ZINC_CUES.get())return;var mc=Minecraft.getInstance();var g=e.getGuiGraphics();var camera=mc.gameRenderer.getMainCamera();Vec3 forward=Vec3.directionFromRotation(camera.getXRot(),camera.getYRot()),right=Vec3.directionFromRotation(0,camera.getYRot()+90),up=right.cross(forward);
        float cx=g.guiWidth()/2f,cy=g.guiHeight()/2f,radius=Math.min(g.guiWidth(),g.guiHeight())*.18f;int color=((int)(255*PowerClientConfig.ZINC_OPACITY.get())<<24)|0xFFD66B;
        for(var t:packet.threats()){double x=t.direction().dot(right),y=-t.direction().dot(up),z=t.direction().dot(forward);if(x*x+y*y<.01)y=z>=0?-1:1;double angle=Math.atan2(y,x);var pose=g.pose();pose.pushPose();pose.translate(cx+Math.cos(angle)*radius,cy+Math.sin(angle)*radius,0);pose.mulPose(Axis.ZP.rotation((float)angle));g.fill(-3,-1,4,2,color);g.fill(1,-3,3,4,color);pose.popPose();}
    }
    @SubscribeEvent public void paths(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!visible()||!PowerClientConfig.ZINC_PATHS.get())return;var mc=Minecraft.getInstance();var pose=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();float alpha=PowerClientConfig.ZINC_OPACITY.get().floatValue();
        for(var threat:packet.threats())for(int i=1;i<threat.path().size();i++){var a=threat.path().get(i-1).subtract(camera);var b=threat.path().get(i).subtract(camera);var delta=b.subtract(a);if(delta.lengthSqr()<1e-8)continue;var normal=delta.normalize();var vertices=buffers.getBuffer(RenderType.lines());vertices.addVertex(pose.last().pose(),(float)a.x,(float)a.y,(float)a.z).setColor(1f,.84f,.42f,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);vertices.addVertex(pose.last().pose(),(float)b.x,(float)b.y,(float)b.z).setColor(1f,.84f,.42f,alpha).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);}
        buffers.endBatch(RenderType.lines());
    }
}
