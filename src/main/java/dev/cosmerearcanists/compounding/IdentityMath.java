package dev.cosmerearcanists.compounding;
import java.util.*;
/** Blank Identity is compatible, but cannot join two differently keyed portions into a single source. */
public final class IdentityMath {
    public record Portion(UUID identity,double strength){public Portion{if(!Double.isFinite(strength)||strength<=0)throw new IllegalArgumentException("Invalid power portion");}}
    public record Pair(UUID identity,double allomancy,double feruchemy){}
    public static Pair best(List<Portion> allomancy,List<Portion> feruchemy){var identities=new HashSet<UUID>();identities.add(null);allomancy.forEach(p->identities.add(p.identity()));feruchemy.forEach(p->identities.add(p.identity()));Pair best=new Pair(null,0,0);for(var identity:identities){double a=matching(allomancy,identity),f=matching(feruchemy,identity);if(a*f>best.allomancy()*best.feruchemy())best=new Pair(identity,a,f);}return best;}
    /** Only Identities actually possessed through implantation may share one compatible pool. Neutral cannot bridge unrelated keys. */
    public static Pair best(List<Portion> allomancy,List<Portion> feruchemy,Set<UUID> possessed){if(possessed.size()<2)return best(allomancy,feruchemy);UUID canonical=possessed.stream().min(UUID::compareTo).orElseThrow();return best(allomancy.stream().map(p->p.identity()!=null&&possessed.contains(p.identity())?new Portion(canonical,p.strength()):p).toList(),feruchemy.stream().map(p->p.identity()!=null&&possessed.contains(p.identity())?new Portion(canonical,p.strength()):p).toList());}
    private static double matching(List<Portion> list,UUID identity){return list.stream().filter(p->p.identity()==null||Objects.equals(p.identity(),identity)).mapToDouble(Portion::strength).sum();}
    public static double fraction(double matched,double total){return Double.isFinite(matched)&&Double.isFinite(total)&&total>0?Math.max(0,Math.min(1,matched/total)):0;}
    private IdentityMath(){}
}
