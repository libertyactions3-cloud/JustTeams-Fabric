package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamLeaderboardGui;
import eu.kotori.justTeams.gui.TeamPersistentNoTeamGui;
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
            if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
                TeamLeaderboardGui.openCategories(player);
            } else {
                TeamPersistentNoTeamGui.openLeaderboardCategories(player);
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
            TeamLeaderboardGui.Type type = switch (category.toLowerCase()) {
                case "kills", "kill" -> TeamLeaderboardGui.Type.KILLS;
                case "balance", "balances" -> TeamLeaderboardGui.Type.BALANCE;
                case "members", "member" -> TeamLeaderboardGui.Type.MEMBERS;
                default -> null;
            };
            if (type == null) {
                source.sendError(Text.literal("Usage: /team top [kills|balance|members]"));
                return 0;
            }
            TeamLeaderboardGui.openLeaderboard(player, type);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal("Unable to open the team leaderboard."));
            return 0;
        }
    }
}
