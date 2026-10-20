package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.power.*;
import net.minecraft.server.level.ServerPlayer;
/** Server position only: camera rotation and menu input do not reset storage inactivity. */
public final class StorageActivity {
    private static final String KEY="cosmerearcanists_storage_activity";
    public static void tick(ServerPlayer player){observe(player,player.level().getGameTime());}
    public static void observe(ServerPlayer player,long tick){
        var root=player.getPersistentData();var tag=root.getCompound(KEY);var position=player.position();String dimension=player.level().dimension().location().toString();
        boolean moved=!tag.contains("x")||!dimension.equals(tag.getString("dimension"))||position.distanceToSqr(new net.minecraft.world.phys.Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z")))>=.0001;
        if(moved){tag.putDouble("x",position.x);tag.putDouble("y",position.y);tag.putDouble("z",position.z);tag.putString("dimension",dimension);tag.putInt("idleTicks",0);}
        else if(!tag.contains("lastTick")||tick!=tag.getLong("lastTick"))tag.putInt("idleTicks",Math.min(2000000000,tag.getInt("idleTicks")+1));
        tag.putLong("lastTick",tick);root.put(KEY,tag);
        if(blocked(player))for(String power:PowerAttributes.keys())if(power.startsWith("f:")&&PowerData.state(player,power)<0)PowerData.state(player,power,0);
    }
    public static boolean blocked(ServerPlayer player){return FeruchemyConfig.IDLE_STORAGE.get()&&player.getPersistentData().getCompound(KEY).getInt("idleTicks")>FeruchemyConfig.IDLE_SECONDS.get()*20;}
    private StorageActivity(){}
}
