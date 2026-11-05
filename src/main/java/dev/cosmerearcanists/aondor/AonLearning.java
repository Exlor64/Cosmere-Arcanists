package dev.cosmerearcanists.aondor;

import dev.cosmerearcanists.power.PowerData;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Only server recognition may add knowledge. PowerData's normal clone/save path preserves it. */
public final class AonLearning {
    private static final String KEY="aondor_known";
    public static final int MAX_KNOWN=1024;
    public static Set<String> known(ServerPlayer player){
        var tags=PowerData.data(player).getList(KEY,Tag.TAG_STRING);var ids=new LinkedHashSet<String>();
        for(int i=0;i<Math.min(tags.size(),MAX_KNOWN);i++){String id=tags.getString(i);if(id.length()<=128&&net.minecraft.resources.ResourceLocation.tryParse(id)!=null)ids.add(id);}
        return Set.copyOf(ids);
    }
    public static boolean learn(ServerPlayer player,AonRecognition.Template template){
        var ids=new TreeSet<>(known(player));if(ids.size()>=MAX_KNOWN||!ids.add(template.id()))return false;
        var tags=new ListTag();ids.forEach(id->tags.add(StringTag.valueOf(id)));PowerData.data(player).put(KEY,tags);return true;
    }
    private AonLearning(){}
}
