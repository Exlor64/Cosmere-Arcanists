package dev.cosmerearcanists.koloss;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
/** Only navigates to an already loaded leader; never teleports or loads chunks. */
final class FollowPackLeaderGoal extends Goal {
    private final KolossEntity mob;
    private KolossEntity leader;
    private int nextPath;
    FollowPackLeaderGoal(KolossEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
    @Override public boolean canUse(){leader=mob.packLeader();return mob.getTarget()==null&&leader!=null&&mob.distanceToSqr(leader)>36&&mob.distanceToSqr(leader)<1024;}
    @Override public boolean canContinueToUse(){return leader!=null&&leader.isAlive()&&mob.getTarget()==null&&mob.distanceToSqr(leader)>16&&mob.distanceToSqr(leader)<1024;}
    @Override public void start(){nextPath=0;}
    @Override public void tick(){if(--nextPath<=0){nextPath=10;mob.getNavigation().moveTo(leader,1.0);}}
    @Override public void stop(){leader=null;mob.getNavigation().stop();}
}
