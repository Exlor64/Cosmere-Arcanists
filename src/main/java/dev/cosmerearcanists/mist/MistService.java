package dev.cosmerearcanists.mist;

import dev.cosmerearcanists.power.TinService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MistService {
    public static double weather(net.minecraft.server.level.ServerLevel level) {
        return MistConfig.ENABLED.get() && MistConfig.DIMENSIONS.get().contains(level.dimension().location().toString())
            ? MistMath.weather(level.getSeed(), level.getDayTime(), MistConfig.CHANCE.get()) : 0;
    }
    public static MistPacket snapshot(ServerPlayer player) {
        var level = player.serverLevel();
        double weather = weather(level);
        double sight = player.isAlive() && !player.isSpectator() ? TinService.sightStrength(player) : 0;
        return new MistPacket(level.dimension().location().toString(),
            (float)MistMath.perceivedDensity(weather, sight, MistConfig.CLEAR_STRENGTH.get()),
            MistConfig.DISTANCE.get().floatValue());
    }
    /** Called after funded senses are updated, alongside the normal power status. */
    public static void sync(ServerPlayer player) {
        if (player.tickCount % 10 == 0)
            PacketDistributor.sendToPlayer(player, snapshot(player));
    }
    private MistService() {}
}
