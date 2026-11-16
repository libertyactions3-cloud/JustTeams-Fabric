package eu.kotori.justTeams.gameplay;

/** Pure decision rule for damage between members of the same JustTeams team. */
public final class FriendlyFirePolicy {
    private FriendlyFirePolicy() {
    }

    public static boolean mayDamage(
            boolean sameTeam,
            boolean teamPvpEnabled,
            boolean attackerFriendlyFireEnabled,
            boolean victimFriendlyFireEnabled) {
        if (!sameTeam) return true;
        return teamPvpEnabled && attackerFriendlyFireEnabled && victimFriendlyFireEnabled;
    }
}
