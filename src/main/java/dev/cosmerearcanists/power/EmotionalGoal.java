package dev.cosmerearcanists.power;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Behavioral pressure competes with ordinary goals; it does not replace a mob's AI or teleport it. */
public final class EmotionalGoal extends Goal {
    private final Mob mob;
    public EmotionalGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
    @Override public boolean canUse(){return EmotionService.forcesBehavior(mob);}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public void tick(){EmotionService.behave(mob);}
    @Override public void stop(){mob.getNavigation().stop();mob.setTarget(null);}
}
