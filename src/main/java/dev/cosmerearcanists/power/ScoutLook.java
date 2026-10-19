package dev.cosmerearcanists.power;

import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Detached cameras stop vanilla player movement packets; send only bounded view angles. */
public record ScoutLook(float yaw,float pitch) implements CustomPacketPayload {
    public static final Type<ScoutLook> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"electrum_scout_look"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ScoutLook> CODEC=new StreamCodec<>(){
        public ScoutLook decode(RegistryFriendlyByteBuf b){float y=b.readFloat(),p=b.readFloat();if(!Float.isFinite(y)||!Float.isFinite(p)||Math.abs(p)>90)throw new IllegalArgumentException("Invalid scout angles");return new ScoutLook(net.minecraft.util.Mth.wrapDegrees(y),p);}
        public void encode(RegistryFriendlyByteBuf b,ScoutLook p){b.writeFloat(p.yaw);b.writeFloat(p.pitch);}
    };
    public Type<ScoutLook> type(){return TYPE;}
}
