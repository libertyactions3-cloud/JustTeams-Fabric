package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks players who have enabled JustTeams team-chat modes. */
public final class TeamChatManager {
    private static final Set<UUID> SPY_ENABLED = ConcurrentHashMap.newKeySet();

    private TeamChatManager() {}

    public static boolean isEnabled(UUID playerUuid) {
        Team team = JustTeamsFabric.teams().getTeam(playerUuid);
        TeamPlayer member = team == null ? null : team.getMember(playerUuid);
        return member != null && member.isTeamChatEnabled();
    }

    public static boolean toggle(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) return false;
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null) return false;
        boolean enabled = !member.isTeamChatEnabled();
        member.setTeamChatEnabled(enabled);
        try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
        catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save team chat state", exception); }
        return enabled;
    }

    public static void disable(UUID playerUuid) {
        Team team = JustTeamsFabric.teams().getTeam(playerUuid);
        if (team == null) return;
        TeamPlayer member = team.getMember(playerUuid);
        if (member == null || !member.isTeamChatEnabled()) return;
        member.setTeamChatEnabled(false);
        try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
        catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save team chat state", exception); }
    }

    public static boolean isSpyEnabled(UUID playerUuid) { return SPY_ENABLED.contains(playerUuid); }

    public static boolean toggleSpy(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (SPY_ENABLED.remove(uuid)) return false;
        SPY_ENABLED.add(uuid);
        return true;
    }

    public static void disableSpy(UUID playerUuid) { SPY_ENABLED.remove(playerUuid); }

    public static Team getActiveTeam(ServerPlayerEntity player) {
        if (!isEnabled(player.getUuid())) return null;
        return JustTeamsFabric.teams().getTeam(player.getUuid());
    }
}
