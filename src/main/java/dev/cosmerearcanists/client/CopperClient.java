package dev.cosmerearcanists.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cosmerearcanists.copper.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import static dev.cosmerearcanists.copper.CopperNetwork.*;

public final class CopperClient {
    public static Snapshot current;
    private static Waypoint waypoint;
    private static Capture capture;
    private record Capture(UUID session,String title,String text){}
    private static byte[] upload;private static int uploadOffset;private static UUID uploadSession,uploadId;
    private static UUID imageId;private static int imageTotal;private static ByteArrayOutputStream imageBytes;
    public static ResourceLocation texture;public static int imageWidth,imageHeight;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    public static void send(int action,UUID entry,String title,String text,int number,byte[] bytes){if(current!=null)PacketDistributor.sendToServer(new Request(action,current.session(),entry,title,text,number,bytes));}
    public static void snapshot(Snapshot packet){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        boolean changed=current==null||!current.book().equals(packet.book());current=packet;
        if(!packet.message().isEmpty())mc.player.displayClientMessage(Component.literal(packet.message()),false);
        if(!packet.open()){capture=null;upload=null;clearImage();if(mc.screen instanceof CopperScreen)mc.setScreen(null);return;}
        if(changed)clearImage();
        if(mc.screen instanceof CopperScreen screen)screen.update(packet,changed);else mc.setScreen(new CopperScreen(packet));
    }
    public static void waypoint(Waypoint packet){waypoint=packet.enabled()?packet:null;}
    public static void requestImage(UUID id){clearImage();imageId=id;send(IMAGE,id,"","",0,new byte[0]);}
    public static void image(Image packet){
        if(current==null||!packet.session().equals(current.session())||!packet.entry().equals(imageId))return;
        if(packet.offset()==0){imageBytes=new ByteArrayOutputStream();imageTotal=packet.total();}
        if(imageBytes==null||imageTotal!=packet.total()||packet.offset()!=imageBytes.size())return;
        imageBytes.writeBytes(packet.bytes());if(imageBytes.size()!=imageTotal)return;
        try{byte[] png=imageBytes.toByteArray();CopperImages.checkHeader(png);var decoded=NativeImage.read(png);if(decoded.getWidth()>CopperImages.WIDTH||decoded.getHeight()>CopperImages.HEIGHT){decoded.close();throw new IllegalArgumentException("Image dimensions exceed the limit");}imageWidth=decoded.getWidth();imageHeight=decoded.getHeight();texture=Minecraft.getInstance().getTextureManager().register("coppermind_memory",new DynamicTexture(decoded));}catch(Exception e){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal("Could not display this screenshot."),false);}finally{imageBytes=null;}
    }
    public static void capture(String title,String text){if(current==null||!current.editable())return;capture=new Capture(current.session(),title,text);Minecraft.getInstance().setScreen(null);}
    public static void clearImage(){if(texture!=null)Minecraft.getInstance().getTextureManager().release(texture);texture=null;imageBytes=null;imageId=null;imageTotal=0;}
    @SubscribeEvent public void render(RenderLevelStageEvent event){
        if(capture==null||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var request=capture;capture=null;var mc=Minecraft.getInstance();
        try(var source=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            double scale=Math.min(1,Math.min((double)CopperImages.WIDTH/source.getWidth(),(double)CopperImages.HEIGHT/source.getHeight()));int w=Math.max(1,(int)(source.getWidth()*scale)),h=Math.max(1,(int)(source.getHeight()*scale));
            try(var resized=new NativeImage(w,h,false)){for(int y=0;y<h;y++)for(int x=0;x<w;x++)resized.setPixelRGBA(x,y,source.getPixelRGBA(Math.min(source.getWidth()-1,(int)(x/scale)),Math.min(source.getHeight()-1,(int)(y/scale))));upload=resized.asByteArray();}
            if(upload.length>CopperImages.HARD_BYTES)throw new IllegalArgumentException("Screenshot too large");uploadOffset=0;uploadSession=request.session();uploadId=UUID.randomUUID();PacketDistributor.sendToServer(new Request(BEGIN,uploadSession,uploadId,request.title(),request.text(),upload.length,new byte[0]));
        }catch(Exception e){upload=null;if(mc.player!=null)mc.player.displayClientMessage(Component.literal("Screenshot capture failed."),false);if(current!=null)mc.setScreen(new CopperScreen(current));}
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(level!=mc.level){level=mc.level;current=null;waypoint=null;capture=null;upload=null;clearImage();}
        if(mc.player==null||upload==null)return;
        int end=Math.min(upload.length,uploadOffset+CopperImages.CHUNK);PacketDistributor.sendToServer(new Request(CHUNK,uploadSession,uploadId,"","",uploadOffset,Arrays.copyOfRange(upload,uploadOffset,end)));uploadOffset=end;
        if(uploadOffset==upload.length){PacketDistributor.sendToServer(new Request(FINISH,uploadSession,uploadId,"","",0,new byte[0]));upload=null;}
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(waypoint==null||mc.player==null||mc.level==null||mc.options.hideGui)return;
        String text;
        if(!mc.level.dimension().location().toString().equals(waypoint.dimension()))text=waypoint.title()+" · "+waypoint.dimension();
        else{double dx=waypoint.position().getX()+.5-mc.player.getX(),dz=waypoint.position().getZ()+.5-mc.player.getZ();double angle=net.minecraft.util.Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(-dx,dz))-mc.player.getYRot()));String arrow=Math.abs(angle)<30?"↑":Math.abs(angle)>150?"↓":angle>0?"→":"←";long distance=Math.round(mc.player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(waypoint.position())));text=arrow+" "+waypoint.title()+" · "+distance+" m";}
        int x=(mc.getWindow().getGuiScaledWidth()-mc.font.width(text))/2;event.getGuiGraphics().drawString(mc.font,text,x,12,0xFFD69A);
    }
}
