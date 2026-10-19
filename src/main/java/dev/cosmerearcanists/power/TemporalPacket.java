package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Private, replaceable forecasts. Client never requests targets or supplies predictions. */
public record TemporalPacket(String dimension,int lifetime,List<Shadow> shadows) implements CustomPacketPayload {
    public static final int SELF=0, OTHER=1, PROJECTILE=2, BRANCH=3, JUMP=4;
    public static final int HAZARD=1, FALL=2, IMPACT=4, ATTACK=8, UNCERTAIN=16, AGGRO=32;
    public static final int DANGER_MASK=HAZARD|FALL|IMPACT|ATTACK|AGGRO;
    public record Point(Vec3 position,int warning) { }
    public record Shadow(int entity,int kind,float width,float height,List<Point> points) { }
    public static Consumer<TemporalPacket> receiver=p->{};
    public static final Type<TemporalPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"temporal_forecast"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TemporalPacket> CODEC=new StreamCodec<>() {
        public TemporalPacket decode(RegistryFriendlyByteBuf b) {
            String dimension=b.readUtf(128);int lifetime=b.readVarInt(),count=b.readVarInt();
            if(lifetime<1||lifetime>40||count<0||count>52)throw new IllegalArgumentException("Invalid forecast");
            var shadows=new ArrayList<Shadow>();
            for(int i=0;i<count;i++) {
                int id=b.readInt(),kind=b.readVarInt();float width=b.readFloat(),height=b.readFloat();int n=b.readVarInt();
                if(kind<0||kind>4||!Float.isFinite(width)||!Float.isFinite(height)||width<=0||height<=0||width>32||height>32||n<1||n>8)throw new IllegalArgumentException("Invalid shadow");
                var points=new ArrayList<Point>();
                for(int j=0;j<n;j++) {
                    double x=b.readDouble(),y=b.readDouble(),z=b.readDouble();int warning=b.readVarInt();
                    if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>3.1e7||Math.abs(y)>3.1e7||Math.abs(z)>3.1e7||warning<0||warning>63)throw new IllegalArgumentException("Invalid future point");
                    points.add(new Point(new Vec3(x,y,z),warning));
                }
                shadows.add(new Shadow(id,kind,width,height,List.copyOf(points)));
            }
            return new TemporalPacket(dimension,lifetime,List.copyOf(shadows));
        }
        public void encode(RegistryFriendlyByteBuf b,TemporalPacket p) {
            b.writeUtf(p.dimension,128);b.writeVarInt(p.lifetime);b.writeVarInt(p.shadows.size());
            for(var s:p.shadows) {b.writeInt(s.entity);b.writeVarInt(s.kind);b.writeFloat(s.width);b.writeFloat(s.height);b.writeVarInt(s.points.size());
                for(var point:s.points){b.writeDouble(point.position.x);b.writeDouble(point.position.y);b.writeDouble(point.position.z);b.writeVarInt(point.warning);}
            }
        }
    };
    @Override public Type<TemporalPacket> type(){return TYPE;}
}
