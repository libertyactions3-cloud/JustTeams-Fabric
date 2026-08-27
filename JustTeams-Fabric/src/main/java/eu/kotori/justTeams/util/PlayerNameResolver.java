package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/** Resolves UUIDs to current or persistent last-known Minecraft usernames. */
public final class PlayerNameResolver {
    private PlayerNameResolver() {}

    public static String resolve(MinecraftServer server, UUID uuid) {
        if (uuid == null) return "Unknown";
        if (server != null) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
            if (online != null) {
                remember(server, online);
                return online.getName().getString();
            }
            String cached = server.getApiServices().nameToIdCache().getByUuid(uuid).map(PlayerConfigEntry::name).orElse(null);
            if (cached != null && !cached.isBlank()) return cached;
        }
        try {
            Team team = JustTeamsFabric.teams().getTeam(uuid);
            TeamPlayer member = team == null ? null : team.getMember(uuid);
            if (member != null && member.getLastKnownName() != null && !member.getLastKnownName().isBlank()) return member.getLastKnownName();
        } catch (IllegalStateException ignored) { }
        return "Unknown";
    }

    public static void remember(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) return;
        String name = player.getName().getString();
        server.getApiServices().nameToIdCache().add(new PlayerConfigEntry(player.getUuid(), name));
        try {
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            TeamPlayer member = team == null ? null : team.getMember(player.getUuid());
            if (member != null) member.setLastKnownName(name);
        } catch (IllegalStateException ignored) { }
    }
}
