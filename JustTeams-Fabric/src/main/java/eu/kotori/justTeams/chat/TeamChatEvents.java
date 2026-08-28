package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.util.ChatInputManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Routes normal server chat into team chat while team-chat mode is enabled. */
public final class TeamChatEvents {
    private TeamChatEvents() {}

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

            TeamPlayer member = team.getMember(serverPlayer.getUuid());
            String rank = member == null ? TeamRank.INITIATE.getDisplayName() : member.getRank().getDisplayName();
            String teamPrefix = "[ᴛᴇᴀᴍꜱ]";
            Text formatted = Text.empty()
                    .append(Text.literal(teamPrefix + " ").setStyle(net.minecraft.text.Style.EMPTY.withColor(0x4C9DDE).withItalic(false)))
                    .append(Text.literal("[" + rank + "]").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                    .append(Text.literal(" ").setStyle(net.minecraft.text.Style.EMPTY.withItalic(false)))
                    .append(Text.literal("[" + serverPlayer.getName().getString() + "]: ").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                    .append(Text.literal(content).setStyle(net.minecraft.text.Style.EMPTY.withColor(0x4C9DDE).withItalic(false)));

            for (ServerPlayerEntity recipient : serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
                boolean teamMember = team.isMember(recipient.getUuid());
                boolean spy = JustTeamsFabric.permissions().has(recipient, JustTeamsPermissions.CHAT_SPY)
                        && TeamChatManager.isSpyEnabled(recipient.getUuid())
                        && !teamMember;
                if (teamMember || spy) {
                    if (spy) {
                        recipient.sendMessage(Text.literal("[SPY] [" + team.getName() + "] " + serverPlayer.getName().getString() + ": " + content), false);
                    } else {
                        recipient.sendMessage(formatted, false);
                    }
                }
            }
            return false;
        });
    }
}
