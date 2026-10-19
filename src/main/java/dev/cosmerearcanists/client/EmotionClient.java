package dev.cosmerearcanists.client;

import dev.cosmerearcanists.power.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class EmotionClient {
    private static EmotionPacket status=new EmotionPacket("",0,0,-1,0);private static long received;
    private static int zinc,brass;
    public static void receive(EmotionPacket p){var mc=Minecraft.getInstance();if(mc.level==null||!p.dimension().equals(mc.level.dimension().location().toString()))return;status=p;received=mc.level.getGameTime();zinc=p.zincMode();brass=p.brassMode();}
    public static int cycle(String power,int direction){if(power.equals("a:zinc")){zinc=Math.floorMod(zinc+direction,5);return zinc;}brass=Math.floorMod(brass+direction,5);return brass;}
    public static String modeLabel(String power){return Emotion.power(power)?Emotion.values()[power.equals("a:zinc")?zinc:brass].label():"";}
    private static boolean active(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&mc.player.isAlive()&&status.dimension().equals(mc.level.dimension().location().toString())&&mc.level.getGameTime()-received<20;}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){if(!active()){status=new EmotionPacket("",0,0,-1,0);zinc=brass=0;}}
    @SubscribeEvent public void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(!active()||!PowerClientConfig.EMOTION_HUD.get()||mc.options.hideGui||mc.screen!=null)return;
        if(status.dominant()>=0)e.getGuiGraphics().drawCenteredString(mc.font,"Dominant emotion: "+Emotion.values()[status.dominant()].label(),e.getGuiGraphics().guiWidth()/2,e.getGuiGraphics().guiHeight()/2+89,0xEDB4B4);
    }
    @SubscribeEvent public void target(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active() || !PowerClientConfig.EMOTION_TARGET.get() || mc.options.hideGui || mc.screen!=null)return;
        float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers=mc.renderBuffers().bufferSource();
        for(var target:status.targets()){
        var entity=mc.level.getEntity(target.entity());if(!(entity instanceof LivingEntity) || !entity.isAlive() || entity==mc.player || !mc.player.hasLineOfSight(entity))continue;
        String text=target.emotion()<0?"Neutral":Emotion.values()[target.emotion()].label();
        int color=target.emotion()<0?0xFFD2DAE3:switch(Emotion.values()[target.emotion()]){case ANGER->0xFFFF8A80;case FEAR->0xFFD4AAFF;case TRUST->0xFF91DDFF;case LOVE->0xFFFFB4DA;case COMPLIANCE->0xFFFFDE91;};
        var pose=e.getPoseStack();var position=entity.getPosition(partial);var camera=e.getCamera().getPosition();pose.pushPose();
        try{pose.translate(position.x-camera.x,position.y+entity.getBbHeight()+(entity.hasCustomName()?.8:.4)-camera.y,position.z-camera.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-.025f,-.025f,.025f);
            mc.font.drawInBatch(text,-mc.font.width(text)/2f,0,color,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0x900C1524,LightTexture.FULL_BRIGHT);
        }finally{pose.popPose();}
        }
        buffers.endBatch();
    }
}
