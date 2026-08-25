package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.ChatInputManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Routes normal server chat into team chat while team-chat mode is enabled. */
public final class TeamChatEvents {
    private TeamChatEvents() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, params) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
            if (ChatInputManager.isWaiting(serverPlayer.getUuid())) return true;
            if (!JustTeamsFabric.permissions().has(serverPlayer, JustTeamsPermissions.COMMAND_CHAT)) {
                TeamChatManager.disable(serverPlayer.getUuid());
                return true;
            }

            Team team = TeamChatManager.getActiveTeam(serverPlayer);
            if (team == null) {
                TeamChatManager.disable(serverPlayer.getUuid());
                return true;
            }

            String content = message.getContent().getString();
            if (content.isBlank()) return false;

            Text formatted = Text.empty()
                    .append(Text.literal("[Team] "))
                    .append(serverPlayer.getName())
                    .append(Text.literal(": " + content));

            for (ServerPlayerEntity recipient : serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
                boolean teamMember = team.isMember(recipient.getUuid());
                boolean spy = JustTeamsFabric.permissions().has(recipient, JustTeamsPermissions.CHAT_SPY)
                        && TeamChatManager.isSpyEnabled(recipient.getUuid())
                        && !teamMember;
                if (teamMember || spy) {
                    if (spy) {
                        recipient.sendMessage(
                                Text.literal("[SPY] [" + team.getName() + "] " + serverPlayer.getName().getString() + ": " + content),
                                false);
                    } else {
                        recipient.sendMessage(formatted, false);
                    }
                }
            }
            return false;
        });
    }
}
