#!/usr/bin/env python3
"""Compile and exercise production copper byte accounting; no Minecraft runtime."""
from pathlib import Path
import subprocess
import tempfile

VERIFY = r'''import dev.cosmerearcanists.copper.CopperCapacity;
public class Verify {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    public static void main(String[] args){
        check(CopperCapacity.bytes(1,1024)==1024);
        check(CopperCapacity.bytes(.5,1024)==512);
        check(CopperCapacity.bytes(600,1024)==614400);
        check(CopperCapacity.bytes(5400,1024)==5529600);
        check(CopperCapacity.bytes(0,1024)==0);
        check(CopperCapacity.bytes(Double.NaN,1024)==0);
        check(CopperCapacity.bytes(Double.POSITIVE_INFINITY,1024)==0);
        check(CopperCapacity.pageBytes("a","b","",0)==66);
        check(CopperCapacity.pageBytes("é","🌌","",0)==70);
        check(CopperCapacity.pageBytes("a","b","world",123)==194);
        check(CopperCapacity.bytes(CopperCapacity.requiredCharge(12345,1024),1024)>=12345);
        for(int rate:new int[]{1,3,1024,1048576})for(long size:new long[]{1,1023,12345,786432,10000000})
            check(CopperCapacity.bytes(CopperCapacity.requiredCharge(size,rate),rate)>=size);
        System.out.println("PASS: "+checks+" production copper capacity checks (Java17; no Minecraft runtime).");
    }
}
'''

def main():
    repo = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory(prefix="copper-capacity-") as scratch:
        root = Path(scratch)
        driver = root / "Verify.java"
        driver.write_text(VERIFY)
        source = repo / "src/main/java/dev/cosmerearcanists/copper/CopperCapacity.java"
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(root), str(source), str(driver)], check=True)
        subprocess.run(["java", "-cp", str(root), "Verify"], check=True)

if __name__ == "__main__":
    main()
