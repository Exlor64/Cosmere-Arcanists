#!/usr/bin/env python3
"""Compile and validate the production theft-category and loss policy without Minecraft dependencies."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[1]
SOURCE=r'''
import java.util.*;import dev.cosmerearcanists.hemalurgy.HemalurgyRules;
class CheckHemalurgy {
 static int checks;static void check(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  String[][] rows={{"steel","a","iron","steel","tin","pewter"},{"pewter","f","iron","steel","tin","pewter"},{"bronze","a","zinc","brass","copper","bronze"},{"brass","f","zinc","brass","copper","bronze"},{"cadmium","a","gold","electrum","cadmium","bendalloy"},{"gold","f","gold","electrum","cadmium","bendalloy"},{"electrum","a","aluminum","duralumin","chromium","nicrosil"},{"bendalloy","f","aluminum","duralumin","chromium","nicrosil"}};
  Set<String> powers=new HashSet<>();for(var row:rows)for(int i=2;i<row.length;i++){String key=row[1]+":"+row[i];check(HemalurgyRules.accepts(row[0],key));check(!HemalurgyRules.accepts(row[0],(row[1].equals("a")?"f:":"a:")+row[i]));check(powers.add(key));for(var other:rows)if(other!=row)check(!HemalurgyRules.accepts(other[0],key));}check(powers.size()==32);check(HemalurgyRules.accepts("cadmium","a:atium"));
  for(String key:powers)for(String unsupported:new String[]{"iron","tin","copper","zinc","aluminum","duralumin","chromium","nicrosil","lead","silver","nickel"})check(!HemalurgyRules.accepts(unsupported,key));
  check(HemalurgyRules.choose("steel",Map.of("a:steel",1.5,"a:iron",1.0,"f:steel",100.0)).equals("a:steel"));check(HemalurgyRules.choose("steel",Map.of("a:steel",1.0,"a:iron",1.0)).equals("a:iron"));check(HemalurgyRules.choose("steel",Map.of("a:gold",2.0)).isEmpty());check(HemalurgyRules.choose("steel",Map.of()).isEmpty());
  for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1025}){check(HemalurgyRules.choose("steel",Map.of("a:steel",bad)).isEmpty());check(HemalurgyRules.retained(bad,.8)==0);}
  for(double n:new double[]{.0001,.5,1,1.5,2,1024})for(double fraction:new double[]{.001,.1,.5,.8,1}){double kept=HemalurgyRules.retained(n,fraction);check(kept>0&&kept<=n);check(Math.abs(kept-n*fraction)<1e-10);}
  for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1.001})check(HemalurgyRules.retained(1,bad)==0);
  System.out.println("PASS: "+checks+" production Hemalurgy theft policy checks (Java17; no Minecraft runtime).");
 }
}
'''
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'CheckHemalurgy.java';p.write_text(SOURCE)
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(ROOT/'src/main/java/dev/cosmerearcanists/hemalurgy/HemalurgyRules.java'),str(p)],check=True)
 subprocess.run(['java','-cp',tmp,'CheckHemalurgy'],check=True)
