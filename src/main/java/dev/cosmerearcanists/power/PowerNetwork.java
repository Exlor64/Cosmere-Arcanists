package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PowerNetwork {
    /** Assigned by physical-client initialization; no common class references a client class. */
    public static Consumer<Snapshot> clientReceiver = packet -> { };
    public static Consumer<Impulse> impulseReceiver = packet -> { };
    public record Entry(String power,int state,double reserve,int storeLevels,int tapLevels,double committedReserve,int burstTicks){
        public Entry(String power,int state,double reserve,int storeLevels,int tapLevels){this(power,state,reserve,storeLevels,tapLevels,0,0);}
        public Entry(String power,int state,double reserve){this(power,state,reserve,5,5);}
        public Entry{if(!Double.isFinite(committedReserve)||committedReserve<0||burstTicks<0||storeLevels<0||tapLevels<0||storeLevels>100000||tapLevels>100000)throw new IllegalArgumentException("Invalid power level limits");}
    }
    public record Snapshot(String dimension, String selected, String secondary, int levels, List<Entry> powers, List<MetalTarget> targets) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "power_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b) {
                String dimension = b.readUtf(128), selected = b.readUtf(32), secondary=b.readUtf(32); int levels = b.readVarInt();
                int n = b.readVarInt(); if (n < 0 || n > 64) throw new IllegalArgumentException("Invalid powers");
                var powers = new ArrayList<Entry>(); for (int i = 0; i < n; i++) powers.add(new Entry(b.readUtf(32),b.readInt(),b.readDouble(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readVarInt()));
                n = b.readVarInt(); if (n < 0 || n > 256) throw new IllegalArgumentException("Invalid targets");
                var targets = new ArrayList<MetalTarget>(); for (int i = 0; i < n; i++) targets.add(new MetalTarget(b.readInt(), b.readInt(), b.readBlockPos()));
                return new Snapshot(dimension, selected, secondary, levels, List.copyOf(powers), List.copyOf(targets));
            }
            public void encode(RegistryFriendlyByteBuf b, Snapshot p) {
                b.writeUtf(p.dimension, 128); b.writeUtf(p.selected, 32); b.writeUtf(p.secondary,32); b.writeVarInt(p.levels); b.writeVarInt(p.powers.size());
                for (var e : p.powers) { b.writeUtf(e.power, 32); b.writeInt(e.state); b.writeDouble(e.reserve);b.writeVarInt(e.storeLevels);b.writeVarInt(e.tapLevels);b.writeDouble(e.committedReserve);b.writeVarInt(e.burstTicks); }
                b.writeVarInt(p.targets.size()); for (var t : p.targets) { b.writeInt(t.entityId()); b.writeInt(t.slot()); b.writeBlockPos(t.block()); }
            }
        };
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    /** Private debt status, synchronized only to the affected player. */
    public static Consumer<PewterStatus> pewterReceiver = packet -> { };
    public record PewterStatus(String dimension, double debt, boolean burning, double strength) implements CustomPacketPayload {
        public static final Type<PewterStatus> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"pewter_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PewterStatus> CODEC = new StreamCodec<>() {
            public PewterStatus decode(RegistryFriendlyByteBuf b){return new PewterStatus(b.readUtf(128),b.readDouble(),b.readBoolean(),b.readDouble());}
            public void encode(RegistryFriendlyByteBuf b,PewterStatus p){b.writeUtf(p.dimension,128);b.writeDouble(p.debt);b.writeBoolean(p.burning);b.writeDouble(p.strength);}
        };
        @Override public Type<PewterStatus> type(){return TYPE;}
    }
    public static Consumer<TinStatus> tinReceiver = packet -> { };
    public static Consumer<TinEcho> echoReceiver = packet -> { };
    public record TinStatus(String dimension,double strength,double zoom,boolean wireframes,float vision,int sense) implements CustomPacketPayload {
        public TinStatus(String dimension,double strength,double zoom,boolean wireframes){this(dimension,strength,zoom,wireframes,0,0);}
        public TinStatus(String dimension,double strength,double zoom,boolean wireframes,float vision){this(dimension,strength,zoom,wireframes,vision,0);}
        public TinStatus {if(!Double.isFinite(strength)||strength<0||!Double.isFinite(zoom)||zoom<1||zoom>20||!Float.isFinite(vision)||vision<0||vision>1||sense<0||sense>2)throw new IllegalArgumentException("Invalid tin status");}
        public static final Type<TinStatus> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"tin_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,TinStatus> CODEC=new StreamCodec<>() {
            public TinStatus decode(RegistryFriendlyByteBuf b){return new TinStatus(b.readUtf(128),b.readDouble(),b.readDouble(),b.readBoolean(),b.readFloat(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,TinStatus p){b.writeUtf(p.dimension,128);b.writeDouble(p.strength);b.writeDouble(p.zoom);b.writeBoolean(p.wireframes);b.writeFloat(p.vision);b.writeVarInt(p.sense);}
        };
        @Override public Type<TinStatus> type(){return TYPE;}
    }
    public record TinEcho(String dimension,net.minecraft.world.phys.Vec3 position,int entityId,net.minecraft.world.phys.AABB sourceBox,int ticks,float audible,List<net.minecraft.world.phys.AABB> surfaces) implements CustomPacketPayload {
        public static final Type<TinEcho> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"tin_echo"));
        private static net.minecraft.world.phys.AABB box(RegistryFriendlyByteBuf b){return new net.minecraft.world.phys.AABB(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble());}
        private static void box(RegistryFriendlyByteBuf b,net.minecraft.world.phys.AABB a){b.writeDouble(a.minX);b.writeDouble(a.minY);b.writeDouble(a.minZ);b.writeDouble(a.maxX);b.writeDouble(a.maxY);b.writeDouble(a.maxZ);}
        public static final StreamCodec<RegistryFriendlyByteBuf,TinEcho> CODEC=new StreamCodec<>() {
            public TinEcho decode(RegistryFriendlyByteBuf b){
                String d=b.readUtf(128);var p=new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble());int id=b.readInt();var source=box(b);int ticks=b.readVarInt();float audible=b.readFloat();int count=b.readVarInt();
                if(count<0 || count>256 || ticks<1 || ticks>200 || !Float.isFinite(audible))throw new IllegalArgumentException("Invalid tin echo");
                var boxes=new ArrayList<net.minecraft.world.phys.AABB>();for(int i=0;i<count;i++)boxes.add(box(b));return new TinEcho(d,p,id,source,ticks,audible,List.copyOf(boxes));
            }
            public void encode(RegistryFriendlyByteBuf b,TinEcho p){b.writeUtf(p.dimension,128);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeInt(p.entityId);box(b,p.sourceBox);b.writeVarInt(p.ticks);b.writeFloat(p.audible);b.writeVarInt(p.surfaces.size());for(var a:p.surfaces)box(b,a);}
        };
        @Override public Type<TinEcho> type(){return TYPE;}
    }
    /** action: 0 select, 1 set state, 2 activate selected ability hold, 3 cycle state, 4 request snapshot, 5 primary pulse, 6 toggle highlighted power slot, 7 secondary hold, 8 secondary pulse, 9 grapple Space input, 10 set emotional mode, 11 set Feruchemical tin sense (0 sight, 1 hearing, 2 touch). Activation value bits: held=1, alternate=2, forward=4, back=8, left=16, right=32, jump=64, descend=128. */
    public record Control(int action, String power, int value) implements CustomPacketPayload {
        public static final Type<Control> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "power_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = new StreamCodec<>() {
            public Control decode(RegistryFriendlyByteBuf b) { return new Control(b.readVarInt(), b.readUtf(32), b.readInt()); }
            public void encode(RegistryFriendlyByteBuf b, Control p) { b.writeVarInt(p.action); b.writeUtf(p.power, 32); b.writeInt(p.value); }
        };
        @Override public Type<Control> type() { return TYPE; }
    }
    /** Add a server-authorized impulse to the recipient's actual client velocity. */
    public record Impulse(String dimension, double x, double y, double z, double maxSpeed, boolean resistWalking) implements CustomPacketPayload {
        public static final Type<Impulse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "power_impulse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Impulse> CODEC = new StreamCodec<>() {
            public Impulse decode(RegistryFriendlyByteBuf b) { return new Impulse(b.readUtf(128), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf b, Impulse p) { b.writeUtf(p.dimension,128); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeDouble(p.maxSpeed); b.writeBoolean(p.resistWalking); }
        };
        @Override public Type<Impulse> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("34");
        dev.cosmerearcanists.hemalurgy.HemalurgyNetwork.register(r);
        dev.cosmerearcanists.copper.CopperNetwork.register(r);
        dev.cosmerearcanists.nicrosil.NicrosilNetwork.register(r);
        r.playToClient(dev.cosmerearcanists.connection.ConnectionPacket.TYPE,dev.cosmerearcanists.connection.ConnectionPacket.CODEC,(packet,context)->context.enqueueWork(()->dev.cosmerearcanists.connection.ConnectionPacket.receiver.accept(packet)));
        r.playToClient(ZincReactionPacket.TYPE,ZincReactionPacket.CODEC,(packet,context)->context.enqueueWork(()->ZincReactionPacket.receiver.accept(packet)));
        r.playToClient(BronzeWarningPacket.TYPE,BronzeWarningPacket.CODEC,(packet,context)->context.enqueueWork(()->BronzeWarningPacket.receiver.accept(packet)));
        r.playToClient(BubblePacket.TYPE,BubblePacket.CODEC,(packet,context)->context.enqueueWork(()->BubblePacket.receiver.accept(packet)));
        r.playToClient(GoldPacket.TYPE,GoldPacket.CODEC,(packet,context)->context.enqueueWork(()->GoldPacket.receiver.accept(packet)));
        r.playToClient(EmotionPacket.TYPE,EmotionPacket.CODEC,(packet,context)->context.enqueueWork(()->EmotionPacket.receiver.accept(packet)));
        r.playToClient(SeekingPacket.TYPE,SeekingPacket.CODEC,(packet,context)->context.enqueueWork(()->SeekingPacket.receiver.accept(packet)));
        r.playToClient(ScoutPacket.TYPE,ScoutPacket.CODEC,(packet,context)->context.enqueueWork(()->ScoutPacket.receiver.accept(packet)));
        r.playToClient(TemporalPacket.TYPE,TemporalPacket.CODEC,(packet,context)->context.enqueueWork(()->TemporalPacket.receiver.accept(packet)));
        r.playToClient(TinStatus.TYPE,TinStatus.CODEC,(packet,context)->context.enqueueWork(()->tinReceiver.accept(packet)));
        r.playToClient(TinEcho.TYPE,TinEcho.CODEC,(packet,context)->context.enqueueWork(()->echoReceiver.accept(packet)));
        r.playToClient(PewterStatus.TYPE, PewterStatus.CODEC, (packet, context) -> context.enqueueWork(() -> pewterReceiver.accept(packet)));
        r.playToClient(Impulse.TYPE, Impulse.CODEC, (packet, context) -> context.enqueueWork(() -> impulseReceiver.accept(packet)));
        r.playToClient(Snapshot.TYPE, Snapshot.CODEC, (packet, context) -> context.enqueueWork(() -> clientReceiver.accept(packet)));
        r.playToServer(ScoutLook.TYPE,ScoutLook.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p)ScoutService.look(p,packet);}));
        r.playToServer(Control.TYPE, Control.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PowerService.control(player, packet);
        }));
    }
    public static void sync(ServerPlayer p, List<MetalTarget> targets) {
        double tin=Math.max(TinService.sightStrength(p),TinService.hearingStrength(p));double sight=TinService.sightStrength(p);
        PacketDistributor.sendToPlayer(p,new TinStatus(p.level().dimension().location().toString(),tin,sight>0 && TinConfig.ZOOM.get() && (PowerService.holdingAbility(p,"a:tin")||dev.cosmerearcanists.feruchemy.TinFeruchemy.sense(p)==0&&PowerService.holdingAbility(p,"f:tin"))?TinMath.zoom(sight,TinConfig.ZOOM_STRENGTH.get(),TinConfig.MAX_ZOOM.get()):1,TinService.revealStrength(p)>0,(float)TinService.nightVision(p),dev.cosmerearcanists.feruchemy.TinFeruchemy.sense(p)));
        double pewter=PewterService.strength(p);
        PacketDistributor.sendToPlayer(p,new PewterStatus(p.level().dimension().location().toString(),PewterService.debt(p),pewter>0,pewter));
        var list = PowerData.available(p).stream().map(power -> new Entry(power, PowerData.controlState(p, power), power.startsWith("a:") ? PowerData.reserve(p, power.substring(2)) : dev.cosmerearcanists.feruchemy.Metalminds.accessibleCharge(p,power.equals("f:tin")?dev.cosmerearcanists.feruchemy.TinFeruchemy.channel(p):power.substring(2)),PowerData.storeLevels(p,power),PowerData.tapLevels(p,power),power.startsWith("f:")?dev.cosmerearcanists.feruchemy.FeruchemicalEnhancement.committedFuel(p,power.equals("f:tin")?dev.cosmerearcanists.feruchemy.TinFeruchemy.channel(p):power.substring(2)):0,power.startsWith("f:")?dev.cosmerearcanists.feruchemy.FeruchemicalEnhancement.committedTicks(p,power.equals("f:tin")?dev.cosmerearcanists.feruchemy.TinFeruchemy.channel(p):power.substring(2)):0)).toList();
        PacketDistributor.sendToPlayer(p, new Snapshot(p.level().dimension().location().toString(), PowerData.selected(p), PowerData.secondary(p), PowerConfig.FERUCHEMY_LEVELS.get(), list, targets));
    }
    private PowerNetwork() { }
}
