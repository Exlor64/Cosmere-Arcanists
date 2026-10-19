package dev.cosmerearcanists.client;

import dev.cosmerearcanists.power.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class EmotionClient {
    private static EmotionPacket status=new EmotionPacket("",0,0,-1,0);private static long received;
    private static int zinc,brass;
    public static void receive(EmotionPacket p){var mc=Minecraft.getInstance();if(mc.level==null||!p.dimension().equals(mc.level.dimension().location().toString()))return;status=p;received=mc.level.getGameTime();zinc=p.zincMode();brass=p.brassMode();}
    public static int cycle(String power,int direction){if(power.equals("a:zinc")){zinc=Math.floorMod(zinc+direction,5);return zinc;}brass=Math.floorMod(brass+direction,5);return brass;}
    private static boolean active(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&mc.player.isAlive()&&status.dimension().equals(mc.level.dimension().location().toString())&&mc.level.getGameTime()-received<20;}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){if(!active()){status=new EmotionPacket("",0,0,-1,0);zinc=brass=0;}}
    @SubscribeEvent public void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(!active()||!PowerClientConfig.EMOTION_HUD.get()||mc.options.hideGui||mc.screen!=null)return;var g=e.getGuiGraphics();int y=g.guiHeight()/2+65;
        String primary=PowerClient.primaryPower(),secondary=PowerClient.secondaryPower();
        if(Emotion.power(primary))g.drawCenteredString(mc.font,(primary.equals("a:zinc")?"Riot ":"Soothe ")+Emotion.values()[primary.equals("a:zinc")?zinc:brass].label()+" · hold ability + scroll",g.guiWidth()/2,y,0xD9ACD7);
        if(Emotion.power(secondary))g.drawCenteredString(mc.font,(secondary.equals("a:zinc")?"Riot ":"Soothe ")+Emotion.values()[secondary.equals("a:zinc")?zinc:brass].label()+" · secondary + scroll",g.guiWidth()/2,y+11,0xD9ACD7);
        if(status.dominant()>=0)g.drawCenteredString(mc.font,"Dominant emotion: "+Emotion.values()[status.dominant()].label(),g.guiWidth()/2,y+24,0xEDB4B4);
    }
}
