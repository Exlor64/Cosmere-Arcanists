#!/usr/bin/env python3
"""Validate production natural-trait formulas and configurable sensory profiles without Minecraft."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[1]
SOURCE=r'''
import java.util.*;import dev.cosmerearcanists.hemalurgy.HemalurgicTraits;
class CheckTraits {
 static int checks;static void check(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}static void near(double a,double b){check(Math.abs(a-b)<1e-9);}
 public static void main(String[] args){
  near(HemalurgicTraits.iron(0),1);near(HemalurgicTraits.iron(1),1.5);near(HemalurgicTraits.iron(4),3);near(HemalurgicTraits.iron(16),9);near(HemalurgicTraits.iron(40),15);near(HemalurgicTraits.iron(100),30);near(HemalurgicTraits.iron(16-1e-8),9-5e-9);near(HemalurgicTraits.iron(16+1e-8),9+2.5e-9);
  for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})near(HemalurgicTraits.iron(invalid),0);
  double previous=0;for(int damage=0;damage<=2048;damage++){double n=HemalurgicTraits.iron(damage);check(n>previous&&Double.isFinite(n));previous=n;}
  var normal=new HemalurgicTraits.Senses(.2,.2,.1);var monster=new HemalurgicTraits.Senses(.35,.4,.25);var defaults=HemalurgicTraits.defaults();
  var ids=new HashSet<String>();for(String entry:defaults){var p=HemalurgicTraits.parse(entry);check(p!=null);check(ids.add(p.entity()));var s=p.ratios().scale(.15);check(s.maximum()>0&&s.maximum()<.15);check(s.sight()>=0&&s.hearing()>=0&&s.touch()>=0);}
  var bat=HemalurgicTraits.select("minecraft:bat",normal,defaults).scale(.15);var warden=HemalurgicTraits.select("minecraft:warden",monster,defaults).scale(.15);var cow=HemalurgicTraits.select("minecraft:cow",normal,defaults).scale(.15);var zombie=HemalurgicTraits.select("minecraft:zombie",monster,defaults).scale(.15);
  near(bat.hearing(),.12);near(cow.hearing(),.03);near(warden.sight(),0);near(warden.hearing(),.1425);check(bat.hearing()>cow.hearing()&&warden.hearing()>bat.hearing()&&zombie.hearing()>cow.hearing());
  var spider=HemalurgicTraits.select("minecraft:spider",monster,defaults).scale(.15);check(spider.touch()>zombie.touch());
  check(HemalurgicTraits.select("other:creature",monster,defaults).equals(monster));check(HemalurgicTraits.select("other:creature",normal,defaults).equals(normal));
  check(HemalurgicTraits.select("other:creature",normal,List.of("other:creature,0.1,0.8,0.2")).hearing()==.8);check(HemalurgicTraits.select("other:creature",normal,List.of("other:creature,0.1,0.8,0.2","other:creature,0.1,0.9,0.2")).hearing()==.9);
  for(String bad:new String[]{"bad","other:mob,1,0,0","other:mob,-1,0.2,0.2","other:mob,NaN,0.2,0.2","other:mob,Infinity,0.2,0.2","other:mob,0,0,0","other:mob,0.2,0.2","BAD:mob,0.2,0.2,0.2"})check(HemalurgicTraits.parse(bad)==null);
  near(HemalurgicTraits.zinc(20,0).resistance(),.15);near(HemalurgicTraits.zinc(100,20).resistance(),.4);check(HemalurgicTraits.zinc(50,7).resistance()>HemalurgicTraits.zinc(20,0).resistance());
  double low=0,high=0;for(int i=0;i<=1000;i++){double u=i/1000.;var a=HemalurgicTraits.copper(.45,u);var b=HemalurgicTraits.copper(.95,u);check(a.resistance()>=.05&&a.resistance()<=.25&&b.resistance()>=a.resistance());near(a.memoryCapacity(),3*a.resistance());check(b.memoryCapacity()<=.75);low+=a.resistance();high+=b.resistance();}check(high>low);near(HemalurgicTraits.copper(.75,0).resistance(),.05);near(HemalurgicTraits.copper(.75,1).resistance(),.25);near(HemalurgicTraits.copper(.75,1).memoryCapacity(),.75);
  for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,1.01}){boolean rejected=false;try{HemalurgicTraits.copper(invalid,.5);}catch(IllegalArgumentException e){rejected=true;}check(rejected);}
  System.out.println("PASS: "+checks+" production Hemalurgic trait checks (Java17; no Minecraft runtime).");
 }
}
'''
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'CheckTraits.java';p.write_text(SOURCE)
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(ROOT/'src/main/java/dev/cosmerearcanists/hemalurgy/HemalurgicTraits.java'),str(p)],check=True)
 subprocess.run(['java','-cp',tmp,'CheckTraits'],check=True)
