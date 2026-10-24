package dev.cosmerearcanists.client;

import dev.cosmerearcanists.copper.*;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static dev.cosmerearcanists.copper.CopperNetwork.*;

/** The server owns notebook identity, permissions and page contents; widgets hold drafts only. */
public final class CopperScreen extends Screen {
    private Snapshot data;private CopperPage selected;private UUID selectedId=ZERO;private int offset;
    private EditBox titleBox;private MultiLineEditBox textBox;
    private int left,right,rightWidth,bodyY,bodyHeight;
    public CopperScreen(Snapshot data){super(Component.literal("Coppermind"));this.data=data;}
    public void update(Snapshot packet,boolean changed){if(!changed&&!packet.message().isEmpty()){data=packet;return;}UUID previousSelection=selectedId;if(!changed&&packet.pages().size()>data.pages().size())selectedId=packet.pages().getLast().id();data=packet;if(changed){selectedId=ZERO;offset=0;}selected=data.pages().stream().filter(p->p.id().equals(selectedId)).findFirst().orElse(null);if(selected!=null&&selected.kind()==CopperPage.SCREENSHOT&&(changed||!selected.id().equals(previousSelection)))CopperClient.requestImage(selected.id());rebuildWidgets();}
    private Button button(String text,int x,int y,int w,Runnable action,boolean enabled){var b=Button.builder(Component.literal(text),ignored->action.run()).bounds(x,y,w,20).build();b.active=enabled;addRenderableWidget(b);return b;}
    private void send(int action,UUID entry){CopperClient.send(action,entry,titleBox.getValue(),textBox.getValue(),0,new byte[0]);}
    private void choose(CopperPage page){selected=page;selectedId=page.id();if(page.kind()==CopperPage.SCREENSHOT)CopperClient.requestImage(page.id());else CopperClient.clearImage();rebuildWidgets();}
    @Override protected void init(){
        left=16;int listWidth=Math.max(100,Math.min(170,width/3));right=left+listWidth+12;rightWidth=Math.max(80,width-right-16);bodyY=72;bodyHeight=Math.max(30,height-168);
        button("Next metalmind",left,42,listWidth,()->CopperClient.send(NEXT,ZERO,"","",0,new byte[0]),true);
        int visible=Math.max(1,(height-142)/22);offset=Math.min(offset,Math.max(0,data.pages().size()-1));
        for(int i=0;i<visible&&offset+i<data.pages().size();i++){var page=data.pages().get(offset+i);String prefix=page.kind()==CopperPage.WAYPOINT?"◆ ":page.kind()==CopperPage.SCREENSHOT?"▣ ":"• ";button(font.plainSubstrByWidth(prefix+page.title(),listWidth-8),left,70+i*22,listWidth,()->choose(page),true);}
        button("Previous",left,height-64,listWidth/2-2,()->{offset=Math.max(0,offset-visible);rebuildWidgets();},offset>0);
        button("Next",left+listWidth/2+2,height-64,listWidth/2-2,()->{offset+=visible;rebuildWidgets();},offset+visible<data.pages().size());
        titleBox=new EditBox(font,right,42,rightWidth,20,Component.literal("Page title"));titleBox.setMaxLength(80);titleBox.setValue(selected==null?"":selected.title());titleBox.setEditable(data.editable());addRenderableWidget(titleBox);
        textBox=new MultiLineEditBox(font,right,bodyY,rightWidth,bodyHeight,Component.literal("Write a note about this memory…"),Component.literal("Memory text"));textBox.setCharacterLimit(2048);textBox.setValue(selected==null?"":selected.text());textBox.active=true;textBox.visible=selected==null||selected.kind()!=CopperPage.SCREENSHOT;addRenderableWidget(textBox);
        int w=(rightWidth-8)/3,y=height-64;
        button("New note",right,y,w,()->{selected=null;selectedId=ZERO;CopperClient.clearImage();rebuildWidgets();},data.editable());
        button("Save",right+w+4,y,w,()->send(SAVE,selectedId),data.editable());
        button("Here",right+2*(w+4),y,w,()->send(LOCATION,ZERO),data.editable());
        button("Capture",right,y+24,w,()->CopperClient.capture(titleBox.getValue(),textBox.getValue()),data.editable()&&dev.cosmerearcanists.feruchemy.FeruchemyConfig.COPPER_SCREENSHOTS.get());
        button("Delete",right+w+4,y+24,w,()->{send(DELETE,selectedId);selected=null;selectedId=ZERO;CopperClient.clearImage();},data.editable()&&selected!=null);
        button("Track",right+2*(w+4),y+24,w,()->send(TRACK,selectedId),!data.editable()&&selected!=null&&selected.kind()==CopperPage.WAYPOINT);
        button("Close",left,height-38,listWidth,()->onClose(),true);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xF0141110);g.drawString(font,data.label(),left,12,0xEBC293);g.drawString(font,(data.editable()?"Storing · edit memories":"Tapping · read memories")+" · "+data.pages().size()+" pages",left,26,0xC9B9AA);
        super.render(g,mouseX,mouseY,partial);
        if(selected!=null&&selected.kind()==CopperPage.SCREENSHOT){var image=CopperClient.texture;if(image==null)g.drawString(font,"Loading screenshot…",right+4,bodyY+8,0xFFFFFF);else{double scale=Math.min((double)rightWidth/CopperClient.imageWidth,(double)bodyHeight/CopperClient.imageHeight);int w=(int)(CopperClient.imageWidth*scale),h=(int)(CopperClient.imageHeight*scale);g.blit(image,right+(rightWidth-w)/2,bodyY+(bodyHeight-h)/2,w,h,0,0,CopperClient.imageWidth,CopperClient.imageHeight,CopperClient.imageWidth,CopperClient.imageHeight);}}
        if(selected!=null&&selected.kind()==CopperPage.WAYPOINT)g.drawString(font,font.plainSubstrByWidth(selected.dimension()+" · "+selected.position().getX()+", "+selected.position().getY()+", "+selected.position().getZ(),rightWidth),right,height-88,0xFFD69A);
        g.drawString(font,font.plainSubstrByWidth("Data: "+data.usedBytes()+" / "+data.capacityBytes()+" bytes · Metal limit: "+data.maximumBytes()+" · Save stores required charge",width-32),left,height-12,0xA99B8C);
    }
    @Override public boolean charTyped(char character,int modifiers){return data.editable()&&super.charTyped(character,modifiers);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(!data.editable()&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE||key==org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE||key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER||hasControlDown()&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_V||key==org.lwjgl.glfw.GLFW.GLFW_KEY_X)))return true;return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
