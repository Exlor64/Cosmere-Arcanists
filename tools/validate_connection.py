#!/usr/bin/env python3
"""Compile production Connection policies/ledger with Java17; no Minecraft runtime."""
from pathlib import Path
import subprocess
import tempfile

VERIFY = r'''
import dev.cosmerearcanists.connection.*;
import java.util.*;
public class Verify {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    static void close(double a,double b){check(Math.abs(a-b)<1e-8);}
    public static void main(String[] args){
        UUID a=new UUID(0,1),b=new UUID(0,2),c=new UUID(0,3),d=new UUID(0,4);
        var ledger=new ConnectionLedger();
        check(new ConnectionLedger.Pair(a,b).equals(new ConnectionLedger.Pair(b,a)));
        check(!ledger.grow(a,a,2,0,true,600,64,16384));
        check(ledger.grow(a,b,2,0,true,600,64,16384));close(ledger.strength(b,a),2);
        check(!ledger.grow(b,a,2,0,true,600,64,16384));
        check(!ledger.grow(b,a,2,599,true,600,64,16384));
        check(ledger.grow(b,a,2,600,true,600,64,16384));close(ledger.strength(a,b),4);
        check(ledger.grow(a,b,1.0/60,600,false,20,64,16384));
        check(!ledger.grow(b,a,1.0/60,600,false,20,64,16384));
        check(ledger.grow(b,a,1.0/60,620,false,20,64,16384));
        check(!ledger.grow(a,b,Double.NaN,1200,true,600,64,16384));
        check(!ledger.grow(a,c,2,1200,true,600,1,16384));
        check(!ledger.grow(c,d,2,1200,true,600,64,1));
        var restored=new ConnectionLedger();ledger.entries().forEach(restored::restore);
        close(restored.strength(b,a),ledger.strength(a,b));
        check(!restored.grow(a,b,2,1000,true,600,64,16384));
        check(restored.grow(a,b,200,1200,true,600,64,16384));close(restored.strength(a,b),100);
        close(ConnectionMath.requiredBond(1,1,100,.25),100);
        check(ConnectionMath.requiredBond(10,1,100,.25)<31);
        check(Double.isInfinite(ConnectionMath.requiredBond(0,1,100,.25)));
        check(ConnectionMath.requiredBond(100000,1,100,.25)>=1);
        close(ConnectionMath.interactionGain(2,0,.75),2);
        close(ConnectionMath.interactionGain(2,80,.75),.8);
        close(ConnectionMath.interactionGain(2,100,.75),.5);
        close(ConnectionMath.expressedBond(80,.7),56);
        close(ConnectionMath.expressedBond(80,5),80);
        close(ConnectionMath.detectionMultiplier(.3,.8),.76);
        close(ConnectionMath.detectionMultiplier(.95,.8),.24);
        check(ConnectionMath.priceAdjustment(20,-.2)==-4);
        check(ConnectionMath.priceAdjustment(20,.15)==3);
        check(ConnectionMath.priceAdjustment(1,-.95)==0);
        var bounded=new ConnectionLedger();for(int i=1;i<=300;i++)bounded.restore(new ConnectionLedger.Pair(a,new UUID(1,i)),new ConnectionLedger.Bond(1,0,0));
        check(bounded.connections(a).size()==256);
        var removed=new ConnectionLedger();removed.restore(new ConnectionLedger.Pair(a,b),new ConnectionLedger.Bond(20,0,0));removed.restore(new ConnectionLedger.Pair(a,c),new ConnectionLedger.Bond(40,0,0));removed.restore(new ConnectionLedger.Pair(b,c),new ConnectionLedger.Bond(60,0,0));removed.remove(a);check(removed.connections(a).isEmpty());check(removed.connections(b).size()==1&&removed.connections(c).size()==1);close(removed.strength(a,b),0);close(removed.strength(b,c),60);removed.remove(a);check(removed.entries().size()==1);check(removed.grow(a,b,2,100,true,20,256,262144));
        System.out.println("PASS: "+checks+" production Connection policy/ledger checks (Java17; no Minecraft runtime).");
    }
}
'''

def main():
    repo = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory(prefix="connection-") as scratch:
        root = Path(scratch)
        driver = root / "Verify.java"
        driver.write_text(VERIFY)
        source = repo / "src/main/java/dev/cosmerearcanists/connection"
        subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", str(root), str(source / "ConnectionMath.java"), str(source / "ConnectionLedger.java"), str(driver)], check=True)
        subprocess.run(["java", "-cp", str(root), "Verify"], check=True)

if __name__ == "__main__":
    main()
