package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Replaces the core invite argument with plain online-player completion and per-member permission. */
public final class TeamInviteCommandExtension {
    private TeamInviteCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        team.addChild(CommandManager.literal("invite")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .build());
    }

    private static int execute(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity inviter = source.getPlayerOrThrow();
            Team team = JustTeamsFabric.teams().getTeam(inviter.getUuid());
            if (team == null) throw new IllegalStateException("You are not in a team.");
            TeamPlayer member = team.getMember(inviter.getUuid());
            if (member == null || !member.canInvite()) throw new IllegalStateException("You do not have permission to invite players.");
            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
            if (target == null) throw new IllegalStateException("Player not found. Use tab completion to select an online player.");
            if (target.getUuid().equals(inviter.getUuid())) throw new IllegalStateException("You cannot invite yourself.");
            if (JustTeamsFabric.teams().isInTeam(target.getUuid())) throw new IllegalStateException("That player is already in a team.");

            team.addInvite(target.getUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal(target.getName().getString() + " has been invited to join " + team.getName() + "."), false);
            target.sendMessage(Text.literal("You have been invited to join " + team.getName() + ". Use /team accept " + team.getName() + "."), false);
            for (TeamPlayer other : team.getMembers()) {
                if (other.getPlayerUuid().equals(inviter.getUuid()) || other.getPlayerUuid().equals(target.getUuid())) continue;
                ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(other.getPlayerUuid());
                if (online != null) online.sendMessage(Text.literal(inviter.getName().getString() + " has invited " + target.getName().getString() + " to join the team."), false);
            }
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to invite that player." : exception.getMessage()));
            return 0;
        }
    }
}
