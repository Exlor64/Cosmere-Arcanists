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
   var transmitted=pattern.strokes().stream().map(points->AonRecognition.resample(points,24).stream().map(q->p((double)(float)q.x(),(double)(float)q.y())).toList()).toList();
   check(AonRecognition.assess(new Glyph(true,.5,.5,transmitted),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018).precise());
   var reversedLines=new ArrayList<List<Point>>();
   for(var line:pattern.strokes()){var reverse=new ArrayList<Point>(line);Collections.reverse(reverse);reversedLines.add(reverse);}Collections.reverse(reversedLines);
   var fallback=AonRecognition.assess(new Glyph(true,.5,.5,reversedLines),patterns,AonRecognition.Tolerance.defaults(.12,.025),.065,.018);
   check(fallback.template()==pattern&&!fallback.precise()&&fallback.potency(.75)==.75);
   var split=new ArrayList<List<Point>>();
   // Split only the framework so the drawing remains within the network stroke budget.
   var framework=pattern.strokes().get(0);for(int j=1;j<framework.size();j++)split.add(List.of(framework.get(j-1),framework.get(j)));split.addAll(pattern.strokes().subList(1,pattern.strokes().size()));
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
  System.out.println("PASS: "+checks+" production Aon recognition/order/bounds checks (Java17; no Minecraft runtime).");
 }
}
'''
patterns=[]
for resource in sorted((ROOT/'src/main/resources/data/cosmerearcanists/aondor/aons').glob('*.json')):
    data=json.loads(resource.read_text())
    lines=','.join('List.of('+','.join(f'p({x},{y})' for x,y in line)+')' for line in data['strokes'])
    patterns.append(f'patterns.add(new AonRecognition.Template("cosmerearcanists:{resource.stem}","{data["name"]}",true,List.of({lines})));')
assert len(patterns)==3, 'The drawing test must ship exactly three Aons'
SOURCE=SOURCE.replace('/* RESOURCE_PATTERNS */','\n'.join(patterns))
with tempfile.TemporaryDirectory() as tmp:
    source=Path(tmp)/'VerifyAonDor.java';source.write_text(SOURCE)
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,
        str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonDrawing.java'),
        str(ROOT/'src/main/java/dev/cosmerearcanists/aondor/AonRecognition.java'),str(source)],check=True)
    subprocess.run(['java','-cp',tmp,'VerifyAonDor'],check=True)
