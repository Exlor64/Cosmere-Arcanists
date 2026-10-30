#!/usr/bin/env python3
"""Exercise production coinshot damage/velocity math without a Minecraft runtime."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = r'''
import dev.cosmerearcanists.power.MetalProjectilePhysics;
public class VerifyCoinshots {
 static int checks;
 static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Check "+checks);}
 static void close(double actual,double expected){check(Math.abs(actual-expected)<1e-8*Math.max(1,Math.abs(expected)));}
 public static void main(String[] args){
  close(MetalProjectilePhysics.baseDamage(1,2,.2),2);
  close(MetalProjectilePhysics.baseDamage(9,2,.2),3.8);
  close(MetalProjectilePhysics.baseDamage(279,2,.2),57.8);
  for(double mass:new double[]{1,2,9,81,279,558,1000,1000000})
   for(double power:new double[]{.1,.5,1,2,4,10,100})
    for(double change:new double[]{0,.25,.5,1,2}){
     double speed=MetalProjectilePhysics.speed(mass,power*2,3.5,8);
     double base=mass<=1?2:2+.2*mass;
     double damage=MetalProjectilePhysics.shotDamage(mass,speed*change,speed,power,2,.2,100);
     close(damage,Math.min(100,base*power*change));
     check(Double.isFinite(damage)&&damage>=0&&damage<=100);
    }
  close(MetalProjectilePhysics.shotDamage(9,8,8,1,2,.2,100),3.8);
  close(MetalProjectilePhysics.shotDamage(9,8,8,2,2,.2,100),7.6);
  close(MetalProjectilePhysics.shotDamage(9,4,8,1,2,.2,100),1.9);
  for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}){
   close(MetalProjectilePhysics.shotDamage(9,bad,4,1,2,.2,100),0);
   close(MetalProjectilePhysics.shotDamage(9,4,bad,1,2,.2,100),0);
   close(MetalProjectilePhysics.shotDamage(9,4,4,bad,2,.2,100),0);
  }
  check(MetalProjectilePhysics.damage(1000000,10,3.5,2,500)<=100);
  check(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)>100);
  check(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)<=500);
  System.out.println("PASS: "+checks+" production coinshot physics checks (Java17; no Minecraft runtime).");
 }
}
'''
with tempfile.TemporaryDirectory() as tmp:
    source = Path(tmp) / "VerifyCoinshots.java"
    source.write_text(SOURCE)
    subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", tmp,
                    str(ROOT / "src/main/java/dev/cosmerearcanists/power/MetalProjectilePhysics.java"), str(source)], check=True)
    subprocess.run(["java", "-cp", tmp, "VerifyCoinshots"], check=True)
