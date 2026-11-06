package dev.cosmerearcanists.client;

import dev.cosmerearcanists.aondor.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Transient, depth-tested luminous ink; local ink updates every frame, remote ink four times/second. */
public final class AonProjectionClient {
    private record View(String dimension,AonDrawing.Ink glyph,long tick){}
    private static final Map<UUID,View> VIEWS=new HashMap<>();
    public static void receive(AonDorNetwork.Projection packet){
        var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().toString().equals(packet.dimension()))return;
        if(!packet.active()){VIEWS.remove(packet.owner());return;}
        VIEWS.put(packet.owner(),new View(packet.dimension(),packet.glyph(),mc.level.getGameTime()));
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(mc.level==null){VIEWS.clear();return;}
        long now=mc.level.getGameTime();VIEWS.entrySet().removeIf(e->!e.getValue().dimension().equals(mc.level.dimension().location().toString())||now<e.getValue().tick()||now-e.getValue().tick()>30);}
    @SubscribeEvent public void render(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||mc.level==null||mc.player==null)return;
        var views=new HashMap<>(VIEWS);
        // Avoid latency for the artist, and remove own stale projection immediately after closing.
        views.remove(mc.player.getUUID());
        if(mc.screen instanceof AonDorScreen screen&&mc.player.isAlive())views.put(mc.player.getUUID(),new View(screen.dimension(),screen.projection(),mc.level.getGameTime()));
        if(views.isEmpty())return;
        var buffers=mc.renderBuffers().bufferSource();var type=AonProjectionRenderType.ink();var vertices=buffers.getBuffer(type);
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(var entry:views.entrySet()){
            var player=mc.level.getPlayerByUUID(entry.getKey());if(player==null||!player.isAlive()||player.isSpectator())continue;
            var forward=player.getViewVector(partial);var right=new Vec3(-forward.z,0,forward.x).normalize();if(right.lengthSqr()<.5)continue;
            var up=right.cross(forward).normalize();var center=player.getEyePosition(partial).add(forward.scale(1.5));
            double size=Math.max(.3,Math.min(1.4,entry.getValue().glyph().size()*1.7));
            // Render broad faint layers first and a narrow bright core last. No depth writes or world lighting.
            double[] widths={.026,.017,.010,.005,.002};float[] alpha={.035f,.065f,.12f,.38f,.98f};
            for(int layer=0;layer<widths.length;layer++)for(var stroke:entry.getValue().glyph().strokes())for(int i=1;i<stroke.size();i++){
                var a=stroke.get(i-1);var b=stroke.get(i);double dx=b.x()-a.x(),dy=b.y()-a.y(),length=Math.hypot(dx,dy);if(length<1e-8)continue;
                var start=center.add(right.scale((a.x()-.5)*size)).add(up.scale((.5-a.y())*size)).subtract(camera);
                var end=center.add(right.scale((b.x()-.5)*size)).add(up.scale((.5-b.y())*size)).subtract(camera);
                var normal=right.scale(dy/length*widths[layer]).add(up.scale(dx/length*widths[layer]));
                for(var point:List.of(start.add(normal),end.add(normal),end.subtract(normal),start.subtract(normal)))vertices.addVertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z).setColor(1f,1f,1f,alpha[layer]);
            }
        }
        buffers.endBatch(type);
    }
}
