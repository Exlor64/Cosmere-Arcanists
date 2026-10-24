package dev.cosmerearcanists.fortune;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FortuneMathTest {
    @Test void partialBonusAttemptsAndHardWorkBudget(){assertEquals(1,FortuneMath.attempts(.5,.4));assertEquals(0,FortuneMath.attempts(.5,.6));assertEquals(4,FortuneMath.attempts(100,0));assertEquals(0,FortuneMath.attempts(Double.NaN,0));}
    @Test void storageProbabilitiesAndQualityRanking(){assertEquals(.225,FortuneMath.probability(.3,.75),1e-9);assertTrue(FortuneMath.score(0,true,false,1)>FortuneMath.score(0,false,false,64));assertEquals(0,FortuneMath.probability(-1,1));}
    @Test void mishapCatalogHasDistinctPersistentIdsAndSeverity(){assertTrue(Misfortune.values().length>=15);assertEquals(Misfortune.values().length,java.util.Arrays.stream(Misfortune.values()).map(Misfortune::key).distinct().count());assertTrue(Misfortune.LIGHTNING.minimum>Misfortune.STUMBLE.minimum);}
}
