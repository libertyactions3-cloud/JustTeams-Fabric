package eu.kotori.justTeams.team;

import eu.kotori.justTeams.JustTeamsFabric;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Centralized home/warp warmup, cooldown, movement cancellation, and teleport effects. */
public final class TeamTeleportManager {
    private enum Type { HOME, WARP }

    private static final class Warmup {
        private final Type type;
        private final UUID playerUuid;
        private final ServerWorld startWorld;
        private final ServerWorld targetWorld;
        private final TeamLocation location;
        private final double startX;
        private final double startY;
        private final double startZ;
        private int remainingSeconds;
        private int tickCounter;

        private Warmup(Type type, ServerPlayerEntity player, ServerWorld targetWorld, TeamLocation location, int remainingSeconds) {
            this.type = type;
            this.playerUuid = player.getUuid();
            this.startWorld = player.getEntityWorld();
            this.targetWorld = targetWorld;
            this.location = location;
            this.startX = player.getX();
            this.startY = player.getY();
            this.startZ = player.getZ();
            this.remainingSeconds = remainingSeconds;
        }
    }

    private final Map<UUID, Instant> homeCooldowns = new HashMap<>();
    private final Map<UUID, Instant> warpCooldowns = new HashMap<>();
    private final Map<UUID, Warmup> warmups = new HashMap<>();

    public TeamTeleportManager() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public boolean requestHome(ServerPlayerEntity player, TeamLocation location) {
        if (isOnCooldown(player, Type.HOME)) return false;
        return startWarmup(player, location, Type.HOME, JustTeamsFabric.config().getHomeWarmupSeconds());
    }

    public boolean requestWarp(ServerPlayerEntity player, TeamLocation location) {
        if (isOnCooldown(player, Type.WARP)) return false;
        return startWarmup(player, location, Type.WARP, JustTeamsFabric.config().getWarpWarmupSeconds());
    }

    private boolean startWarmup(ServerPlayerEntity player, TeamLocation location, Type type, int warmupSeconds) {
        if (warmups.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("You already have a teleportation in progress."), true);
            return false;
        }

        ServerWorld targetWorld = resolveWorld(player, location);
        if (targetWorld == null) return false;

        Warmup warmup = new Warmup(type, player, targetWorld, location, warmupSeconds);
        if (warmupSeconds <= 0) {
            finishWarmup(player, warmup);
            return true;
        }

