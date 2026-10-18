package dev.cosmerearcanists.client;

import dev.cosmerearcanists.equipment.EquipmentProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class EquipmentClient {
    @SubscribeEvent public void ropes(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.level==null || mc.player==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers=mc.renderBuffers().bufferSource();var type=RenderType.lines();var vertices=buffers.getBuffer(type);var pose=event.getPoseStack().last();var camera=event.getCamera().getPosition();
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof EquipmentProjectile tether && tether.kind()!=EquipmentProjectile.SPIKE && tether.getOwner()!=null){
            Vec3 from=tether.getOwner().getPosition(partial).add(0,tether.getOwner().getBbHeight()*.65,0),to=tether.getPosition(partial);
            double slack=tether.latched()?Math.max(0,tether.ropeLength()-from.distanceTo(to)):1;
            for(int i=0;i<16;i++){
                double a=i/16.0,b=(i+1)/16.0;
                Vec3 p=from.lerp(to,a).add(0,-Math.sin(a*Math.PI)*Math.min(2,slack*.2),0).subtract(camera);
                Vec3 q=from.lerp(to,b).add(0,-Math.sin(b*Math.PI)*Math.min(2,slack*.2),0).subtract(camera);var normal=q.subtract(p).normalize();
                vertices.addVertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).setColor(130,102,65,255).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
                vertices.addVertex(pose.pose(),(float)q.x,(float)q.y,(float)q.z).setColor(130,102,65,255).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
            }
        }
        buffers.endBatch(type);
    }
}
