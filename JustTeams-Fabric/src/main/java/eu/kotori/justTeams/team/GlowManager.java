package eu.kotori.justTeams.team;

import eu.kotori.justTeams.JustTeamsFabric;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reproduces the Paper implementation's viewer-specific team glow without
 * changing the real server scoreboard or the entity's server-side glowing state.
 */
public final class GlowManager {
    private static final int DEFAULT_RANGE = 64;
    private static final int DEFAULT_CHECK_INTERVAL = 20;
    private static final String TEAM_PREFIX = "JT_GLOW_";

    private final Map<UUID, Map<UUID, Formatting>> glowingCache = new HashMap<>();
    private final Scoreboard packetScoreboard = new Scoreboard();
    private final EnumMap<Formatting, Team> packetTeams = new EnumMap<>(Formatting.class);
    private long tickCounter;

    public GlowManager() {
        for (Formatting color : Formatting.values()) {
            if (!color.isColor()) continue;
            Team team = packetScoreboard.addTeam(TEAM_PREFIX + color.getName().toUpperCase());
            team.setColor(color);
            packetTeams.put(color, team);
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> refreshAll(server)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                glowingCache.remove(handler.player.getUuid()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                newPlayer.getServer().execute(() -> refreshAll(newPlayer.getServer())));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                player.getServer().execute(() -> refreshAll(player.getServer())));
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        int interval = DEFAULT_CHECK_INTERVAL;
        tickCounter++;
        if (tickCounter % interval == 0) refreshAll(server);
    }

    public void updateGlowForTeam(MinecraftServer server, Team ignored) {
        refreshAll(server);
    }

    public void refreshPlayer(MinecraftServer server, ServerPlayerEntity player) {
        refreshAll(server);
    }

    public void stopGlowForPlayer(MinecraftServer server, UUID targetUuid) {
        for (ServerPlayerEntity receiver : server.getPlayerManager().getPlayerList()) {
            unsetGlow(targetUuid, receiver);
        }
    }

    public void refreshAll(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity receiver : players) {
            Map<UUID, Formatting> receiverCache = glowingCache.computeIfAbsent(receiver.getUuid(), ignored -> new HashMap<>());
            for (ServerPlayerEntity target : players) refreshPair(target, receiver, receiverCache);
        }
    }

    private void refreshPair(ServerPlayerEntity target, ServerPlayerEntity receiver, Map<UUID, Formatting> receiverCache) {
        if (target.getUuid().equals(receiver.getUuid())) {
            unsetGlow(target.getUuid(), receiver);
            return;
        }

        Team team = JustTeamsFabric.teams().getTeam(target.getUuid());
        boolean visible = team != null
                && team.isGlowEnabled()
                && team.isMember(receiver.getUuid())
                && target.getEntityWorld() == receiver.getEntityWorld()
                && target.squaredDistanceTo(receiver) <= (double) DEFAULT_RANGE * DEFAULT_RANGE;

        if (!visible) {
            unsetGlow(target.getUuid(), receiver);
            return;
        }

        TeamPlayer member = team.getMember(target.getUuid());
        Formatting color = team.getGlowColor();
        if (color == null && member != null) color = JustTeamsFabric.config().getGlowColor(member.getRole());
        if (color == null) color = Formatting.WHITE;
        setGlow(target, receiver, color, receiverCache);
    }

    private void setGlow(ServerPlayerEntity target, ServerPlayerEntity receiver, Formatting color, Map<UUID, Formatting> receiverCache) {
        Formatting previous = receiverCache.get(target.getUuid());
        if (color.equals(previous)) return;

        if (previous != null) sendTeamRemove(previous, target, receiver);
        sendTeamAdd(color, target, receiver);
        sendMetadataPacket(target, receiver, true);
        receiverCache.put(target.getUuid(), color);
    }

    private void unsetGlow(UUID targetUuid, ServerPlayerEntity receiver) {
        Map<UUID, Formatting> receiverCache = glowingCache.get(receiver.getUuid());
        if (receiverCache == null) return;
        Formatting previous = receiverCache.remove(targetUuid);
        if (previous == null) return;

        ServerPlayerEntity target = receiver.getServer().getPlayerManager().getPlayer(targetUuid);
        if (target != null) {
            sendMetadataPacket(target, receiver, false);
            sendTeamRemove(previous, target, receiver);
        }
    }

    private void sendTeamAdd(Formatting color, ServerPlayerEntity target, ServerPlayerEntity receiver) {
        Team packetTeam = packetTeams.getOrDefault(color, packetTeams.get(Formatting.WHITE));
        receiver.networkHandler.sendPacket(TeamS2CPacket.updateTeam(packetTeam, false));
        receiver.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(packetTeam, target.getNameForScoreboard(), TeamS2CPacket.Operation.ADD));
    }

    private void sendTeamRemove(Formatting color, ServerPlayerEntity target, ServerPlayerEntity receiver) {
        Team packetTeam = packetTeams.getOrDefault(color, packetTeams.get(Formatting.WHITE));
        receiver.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(packetTeam, target.getNameForScoreboard(), TeamS2CPacket.Operation.REMOVE));
    }

    private void sendMetadataPacket(ServerPlayerEntity target, ServerPlayerEntity receiver, boolean glowing) {
        byte flags = 0;
        if (target.isOnFire()) flags |= 0x01;
        if (target.isSneaking()) flags |= 0x02;
        if (target.isSprinting()) flags |= 0x08;
        if (target.isSwimming()) flags |= 0x10;
        if (target.isInvisible()) flags |= 0x20;
        if (glowing || target.isGlowing()) flags |= 0x40;
        if (target.getPose() == EntityPose.FALL_FLYING) flags |= (byte) 0x80;

        EntityTrackerUpdateS2CPacket packet = new EntityTrackerUpdateS2CPacket(
                target.getId(),
                List.of(net.minecraft.entity.data.DataTracker.SerializedEntry.of(EntityAccessor.FLAGS, flags))
        );
        receiver.networkHandler.sendPacket(packet);
    }

    /** Accessor kept isolated so the packet implementation has one version-sensitive location. */
    private static final class EntityAccessor extends Entity {
        private static final net.minecraft.entity.data.TrackedData<Byte> FLAGS = Entity.FLAGS;

        private EntityAccessor() { super(null, null); }
    }
}
