package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds the optional password argument to the existing /team warp set command. */
public final class TeamWarpCommandExtensions {
    private TeamWarpCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        CommandNode<ServerCommandSource> warp = team.getChild("warp");
        if (warp == null) return;
        CommandNode<ServerCommandSource> set = warp.getChild("set");
        if (set == null) return;
        CommandNode<ServerCommandSource> name = set.getChild("name");
        if (name == null) return;

        name.addChild(CommandManager.argument("password", StringArgumentType.greedyString())
                .executes(context -> setWarpWithPassword(
                        context.getSource(),
                        StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "password")))
                .build());
    }

    private static int setWarpWithPassword(ServerCommandSource source, String name, String password) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team == null) {
                source.sendError(Text.literal("You are not in a team."));
                return 0;
            }

            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_SETWARP)) {
                source.sendError(Text.literal("You do not have permission to create team warps."));
                return 0;
            }

            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canSetHome()) {
                source.sendError(Text.literal("You do not have permission to create team warps."));
                return 0;
            }
            if (name.length() > 32) {
                source.sendError(Text.literal("Warp name must be 32 characters or fewer."));
                return 0;
            }
            if (team.getWarp(name) != null) {
                source.sendError(Text.literal("A warp with that name already exists."));
                return 0;
            }
            if (password.length() > 64) {
                source.sendError(Text.literal("Warp passwords may not exceed 64 characters."));
                return 0;
            }
            if (!FeatureCostManager.charge(player, "setwarp")) return 0;

            TeamLocation location = TeamLocation.fromPlayer(player);
            TeamWarp warp = new TeamWarp(
                    name,
                    player.getUuid(),
                    location.getDimension(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch());
            warp.setPassword(password);
            team.addWarp(warp);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team warp '" + name + "' created."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to create the team warp."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to create team warp {}", name, exception);
            return 0;
        }
    }
}
