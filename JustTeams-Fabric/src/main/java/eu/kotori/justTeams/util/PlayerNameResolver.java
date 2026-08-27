package eu.kotori.justTeams.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/** Resolves UUIDs to current or last-known Minecraft usernames. */
public final class PlayerNameResolver {
    private PlayerNameResolver() {}

    public static String resolve(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return "Unknown";

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            remember(server, online);
            return online.getName().getString();
        }

        return server.getApiServices().nameToIdCache()
                .getByUuid(uuid)
                .map(PlayerConfigEntry::name)
                .orElse("Unknown");
    }

    public static void remember(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) return;
        server.getApiServices().nameToIdCache().add(player.getPlayerConfigEntry());
    }
}
