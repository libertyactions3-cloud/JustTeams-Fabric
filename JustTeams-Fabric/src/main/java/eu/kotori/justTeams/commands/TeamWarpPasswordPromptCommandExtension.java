package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamWarp;
import eu.kotori.justTeams.util.ChatInputManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds the 2.5.3-style chat prompt when a protected warp is used without a password. */
public final class TeamWarpPasswordPromptCommandExtension {
    private TeamWarpPasswordPromptCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        CommandNode<ServerCommandSource> warp = team.getChild("warp");
        if (warp == null) return;

        warp.addChild(CommandManager.argument("name", StringArgumentType.word())
                .executes(context -> executeWithoutPassword(
                        context.getSource(),
                        StringArgumentType.getString(context, "name")))
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .executes(context -> executeWithPassword(
                                context.getSource(),
                                StringArgumentType.getString(context, "name"),
                                StringArgumentType.getString(context, "password"))))
                .build());
    }

    private static int executeWithoutPassword(ServerCommandSource source, String name) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = requireTeam(player);
            TeamWarp warp = team.getWarp(name);
            if (warp == null) {
                source.sendError(Text.literal("Warp not found."));
                return 0;
            }
            if (!warp.isEnabled()) {
                source.sendError(Text.literal("This warp is disabled."));
                return 0;
            }
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_WARP)) {
                source.sendError(Text.literal("You do not have permission to use team warps."));
                return 0;
            }

            if (warp.getPassword().isEmpty()) {
                return requestTeleport(player, warp);
            }

            ChatInputManager.begin(
                    player,
                    "This warp is password protected. Enter the password in chat, or type cancel.",
                    password -> executePromptedPassword(player, name, password));
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to use the team warp." : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to begin team warp password prompt for {}", name, exception);
            return 0;
        }
    }

    private static int executeWithPassword(ServerCommandSource source, String name, String password) {
        try {
            return usePassword(source.getPlayerOrThrow(), name, password);
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to use the team warp." : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to use team warp {}", name, exception);
            return 0;
        }
    }

    private static void executePromptedPassword(ServerPlayerEntity player, String name, String password) {
        try {
            usePassword(player, name, password);
        } catch (Exception exception) {
            player.sendMessage(Text.literal(exception.getMessage() == null ? "Unable to use the team warp." : exception.getMessage()), false);
            JustTeamsFabric.LOGGER.error("Failed to use prompted team warp {}", name, exception);
        }
    }

    private static int usePassword(ServerPlayerEntity player, String name, String password) {
        Team team = requireTeam(player);
        TeamWarp warp = team.getWarp(name);
        if (warp == null) {
            player.sendMessage(Text.literal("Warp not found."), false);
            return 0;
        }
        if (!warp.isEnabled()) {
            player.sendMessage(Text.literal("This warp is disabled."), false);
            return 0;
        }
        if (JustTeamsFabric.teleports().checkWarpCooldown(player)) return 0;
        if (!warp.getPassword().isEmpty() && !warp.getPassword().equals(password)) {
            player.sendMessage(Text.literal("Incorrect password for warp '" + name + "'."), false);
            return 0;
        }
        return requestTeleport(player, warp);
    }

    private static int requestTeleport(ServerPlayerEntity player, TeamWarp warp) {
        JustTeamsFabric.teleports().requestWarp(
                player,
                new TeamLocation(warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch()),
                warp.getCost());
        return 1;
    }

    private static Team requireTeam(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        return team;
    }
}
