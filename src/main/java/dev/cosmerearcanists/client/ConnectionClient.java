package dev.cosmerearcanists.client;

import dev.cosmerearcanists.connection.ConnectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;

/** Bounded Connection compass, expiring locally if the server stops authorizing perception. */
public final class ConnectionClient {
    private static ClientLevel world;
    private static ConnectionPacket packet;
    private static long expires;
    public static void receive(ConnectionPacket next){var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().toString().equals(next.dimension()))return;world=mc.level;expires=world.getGameTime()+15;packet=next.players().isEmpty()?null:next;}
    private static boolean active(){var mc=Minecraft.getInstance();return packet!=null&&mc.level==world&&mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&world.getGameTime()<expires;}
    @SubscribeEvent public void tick(ClientTickEvent.Post e){if(!active())packet=null;}
    @SubscribeEvent public void hud(RenderGuiEvent.Post e){if(!active()||!PowerClientConfig.CONNECTION_HUD.get())return;var mc=Minecraft.getInstance();if(mc.options.hideGui||mc.screen!=null||mc.getCameraEntity()!=mc.player)return;var g=e.getGuiGraphics();var pose=g.pose();pose.pushPose();pose.translate(PowerClientConfig.CONNECTION_X.get(),PowerClientConfig.CONNECTION_Y.get(),0);float scale=PowerClientConfig.CONNECTION_SCALE.get().floatValue();pose.scale(scale,scale,1);g.drawString(mc.font,"Connections",0,0,0xDFB9EA);int y=12;for(var sensed:packet.players()){var d=sensed.direction();double angle=Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(-d.x,d.z))-mc.player.getYRot()));String arrow=Math.abs(angle)<30?"↑":Math.abs(angle)>150?"↓":angle>0?"→":"←";String vertical=d.y>.5?" above":d.y<-.5?" below":"";g.drawString(mc.font,arrow+" "+sensed.name()+" · "+Math.round(sensed.distance())+" m"+vertical,0,y,0xEAD8EF);y+=11;}pose.popPose();}
}
