package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamGuiManager;
import eu.kotori.justTeams.gui.TeamInPlaceGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TeamSettingsCommandExtension {
    private static final int TEAM_BLUE = 0x4C9DDE;
    private TeamSettingsCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        team.addChild(CommandManager.literal("settings")
                .executes(context -> open(context.getSource()))
                .build());
    }

    private static int open(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.USER)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team == null) {
                source.sendError(noTeam());
                return 0;
            }
            if (!team.hasElevatedPermissions(player.getUuid())) {
                source.sendError(Text.literal("Only the owner or co-owner can access team settings."));
                return 0;
            }
            TeamGuiManager.openPersistentView(player, TeamInPlaceGui.View.SETTINGS);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to open team settings." : exception.getMessage()));
            return 0;
        }
    }

    private static Text noTeam() {
        return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false))
                .append(Text.literal("You are not in a team.").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false)));
    }
}
