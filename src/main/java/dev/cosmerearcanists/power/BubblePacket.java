package dev.cosmerearcanists.power;
import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
public record BubblePacket(String dimension,double playerRate,List<Sphere> bubbles) implements CustomPacketPayload {
    public record Sphere(UUID owner,String metal,Vec3 center,double radius,double rate){}
    public static Consumer<BubblePacket> receiver=p->{};
    public BubblePacket{bubbles=List.copyOf(bubbles);if(!Double.isFinite(playerRate)||playerRate<.05||playerRate>8||bubbles.size()>128)throw new IllegalArgumentException("Invalid time bubbles");for(var s:bubbles)if(!Set.of("bendalloy","cadmium").contains(s.metal)||!Double.isFinite(s.radius)||s.radius<.5||s.radius>32||!Double.isFinite(s.rate)||s.rate<.05||s.rate>8||!Double.isFinite(s.center.x)||!Double.isFinite(s.center.y)||!Double.isFinite(s.center.z))throw new IllegalArgumentException("Invalid sphere");}
    public static final Type<BubblePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"time_bubbles"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BubblePacket> CODEC=new StreamCodec<>(){
        public BubblePacket decode(RegistryFriendlyByteBuf b){String d=b.readUtf(128);double rate=b.readDouble();int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Invalid bubble count");var list=new ArrayList<Sphere>();for(int i=0;i<n;i++)list.add(new Sphere(b.readUUID(),b.readUtf(16),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readDouble(),b.readDouble()));return new BubblePacket(d,rate,list);}
        public void encode(RegistryFriendlyByteBuf b,BubblePacket p){b.writeUtf(p.dimension,128);b.writeDouble(p.playerRate);b.writeVarInt(p.bubbles.size());for(var s:p.bubbles){b.writeUUID(s.owner);b.writeUtf(s.metal,16);b.writeDouble(s.center.x);b.writeDouble(s.center.y);b.writeDouble(s.center.z);b.writeDouble(s.radius);b.writeDouble(s.rate);}}
    };
    @Override public Type<BubblePacket> type(){return TYPE;}
}
