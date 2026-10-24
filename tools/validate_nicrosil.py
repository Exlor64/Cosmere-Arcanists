#!/usr/bin/env python3
"""Compile and exercise the real persistent-content policy without Minecraft stubs."""
from pathlib import Path
import subprocess
import tempfile

VERIFY = r'''
import dev.cosmerearcanists.nicrosil.NicrosilLedger;
import dev.cosmerearcanists.nicrosil.NicrosilProgress;
import java.util.*;
public class Verify {
 static int checks;
 static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
 static void bad(Runnable r){boolean rejected=false;try{r.run();}catch(IllegalArgumentException expected){rejected=true;}check(rejected);}
 public static void main(String[] args){
  var ledger=new NicrosilLedger();var owner=UUID.randomUUID();var stranger=UUID.randomUUID();var id=UUID.randomUUID();
  var mind=new NicrosilLedger.Mind(id,owner,600);check(ledger.add(mind,2));check(!ledger.add(mind,2));check(ledger.size()==1);
  check(mind.accessible(owner,true));check(!mind.accessible(stranger,true));check(mind.accessible(stranger,false));
  var unkeyed=new NicrosilLedger.Mind(UUID.randomUUID(),null,600);check(unkeyed.accessible(stranger,true));check(ledger.add(unkeyed,2));check(!ledger.add(new NicrosilLedger.Mind(UUID.randomUUID(),null,600),2));
  var power=new NicrosilLedger.Entry(UUID.randomUUID(),0,"a:steel",1.5,150);check(mind.add(power,32));check(!mind.add(power,32));check(mind.used()==150);
  check(ledger.get(id)==mind);check(ledger.get(id).entry(power.id())==power);
  var resource=new NicrosilLedger.Entry(UUID.randomUUID(),1,"test:future_resource",100,200);check(mind.add(resource,32));check(mind.used()==350);
  check(!mind.add(new NicrosilLedger.Entry(UUID.randomUUID(),0,"f:gold",3,300),32));check(mind.entries().size()==2);check(!mind.add(new NicrosilLedger.Entry(UUID.randomUUID(),0,"f:gold",1,100),2));
  check(mind.remove(power.id())==power);check(ledger.get(id).remove(power.id())==null);check(mind.used()==200);check(mind.entry(power.id())==null);
  check(ledger.erase(id));check(!ledger.erase(id));check(ledger.get(id)==null);check(ledger.size()==1);
  for(double strength:new double[]{.01,.5,1,1.5,2,10,1024}){check(NicrosilLedger.powerCharge(strength,100)==strength*100);check(NicrosilLedger.canReceive(0,strength));check(NicrosilLedger.canReceive(1,strength)==(1+strength<=1024));}
  for(double invalid:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1025}){check(NicrosilLedger.powerCharge(invalid,100)==0);check(!NicrosilLedger.canReceive(0,invalid));}
  check(NicrosilLedger.powerCharge(1,Double.NaN)==0);check(NicrosilLedger.powerCharge(1,-1)==0);
  bad(()->new NicrosilLedger.Entry(UUID.randomUUID(),3,"bad",1,1));bad(()->new NicrosilLedger.Entry(UUID.randomUUID(),0,"",1,1));bad(()->new NicrosilLedger.Entry(UUID.randomUUID(),0,"x",Double.NaN,1));bad(()->new NicrosilLedger.Entry(UUID.randomUUID(),0,"x",1,0));bad(()->new NicrosilLedger.Mind(UUID.randomUUID(),null,Double.POSITIVE_INFINITY));
  var bounded=new NicrosilLedger.Mind(UUID.randomUUID(),null,10000);for(int i=0;i<NicrosilLedger.HARD_ENTRIES;i++)check(bounded.add(new NicrosilLedger.Entry(UUID.randomUUID(),1,"future:type",1,1),10000));check(!bounded.add(new NicrosilLedger.Entry(UUID.randomUUID(),1,"future:type",1,1),10000));
  check(NicrosilLedger.canReceive(.5,.5));check(NicrosilLedger.canReceive(1023.5,.5));check(!NicrosilLedger.canReceive(1023.5,.6));check(!NicrosilLedger.canReceive(Double.NaN,1));
  check(NicrosilProgress.target(1,.5)==.5);check(NicrosilProgress.target(1,0)==1);check(NicrosilProgress.target(1,2)==1);check(NicrosilProgress.target(1,-1)==0);check(NicrosilProgress.target(Double.NaN,0)==0);
  check(NicrosilProgress.step(.5,1,600,100,.1,1)==.005);check(NicrosilProgress.step(.5,1,600,100,.1,2)==.01);check(NicrosilProgress.step(.001,1,600,100,.1,1)==.001);check(NicrosilProgress.step(.5,1,.1,100,.1,1)==.001);check(NicrosilProgress.step(.5,1,0,100,.1,1)==0);check(NicrosilProgress.step(.5,1,600,0,.1,1)==0);check(NicrosilProgress.step(.5,1,600,100,Double.NaN,1)==0);
  var portions=new NicrosilLedger.Mind(UUID.randomUUID(),null,100);double personal=1,remaining=.5;UUID stable=null;for(int i=0;i<100;i++){double step=NicrosilProgress.step(remaining,personal,100-portions.used(),100,.1,1);double after=Math.max(0,personal-step),moved=personal-after;check(portions.deposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,"a:steel",moved,moved*100),1));personal=after;remaining=Math.max(0,remaining-moved);if(stable==null)stable=portions.entries().get(0).id();check(portions.entries().size()==1&&portions.entry(stable)!=null&&Math.abs(personal+portions.entry(stable).amount()-1)<1e-10);}
  check(Math.abs(personal-.5)<1e-9);check(Math.abs(portions.entry(stable).amount()-.5)<1e-9);check(Math.abs(portions.used()-50)<1e-8);check(!portions.deposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,"a:steel",1,100),1));check(!portions.deposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,"a:steel",.1,20),1));
  System.out.println("PASS: "+checks+" production nicrosil ledger/policy checks (Java17; no Minecraft runtime).");
 }
}
'''
ROOT=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as tmp:
    folder=Path(tmp)
    (folder/'Verify.java').write_text(VERIFY)
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',str(folder),str(ROOT/'src/main/java/dev/cosmerearcanists/nicrosil/NicrosilLedger.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/nicrosil/NicrosilProgress.java'),str(folder/'Verify.java')],check=True)
    subprocess.run(['java','-cp',str(folder),'Verify'],check=True)
