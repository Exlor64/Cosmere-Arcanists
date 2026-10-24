#!/usr/bin/env python3
"""Exercise production suppression timing with small Minecraft/service stubs, not engine validation."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[1]
FILES={
'net/minecraft/server/level/ServerPlayer.java':'''package net.minecraft.server.level;public class ServerPlayer {public final Clock server=new Clock();public final java.util.Map<String,Long> values=new java.util.HashMap<>();public static class Clock{public long tick;public Clock overworld(){return this;}public long getGameTime(){return tick;}}}''',
'dev/cosmerearcanists/power/PowerData.java':'''package dev.cosmerearcanists.power;import net.minecraft.server.level.ServerPlayer;public class PowerData {public record Tag(ServerPlayer p){public long getLong(String key){return p.values.getOrDefault(key,0L);}public void putLong(String key,long value){p.values.put(key,value);}}public static Tag data(ServerPlayer p){return new Tag(p);}}''',
'dev/cosmerearcanists/power/PowerAttributes.java':'''package dev.cosmerearcanists.power;public class PowerAttributes{public static java.util.Set<String> keys(){return java.util.Set.of("a:steel","f:gold");}}''',
'dev/cosmerearcanists/power/PowerService.java':'''package dev.cosmerearcanists.power;import net.minecraft.server.level.ServerPlayer;public class PowerService{public static int stopped;public static void powerTransferred(ServerPlayer p,String key){stopped++;}}''',
'dev/cosmerearcanists/hemalurgy/HemalurgyConfig.java':'''package dev.cosmerearcanists.hemalurgy;public class HemalurgyConfig{public record Value(int get){}public static Value ALUMINUM_SECONDS=new Value(300);}''',
'CheckAluminum.java':'''import net.minecraft.server.level.ServerPlayer;import dev.cosmerearcanists.hemalurgy.*;import dev.cosmerearcanists.power.*;class CheckAluminum{static int n;static void check(boolean ok){n++;if(!ok)throw new AssertionError("check"+n);}public static void main(String[] args){var p=new ServerPlayer();p.server.tick=100;check(!AluminumSuppression.active(p));AluminumSuppression.apply(p);check(AluminumSuppression.remaining(p)==6000);check(PowerService.stopped==2);p.server.tick=6099;check(AluminumSuppression.active(p)&&AluminumSuppression.remaining(p)==1);p.server.tick=6100;check(!AluminumSuppression.active(p));AluminumSuppression.apply(p);p.server.tick+=20;AluminumSuppression.apply(p);check(AluminumSuppression.remaining(p)==6000);var replacement=new ServerPlayer();replacement.server.tick=p.server.tick;replacement.values.putAll(p.values);check(AluminumSuppression.remaining(replacement)==6000);replacement.server.tick+=6000;check(!AluminumSuppression.active(replacement));HemalurgyConfig.ALUMINUM_SECONDS=new HemalurgyConfig.Value(1);AluminumSuppression.apply(replacement);check(AluminumSuppression.remaining(replacement)==20);System.out.println("PASS: "+n+" production suppression timing checks (stub services, no Minecraft runtime).");}}'''
}
for name,method in [('nicrosil/NicrosilService','clear'),('nicrosil/TemporaryPowers','stop'),('compounding/CompoundingService','clear'),('power/EnhancementService','clear')]:
 package,cls=name.split('/');FILES[f'dev/cosmerearcanists/{name}.java']=f'package dev.cosmerearcanists.{package};public class {cls}{{public static void {method}(net.minecraft.server.level.ServerPlayer p){{}}}}'
with tempfile.TemporaryDirectory() as tmp:
 paths=[]
 for name,source in FILES.items():
  p=Path(tmp)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source);paths.append(str(p))
 paths.append(str(ROOT/'src/main/java/dev/cosmerearcanists/hemalurgy/AluminumSuppression.java'))
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,*paths],check=True)
 subprocess.run(['java','-cp',tmp,'CheckAluminum'],check=True)
