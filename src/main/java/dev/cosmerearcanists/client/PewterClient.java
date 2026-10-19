package dev.cosmerearcanists.client;

import dev.cosmerearcanists.power.PowerNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Separate gray injury hearts: these represent deferred debt, never extra usable health. */
public final class PewterClient {
    private static PowerNetwork.PewterStatus status=new PowerNetwork.PewterStatus("",0,false,0);
    public static void receive(PowerNetwork.PewterStatus packet){status=packet;}
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        if(Minecraft.getInstance().level==null)status=new PowerNetwork.PewterStatus("",0,false,0);
    }
    @SubscribeEvent public void render(RenderGuiLayerEvent.Post event) {
        if(!event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL))return;
        var mc=Minecraft.getInstance();
        if(mc.gameMode==null || !mc.gameMode.canHurtPlayer() || !PowerClientConfig.PEWTER_HEARTS.get() || mc.player==null || mc.level==null || mc.player.isSpectator()
            || mc.options.hideGui || status.debt()<=.001 || !status.dimension().equals(mc.level.dimension().location().toString()))return;
        var g=event.getGuiGraphics();
        // Health and armor have already advanced the actual NeoForge HUD stack.
        int x=g.guiWidth()/2-90, y=g.guiHeight()-mc.gui.leftHeight+1;
        mc.gui.leftHeight+=10; // Reserve space for following overlays and the selected-item tooltip.
        int count=(int)Math.min(10,Math.ceil(status.debt()/2));
        for(int i=0;i<count;i++) {
            int left=x+i*9;
            double fill=Math.clamp(status.debt()-i*2,0,2);
            // Pixel heart silhouette with gray half/full fills; 2 damage = 1 heart.
            for(int row=0;row<7;row++) {
                int inset=row==0?1:row<3?0:row-2;
                for(int col=inset;col<7-inset;col++) {
                    if(row==0 && col==3)continue;
                    boolean filled=fill>1 || col<3 && fill>0;
                    int color=filled?0xFF969BA3:0xFF343840;
                    g.fill(left+col,y+row,left+col+1,y+row+1,color);
                }
            }
        }
    }
}
