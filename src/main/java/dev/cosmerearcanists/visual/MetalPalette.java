package dev.cosmerearcanists.visual;
import java.util.Map;
/** Presentation colors shared by ingots, spikes, loose shavings and mixed vial contents. */
public final class MetalPalette {
    private static final Map<String,Integer> COLORS=Map.ofEntries(
        Map.entry("iron",0xD8D8D8),Map.entry("steel",0x5D7289),Map.entry("tin",0xBFCFD0),Map.entry("pewter",0x999FAC),
        Map.entry("zinc",0x90A6B9),Map.entry("brass",0xD2AE45),Map.entry("copper",0xE49265),Map.entry("bronze",0xAE703B),
        Map.entry("aluminum",0xD7DFEB),Map.entry("duralumin",0xBECADC),Map.entry("chromium",0x9BADC7),Map.entry("nicrosil",0x8BA9AF),
        Map.entry("gold",0xFFD65C),Map.entry("electrum",0xE8D279),Map.entry("cadmium",0xAAB89C),Map.entry("bendalloy",0x89939B),
        Map.entry("lead",0x5D657E),Map.entry("silver",0xE2E7EB),Map.entry("nickel",0xA0AC91),Map.entry("atium",0xC2BED5),Map.entry("lerasium",0xDFEAC5));
    public static int color(String metal){return 0xFF000000|COLORS.getOrDefault(metal,0xFFFFFF);}
    public static java.util.Set<String> metals(){return COLORS.keySet();}
    /** Empty vials remain untinted. Weighted averaging represents the actual filing mixture. */
    public static int mixture(Map<String,Integer> contents){long count=0,r=0,g=0,b=0;for(var e:contents.entrySet()){long n=Math.max(0,e.getValue());int c=color(e.getKey());count+=n;r+=((c>>>16)&255)*n;g+=((c>>>8)&255)*n;b+=(c&255)*n;}return count==0?-1:0xFF000000|((int)((r+count/2)/count)<<16)|((int)((g+count/2)/count)<<8)|(int)((b+count/2)/count);}
    private MetalPalette(){}
}
