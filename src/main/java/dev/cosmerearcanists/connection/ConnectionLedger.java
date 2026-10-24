package dev.cosmerearcanists.connection;

import java.util.*;

/** Symmetric relationships, with separate persistent proximity and interaction clocks. */
public final class ConnectionLedger {
    public record Pair(UUID first,UUID second){
        public Pair{Objects.requireNonNull(first);Objects.requireNonNull(second);if(first.equals(second))throw new IllegalArgumentException("Self connection");if(first.compareTo(second)>0){var swap=first;first=second;second=swap;}}
        public UUID other(UUID player){return first.equals(player)?second:first;}
    }
    public record Bond(double strength,long proximityTick,long interactionTick){public Bond{if(!Double.isFinite(strength)||strength<=0||strength>100)throw new IllegalArgumentException("Invalid bond strength");}}
    private final Map<Pair,Bond> bonds=new LinkedHashMap<>();
    private final Map<UUID,Set<Pair>> index=new HashMap<>();
    public Map<Pair,Bond> entries(){return Collections.unmodifiableMap(bonds);}
    public Set<Pair> connections(UUID player){return Collections.unmodifiableSet(index.getOrDefault(player,Set.of()));}
    public double strength(UUID a,UUID b){if(a.equals(b))return 0;var bond=bonds.get(new Pair(a,b));return bond==null?0:bond.strength();}
    public void restore(Pair pair,Bond bond){if(!bonds.containsKey(pair)&&(bonds.size()>=262144||connections(pair.first()).size()>=256||connections(pair.second()).size()>=256))return;bonds.put(pair,bond);index.computeIfAbsent(pair.first(),k->new LinkedHashSet<>()).add(pair);index.computeIfAbsent(pair.second(),k->new LinkedHashSet<>()).add(pair);}
    public void remove(UUID player){for(var pair:List.copyOf(connections(player))){bonds.remove(pair);for(var endpoint:List.of(pair.first(),pair.second())){var entries=index.get(endpoint);if(entries!=null){entries.remove(pair);if(entries.isEmpty())index.remove(endpoint);}}}}
    public boolean grow(UUID a,UUID b,double amount,long now,boolean interaction,long interval,int maxBonds,int maxPairs){
        if(a.equals(b)||!Double.isFinite(amount)||amount<=0)return false;var key=new Pair(a,b);var old=bonds.get(key);
        if(old==null&&(bonds.size()>=maxPairs||connections(a).size()>=maxBonds||connections(b).size()>=maxBonds))return false;
        long previous=old==null?Long.MIN_VALUE:interaction?old.interactionTick():old.proximityTick();
        if(previous!=Long.MIN_VALUE&&(now<previous||now-previous<interval))return false;
        double strength=Math.min(100,(old==null?0:old.strength())+amount);if(old!=null&&strength==old.strength())return false;
        restore(key,new Bond(strength,interaction?(old==null?Long.MIN_VALUE:old.proximityTick()):now,interaction?now:old==null?Long.MIN_VALUE:old.interactionTick()));return true;
    }
}
