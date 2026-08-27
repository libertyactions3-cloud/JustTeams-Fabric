package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamPersistentNoTeamGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Compatibility wrapper for the pluralized registration name used by the initializer. */
public final class TeamInvitesCommandExtensions {
    private TeamInvitesCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("team")
                .then(CommandManager.literal("invites")
                        .executes(context -> execute(context.getSource()))));
    }

    private static int execute(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("A player is required to run this command here."));
            return 0;
        }
        if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.USER)) {
            source.sendError(Text.literal("You do not have permission to use this command."));
            return 0;
        }
        TeamPersistentNoTeamGui.openInvites(player);
        return 1;
    }
}
