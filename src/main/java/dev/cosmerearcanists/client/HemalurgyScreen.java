package dev.cosmerearcanists.client;
import com.mojang.blaze3d.platform.InputConstants;
import dev.cosmerearcanists.hemalurgy.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
/** Native container cursor/inventory, freehand body hit requests, front/back player model. */
public final class HemalurgyScreen extends AbstractContainerScreen<HemalurgyMenu> {
    public static final KeyMapping KEY=new KeyMapping("key.cosmerearcanists.hemalurgy",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,"key.categories.cosmerearcanists");
    private boolean back;private int bodyHeight;
    public HemalurgyScreen(HemalurgyMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=312;}
    @Override protected void init(){bodyHeight=Math.min(192,Math.max(96,height-80));imageHeight=bodyHeight+56;super.init();addRenderableWidget(Button.builder(Component.literal("Front / Back"),button->back=!back).bounds(leftPos+16,topPos+bodyHeight+30,108,20).build());}
    private int bodyX(){return leftPos+24;}private int bodyY(){return topPos+24;}private int bodyWidth(){return bodyHeight/2;}
    private void send(int action,double u,double v,int point){PacketDistributor.sendToServer(new HemalurgyNetwork.Request(action,menu.containerId,menu.getStateId(),back,u,v,point));}
    private net.minecraft.nbt.CompoundTag location(int point){return menu.getSlot(point).getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(ImplantService.DISPLAY);}
    private int hovered(double x,double y){int found=-1;double best=Double.POSITIVE_INFINITY;for(int point=0;point<13;point++){if(menu.getSlot(point).getItem().isEmpty()||ImplantRules.POINTS.get(point).back()!=back)continue;var t=location(point);double dx=x-(bodyX()+t.getDouble("u")*bodyWidth()),dy=y-(bodyY()+t.getDouble("v")*bodyHeight),distance=dx*dx+dy*dy;if(Math.abs(dx)<=8&&Math.abs(dy)<=8&&distance<best){found=point;best=distance;}}return found;}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&x>=bodyX()&&x<=bodyX()+bodyWidth()&&y>=bodyY()&&y<=bodyY()+bodyHeight){double u=(x-bodyX())/bodyWidth(),v=(y-bodyY())/bodyHeight;int hit=hovered(x,y);if(menu.getCarried().isEmpty()){if(hit>=0)send(2,u,v,hit);}else if(ImplantRules.body(u,v))send(1,u,v,-1);return true;}return super.mouseClicked(x,y,button);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xF01B2530);g.fill(bodyX()-4,bodyY()-4,bodyX()+bodyWidth()+4,bodyY()+bodyHeight+4,0xFF101820);var player=minecraft.player;if(player!=null){float body=player.yBodyRot,head=player.yHeadRot,yaw=player.getYRot(),pitch=player.getXRot();try{player.yBodyRot=player.yHeadRot=180;player.setYRot(180);player.setXRot(0);InventoryScreen.renderEntityInInventory(g,bodyX()+bodyWidth()/2f,bodyY()+bodyHeight,bodyHeight/1.875f,new Vector3f(),new Quaternionf().rotationZ((float)Math.PI).rotateY(back?(float)Math.PI:0),null,player);}finally{player.yBodyRot=body;player.yHeadRot=head;player.setYRot(yaw);player.setXRot(pitch);}}for(int point=0;point<13;point++){var item=menu.getSlot(point).getItem();if(item.isEmpty()||ImplantRules.POINTS.get(point).back()!=back)continue;var t=location(point);g.renderItem(item,(int)(bodyX()+t.getDouble("u")*bodyWidth())-8,(int)(bodyY()+t.getDouble("v")*bodyHeight)-8);}for(int row=0;row<3;row++)for(int col=0;col<9;col++)g.fill(leftPos+138+col*18,topPos+33+row*18,leftPos+155+col*18,topPos+50+row*18,0xFF44515D);for(int col=0;col<9;col++)g.fill(leftPos+138+col*18,topPos+95,leftPos+155+col*18,topPos+112,0xFF44515D);}
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){g.drawString(font,title,8,7,0xFFFFFF,false);g.drawString(font,playerInventoryTitle,139,22,0xD8E3ED,false);g.drawString(font,"Integrity: "+menu.status.get(0)+"%",139,126,0xDDD5D0,false);g.drawString(font,back?"Back":"Front",84,7,0xA8BDCF,false);g.drawString(font,"Pick up a spike, then",139,146,0xB8C8D8,false);g.drawString(font,"click your body to place.",139,157,0xB8C8D8,false);g.drawString(font,"Click a spike to remove.",139,174,0xB8C8D8,false);int count=0;for(int i=0;i<13;i++)if(!menu.getSlot(i).getItem().isEmpty())count++;g.drawString(font,"Spikes: "+count+" / 13",139,136,0xB8C8D8,false);int hit=hovered(mouseX,mouseY);if(hit>=0)g.drawString(font,"Removal: "+(int)(ImplantRules.removalDamage(hit,count)/2)+" heart(s)",139,187,0xEBAA99,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);int hit=hovered(x,y);if(hit>=0&&menu.getCarried().isEmpty())g.renderTooltip(font,menu.getSlot(hit).getItem(),x,y);}
    public static final class Events {
        @SubscribeEvent public void tick(ClientTickEvent.Post event){var mc=Minecraft.getInstance();while(KEY.consumeClick())if(mc.player!=null&&mc.screen==null)PacketDistributor.sendToServer(new HemalurgyNetwork.Request(0,0,0,false,0,0,-1));}
        @SubscribeEvent public void fov(ViewportEvent.ComputeFov event){var p=Minecraft.getInstance().player;if(p!=null&&event.usedConfiguredFov())event.setFOV(event.getFOV()*p.getAttributeValue(dev.cosmerearcanists.power.PowerAttributes.IMPLANT_VISION));}
    }
}
