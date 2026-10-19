package dev.cosmerearcanists.client;

import dev.cosmerearcanists.power.PowerNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Separate gray injury hearts: these represent deferred debt, never extra usable health. */
public final class PewterClient {
    private static PowerNetwork.PewterStatus status=new PowerNetwork.PewterStatus("",0,false,0);
    public static void receive(PowerNetwork.PewterStatus packet){status=packet;}
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        if(Minecraft.getInstance().level==null)status=new PowerNetwork.PewterStatus("",0,false,0);
    }
    @SubscribeEvent public void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!PowerClientConfig.PEWTER_HEARTS.get() || mc.player==null || mc.level==null || mc.player.isSpectator()
            || mc.options.hideGui || status.debt()<=.001 || !status.dimension().equals(mc.level.dimension().location().toString()))return;
        var g=event.getGuiGraphics();
        int rows=(int)Math.ceil((mc.player.getMaxHealth()+mc.player.getAbsorptionAmount())/20.0);
        int x=g.guiWidth()/2-91, y=g.guiHeight()-62-Math.max(0,rows-1)*10;
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
        g.drawString(mc.font,String.format(java.util.Locale.ROOT,"Pewter drag: %.2f",status.debt()),x,y-10,0xB5BAC2);
    }
}
