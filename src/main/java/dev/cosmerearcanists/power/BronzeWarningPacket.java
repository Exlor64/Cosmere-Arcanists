package dev.cosmerearcanists.power;

import dev.cosmerearcanists.CosmereArcanists;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Private perceived danger directions, never entity identities or future paths. */
public record BronzeWarningPacket(String dimension, int lifetime, List<Warning> warnings) implements CustomPacketPayload {
    public record Warning(Vec3 direction, float intensity) {
        public Warning {
            if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < .9 || direction.lengthSqr() > 1.1
                || !Float.isFinite(intensity) || intensity <= 0 || intensity > 1) throw new IllegalArgumentException("Invalid bronze warning");
        }
    }
    public BronzeWarningPacket {
        warnings = List.copyOf(warnings);
        if (dimension.length() > 128 || lifetime < 1 || lifetime > 20 || warnings.size() > 8) throw new IllegalArgumentException("Invalid bronze warnings");
    }
    public static Consumer<BronzeWarningPacket> receiver = p -> {};
    public static final Type<BronzeWarningPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "bronze_warnings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BronzeWarningPacket> CODEC = new StreamCodec<>() {
        public BronzeWarningPacket decode(RegistryFriendlyByteBuf b) {
            String dimension = b.readUtf(128); int life = b.readVarInt(), count = b.readVarInt();
            if (count < 0 || count > 8) throw new IllegalArgumentException("Too many bronze warnings");
            var warnings = new ArrayList<Warning>();
            for (int i = 0; i < count; i++) warnings.add(new Warning(new Vec3(b.readFloat(), b.readFloat(), b.readFloat()), b.readFloat()));
            return new BronzeWarningPacket(dimension, life, warnings);
        }
        public void encode(RegistryFriendlyByteBuf b, BronzeWarningPacket p) {
            b.writeUtf(p.dimension, 128); b.writeVarInt(p.lifetime); b.writeVarInt(p.warnings.size());
            for (var w : p.warnings) { b.writeFloat((float)w.direction.x); b.writeFloat((float)w.direction.y); b.writeFloat((float)w.direction.z); b.writeFloat(w.intensity); }
        }
    };
    @Override public Type<BronzeWarningPacket> type() { return TYPE; }
}
