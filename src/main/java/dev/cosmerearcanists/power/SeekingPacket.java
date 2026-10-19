package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import dev.cosmerearcanists.CosmereArcanists;

/** Private filtered pulses: no source UUID, player name, reserve count or hidden coppercloud position. */
public record SeekingPacket(String dimension,int lifetime,boolean bronze,boolean focused,double ownCloud,List<Pulse> pulses) implements CustomPacketPayload {
    public record Pulse(Vec3 position,String art,String effect,float clarity,int period,int burnState) { }
    public static Consumer<SeekingPacket> receiver=p->{};
    public static final Type<SeekingPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"seeking_pulses"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SeekingPacket> CODEC=new StreamCodec<>() {
        public SeekingPacket decode(RegistryFriendlyByteBuf b){
            String dim=b.readUtf(128);int life=b.readVarInt();boolean bronze=b.readBoolean(),focus=b.readBoolean();double cloud=b.readDouble();int n=b.readVarInt();
            if(life<1||life>40||!Double.isFinite(cloud)||cloud<0||cloud>64||n<0||n>64)throw new IllegalArgumentException("Invalid seeking status");
            var list=new ArrayList<Pulse>();
            for(int i=0;i<n;i++) {
                var point=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());String art=b.readUtf(128),effect=b.readUtf(128);float clarity=b.readFloat();int period=b.readVarInt(),state=b.readVarInt();
                if(!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z)||Math.abs(point.x)>3.1e7||Math.abs(point.y)>3.1e7||Math.abs(point.z)>3.1e7||!Float.isFinite(clarity)||clarity<0||clarity>1||period<4||period>200||state<0||state>3)throw new IllegalArgumentException("Invalid seeking pulse");
                list.add(new Pulse(point,art,effect,clarity,period,state));
            }
            return new SeekingPacket(dim,life,bronze,focus,cloud,List.copyOf(list));
        }
        public void encode(RegistryFriendlyByteBuf b,SeekingPacket p){b.writeUtf(p.dimension,128);b.writeVarInt(p.lifetime);b.writeBoolean(p.bronze);b.writeBoolean(p.focused);b.writeDouble(p.ownCloud);b.writeVarInt(p.pulses.size());for(var s:p.pulses){b.writeDouble(s.position.x);b.writeDouble(s.position.y);b.writeDouble(s.position.z);b.writeUtf(s.art,128);b.writeUtf(s.effect,128);b.writeFloat(s.clarity);b.writeVarInt(s.period);b.writeVarInt(s.burnState);}}
    };
    @Override public Type<SeekingPacket> type(){return TYPE;}
}
