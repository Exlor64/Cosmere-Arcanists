package dev.cosmerearcanists.mobs;
import java.util.*;
/** Shared successful-birth cooldowns; keys never depend on the mob's later position. */
public final class ChunkPowerLedger {
    public static final int HARD_LIMIT=262144;
    public record Key(String dimension,long chunk){public Key{if(dimension==null||dimension.isBlank()||dimension.length()>128)throw new IllegalArgumentException("Dimension key");}}
    private final Map<Key,Long> until=new HashMap<>();
    private long nextPrune;
    public synchronized int size(){return until.size();}
    public synchronized Map<Key,Long> entries(){return Map.copyOf(until);}
    public synchronized void restore(Key key,long expiry){if(expiry>=0&&(until.containsKey(key)||until.size()<HARD_LIMIT))until.merge(key,expiry,Math::max);}
    private void prune(long now){if(now>=nextPrune||until.size()>=HARD_LIMIT){until.values().removeIf(expiry->expiry<=now);nextPrune=now>Long.MAX_VALUE-1200?Long.MAX_VALUE:now+1200;}}
    public synchronized boolean available(Key key,long now){
        if(now<0)return false;prune(now);Long expiry=until.get(key);
        if(expiry!=null&&expiry<=now){until.remove(key);expiry=null;}
        return expiry==null&&until.size()<HARD_LIMIT;
    }
    public synchronized boolean claim(Key key,long now,long ticks){
        if(ticks<=0)return true;if(!available(key,now))return false;
        until.put(key,now>Long.MAX_VALUE-ticks?Long.MAX_VALUE:now+ticks);return true;
    }
}
