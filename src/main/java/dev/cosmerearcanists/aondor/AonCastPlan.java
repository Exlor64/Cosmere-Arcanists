package dev.cosmerearcanists.aondor;
import java.util.function.Supplier;

/** Complete validated work and a deferred application. Source payment happens between these phases. */
public record AonCastPlan(AonCost.Quote quote,Supplier<Outcome> apply) {
    public record Outcome(String message,boolean changed){}
    static Supplier<Outcome> none(String message){return ()->new Outcome(message,false);}
}
