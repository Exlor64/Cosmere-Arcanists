package dev.cosmerearcanists.power;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Only replace target selection. Native bow, crossbow, melee and special attack goals stay free. */
public final class AngerTargetGoal extends Goal {
    private final Mob mob;
    public AngerTargetGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.TARGET));}
    @Override public boolean canUse(){return EmotionService.nativeAnger(mob);}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public void start(){tick();}
    @Override public void tick(){EmotionService.redirectAnger(mob);}
    @Override public void stop(){EmotionService.releaseAngerTarget(mob);}
}
