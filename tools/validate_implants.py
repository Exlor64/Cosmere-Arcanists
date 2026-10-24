#!/usr/bin/env python3
"""Verify production placement/cost/seed policy and exact compatibility against accepted design."""
from pathlib import Path
import tempfile,subprocess,json
ROOT=Path(__file__).resolve().parents[1]
spec=json.loads((ROOT/'docs/hemalurgy-bind-points.json').read_text())
expected='\n'.join('check(ImplantRules.compatible("%s","%s",%s)==%s);'%(b['metal'],b['power'],i,str(p['id'] in b['points'] or i==12).lower()) for b in spec['bindings'] for i,p in enumerate(spec['points']))
expected+='\n'+'\n'.join('near(ImplantRules.POINTS.get(%d).u(),%.17g);near(ImplantRules.POINTS.get(%d).v(),%.17g);check(ImplantRules.POINTS.get(%d).id().equals("%s"));check(ImplantRules.POINTS.get(%d).back()==%s);'%(i,p['u'],i,p['v'],i,p['id'],i,str(p['view']=='back').lower()) for i,p in enumerate(spec['points']))
source=r'''
import dev.cosmerearcanists.hemalurgy.*;import dev.cosmerearcanists.ScadrialWorld;import java.util.*;
class CheckImplants {static int n;static void check(boolean v){n++;if(!v)throw new AssertionError("check"+n);}static void near(double a,double b){check(Math.abs(a-b)<1e-8);}public static void main(String[] args){check(ImplantRules.POINTS.size()==13);check(ImplantRules.BINDINGS.size()==38);
 for(int i=0;i<13;i++){var p=ImplantRules.POINTS.get(i);check(ImplantRules.nearest(p.back(),p.u(),p.v())==i);near(ImplantRules.accuracy(i,p.u(),p.v(),.16),1);for(int j=0;j<=100;j++){double u=p.u()+j*.001;double a=ImplantRules.accuracy(i,u,p.v(),.16);check(a>=0&&a<=1);}}
 for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1.01}){check(ImplantRules.nearest(false,bad,.5)==-1);check(ImplantRules.nearest(false,.5,bad)==-1);}check(ImplantRules.nearest(false,0,0)==-1);check(ImplantRules.nearest(true,.5,.4)==12);check(!ImplantRules.compatible("steel","a:iron",10));check(ImplantRules.compatible("steel","a:iron",0)&&ImplantRules.compatible("steel","a:steel",1));
 near(ImplantRules.activeCost(4,false,.25,.5),1);near(ImplantRules.activeCost(5,false,.25,.5),1.875);near(ImplantRules.activeCost(13,true,.25,.5),3.25);near(ImplantRules.takeover(-.875),1.875);near(ImplantRules.takeover(1),1);near(ImplantRules.removalDamage(0,13),2);near(ImplantRules.removalDamage(12,4),4);near(ImplantRules.removalDamage(12,5),10);check(ImplantRules.DAY==24000&&ImplantRules.WEEK==168000);
 for(var b:ImplantRules.BINDINGS.entrySet()){String[] key=b.getKey().split("\\|");check(ImplantRules.compatible(key[0],key[1],12));}for(int i=0;i<13;i++)check(!ImplantRules.compatible("chromium","trait:destiny",i));
 for(long seed=-1000;seed<=1000;seed++){check(ScadrialWorld.seed(seed)!=seed);check(ScadrialWorld.seed(seed)==ScadrialWorld.seed(seed));check(ScadrialWorld.seed(ScadrialWorld.seed(seed))==seed);}for(long seed:new long[]{Long.MIN_VALUE,Long.MAX_VALUE,0})check(ScadrialWorld.seed(seed)!=seed);
 __EXPECTED__
 System.out.println("PASS: "+n+" production implant/seed policy checks (Java17; no Minecraft runtime).");}}
'''.replace('__EXPECTED__',expected)
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'CheckImplants.java';p.write_text(source)
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(ROOT/'src/main/java/dev/cosmerearcanists/hemalurgy/ImplantRules.java'),str(ROOT/'src/main/java/dev/cosmerearcanists/ScadrialWorld.java'),str(p)],check=True)
 subprocess.run(['java','-cp',tmp,'CheckImplants'],check=True)
