#!/usr/bin/env python3
"""Compile and exercise actual ordered-stroke recognition and clockwise argument policy."""
from pathlib import Path
import subprocess
import json
import tempfile
ROOT=Path(__file__).resolve().parents[1]
SOURCE=r'''
import dev.cosmerearcanists.aondor.*;
import java.util.*;
import static dev.cosmerearcanists.aondor.AonDrawing.*;
public class VerifyAonDor {
 static int checks;
 static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
 static void bad(Runnable action){try{action.run();}catch(IllegalArgumentException expected){check(true);return;}throw new AssertionError("Accepted invalid input");}
 static Point p(double x,double y){return new Point(x,y);}
 static Glyph placed(AonRecognition.Template template,double x,double y,double size,int parent,double number){
  var source=template.id().equals("cosmerearcanists:number")?AonNumerals.strokes((int)number):template.strokes();
  var lines=source.stream().map(line->line.stream().map(q->p(x+(q.x()-.5)*size,y+(q.y()-.5)*size)).toList()).toList();return new Glyph(template.effect(),x,y,size,lines,parent,number);
 }
 public static void main(String[] args){
  var path=List.of(p(0,0),p(0,1),p(1,1));
  var template=new AonRecognition.Template("test:l",true,List.of(path));
  var glyph=new Glyph(true,.5,.5,List.of(path));
  check(AonRecognition.recognize(glyph,List.of(template),.12,.025).template()==template);
  var backwards=new ArrayList<>(path);Collections.reverse(backwards);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(backwards)),List.of(template),.12,.025).template()==null);
  var two=List.of(List.of(p(0,0),p(1,0)),List.of(p(1,0),p(1,1)));
  var ordered=new AonRecognition.Template("test:ordered",true,two);
  check(AonRecognition.error(two,ordered)==0);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(two.get(1),two.get(0))),List.of(ordered),.12,.025).template()==null);
  var duplicate=new AonRecognition.Template("test:ambiguous",true,List.of(path));
  check(AonRecognition.recognize(glyph,List.of(template,duplicate),.12,.025).ambiguous());
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of()),List.of(template),.12,.025).template()==null);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(List.of(p(.5,.5),p(.5,.5)))),List.of(template),.12,.025).template()==null);
  for(int i=1;i<=20;i++){
   double scale=.02+i*.02,offset=.2;
   var transformed=path.stream().map(q->p(offset+q.x()*scale,offset+q.y()*scale)).toList();
   check(AonRecognition.error(List.of(transformed),template)<1e-10);
  }
  var up=new Glyph(false,.5,.1,List.of(path));var right=new Glyph(false,.9,.5,List.of(path));
  var down=new Glyph(false,.5,.9,List.of(path));var left=new Glyph(false,.1,.5,List.of(path));
  var drawing=new AonDrawing(List.of(left,down,glyph,right,up));
  check(drawing.arguments().equals(List.of(up,right,down,left)));
  var nearUp=new Glyph(false,.5,.3,List.of(path));
  check(new AonDrawing(List.of(glyph,up,nearUp)).arguments().equals(List.of(nearUp,up)));
  bad(()->p(Double.NaN,0));bad(()->p(1.01,0));bad(()->new AonDrawing(List.of()));
  bad(()->new AonDrawing(List.of(glyph,glyph)));bad(()->new AonDrawing(List.of(up)));
  bad(()->new Glyph(true,.5,.5,List.of(List.of(p(.5,.5)))));
  var mutable=new ArrayList<Point>(path);var protectedGlyph=new Glyph(true,.5,.5,List.of(mutable));mutable.clear();check(protectedGlyph.strokes().get(0).size()==3);
  var oversized=new ArrayList<Glyph>();for(int i=0;i<18;i++)oversized.add(new Glyph(i==0,.5,.5,List.of()));bad(()->new AonDrawing(oversized));
  var many=new ArrayList<Glyph>();var sampledLine=AonRecognition.resample(path,48);var strokes=new ArrayList<List<Point>>();for(int i=0;i<16;i++)strokes.add(sampledLine);
  for(int i=0;i<5;i++)many.add(new Glyph(i==0,.5,.5,strokes));bad(()->new AonDrawing(many));
  for(int i=0;i<100;i++){
   double scale=.011+i*.007;var small=path.stream().map(q->p(.05+q.x()*scale,.05+q.y()*scale)).toList();
   check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(small)),List.of(template),.12,.025).template()==template);
  }
  var tiny=path.stream().map(q->p(.05+q.x()*.003,.05+q.y()*.003)).toList();
  var permissive=new AonRecognition.Tolerance(.12,.025,.001,.55,.25,.1,.1);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(tiny)),List.of(template),permissive).template()==template);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(tiny)),List.of(template),.12,.025).template()==null);
  var wobbly=List.of(p(.1,.1),p(.101,.5),p(.1,.9),p(.5,.899),p(.9,.9));
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(wobbly)),List.of(template),.12,.025).template()==template);
  var smallGlyph=new Glyph(true,.5,.5,.04,List.of(path));var bigGlyph=new Glyph(true,.5,.5,.9,List.of(path));
  check(AonRecognition.recognize(smallGlyph,List.of(template),.12,.025).template()==AonRecognition.recognize(bigGlyph,List.of(template),.12,.025).template());
  var square=List.of(p(0,0),p(1,0),p(1,1),p(0,1),p(0,0));var squareTemplate=new AonRecognition.Template("test:square",true,List.of(square));
  var reverseSquare=new ArrayList<Point>(square);Collections.reverse(reverseSquare);
  check(AonRecognition.recognize(new Glyph(true,.5,.5,List.of(reverseSquare)),List.of(squareTemplate),.12,.025).template()==null);
  bad(()->new Glyph(true,.5,.5,Double.NaN,List.of(path)));bad(()->new Glyph(true,.5,.5,1.1,List.of(path)));

  var patterns=new ArrayList<AonRecognition.Template>();
  /* RESOURCE_PATTERNS */
  for(var pattern:patterns){
   var exact=new Glyph(true,.5,.5,pattern.strokes());
   var precise=AonRecognition.assess(exact,patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(precise.template()==pattern&&precise.precise()&&precise.potency(.75)==1);
   for(int count=1;count<pattern.strokes().size();count++){
    var partial=new Glyph(pattern.effect(),.5,.5,pattern.strokes().subList(0,count));
    var partialMatch=AonRecognition.assess(partial,patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
    check(partialMatch.template()==null||partialMatch.template()==pattern);
   }
   var transmitted=pattern.strokes().stream().map(points->AonRecognition.resample(points,24).stream().map(q->p((double)(float)q.x(),(double)(float)q.y())).toList()).toList();
   check(AonRecognition.assess(new Glyph(true,.5,.5,transmitted),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).precise());
   if(pattern.id().endsWith(":heat")){
    var narrower=pattern.strokes().stream().map(line->line.stream().map(q->p(.05+q.x()*.85,q.y())).toList()).toList();
    var handDrawn=new Glyph(true,.5,.5,narrower);
    check(AonRecognition.error(narrower,pattern)<.12);
    check(AonRecognition.shapeError(narrower,pattern,.01)>.065);
    var result=AonRecognition.assess(handDrawn,patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
    check(result.template()==pattern&&result.precise());
    var duplicateHeat=new AonRecognition.Template("test:heat_copy",true,pattern.strokes());
    check(AonRecognition.assess(handDrawn,List.of(pattern,duplicateHeat),AonRecognition.Tolerance.defaults(.12,.025),.065,.018).ambiguous());
   }
   var reversedLines=new ArrayList<List<Point>>();
   for(var line:pattern.strokes()){var reverse=new ArrayList<Point>(line);Collections.reverse(reverse);reversedLines.add(reverse);}Collections.reverse(reversedLines);
   var fallback=AonRecognition.assess(new Glyph(true,.5,.5,reversedLines),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(fallback.template()==pattern&&!fallback.precise()&&fallback.potency(.75)==.75);
   var split=new ArrayList<List<Point>>();
   // Split only the framework so the drawing remains within the network stroke budget.
   var framework=pattern.strokes().get(0);int middle=Math.max(1,framework.size()/2);split.add(framework.subList(0,middle+1));if(middle<framework.size()-1)split.add(framework.subList(middle,framework.size()));split.addAll(pattern.strokes().subList(1,pattern.strokes().size()));
   check(AonRecognition.assess(new Glyph(true,.5,.5,split),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==pattern);
   for(int trial=0;trial<30;trial++){
    final int seed=trial;var noise=pattern.strokes().stream().map(line->line.stream().map(q->p(.15+.65*q.x()+.004*Math.sin(q.y()*29+seed),.15+.65*q.y()+.004*Math.cos(q.x()*31+seed))).toList()).toList();
    check(AonRecognition.assess(new Glyph(true,.5,.5,noise),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==pattern);
   }
   var duplicateShape=new AonRecognition.Template("test:duplicate_shape",true,pattern.strokes());
   check(AonRecognition.assess(exact,List.of(pattern,duplicateShape),AonRecognition.Tolerance.defaults(.12,.025),.065,.018).ambiguous());
   for(var other:patterns)if(other!=pattern)check(AonRecognition.shapeError(pattern.strokes(),other,.01)>.065+.018);
  }
  check(AonRecognition.assess(new Glyph(true,.5,.5,List.of(List.of(p(.1,.1),p(.9,.9)))),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==null);

  var first=List.of(3.0,1.0,3.0,2.0);var second=List.of(2.0,3.0,4.0);var third=List.of(3.0,5.0);
  check(AonLists.sets("intersection",List.of(first,second,third)).equals(List.of(3.0)));
  check(AonLists.sets("union",List.of(first,second,third)).equals(List.of(3.0,1.0,2.0,4.0,5.0)));
  check(AonLists.sets("difference",List.of(first,second,third)).equals(List.of(1.0)));
  check(AonLists.sets("intersection",List.of(first,List.<Double>of())).isEmpty());
  check(AonLists.sets("difference",List.of(first,first)).isEmpty());
  check(AonLists.removeAt(first,2).equals(List.of(3.0,1.0,2.0))&&first.equals(List.of(3.0,1.0,3.0,2.0)));
  check(AonLists.at(first,0)==3.0&&AonLists.at(first,3)==2.0);
  check(AonLists.append(first,List.of(7.0,8.0)).equals(List.of(3.0,1.0,3.0,2.0,7.0,8.0)));
  check(AonLists.create(List.of()).isEmpty());
  for(double index:new double[]{-1,.5,4,Double.NaN,Double.POSITIVE_INFINITY})bad(()->AonLists.at(first,index));
  bad(()->AonLists.removeAt(List.of(),0));bad(()->AonLists.create(Collections.nCopies(129,0)));bad(()->AonLists.append(Collections.nCopies(128,0),List.of(1)));bad(()->AonLists.sets("union",List.of(first)));
  try{AonLists.create(first).add(9.0);throw new AssertionError("Mutable list");}catch(UnsupportedOperationException expected){check(true);}
  for(int i=0;i<80;i++){
   var listLeft=new ArrayList<Integer>();var listRight=new ArrayList<Integer>();for(int j=0;j<20;j++){listLeft.add((i+j*3)%11);listRight.add((i*2+j*5)%13);}
   var common=AonLists.sets("intersection",List.of(listLeft,listRight));var union=AonLists.sets("union",List.of(listLeft,listRight));var difference=AonLists.sets("difference",List.of(listLeft,listRight));
   check(new HashSet<>(common).size()==common.size()&&common.stream().allMatch(n->listLeft.contains(n)&&listRight.contains(n)));
   check(union.containsAll(listLeft)&&union.containsAll(listRight)&&new HashSet<>(union).size()==union.size());
   check(difference.stream().allMatch(n->listLeft.contains(n)&&!listRight.contains(n))&&listLeft.stream().filter(n->!listRight.contains(n)).allMatch(difference::contains));
  }
  var listNode=new AonProgram.Node("test",0,AonProgram.Type.LIST,List.of());var boolNode=new AonProgram.Node("test",0,AonProgram.Type.BOOLEAN,List.of());var numNode=new AonProgram.Node("test",0,AonProgram.Type.NUMBER,List.of());var entityNode=new AonProgram.Node("test",0,AonProgram.Type.ENTITY,List.of());
  check(AonLists.resultType("list",List.of(numNode,entityNode))==AonProgram.Type.LIST);
  for(String key:List.of("union","intersection","difference"))check(AonLists.resultType(key,List.of(listNode,listNode,listNode))==AonProgram.Type.LIST);
  check(AonLists.resultType("at",List.of(listNode,numNode))==AonProgram.Type.VALUE);
  check(AonLists.resultType("contains",List.of(listNode,numNode))==AonProgram.Type.BOOLEAN);
  check(AonLists.resultType("if",List.of(boolNode,numNode,numNode))==AonProgram.Type.NUMBER);
  check(AonLists.resultType("if",List.of(boolNode,numNode,entityNode))==AonProgram.Type.VALUE);
  for(String key:List.of("and","or"))check(AonLists.resultType(key,List.of(boolNode,boolNode,boolNode))==AonProgram.Type.BOOLEAN);
  check(AonLists.resultType("not",List.of(boolNode))==AonProgram.Type.BOOLEAN);
  bad(()->AonLists.resultType("if",List.of(numNode,numNode,numNode)));bad(()->AonLists.resultType("append",List.of(numNode,numNode)));bad(()->AonLists.resultType("intersection",List.of(listNode,numNode)));bad(()->AonLists.resultType("not",List.of(numNode)));
  var byId=new HashMap<String,AonRecognition.Template>();for(var entry:patterns)byId.put(entry.id().substring(17),entry);
  var rootForce=placed(byId.get("force"),.5,.7,.16,-1,1);
  var explicit=new AonDrawing(List.of(rootForce,placed(byId.get("caster"),.5,.3,.08,0,1),placed(byId.get("look_direction"),.8,.7,.08,0,1)));
  var program=AonProgram.compile(explicit,patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75);
  check(program.root().arguments().size()==2&&program.root().arguments().get(0).type()==AonProgram.Type.ENTITY&&program.root().arguments().get(1).type()==AonProgram.Type.DIRECTION);
  var nestedGlyphs=new ArrayList<Glyph>(List.of(rootForce,placed(byId.get("entities_in"),.5,.4,.1,0,1),placed(byId.get("sphere"),.5,.2,.08,1,1),placed(byId.get("position_of"),.5,.1,.05,2,1),placed(byId.get("caster"),.5,.04,.04,3,1),placed(byId.get("number"),.7,.2,.08,2,5)));
  var nested=AonProgram.compile(new AonDrawing(nestedGlyphs),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75);
  check(nested.root().arguments().get(0).type()==AonProgram.Type.ENTITIES&&nested.root().arguments().get(0).arguments().get(0).arguments().get(1).number()==5);
  nestedGlyphs.set(5,placed(byId.get("caster"),.7,.2,.08,2,1));bad(()->AonProgram.compile(new AonDrawing(nestedGlyphs),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75));
  for(String filter:List.of("players","mobs","nearest","exclude")){
   var filtered=new ArrayList<Glyph>(List.of(placed(byId.get("heat"),.5,.85,.08,-1,1),placed(byId.get(filter),.5,.65,.08,0,1),placed(byId.get("entities_in"),.5,.45,.08,1,1),placed(byId.get("sphere"),.5,.3,.08,2,1),placed(byId.get("position_of"),.5,.18,.06,3,1),placed(byId.get("caster"),.5,.06,.05,4,1),placed(byId.get("number"),.7,.3,.07,3,3)));
   if(filter.equals("exclude"))filtered.add(placed(byId.get("caster"),.8,.65,.07,1,1));
   check(AonProgram.compile(new AonDrawing(filtered),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==(filter.equals("nearest")?AonProgram.Type.ENTITY:AonProgram.Type.ENTITIES));
   filtered.set(2,placed(byId.get("number"),.5,.45,.08,1,2));
   bad(()->AonProgram.compile(new AonDrawing(filtered),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75));
  }
  var memoryInk=List.of(placed(byId.get("remember"),.5,.7,.08,-1,1),placed(byId.get("number"),.5,.4,.08,0,2),placed(byId.get("caster"),.8,.7,.08,0,1));
  check(AonProgram.compile(new AonDrawing(memoryInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().type()==AonProgram.Type.EFFECT);
  var recallInk=List.of(placed(byId.get("heat"),.5,.7,.08,-1,1),placed(byId.get("recall"),.5,.4,.08,0,1),placed(byId.get("number"),.5,.2,.08,1,2));
  check(AonProgram.compile(new AonDrawing(recallInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==AonProgram.Type.VALUE);
  var createInk=List.of(placed(byId.get("remember"),.5,.8,.06,-1,1),placed(byId.get("number"),.5,.6,.05,0,0),placed(byId.get("list"),.8,.8,.06,0,1),placed(byId.get("number"),.8,.6,.05,2,1),placed(byId.get("caster"),.95,.8,.05,2,1));
  check(AonProgram.compile(new AonDrawing(createInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(1).type()==AonProgram.Type.LIST);
  var targetInk=List.of(placed(byId.get("force"),.5,.8,.06,-1,1),placed(byId.get("list"),.5,.6,.05,0,1),placed(byId.get("caster"),.5,.4,.05,1,1));
  check(AonProgram.compile(new AonDrawing(targetInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==AonProgram.Type.LIST);
  var skipHeat=new ArrayList<Glyph>(List.of(placed(byId.get("heat"),.5,.6,.08,-1,1),placed(byId.get("skip"),.53,.3,.08,0,1),placed(byId.get("number"),.8,.6,.08,0,8)));
  check(AonProgram.compile(new AonDrawing(skipHeat),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==AonProgram.Type.SKIP);
  skipHeat.set(2,placed(byId.get("caster"),.8,.6,.08,0,1));bad(()->AonProgram.compile(new AonDrawing(skipHeat),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75));
  var criticalSkip=List.of(placed(byId.get("light"),.5,.8,.06,-1,1),placed(byId.get("position_of"),.5,.6,.06,0,1),placed(byId.get("skip"),.5,.4,.06,1,1));bad(()->AonProgram.compile(new AonDrawing(criticalSkip),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75));
  var skipNode=new AonProgram.Node("cosmerearcanists:skip",0,AonProgram.Type.SKIP,List.of());bad(()->AonLists.resultType("list",List.of(skipNode)));bad(()->AonLists.resultType("if",List.of(boolNode,skipNode,numNode)));
  check(AonHeatPower.effective(8,.75,64)==6&&AonHeatPower.effective(0,4,64)==0);
  for(double power:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,65})bad(()->AonHeatPower.effective(power,1,64));
  check(!AonHeatPower.reaches(.75,1)&&AonHeatPower.reaches(4,4));
  check(AonHeatPower.damage(1,2,1,40)==0&&AonHeatPower.damage(8,2,1,40)==8&&AonHeatPower.damage(64,2,1,40)==40);
  for(double power:new double[]{8,16,64,128,1024})check(AonHeatPower.effective(power,1,0)==power&&AonHeatPower.damage(power,2,1,0)==power);
  bad(()->AonHeatPower.effective(Double.MAX_VALUE,2,0));bad(()->AonHeatPower.damage(Float.MAX_VALUE,2,2,0));
  var lightEntityInk=List.of(placed(byId.get("light"),.5,.6,.08,-1,1),placed(byId.get("caster"),.5,.3,.08,0,1));
  check(AonProgram.compile(new AonDrawing(lightEntityInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==AonProgram.Type.ENTITY);
  var lightListInk=List.of(placed(byId.get("light"),.5,.6,.08,-1,1),placed(byId.get("list"),.5,.3,.08,0,1),placed(byId.get("caster"),.5,.1,.08,1,1));
  check(AonProgram.compile(new AonDrawing(lightListInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().arguments().get(0).type()==AonProgram.Type.LIST);
  check(AonCost.connected(1,3,3)&&!AonCost.connected(1,2.999,3)&&AonCost.connected(4,12,3));
  check(!AonCost.connected(0,3,3)&&!AonCost.connected(Double.NaN,3,3)&&!AonCost.connected(1,Double.POSITIVE_INFINITY,3));
  var work=new AonCost.Builder(2);work.add("force",3);work.add("force",4);work.add("query",.5);check(work.quote().total()==15&&work.quote().lines().size()==2);bad(()->work.add("bad",Double.NaN));bad(()->work.add("bad",-1));
  var overflow=new AonCost.Builder(2);overflow.add("bad",Double.MAX_VALUE);bad(overflow::quote);
  for(String comparator:List.of("equal","greater_than","less_than"))check(AonLists.resultType(comparator,List.of(numNode,numNode))==AonProgram.Type.BOOLEAN);
  bad(()->AonLists.resultType("greater_than",List.of(entityNode,numNode)));
  var connectionInk=List.of(placed(byId.get("connection"),.5,.6,.08,-1,1),placed(byId.get("number"),.55,.3,.06,0,3),placed(byId.get("number"),.8,.6,.06,0,5));
  check(AonProgram.compile(new AonDrawing(connectionInk),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75).root().type()==AonProgram.Type.EFFECT);
  var cyclic=new AonDrawing(List.of(rootForce,new Glyph(false,.2,.2,.1,byId.get("caster").strokes(),1,1)));bad(()->AonLayout.parents(cyclic));
  var a=placed(byId.get("gaze_block"),.35,.5,.2,-1,1);var b=placed(byId.get("gaze_block"),.65,.5,.2,-1,1);
  check(AonLayout.parentAt(List.of(a,b),2,p(.5,.2))==-2);
  check(AonLayout.parentAt(List.of(a,b),2,p(.35,.2))==0);
  check(AonLayout.parentAt(List.of(a),1,p(.9,.1))==-1);
  var tinyA=placed(byId.get("gaze_block"),.175,.25,.1,-1,1);
  check(AonLayout.parentAt(List.of(tinyA),1,p(.175,.1))==0);
  var explicitFar=new AonDrawing(List.of(rootForce,placed(byId.get("caster"),.05,.05,.05,0,1)));
  check(AonLayout.parents(explicitFar)[1]==0);

  for(int n=0;n<=10;n++){
   var numeral=new Glyph(false,.5,.5,.3,AonNumerals.strokes(n),0,98765);
   var match=AonRecognition.assess(numeral,patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(match.template()!=null&&match.template().id().equals("cosmerearcanists:number")&&match.number()==n&&match.precise());
   var narrow=numeral.strokes().stream().map(stroke->stroke.stream().map(q->p(.05+q.x()*.85,q.y())).toList()).toList();
   var narrowMatch=AonRecognition.assess(new Glyph(false,.5,.5,narrow),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(narrowMatch.template()!=null&&narrowMatch.number()==n&&narrowMatch.precise());
   var reverse=new ArrayList<List<Point>>();for(var circle:numeral.strokes()){var strokeCopy=new ArrayList<Point>(circle);Collections.reverse(strokeCopy);reverse.add(strokeCopy);}Collections.reverse(reverse);
   var fallback=AonRecognition.assess(new Glyph(false,.5,.5,reverse),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(fallback.template()!=null&&fallback.number()==n&&!fallback.precise());
   var scaled=numeral.strokes().stream().map(stroke->stroke.stream().map(q->p(.1+q.x()*.28,.2+q.y()*.28)).toList()).toList();
   check(AonNumerals.read(new Glyph(false,.5,.5,scaled),AonRecognition.Tolerance.defaults(.12,.025)).value()==n);
  }
  var tooMany=new ArrayList<List<Point>>(AonNumerals.strokes(10));tooMany.add(AonNumerals.strokes(1).get(2));
  check(AonNumerals.read(new Glyph(false,.5,.5,tooMany),AonRecognition.Tolerance.defaults(.12,.025))==null);
  var missingCenter=new ArrayList<List<Point>>(AonNumerals.strokes(3));missingCenter.remove(1);check(AonNumerals.read(new Glyph(false,.5,.5,missingCenter),AonRecognition.Tolerance.defaults(.12,.025))==null);
  var duplicateRing=new ArrayList<List<Point>>(AonNumerals.strokes(2));duplicateRing.add(duplicateRing.get(2));check(AonNumerals.read(new Glyph(false,.5,.5,duplicateRing),AonRecognition.Tolerance.defaults(.12,.025))==null);
  check(AonRecognition.assess(new Glyph(false,.5,.5,tooMany),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==null);
  check(AonRecognition.assess(new Glyph(false,.5,.5,missingCenter),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==null);
  check(AonRecognition.assess(new Glyph(false,.5,.5,duplicateRing),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).template()==null);

  check(AonMath.apply("add",10.0,7.0).equals(17.0));
  check(AonMath.apply("subtract",2.0,7.0).equals(-5.0));
  check(AonMath.apply("multiply",7.0,8.0).equals(56.0));
  check(AonMath.apply("divide",7.0,2.0).equals(3.5));
  var v=new AonMath.Vector(1,2,3);var w=new AonMath.Vector(4,5,6);
  check(AonMath.apply("add",v,w).equals(new AonMath.Vector(5,7,9)));
  check(AonMath.apply("subtract",w,v).equals(new AonMath.Vector(3,3,3)));
  check(AonMath.apply("multiply",v,2.0).equals(new AonMath.Vector(2,4,6)));
  check(AonMath.apply("multiply",2.0,v).equals(new AonMath.Vector(2,4,6)));
  check(AonMath.apply("divide",v,2.0).equals(new AonMath.Vector(.5,1,1.5)));
  check(AonMath.apply("length",new AonMath.Vector(3,4,0)).equals(5.0));
  check(AonMath.apply("normalize",new AonMath.Vector(0,0,4)).equals(new AonMath.Vector(0,0,1)));
  check(AonMath.apply("normalize",new AonMath.Vector(Double.MIN_VALUE,0,0)).equals(new AonMath.Vector(1,0,0)));
  bad(()->AonMath.apply("divide",1.0,0.0));bad(()->AonMath.apply("divide",v,-0.0));bad(()->AonMath.apply("divide",1.0,v));
  bad(()->AonMath.apply("normalize",new AonMath.Vector(0,0,0)));bad(()->AonMath.apply("multiply",Double.MAX_VALUE,2.0));bad(()->new AonMath.Vector(Double.NaN,0,0));
  var scalar=new AonProgram.Node("cosmerearcanists:number",2,AonProgram.Type.NUMBER,List.of());
  var vecNode=new AonProgram.Node("cosmerearcanists:vector",0,AonProgram.Type.VECTOR,List.of(scalar,scalar,scalar));
  var dirNode=new AonProgram.Node("cosmerearcanists:look_direction",0,AonProgram.Type.DIRECTION,List.of());
  var posNode=new AonProgram.Node("cosmerearcanists:position_of",0,AonProgram.Type.POSITION,List.of());
  check(AonMath.resultType("multiply",List.of(dirNode,scalar))==AonProgram.Type.VECTOR);
  check(AonMath.resultType("add",List.of(posNode,vecNode))==AonProgram.Type.POSITION);
  check(AonMath.resultType("add",List.of(vecNode,posNode))==AonProgram.Type.POSITION);
  check(AonMath.resultType("subtract",List.of(posNode,posNode))==AonProgram.Type.VECTOR);
  check(AonMath.resultType("subtract",List.of(posNode,dirNode))==AonProgram.Type.POSITION);
  bad(()->AonMath.resultType("add",List.of(posNode,posNode)));bad(()->AonMath.resultType("divide",List.of(scalar,vecNode)));bad(()->AonMath.resultType("multiply",List.of(vecNode,vecNode)));
  var dynamicDivideZero=new AonProgram.Node("cosmerearcanists:divide",0,AonProgram.Type.VECTOR,List.of(dirNode,new AonProgram.Node("cosmerearcanists:number",0,AonProgram.Type.NUMBER,List.of())));
  bad(()->AonMath.constant(dynamicDivideZero));
  // Compile actual drawn operator/Number templates, ordered clockwise, inside a Sphere.
  var mathDrawing=new ArrayList<Glyph>(List.of(rootForce,placed(byId.get("entities_in"),.5,.45,.07,0,1),placed(byId.get("sphere"),.5,.28,.07,1,1),placed(byId.get("position_of"),.5,.15,.04,2,1),placed(byId.get("caster"),.5,.07,.04,3,1),placed(byId.get("divide"),.72,.28,.07,2,1),placed(byId.get("number"),.72,.15,.07,5,7),placed(byId.get("number"),.85,.28,.07,5,2)));
  var mathProgram=AonProgram.compile(new AonDrawing(mathDrawing),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75);
  check(mathProgram.calculations().contains("Divide = 3.5"));
  var division=mathProgram.root().arguments().get(0).arguments().get(0).arguments().get(1);
  check(AonMath.constant(division).equals(3.5));
  mathDrawing.set(7,placed(byId.get("number"),.85,.28,.07,5,0));bad(()->AonProgram.compile(new AonDrawing(mathDrawing),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018,.75));
  var rng=new Random(20261106);for(int i=0;i<100;i++){
   var sample=new AonMath.Vector(rng.nextDouble()*20-10,rng.nextDouble()*20-10,rng.nextDouble()*20-10);double factor=.1+rng.nextDouble()*9;
   var restored=(AonMath.Vector)AonMath.apply("divide",AonMath.apply("multiply",sample,factor),factor);
   check(Math.abs(restored.x()-sample.x())<1e-12&&Math.abs(restored.y()-sample.y())<1e-12&&Math.abs(restored.z()-sample.z())<1e-12);
  }
  System.out.println("PASS: "+checks+" production Aon recognition/order/bounds checks (Java17; no Minecraft runtime).");
 }
}
'''
patterns=[]
for resource in sorted((ROOT/'src/main/resources/data/cosmerearcanists/aondor/aons').glob('*.json')):
    data=json.loads(resource.read_text())
    lines=','.join('List.of('+','.join(f'p({x},{y})' for x,y in line)+')' for line in data['strokes'])
    patterns.append(f'patterns.add(new AonRecognition.Template("cosmerearcanists:{resource.stem}","{data["name"]}",{str(data["kind"]=="effect").lower()},List.of({lines})));')
assert len(patterns)==42, 'Expected five effects and thirty-seven argument Aons'
SOURCE=SOURCE.replace('/* RESOURCE_PATTERNS */','\n'.join(patterns))
with tempfile.TemporaryDirectory() as tmp:
    source=Path(tmp)/'VerifyAonDor.java';source.write_text(SOURCE)
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,
        str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonDrawing.java'),
        str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonRecognition.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonNumerals.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonLayout.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonProgram.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonMath.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonLists.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonHeatPower.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonCost.java'),str(source)],check=True)
    subprocess.run(['java','-cp',tmp,'VerifyAonDor'],check=True)
