package dev.cosmerearcanists.nicrosil;

import dev.cosmerearcanists.compounding.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Temporary powers are paid transient modifiers, never personal base attributes or permanent grants. */
public final class TemporaryPowers {
    private static final class Lease {
        final UUID mind,entry,identity;final String power;final double strength;final ResourceLocation modifier;long paid=Long.MIN_VALUE;double funded;
        Lease(UUID mind,NicrosilLedger.Entry e){this.mind=mind;entry=e.id();identity=e.sourceIdentity();power=e.key();strength=e.amount();modifier=ResourceLocation.fromNamespaceAndPath("cosmerearcanists","temporary_nicrosil_"+mind+"_"+entry);}
    }
    private static final Map<ServerPlayer,List<Lease>> LEASES=new WeakHashMap<>();
    private static final Map<UUID,Long> TICKS=new HashMap<>();
    static{PowerSources.registerExternal(TemporaryPowers::sources);}
    public static boolean allowed(ServerPlayer p,NicrosilLedger.Entry e){return NicrosilService.configured()&&NicrosilConfig.TEMPORARY.get()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&e.kind()==NicrosilLedger.TEMPORARY&&e.amount()<=1024&&PowerAttributes.keys().contains(e.key())&&PowerData.powerEnabled(e.key());}
    public static boolean start(ServerPlayer p,ItemStack stack,NicrosilLedger.Entry e){if(!allowed(p,e)||!NicrosilService.canAccess(p,stack))return false;var leases=LEASES.computeIfAbsent(p,k->new ArrayList<>());if(leases.stream().anyMatch(l->l.entry.equals(e.id())&&l.mind.equals(NicrosilService.identity(stack))))return true;if(leases.size()>=16)return false;leases.add(new Lease(NicrosilService.identity(stack),e));return true;}
    public static boolean active(ServerPlayer p){return LEASES.containsKey(p)&&!LEASES.get(p).isEmpty();}
    public static String progress(ServerPlayer p){return active(p)?LEASES.get(p).size()+" temporary power(s) selected; contact and charge required":"";}
    private static ItemStack find(ServerPlayer p,Lease lease){for(var a:dev.cosmerearcanists.feruchemy.Metalminds.touching(p))if(NicrosilService.identity(a.stack()).equals(lease.mind)&&NicrosilService.canAccess(p,a.stack()))return a.stack();return ItemStack.EMPTY;}
    public static List<IdentityMath.Portion> sources(ServerPlayer p,String key){var result=new ArrayList<IdentityMath.Portion>();for(var lease:LEASES.getOrDefault(p,List.of()))if(lease.power.equals(key)&&lease.paid==p.level().getGameTime()&&lease.funded>0)result.add(new IdentityMath.Portion(lease.identity,lease.strength*lease.funded));return result;}
    private static void remove(ServerPlayer p,Lease l){var a=PowerAttributes.instance(p,l.power);if(a!=null)a.removeModifier(l.modifier);}
    public static void tick(ServerPlayer p){tickAt(p,p.level().getGameTime());}
    /** Explicit clock is used by synchronous GameTests; production always uses world time. */
    public static void tickAt(ServerPlayer p,long tick){var leases=LEASES.get(p);if(leases==null||tick<=TICKS.getOrDefault(p.getUUID(),Long.MIN_VALUE))return;TICKS.put(p.getUUID(),tick);for(var it=leases.iterator();it.hasNext();){var l=it.next();remove(p,l);var stack=find(p,l);var mind=NicrosilService.contents(p,stack);var e=mind==null?null:mind.entry(l.entry);if(e==null||!allowed(p,e)){it.remove();continue;}double cost=l.strength*NicrosilConfig.TEMP_COST.get()/20;double actual=NicrosilService.drainEntry(p,stack,l.entry,cost);l.funded=Math.min(1,actual/cost);l.paid=tick;if(l.funded<=0){it.remove();continue;}var a=PowerAttributes.instance(p,l.power);if(a!=null)a.addTransientModifier(new AttributeModifier(l.modifier,l.strength*l.funded,AttributeModifier.Operation.ADD_VALUE));}if(leases.isEmpty())LEASES.remove(p);}
    public static void stop(ServerPlayer p){for(var l:LEASES.getOrDefault(p,List.of()))remove(p,l);LEASES.remove(p);TICKS.remove(p.getUUID());}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer p)stop(p);if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){for(var p:List.copyOf(LEASES.keySet()))stop(p);TICKS.clear();}
}
