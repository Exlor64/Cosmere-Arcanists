package dev.cosmerearcanists.power;

import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Bounded private perception: directions and short estimated paths, no entity identities. */
public record ZincReactionPacket(String dimension,int lifetime,List<Threat> threats) implements CustomPacketPayload {
    public record Threat(Vec3 direction,List<Vec3> path){
        public Threat{path=List.copyOf(path);if(!Double.isFinite(direction.lengthSqr())||direction.lengthSqr()<.9||direction.lengthSqr()>1.1||path.size()>17)throw new IllegalArgumentException("Invalid zinc threat");for(var p:path)if(!Double.isFinite(p.lengthSqr())||Math.abs(p.x)>32000000||Math.abs(p.y)>32000000||Math.abs(p.z)>32000000)throw new IllegalArgumentException("Invalid zinc path");}
    }
    public ZincReactionPacket{threats=List.copyOf(threats);if(dimension.length()>128||lifetime<1||lifetime>20||threats.size()>8)throw new IllegalArgumentException("Invalid zinc reactions");}
    public static Consumer<ZincReactionPacket> receiver=p->{};
    public static final Type<ZincReactionPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"zinc_reactions"));
    private static Vec3 vector(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    private static void vector(RegistryFriendlyByteBuf b,Vec3 p){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}
    public static final StreamCodec<RegistryFriendlyByteBuf,ZincReactionPacket> CODEC=new StreamCodec<>(){
        public ZincReactionPacket decode(RegistryFriendlyByteBuf b){String dimension=b.readUtf(128);int life=b.readVarInt(),count=b.readVarInt();if(count<0||count>8)throw new IllegalArgumentException("Too many zinc threats");var threats=new ArrayList<Threat>();for(int i=0;i<count;i++){var direction=vector(b);int n=b.readVarInt();if(n<0||n>17)throw new IllegalArgumentException("Too many zinc path points");var path=new ArrayList<Vec3>();for(int j=0;j<n;j++)path.add(vector(b));threats.add(new Threat(direction,path));}return new ZincReactionPacket(dimension,life,threats);}
        public void encode(RegistryFriendlyByteBuf b,ZincReactionPacket p){b.writeUtf(p.dimension,128);b.writeVarInt(p.lifetime);b.writeVarInt(p.threats.size());for(var t:p.threats){vector(b,t.direction);b.writeVarInt(t.path.size());for(var v:t.path)vector(b,v);}}
    };
    @Override public Type<ZincReactionPacket> type(){return TYPE;}
}
