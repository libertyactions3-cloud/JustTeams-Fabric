package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Toggleable per-player use of the team bank for feature costs. */
public final class TeamAutoBankCommandExtension {
    private TeamAutoBankCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("team")
                .then(CommandManager.literal("autobank")
                        .executes(context -> execute(context.getSource()))));
    }

    private static int execute(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.USER)) throw new IllegalStateException("You do not have permission to use this command.");
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team == null) throw new IllegalStateException("You are not in a team.");
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null) throw new IllegalStateException("You are not a team member.");
            boolean enabled = !member.canUseAutoBank();
            member.setCanUseAutoBank(enabled);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team AutoBank is now " + (enabled ? "enabled" : "disabled") + "."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to toggle AutoBank." : exception.getMessage()));
            return 0;
        }
    }
}
