package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds the 2.5.3 team-chat spy toggle commands. */
public final class TeamChatSpyCommandExtension {
    private TeamChatSpyCommandExtension() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("chatspy")
                .executes(context -> toggle(context.getSource()))
                .build());
        team.addChild(CommandManager.literal("spy")
                .executes(context -> toggle(context.getSource()))
                .build());
    }

    private static int toggle(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.CHAT_SPY)) {
                source.sendError(Text.literal("You do not have permission to use team chat spy."));
                return 0;
            }

            boolean enabled = TeamChatManager.toggleSpy(player);
            player.sendMessage(Text.literal(enabled
                    ? "Team chat spy enabled - You can now see all team chats."
                    : "Team chat spy disabled."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to toggle team chat spy."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to toggle team chat spy", exception);
            return 0;
        }
    }
}
