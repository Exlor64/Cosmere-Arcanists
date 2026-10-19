package dev.cosmerearcanists.power;

import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Server-approved private camera position; never a client-supplied teleport. */
public record ScoutPacket(String dimension,boolean active,Vec3 position,double radius) implements CustomPacketPayload {
    public static Consumer<ScoutPacket> receiver=p->{};
    public static final Type<ScoutPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"electrum_scout"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ScoutPacket> CODEC=new StreamCodec<>(){
        public ScoutPacket decode(RegistryFriendlyByteBuf b){String dim=b.readUtf(128);boolean active=b.readBoolean();double x=b.readDouble(),y=b.readDouble(),z=b.readDouble(),radius=b.readDouble();if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(radius)||Math.abs(x)>3.1e7||Math.abs(y)>3.1e7||Math.abs(z)>3.1e7||radius<0||radius>64)throw new IllegalArgumentException("Invalid electrum scout");return new ScoutPacket(dim,active,new Vec3(x,y,z),radius);}
        public void encode(RegistryFriendlyByteBuf b,ScoutPacket p){b.writeUtf(p.dimension,128);b.writeBoolean(p.active);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeDouble(p.radius);}
    };
    public Type<ScoutPacket> type(){return TYPE;}
}
