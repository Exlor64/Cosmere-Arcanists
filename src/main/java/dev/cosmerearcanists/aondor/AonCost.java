package dev.cosmerearcanists.aondor;

import java.util.*;

/** Relative Investiture work units, independent of the external source's storage/conversion units. */
public final class AonCost {
    public record Line(String kind,double units){public Line{if(!Double.isFinite(units)||units<0)throw new IllegalArgumentException("Spell cost is not finite");}}
    public record Quote(List<Line> lines,double total){public Quote{lines=List.copyOf(lines);if(!Double.isFinite(total)||total<0)throw new IllegalArgumentException("Spell cost is not finite");}}
    public static final class Builder {
        private final LinkedHashMap<String,Double> lines=new LinkedHashMap<>();private final double scale;
        public Builder(double scale){if(!Double.isFinite(scale)||scale<0)throw new IllegalArgumentException("Invalid cost scale");this.scale=scale;}
        public void add(String kind,double units){var line=new Line(kind,units);double next=lines.getOrDefault(kind,0.0)+line.units();if(!Double.isFinite(next))throw new IllegalArgumentException("Spell cost overflow");lines.put(kind,next);}
        public Quote quote(){var result=new ArrayList<Line>();double total=0;for(var entry:lines.entrySet()){var line=new Line(entry.getKey(),entry.getValue()*scale);result.add(line);total+=line.units();}return new Quote(result,total);}
    }
    static final ThreadLocal<Builder> ACTIVE=new ThreadLocal<>();
    static void add(String kind,double units){var builder=ACTIVE.get();if(builder!=null)builder.add(kind,units);}
    public static boolean connected(double baseline,double effective,double ratio){return Double.isFinite(baseline)&&baseline>0&&Double.isFinite(effective)&&effective>=0&&Double.isFinite(ratio)&&ratio>0&&effective/baseline>=ratio;}
    private AonCost(){}
}
