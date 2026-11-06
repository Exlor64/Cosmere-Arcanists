package dev.cosmerearcanists.aondor;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
/** Old engine integration fixtures explicitly designate their test dimension as ambient Dor, restoring config immediately. */
final class AonTestCasting {
    static String execute(ServerPlayer player,AonDorNetwork.Check request){var previous=AonDorConfig.DOR_DIMENSIONS.get();var worlds=new ArrayList<String>(previous);worlds.add(player.level().dimension().location().toString());try{AonDorConfig.DOR_DIMENSIONS.set(worlds);return AonDorService.execute(player,request);}finally{AonDorConfig.DOR_DIMENSIONS.set(previous);}}
    private AonTestCasting(){}
}
