package dev.cosmerearcanists.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.cosmerearcanists.power.ScoutPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;

/** Camera-only projection: the actual player remains the server's interaction and damage target. */
public final class ScoutClient {
    private static ScoutPacket state=new ScoutPacket("",false,net.minecraft.world.phys.Vec3.ZERO,0);
    private static long received;
    private static ArmorStand camera;
    public static boolean active(){var mc=Minecraft.getInstance();return state.active() && mc.player!=null && mc.level!=null && mc.player.isAlive() && !mc.player.isSpectator() && mc.screen==null && state.dimension().equals(mc.level.dimension().location().toString()) && mc.level.getGameTime()-received<10;}
    private static void clear(){var mc=Minecraft.getInstance();if(camera!=null && mc.getCameraEntity()==camera)mc.setCameraEntity(mc.player);camera=null;state=new ScoutPacket("",false,net.minecraft.world.phys.Vec3.ZERO,0);}
    public static void receive(ScoutPacket p){var mc=Minecraft.getInstance();if(mc.level==null || !p.dimension().equals(mc.level.dimension().location().toString()))return;if(!p.active()){clear();return;}state=p;received=mc.level.getGameTime();}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(!active()){clear();return;}
        boolean fresh=camera==null || camera.level()!=mc.level;
        if(fresh){camera=new ArmorStand(mc.level,0,0,0);camera.setInvisible(true);}
        double x=state.position().x,y=state.position().y+mc.player.getEyeHeight()-camera.getEyeHeight(),z=state.position().z;
        camera.xo=fresh?x:camera.getX();camera.yo=fresh?y:camera.getY();camera.zo=fresh?z:camera.getZ();
        camera.setPos(x,y,z);camera.setYRot(mc.player.getYRot());camera.setXRot(mc.player.getXRot());camera.yRotO=mc.player.yRotO;camera.xRotO=mc.player.xRotO;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(mc.getCameraEntity()!=camera)mc.setCameraEntity(camera);
    }
    @SubscribeEvent public void interaction(InputEvent.InteractionKeyMappingTriggered e){if(active())e.setCanceled(true);}
    @SubscribeEvent public void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(!active() || e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.options.getCameraType().isFirstPerson())return;
        var pose=e.getPoseStack();var from=e.getCamera().getPosition();var at=state.position();var buffers=mc.renderBuffers().bufferSource();var type=TemporalRenderTypes.ghost();
        pose.pushPose();pose.translate(at.x-from.x,at.y-from.y,at.z-from.z);
        try{RenderSystem.setShaderColor(1,.78f,.35f,.25f);net.minecraft.client.renderer.MultiBufferSource ghost=original->original.format().equals(com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY)?buffers.getBuffer(type):TemporalClient.DISCARD;
            mc.getEntityRenderDispatcher().getRenderer(mc.player).render(mc.player,mc.player.getYRot(),e.getPartialTick().getGameTimeDeltaPartialTick(false),pose,ghost,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);buffers.endBatch(type);
        }finally{RenderSystem.setShaderColor(1,1,1,1);pose.popPose();}
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post e){if(!active())return;var mc=Minecraft.getInstance();if(mc.options.hideGui)return;e.getGuiGraphics().drawCenteredString(mc.font,String.format(java.util.Locale.ROOT,"Electrum shadow · %.1f / %.1f blocks · release Alt to return",state.position().distanceTo(mc.player.position()),state.radius()),e.getGuiGraphics().guiWidth()/2,e.getGuiGraphics().guiHeight()-64,0xFFFFD47A);}
}
