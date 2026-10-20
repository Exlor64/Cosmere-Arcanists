package dev.cosmerearcanists.client;
import java.util.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.TinMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
/** Movement impressions use displacement of tracked entities, never guessed AI intention. */
public final class TinTouchClient {
    private static final Map<Integer,Vec3> LAST=new HashMap<>();
    private record Motion(int entity,double speed){}
    private static final List<Motion> MOVING=new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel dimension;
    @SubscribeEvent public void tick(ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(mc.level!=dimension){dimension=mc.level;LAST.clear();}MOVING.clear();if(mc.player==null||mc.level==null||!mc.player.isAlive()||mc.player.isSpectator()||!FeruchemyConfig.TIN_TOUCH_MOTION.get()||TinFeruchemy.touch(mc.player)<=1){LAST.clear();return;}double range=range();var current=new HashMap<Integer,Vec3>();int scanned=0;
        for(var entity:mc.level.entitiesForRendering()){if(entity==mc.player||entity.isRemoved()||entity.distanceToSqr(mc.player)>range*range)continue;if(++scanned>256)break;var position=entity.position();var old=LAST.get(entity.getId());current.put(entity.getId(),position);if(old!=null){double speed=position.distanceTo(old);if(speed>.005)MOVING.add(new Motion(entity.getId(),speed));}}
        LAST.clear();LAST.putAll(current);MOVING.sort(Comparator.comparingDouble(m->mc.level.getEntity(m.entity()).distanceToSqr(mc.player)));if(MOVING.size()>64)MOVING.subList(64,MOVING.size()).clear();
    }
    private static double range(){var p=Minecraft.getInstance().player;return Math.min(FeruchemyConfig.TIN_TOUCH_RANGE_CAP.get(),FeruchemyConfig.TIN_TOUCH_RANGE.get()*Math.sqrt(TinFeruchemy.touch(p)));}
    @SubscribeEvent public void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||TinFeruchemy.touch(mc.player)<=1||!FeruchemyConfig.TIN_TOUCH_MOTION.get())return;var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();var type=TinRenderTypes.blur();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);boolean depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        try{for(var motion:MOVING){var entity=mc.level.getEntity(motion.entity());if(entity==null||entity.distanceToSqr(mc.player)>range()*range()||!FeruchemyConfig.TIN_TOUCH_WALLS.get()&&!mc.player.hasLineOfSight(entity))continue;double alpha=TinMath.movementBlur(motion.speed(),TinFeruchemy.touchLevels(mc.player),FeruchemyConfig.TIN_TOUCH_SPEED_RESPONSE.get(),FeruchemyConfig.TIN_TOUCH_POWER_RESPONSE.get(),FeruchemyConfig.TIN_TOUCH_OPACITY.get());var center=entity.getPosition(partial).add(0,entity.getBbHeight()*.5,0);if(center.distanceToSqr(camera)<.25)continue;float radius=(float)Math.min(1.5,.35+Math.sqrt(motion.speed()));pose.pushPose();try{pose.translate(center.x-camera.x,center.y-camera.y,center.z-camera.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());var v=buffers.getBuffer(type);v.addVertex(pose.last().pose(),-radius,-radius,0).setUv(0,1).setColor(1f,1f,1f,(float)alpha);v.addVertex(pose.last().pose(),radius,-radius,0).setUv(1,1).setColor(1f,1f,1f,(float)alpha);v.addVertex(pose.last().pose(),radius,radius,0).setUv(1,0).setColor(1f,1f,1f,(float)alpha);v.addVertex(pose.last().pose(),-radius,radius,0).setUv(0,0).setColor(1f,1f,1f,(float)alpha);}finally{pose.popPose();}}
            if(FeruchemyConfig.TIN_TOUCH_WALLS.get())RenderSystem.disableDepthTest();buffers.endBatch(type);
        }finally{if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();}
    }
}
