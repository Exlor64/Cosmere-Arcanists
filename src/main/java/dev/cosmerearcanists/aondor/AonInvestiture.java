package dev.cosmerearcanists.aondor;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import dev.cosmerearcanists.connection.WorldConnectionAccess;

/** All providers are authoritative server integrations, never client-declared fuel or Connection. */
public final class AonInvestiture {
    public interface Payment extends AutoCloseable {boolean commit();void refund();@Override void close();}
    @FunctionalInterface public interface Source {Payment reserve(ServerPlayer player,AonCost.Quote quote);}
    private static final Map<ResourceLocation,Source> SOURCES=new LinkedHashMap<>();
    private static final Payment AMBIENT=new Payment(){public boolean commit(){return true;}public void refund(){}public void close(){}};
    public static AutoCloseable register(ResourceLocation id,Source source){if(SOURCES.size()>=64||SOURCES.putIfAbsent(id,Objects.requireNonNull(source))!=null)throw new IllegalArgumentException("Duplicate/full Investiture source registry");return ()->SOURCES.remove(id,source);}
    public static boolean ambient(ServerPlayer player){return !AonDorConfig.ACCESS.get()||AonDorConfig.DOR_DIMENSIONS.get().contains(player.level().dimension().location().toString());}
    public static WorldConnectionAccess.State connection(ServerPlayer player){var base=WorldConnectionAccess.sample(player,"sel");return AonConnectionBoosts.apply(player,base);}
    public static void requireAccess(ServerPlayer player){if(ambient(player))return;var state=connection(player);if(!AonCost.connected(state.baseline(),state.effective(),AonDorConfig.CONNECTION_RATIO.get()))throw new IllegalArgumentException("Off-world AonDor requires "+AonDorConfig.CONNECTION_RATIO.get()+"× unboosted Connection to Sel");}
    public static String describe(ServerPlayer player){if(ambient(player))return "Unlimited ambient Dor";var c=connection(player);return String.format(Locale.ROOT,"Sel Connection %.3f / %.3f; external Investiture required",c.effective(),c.baseline()*AonDorConfig.CONNECTION_RATIO.get());}
    public static String execute(ServerPlayer player,AonCastPlan plan){
        if(!AonDorService.allowed(player))throw new IllegalArgumentException("AonDor is no longer available");requireAccess(player);var position=player.position();var dimension=player.level().dimension();boolean wasAmbient=ambient(player);Payment payment=null;
        if(wasAmbient)payment=AMBIENT;else for(var source:List.copyOf(SOURCES.values())){payment=source.reserve(player,plan.quote());if(payment!=null)break;}
        if(payment==null)throw new IllegalArgumentException("No external Investiture source can fund this spell (cost "+format(plan.quote().total())+")");
        try(var receipt=payment){
            requireAccess(player);if(!AonDorService.allowed(player)||player.position().distanceToSqr(position)>1e-12||player.level().dimension()!=dimension||ambient(player)!=wasAmbient)throw new IllegalArgumentException("Investiture access changed before payment");if(!receipt.commit())throw new IllegalArgumentException("External Investiture payment failed; no effects applied");
            final AonCastPlan.Outcome result;try{requireAccess(player);if(!AonDorService.allowed(player)||player.position().distanceToSqr(position)>1e-12||player.level().dimension()!=dimension||ambient(player)!=wasAmbient)throw new IllegalArgumentException("Investiture access changed during payment");result=plan.apply().get();}catch(RuntimeException error){receipt.refund();throw error;}
            if(!result.changed())receipt.refund();return result.message();
        }
    }
    public static String format(double units){return String.format(Locale.ROOT,units>0&&units<.001?"%.3g":"%.3f",units);}
    private AonInvestiture(){}
}
