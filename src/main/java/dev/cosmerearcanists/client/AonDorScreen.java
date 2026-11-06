package dev.cosmerearcanists.client;

import dev.cosmerearcanists.aondor.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Manual glyph tiles plus a learned-Aon preview palette and radial placement sizing. */
public final class AonDorScreen extends Screen {
    private final int slot;
    private final UUID owner;
    private final String dimension;
    private final List<AonDrawing.Glyph> glyphs=new ArrayList<>();
    private List<AonDorNetwork.Known> known=List.of();
    private final Map<String,AonDorNetwork.Stamp> cache=new HashMap<>();
    private AonDorNetwork.Stamp preview;
    private int libraryIndex=-1,selected,canvasX,canvasY,canvasWidth,canvasHeight;
    private boolean moving,requestedLibrary,recognized;
    private record Edit(List<AonDrawing.Glyph> glyphs,int selected,boolean recognized){}
    private final Deque<Edit> undo=new ArrayDeque<>();
    private Edit placementEdit;
    private AonDrawing pendingDrawing;
    private void saveEdit(){undo.addLast(new Edit(List.copyOf(glyphs),selected,recognized));while(undo.size()>100)undo.removeFirst();pending=null;}
    private void undo(){
        if(placement>=0){cancelPlacement();return;}
        if(stroke!=null){stroke=null;message="Unfinished stroke undone.";return;}
        if(undo.isEmpty())return;var edit=undo.removeLast();glyphs.clear();glyphs.addAll(edit.glyphs());selected=edit.selected();recognized=edit.recognized();pending=null;moving=false;needsRecognition=true;message=recognized?"Restored recognized Aon.":"Edit undone.";
    }
    private List<AonDrawing.Point> stroke;
    private String message="Draw an Aon correctly once to add it to your scroll library.";
    private UUID pending,pendingPick;
    private long submitted=-100,pickSubmitted;
    private int placement=-1,previousSelection;
    private AonDrawing.Glyph beforePlacement;
    public AonDorScreen(int slot,AonDrawing draft) {
        super(Component.literal("AonDor"));this.slot=slot;var mc=Minecraft.getInstance();owner=mc.player.getUUID();dimension=mc.level.dimension().location().toString();
        if(draft==null)glyphs.add(new AonDrawing.Glyph(true,.5,.5,.72,List.of()));else glyphs.addAll(draft.glyphs());
    }
    public UUID owner(){return owner;}
    public String dimension(){return dimension;}
    public boolean recognized(){return recognized;}
    public void restoreRecognition(boolean value){recognized=value;if(recognized)message="Aon recognized. You can keep editing; release power to cast.";}
    public AonDrawing.Glyph projection(){
        var main=drawing().main();var lines=new ArrayList<>(main.strokes());
        if(stroke!=null&&stroke.size()>1&&selected==glyphs.indexOf(main)&&lines.size()<AonDrawing.MAX_STROKES)lines.add(AonRecognition.resample(stroke,24));
        if(lines.isEmpty())return new AonDrawing.Glyph(true,.5,.5,List.of());
        double minX=1,minY=1,maxX=0,maxY=0;
        for(var line:lines)for(var p:line){minX=Math.min(minX,p.x());minY=Math.min(minY,p.y());maxX=Math.max(maxX,p.x());maxY=Math.max(maxY,p.y());}
        final double midX=(minX+maxX)/2,midY=(minY+maxY)/2,extent=Math.max(.001,Math.max(maxX-minX,maxY-minY));
        var centered=lines.stream().map(points->points.stream().map(p->new AonDrawing.Point(.5+(p.x()-midX)/extent*.8,.5+(p.y()-midY)/extent*.8)).toList()).toList();
        return new AonDrawing.Glyph(true,.5,.5,Math.max(.1,Math.min(1,extent*1.5)),centered);
    }
    private void stopProjection(){if(minecraft.player!=null&&minecraft.level!=null&&minecraft.getConnection()!=null&&dimension.equals(minecraft.level.dimension().location().toString()))PacketDistributor.sendToServer(new AonDorNetwork.Preview(dimension,slot,false,new AonDrawing.Glyph(true,.5,.5,List.of())));}
    public AonDrawing drawing(){return new AonDrawing(glyphs);}
    private void button(String label,int x,int width,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,height-62,width,20).build());}
    @Override protected void init(){
        cancelPlacement();canvasX=12;canvasY=48;canvasWidth=Math.max(120,width-24);canvasHeight=Math.max(100,height-118);stroke=null;moving=false;
        int start=Math.max(5,(width-132)/2);
        button("Undo",start,62,this::undo);
        button("Reset",start+66,62,()->{saveEdit();glyphs.clear();glyphs.add(new AonDrawing.Glyph(true,.5,.5,1,List.of()));selected=0;recognized=false;message="Draw an Aon; release the power key to cast.";});
        if(!requestedLibrary){requestedLibrary=true;PacketDistributor.sendToServer(new AonDorNetwork.Pick(UUID.randomUUID(),dimension,slot,""));}
    }
    private void replace(double x,double y,double size,List<List<AonDrawing.Point>> lines){var old=glyphs.get(selected);glyphs.set(selected,new AonDrawing.Glyph(old.effect(),x,y,size,lines));}
    private int size(AonDrawing.Glyph g){if(g.effect()&&placement<0)return Math.max(canvasWidth,canvasHeight);return Math.max(16,(int)(g.size()*Math.min(canvasWidth,canvasHeight)));}
    private int left(AonDrawing.Glyph g){if(g.effect()&&placement<0)return canvasX;return canvasX+(int)(g.x()*canvasWidth)-size(g)/2;}
    private int top(AonDrawing.Glyph g){if(g.effect()&&placement<0)return canvasY;return canvasY+(int)(g.y()*canvasHeight)-size(g)/2;}
    private AonDrawing.Point point(double x,double y){var g=glyphs.get(selected);if(g.effect()){x=Math.max(canvasX,Math.min(canvasX+canvasWidth,x));y=Math.max(canvasY,Math.min(canvasY+canvasHeight,y));}return new AonDrawing.Point(Math.max(0,Math.min(1,(x-left(g))/size(g))),Math.max(0,Math.min(1,(y-top(g))/size(g))));}
    private boolean inCanvas(double x,double y){return x>=canvasX&&x<=canvasX+canvasWidth&&y>=canvasY&&y<=canvasY+canvasHeight;}
    private void startPlacement(double x,double y){

        if(preview==null){message=known.isEmpty()?"Draw an Aon correctly once to learn it.":"Wait for the selected Aon's preview.";return;}
        if(!preview.glyph().effect()){message="Arguments are disabled for this drawing test.";return;}
        finishStroke();placementEdit=new Edit(List.copyOf(glyphs),selected,recognized);moving=false;previousSelection=selected;pending=null;
        double cx=Math.max(canvasX+20,Math.min(canvasX+canvasWidth-20,x)),cy=Math.max(canvasY+32,Math.min(canvasY+canvasHeight-20,y));
        if(preview.glyph().effect()){placement=glyphs.indexOf(drawing().main());beforePlacement=glyphs.get(placement);}
        else {placement=glyphs.size();beforePlacement=null;glyphs.add(new AonDrawing.Glyph(false,.5,.5,List.of()));}
        selected=placement;glyphs.set(placement,new AonDrawing.Glyph(preview.glyph().effect(),(cx-canvasX)/canvasWidth,(cy-canvasY)/canvasHeight,.1,preview.glyph().strokes()));
        resizePlacement(x,y);message="Move away to enlarge, closer to shrink. Left-click confirms; right-click or Escape cancels.";
    }
    private void resizePlacement(double x,double y){
        if(placement<0)return;var g=glyphs.get(placement);double cx=canvasX+g.x()*canvasWidth,cy=canvasY+g.y()*canvasHeight;
        double limit=2*Math.min(Math.min(cx-canvasX,canvasX+canvasWidth-cx),Math.min(cy-canvasY-12,canvasY+canvasHeight-cy));
        double pixels=Math.max(24,Math.min(Math.min(canvasWidth,canvasHeight),Math.min(limit,2*Math.hypot(x-cx,y-cy))));
        glyphs.set(placement,new AonDrawing.Glyph(g.effect(),g.x(),g.y(),Math.max(.02,Math.min(1,pixels/Math.min(canvasWidth,canvasHeight))),g.strokes()));
    }
    private void bakePlacement(){
        var glyph=glyphs.get(placement);double pixels=size(glyph),x=left(glyph),y=top(glyph),scale=Math.max(canvasWidth,canvasHeight);
        var lines=glyph.strokes().stream().map(points->points.stream().map(p->new AonDrawing.Point(Math.max(0,Math.min(1,(x+p.x()*pixels-canvasX)/scale)),Math.max(0,Math.min(1,(y+p.y()*pixels-canvasY)/scale)))).toList()).toList();
        glyphs.set(placement,new AonDrawing.Glyph(true,.5,.5,1,lines));recognized=false;
    }
    private void cancelPlacement(){if(placement<0)return;if(beforePlacement==null)glyphs.remove(placement);else glyphs.set(placement,beforePlacement);selected=Math.min(previousSelection,glyphs.size()-1);placement=-1;beforePlacement=null;placementEdit=null;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(placement>=0){if(button==0){resizePlacement(x,y);bakePlacement();if(placementEdit!=null){undo.addLast(placementEdit);while(undo.size()>100)undo.removeFirst();}placement=-1;beforePlacement=null;placementEdit=null;check();}else if(button==1){cancelPlacement();message="Placement cancelled.";}return true;}
        if(super.mouseClicked(x,y,button))return true;
        if(button==1&&inCanvas(x,y)){startPlacement(x,y);return true;}
        if(button!=0)return false;
        for(int i=glyphs.size()-1;i>=0;i--){var g=glyphs.get(i);int left=left(g),top=top(g),size=size(g);
            if(g.effect()?inCanvas(x,y):(x>=left&&x<=left+size&&y>=top-12&&y<=top+size)){selected=i;moving=!g.effect()&&y<top;if(moving)saveEdit();recognized=false;pending=null;
                if(!moving&&g.strokes().size()<AonDrawing.MAX_STROKES){stroke=new ArrayList<>();stroke.add(point(x,y));}return true;}}
        return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(placement>=0){resizePlacement(x,y);return true;}
        if(button!=0)return super.mouseDragged(x,y,button,dx,dy);
        if(moving){var g=glyphs.get(selected);double marginX=(size(g)/2.0+2)/canvasWidth,marginY=(size(g)/2.0+14)/canvasHeight;
            replace(Math.max(marginX,Math.min(1-marginX,g.x()+dx/canvasWidth)),Math.max(marginY,Math.min(1-marginY,g.y()+dy/canvasHeight)),g.size(),g.strokes());return true;}
        if(stroke!=null){var p=point(x,y);var last=stroke.getLast();if(Math.hypot(p.x()-last.x(),p.y()-last.y())>.008){if(stroke.size()>=256)stroke=new ArrayList<>(AonRecognition.resample(stroke,128));stroke.add(p);}return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    private void finishStroke(){
        if(stroke!=null&&!stroke.isEmpty()){if(stroke.size()==1)stroke.add(stroke.getFirst());var g=glyphs.get(selected);var lines=new ArrayList<>(g.strokes());lines.add(AonRecognition.resample(stroke,24));
            try{var glyph=new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),lines);var candidate=new ArrayList<>(glyphs);candidate.set(selected,glyph);new AonDrawing(candidate);saveEdit();glyphs.set(selected,glyph);
                if(minecraft.player!=null&&minecraft.level!=null&&minecraft.getConnection()!=null&&dimension.equals(minecraft.level.dimension().location().toString()))PacketDistributor.sendToServer(new AonDorNetwork.Trace(dimension,slot,glyph));}
            catch(IllegalArgumentException invalid){message=invalid.getMessage();}}
        if(stroke!=null){recognized=false;needsRecognition=true;}stroke=null;
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&(moving||stroke!=null)){moving=false;finishStroke();return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_Z&&hasControlDown()){undo();return true;}if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE&&placement>=0){cancelPlacement();message="Placement cancelled.";return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(vertical==0)return false;
        if(placement>=0||moving||stroke!=null){message="Finish drawing or sizing before choosing another Aon.";return true;}
        if(known.isEmpty()){message="Draw an Aon correctly once to unlock it here.";return true;}
        libraryIndex=Math.floorMod(libraryIndex+(vertical>0?-1:1),known.size());selectPreview();return true;
    }
    private void selectPreview(){
        preview=null;if(libraryIndex<0||libraryIndex>=known.size())return;var entry=known.get(libraryIndex);
        if(cache.containsKey(entry.id())){preview=cache.get(entry.id());pendingPick=null;return;}
        pendingPick=UUID.randomUUID();pickSubmitted=minecraft.level.getGameTime();PacketDistributor.sendToServer(new AonDorNetwork.Pick(pendingPick,dimension,slot,entry.id()));
    }
    public void library(AonDorNetwork.Library library){
        if(!dimension.equals(library.dimension()))return;String selectedId=libraryIndex>=0&&libraryIndex<known.size()?known.get(libraryIndex).id():"";
        known=library.aons();cache.clear();libraryIndex=-1;for(int i=0;i<known.size();i++)if(known.get(i).id().equals(selectedId))libraryIndex=i;
        if(libraryIndex<0&&!known.isEmpty())libraryIndex=0;selectPreview();if(!library.message().isEmpty())message=library.message();
    }
    public void stamp(AonDorNetwork.Stamp stamp){
        if(!stamp.nonce().equals(pendingPick)||!dimension.equals(stamp.dimension())||libraryIndex<0||!known.get(libraryIndex).id().equals(stamp.id()))return;
        cache.put(stamp.id(),stamp);preview=stamp;pendingPick=null;
    }
    private void check(){

        if(stroke!=null||moving||placement>=0){message="Finish drawing or sizing first.";return;}
        long now=minecraft.level.getGameTime();if(now-submitted<10){message="Wait a moment before checking again.";return;}
        submitted=now;pending=UUID.randomUUID();pendingDrawing=drawing();message="Recognizing central Aon...";PacketDistributor.sendToServer(new AonDorNetwork.Check(pending,dimension,slot,drawing()));
    }
    private void cast(){
        if(stroke!=null||moving||placement>=0||drawing().main().strokes().isEmpty())return;
        long now=minecraft.level.getGameTime();submitted=now;pending=UUID.randomUUID();pendingDrawing=null;
        message="Casting...";PacketDistributor.sendToServer(new AonDorNetwork.Cast(new AonDorNetwork.Check(pending,dimension,slot,drawing())));
    }
    public void result(AonDorNetwork.Result result){if(result.nonce().equals(pending)&&result.dimension().equals(dimension)){message=result.message();pending=null;
            if(message.startsWith("Recognized:")&&stroke==null&&!moving&&placement<0&&pendingDrawing!=null&&pendingDrawing.equals(drawing())){recognized=true;}
            pendingDrawing=null;}}
    private boolean needsRecognition;
    @Override public void tick(){
        if(minecraft.player==null||minecraft.level==null||!minecraft.player.isAlive()||minecraft.player.isSpectator()
            ||!minecraft.isWindowActive()||!dimension.equals(minecraft.level.dimension().location().toString())
            ||!AonDorService.POWER.equals(slot==0?PowerClient.primaryPower():PowerClient.secondaryPower())){onClose();return;}
        if(!AonDorClient.held(slot)){cancelPlacement();finishStroke();moving=false;cast();onClose();return;}
        long now=minecraft.level.getGameTime();if(needsRecognition&&stroke==null&&!moving&&placement<0&&now-submitted>=10){needsRecognition=false;check();}
        if(now%5==0)PacketDistributor.sendToServer(new AonDorNetwork.Preview(dimension,slot,true,projection()));
        if(pending!=null&&now-submitted>60){message="No reply received. You can check again.";pending=null;}
        if(pendingPick!=null&&now-pickSubmitted>60){message="Preview unavailable. Scroll to try again.";pendingPick=null;}
    }
    @Override public void onClose(){cancelPlacement();finishStroke();super.onClose();}
    @Override public void removed(){cancelPlacement();finishStroke();AonDorClient.remember(this);stopProjection();super.removed();}
    @Override public boolean isPauseScreen(){return false;}
    private void line(GuiGraphics graphics,double x1,double y1,double x2,double y2,int color){
        var pose=graphics.pose();pose.pushPose();pose.translate(x1,y1,0);pose.mulPose(new org.joml.Quaternionf().rotationZ((float)Math.atan2(y2-y1,x2-x1)));
        graphics.fill(0,-1,(int)Math.ceil(Math.hypot(x2-x1,y2-y1))+1,1,color);pose.popPose();
    }
    /** Broad faint halo first, narrow white-hot core last; all layers use normal GUI alpha blending. */
    private void strokes(GuiGraphics graphics,List<List<AonDrawing.Point>> lines,int x,int y,int size){
        int[] radii={7,5,3,2,1,0};int[] alpha={6,10,20,45,135,255};
        for(int layer=0;layer<radii.length;layer++)for(var points:lines)for(int i=1;i<points.size();i++){
            var a=points.get(i-1);var b=points.get(i);double x1=x+a.x()*size,y1=y+a.y()*size,x2=x+b.x()*size,y2=y+b.y()*size;
            var pose=graphics.pose();pose.pushPose();pose.translate(x1,y1,0);
            pose.mulPose(new org.joml.Quaternionf().rotationZ((float)Math.atan2(y2-y1,x2-x1)));
            int radius=radii[layer];graphics.fill(-radius,-radius,(int)Math.ceil(Math.hypot(x2-x1,y2-y1))+radius+1,radius+1,(alpha[layer]<<24)|0xFFFFFF);
            pose.popPose();
        }
    }
    // Screen.render calls this before widgets. Our canvas/background are already drawn;
    // vanilla's blur here would post-process the completed ink and obscure the projection.
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partial){}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        resizePlacement(mouseX,mouseY);graphics.fill(0,0,width,height,0x65080F19);graphics.drawCenteredString(font,title,width/2,8,0xFFD8F8FF);
        graphics.drawCenteredString(font,"Release power to cast · Ctrl+Z undo · scroll learned Aons · right-click places",width/2,24,0xFFB7CAD8);
        var ordered=drawing().arguments();var main=drawing().main();
        for(int index=0;index<glyphs.size();index++){
            var glyph=glyphs.get(index);int x=left(glyph),y=top(glyph),size=size(glyph),boxWidth=glyph.effect()&&placement<0?canvasWidth:size,boxHeight=glyph.effect()&&placement<0?canvasHeight:size,color=index==placement?0xFFE6BC71:index==selected?0xFF87EBFF:0xFF426579;
            if(!glyph.effect())line(graphics,canvasX+main.x()*canvasWidth,canvasY+main.y()*canvasHeight,canvasX+glyph.x()*canvasWidth,canvasY+glyph.y()*canvasHeight,0x554F8CA5);
            graphics.fill(x-1,y-13,x+boxWidth+1,y,color);
            graphics.fill(x-1,y,x,y+boxHeight+1,color);graphics.fill(x+boxWidth,y,x+boxWidth+1,y+boxHeight+1,color);
            graphics.fill(x,y+boxHeight,x+boxWidth,y+boxHeight+1,color);graphics.fill(x,y,x+boxWidth,y+boxHeight,0x20000000);
            graphics.drawString(font,glyph.effect()?(recognized?"Recognized Aon":"Drawing Aon"):"Arg "+(ordered.indexOf(glyph)+1),x+2,y-11,0xFFFFFFFF,false);
            for(int offset=16;offset<boxWidth;offset+=16)graphics.fill(x+offset,y,x+offset+1,y+boxHeight,0x28FFFFFF);
            for(int offset=16;offset<boxHeight;offset+=16)graphics.fill(x,y+offset,x+boxWidth,y+offset+1,0x28FFFFFF);
            strokes(graphics,glyph.strokes(),x,y,size);if(index==selected&&stroke!=null)strokes(graphics,List.of(stroke),x,y,size);
        }
        int side=width-136;graphics.fill(side,42,width-10,height-78,0xE5122332);graphics.drawString(font,"Learned Aons: "+known.size(),side+5,48,0xFFCBDFE8,false);
        if(libraryIndex>=0&&libraryIndex<known.size()){
            var entry=known.get(libraryIndex);int nameY=64;for(var text:font.split(Component.literal(entry.name()),114)){graphics.drawString(font,text,side+5,nameY,0xFFFFFFFF,false);nameY+=10;if(nameY>94)break;}
            graphics.drawString(font,entry.effect()?"Effect":"Argument",side+5,104,0xFF8DDCF2,false);
            if(preview!=null)strokes(graphics,preview.glyph().strokes(),side+18,124,90);else graphics.drawString(font,"Loading...",side+5,126,0xFF9AAFBF,false);
            graphics.drawString(font,(libraryIndex+1)+" / "+known.size(),side+5,222,0xFFCBDFE8,false);
        } else graphics.drawString(font,"Draw to unlock",side+5,72,0xFF9AAFBF,false);
        int y=height-36;for(var text:font.split(Component.literal(message),Math.max(80,width-24))){graphics.drawString(font,text,12,y,0xFFCBDFE8,false);y+=10;if(y>height-8)break;}
        super.render(graphics,mouseX,mouseY,partial);
    }
}
