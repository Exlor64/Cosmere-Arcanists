package dev.cosmerearcanists.copper;
import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
public final class CopperNetwork {
    public static final UUID ZERO=new UUID(0,0);
    public static final int OPEN=0,NEXT=1,SAVE=2,LOCATION=3,DELETE=4,TRACK=5,IMAGE=6,BEGIN=7,CHUNK=8,FINISH=9;
    public static Consumer<Snapshot> snapshotReceiver=p->{};public static Consumer<Image> imageReceiver=p->{};public static Consumer<Waypoint> waypointReceiver=p->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,id));}
    public record Request(int action,UUID session,UUID entry,String title,String text,int number,byte[] bytes) implements CustomPacketPayload {
        public static final Type<Request> TYPE=CopperNetwork.type("copper_request");
        public Request{if(action<0||action>9||title.length()>80||text.length()>2048||bytes.length>CopperImages.CHUNK)throw new IllegalArgumentException("Invalid copper request");bytes=bytes.clone();}
        @Override public Type<Request> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>(){public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readVarInt(),b.readUUID(),b.readUUID(),b.readUtf(80),b.readUtf(2048),b.readInt(),b.readByteArray(CopperImages.CHUNK));}public void encode(RegistryFriendlyByteBuf b,Request p){b.writeVarInt(p.action);b.writeUUID(p.session);b.writeUUID(p.entry);b.writeUtf(p.title,80);b.writeUtf(p.text,2048);b.writeInt(p.number);b.writeByteArray(p.bytes);}};
    }
    private static CopperPage page(RegistryFriendlyByteBuf b){return new CopperPage(b.readUUID(),b.readVarInt(),b.readUtf(80),b.readUtf(2048),b.readUtf(128),b.readBlockPos(),b.readVarInt());}
    private static void page(RegistryFriendlyByteBuf b,CopperPage p){b.writeUUID(p.id());b.writeVarInt(p.kind());b.writeUtf(p.title(),80);b.writeUtf(p.text(),2048);b.writeUtf(p.dimension(),128);b.writeBlockPos(p.position());b.writeVarInt(p.imageBytes());}
    public record Snapshot(boolean open,UUID session,UUID book,String label,boolean editable,List<CopperPage> pages,String message,long usedBytes,long capacityBytes,long maximumBytes) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=CopperNetwork.type("copper_snapshot");
        public Snapshot(boolean open,UUID session,UUID book,String label,boolean editable,List<CopperPage> pages,String message){this(open,session,book,label,editable,pages,message,0,0,0);}
        public Snapshot{pages=List.copyOf(pages);if(usedBytes<0||capacityBytes<0||maximumBytes<capacityBytes||pages.size()>64||label.length()>128||message.length()>200||pages.stream().mapToInt(p->p.text().length()).sum()>65536)throw new IllegalArgumentException("Invalid notebook snapshot");}
        @Override public Type<Snapshot> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=new StreamCodec<>(){public Snapshot decode(RegistryFriendlyByteBuf b){boolean o=b.readBoolean();var session=b.readUUID();var book=b.readUUID();String label=b.readUtf(128);boolean edit=b.readBoolean();int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("Too many memories");var pages=new ArrayList<CopperPage>();for(int i=0;i<count;i++)pages.add(page(b));return new Snapshot(o,session,book,label,edit,pages,b.readUtf(200),b.readLong(),b.readLong(),b.readLong());}public void encode(RegistryFriendlyByteBuf b,Snapshot p){b.writeBoolean(p.open);b.writeUUID(p.session);b.writeUUID(p.book);b.writeUtf(p.label,128);b.writeBoolean(p.editable);b.writeVarInt(p.pages.size());p.pages.forEach(row->page(b,row));b.writeUtf(p.message,200);b.writeLong(p.usedBytes);b.writeLong(p.capacityBytes);b.writeLong(p.maximumBytes);}};
    }
    public record Image(UUID session,UUID entry,int total,int offset,byte[] bytes) implements CustomPacketPayload {
        public static final Type<Image> TYPE=CopperNetwork.type("copper_image");
        public Image{if(total<8||total>CopperImages.HARD_BYTES||offset<0||bytes.length<1||bytes.length>CopperImages.CHUNK||offset>total||bytes.length>total-offset)throw new IllegalArgumentException("Invalid image chunk");bytes=bytes.clone();}
        @Override public Type<Image> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Image> CODEC=new StreamCodec<>(){public Image decode(RegistryFriendlyByteBuf b){return new Image(b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readByteArray(CopperImages.CHUNK));}public void encode(RegistryFriendlyByteBuf b,Image p){b.writeUUID(p.session);b.writeUUID(p.entry);b.writeInt(p.total);b.writeInt(p.offset);b.writeByteArray(p.bytes);}};
    }
    public record Waypoint(boolean enabled,String title,String dimension,BlockPos position) implements CustomPacketPayload {
        public static final Type<Waypoint> TYPE=CopperNetwork.type("copper_waypoint");
        public Waypoint{if(title.length()>80||dimension.length()>128)throw new IllegalArgumentException("Invalid waypoint");}
        @Override public Type<Waypoint> type(){return TYPE;}
        public static final StreamCodec<RegistryFriendlyByteBuf,Waypoint> CODEC=new StreamCodec<>(){public Waypoint decode(RegistryFriendlyByteBuf b){return new Waypoint(b.readBoolean(),b.readUtf(80),b.readUtf(128),b.readBlockPos());}public void encode(RegistryFriendlyByteBuf b,Waypoint p){b.writeBoolean(p.enabled);b.writeUtf(p.title,80);b.writeUtf(p.dimension,128);b.writeBlockPos(p.position);}};
    }
    public static void register(PayloadRegistrar registrar){
        registrar.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)CopperService.request(player,p);}));
        registrar.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->c.enqueueWork(()->snapshotReceiver.accept(p)));
        registrar.playToClient(Image.TYPE,Image.CODEC,(p,c)->c.enqueueWork(()->imageReceiver.accept(p)));
        registrar.playToClient(Waypoint.TYPE,Waypoint.CODEC,(p,c)->c.enqueueWork(()->waypointReceiver.accept(p)));
    }
}
