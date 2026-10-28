package dev.cosmerearcanists.mobs;
import java.util.*;
import java.util.function.IntUnaryOperator;
/** One birth roll, then an art choice and independent single/full-set choices. */
public final class NaturalPowerRoll {
    public static List<String> roll(int oneIn,boolean both,boolean sets,int setOneIn,List<String> allomancy,List<String> feruchemy,IntUnaryOperator random){
        if(oneIn<1||setOneIn<1||allomancy.isEmpty()&&feruchemy.isEmpty()||random.applyAsInt(oneIn)!=0)return List.of();
        int art=allomancy.isEmpty()?1:feruchemy.isEmpty()?0:random.applyAsInt(both?3:2);
        var result=new ArrayList<String>();
        if(art!=1)add(result,allomancy,sets,setOneIn,random);
        if(art!=0)add(result,feruchemy,sets,setOneIn,random);
        return List.copyOf(result);
    }
    private static void add(List<String> result,List<String> pool,boolean sets,int oneIn,IntUnaryOperator random){
        if(sets&&random.applyAsInt(oneIn)==0)result.addAll(pool);else result.add(pool.get(random.applyAsInt(pool.size())));
    }
    private NaturalPowerRoll(){}
}
