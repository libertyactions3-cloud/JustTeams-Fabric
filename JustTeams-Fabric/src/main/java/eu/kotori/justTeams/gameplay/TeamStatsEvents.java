package eu.kotori.justTeams.gameplay;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

/** Tracks the player kill/death statistics used by JustTeams leaderboards and info. */
public final class TeamStatsEvents {
    private TeamStatsEvents() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(TeamStatsEvents::onDeath);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                (world, killer, killedEntity, damageSource) -> onKill(killer, killedEntity));
    }

    private static void onDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof ServerPlayerEntity victim)) return;

        Team victimTeam = JustTeamsFabric.teams().getTeam(victim.getUuid());
        if (victimTeam == null) return;

        victimTeam.incrementDeaths();
        saveStats();
    }

    private static void onKill(Entity killer, LivingEntity killedEntity) {
        if (!(killer instanceof ServerPlayerEntity killerPlayer)) return;
        if (!(killedEntity instanceof ServerPlayerEntity victim)) return;

        Team killerTeam = JustTeamsFabric.teams().getTeam(killerPlayer.getUuid());
        if (killerTeam == null) return;

        Team victimTeam = JustTeamsFabric.teams().getTeam(victim.getUuid());
        if (victimTeam != null && victimTeam.getId() == killerTeam.getId()) return;

        killerTeam.incrementKills();
        saveStats();
    }

    private static void saveStats() {
        try {
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.warn("Failed to save team kill/death statistics", exception);
        }
    }
}
