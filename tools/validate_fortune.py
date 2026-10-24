#!/usr/bin/env python3
"""Exercise production Fortune math and the complete mishap catalog; no Minecraft runtime."""
from pathlib import Path
import subprocess
import tempfile

VERIFY = r'''
import dev.cosmerearcanists.fortune.*;
import java.util.*;
public class Verify {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    static void close(double a,double b){check(Math.abs(a-b)<1e-9);}
    public static void main(String[] args){
        check(FortuneMath.attempts(0,0)==0);check(FortuneMath.attempts(.5,.49)==1);check(FortuneMath.attempts(.5,.5)==0);
        check(FortuneMath.attempts(2.5,.4)==3);check(FortuneMath.attempts(2.5,.8)==2);
        check(FortuneMath.attempts(100,.1)==4);check(FortuneMath.attempts(Double.NaN,0)==0);
        close(FortuneMath.probability(.3,.75),.225);close(FortuneMath.probability(.95,.08),.076);
        close(FortuneMath.probability(3,2),1);close(FortuneMath.probability(-1,.5),0);close(FortuneMath.probability(Double.NaN,1),0);
        check(FortuneMath.score(0,true,false,1)>FortuneMath.score(0,false,false,64));
        check(FortuneMath.score(0,false,true,1)>FortuneMath.score(0,false,false,64));
        var rotated=FortuneMath.rotate(1,0,Math.PI/2);close(rotated[0],0);close(rotated[1],1);
        var reverse=FortuneMath.rotate(1,0,Math.PI);close(reverse[0],-1);close(reverse[1],0);
        check(Misfortune.values().length>=15);var keys=new HashSet<String>();for(var effect:Misfortune.values()){
            check(keys.add(effect.key()));check(effect.minimum>0&&effect.minimum<=.95);check(effect.cooldown>=20&&effect.weight>0);
        }
        check(Misfortune.LIGHTNING.minimum>Misfortune.STUMBLE.minimum);
        check(Misfortune.DROPPED_ITEM_VEER.action&&Misfortune.PROJECTILE_VEER.action&&Misfortune.LOST_CATCH.action);
        System.out.println("PASS: "+checks+" production Fortune math/catalog checks; "+Misfortune.values().length+" distinct mishaps (Java17; no Minecraft runtime).");
    }
}
'''

def main():
    repo = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory(prefix="fortune-") as scratch:
        root = Path(scratch)
        driver = root / "Verify.java"
        driver.write_text(VERIFY)
        source = repo / "src/main/java/dev/cosmerearcanists/fortune"
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(root), str(source / "FortuneMath.java"), str(source / "Misfortune.java"), str(driver)], check=True)
        subprocess.run(["java", "-cp", str(root), "Verify"], check=True)

if __name__ == "__main__":
    main()
