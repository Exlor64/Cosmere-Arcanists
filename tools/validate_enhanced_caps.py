"""Compile production cap/projectile math with Java17 and minimal bindings; not a Minecraft runtime test."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix="cosmere-enhanced-caps-") as task_dir:
    scratch = Path(task_dir)
    sources = {
        "net/minecraft/server/level/ServerPlayer.java": "package net.minecraft.server.level; public class ServerPlayer {}",
        "dev/cosmerearcanists/power/EnhancementService.java": "package dev.cosmerearcanists.power; public class EnhancementService { public static double capMultiplier(net.minecraft.server.level.ServerPlayer p,String power){return 1;} }",
        "EnhancedCapsCheck.java": """
import dev.cosmerearcanists.power.*;
public class EnhancedCapsCheck {
 static void close(double value,double expected){if(!Double.isFinite(value)||Math.abs(value-expected)>1e-9)throw new AssertionError(value+" expected "+expected);}
 static void check(boolean value){if(!value)throw new AssertionError("Cap invariant failed");}
 public static void main(String[] args){
  close(EnhancedCaps.scaled(8,1),8);close(EnhancedCaps.scaled(8,5),40);
  close(EnhancedCaps.fraction(.5,1),.5);close(EnhancedCaps.fraction(.5,1.75),.7026982212493197);
  close(EnhancedCaps.fraction(.5,100),.95);close(EnhancedCaps.fraction(0,5),0);close(EnhancedCaps.fraction(1,5),1);
  close(EnhancedCaps.fraction(.99,5),.99);close(EnhancedCaps.velocity(128,1),128);close(EnhancedCaps.velocity(32,5),64);
  check(MetalProjectilePhysics.damage(1000000,10,3.5,2,500)<=100);
  check(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)>100);
  check(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)<=500);
  System.out.println("PASS: 13 compiled production cap/projectile checks; finite reductions and ordinary/enhanced caps preserved.");
 }
}
""",
    }
    paths = []
    for name, source in sources.items():
        path = scratch / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source)
        paths.append(str(path))
    for name in ("EnhancedCaps", "MetalProjectilePhysics"):
        paths.append(str(ROOT / f"src/main/java/dev/cosmerearcanists/power/{name}.java"))
    classes = scratch / "classes"
    classes.mkdir()
    subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(classes), *paths], check=True)
    subprocess.run(["java", "-cp", str(classes), "EnhancedCapsCheck"], check=True)
