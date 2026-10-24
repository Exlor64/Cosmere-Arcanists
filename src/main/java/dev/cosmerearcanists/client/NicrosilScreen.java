package dev.cosmerearcanists.client;

import dev.cosmerearcanists.nicrosil.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import static dev.cosmerearcanists.nicrosil.NicrosilNetwork.*;

/** Targets are requests; server ticks determine actual transfers. Choices require an explicit button. */
public final class NicrosilScreen extends Screen {
    private Snapshot data;private Row selected;private int offset;private final Object world;
    private int left,top,bodyWidth,visible;private boolean pending;private String amountText="",inputError="";private EditBox amountBox;
    public NicrosilScreen(Snapshot data){super(Component.literal("Nicrosilmind"));this.data=data;world=Minecraft.getInstance().level;}
    public static void receive(Snapshot packet){var mc=Minecraft.getInstance();if(mc.player==null)return;if(!packet.open()){if(mc.screen instanceof NicrosilScreen)mc.setScreen(null);return;}if(mc.screen instanceof NicrosilScreen screen)screen.update(packet);else mc.setScreen(new NicrosilScreen(packet));}
    private void update(Snapshot packet){boolean changed=data.storing()!=packet.storing();data=packet;pending=false;if(changed){selected=null;offset=0;}else if(selected!=null){var old=selected;selected=data.rows().stream().filter(r->r.entry().equals(old.entry())&&r.key().equals(old.key())&&r.kind()==old.kind()).findFirst().orElse(null);}rebuildWidgets();}
    private void send(int action,Row row){double amount=0;if(action==STORE&&row!=null&&row.kind()==NicrosilLedger.POWER&&!amountText.isBlank()){try{amount=Double.parseDouble(amountText);if(!Double.isFinite(amount)||amount<=0||amount>1024)throw new NumberFormatException();}catch(NumberFormatException e){inputError="Enter a positive strength up to1024, or leave blank for all.";return;}}inputError="";PacketDistributor.sendToServer(new Request(action,data.session(),row==null?ZERO:row.entry(),row==null?0:row.kind(),row==null?"":row.key(),amount));pending=true;rebuildWidgets();}

    private void button(String text,int x,int y,int width,Runnable action,boolean active){var b=Button.builder(Component.literal(text),ignored->action.run()).bounds(x,y,width,20).build();b.active=active;addRenderableWidget(b);}
    @Override protected void init(){
        boolean editing=amountBox!=null&&amountBox.isFocused();int cursor=amountBox==null?0:amountBox.getCursorPosition();
        bodyWidth=Math.min(460,width-24);left=(width-bodyWidth)/2;top=14;visible=Math.max(1,(height-190)/22);offset=Math.max(0,Math.min(offset,Math.max(0,data.rows().size()-visible)));
        button("Store",left,top+32,bodyWidth/2-2,()->send(STORE_MODE,null),!pending&&data.canStore()&&!data.storing());button("Tap",left+bodyWidth/2+2,top+32,bodyWidth/2-2,()->send(TAP_MODE,null),!pending&&data.storing());
        for(int i=0;i<visible&&offset+i<data.rows().size();i++){var row=data.rows().get(offset+i);String marker=row==selected?"> ":"";String label=marker+(row.kind()==NicrosilLedger.RESOURCE?"Investiture: ":row.kind()==NicrosilLedger.TEMPORARY?"Temporary: ":"Power: ")+row.label()+String.format(Locale.ROOT," · %.2f",row.amount());button(font.plainSubstrByWidth(label,bodyWidth-12),left,top+60+i*22,bodyWidth,()->{selected=row;rebuildWidgets();},!pending);}
        int y=height-108;button("Previous",left,y,86,()->{offset=Math.max(0,offset-visible);rebuildWidgets();},offset>0&&!pending);button("Next",left+90,y,86,()->{offset+=visible;rebuildWidgets();},offset+visible<data.rows().size()&&!pending);
        amountBox=new EditBox(font,left+128,height-84,bodyWidth-128,20,Component.literal("Power strength to store"));amountBox.setMaxLength(24);amountBox.setValue(amountText);amountBox.setResponder(text->amountText=text);amountBox.active=data.storing()&&!pending;addRenderableWidget(amountBox);if(editing&&amountBox.active){setFocused(amountBox);amountBox.setCursorPosition(Math.min(cursor,amountText.length()));}
        button(data.storing()?(data.compounding()?"Start compounding":"Start storing"):"Tap selected",left,height-58,bodyWidth-180,()->send(data.storing()?STORE:TAP,selected),!pending&&selected!=null&&selected.enabled());button("Stop",left+bodyWidth-176,height-58,84,()->send(STOP,null),!pending&&data.active());button("Close",left+bodyWidth-88,height-58,88,this::onClose,true);

    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xF012171C);g.drawString(font,font.plainSubstrByWidth(data.label(),bodyWidth),left,top,0xB8D0D7);g.drawString(font,(data.storing()?(data.compounding()?"Encode temporary powers / Investiture":"Store your powers / Investiture"):"Tap stored powers / Investiture")+(data.unkeyed()?" · Unkeyed":" · Identity bound"),left,top+16,0xC1CCD0);
        super.render(g,mouseX,mouseY,partial);
        if(data.rows().isEmpty())g.drawString(font,data.storing()?"No supported powers or Investiture to store.":"This nicrosilmind is empty.",left,top+64,0xA0AEB7);
        String warning=selected!=null&&selected.kind()==NicrosilLedger.TEMPORARY?"Temporary power: needs contact and consumes charge over time.":data.active()?data.progress():selected!=null&&!selected.enabled()?"Unavailable: check capacity, power and Identity.":data.storing()?(data.compounding()?"Reserves fund charge; your personal power is retained.":"Power storage is gradual; blank target means all."):"Tapping permanently adds the stored strength to your power.";
        g.drawString(font,font.plainSubstrByWidth(warning,bodyWidth),left,height-122,0xE8C49B);
        g.drawString(font,"Power target (or all)",left,height-78,0xA0AEB7);
        g.drawString(font,font.plainSubstrByWidth(inputError.isEmpty()?data.message():inputError,bodyWidth),left,height-30,0xE8C49B);g.drawString(font,String.format(Locale.ROOT,"Capacity: %.2f / %.2f",data.used(),data.capacity()),left,height-16,0xA0AEB7);
    }
    @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=world||mc.player==null||!mc.player.isAlive()){onClose();return;}}
    @Override public void onClose(){if(Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new Request(CLOSE,data.session(),ZERO,0,""));super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
