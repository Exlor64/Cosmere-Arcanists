package dev.cosmerearcanists.connection;

import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Only already-sensed players' names, directions and distances; no world coordinates or relationship ledger. */
public record ConnectionPacket(String dimension,List<Sensed> players) implements CustomPacketPayload {
    public record Sensed(String name,Vec3 direction,double distance){public Sensed{if(name.length()>64||!Double.isFinite(direction.lengthSqr())||direction.lengthSqr()<.9||direction.lengthSqr()>1.1||!Double.isFinite(distance)||distance<0||distance>4096)throw new IllegalArgumentException("Invalid sensed connection");}}
    public ConnectionPacket{players=List.copyOf(players);if(dimension.length()>128||players.size()>8)throw new IllegalArgumentException("Invalid connections");}
    public static Consumer<ConnectionPacket> receiver=p->{};
    public static final Type<ConnectionPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"connections"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConnectionPacket> CODEC=new StreamCodec<>(){
        public ConnectionPacket decode(RegistryFriendlyByteBuf b){String dimension=b.readUtf(128);int count=b.readVarInt();if(count<0||count>8)throw new IllegalArgumentException("Too many connections");var list=new ArrayList<Sensed>();for(int i=0;i<count;i++)list.add(new Sensed(b.readUtf(64),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readDouble()));return new ConnectionPacket(dimension,list);}
        public void encode(RegistryFriendlyByteBuf b,ConnectionPacket p){b.writeUtf(p.dimension,128);b.writeVarInt(p.players.size());for(var sensed:p.players){b.writeUtf(sensed.name,64);b.writeDouble(sensed.direction.x);b.writeDouble(sensed.direction.y);b.writeDouble(sensed.direction.z);b.writeDouble(sensed.distance);}}
    };
    @Override public Type<ConnectionPacket> type(){return TYPE;}
}
