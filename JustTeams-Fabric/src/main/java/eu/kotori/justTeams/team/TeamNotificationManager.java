package eu.kotori.justTeams.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Centralizes player/team notifications for team lifecycle changes.
 *
 * The Fabric port does not currently have the MessageManager/EffectsUtil
 * infrastructure used by justTeams 2.5.3, so this class intentionally stays
 * small and uses Minecraft's native Text API directly.
 */
public final class TeamNotificationManager {
    private TeamNotificationManager() {}

    public static void notifyLeave(MinecraftServer server, Team team, UUID playerUuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player != null) player.sendMessage(Text.literal("You left " + team.getName() + "."), false);
        broadcastExcept(server, team, Text.literal(playerName(server, playerUuid) + " has left the team."), playerUuid);
    }

    public static void notifyKick(MinecraftServer server, Team team, UUID kickerUuid, UUID targetUuid) {
        ServerPlayerEntity kicker = server.getPlayerManager().getPlayer(kickerUuid);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);

        if (kicker != null) {
            String targetName = target != null ? target.getName().getString() : playerName(server, targetUuid);
            kicker.sendMessage(Text.literal("You have kicked " + targetName + " from the team."), false);
        }

        broadcastExcept(server, team, Text.literal(playerName(server, targetUuid) + " has left the team."), kickerUuid, targetUuid);

        if (target != null) target.sendMessage(Text.literal("You have been kicked from the team " + team.getName() + "."), false);
    }

    public static void notifyDisband(MinecraftServer server, Team team, UUID ownerUuid) {
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) owner.sendMessage(Text.literal("You have successfully disbanded your team."), false);
        broadcastExcept(server, team, Text.literal("The team " + team.getName() + " has been disbanded."), ownerUuid);
    }

    private static void broadcastExcept(MinecraftServer server, Team team, Text message, UUID... excludedUuids) {
        for (TeamPlayer member : team.getMembers()) {
            UUID uuid = member.getPlayerUuid();
            boolean excluded = false;
            for (UUID excludedUuid : excludedUuids) {
                if (uuid.equals(excludedUuid)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) continue;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) player.sendMessage(message, false);
        }
    }

    private static String playerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        return player != null ? player.getName().getString() : uuid.toString();
    }
}
