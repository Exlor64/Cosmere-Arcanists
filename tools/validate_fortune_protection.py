#!/usr/bin/env python3
"""Check actual production Fortune protection probabilities and three-dimensional deflection math."""
from pathlib import Path
import subprocess
import tempfile

VERIFY = r'''
import dev.cosmerearcanists.fortune.FortuneProtectionMath;
public class Verify {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    static void close(double a,double b){check(Math.abs(a-b)<1e-9);}
    static double length(double[] a){return Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);}
    public static void main(String[] args){
        close(FortuneProtectionMath.chance(10,.02,.5),.2);
        close(FortuneProtectionMath.chance(10,.01,.4),.1);
        close(FortuneProtectionMath.chance(100,.02,.5),.5);
        close(FortuneProtectionMath.chance(1000,1,1),.95);
        close(FortuneProtectionMath.chance(0,.02,.5),0);
        close(FortuneProtectionMath.chance(-1,.02,.5),0);
        close(FortuneProtectionMath.chance(Double.NaN,.02,.5),0);
        close(FortuneProtectionMath.chance(10,.02,0),0);
        for(double angle:new double[]{3,6,9,12,20}){
            var straight=FortuneProtectionMath.turn(2,0,0,0,0,1,Math.toRadians(angle));
            close(length(straight),2);check(straight[2]>0);close(Math.toDegrees(Math.acos(straight[0]/2)),angle);
            var vertical=FortuneProtectionMath.turn(0,-3,0,1,0,0,Math.toRadians(angle));
            close(length(vertical),3);check(vertical[0]>0&&vertical[1]<0);
        }
        var parallel=FortuneProtectionMath.turn(2,0,0,1,0,0,.2);close(parallel[0],2);close(parallel[2],0);
        var zero=FortuneProtectionMath.turn(0,0,0,0,0,1,.2);close(length(zero),0);
        var invalid=FortuneProtectionMath.turn(2,0,0,Double.NaN,0,0,.2);close(invalid[0],2);
        System.out.println("PASS: "+checks+" production protection math checks (Java17; no Minecraft runtime).");
    }
}
'''

def main():
    repo = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory(prefix="fortune-protection-") as scratch:
        root = Path(scratch)
        driver = root / "Verify.java"
        driver.write_text(VERIFY)
        source = repo / "src/main/java/dev/cosmerearcanists/fortune/FortuneProtectionMath.java"
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(root), str(source), str(driver)], check=True)
        subprocess.run(["java", "-cp", str(root), "Verify"], check=True)

if __name__ == "__main__":
    main()
