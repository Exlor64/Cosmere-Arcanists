package dev.cosmerearcanists.investiture;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
/** Server-only aluminum cleansing. Foreign Investiture systems remove only their own external influences.
 * No vanilla potion, native power, spirit damage or unrelated data is removed automatically. */
public final class InvestitureCleanseEvent extends Event {
    private final ServerPlayer player;
    public InvestitureCleanseEvent(ServerPlayer player){this.player=java.util.Objects.requireNonNull(player);}
    public ServerPlayer player(){return player;}
}
