package dev.cosmerearcanists.aondor;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import dev.cosmerearcanists.connection.WorldConnectionAccess;
import dev.cosmerearcanists.power.PowerData;

/** Independent timed self-buffs: strongest Aon boost wins, each retains its paid expiry. */
public final class AonConnectionBoosts {
    private static final String KEY="aondor_connection_boosts";
    private static ListTag active(ServerPlayer p){long now=p.server.overworld().getGameTime();var all=PowerData.data(p).getList(KEY,Tag.TAG_COMPOUND);var live=new ListTag();for(int i=0;i<Math.min(8,all.size());i++){var t=all.getCompound(i);double factor=t.getDouble("factor");if(t.getLong("until")>now&&Double.isFinite(factor)&&factor>1)live.add(t.copy());}return live;}
    public static WorldConnectionAccess.State apply(ServerPlayer p,WorldConnectionAccess.State connection){double factor=1;var live=active(p);for(int i=0;i<live.size();i++)factor=Math.max(factor,live.getCompound(i).getDouble("factor"));return new WorldConnectionAccess.State(connection.baseline(),factor==1?connection.effective():Math.max(connection.effective(),connection.baseline()*factor*Math.min(1,dev.cosmerearcanists.feruchemy.DuraluminFeruchemy.connectionMultiplier(p))));}
    static Supplier<AonCastPlan.Outcome> plan(ServerPlayer p,double requested,double seconds,double strength){
        double factor=1+(requested-1)*strength;if(!Double.isFinite(factor)||factor<1||factor>AonDorConfig.CONNECTION_CAP.get()||!Double.isFinite(seconds)||seconds<=0||seconds>AonDorConfig.CONNECTION_SECONDS.get())throw new IllegalArgumentException("Connection multiplier/duration exceeds configured bounds");
        var live=active(p);if(live.size()>=8)throw new IllegalArgumentException("Too many active Aon Connection boosts");if(factor==1)return AonCastPlan.none("Connection: no increase requested.");
        var entry=new CompoundTag();entry.putDouble("factor",factor);entry.putLong("until",p.server.overworld().getGameTime()+Math.max(1,Math.round(seconds*20)));live.add(entry);
        AonCost.add("connection",(factor-1)*seconds);
        return ()->{PowerData.data(p).put(KEY,live);return new AonCastPlan.Outcome("Connection: "+AonInvestiture.format(factor)+"× baseline for "+AonInvestiture.format(seconds)+" seconds.",true);};
    }
    private AonConnectionBoosts(){}
}
