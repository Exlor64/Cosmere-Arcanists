package dev.cosmerearcanists.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class MetalResistance {
    /** Traverse each intervening voxel once; neither the player nor target voxel obstructs itself. */
    public static double cost(ServerPlayer player, Vec3 start, Vec3 end) {
        if (!PowerConfig.BLOCK_RESISTANCE.get()) return 0;
        var first = BlockPos.containing(start); var last = BlockPos.containing(end);
        int x=first.getX(), y=first.getY(), z=first.getZ();
        Vec3 delta=end.subtract(start);
        int sx=sign(delta.x), sy=sign(delta.y), sz=sign(delta.z);
        double tx=boundary(start.x,x,sx,delta.x), ty=boundary(start.y,y,sy,delta.y), tz=boundary(start.z,z,sz,delta.z);
        double dx=delta.x==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.x);
        double dy=delta.y==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.y);
        double dz=delta.z==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.z);
        double total=0;
        int steps=Math.abs(last.getX()-x)+Math.abs(last.getY()-y)+Math.abs(last.getZ()-z)+3;
        for(int i=0;i<steps;i++) {
            double t=Math.min(tx,Math.min(ty,tz));
            if(t>1 || !Double.isFinite(t)) break;
            if(tx<=t+1e-10){x+=sx;tx+=dx;}
            if(ty<=t+1e-10){y+=sy;ty+=dy;}
            if(tz<=t+1e-10){z+=sz;tz+=dz;}
            var pos=new BlockPos(x,y,z);
            if(pos.equals(last)) break;
            if(!player.serverLevel().hasChunkAt(pos)) return Double.POSITIVE_INFINITY;
            var state=player.level().getBlockState(pos);
            if(state.isAir() || state.getCollisionShape(player.level(),pos).isEmpty()) continue;
            float hardness=state.getDestroySpeed(player.level(),pos);
            total += (hardness<0 ? PowerConfig.RESISTANCE_HARDNESS_CAP.get() : Math.min(hardness,PowerConfig.RESISTANCE_HARDNESS_CAP.get())) * PowerConfig.RESISTANCE_COST.get();
            if(total>PowerConfig.RANGE.get()) break;
        }
        return total;
    }
    private static int sign(double n){return n>0?1:n<0?-1:0;}
    private static double boundary(double origin,int cell,int step,double delta){return step==0?Double.POSITIVE_INFINITY:((step>0?cell+1:cell)-origin)/delta;}
    private MetalResistance() { }
}
