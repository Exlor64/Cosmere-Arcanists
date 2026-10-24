package dev.cosmerearcanists.copper;

import java.nio.charset.StandardCharsets;

/** Logical data bytes, independent of NBT compression and packet encoding. */
public final class CopperCapacity {
    private CopperCapacity(){}
    public static long bytes(double charge,int bytesPerCharge){
        if(!Double.isFinite(charge)||charge<=0||bytesPerCharge<=0)return 0;
        return (long)Math.min(Long.MAX_VALUE,Math.floor(charge*bytesPerCharge));
    }
    public static long pageBytes(String title,String text,String dimension,int imageBytes){
        // UUID/type/position plus a fixed index allowance; all strings count UTF-8 bytes.
        return 64L+title.getBytes(StandardCharsets.UTF_8).length+text.getBytes(StandardCharsets.UTF_8).length+dimension.getBytes(StandardCharsets.UTF_8).length+Math.max(0,imageBytes);
    }
    public static double requiredCharge(long bytes,int bytesPerCharge){
        if(bytes<0||bytesPerCharge<=0)throw new IllegalArgumentException("Invalid copper capacity");
        // Round upward to avoid a one-byte shortage after a floating-point round trip.
        double result=(double)bytes/bytesPerCharge;
        return bytes(result,bytesPerCharge)>=bytes?result:Math.nextUp(result);
    }
}
