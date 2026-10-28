package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
/** Per-player surface collision, shared by client prediction and authoritative server movement. */
public final class SteelFluidRunning {
    private record Movement(Player player,Vec3 motion){}
    private static final ThreadLocal<Movement> MOVEMENT=new ThreadLocal<>();
    /** Scope actual requested movement: server walking packets need not set horizontal deltaMovement. */
    public static void move(Player player,Vec3 motion,Runnable original){
        var previous=MOVEMENT.get();MOVEMENT.set(new Movement(player,motion));
        try{original.run();}finally{if(previous==null)MOVEMENT.remove();else MOVEMENT.set(previous);}
    }
    public static boolean eligible(Player p){
        if(!p.isAlive()||p.isSpectator()||p.isShiftKeyDown()||p.isPassenger()||p.isFallFlying()||p.getAbilities().flying||!SteelFeruchemy.configured())return false;
        // This named transient modifier contains actual PAID output, including partial reserves and enhancement.
        double level=(SteelFeruchemy.factor(p)-1)/FeruchemyConfig.STEEL_TAP_STEP.get();
        if(level<=FeruchemyConfig.STEEL_FLUID_THRESHOLD.get()+1e-7)return false;
        if(p instanceof ServerPlayer server){
            if(PowerData.state(server,"f:steel")<=0||PowerAttributes.strength(server,"f:steel")<=0)return false;
            boolean contact=false;for(var piece:Metalminds.touching(server))if(Metalminds.usableBy(piece.stack(),server,"steel")){contact=true;break;}
            if(!contact)return false;
        }
        var current=MOVEMENT.get();var motion=current!=null&&current.player()==p?current.motion():p.getDeltaMovement();
        double speed=FeruchemyConfig.STEEL_FLUID_MIN_SPEED.get();return motion.horizontalDistanceSqr()>=speed*speed;
    }
    public static VoxelShape surface(BlockGetter level,BlockPos pos,FluidState fluid,CollisionContext context){
        if(!(context instanceof EntityCollisionContext entityContext)||!(entityContext.getEntity() instanceof Player p)||!eligible(p))return Shapes.empty();
        boolean enabled=fluid.is(FluidTags.WATER)?FeruchemyConfig.STEEL_WATER_RUNNING.get():fluid.is(FluidTags.LAVA)&&FeruchemyConfig.STEEL_LAVA_RUNNING.get();
        if(!enabled||!level.getFluidState(pos.above()).isEmpty())return Shapes.empty();
        // Small clearance avoids fluid-contact damage/drag at the exact lava boundary. Never lifts submerged players.
        double height=Math.min(1,fluid.getHeight(level,pos)+1.0/64);
        if(height<=0)return Shapes.empty();
        var shape=Shapes.box(0,0,0,1,height,1);return context.isAbove(shape,pos,false)?shape:Shapes.empty();
    }
    private SteelFluidRunning(){}
}
