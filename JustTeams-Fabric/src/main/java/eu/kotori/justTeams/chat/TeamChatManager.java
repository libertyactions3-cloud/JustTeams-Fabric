package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks players who have enabled JustTeams team-chat modes. */
public final class TeamChatManager {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SPY_ENABLED = ConcurrentHashMap.newKeySet();

    private TeamChatManager() {
    }

    public static boolean isEnabled(UUID playerUuid) {
        return ENABLED.contains(playerUuid);
    }

    public static boolean toggle(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (ENABLED.remove(uuid)) return false;
        ENABLED.add(uuid);
        return true;
    }

    public static void disable(UUID playerUuid) {
        ENABLED.remove(playerUuid);
    }

    public static boolean isSpyEnabled(UUID playerUuid) {
        return SPY_ENABLED.contains(playerUuid);
    }

    public static boolean toggleSpy(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (SPY_ENABLED.remove(uuid)) return false;
        SPY_ENABLED.add(uuid);
        return true;
    }

    public static void disableSpy(UUID playerUuid) {
        SPY_ENABLED.remove(playerUuid);
    }

    public static Team getActiveTeam(ServerPlayerEntity player) {
        if (!isEnabled(player.getUuid())) return null;
        return JustTeamsFabric.teams().getTeam(player.getUuid());
    }
}
