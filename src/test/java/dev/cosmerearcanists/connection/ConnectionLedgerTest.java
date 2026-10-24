package dev.cosmerearcanists.connection;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectionLedgerTest {
    @Test void pairDirectionAndRepeatedActionsShareCooldown(){var a=UUID.randomUUID();var b=UUID.randomUUID();var ledger=new ConnectionLedger();assertTrue(ledger.grow(a,b,2,0,true,600,64,16384));assertFalse(ledger.grow(b,a,2,599,true,600,64,16384));assertEquals(2,ledger.strength(b,a));assertTrue(ledger.grow(b,a,2,600,true,600,64,16384));}
    @Test void restoredHistoryKeepsClocksAndIndependentProximityBudget(){var a=UUID.randomUUID();var b=UUID.randomUUID();var ledger=new ConnectionLedger();ledger.grow(a,b,2,1000,true,600,64,16384);var loaded=new ConnectionLedger();ledger.entries().forEach(loaded::restore);assertFalse(loaded.grow(b,a,2,1001,true,600,64,16384));assertTrue(loaded.grow(b,a,1.0/60,1001,false,20,64,16384));assertFalse(loaded.grow(a,b,1.0/60,1001,false,20,64,16384));assertTrue(loaded.strength(a,b)>2);}
    @Test void strongerTappingSensesWeakerBondsAndStorageSuppressesExpression(){assertTrue(ConnectionMath.requiredBond(10,1,100,.25)<ConnectionMath.requiredBond(1,1,100,.25));assertEquals(28,ConnectionMath.expressedBond(40,.7),1e-9);assertEquals(40,ConnectionMath.expressedBond(40,2),1e-9);assertTrue(Double.isInfinite(ConnectionMath.requiredBond(0,1,100,.25)));}
}
