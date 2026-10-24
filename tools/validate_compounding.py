#!/usr/bin/env python3
"""Compile production resource/Identity/temporary-entry/control logic without Minecraft dependencies."""
from pathlib import Path
import subprocess
import tempfile
ROOT=Path(__file__).resolve().parents[1]
VERIFY=r'''
import java.util.*;
import dev.cosmerearcanists.compounding.*;
import dev.cosmerearcanists.nicrosil.*;
import dev.cosmerearcanists.power.PowerRules;
public class Verify {
 static int checks;
 static void check(boolean yes){checks++;if(!yes)throw new AssertionError("check "+checks);}
 static void near(double a,double b){check(Math.abs(a-b)<1e-9);}
 static IdentityMath.Portion part(UUID id,double n){return new IdentityMath.Portion(id,n);}
 public static void main(String[] args){
  for(double fraction:new double[]{0,.01,.1,.25,.5,.75,1,1.5}){
   double clamped=Math.min(1,fraction);near(CompoundingMath.fullness(600*fraction,600),clamped);near(CompoundingMath.boost(600*fraction,600,.5),.5*clamped);check(CompoundingMath.ticks(600*fraction,600,10)==(int)Math.ceil(200*clamped));
  }
  for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1}){near(CompoundingMath.fullness(bad,600),0);near(CompoundingMath.fullness(5,bad),0);near(CompoundingMath.input(bad,1,10,10),0);near(CompoundingMath.input(1,bad,10,10),0);near(CompoundingMath.input(1,1,bad,10),0);near(CompoundingMath.input(1,1,10,bad),0);}
  near(CompoundingMath.input(10,1,100,10),1);near(CompoundingMath.input(.2,1,100,10),.2);near(CompoundingMath.input(10,1,2,10),.2);near(CompoundingMath.input(10,1,0,10),0);near(CompoundingMath.input(0,1,100,10),0);
  double reserve=100,charge=0;for(int tick=0;tick<1000;tick++){double spent=CompoundingMath.input(reserve,.1,600-charge,10);reserve-=spent;charge+=10*spent;check(reserve>=0&&charge<=600+1e-9);near(reserve+charge/10,100);}near(charge,600);near(reserve,40);
  var a=UUID.randomUUID();var b=UUID.randomUUID();var own=List.of(part(a,1));var foreign=List.of(part(b,1));var neutral=List.of(part(null,.5));
  var pair=IdentityMath.best(own,own);near(pair.allomancy(),1);near(pair.feruchemy(),1);
  pair=IdentityMath.best(own,foreign);check(pair.allomancy()==0||pair.feruchemy()==0);
  pair=IdentityMath.best(own,neutral);near(pair.allomancy(),1);near(pair.feruchemy(),.5);
  pair=IdentityMath.best(neutral,own);near(pair.allomancy(),.5);near(pair.feruchemy(),1);
  pair=IdentityMath.best(List.of(part(a,1),part(null,.5)),List.of(part(b,1),part(null,.5)));check(pair.allomancy()<=.5||pair.feruchemy()<=.5);check(pair.allomancy()*pair.feruchemy()<2.25);
  var mind=new NicrosilLedger.Mind(UUID.randomUUID(),a,600);var first=new NicrosilLedger.Entry(UUID.randomUUID(),2,"a:steel",1,10,a);check(mind.deposit(first,1));check(mind.deposit(new NicrosilLedger.Entry(UUID.randomUUID(),2,"a:steel",1,10,a),1));near(mind.entry(first.id()).amount(),1);near(mind.used(),20);check(mind.entries().size()==1);
  check(!mind.canDeposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,"a:steel",1,10,a),1));check(!mind.canDeposit(new NicrosilLedger.Entry(UUID.randomUUID(),2,"a:steel",1,10,b),1));check(!mind.canDeposit(new NicrosilLedger.Entry(UUID.randomUUID(),2,"a:steel",.5,10,a),1));
  mind.reduce(new NicrosilLedger.Entry(first.id(),2,"a:steel",1,5,a));near(mind.used(),5);near(mind.entry(first.id()).amount(),1);check(mind.entry(first.id()).kind()==2);
  check(PowerRules.clamp("a:steel",-2,3,10)==-2);check(PowerRules.clamp("a:steel",-2,0,10)==0);check(PowerRules.next("a:steel",3,3,10)==0);check(PowerRules.label("a:steel",-1).equals("Compounding 1"));
  System.out.println("PASS: "+checks+" production compounding checks (Java17, no Minecraft runtime).");
 }
}
'''
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp);(p/'Verify.java').write_text(VERIFY)
 sources=['compounding/CompoundingMath.java','compounding/IdentityMath.java','nicrosil/NicrosilLedger.java','power/PowerRules.java']
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,*[str(ROOT/'src/main/java/dev/cosmerearcanists'/s) for s in sources],str(p/'Verify.java')],check=True)
 subprocess.run(['java','-cp',tmp,'Verify'],check=True)
