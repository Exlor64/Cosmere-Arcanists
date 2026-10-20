package dev.cosmerearcanists.client;

import dev.cosmerearcanists.power.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;

/** A detached display model. Never equips or changes the real player, and is never added to the world. */
public final class GoldClient {
    private static GoldPacket vision;
    private static RemotePlayer model;
    private static long received;
    public static void receive(GoldPacket p){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!p.dimension().equals(mc.level.dimension().location().toString()))return;vision=p;received=mc.level.getGameTime();if(p.clarity()<=0){model=null;return;}if(model==null||model.level()!=mc.level)model=new RemotePlayer(mc.level,mc.player.getGameProfile());
        for(int i=0;i<GoldService.SLOTS.size();i++){var id=ResourceLocation.tryParse(p.equipment().get(i));model.setItemSlot(GoldService.SLOTS.get(i),id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(id)));}model.setPos(p.position());model.setYRot(p.yaw());model.yRotO=p.yaw();model.yBodyRot=model.yBodyRotO=p.yaw();model.yHeadRot=model.yHeadRotO=p.yaw();}
    private static boolean active(){var mc=Minecraft.getInstance();return vision!=null&&vision.clarity()>0&&model!=null&&mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&mc.level==model.level()&&vision.dimension().equals(mc.level.dimension().location().toString())&&mc.level.getGameTime()-received<vision.lifetime();}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){if(!active()){vision=null;model=null;}}
    @SubscribeEvent public void render(RenderLevelStageEvent e){if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!active()||!PowerClientConfig.GOLD_SHADOW.get())return;var mc=Minecraft.getInstance();var pose=e.getPoseStack();var camera=e.getCamera().getPosition();var pos=vision.position();var buffers=mc.renderBuffers().bufferSource();float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);float fade=(float)Math.clamp((vision.lifetime()-(mc.level.getGameTime()-received+partial))/4,0,1);float alpha=(float)(vision.clarity()*PowerClientConfig.GOLD_OPACITY.get()*fade);
        if(pos.distanceToSqr(mc.player.position())<.5)return;
        pose.pushPose();try{pose.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);var type=TemporalRenderTypes.ghost();MultiBufferSource ghost=original->original.format().equals(DefaultVertexFormat.NEW_ENTITY)?buffers.getBuffer(type):TemporalClient.DISCARD;
            try{RenderSystem.setShaderColor(1,.76f,.25f,alpha);mc.getEntityRenderDispatcher().getRenderer(model).render(model,vision.yaw(),partial,pose,ghost,LightTexture.FULL_BRIGHT);buffers.endBatch(type);}finally{RenderSystem.setShaderColor(1,1,1,1);}
            if(PowerClientConfig.GOLD_DETAILS.get()&&!mc.options.hideGui&&mc.screen==null){pose.translate(0,2.15,0);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-.018f,-.018f,.018f);int y=0;for(String text:vision.details()){mc.font.drawInBatch(text,-mc.font.width(text)/2f,y,0xFFFFD782,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0x900C1524,LightTexture.FULL_BRIGHT);y+=10;}buffers.endBatch();}
        }finally{pose.popPose();}
    }
}
