package eu.kotori.justTeams.commands;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.command.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;

/** Toggleable per-player use of the team bank for feature costs. */
public final class TeamAutoBankCommandExtension {
    private static final int TEAM_BLUE = 0x4C9DDE;
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
            if (!member.canUseAutoBank()) throw new IllegalStateException("You do not have permission to use team AutoBank.");
            boolean enabled = !member.isAutoBankEnabled();
            member.setAutoBankEnabled(enabled);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            Text message = prefix().append(Text.literal(enabled
                    ? "Team AutoBank enabled. Team commands that use currency will automatically withdraw from your team's bank."
                    : "Team AutoBank disabled. Team commands now use currency from your own inventory.")
                    .setStyle(Style.EMPTY.withColor(enabled ? Formatting.GREEN : Formatting.RED).withItalic(false)));
            player.sendMessage(message, false);
            return 1;
        } catch (Exception exception) {
            source.sendMessage(prefix().append(Text.literal(exception.getMessage() == null ? "Unable to toggle AutoBank." : exception.getMessage()).setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false))), false);
            return 0;
        }
    }

    private static Text prefix() { return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false)); }
}
