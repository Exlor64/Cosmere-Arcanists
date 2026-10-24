package dev.cosmerearcanists.nicrosil;

import java.util.*;

/** Authoritative contents: copied items reference the same inventory, never independent powers. */
public final class NicrosilLedger {
    public static final int POWER=0,RESOURCE=1,TEMPORARY=2,HARD_ENTRIES=256;
    public record Entry(UUID id,int kind,String key,double amount,double charge,UUID sourceIdentity){
        public Entry(UUID id,int kind,String key,double amount,double charge){this(id,kind,key,amount,charge,null);}
        public Entry{Objects.requireNonNull(id);Objects.requireNonNull(key);if(kind<0||kind>2||key.isBlank()||key.length()>128||!Double.isFinite(amount)||amount<=0||!Double.isFinite(charge)||charge<=0)throw new IllegalArgumentException("Invalid nicrosil content");}
    }
    public static final class Mind {
        public final UUID id,owner;public final double capacity;
        private final Map<UUID,Entry> contents=new LinkedHashMap<>();
        public Mind(UUID id,UUID owner,double capacity){this.id=Objects.requireNonNull(id);this.owner=owner;this.capacity=capacity;if(!Double.isFinite(capacity)||capacity<=0)throw new IllegalArgumentException("Invalid nicrosilmind capacity");}
        public List<Entry> entries(){return List.copyOf(contents.values());}
        public Entry entry(UUID id){return contents.get(id);}
        public double used(){return contents.values().stream().mapToDouble(Entry::charge).sum();}
        public boolean accepts(Entry e,int limit){return contents.size()<Math.min(HARD_ENTRIES,limit)&&!contents.containsKey(e.id())&&e.charge()<=capacity-used()+1e-8;}
        public boolean add(Entry e,int limit){if(!accepts(e,limit))return false;contents.put(e.id(),e);return true;}
        private Entry merging(Entry incoming){for(var e:contents.values())if(e.kind()==incoming.kind()&&e.key().equals(incoming.key())&&Objects.equals(e.sourceIdentity(),incoming.sourceIdentity())&&(e.kind()!=POWER||e.amount()+incoming.amount()<=1024)&&(e.kind()==TEMPORARY?Math.abs(e.amount()-incoming.amount())<1e-9:Math.abs(e.charge()/e.amount()-incoming.charge()/incoming.amount())<=1e-9*Math.max(1,e.charge()/e.amount())))return e;return null;}
        public boolean canDeposit(Entry e,int limit){return !contents.containsKey(e.id())&&e.charge()<=capacity-used()+1e-8&&(merging(e)!=null||contents.size()<Math.min(HARD_ENTRIES,limit));}
        /** Consecutive fractions share a row and preserve its ID; a changed conversion cost uses a new row. */
        public boolean deposit(Entry incoming,int limit){if(!canDeposit(incoming,limit))return false;var previous=merging(incoming);if(previous==null)contents.put(incoming.id(),incoming);else contents.put(previous.id(),new Entry(previous.id(),previous.kind(),previous.key(),previous.kind()==TEMPORARY?previous.amount():previous.amount()+incoming.amount(),previous.charge()+incoming.charge(),previous.sourceIdentity()));return true;}
        public void reduce(Entry e){var old=contents.get(e.id());if(old==null||e.charge()>old.charge()+1e-8)throw new IllegalArgumentException("Not a reduction");contents.put(e.id(),e);}
        public Entry remove(UUID id){return contents.remove(id);}
        public boolean accessible(UUID user,boolean requireOwner){return owner==null||!requireOwner||owner.equals(user);}
    }
    private final Map<UUID,Mind> minds=new LinkedHashMap<>();
    public int size(){return minds.size();}
    public Collection<Mind> minds(){return List.copyOf(minds.values());}
    public Mind get(UUID id){return minds.get(id);}
    public boolean add(Mind mind,int limit){if(minds.size()>=limit||minds.containsKey(mind.id))return false;minds.put(mind.id,mind);return true;}
    public boolean erase(UUID id){return minds.remove(id)!=null;}
    private static boolean finitePositive(double n){return Double.isFinite(n)&&n>0;}
    public static double powerCharge(double strength,double rate){double value=strength*rate;return finitePositive(strength)&&strength<=1024&&finitePositive(rate)&&finitePositive(value)?value:0;}
    /** Recombine transferred portions without replacing existing strength or clipping at the attribute ceiling. */
    public static boolean canReceive(double existingBase,double stored){return Double.isFinite(existingBase)&&existingBase>=0&&finitePositive(stored)&&existingBase+stored<=1024;}
}
