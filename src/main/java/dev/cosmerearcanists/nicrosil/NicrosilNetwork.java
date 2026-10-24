package dev.cosmerearcanists.nicrosil;

import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NicrosilNetwork {
    public static final UUID ZERO=new UUID(0,0);
    public static final int REFRESH=0,STORE_MODE=1,TAP_MODE=2,STORE=3,TAP=4,CLOSE=5,STOP=6;
    public static Consumer<Snapshot> receiver=p->{};
    public record Row(UUID entry,int kind,String key,String label,double amount,boolean enabled){public Row{if(kind<0||kind>2||key.length()>128||label.length()>128||!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid nicrosil row");}}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,id));}
    public record Request(int action,UUID session,UUID entry,int kind,String key,double amount) implements CustomPacketPayload {
        public static final Type<Request> TYPE=NicrosilNetwork.type("nicrosil_request");
        public Request(int action,UUID session,UUID entry,int kind,String key){this(action,session,entry,kind,key,0);}
        public Request{if(action<0||action>6||kind<0||kind>2||key.length()>128||!Double.isFinite(amount)||amount<0||amount>1024)throw new IllegalArgumentException("Invalid nicrosil request");}
        @Override public Type<Request> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>(){public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readVarInt(),b.readUUID(),b.readUUID(),b.readVarInt(),b.readUtf(128),b.readDouble());}public void encode(RegistryFriendlyByteBuf b,Request p){b.writeVarInt(p.action);b.writeUUID(p.session);b.writeUUID(p.entry);b.writeVarInt(p.kind);b.writeUtf(p.key,128);b.writeDouble(p.amount);}};
    }
    public record Snapshot(boolean open,UUID session,String label,boolean storing,boolean canStore,boolean unkeyed,double used,double capacity,List<Row> rows,String message,boolean active,String progress,boolean compounding) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=NicrosilNetwork.type("nicrosil_snapshot");
        public Snapshot(boolean open,UUID session,String label,boolean storing,boolean canStore,boolean unkeyed,double used,double capacity,List<Row> rows,String message,boolean active,String progress){this(open,session,label,storing,canStore,unkeyed,used,capacity,rows,message,active,progress,false);}
        public Snapshot(boolean open,UUID session,String label,boolean storing,boolean canStore,boolean unkeyed,double used,double capacity,List<Row> rows,String message){this(open,session,label,storing,canStore,unkeyed,used,capacity,rows,message,false,"");}
        public Snapshot{if(progress.length()>200)throw new IllegalArgumentException("Invalid storage progress");rows=List.copyOf(rows);if(rows.size()>256||label.length()>128||message.length()>200||!Double.isFinite(used)||!Double.isFinite(capacity)||used<0||capacity<0||used>capacity+1e-7)throw new IllegalArgumentException("Invalid nicrosil snapshot");}
        @Override public Type<Snapshot> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=new StreamCodec<>(){public Snapshot decode(RegistryFriendlyByteBuf b){boolean open=b.readBoolean();var token=b.readUUID();String label=b.readUtf(128);boolean storing=b.readBoolean(),canStore=b.readBoolean(),unkeyed=b.readBoolean();double used=b.readDouble(),capacity=b.readDouble();int n=b.readVarInt();if(n<0||n>256)throw new IllegalArgumentException("Too many nicrosil rows");var rows=new ArrayList<Row>();for(int i=0;i<n;i++)rows.add(new Row(b.readUUID(),b.readVarInt(),b.readUtf(128),b.readUtf(128),b.readDouble(),b.readBoolean()));return new Snapshot(open,token,label,storing,canStore,unkeyed,used,capacity,rows,b.readUtf(200),b.readBoolean(),b.readUtf(200),b.readBoolean());}public void encode(RegistryFriendlyByteBuf b,Snapshot p){b.writeBoolean(p.open);b.writeUUID(p.session);b.writeUtf(p.label,128);b.writeBoolean(p.storing);b.writeBoolean(p.canStore);b.writeBoolean(p.unkeyed);b.writeDouble(p.used);b.writeDouble(p.capacity);b.writeVarInt(p.rows.size());for(var row:p.rows){b.writeUUID(row.entry);b.writeVarInt(row.kind);b.writeUtf(row.key,128);b.writeUtf(row.label,128);b.writeDouble(row.amount);b.writeBoolean(row.enabled);}b.writeUtf(p.message,200);b.writeBoolean(p.active);b.writeUtf(p.progress,200);b.writeBoolean(p.compounding);}};
    }
    public static void register(PayloadRegistrar r){r.playToServer(Request.TYPE,Request.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p)NicrosilService.request(p,packet);}));r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(packet,context)->context.enqueueWork(()->receiver.accept(packet)));}
    private NicrosilNetwork(){}
}
