package eu.kotori.justTeams.gameplay;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/** Applies the JustTeams team PvP and per-player friendly-fire settings. */
public final class TeamFriendlyFire {
    private TeamFriendlyFire() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity victimPlayer)) return true;
            if (!(source.getAttacker() instanceof ServerPlayerEntity attackerPlayer)) return true;

            Team attackerTeam = JustTeamsFabric.teams().getTeam(attackerPlayer.getUuid());
            Team victimTeam = JustTeamsFabric.teams().getTeam(victimPlayer.getUuid());
            if (attackerTeam == null || attackerTeam != victimTeam) return true;

            TeamPlayer attacker = attackerTeam.getMember(attackerPlayer.getUuid());
            TeamPlayer victim = victimTeam.getMember(victimPlayer.getUuid());
            return FriendlyFirePolicy.mayDamage(
                    true,
                    attackerTeam.isPvpEnabled(),
                    attacker != null && attacker.isFriendlyFireEnabled(),
                    victim != null && victim.isFriendlyFireEnabled());
        });
    }
}
