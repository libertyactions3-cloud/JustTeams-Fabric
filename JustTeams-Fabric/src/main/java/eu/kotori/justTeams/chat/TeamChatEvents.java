package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.util.ChatInputManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Routes normal server chat into team chat while team-chat mode is enabled. */
public final class TeamChatEvents {
    private static final int TEAM_BLUE = 0x4C9DDE;

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
            TeamRank rank = member == null ? TeamRank.INITIATE : member.getRank();
            Formatting rankColor = rankColor(rank);
            String rankName = rank.getDisplayName();
            String username = serverPlayer.getName().getString();

            Text formatted = Text.empty()
                    .append(Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(
                            Style.EMPTY.withColor(TEAM_BLUE).withItalic(false)))
                    .append(Text.literal("[").setStyle(
                            Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(false)))
                    .append(Text.literal(rankName).setStyle(
                            Style.EMPTY.withColor(rankColor).withItalic(false)))
                    .append(Text.literal("] ").setStyle(
                            Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(false)))
                    .append(Text.literal(username).setStyle(
                            Style.EMPTY.withColor(rankColor).withItalic(false)))
                    .append(Text.literal(": ").setStyle(
                            Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                    .append(Text.literal(content).setStyle(
                            Style.EMPTY.withColor(TEAM_BLUE).withItalic(false)));

            for (ServerPlayerEntity recipient : serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
                boolean teamMember = team.isMember(recipient.getUuid());
                boolean spy = JustTeamsFabric.permissions().has(recipient, JustTeamsPermissions.CHAT_SPY)
                        && TeamChatManager.isSpyEnabled(recipient.getUuid())
                        && !teamMember;
                if (teamMember || spy) {
                    if (spy) {
                        recipient.sendMessage(Text.literal("[SPY] [" + team.getName() + "] "
                                + username + ": " + content), false);
                    } else {
                        recipient.sendMessage(formatted, false);
                    }
                }
            }
            return false;
        });
    }

    private static Formatting rankColor(TeamRank rank) {
        return switch (rank) {
            case LEADER -> Formatting.DARK_AQUA;      // &3
            case CO_LEADER -> Formatting.AQUA;        // &b
            case OFFICER, UNDEROFFICER -> Formatting.BLUE; // &9
            case ASSOCIATE -> Formatting.DARK_GREEN;  // &2
            case MEMBER -> Formatting.GRAY;           // &7
            case INITIATE -> Formatting.WHITE;        // &f
        };
    }
}
