#!/usr/bin/env python3
"""Exercise the production copper PNG decoder/storage with minimal Minecraft/config bindings.
This Java17+ check does not replace the NeoForge Java21 build or GameTests.
"""
from pathlib import Path
import subprocess
import tempfile

FIXTURES = {'Verify.java': 'import dev.cosmerearcanists.copper.CopperImages;import dev.cosmerearcanists.feruchemy.FeruchemyConfig;import javax.imageio.ImageIO;import java.io.*;import java.nio.file.*;import java.util.*; public class Verify {static byte[] png(int w,int h)throws Exception{var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,0xA0B0C0);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}static void reject(byte[] b)throws Exception{try{CopperImages.normalize(b);throw new AssertionError("Accepted invalid image");}catch(IOException expected){}}public static void main(String[] args)throws Exception{byte[] normalized=CopperImages.normalize(png(16,8));var image=ImageIO.read(new ByteArrayInputStream(normalized));if(image.getWidth()!=16||image.getHeight()!=8||(image.getRGB(0,0)&0xFFFFFF)!=0xA0B0C0)throw new AssertionError("Pixel mismatch");reject(png(513,1));reject(png(1,289));reject(new byte[16]);byte[] bad=png(1,1);java.nio.ByteBuffer.wrap(bad).putInt(16,Integer.MAX_VALUE);reject(bad);var server=new net.minecraft.server.MinecraftServer(Path.of(args[0]));UUID first=UUID.randomUUID();CopperImages.write(server,first,normalized);if(!Arrays.equals(normalized,CopperImages.read(server,first)))throw new AssertionError("Disk round trip");FeruchemyConfig.COPPER_IMAGES.set(1);try{CopperImages.write(server,UUID.randomUUID(),normalized);throw new AssertionError("Quota ignored");}catch(IOException expected){}CopperImages.delete(server,first);if(Files.exists(CopperImages.directory(server).resolve(first+".png")))throw new AssertionError("Delete failed");System.out.println("PASS: production PNG validation, pixel preservation, bounded dimensions/headers, disk persistence, quota and deletion (Java17; Minecraft bindings stubbed).");}}', 'net/minecraft/server/MinecraftServer.java': 'package net.minecraft.server; public class MinecraftServer {final java.nio.file.Path path;public MinecraftServer(java.nio.file.Path p){path=p;}public java.nio.file.Path getWorldPath(net.minecraft.world.level.storage.LevelResource r){return path;}}', 'net/minecraft/world/level/storage/LevelResource.java': 'package net.minecraft.world.level.storage; public class LevelResource {public static final LevelResource ROOT=new LevelResource();}', 'dev/cosmerearcanists/feruchemy/FeruchemyConfig.java': 'package dev.cosmerearcanists.feruchemy; public class FeruchemyConfig { public static class Value {int n;public Value(int n){this.n=n;}public int get(){return n;}public void set(int n){this.n=n;}} public static Value COPPER_IMAGE_BYTES=new Value(786432),COPPER_IMAGES=new Value(2048),COPPER_DISK_MB=new Value(256); }'}


def main():
    repo = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory(prefix="copper-images-") as scratch:
        root = Path(scratch)
        files = dict(FIXTURES)
        files["dev/cosmerearcanists/copper/CopperImages.java"] = (repo / "src/main/java/dev/cosmerearcanists/copper/CopperImages.java").read_text()
        sources = []
        for name, content in files.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
            sources.append(str(path))
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(root / "classes"), *sources], check=True)
        subprocess.run(["java", "-Djava.awt.headless=true", "-cp", str(root / "classes"), "Verify", str(root / "world")], check=True)


if __name__ == "__main__":
    main()
