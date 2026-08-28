package eu.kotori.justTeams.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/** Centralized team lifecycle/request notifications with the 2.5.3 team prefix. */
public final class TeamNotificationManager {
    private static final int TEAM_BLUE = 0x4C9DDE;
    private TeamNotificationManager() {}

    public static void notifyJoinRequest(MinecraftServer server, Team team, UUID requesterUuid) {
        String playerName = playerName(server, requesterUuid);
        Text message = prefix().append(Text.literal(playerName).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal(" wants to join your team. Use ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                .append(Text.literal("/team requests").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal(" to view.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        broadcast(server, team, message);
    }

    public static void notifyJoinRequestSent(MinecraftServer server, ServerPlayerEntity requester, Team team) {
        Text message = prefix().append(Text.literal("Your request to join ").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)))
                .append(Text.literal(team.getName()).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal(" has been sent.").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)));
        requester.sendMessage(message, false);
    }

    public static void notifyJoinRequestAccepted(MinecraftServer server, Team team, UUID playerUuid) {
        String name = playerName(server, playerUuid);
        Text message = prefix().append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal("'s has joined the team.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        broadcast(server, team, message);
        ServerPlayerEntity joined = server.getPlayerManager().getPlayer(playerUuid);
        if (joined != null) playSuccessSound(joined);
    }

    public static void notifyJoinRequestDenied(MinecraftServer server, Team team, UUID playerUuid) {
        String name = playerName(server, playerUuid);
        Text message = prefix().append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal("'s request to join has been denied.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        broadcast(server, team, message);
    }

    public static void notifyLeave(MinecraftServer server, Team team, UUID playerUuid) {
        String name = playerName(server, playerUuid);
        Text message = prefix().append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal("'s has left the team.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        broadcast(server, team, message);
    }

    public static void notifyKick(MinecraftServer server, Team team, UUID kickerUuid, UUID targetUuid) {
        ServerPlayerEntity kicker = server.getPlayerManager().getPlayer(kickerUuid);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
        if (kicker != null) {
            String targetName = target != null ? target.getName().getString() : playerName(server, targetUuid);
            kicker.sendMessage(Text.literal("You have kicked " + targetName + " from the team."), false);
            playSuccessSound(kicker);
        }
        Text message = prefix().append(Text.literal(playerName(server, targetUuid)).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                .append(Text.literal("'s has left the team.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        broadcastExcept(server, team, message, kickerUuid, targetUuid);
        if (target != null) target.sendMessage(Text.literal("You have been kicked from the team " + team.getName() + "."), false);
    }

    public static void notifyDisband(MinecraftServer server, Team team, UUID ownerUuid) {
        for (TeamPlayer member : team.getMembers()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(member.getPlayerUuid());
            if (player != null) player.closeHandledScreen();
        }
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            owner.sendMessage(prefix().append(Text.literal("You have successfully disbanded your team.").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false))), false);
            playSuccessSound(owner);
        }
        broadcastExcept(server, team, prefix().append(Text.literal("The team " + team.getName() + " has been disbanded.").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))), ownerUuid);
    }

    private static Text prefix() { return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false)); }

    private static void broadcast(MinecraftServer server, Team team, Text message) {
        for (TeamPlayer member : team.getMembers()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(member.getPlayerUuid());
            if (player != null) player.sendMessage(message, false);
        }
    }

    private static void broadcastExcept(MinecraftServer server, Team team, Text message, UUID... excludedUuids) {
        for (TeamPlayer member : team.getMembers()) {
            UUID uuid = member.getPlayerUuid(); boolean excluded = false;
            for (UUID excludedUuid : excludedUuids) if (uuid.equals(excludedUuid)) { excluded = true; break; }
            if (excluded) continue;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) player.sendMessage(message, false);
        }
    }

    private static void playSuccessSound(ServerPlayerEntity player) {
        if (!eu.kotori.justTeams.JustTeamsFabric.config().isSoundsEnabled()) return;
        player.playSound(resolveSound(eu.kotori.justTeams.JustTeamsFabric.config().getSuccessSound()), 1.0F, 1.0F);
    }

    private static SoundEvent resolveSound(String configured) {
        return switch (configured.toUpperCase()) {
            case "BLOCK_NOTE_BLOCK_PLING" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case "BLOCK_NOTE_BLOCK_BASS" -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case "BLOCK_BEACON_ACTIVATE" -> SoundEvents.BLOCK_BEACON_ACTIVATE;
            default -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
        };
    }

    private static String playerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) return player.getName().getString();
        if (eu.kotori.justTeams.JustTeamsFabric.teams() != null) {
            for (Team team : eu.kotori.justTeams.JustTeamsFabric.teams().getTeams()) {
                TeamPlayer member = team.getMember(uuid);
                if (member != null && member.getLastKnownName() != null) return member.getLastKnownName();
            }
        }
        return uuid.toString();
    }
}
