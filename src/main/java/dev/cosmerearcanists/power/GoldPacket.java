package dev.cosmerearcanists.power;
import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
/** Only the observer receives their own historical appearance, never a usable item or entity. */
public record GoldPacket(String dimension,int lifetime,Vec3 position,float yaw,float clarity,List<String> equipment,List<String> details) implements CustomPacketPayload {
    public static Consumer<GoldPacket> receiver=p->{};
    public GoldPacket{equipment=List.copyOf(equipment);details=List.copyOf(details);if(lifetime<1||lifetime>40||!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)||!Float.isFinite(yaw)||!Float.isFinite(clarity)||clarity<0||clarity>1||equipment.size()!=6||details.size()>10)throw new IllegalArgumentException("Invalid gold vision");for(String s:equipment)if(s.length()>128)throw new IllegalArgumentException("Invalid equipment id");for(String s:details)if(s.length()>160)throw new IllegalArgumentException("Invalid history label");}
    public static final Type<GoldPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"gold_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GoldPacket> CODEC=new StreamCodec<>(){
        public GoldPacket decode(RegistryFriendlyByteBuf b){String d=b.readUtf(128);int life=b.readVarInt();var pos=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());float yaw=b.readFloat(),alpha=b.readFloat();var gear=new ArrayList<String>();for(int i=0;i<6;i++)gear.add(b.readUtf(128));int n=b.readVarInt();if(n<0||n>10)throw new IllegalArgumentException("Invalid labels");var lines=new ArrayList<String>();for(int i=0;i<n;i++)lines.add(b.readUtf(160));return new GoldPacket(d,life,pos,yaw,alpha,gear,lines);}
        public void encode(RegistryFriendlyByteBuf b,GoldPacket p){b.writeUtf(p.dimension,128);b.writeVarInt(p.lifetime);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeFloat(p.yaw);b.writeFloat(p.clarity);for(String s:p.equipment)b.writeUtf(s,128);b.writeVarInt(p.details.size());for(String s:p.details)b.writeUtf(s,160);}
    };
    @Override public Type<GoldPacket> type(){return TYPE;}
}