        warmups.put(player.getUuid(), warmup);
        return true;
    }

    private void tick(MinecraftServer server) {
        tickCooldowns();

        Iterator<Map.Entry<UUID, Warmup>> iterator = warmups.entrySet().iterator();
        while (iterator.hasNext()) {
            Warmup warmup = iterator.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(warmup.playerUuid);

            if (player == null || !player.isAlive() || player.getServer() != server) {
                iterator.remove();
                continue;
            }

            if (player.getEntityWorld() != warmup.startWorld
                    || player.squaredDistanceTo(warmup.startX, warmup.startY, warmup.startZ) > 1.0D) {
                cancelWarmup(player, iterator);
                continue;
            }

            warmup.tickCounter++;
            if (warmup.tickCounter < 20) continue;
            warmup.tickCounter = 0;

            if (warmup.remainingSeconds > 0) {
                player.sendMessage(Text.literal("Teleporting in " + warmup.remainingSeconds + " seconds... Don't move!"), true);
                spawnWarmupParticles(player);
                warmup.remainingSeconds--;
            } else {
                iterator.remove();
                finishWarmup(player, warmup);
            }
        }
    }

    private void cancelWarmup(ServerPlayerEntity player, Iterator<Map.Entry<UUID, Warmup>> iterator) {
        iterator.remove();
        player.sendMessage(Text.literal("Teleportation canceled because you moved."), true);
        playErrorSound(player);
    }

    private void finishWarmup(ServerPlayerEntity player, Warmup warmup) {
        if (!player.isAlive() || player.getEntityWorld() != warmup.startWorld) return;

        player.teleport(
                warmup.targetWorld,
                warmup.location.getX(),
                warmup.location.getY(),
                warmup.location.getZ(),
                java.util.Set.of(),
                warmup.location.getYaw(),
                warmup.location.getPitch(),
                true);

        player.sendMessage(Text.literal("You have been successfully teleported to your team home."), true);
        if (JustTeamsFabric.config().isSoundsEnabled()) {
            player.playSoundToPlayer(getTeleportSound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        spawnSuccessParticles(player);
        setCooldown(player, warmup.type);
    }

    private boolean isOnCooldown(ServerPlayerEntity player, Type type) {
        boolean bypass = type == Type.HOME
                ? JustTeamsFabric.permissions().has(player, "justteams.bypass.home.cooldown")
                : JustTeamsFabric.permissions().has(player, "justteams.bypass.warp.cooldown");
        if (bypass) return false;

        Instant end = (type == Type.HOME ? homeCooldowns : warpCooldowns).get(player.getUuid());
        if (end == null || !Instant.now().isBefore(end)) return false;

        long seconds = Math.max(0L, Duration.between(Instant.now(), end).toSeconds());
        if (type == Type.HOME) {
            player.sendMessage(Text.literal("You must wait " + seconds + "s before teleporting again."), true);
        } else {
            player.sendMessage(Text.literal("Warp cooldown: " + seconds + "s remaining."), true);
        }
        return true;
    }

    private void setCooldown(ServerPlayerEntity player, Type type) {
        boolean bypass = type == Type.HOME
                ? JustTeamsFabric.permissions().has(player, "justteams.bypass.home.cooldown")
                : JustTeamsFabric.permissions().has(player, "justteams.bypass.warp.cooldown");
        if (bypass) return;

        int seconds = type == Type.HOME
                ? JustTeamsFabric.config().getHomeCooldownSeconds()
                : JustTeamsFabric.config().getWarpCooldownSeconds();
        if (seconds <= 0) return;

        (type == Type.HOME ? homeCooldowns : warpCooldowns)
                .put(player.getUuid(), Instant.now().plusSeconds(seconds));
    }

    private void tickCooldowns() {
        Instant now = Instant.now();
        homeCooldowns.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        warpCooldowns.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    private ServerWorld resolveWorld(ServerPlayerEntity player, TeamLocation location) {
        Identifier identifier = Identifier.tryParse(location.getDimension());
        if (identifier == null) {
            player.sendMessage(Text.literal("The saved teleport location has an invalid dimension."), true);
            return null;
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, identifier);
        ServerWorld world = player.getServer().getWorld(key);
        if (world == null) {
            player.sendMessage(Text.literal("The saved teleport location's dimension is not available."), true);
            return null;
        }
        return world;
    }

    private void spawnWarmupParticles(ServerPlayerEntity player) {
        if (!JustTeamsFabric.config().isParticlesEnabled()) return;
        player.getEntityWorld().spawnParticles(
                getWarmupParticle(),
                player.getX(), player.getY() + 1.0D, player.getZ(),
                JustTeamsFabric.config().getWarmupParticleCount(),
                0.5D, 0.5D, 0.5D, 0.0D);
    }

    private void spawnSuccessParticles(ServerPlayerEntity player) {
        if (!JustTeamsFabric.config().isParticlesEnabled()) return;
        player.getEntityWorld().spawnParticles(
                getSuccessParticle(),
                player.getX(), player.getY(), player.getZ(),
                JustTeamsFabric.config().getSuccessParticleCount(),
                0.5D, 0.5D, 0.5D, 0.0D);
    }

    private void playErrorSound(ServerPlayerEntity player) {
        if (JustTeamsFabric.config().isSoundsEnabled()) {
            player.playSoundToPlayer(getErrorSound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private SoundEvent getTeleportSound() {
        return soundByName(JustTeamsFabric.config().getTeleportSound(), SoundEvents.BLOCK_BEACON_ACTIVATE);
    }

    private SoundEvent getErrorSound() {
        return soundByName(JustTeamsFabric.config().getErrorSound(), SoundEvents.BLOCK_NOTE_BLOCK_BASS);
    }

    private SoundEvent soundByName(String name, SoundEvent fallback) {
        return switch (name.toUpperCase()) {
            case "BLOCK_NOTE_BLOCK_PLING" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING;
            case "BLOCK_NOTE_BLOCK_BASS" -> SoundEvents.BLOCK_NOTE_BLOCK_BASS;
            case "BLOCK_BEACON_ACTIVATE" -> SoundEvents.BLOCK_BEACON_ACTIVATE;
            default -> fallback;
        };
    }

    private ParticleEffect getWarmupParticle() {
        return particleByName(JustTeamsFabric.config().getWarmupParticle(), ParticleTypes.PORTAL);
    }

    private ParticleEffect getSuccessParticle() {
        return particleByName(JustTeamsFabric.config().getSuccessParticle(), ParticleTypes.END_ROD);
    }

    private ParticleEffect particleByName(String name, ParticleEffect fallback) {
        return switch (name.toUpperCase()) {
            case "PORTAL" -> ParticleTypes.PORTAL;
            case "END_ROD" -> ParticleTypes.END_ROD;
            default -> fallback;
        };
    }
}
