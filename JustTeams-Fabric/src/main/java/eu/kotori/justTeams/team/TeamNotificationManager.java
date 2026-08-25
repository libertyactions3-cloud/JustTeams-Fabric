package eu.kotori.justTeams.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
        if (player != null) {
            player.sendMessage(Text.literal("You left " + team.getName() + "."), false);
            playSuccessSound(player);
        }
        broadcastExcept(server, team, Text.literal(playerName(server, playerUuid) + " has left the team."), playerUuid);
    }

    public static void notifyKick(MinecraftServer server, Team team, UUID kickerUuid, UUID targetUuid) {
        ServerPlayerEntity kicker = server.getPlayerManager().getPlayer(kickerUuid);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);

        if (kicker != null) {
            String targetName = target != null ? target.getName().getString() : playerName(server, targetUuid);
            kicker.sendMessage(Text.literal("You have kicked " + targetName + " from the team."), false);
            playSuccessSound(kicker);
        }

        broadcastExcept(server, team, Text.literal(playerName(server, targetUuid) + " has left the team."), kickerUuid, targetUuid);

        if (target != null) target.sendMessage(Text.literal("You have been kicked from the team " + team.getName() + "."), false);
    }

    public static void notifyDisband(MinecraftServer server, Team team, UUID ownerUuid) {
        // 2.5.3 closes the inventory of every online team member when disbanding.
        // TeamEnderChestGui already closes/releases its tracked viewers; this also
        // closes any other currently-open handled screen for every online member.
        for (TeamPlayer member : team.getMembers()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(member.getPlayerUuid());
            if (player != null) player.closeHandledScreen();
        }

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            owner.sendMessage(Text.literal("You have successfully disbanded your team."), false);
            playSuccessSound(owner);
        }
        broadcastExcept(server, team, Text.literal("The team " + team.getName() + " has been disbanded."), ownerUuid);
    }

    private static void playSuccessSound(ServerPlayerEntity player) {
        if (!eu.kotori.justTeams.JustTeamsFabric.config().isSoundsEnabled()) return;
        player.playSoundToPlayer(resolveSound(eu.kotori.justTeams.JustTeamsFabric.config().getSuccessSound()),
                net.minecraft.sound.SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static SoundEvent resolveSound(String configured) {
        return switch (configured.toUpperCase()) {
            case "BLOCK_NOTE_BLOCK_PLING" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case "BLOCK_NOTE_BLOCK_BASS" -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case "BLOCK_BEACON_ACTIVATE" -> SoundEvents.BLOCK_BEACON_ACTIVATE;
            default -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
        };
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
