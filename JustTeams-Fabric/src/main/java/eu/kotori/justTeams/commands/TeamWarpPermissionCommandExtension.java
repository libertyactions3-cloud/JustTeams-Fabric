package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Replaces the base warp set/remove nodes with the current independent permissions and rank rules. */
public final class TeamWarpPermissionCommandExtension {
    private TeamWarpPermissionCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        CommandNode<ServerCommandSource> warp = team.getChild("warp");
        if (warp == null) return;

        warp.addChild(CommandManager.literal("set")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> createWarp(context.getSource(), StringArgumentType.getString(context, "name"), ""))
                        .then(CommandManager.argument("password", StringArgumentType.greedyString())
                                .executes(context -> createWarp(context.getSource(), StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "password")))))
                .build());

        warp.addChild(CommandManager.literal("remove")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> removeWarp(context.getSource(), StringArgumentType.getString(context, "name"))))
                .build());
    }

    private static int createWarp(ServerCommandSource source, String name, String password) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = requireTeam(player);
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canSetWarps()) throw new IllegalStateException("You do not have permission to create team warps.");
            if (name.length() > 32) throw new IllegalArgumentException("Warp name must be 32 characters or fewer.");
            if (team.getWarp(name) != null) throw new IllegalArgumentException("A warp with that name already exists.");
            if (password.length() > 64) throw new IllegalArgumentException("Warp passwords may not exceed 64 characters.");
            if (!FeatureCostManager.charge(player, "setwarp")) return 0;

            TeamLocation location = TeamLocation.fromPlayer(player);
            TeamWarp warp = new TeamWarp(name, player.getUuid(), location.getDimension(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            warp.setPassword(password);
            team.addWarp(warp);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team warp '" + name + "' created."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to create the team warp." : exception.getMessage()));
            return 0;
        }
    }

    private static int removeWarp(ServerCommandSource source, String name) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = requireTeam(player);
            TeamWarp warp = team.getWarp(name);
            if (warp == null) throw new IllegalStateException("Warp not found.");
            TeamPlayer member = team.getMember(player.getUuid());
            if (!team.isOwner(player.getUuid()) && (member == null || member.getRank() != TeamRank.CO_LEADER)) {
                throw new IllegalStateException("Only the team owner or a Co-Leader can remove any team warp.");
            }
            team.removeWarp(name);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team warp '" + name + "' removed."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to remove the team warp." : exception.getMessage()));
            return 0;
        }
    }

    private static Team requireTeam(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        return team;
    }
}
