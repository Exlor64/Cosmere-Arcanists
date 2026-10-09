package dev.cosmerearcanists.power;

import java.util.*;
import java.util.function.Consumer;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PowerNetwork {
    /** Assigned by physical-client initialization; no common class references a client class. */
    public static Consumer<Snapshot> clientReceiver = packet -> { };
    public record Entry(String power, int state, double reserve) { }
    public record Snapshot(String dimension, String selected, int levels, List<Entry> powers, List<MetalTarget> targets) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "power_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b) {
                String dimension = b.readUtf(128), selected = b.readUtf(32); int levels = b.readVarInt();
                int n = b.readVarInt(); if (n < 0 || n > 32) throw new IllegalArgumentException("Invalid powers");
                var powers = new ArrayList<Entry>(); for (int i = 0; i < n; i++) powers.add(new Entry(b.readUtf(32), b.readInt(), b.readDouble()));
                n = b.readVarInt(); if (n < 0 || n > 256) throw new IllegalArgumentException("Invalid targets");
                var targets = new ArrayList<MetalTarget>(); for (int i = 0; i < n; i++) targets.add(new MetalTarget(b.readInt(), b.readInt(), b.readBlockPos()));
                return new Snapshot(dimension, selected, levels, List.copyOf(powers), List.copyOf(targets));
            }
            public void encode(RegistryFriendlyByteBuf b, Snapshot p) {
                b.writeUtf(p.dimension, 128); b.writeUtf(p.selected, 32); b.writeVarInt(p.levels); b.writeVarInt(p.powers.size());
                for (var e : p.powers) { b.writeUtf(e.power, 32); b.writeInt(e.state); b.writeDouble(e.reserve); }
                b.writeVarInt(p.targets.size()); for (var t : p.targets) { b.writeInt(t.entityId()); b.writeInt(t.slot()); b.writeBlockPos(t.block()); }
            }
        };
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    /** action: 0 select, 1 set state, 2 apply force hold, 3 cycle state, 4 request snapshot. */
    public record Control(int action, String power, int value) implements CustomPacketPayload {
        public static final Type<Control> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "power_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = new StreamCodec<>() {
            public Control decode(RegistryFriendlyByteBuf b) { return new Control(b.readVarInt(), b.readUtf(32), b.readInt()); }
            public void encode(RegistryFriendlyByteBuf b, Control p) { b.writeVarInt(p.action); b.writeUtf(p.power, 32); b.writeInt(p.value); }
        };
        @Override public Type<Control> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(Snapshot.TYPE, Snapshot.CODEC, (packet, context) -> context.enqueueWork(() -> clientReceiver.accept(packet)));
        r.playToServer(Control.TYPE, Control.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PowerService.control(player, packet);
        }));
    }
    public static void sync(ServerPlayer p, List<MetalTarget> targets) {
        var list = PowerData.available(p).stream().map(power -> new Entry(power, PowerData.state(p, power), power.startsWith("a:") ? PowerData.reserve(p, power.substring(2)) : 0)).toList();
        PacketDistributor.sendToPlayer(p, new Snapshot(p.level().dimension().location().toString(), PowerData.selected(p), PowerConfig.FERUCHEMY_LEVELS.get(), list, targets));
    }
    private PowerNetwork() { }
}
