package dev.cosmerearcanists.nicrosil;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import static dev.cosmerearcanists.nicrosil.NicrosilNetwork.*;

/** Explicit menu choices start gradual, conserved power storage; stored portions grant permanent strength. */
public final class NicrosilService {
    private static final String KEY="cosmerearcanists_nicrosilmind",METALMIND="cosmerearcanists_metalmind";
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Integer> MODES=new HashMap<>();
    private static final Map<UUID,Long> LAST_ACTION=new HashMap<>();
    private static final Map<UUID,Storage> STORING=new HashMap<>();
    private static final Map<UUID,Long> STORAGE_TICKS=new HashMap<>();
    private static final Map<UUID,String> STATUS=new HashMap<>();
    private record CompoundChoice(int kind,String key,double strength,UUID identity){}
    private static final Map<UUID,CompoundChoice> COMPOUND=new HashMap<>();
    private static boolean compounding(ServerPlayer p){return dev.cosmerearcanists.compounding.CompoundingService.active(p,"nicrosil");}
    private static int menuMode(ServerPlayer p){return compounding(p)?-1:ferring(p)?Integer.signum(PowerData.state(p,"f:nicrosil")):0;}
    /** Menu selection chooses an encoding, not a second source metalmind or a permanent-power transfer. */
    public static double compound(ServerPlayer p,double charge){var choice=COMPOUND.get(p.getUUID());if(choice==null||!compounding(p)||!Double.isFinite(charge)||charge<=0)return 0;
        double strength=choice.strength();if(choice.kind()==NicrosilLedger.TEMPORARY){if(!NicrosilConfig.TEMPORARY.get()||!PowerData.powerEnabled(choice.key()))return 0;double owned=dev.cosmerearcanists.compounding.PowerSources.personal(p,choice.key()).stream().filter(v->Objects.equals(v.identity(),choice.identity())).mapToDouble(v->v.strength()).sum();if(owned+1e-9<strength)return 0;}
        else if(!NicrosilConfig.RESOURCES.get()||NicrosilResources.get(choice.key())==null)return 0;
        double moved=0;var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());for(var access:Metalminds.touching(p)){var stack=access.stack();if(!seen.add(stack)||!physical(stack)||!writable(p,stack))continue;var mind=contents(p,stack);double space=mind==null?Metalminds.capacity(stack):mind.capacity-mind.used();double step=Math.min(charge-moved,space);if(step<=0)continue;double amount=choice.kind()==NicrosilLedger.TEMPORARY?strength:step/NicrosilResources.get(choice.key()).chargePerUnit();if(!Double.isFinite(amount)||amount<=0)continue;var e=new NicrosilLedger.Entry(UUID.randomUUID(),choice.kind(),choice.key(),amount,step,Metalminds.unkeyedForStorage(p,"nicrosil")?null:choice.identity());if(depositCompounded(p,stack,e))moved+=step;if(moved>=charge)break;}return moved;
    }

    private static final class Storage {
        final ServerPlayer player;final ItemStack stack;final Object level;final String power;final double target,cost;double remaining;UUID mind;
        Storage(Session s,String power,double target){player=s.player;stack=s.stack;level=player.level();this.power=power;this.target=remaining=target;cost=NicrosilConfig.POWER_CHARGE.get();mind=identity(stack);}
    }
    public static boolean storing(ServerPlayer p){return STORING.containsKey(p.getUUID());}
    public static void stopStorage(ServerPlayer p,String reason){var plan=STORING.remove(p.getUUID());if(plan!=null)STATUS.put(p.getUUID(),String.format(Locale.ROOT,"%s: %.4f / %.4f stored. %s",NicrosilPowers.label(plan.power),plan.target-plan.remaining,plan.target,reason));}
    private static final Set<UUID> BUSY=new HashSet<>();
    public static final class Session {
        public final ServerPlayer player;public final ItemStack stack;public UUID token=UUID.randomUUID();public boolean storing;
        final Object level;long expires;
        Session(ServerPlayer player,ItemStack stack,boolean storing){this.player=player;this.stack=stack;this.storing=storing;level=player.level();expires=player.server.overworld().getGameTime()+12000;}
    }
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&NicrosilConfig.ENABLED.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.NICROSIL).get();}
    private static boolean alive(ServerPlayer p){return !dev.cosmerearcanists.hemalurgy.AluminumSuppression.active(p)&&configured()&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!p.getAbilities().invulnerable;}
    private static boolean ferring(ServerPlayer p){return alive(p)&&PowerAttributes.strength(p,"f:nicrosil")>0;}
    public static UUID identity(ItemStack stack){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return data.hasUUID(KEY)?data.getUUID(KEY):ZERO;}
    public static boolean specialized(ItemStack stack){return !identity(stack).equals(ZERO);}
    public static NicrosilLedger.Mind contents(ServerPlayer p,ItemStack stack){return NicrosilData.get(p.server).ledger.get(identity(stack));}
    private static boolean physical(ItemStack stack){var source=Metalminds.source(stack);return stack.getCount()==1&&source!=null&&source.metal().equals("nicrosil");}
    private static boolean access(ServerPlayer p,ItemStack stack){
        if(!alive(p)||!physical(stack)||Metalminds.capacity(stack)<=0)return false;var mind=contents(p,stack);
        if(specialized(stack)&&mind==null)return false;
        return mind==null?ferring(p)&&Metalminds.usableBy(stack,p,"nicrosil")&&!Metalminds.bound(stack):(mind.owner==null||!FeruchemyConfig.OWNERSHIP.get()||dev.cosmerearcanists.hemalurgy.IdentityAccess.accepts(p,mind.owner))&&(mind.owner==null||ferring(p));
    }
    public static boolean canAccess(ServerPlayer p,ItemStack stack){return access(p,stack);}
    private static boolean writable(ServerPlayer p,ItemStack stack){var mind=contents(p,stack);return ferring(p)&&!StorageActivity.blocked(p)&&access(p,stack)&&(mind==null||((mind.owner==null)==Metalminds.unkeyedForStorage(p,"nicrosil")));}
    private static boolean valid(Session s){if(s==null||s.level!=s.player.level()||s.expires<s.player.server.overworld().getGameTime()||!access(s.player,s.stack))return false;return Metalminds.touching(s.player).stream().anyMatch(a->a.stack()==s.stack);}
    private static void changed(ServerPlayer p,ItemStack stack){for(var a:Metalminds.touching(p))if(a.stack()==stack)a.changed().run();p.getInventory().setChanged();p.containerMenu.broadcastChanges();}
    /** Mirror only for tooltips/processing guards; the SavedData ledger remains authoritative. */
    private static void mirror(ItemStack stack,NicrosilLedger.Mind mind){
        var root=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var tag=root.getCompound(METALMIND);tag.putDouble("capacity",mind.capacity);tag.putDouble("charge",mind.used());
        if(mind.entries().isEmpty()){root.remove(KEY);root.remove("cosmerearcanists_nicrosil_count");tag.remove("owner");tag.remove("metal");tag.remove("unkeyed");}
        else{root.putUUID(KEY,mind.id);root.putInt("cosmerearcanists_nicrosil_count",mind.entries().size());tag.putString("metal","nicrosil");if(mind.owner==null){tag.remove("owner");tag.putBoolean("unkeyed",true);}else{tag.putUUID("owner",mind.owner);tag.remove("unkeyed");}}
        root.put(METALMIND,tag);stack.set(DataComponents.MAX_STACK_SIZE,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
    }
    /** Authoritative acquisition integration. Creates one neutral permanent power in an empty singleton. */
    public static boolean initializeUnkeyedPower(net.minecraft.server.MinecraftServer server, ItemStack stack,
                                                 String power, double strength) {
        if (!server.isSameThread() || !configured() || !NicrosilConfig.POWERS.get() || !physical(stack)
            || specialized(stack) || Metalminds.bound(stack) || !PowerAttributes.keys().contains(power)
            || !PowerData.powerEnabled(power)) return false;
        double charge = NicrosilLedger.powerCharge(strength, NicrosilConfig.POWER_CHARGE.get());
        if (charge <= 0 || charge > Metalminds.capacity(stack)) return false;
        var data = NicrosilData.get(server);
        var mind = new NicrosilLedger.Mind(UUID.randomUUID(), null, Metalminds.capacity(stack));
        if (!mind.add(new NicrosilLedger.Entry(UUID.randomUUID(), NicrosilLedger.POWER, power, strength, charge, null),
            NicrosilConfig.ENTRIES.get()) || !data.ledger.add(mind, NicrosilConfig.MINDS.get())) return false;
        mirror(stack, mind); data.setDirty(); return true;
    }

    /** Wiping a copied item erases its shared record too; stale copies can never resurrect consumed contents. */
    public static boolean eraseItem(ItemStack stack){if(!specialized(stack))return true;var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return false;var data=NicrosilData.get(server);if(data.ledger.erase(identity(stack)))data.setDirty();var root=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();root.remove(KEY);root.remove("cosmerearcanists_nicrosil_count");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return true;}
    public static Session session(ServerPlayer p){return SESSIONS.get(p.getUUID());}
    public static Session open(ServerPlayer p,ItemStack preferred){
        if(!alive(p))return null;ItemStack selected=null;
        for(var a:Metalminds.touching(p)){var stack=a.stack();if(preferred!=null&&preferred!=stack)continue;if(physical(stack)&&specialized(stack)&&contents(p,stack)==null){Metalminds.wipe(stack);a.changed().run();}if(access(p,stack)){selected=stack;break;}}
        if(selected==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Hold or wear an accessible nicrosilmind. Unkeyed nicrosilminds can be tapped by anyone."),true);return null;}
        var contents=contents(p,selected);if(contents!=null){mirror(selected,contents);changed(p,selected);}
        var s=new Session(p,selected,compounding(p)||ferring(p)&&PowerData.state(p,"f:nicrosil")<0);SESSIONS.put(p.getUUID(),s);snapshot(s,"");return s;
    }
    private static void closeMenu(ServerPlayer p){MODES.put(p.getUUID(),menuMode(p));if(SESSIONS.remove(p.getUUID())!=null)PacketDistributor.sendToPlayer(p,new Snapshot(false,ZERO,"",false,false,false,0,0,List.of(),"",false,""));}
    public static void clear(ServerPlayer p){COMPOUND.remove(p.getUUID());stopStorage(p,"");closeMenu(p);MODES.remove(p.getUUID());LAST_ACTION.remove(p.getUUID());STORAGE_TICKS.remove(p.getUUID());STATUS.remove(p.getUUID());}

    private static double amount(NicrosilResources.Provider provider,ServerPlayer p,double space){double available=provider.available(p),rate=provider.chargePerUnit();if(!Double.isFinite(available)||available<=0||!Double.isFinite(rate)||rate<=0)return 0;return Math.max(0,Math.min(Math.min(available,NicrosilConfig.RESOURCE_BATCH.get()),space/rate));}
    private static boolean storePowerAllowed(ServerPlayer p,ItemStack stack,String key){return NicrosilConfig.POWERS.get()&&PowerData.available(p).contains(key)&&NicrosilPowers.personal(p,key)>0&&(!key.equals("f:nicrosil")||NicrosilConfig.SELF.get()&&Metalminds.unkeyedForStorage(p,"nicrosil"));}
    private static boolean receivePower(ServerPlayer p,NicrosilLedger.Entry e){return NicrosilConfig.POWERS.get()&&PowerAttributes.keys().contains(e.key())&&NicrosilLedger.canReceive(PowerAttributes.baseStrength(p,e.key()),e.amount())&&PowerData.powerEnabled(e.key())&&dev.cosmerearcanists.compounding.PowerSources.canGrant(p,e.key(),e.sourceIdentity());}
    private static boolean receiveResource(ServerPlayer p,NicrosilLedger.Entry e){var provider=NicrosilResources.get(e.key());return NicrosilConfig.RESOURCES.get()&&provider!=null&&provider.canReceive(p,e.amount());}
    private static void snapshot(Session s,String message){
        if(!valid(s)){clear(s.player);return;}var p=s.player;var mind=contents(p,s.stack);double used=mind==null?0:mind.used(),capacity=mind==null?Metalminds.capacity(s.stack):mind.capacity;boolean writable=writable(p,s.stack);var rows=new ArrayList<Row>();
        if(s.storing){for(String key:PowerAttributes.keys())if(PowerAttributes.strength(p,key)>0){double strength=NicrosilPowers.personal(p,key);double step=NicrosilProgress.step(strength,strength,capacity-used,NicrosilConfig.POWER_CHARGE.get(),NicrosilConfig.POWER_RATE.get(),Metalminds.storageRate(p));boolean fits=step>0&&step*NicrosilConfig.POWER_CHARGE.get()>0&&(mind==null||mind.canDeposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,key,step,step*NicrosilConfig.POWER_CHARGE.get()),NicrosilConfig.ENTRIES.get()));rows.add(new Row(ZERO,NicrosilLedger.POWER,key,NicrosilPowers.label(key),strength,writable&&(compounding(p)?NicrosilConfig.TEMPORARY.get()&&strength>0:storePowerAllowed(p,s.stack,key)&&fits)));}
            if(NicrosilConfig.RESOURCES.get())for(var entry:NicrosilResources.providers().entrySet()){var provider=entry.getValue();double available=provider.available(p);if(Double.isFinite(available)&&available>0&&rows.size()<256){double value=amount(provider,p,capacity-used);rows.add(new Row(ZERO,NicrosilLedger.RESOURCE,entry.getKey(),provider.label()+" (batch)",value,writable&&value>0&&(mind==null||mind.entries().size()<NicrosilConfig.ENTRIES.get())));}}
        }else if(mind!=null){for(var entry:mind.entries()){String label=(entry.kind()==NicrosilLedger.POWER||entry.kind()==NicrosilLedger.TEMPORARY)&&PowerAttributes.keys().contains(entry.key())?NicrosilPowers.label(entry.key())+(entry.kind()==NicrosilLedger.TEMPORARY?String.format(Locale.ROOT," · temporary %.1fs",entry.charge()/entry.amount()/NicrosilConfig.TEMP_COST.get()):""):NicrosilResources.get(entry.key())==null?entry.key():NicrosilResources.get(entry.key()).label();rows.add(new Row(entry.id(),entry.kind(),entry.key(),label,entry.amount(),entry.kind()==NicrosilLedger.POWER?receivePower(p,entry):entry.kind()==NicrosilLedger.TEMPORARY?TemporaryPowers.allowed(p,entry):receiveResource(p,entry)));}}
        PacketDistributor.sendToPlayer(p,new Snapshot(true,s.token,truncate(s.stack.getHoverName().getString(),128),s.storing,ferring(p),mind==null?Metalminds.unkeyedForStorage(p,"nicrosil"):mind.owner==null,used,capacity,rows,truncate(message.isEmpty()?STATUS.getOrDefault(p.getUUID(),""):message,200),storing(p)||COMPOUND.containsKey(p.getUUID())||TemporaryPowers.active(p),progress(p).isEmpty()?TemporaryPowers.progress(p):progress(p),compounding(p)));
    }
    private static String truncate(String value,int max){return value.length()>max?value.substring(0,max):value;}
    /** Compounded output keeps its source Identity unless paid aluminum suppression removes it. */
    public static boolean canDepositCompounded(ServerPlayer p,ItemStack stack,NicrosilLedger.Entry entry){if(!writable(p,stack))return false;if(entry.kind()==NicrosilLedger.TEMPORARY&&!NicrosilConfig.TEMPORARY.get()||entry.kind()==NicrosilLedger.RESOURCE&&!NicrosilConfig.RESOURCES.get())return false;var mind=contents(p,stack);return mind==null?NicrosilData.get(p.server).ledger.size()<NicrosilConfig.MINDS.get()&&entry.charge()<=Metalminds.capacity(stack):mind.canDeposit(entry,NicrosilConfig.ENTRIES.get());}
    public static boolean depositCompounded(ServerPlayer p,ItemStack stack,NicrosilLedger.Entry entry){if(!canDepositCompounded(p,stack,entry))return false;var data=NicrosilData.get(p.server);var mind=contents(p,stack);boolean fresh=mind==null;if(fresh)mind=new NicrosilLedger.Mind(UUID.randomUUID(),Metalminds.unkeyedForStorage(p,"nicrosil")?null:dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p),Metalminds.capacity(stack));var stored=new NicrosilLedger.Entry(entry.id(),entry.kind(),entry.key(),entry.amount(),entry.charge(),mind.owner==null?null:entry.sourceIdentity());if(!mind.deposit(stored,NicrosilConfig.ENTRIES.get()))return false;if(fresh)data.ledger.add(mind,NicrosilConfig.MINDS.get());data.setDirty();mirror(stack,mind);changed(p,stack);return true;}
    public static double drainEntry(ServerPlayer p,ItemStack stack,UUID id,double requested){if(!Double.isFinite(requested)||requested<=0||!access(p,stack))return 0;var data=NicrosilData.get(p.server);var mind=contents(p,stack);var entry=mind==null?null:mind.entry(id);if(entry==null)return 0;double moved=Math.min(requested,entry.charge()),left=entry.charge()-moved;if(left<=0)mind.remove(id);else mind.reduce(new NicrosilLedger.Entry(id,entry.kind(),entry.key(),entry.kind()==NicrosilLedger.TEMPORARY?entry.amount():entry.amount()*left/entry.charge(),left,entry.sourceIdentity()));if(mind.entries().isEmpty())data.ledger.erase(mind.id);data.setDirty();mirror(stack,mind);changed(p,stack);return moved;}
    private static String progress(ServerPlayer p){var plan=STORING.get(p.getUUID());if(compounding(p)&&COMPOUND.containsKey(p.getUUID()))return "Compounding temporary charge: "+COMPOUND.get(p.getUUID()).key();return plan==null?"":String.format(Locale.ROOT,"%s: %.4f / %.4f",NicrosilPowers.label(plan.power),plan.target-plan.remaining,plan.target);}
    /** The menu arms a target, and only server ticks move power. Requested zero means all current personal strength. */
    public static String store(Session s,int kind,String key){return store(s,kind,key,0);}
    public static String store(Session s,int kind,String key,double requested){
        if(!valid(s)||!s.storing||!writable(s.player,s.stack))return "Storage requires your nicrosil power, usable contact and the matching Identity state.";
        var p=s.player;var data=NicrosilData.get(p.server);var mind=contents(p,s.stack);
        if(mind==null&&data.ledger.size()>=NicrosilConfig.MINDS.get())return "The server nicrosilmind limit has been reached.";
        if(compounding(p)){
            if(kind==NicrosilLedger.POWER){if(!NicrosilConfig.TEMPORARY.get()||NicrosilPowers.personal(p,key)<=0||!PowerData.powerEnabled(key))return "Choose one of your personal powers.";var part=dev.cosmerearcanists.compounding.PowerSources.personal(p,key).getFirst();double strength=NicrosilProgress.target(part.strength(),requested);if(strength<=0)return "Choose a positive strength.";COMPOUND.put(p.getUUID(),new CompoundChoice(NicrosilLedger.TEMPORARY,key,strength,part.identity()));}
            else if(kind==NicrosilLedger.RESOURCE&&NicrosilConfig.RESOURCES.get()&&NicrosilResources.get(key)!=null&&NicrosilResources.get(key).available(p)>0){COMPOUND.put(p.getUUID(),new CompoundChoice(NicrosilLedger.RESOURCE,key,0,null));}
            else return "This encoding is unavailable.";
            s.token=UUID.randomUUID();return "Allomantic reserves now fill temporary charge; your personal power is retained.";
        }
        if(kind==NicrosilLedger.POWER){
            if(!storePowerAllowed(p,s.stack,key))return "That power cannot be stored. Nicrosil itself needs an unkeyed destination.";
            double target=NicrosilProgress.target(NicrosilPowers.personal(p,key),requested);if(target<=0)return "Choose a positive amount, or leave the amount blank for all.";
            double capacity=mind==null?Metalminds.capacity(s.stack):mind.capacity,used=mind==null?0:mind.used();double step=NicrosilProgress.step(target,NicrosilPowers.personal(p,key),capacity-used,NicrosilConfig.POWER_CHARGE.get(),NicrosilConfig.POWER_RATE.get(),Metalminds.storageRate(p));
            if(step<=0||step*NicrosilConfig.POWER_CHARGE.get()<=0||mind!=null&&!mind.canDeposit(new NicrosilLedger.Entry(UUID.randomUUID(),0,key,step,step*NicrosilConfig.POWER_CHARGE.get()),NicrosilConfig.ENTRIES.get()))return "This nicrosilmind has no capacity or entry slots left.";
            STORING.put(p.getUUID(),new Storage(s,key,target));STATUS.remove(p.getUUID());s.token=UUID.randomUUID();return "Gradual storage started. Stop at any time to keep the remaining strength.";
        }
        if(kind!=NicrosilLedger.RESOURCE)return "Unknown content type.";
        var provider=NicrosilResources.get(key);if(!NicrosilConfig.RESOURCES.get()||provider==null)return "This Investiture type is unavailable.";
        boolean fresh=mind==null;if(fresh)mind=new NicrosilLedger.Mind(UUID.randomUUID(),Metalminds.unkeyedForStorage(p,"nicrosil")?null:dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p),Metalminds.capacity(s.stack));
        double amount=amount(provider,p,Math.max(0,mind.capacity-mind.used())),charge=amount*provider.chargePerUnit();if(amount<=0||!Double.isFinite(charge)||charge<=0)return "There is nothing storable or no capacity left.";
        var entry=new NicrosilLedger.Entry(UUID.randomUUID(),kind,key,amount,charge);if(!mind.accepts(entry,NicrosilConfig.ENTRIES.get()))return "This nicrosilmind has insufficient capacity or entry slots.";
        if(!BUSY.add(p.getUUID()))return "A transfer is already in progress.";
        try{if(!provider.extract(p,amount))return "The Investiture source could not complete the transfer.";stopStorage(p,"Another type selected.");mind.add(entry,NicrosilConfig.ENTRIES.get());if(fresh)data.ledger.add(mind,NicrosilConfig.MINDS.get());data.setDirty();mirror(s.stack,mind);changed(p,s.stack);s.token=UUID.randomUUID();pulse(p,amount);return "Investiture stored.";}finally{BUSY.remove(p.getUUID());}
    }
    /** Production passes the world gameTime. An explicit clock also lets GameTests check same-tick deduplication. */
    public static void storageTick(ServerPlayer p,long tick){
        var plan=STORING.get(p.getUUID());if(plan==null||tick<=STORAGE_TICKS.getOrDefault(p.getUUID(),Long.MIN_VALUE))return;STORAGE_TICKS.put(p.getUUID(),tick);
        if(plan.player!=p||plan.level!=p.level()||PowerData.state(p,"f:nicrosil")>=0||!plan.mind.equals(identity(plan.stack))||!writable(p,plan.stack)||!Metalminds.touching(p).stream().anyMatch(a->a.stack()==plan.stack)||!storePowerAllowed(p,plan.stack,plan.power)){stopStorage(p,"Storage stopped.");return;}
        var data=NicrosilData.get(p.server);var mind=contents(p,plan.stack);boolean fresh=mind==null;if(fresh){if(data.ledger.size()>=NicrosilConfig.MINDS.get()){stopStorage(p,"Server capacity reached.");return;}mind=new NicrosilLedger.Mind(UUID.randomUUID(),Metalminds.unkeyedForStorage(p,"nicrosil")?null:dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p),Metalminds.capacity(plan.stack));}
        var source=dev.cosmerearcanists.compounding.PowerSources.personal(p,plan.power).getFirst();double before=NicrosilPowers.personal(p,plan.power),step=NicrosilProgress.step(plan.remaining,Math.min(before,source.strength()),Math.max(0,mind.capacity-mind.used()),plan.cost,NicrosilConfig.POWER_RATE.get(),Metalminds.storageRate(p));double after=Math.max(0,before-step),actual=before-after;
        double charge=actual*plan.cost;if(actual<=0||charge<=0||!Double.isFinite(charge)){stopStorage(p,"Target or capacity reached.");return;}var entry=new NicrosilLedger.Entry(UUID.randomUUID(),0,plan.power,actual,charge,Metalminds.unkeyedForStorage(p,"nicrosil")?null:source.identity());
        if(!mind.canDeposit(entry,NicrosilConfig.ENTRIES.get())){stopStorage(p,"No entry slots left.");return;}if(!BUSY.add(p.getUUID()))return;
        try{PowerAttributes.set(p,plan.power,after);if(after==0)PowerService.powerTransferred(p,plan.power);else PowerData.state(p,plan.power,PowerData.controlState(p,plan.power));mind.deposit(entry,NicrosilConfig.ENTRIES.get());if(fresh)data.ledger.add(mind,NicrosilConfig.MINDS.get());data.setDirty();mirror(plan.stack,mind);plan.mind=mind.id;plan.remaining=Math.max(0,plan.remaining-actual);changed(p,plan.stack);if(p.tickCount%5==0)pulse(p,actual);if(plan.remaining==0||after==0)stopStorage(p,"Complete.");}finally{BUSY.remove(p.getUUID());}
    }
    public static String tap(Session s,UUID id){
        if(!valid(s)||s.storing)return "The nicrosilmind is no longer accessible.";var p=s.player;var data=NicrosilData.get(p.server);var mind=contents(p,s.stack);var entry=mind==null?null:mind.entry(id);if(entry==null)return "This entry has already been removed.";
        if(entry.kind()==NicrosilLedger.TEMPORARY)return TemporaryPowers.start(p,s.stack,entry)?"Temporary power tapping started; contact and charge are required.":"This temporary power is unavailable.";
        if(entry.kind()==NicrosilLedger.POWER&&!receivePower(p,entry))return "This transfer would exceed the power limit, or the power is disabled. The entry was kept.";
        if(entry.kind()==NicrosilLedger.RESOURCE&&!receiveResource(p,entry))return "This Investiture type is unavailable or you cannot receive the full entry.";
        if(!BUSY.add(p.getUUID()))return "A transfer is already in progress.";
        try{
            if(entry.kind()==NicrosilLedger.POWER){dev.cosmerearcanists.compounding.PowerSources.grant(p,entry.key(),entry.amount(),entry.sourceIdentity());PowerData.state(p,entry.key(),0);}
            else if(!NicrosilResources.get(entry.key()).insert(p,entry.amount()))return "The Investiture recipient could not complete the transfer.";
            mind.remove(id);if(mind.entries().isEmpty())data.ledger.erase(mind.id);data.setDirty();mirror(s.stack,mind);changed(p,s.stack);s.token=UUID.randomUUID();pulse(p,entry.amount());
            return entry.kind()==NicrosilLedger.POWER?"Power gained permanently.":"Investiture retrieved.";
        }finally{BUSY.remove(p.getUUID());}
    }
    public static void request(ServerPlayer p,Request packet){
        var s=session(p);if(s==null)return;if(packet.action()==CLOSE){closeMenu(p);return;}if(!s.token.equals(packet.session()))return;
        if(!valid(s)){clear(p);return;}
        if(packet.action()==STOP){COMPOUND.remove(p.getUUID());stopStorage(p,"Stopped.");TemporaryPowers.stop(p);s.token=UUID.randomUUID();snapshot(s,"");return;}
        long now=p.server.overworld().getGameTime();if(now-LAST_ACTION.getOrDefault(p.getUUID(),Long.MIN_VALUE/2)<NicrosilConfig.INTERVAL.get())return;LAST_ACTION.put(p.getUUID(),now);s.expires=now+12000;
        String message="";switch(packet.action()){case STORE_MODE->{if(ferring(p)){s.storing=true;if(!compounding(p))PowerData.state(p,"f:nicrosil",-1);}}case TAP_MODE->{stopStorage(p,"Tapping selected.");s.storing=false;if(ferring(p)&&!compounding(p))PowerData.state(p,"f:nicrosil",1);}case STORE->{message=store(s,packet.kind(),packet.key(),packet.amount());}case TAP->{message=tap(s,packet.entry());}default->{}}
        snapshot(s,message);PowerNetwork.sync(p,List.of());
    }
    private static void pulse(ServerPlayer p,double amount){var signal=new dev.cosmerearcanists.investiture.InvestitureSignals.Signal(dev.cosmerearcanists.investiture.InvestitureSignals.FERUCHEMY,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"nicrosil"),p.getUUID(),p.position(),Math.max(.01,Math.min(4096,amount)),1,40,1,true);dev.cosmerearcanists.investiture.InvestitureSignals.emit(p.serverLevel(),signal,10);}
    public static void tick(ServerPlayer p){
        storageTick(p,p.level().getGameTime());
        if(!compounding(p))COMPOUND.remove(p.getUUID());
        int mode=menuMode(p);int previous=MODES.getOrDefault(p.getUUID(),0);MODES.put(p.getUUID(),mode);
        if(mode!=0&&mode!=previous&&session(p)==null)open(p,null);
        var s=session(p);if(s!=null){if(!valid(s))closeMenu(p);else if(p.tickCount%5==0)snapshot(s,"");}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void use(PlayerInteractEvent.RightClickItem e){if(!physical(e.getItemStack())||dev.cosmerearcanists.compounding.CompoundingService.nugget(e.getItemStack()))return;if(e.getEntity() instanceof ServerPlayer p&&open(p,e.getItemStack())!=null){e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer old)clear(old);if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void stop(ServerStoppedEvent e){COMPOUND.clear();SESSIONS.clear();MODES.clear();LAST_ACTION.clear();STORING.clear();STORAGE_TICKS.clear();STATUS.clear();BUSY.clear();}
}
