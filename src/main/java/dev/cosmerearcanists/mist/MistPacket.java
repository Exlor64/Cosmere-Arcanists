package dev.cosmerearcanists.mist;

import dev.cosmerearcanists.CosmereArcanists;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server computes personal clarity from real, funded senses; client only renders it. */
public record MistPacket(String dimension, float density, float distance) implements CustomPacketPayload {
    public MistPacket {
        if (dimension.length() > 128 || !Float.isFinite(density) || density < 0 || density > 1
            || !Float.isFinite(distance) || distance < 8 || distance > 128)
            throw new IllegalArgumentException("Invalid Scadrian mist");
    }
    public static Consumer<MistPacket> receiver = p -> {};
    public static final Type<MistPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "scadrial_mist"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MistPacket> CODEC = new StreamCodec<>() {
        public MistPacket decode(RegistryFriendlyByteBuf b) {
            return new MistPacket(b.readUtf(128), b.readFloat(), b.readFloat());
        }
        public void encode(RegistryFriendlyByteBuf b, MistPacket p) {
            b.writeUtf(p.dimension, 128); b.writeFloat(p.density); b.writeFloat(p.distance);
        }
    };
    @Override public Type<MistPacket> type() { return TYPE; }
}
