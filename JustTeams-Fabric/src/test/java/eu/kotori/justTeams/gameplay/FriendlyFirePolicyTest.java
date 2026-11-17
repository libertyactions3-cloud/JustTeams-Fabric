package eu.kotori.justTeams.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendlyFirePolicyTest {
    @Test
    void teamPvpEnabledAllowsTeammateDamageWithoutPersonalFriendlyFireToggles() {
        assertTrue(FriendlyFirePolicy.mayDamage(true, true, false, false));
        assertTrue(FriendlyFirePolicy.mayDamage(true, true, true, false));
        assertTrue(FriendlyFirePolicy.mayDamage(true, true, false, true));
        assertTrue(FriendlyFirePolicy.mayDamage(true, true, true, true));
    }

    @Test
    void personalFriendlyFireOverridesDisabledTeamPvpOnlyWhenBothPlayersEnableIt() {
        assertFalse(FriendlyFirePolicy.mayDamage(true, false, false, false));
        assertFalse(FriendlyFirePolicy.mayDamage(true, false, true, false));
        assertFalse(FriendlyFirePolicy.mayDamage(true, false, false, true));
        assertTrue(FriendlyFirePolicy.mayDamage(true, false, true, true));
    }

    @Test
    void playersOnDifferentTeamsAreNotBlockedByTheFriendlyFireRule() {
        assertTrue(FriendlyFirePolicy.mayDamage(false, false, false, false));
    }
}
