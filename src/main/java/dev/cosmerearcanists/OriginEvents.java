package dev.cosmerearcanists;

import java.util.Arrays;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class OriginEvents {
    @SubscribeEvent
    public void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement)
            OriginData.copy(original, replacement);
    }
    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.ORIGINS_ENABLED.get())
            message(player, OriginData.assigned(player) ? OriginData.read(player).summary()
                : "Choose an origin with /cosmere origin scadrial or /cosmere origin overworld.");
    }
    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("cosmere");
        root.then(Commands.literal("status").executes(ctx -> {
            var p = ctx.getSource().getPlayerOrException();
            return message(p, OriginData.assigned(p) ? OriginData.read(p).summary() : "Origin not assigned.");
        }));
        root.then(Commands.literal("origin").then(Commands.argument("planet", StringArgumentType.word())
            .suggests((ctx, b) -> { b.suggest("scadrial"); b.suggest("overworld"); return b.buildFuture(); })
            .executes(ctx -> assign(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "planet"), "none", "none", false))
            .then(Commands.argument("allomancy", StringArgumentType.word()).then(Commands.argument("feruchemy", StringArgumentType.word())
                .executes(ctx -> assign(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "planet"),
                    StringArgumentType.getString(ctx, "allomancy"), StringArgumentType.getString(ctx, "feruchemy"), true))))));
        root.then(Commands.literal("reset").requires(s -> s.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                var p = EntityArgument.getPlayer(ctx, "player");
                OriginData.reset(p);
                p.getPersistentData().remove(dev.cosmerearcanists.power.PowerData.KEY);
                dev.cosmerearcanists.power.PowerNetwork.sync(p, java.util.List.of());
                ctx.getSource().sendSuccess(() -> Component.literal("Reset origin for " + p.getScoreboardName()), true);
                return 1;
            })));
        event.getDispatcher().register(root);
    }
    private int assign(ServerPlayer p, String planet, String a, String f, boolean explicit) {
        if (!ServerConfig.ORIGINS_ENABLED.get()) return message(p, "Origins are disabled.");
        if (!planet.equals("scadrial") && !planet.equals("overworld")) return message(p, "Unknown planet.");
        if (OriginData.assigned(p) && !ServerConfig.ALLOW_REROLLS.get()) return message(p, "Your origin is already assigned.");
        boolean choice = ServerConfig.ASSIGNMENT_MODE.get() == ServerConfig.AssignmentMode.CHOICE;
        if (explicit != choice) return message(p, choice
            ? "Use /cosmere origin <planet> <allomancy metal or none> <feruchemy metal or none>."
            : "Power selection requires CHOICE mode.");
        if (choice) {
            if (!valid(a, ServerConfig.ALLOMANCY) || !valid(f, ServerConfig.FERUCHEMY)) return message(p, "Unknown or disabled power.");
            if (!planet.equals("scadrial") && (!a.equals("none") || !f.equals("none"))) return message(p, "This origin has no natural Metallic Arts.");
            if (!ServerConfig.ALLOW_TWINBORN.get() && !a.equals("none") && !f.equals("none")) return message(p, "Twinborn are disabled.");
        } else if (planet.equals("scadrial")) {
            double multiplier = ServerConfig.ASSIGNMENT_MODE.get() == ServerConfig.AssignmentMode.ANCESTRY
                ? ServerConfig.SCADRIAN_ANCESTRY_MULTIPLIER.get() : 1.0;
            a = roll(p, ServerConfig.ALLOMANCY, multiplier);
            f = roll(p, ServerConfig.FERUCHEMY, multiplier);
            if (!ServerConfig.ALLOW_TWINBORN.get() && !a.equals("none") && !f.equals("none")) {
                if (p.getRandom().nextBoolean()) a = "none"; else f = "none";
            }
        }
        var data = new OriginData(planet, "human", a.equals("none") ? "" : a, f.equals("none") ? "" : f);
        data.save(p);
        message(p, data.summary());
        dev.cosmerearcanists.power.PowerNetwork.sync(p, java.util.List.of());
        return message(p, "Origin set. Iron/steel Allomancy is available; other effects and planetary travel are still in development.");
    }
    private boolean valid(String id, ServerConfig.ArtSettings art) {
        return id.equals("none") || art.enabled().get() && Arrays.stream(Metal.values())
            .anyMatch(m -> m.id().equals(id) && art.metals().get(m).get());
    }
    private String roll(ServerPlayer p, ServerConfig.ArtSettings art, double multiplier) {
        var pool = Arrays.stream(Metal.values()).filter(m -> art.metals().get(m).get()).toList();
        if (!art.enabled().get() || pool.isEmpty() || p.getRandom().nextDouble() >= Math.min(1.0, art.startingChance().get() * multiplier)) return "none";
        return pool.get(p.getRandom().nextInt(pool.size())).id();
    }
    private int message(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); return 1; }
}
