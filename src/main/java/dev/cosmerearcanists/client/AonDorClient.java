package dev.cosmerearcanists.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cosmerearcanists.aondor.*;
import java.util.*;
import net.minecraft.client.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

public final class AonDorClient {
    private static boolean opened,draftRecognized;
    private static UUID owner;
    private static String dimension="";
    private static AonDrawing draft;
    public static boolean held(int slot) {
        var mc=Minecraft.getInstance();if(!mc.isWindowActive())return false;
        var mapping=slot==0?PowerClient.APPLY:PowerClient.SECONDARY;if(!mapping.getKeyModifier().isActive(null))return false;var key=mapping.getKey();long window=mc.getWindow().getWindow();
        if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;
        if(key.getType()==InputConstants.Type.KEYSYM)return key.getValue()>=0&&InputConstants.isKeyDown(window,key.getValue());
        for(int code=GLFW.GLFW_KEY_SPACE;code<=GLFW.GLFW_KEY_LAST;code++)if(GLFW.glfwGetKeyScancode(code)==key.getValue())return InputConstants.isKeyDown(window,code);
        return false;
    }
    public static void remember(AonDorScreen screen) {owner=screen.owner();dimension=screen.dimension();draft=screen.drawing();draftRecognized=screen.recognized();}
    public static void receive(AonDorNetwork.Result result) {var mc=Minecraft.getInstance();if(mc.screen instanceof AonDorScreen screen)screen.result(result);}
    public static void receiveLibrary(AonDorNetwork.Library packet){var mc=Minecraft.getInstance();if(mc.level!=null&&mc.player!=null&&mc.screen instanceof AonDorScreen screen)screen.library(packet);}
    public static void receiveStamp(AonDorNetwork.Stamp packet){var mc=Minecraft.getInstance();if(mc.level!=null&&mc.player!=null&&mc.screen instanceof AonDorScreen screen)screen.stamp(packet);}
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){draft=null;owner=null;dimension="";opened=false;return;}
        boolean primary=held(0),secondary=held(1);if(!primary&&!secondary)opened=false;
        if(opened||mc.screen!=null||!mc.isWindowActive()||!mc.player.isAlive()||PowerClient.SELECT.isDown()||PowerClient.STATE.isDown())return;
        int slot=primary&&PowerClient.APPLY.isDown()&&AonDorService.POWER.equals(PowerClient.primaryPower())?0:secondary&&PowerClient.SECONDARY.isDown()&&AonDorService.POWER.equals(PowerClient.secondaryPower())?1:-1;
        if(slot<0)return;opened=true;
        if(!mc.player.getUUID().equals(owner)||!mc.level.dimension().location().toString().equals(dimension))draft=null;
        var screen=new AonDorScreen(slot,draft);screen.restoreRecognition(draft!=null&&draftRecognized);mc.setScreen(screen);
    }
}
