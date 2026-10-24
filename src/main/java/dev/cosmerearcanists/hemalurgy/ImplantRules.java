package dev.cosmerearcanists.hemalurgy;
import java.util.*;
/** Accepted diagram coordinates and compatibility; independent of Minecraft for verification. */
public final class ImplantRules {
    public static final int LIMIT=13;public static final long DAY=24000,WEEK=168000;
    public record Point(String id,boolean back,double u,double v){}
    public static final List<Point> POINTS=List.of(
        new Point("left_eye",false,0.6041666666666666,0.1041666666666667),
        new Point("right_eye",false,0.3958333333333333,0.1041666666666667),
        new Point("left_earlobe",false,0.75,0.175),
        new Point("right_earlobe",false,0.25,0.175),
        new Point("left_shoulder",false,0.8416666666666667,0.2875),
        new Point("right_shoulder",false,0.1583333333333333,0.2875),
        new Point("left_upper_arm",false,0.8833333333333333,0.3979166666666666),
        new Point("right_upper_arm",false,0.1166666666666667,0.3979166666666666),
        new Point("left_ribs",false,0.6583333333333333,0.5),
        new Point("right_ribs",false,0.3416666666666667,0.5),
        new Point("sternum",false,0.5,0.35),
        new Point("heart",false,0.5916666666666667,0.425),
        new Point("linchpin",true,0.5,0.3375));
    public static final Map<String,Set<String>> BINDINGS=Map.ofEntries(
        Map.entry("steel|a:steel",Set.of("sternum","left_eye","right_eye")),
        Map.entry("steel|a:iron",Set.of("left_eye","right_eye")),
        Map.entry("steel|a:pewter",Set.of("left_shoulder","right_shoulder","left_ribs","right_ribs")),
        Map.entry("steel|a:tin",Set.of("left_ribs","right_ribs")),
        Map.entry("pewter|f:iron",Set.of("left_shoulder","right_shoulder")),
        Map.entry("pewter|f:tin",Set.of("left_shoulder","right_shoulder")),
        Map.entry("pewter|f:pewter",Set.of("left_shoulder","right_shoulder")),
        Map.entry("pewter|f:steel",Set.of("heart")),
        Map.entry("bronze|a:brass",Set.of("left_ribs","right_ribs")),
        Map.entry("bronze|a:zinc",Set.of("left_ribs","right_ribs")),
        Map.entry("bronze|a:copper",Set.of("left_ribs","right_ribs","heart")),
        Map.entry("bronze|a:bronze",Set.of("left_ribs","right_ribs","left_earlobe","right_earlobe","left_upper_arm","right_upper_arm")),
        Map.entry("brass|f:zinc",Set.of("left_upper_arm","right_upper_arm")),
        Map.entry("brass|f:brass",Set.of("left_upper_arm","right_upper_arm")),
        Map.entry("brass|f:copper",Set.of("left_upper_arm","right_upper_arm")),
        Map.entry("brass|f:bronze",Set.of("left_upper_arm","right_upper_arm")),
        Map.entry("cadmium|a:gold",Set.of("left_ribs","right_ribs")),
        Map.entry("cadmium|a:electrum",Set.of("left_ribs","right_ribs")),
        Map.entry("cadmium|a:cadmium",Set.of("left_ribs","right_ribs")),
        Map.entry("cadmium|a:bendalloy",Set.of("left_ribs","right_ribs")),
        Map.entry("cadmium|a:atium",Set.of("left_ribs","right_ribs")),
        Map.entry("gold|f:gold",Set.of("left_ribs","right_ribs")),
        Map.entry("gold|f:electrum",Set.of("left_ribs","right_ribs")),
        Map.entry("gold|f:cadmium",Set.of("left_ribs","right_ribs")),
        Map.entry("gold|f:bendalloy",Set.of("left_ribs","right_ribs")),
        Map.entry("electrum|a:aluminum",Set.of("left_ribs","right_ribs")),
        Map.entry("bendalloy|f:aluminum",Set.of("sternum")),
        Map.entry("electrum|a:duralumin",Set.of("left_ribs","right_ribs")),
        Map.entry("bendalloy|f:duralumin",Set.of("sternum")),
        Map.entry("electrum|a:chromium",Set.of("left_ribs","right_ribs")),
        Map.entry("bendalloy|f:chromium",Set.of("sternum")),
        Map.entry("electrum|a:nicrosil",Set.of("left_ribs","right_ribs")),
        Map.entry("bendalloy|f:nicrosil",Set.of("sternum")),
        Map.entry("iron|trait:strength",Set.of("left_shoulder","right_shoulder")),
        Map.entry("tin|trait:senses",Set.of("left_shoulder","right_shoulder")),
        Map.entry("copper|trait:memory",Set.of("left_shoulder","right_shoulder")),
        Map.entry("zinc|trait:fortitude",Set.of("left_shoulder","right_shoulder")),
        Map.entry("duralumin|trait:connection",Set.of("heart")));
    public static boolean body(double u,double v){return Double.isFinite(u)&&Double.isFinite(v)&&v>=0&&v<=1&&u>=0&&u<=1&&(v>=.25&&v<=.625||u>=.25&&u<=.75);}
    public static int nearest(boolean back,double u,double v){if(!body(u,v))return -1;int found=-1;double best=Double.POSITIVE_INFINITY;for(int i=0;i<POINTS.size();i++){var p=POINTS.get(i);double distance=distance(p,u,v);if(p.back()==back&&distance<best){best=distance;found=i;}}return found;}
    public static double distance(Point point,double u,double v){return Math.hypot((point.u()-u)*.5,point.v()-v);}
    public static double accuracy(int point,double u,double v,double radius){if(point<0||point>=LIMIT||!body(u,v)||!Double.isFinite(radius)||radius<=0)return 0;return Math.max(0,1-distance(POINTS.get(point),u,v)/radius);}
    public static boolean compatible(String metal,String power,int point){if(point<0||point>=LIMIT)return false;var regions=BINDINGS.get(metal+"|"+power);return regions!=null&&(point==12||regions.contains(POINTS.get(point).id()));}
    public static double activeCost(int count,boolean linchpin,double cost,double extra){return Math.max(0,count)*cost*(count>4&&!linchpin?1+extra:1);}
    public static double removalDamage(int point,int before){return point==12?(before>4?10:4):2;}
    public static double takeover(double integrity){return 1+Math.max(0,-integrity);}
    private ImplantRules(){}
}
