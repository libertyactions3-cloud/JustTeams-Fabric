package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamLeaderboardGui;
import eu.kotori.justTeams.gui.TeamPersistentLeaderboardGui;
import eu.kotori.justTeams.gui.TeamGuiManager;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds /team top and its optional leaderboard category argument. */
public final class TeamLeaderboardCommandExtensions {
    private TeamLeaderboardCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("top")
                .executes(context -> openCategories(context.getSource()))
                .then(CommandManager.argument("category", StringArgumentType.word())
                        .executes(context -> openCategory(
                                context.getSource(),
                                StringArgumentType.getString(context, "category"))))
                .build());
    }

    private static int openCategories(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team != null) {
                TeamGuiManager.openPersistentLeaderboard(player,
                        TeamPersistentLeaderboardGui.View.CATEGORIES,
                        TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType.KILLS);
            } else {
                TeamLeaderboardGui.openCategories(player);
            }
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal("Unable to open the team leaderboard."));
            return 0;
        }
    }

    private static int openCategory(ServerCommandSource source, String category) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType type = switch (category.toLowerCase()) {
                case "kills", "kill" -> TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType.KILLS;
                case "balance", "balances" -> TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType.BALANCE;
                case "members", "member" -> TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType.MEMBERS;
                default -> null;
            };
            if (type == null) {
                source.sendError(Text.literal("Usage: /team top [kills|balance|members]"));
                return 0;
            }
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team != null) {
                TeamGuiManager.openPersistentLeaderboard(player,
                        TeamPersistentLeaderboardGui.View.RANKED, type);
            } else {
                TeamLeaderboardGui.openLeaderboard(player, switch (type) {
                    case KILLS -> TeamLeaderboardGui.Type.KILLS;
                    case BALANCE -> TeamLeaderboardGui.Type.BALANCE;
                    case MEMBERS -> TeamLeaderboardGui.Type.MEMBERS;
                });
            }
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal("Unable to open the team leaderboard."));
            return 0;
        }
    }
}
