package eu.kotori.justTeams.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendlyFirePolicyTest {
    @Test
    void teamMembersNeedBothPersonalTogglesAndTheTeamMasterSetting() {
        assertFalse(FriendlyFirePolicy.mayDamage(true, true, false, false));
        assertFalse(FriendlyFirePolicy.mayDamage(true, true, true, false));
        assertFalse(FriendlyFirePolicy.mayDamage(true, true, false, true));
        assertFalse(FriendlyFirePolicy.mayDamage(true, false, true, true));
        assertTrue(FriendlyFirePolicy.mayDamage(true, true, true, true));
    }

    @Test
    void playersOnDifferentTeamsAreNotBlockedByTheFriendlyFireRule() {
        assertTrue(FriendlyFirePolicy.mayDamage(false, false, false, false));
    }
}
