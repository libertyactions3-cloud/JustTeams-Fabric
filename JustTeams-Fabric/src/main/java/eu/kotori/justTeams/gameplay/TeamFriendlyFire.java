package eu.kotori.justTeams.gameplay;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Applies the JustTeams PvP/friendly-fire setting to team members. */
public final class TeamFriendlyFire {
    private TeamFriendlyFire() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof PlayerEntity victim)) return true;
            if (!(source.getAttacker() instanceof PlayerEntity attacker)) return true;
            if (!(attacker instanceof ServerPlayerEntity attackerPlayer)) return true;
            if (!(victim instanceof ServerPlayerEntity victimPlayer)) return true;

            Team attackerTeam = JustTeamsFabric.teams().getTeam(attackerPlayer.getUuid());
            if (attackerTeam == null || !attackerTeam.isMember(victimPlayer.getUuid())) return true;
            return attackerTeam.isPvpEnabled();
        });
    }
}
