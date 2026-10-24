#!/usr/bin/env python3
"""Verify production compounding Identity equivalence; unrelated keys remain separate."""
from pathlib import Path
import tempfile,subprocess
ROOT=Path(__file__).resolve().parents[1]
SOURCE=r'''
import java.util.*;import dev.cosmerearcanists.compounding.IdentityMath;
class VerifyIdentityAccess {static int n;static void check(boolean value){n++;if(!value)throw new AssertionError("check"+n);}static void near(double a,double b){check(Math.abs(a-b)<1e-8);}public static void main(String[] args){var a=new UUID(0,1);var b=new UUID(0,2);var c=new UUID(0,3);for(int i=1;i<=100;i++){double x=i/10.;var all=List.of(new IdentityMath.Portion(a,x));var feru=List.of(new IdentityMath.Portion(b,x));near(IdentityMath.best(all,feru).allomancy(),0);var shared=IdentityMath.best(all,feru,Set.of(a,b));near(shared.allomancy(),x);near(shared.feruchemy(),x);near(IdentityMath.best(all,feru,Set.of(a)).allomancy(),0);var foreign=List.of(new IdentityMath.Portion(c,x));near(IdentityMath.best(all,foreign,Set.of(a,b)).allomancy(),0);var neutral=List.of(new IdentityMath.Portion(null,1),new IdentityMath.Portion(c,x));var result=IdentityMath.best(all,neutral,Set.of(a,b));near(result.feruchemy(),1);check(result.allomancy()==x);check(all.get(0).identity().equals(a)&&feru.get(0).identity().equals(b));}System.out.println("PASS: "+n+" production multi-Identity compounding checks (Java17; no Minecraft runtime).");}}
'''
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'VerifyIdentityAccess.java';p.write_text(SOURCE)
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(ROOT/'src/main/java/dev/cosmerearcanists/compounding/IdentityMath.java'),str(p)],check=True)
 subprocess.run(['java','-cp',tmp,'VerifyIdentityAccess'],check=True)
