package dev.cosmerearcanists.client;

import dev.cosmerearcanists.aondor.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared-canvas ink groups with spatial parenting and an explicit fallback. */
public final class AonDorScreen extends Screen {
    private final int slot;private final UUID owner;private final String dimension;
    private final List<AonDrawing.Glyph> glyphs=new ArrayList<>();
    private record Edit(List<AonDrawing.Glyph> glyphs,int selected){}
    private final Deque<Edit> history=new ArrayDeque<>();
    private final Map<Integer,String> ids=new HashMap<>(),names=new HashMap<>();
    private final Map<Integer,UUID> probes=new HashMap<>();
    private final Set<Integer> invalidNumbers=new HashSet<>();private final Map<Integer,String> numberText=new HashMap<>();
    private List<AonDorNetwork.Known> known=List.of();
    private final Map<String,AonDorNetwork.Stamp> cache=new HashMap<>();
    private int selected,forcedParent=-1,placement=-1,libraryIndex=-1,canvasX,canvasY,canvasWidth,canvasHeight;
    private List<AonDrawing.Point> stroke;private Edit placementEdit;
    private AonDorNetwork.Stamp preview;private AonDrawing.Glyph placementTemplate;private EditBox numberBox;private boolean updatingNumber,numberValid=true,requestedLibrary,needsCheck;
    private UUID pending,pendingPick;private long submitted=-100,pickSubmitted;
    private String message="Draw the main Aon first. Alt-click selects a parent; Draw Argument is the fallback.";
    public AonDorScreen(int slot,AonDrawing draft){super(Component.literal("AonDor"));this.slot=slot;var mc=Minecraft.getInstance();owner=mc.player.getUUID();dimension=mc.level.dimension().location().toString();if(draft==null)glyphs.add(new AonDrawing.Glyph(true,.5,.5,1,List.of()));else glyphs.addAll(draft.glyphs());}
    public UUID owner(){return owner;}public String dimension(){return dimension;}
    public boolean recognized(){return ids.containsKey(0);}public void restoreRecognition(boolean value){}
    public AonDrawing drawing(){return new AonDrawing(glyphs);}
    private void save(){history.addLast(new Edit(List.copyOf(glyphs),selected));while(history.size()>100)history.removeFirst();pending=null;}
    private void restore(Edit edit){glyphs.clear();glyphs.addAll(edit.glyphs());selected=Math.min(edit.selected(),glyphs.size()-1);ids.clear();names.clear();probes.clear();invalidNumbers.clear();numberText.clear();numberValid=true;pending=null;forcedParent=-1;stroke=null;for(int i=0;i<glyphs.size();i++)probe(i);needsCheck=true;updateNumber();}
    private void undo(){if(placement>=0){cancelPlacement();return;}if(stroke!=null){stroke=null;return;}if(!history.isEmpty())restore(history.removeLast());}
    private void button(String text,int x,int width,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,height-62,width,20).build());}
    @Override protected void init(){
        cancelPlacement();stroke=null;canvasX=12;canvasY=48;canvasWidth=Math.max(120,width-24);canvasHeight=Math.max(100,height-118);
        int x=Math.max(4,(width-306)/2);button("Undo",x,42,this::undo);button("Reset",x+46,42,()->{save();restore(new Edit(List.of(new AonDrawing.Glyph(true,.5,.5,1,List.of())),0));});
        button("Draw Argument",x+92,92,()->{if(ids.containsKey(selected)){forcedParent=selected;message="Click to begin a separate argument of "+names.get(selected)+".";}else message="Recognize and select the parent first.";});
        button("Remove",x+188,50,this::remove);
        numberBox=new EditBox(font,x+242,height-62,60,20,Component.literal("Number value"));numberBox.setMaxLength(24);numberBox.setResponder(this::numberChanged);addRenderableWidget(numberBox);updateNumber();
        if(!requestedLibrary){requestedLibrary=true;PacketDistributor.sendToServer(new AonDorNetwork.Pick(UUID.randomUUID(),dimension,slot,""));for(int i=0;i<glyphs.size();i++)probe(i);}
    }
    private int scale(){return Math.max(canvasWidth,canvasHeight);}
    private AonDrawing.Point point(double x,double y){return new AonDrawing.Point(Math.max(0,Math.min(canvasWidth,(x-canvasX)))/scale(),Math.max(0,Math.min(canvasHeight,(y-canvasY)))/scale());}
    private boolean inCanvas(double x,double y){return x>=canvasX&&x<=canvasX+canvasWidth&&y>=canvasY&&y<=canvasY+canvasHeight&&!(x>=width-140&&y<height-78);}
    private void updateNumber(){if(numberBox==null)return;updatingNumber=true;boolean show="cosmerearcanists:number".equals(ids.get(selected));numberBox.setVisible(show);numberBox.setEditable(show);numberBox.setValue(numberText.getOrDefault(selected,Double.toString(glyphs.get(selected).number())));numberValid=invalidNumbers.isEmpty();updatingNumber=false;}
    private void numberChanged(String value){if(updatingNumber||!"cosmerearcanists:number".equals(ids.get(selected)))return;numberText.put(selected,value);try{double number=Double.parseDouble(value);if(!Double.isFinite(number)||Math.abs(number)>1000000)throw new NumberFormatException();var g=glyphs.get(selected);save();glyphs.set(selected,new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),g.strokes(),g.parent(),number));invalidNumbers.remove(selected);numberValid=invalidNumbers.isEmpty();needsCheck=true;}catch(NumberFormatException invalid){invalidNumbers.add(selected);numberValid=false;message="Enter a finite Number between -1000000 and 1000000.";}}
    private void remove(){if(selected==0){message="Reset removes the main Aon and its arguments.";return;}save();int[] parents;try{parents=AonLayout.parents(drawing());}catch(IllegalArgumentException invalid){parents=new int[glyphs.size()];for(int i=0;i<parents.length;i++)parents[i]=glyphs.get(i).parent();}var removed=new HashSet<Integer>();removed.add(selected);for(int i=selected+1;i<parents.length;i++)if(removed.contains(parents[i]))removed.add(i);var out=new ArrayList<AonDrawing.Glyph>();int[] remap=new int[glyphs.size()];Arrays.fill(remap,-1);
        int next=0;for(int i=0;i<glyphs.size();i++)if(!removed.contains(i))remap[i]=next++;
        for(int i=0;i<glyphs.size();i++)if(!removed.contains(i)){var g=glyphs.get(i);out.add(new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),g.strokes(),g.parent()<0?-1:remap[g.parent()],g.number()));}restore(new Edit(out,0));}
    private int at(AonDrawing.Point p){int best=-1;double size=Double.POSITIVE_INFINITY;for(int i=0;i<glyphs.size();i++){var g=glyphs.get(i);if(g.strokes().isEmpty())continue;var b=AonLayout.bounds(g);if(b.contains(p,b.diameter()*.12)&&b.diameter()<size){size=b.diameter();best=i;}}return best;}
    private int autoParent(AonDrawing.Point p){var eligible=new ArrayList<AonDrawing.Glyph>();for(int i=0;i<glyphs.size();i++){var g=glyphs.get(i);eligible.add(ids.containsKey(i)?g:new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),List.of()));}return AonLayout.parentAt(eligible,eligible.size(),p);}
    private int newArgument(AonDrawing.Point p,int parent){if(glyphs.size()>=AonDrawing.MAX_GLYPHS){message="Maximum of 16 arguments reached.";return -1;}glyphs.add(new AonDrawing.Glyph(false,p.x(),p.y(),.12,List.of(),parent,1));return glyphs.size()-1;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(placement>=0){if(button==0){resizePlacement(x,y);finishPlacement();}else if(button==1)cancelPlacement();return true;}
        if(super.mouseClicked(x,y,button))return true;if(!inCanvas(x,y))return false;var p=point(x,y);
        if(button==1){startPlacement(p);return true;}if(button!=0)return false;
        if(hasAltDown()){int hit=at(p);if(hit>=0){selected=hit;updateNumber();message="Selected "+names.getOrDefault(hit,"unrecognized Aon")+". Draw Argument explicitly attaches a child.";}return true;}
        save();
        if(forcedParent>=0){int hit=at(p);if(hit>forcedParent&&ids.containsKey(hit)){var g=glyphs.get(hit);glyphs.set(hit,new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),g.strokes(),forcedParent,g.number()));selected=hit;forcedParent=-1;needsCheck=true;updateNumber();message="Argument reattached to selected parent.";return true;}int created=newArgument(p,forcedParent);if(created<0)return true;selected=created;forcedParent=-1;}
        else if(!ids.containsKey(selected)&&!glyphs.get(selected).effect()){}
        else {int hit=at(p);if(hit>=0)selected=hit;else if(ids.containsKey(0)){int parent=autoParent(p);if(parent<0){history.removeLast();message=parent==-2?"Ambiguous parent. Alt-click a parent and use Draw Argument.":"Outside attachment bands. Move closer, or use Draw Argument.";return true;}int created=newArgument(p,-1);if(created<0)return true;selected=created;}else selected=0;}
        ids.remove(selected);names.remove(selected);probes.remove(selected);invalidNumbers.remove(selected);numberText.remove(selected);numberValid=invalidNumbers.isEmpty();pending=null;stroke=new ArrayList<>();stroke.add(p);updateNumber();return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(placement>=0){resizePlacement(x,y);return true;}if(button==0&&stroke!=null){var p=point(x,y);if(stroke.size()>=256)stroke=new ArrayList<>(AonRecognition.resample(stroke,128));stroke.add(p);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    private void finishStroke(){if(stroke==null)return;var raw=stroke;stroke=null;if(raw.size()==1)raw.add(raw.getFirst());var g=glyphs.get(selected);var lines=new ArrayList<>(g.strokes());lines.add(AonRecognition.resample(raw,24));try{var updated=new AonDrawing.Glyph(g.effect(),g.x(),g.y(),g.size(),lines,g.parent(),g.number());var b=AonLayout.bounds(updated);updated=new AonDrawing.Glyph(g.effect(),b.x(),b.y(),Math.min(1,b.diameter()),lines,g.parent(),g.number());var candidate=new ArrayList<>(glyphs);candidate.set(selected,updated);new AonDrawing(candidate);glyphs.set(selected,updated);probe(selected);needsCheck=true;}catch(IllegalArgumentException invalid){message=invalid.getMessage();}}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&stroke!=null){finishStroke();return true;}return super.mouseReleased(x,y,button);}
    private void probe(int index){if(glyphs.get(index).strokes().isEmpty()||minecraft==null||minecraft.player==null||minecraft.level==null||minecraft.getConnection()==null)return;UUID nonce=UUID.randomUUID();probes.put(index,nonce);PacketDistributor.sendToServer(new AonDorNetwork.Trace(dimension,slot,glyphs.get(index),nonce,index));}
    public void identified(AonDorNetwork.Identified result){if(!dimension.equals(result.dimension())||result.index()<0||result.index()>=glyphs.size()||!result.nonce().equals(probes.get(result.index())))return;if(result.id().isEmpty()){ids.remove(result.index());names.remove(result.index());}else{ids.put(result.index(),result.id());names.put(result.index(),result.name());}if(result.index()==selected)updateNumber();needsCheck=true;}
    private void check(){submitted=minecraft.level.getGameTime();pending=UUID.randomUUID();PacketDistributor.sendToServer(new AonDorNetwork.Check(pending,dimension,slot,drawing()));}
    public void result(AonDorNetwork.Result result){if(dimension.equals(result.dimension())&&result.nonce().equals(pending)){pending=null;message=result.message();}}
    private void cast(){if(!numberValid){minecraft.player.displayClientMessage(Component.literal("Invalid Number; spell cancelled."),true);return;}if(glyphs.get(0).strokes().isEmpty())return;PacketDistributor.sendToServer(new AonDorNetwork.Cast(new AonDorNetwork.Check(UUID.randomUUID(),dimension,slot,drawing())));}
    @Override public void tick(){if(minecraft.player==null||minecraft.level==null||!minecraft.player.isAlive()||minecraft.player.isSpectator()||!minecraft.isWindowActive()||!dimension.equals(minecraft.level.dimension().location().toString())||!AonDorService.POWER.equals(slot==0?PowerClient.primaryPower():PowerClient.secondaryPower())){onClose();return;}if(!AonDorClient.held(slot)){cancelPlacement();finishStroke();cast();onClose();return;}long now=minecraft.level.getGameTime();if(needsCheck&&stroke==null&&placement<0&&now-submitted>=10){needsCheck=false;check();}if(now%5==0)PacketDistributor.sendToServer(new AonDorNetwork.Preview(dimension,slot,true,projection()));if(pending!=null&&now-submitted>60){pending=null;message="Recognition timed out; edit or retry.";}if(pendingPick!=null&&now-pickSubmitted>60){pendingPick=null;message="Preview timed out; scroll to retry.";}}
    public AonDrawing.Ink projection(){var lines=new ArrayList<List<AonDrawing.Point>>();for(var g:glyphs)lines.addAll(g.strokes());if(stroke!=null&&stroke.size()>1)lines.add(AonRecognition.resample(stroke,24));if(lines.isEmpty())return new AonDrawing.Ink(List.of(),.1);double x=1,y=1,X=0,Y=0;for(var l:lines)for(var p:l){x=Math.min(x,p.x());y=Math.min(y,p.y());X=Math.max(X,p.x());Y=Math.max(Y,p.y());}final double cx=(x+X)/2,cy=(y+Y)/2,extent=Math.max(.001,Math.max(X-x,Y-y));var normalized=lines.stream().limit(125).map(l->AonRecognition.resample(l,24)).map(l->l.stream().map(p->new AonDrawing.Point(.5+(p.x()-cx)/extent*.8,.5+(p.y()-cy)/extent*.8)).toList()).toList();return new AonDrawing.Ink(normalized,Math.max(.1,Math.min(1,extent*1.5)));}
    private void startPlacement(AonDrawing.Point p){if(preview==null){message="Draw an Aon once to learn its reusable template.";return;}save();placementEdit=new Edit(List.copyOf(glyphs),selected);if(preview.glyph().effect()){if(glyphs.size()>1){message="Reset before replacing an effect with existing arguments.";return;}placement=0;}else{int parent=forcedParent>=0?forcedParent:autoParent(p);if(parent<0){message="Choose a parent with Alt-click, then Draw Argument.";return;}placement=newArgument(p,forcedParent>=0?parent:-1);if(placement<0)return;}forcedParent=-1;selected=placement;placementTemplate=preview.glyph();glyphs.set(placement,new AonDrawing.Glyph(preview.glyph().effect(),p.x(),p.y(),.1,List.of(),glyphs.get(placement).parent(),1));}
    private void resizePlacement(double x,double y){if(placement<0)return;var g=glyphs.get(placement);double radius=Math.hypot((x-canvasX)/scale()-g.x(),(y-canvasY)/scale()-g.y());double limit=2*Math.min(Math.min(g.x(),canvasWidth/(double)scale()-g.x()),Math.min(g.y(),canvasHeight/(double)scale()-g.y()));double size=Math.max(.02,Math.min(Math.max(.02,limit),radius*2));var lines=placementTemplate.strokes().stream().map(l->l.stream().map(p->new AonDrawing.Point(Math.max(0,Math.min(1,g.x()+(p.x()-.5)*size)),Math.max(0,Math.min(1,g.y()+(p.y()-.5)*size)))).toList()).toList();glyphs.set(placement,new AonDrawing.Glyph(g.effect(),g.x(),g.y(),size,lines,g.parent(),g.number()));}
    private void finishPlacement(){placement=-1;placementEdit=null;placementTemplate=null;probe(selected);needsCheck=true;updateNumber();}
    private void cancelPlacement(){if(placement<0)return;var edit=placementEdit;placement=-1;placementEdit=null;placementTemplate=null;if(edit!=null)restore(edit);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(vertical==0)return false;if(placement>=0||stroke!=null)return true;if(known.isEmpty()){message="Draw an Aon to learn it.";return true;}libraryIndex=Math.floorMod(libraryIndex+(vertical>0?-1:1),known.size());selectPreview();return true;}
    private void selectPreview(){preview=null;if(libraryIndex<0||libraryIndex>=known.size())return;var entry=known.get(libraryIndex);if(cache.containsKey(entry.id())){preview=cache.get(entry.id());pendingPick=null;return;}pendingPick=UUID.randomUUID();pickSubmitted=minecraft.level.getGameTime();PacketDistributor.sendToServer(new AonDorNetwork.Pick(pendingPick,dimension,slot,entry.id()));}
    public void library(AonDorNetwork.Library packet){if(!dimension.equals(packet.dimension()))return;String id=libraryIndex>=0&&libraryIndex<known.size()?known.get(libraryIndex).id():"";known=packet.aons();cache.clear();libraryIndex=-1;for(int i=0;i<known.size();i++)if(known.get(i).id().equals(id))libraryIndex=i;if(libraryIndex<0&&!known.isEmpty())libraryIndex=0;selectPreview();}
    public void stamp(AonDorNetwork.Stamp packet){if(!packet.nonce().equals(pendingPick)||!dimension.equals(packet.dimension())||libraryIndex<0||!known.get(libraryIndex).id().equals(packet.id()))return;cache.put(packet.id(),packet);preview=packet;pendingPick=null;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_Z&&hasControlDown()){undo();return true;}if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE&&placement>=0){cancelPlacement();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){cancelPlacement();finishStroke();super.onClose();}
    @Override public void removed(){cancelPlacement();finishStroke();AonDorClient.remember(this);if(minecraft.player!=null&&minecraft.level!=null&&minecraft.getConnection()!=null)PacketDistributor.sendToServer(new AonDorNetwork.Preview(dimension,slot,false,new AonDrawing.Glyph(true,.5,.5,List.of())));super.removed();}
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
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partial){}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        resizePlacement(mouseX,mouseY);graphics.fill(0,0,width,height,0x65080F19);graphics.drawCenteredString(font,title,width/2,8,0xFFFFFFFF);graphics.drawCenteredString(font,"Release to cast · Alt-click selects · relative spacing attaches arguments",width/2,24,0xFFDDDDDD);
        graphics.drawCenteredString(font,AonProgram.signature(ids.get(selected)),width/2,35,0xFFBBBBBB);
        graphics.fill(canvasX,canvasY,canvasX+canvasWidth,canvasY+canvasHeight,0x20000000);
        for(int i=16;i<canvasWidth;i+=16)graphics.fill(canvasX+i,canvasY,canvasX+i+1,canvasY+canvasHeight,0x28FFFFFF);for(int i=16;i<canvasHeight;i+=16)graphics.fill(canvasX,canvasY+i,canvasX+canvasWidth,canvasY+i+1,0x28FFFFFF);
        int[] parents;try{parents=AonLayout.parents(drawing());}catch(IllegalArgumentException invalid){parents=new int[glyphs.size()];for(int i=0;i<parents.length;i++)parents[i]=glyphs.get(i).parent();}
        for(int i=0;i<glyphs.size();i++){var g=glyphs.get(i);var b=AonLayout.bounds(g);if(i>0&&parents[i]>=0){var parent=AonLayout.bounds(glyphs.get(parents[i]));line(graphics,canvasX+parent.x()*scale(),canvasY+parent.y()*scale(),canvasX+b.x()*scale(),canvasY+b.y()*scale(),0x35FFFFFF);int order=AonLayout.children(drawing(),parents,parents[i]).indexOf(i)+1;graphics.drawString(font,"arg "+order,canvasX+(int)((parent.x()+b.x())*.5*scale()),canvasY+(int)((parent.y()+b.y())*.5*scale()),0xFF999999,false);}strokes(graphics,g.strokes(),canvasX,canvasY,scale());
            int x=canvasX+(int)(b.minX()*scale()),y=canvasY+(int)(b.minY()*scale());graphics.drawString(font,(i==selected?"> ":"")+(i+1)+" "+names.getOrDefault(i,g.effect()?"Main Aon":"Argument"),x,y-11,i==selected?0xFFFFFFFF:0xFFAAAAAA,false);
        }
        if(stroke!=null)strokes(graphics,List.of(stroke),canvasX,canvasY,scale());
        if(forcedParent>=0||stroke==null&&ids.containsKey(0)){int parent=forcedParent>=0?forcedParent:autoParent(point(mouseX,mouseY));if(parent>=0&&inCanvas(mouseX,mouseY)){var b=AonLayout.bounds(glyphs.get(parent));line(graphics,canvasX+b.x()*scale(),canvasY+b.y()*scale(),mouseX,mouseY,0x55FFFFFF);}}
        int side=width-136;graphics.fill(side,42,width-10,height-78,0xD0151515);graphics.drawString(font,"Learned: "+known.size(),side+5,48,0xFFFFFFFF,false);
        if(libraryIndex>=0&&libraryIndex<known.size()){int y=66;for(var text:font.split(Component.literal(known.get(libraryIndex).name()),114)){graphics.drawString(font,text,side+5,y,0xFFFFFFFF,false);y+=10;}if(preview!=null)strokes(graphics,preview.glyph().strokes(),side+18,108,88);}
        int y=height-36;for(var text:font.split(Component.literal(message),width-24)){graphics.drawString(font,text,12,y,0xFFFFFFFF,false);y+=10;if(y>height-8)break;}super.render(graphics,mouseX,mouseY,partial);
    }
}
