package dev.cosmerearcanists.aondor;
import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
public final class AonDorNetwork {
    public static Consumer<Draft> draftReceiver=p->{};
    public static Consumer<Identified> identifiedReceiver=p->{};
    public static Consumer<Projection> projectionReceiver=p->{};
    public static Consumer<Result> receiver=p->{};
    public static Consumer<Library> libraryReceiver=p->{};
    public static Consumer<Stamp> stampReceiver=p->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,name));}
    private static void context(String dimension,int slot){if(dimension.length()>128||slot<0||slot>1)throw new IllegalArgumentException("Invalid drawing context");}
    private static AonDrawing.Glyph readGlyph(RegistryFriendlyByteBuf b,int[] budget){
        boolean effect=b.readBoolean();double x=b.readFloat(),y=b.readFloat(),size=b.readDouble();int parent=b.readVarInt();double number=b.readDouble();int count=b.readVarInt();
        if(count<0||count>AonDrawing.MAX_STROKES)throw new IllegalArgumentException("Too many strokes");var strokes=new ArrayList<List<AonDrawing.Point>>();
        for(int i=0;i<count;i++){int n=b.readVarInt();if(n<2||n>AonDrawing.MAX_POINTS||n>budget[0])throw new IllegalArgumentException("Drawing exceeds point budget");budget[0]-=n;
            var points=new ArrayList<AonDrawing.Point>();for(int j=0;j<n;j++)points.add(new AonDrawing.Point(b.readFloat(),b.readFloat()));strokes.add(points);}
        return new AonDrawing.Glyph(effect,x,y,size,strokes,parent,number);
    }
    private static void writeGlyph(RegistryFriendlyByteBuf b,AonDrawing.Glyph glyph){
        b.writeBoolean(glyph.effect());b.writeFloat((float)glyph.x());b.writeFloat((float)glyph.y());b.writeDouble(glyph.size());b.writeVarInt(glyph.parent());b.writeDouble(glyph.number());b.writeVarInt(glyph.strokes().size());
        for(var stroke:glyph.strokes()){b.writeVarInt(stroke.size());for(var point:stroke){b.writeFloat((float)point.x());b.writeFloat((float)point.y());}}
    }
    public record Check(UUID nonce,String dimension,int slot,AonDrawing drawing) implements CustomPacketPayload {
        public Check {context(dimension,slot);}
        public static final Type<Check> TYPE=AonDorNetwork.type("aondor_check");
        public static final StreamCodec<RegistryFriendlyByteBuf,Check> CODEC=new StreamCodec<>(){
            public Check decode(RegistryFriendlyByteBuf b){var nonce=b.readUUID();var world=b.readUtf(128);int slot=b.readVarInt(),count=b.readVarInt();
                if(count<1||count>AonDrawing.MAX_GLYPHS)throw new IllegalArgumentException("Too many Aons");var glyphs=new ArrayList<AonDrawing.Glyph>();int[] budget={AonDrawing.MAX_TOTAL_POINTS};
                for(int i=0;i<count;i++)glyphs.add(readGlyph(b,budget));return new Check(nonce,world,slot,new AonDrawing(glyphs));}
            public void encode(RegistryFriendlyByteBuf b,Check p){b.writeUUID(p.nonce);b.writeUtf(p.dimension,128);b.writeVarInt(p.slot);b.writeVarInt(p.drawing.glyphs().size());for(var glyph:p.drawing.glyphs())writeGlyph(b,glyph);}
        };
        @Override public Type<Check> type(){return TYPE;}
    }
    public record Trace(String dimension,int slot,AonDrawing.Glyph glyph,UUID nonce,int index) implements CustomPacketPayload {
        public Trace(String dimension,int slot,AonDrawing.Glyph glyph){this(dimension,slot,glyph,UUID.randomUUID(),0);}
        public Trace {context(dimension,slot);if(index<0||index>=AonDrawing.MAX_GLYPHS)throw new IllegalArgumentException("Invalid Aon index");}
        public static final Type<Trace> TYPE=AonDorNetwork.type("aondor_trace");
        public static final StreamCodec<RegistryFriendlyByteBuf,Trace> CODEC=new StreamCodec<>(){
            public Trace decode(RegistryFriendlyByteBuf b){return new Trace(b.readUtf(128),b.readVarInt(),readGlyph(b,new int[]{AonDrawing.MAX_TOTAL_POINTS}),b.readUUID(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Trace p){b.writeUtf(p.dimension,128);b.writeVarInt(p.slot);writeGlyph(b,p.glyph);b.writeUUID(p.nonce);b.writeVarInt(p.index);}};
        @Override public Type<Trace> type(){return TYPE;}
    }
    /** Empty id requests the library; nonempty id requests a previously learned shape. */
    public record Pick(UUID nonce,String dimension,int slot,String id) implements CustomPacketPayload {
        public Pick {context(dimension,slot);if(id.length()>128)throw new IllegalArgumentException("Invalid Aon id");}
        public static final Type<Pick> TYPE=AonDorNetwork.type("aondor_pick");
        public static final StreamCodec<RegistryFriendlyByteBuf,Pick> CODEC=new StreamCodec<>(){
            public Pick decode(RegistryFriendlyByteBuf b){return new Pick(b.readUUID(),b.readUtf(128),b.readVarInt(),b.readUtf(128));}
            public void encode(RegistryFriendlyByteBuf b,Pick p){b.writeUUID(p.nonce);b.writeUtf(p.dimension,128);b.writeVarInt(p.slot);b.writeUtf(p.id,128);}};
        @Override public Type<Pick> type(){return TYPE;}
    }
    public record Known(String id,String name,boolean effect){public Known{if(id.length()>128||name.length()>128)throw new IllegalArgumentException("Invalid learned Aon");}}
    public record Library(String dimension,List<Known> aons,String message) implements CustomPacketPayload {
        public Library {aons=List.copyOf(aons);if(dimension.length()>128||aons.size()>256||message.length()>256)throw new IllegalArgumentException("Oversized Aon library");}
        public static final Type<Library> TYPE=AonDorNetwork.type("aondor_library");
        public static final StreamCodec<RegistryFriendlyByteBuf,Library> CODEC=new StreamCodec<>(){
            public Library decode(RegistryFriendlyByteBuf b){String world=b.readUtf(128),message=b.readUtf(256);int count=b.readVarInt();if(count<0||count>256)throw new IllegalArgumentException("Too many learned Aons");var list=new ArrayList<Known>();for(int i=0;i<count;i++)list.add(new Known(b.readUtf(128),b.readUtf(128),b.readBoolean()));return new Library(world,list,message);}
            public void encode(RegistryFriendlyByteBuf b,Library p){b.writeUtf(p.dimension,128);b.writeUtf(p.message,256);b.writeVarInt(p.aons.size());for(var aon:p.aons){b.writeUtf(aon.id,128);b.writeUtf(aon.name,128);b.writeBoolean(aon.effect);}}};
        @Override public Type<Library> type(){return TYPE;}
    }
    public record Stamp(UUID nonce,String dimension,String id,String name,AonDrawing.Glyph glyph) implements CustomPacketPayload {
        public Stamp {if(dimension.length()>128||id.length()>128||name.length()>128)throw new IllegalArgumentException("Invalid Aon stamp");}
        public static final Type<Stamp> TYPE=AonDorNetwork.type("aondor_stamp");
        public static final StreamCodec<RegistryFriendlyByteBuf,Stamp> CODEC=new StreamCodec<>(){
            public Stamp decode(RegistryFriendlyByteBuf b){return new Stamp(b.readUUID(),b.readUtf(128),b.readUtf(128),b.readUtf(128),readGlyph(b,new int[]{AonDrawing.MAX_TOTAL_POINTS}));}
            public void encode(RegistryFriendlyByteBuf b,Stamp p){b.writeUUID(p.nonce);b.writeUtf(p.dimension,128);b.writeUtf(p.id,128);b.writeUtf(p.name,128);writeGlyph(b,p.glyph);}};
        @Override public Type<Stamp> type(){return TYPE;}
    }
    public record Result(UUID nonce,String dimension,String message) implements CustomPacketPayload {
        public Result {if(dimension.length()>128||message.length()>2048)throw new IllegalArgumentException("Oversized Aon result");}
        public static final Type<Result> TYPE=AonDorNetwork.type("aondor_result");
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=new StreamCodec<>(){
            public Result decode(RegistryFriendlyByteBuf b){return new Result(b.readUUID(),b.readUtf(128),b.readUtf(2048));}
            public void encode(RegistryFriendlyByteBuf b,Result p){b.writeUUID(p.nonce);b.writeUtf(p.dimension,128);b.writeUtf(p.message,2048);}};
        @Override public Type<Result> type(){return TYPE;}
    }
    private static AonDrawing.Ink readInk(RegistryFriendlyByteBuf b){
        double size=b.readDouble();int count=b.readVarInt(),remaining=AonDrawing.MAX_TOTAL_POINTS;if(count<0||count>128)throw new IllegalArgumentException("Too much projected ink");var lines=new ArrayList<List<AonDrawing.Point>>();
        for(int i=0;i<count;i++){int n=b.readVarInt();if(n<2||n>AonDrawing.MAX_POINTS||n>remaining)throw new IllegalArgumentException("Projection point budget exceeded");remaining-=n;var points=new ArrayList<AonDrawing.Point>();for(int j=0;j<n;j++)points.add(new AonDrawing.Point(b.readFloat(),b.readFloat()));lines.add(points);}return new AonDrawing.Ink(lines,size);
    }
    private static void writeInk(RegistryFriendlyByteBuf b,AonDrawing.Ink ink){b.writeDouble(ink.size());b.writeVarInt(ink.strokes().size());for(var line:ink.strokes()){b.writeVarInt(line.size());for(var point:line){b.writeFloat((float)point.x());b.writeFloat((float)point.y());}}}
    public record Preview(String dimension,int slot,boolean active,AonDrawing.Ink glyph) implements CustomPacketPayload {
        public Preview(String dimension,int slot,boolean active,AonDrawing.Glyph glyph){this(dimension,slot,active,new AonDrawing.Ink(glyph));}
        public Preview {context(dimension,slot);}
        public static final Type<Preview> TYPE=AonDorNetwork.type("aondor_preview");
        public static final StreamCodec<RegistryFriendlyByteBuf,Preview> CODEC=new StreamCodec<>(){
            public Preview decode(RegistryFriendlyByteBuf b){return new Preview(b.readUtf(128),b.readVarInt(),b.readBoolean(),readInk(b));}
            public void encode(RegistryFriendlyByteBuf b,Preview p){b.writeUtf(p.dimension,128);b.writeVarInt(p.slot);b.writeBoolean(p.active);writeInk(b,p.glyph);}};
        @Override public Type<Preview> type(){return TYPE;}
    }
    public record Projection(UUID owner,String dimension,boolean active,AonDrawing.Ink glyph) implements CustomPacketPayload {
        public Projection(UUID owner,String dimension,boolean active,AonDrawing.Glyph glyph){this(owner,dimension,active,new AonDrawing.Ink(glyph));}
        public Projection {if(dimension.length()>128)throw new IllegalArgumentException("Invalid projection dimension");}
        public static final Type<Projection> TYPE=AonDorNetwork.type("aondor_projection");
        public static final StreamCodec<RegistryFriendlyByteBuf,Projection> CODEC=new StreamCodec<>(){
            public Projection decode(RegistryFriendlyByteBuf b){return new Projection(b.readUUID(),b.readUtf(128),b.readBoolean(),readInk(b));}
            public void encode(RegistryFriendlyByteBuf b,Projection p){b.writeUUID(p.owner);b.writeUtf(p.dimension,128);b.writeBoolean(p.active);writeInk(b,p.glyph);}};
        @Override public Type<Projection> type(){return TYPE;}
    }
    public record Cast(Check request) implements CustomPacketPayload {
        public static final Type<Cast> TYPE=AonDorNetwork.type("aondor_cast");
        public static final StreamCodec<RegistryFriendlyByteBuf,Cast> CODEC=new StreamCodec<>(){
            public Cast decode(RegistryFriendlyByteBuf b){return new Cast(Check.CODEC.decode(b));}
            public void encode(RegistryFriendlyByteBuf b,Cast p){Check.CODEC.encode(b,p.request);}};
        @Override public Type<Cast> type(){return TYPE;}
    }
    public record Identified(UUID nonce,String dimension,int index,String id,String name,double number) implements CustomPacketPayload {
        public static final Type<Identified> TYPE=AonDorNetwork.type("aondor_identified");
        public static final StreamCodec<RegistryFriendlyByteBuf,Identified> CODEC=new StreamCodec<>(){
            public Identified decode(RegistryFriendlyByteBuf b){return new Identified(b.readUUID(),b.readUtf(128),b.readVarInt(),b.readUtf(128),b.readUtf(128),b.readDouble());}
            public void encode(RegistryFriendlyByteBuf b,Identified p){b.writeUUID(p.nonce);b.writeUtf(p.dimension,128);b.writeVarInt(p.index);b.writeUtf(p.id,128);b.writeUtf(p.name,128);b.writeDouble(p.number);}};
        @Override public Type<Identified> type(){return TYPE;}
    }
    public record Draft(Check drawing) implements CustomPacketPayload {
        public static final Type<Draft> TYPE=AonDorNetwork.type("aondor_draft");
        public static final StreamCodec<RegistryFriendlyByteBuf,Draft> CODEC=new StreamCodec<>(){public Draft decode(RegistryFriendlyByteBuf b){return new Draft(Check.CODEC.decode(b));}public void encode(RegistryFriendlyByteBuf b,Draft p){Check.CODEC.encode(b,p.drawing);}};
        @Override public Type<Draft> type(){return TYPE;}
    }
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(Draft.TYPE,Draft.CODEC,(p,c)->c.enqueueWork(()->draftReceiver.accept(p)));
        registrar.playToServer(Cast.TYPE,Cast.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)AonDorService.cast(player,p.request());}));
        registrar.playToServer(Preview.TYPE,Preview.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)AonDorService.preview(player,p);}));
        registrar.playToClient(Identified.TYPE,Identified.CODEC,(p,c)->c.enqueueWork(()->identifiedReceiver.accept(p)));
        registrar.playToClient(Projection.TYPE,Projection.CODEC,(p,c)->c.enqueueWork(()->projectionReceiver.accept(p)));
        registrar.playToServer(Check.TYPE,Check.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)AonDorService.check(player,p);}));
        registrar.playToServer(Trace.TYPE,Trace.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)AonDorService.trace(player,p);}));
        registrar.playToServer(Pick.TYPE,Pick.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)AonDorService.pick(player,p);}));
        registrar.playToClient(Result.TYPE,Result.CODEC,(p,c)->c.enqueueWork(()->receiver.accept(p)));
        registrar.playToClient(Library.TYPE,Library.CODEC,(p,c)->c.enqueueWork(()->libraryReceiver.accept(p)));
        registrar.playToClient(Stamp.TYPE,Stamp.CODEC,(p,c)->c.enqueueWork(()->stampReceiver.accept(p)));
    }
    private AonDorNetwork(){}
}
