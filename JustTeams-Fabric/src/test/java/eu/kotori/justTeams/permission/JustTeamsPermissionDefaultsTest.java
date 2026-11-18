package eu.kotori.justTeams.permission;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustTeamsPermissionDefaultsTest {
    private static final Function<String, Boolean> UNDEFINED = permission -> null;

    @Test
    void parentGroupsAreOperatorDefaultButIndividualUserCommandsArePublicByDefault() {
        assertEquals("op", JustTeamsPermissionDefaults.declaredDefault(JustTeamsPermissions.ALL));
        assertEquals("op", JustTeamsPermissionDefaults.declaredDefault(JustTeamsPermissions.USER));
        assertEquals("op", JustTeamsPermissionDefaults.declaredDefault(JustTeamsPermissions.ADMIN));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.USER, UNDEFINED, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.USER, UNDEFINED, true));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, UNDEFINED, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_FRIENDLYFIRE, UNDEFINED, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_WARP, UNDEFINED, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_AUTOBANK, UNDEFINED, false));
    }

    @Test
    void administrativeDefaultsAreOperatorOnly() {
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN, UNDEFINED, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_ADMIN, UNDEFINED, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.CHAT_SPY, UNDEFINED, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN_PERFORMANCE, UNDEFINED, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.BYPASS_HOME_COOLDOWN, UNDEFINED, false));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN, UNDEFINED, true));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_RELOAD, UNDEFINED, true));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.CHAT_SPY, UNDEFINED, true));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN_PERFORMANCE, UNDEFINED, true));
    }

    @Test
    void userParentGrantsItsChildCommands() {
        Function<String, Boolean> user = explicit(Map.of(JustTeamsPermissions.USER, true));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, user, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_WARPS, user, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_AUTOBANK, user, false));
    }

    @Test
    void denyingUserParentDeniesChildCommandsEvenForOperators() {
        Function<String, Boolean> noUser = explicit(Map.of(JustTeamsPermissions.USER, false));

        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, noUser, true));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_HOME, noUser, true));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_SETTINGS, noUser, true));
    }

    @Test
    void explicitChildValuesOverrideParentValues() {
        Function<String, Boolean> childDenied = explicit(Map.of(
                JustTeamsPermissions.USER, true,
                JustTeamsPermissions.COMMAND_CREATE, false
        ));
        Function<String, Boolean> childGranted = explicit(Map.of(
                JustTeamsPermissions.USER, false,
                JustTeamsPermissions.COMMAND_CREATE, true
        ));

        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, childDenied, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, childGranted, false));
    }

    @Test
    void adminParentGrantsOnlyItsDeclaredAdministrativeChildren() {
        Function<String, Boolean> admin = explicit(Map.of(JustTeamsPermissions.ADMIN, true));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_RELOAD, admin, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.BYPASS_WARP_COOLDOWN, admin, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.CHAT_SPY, admin, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN_PERFORMANCE, admin, false));
    }

    @Test
    void globalWildcardGrantFlowsThroughItsDeclaredParents() {
        Function<String, Boolean> wildcard = explicit(Map.of(JustTeamsPermissions.ALL, true));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.USER, wildcard, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN, wildcard, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, wildcard, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_RELOAD, wildcard, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.BYPASS_BANK_WITHDRAW, wildcard, false));

        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.CHAT_SPY, wildcard, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.ADMIN_PERFORMANCE, wildcard, false));
        assertFalse(JustTeamsPermissionDefaults.resolve("justteams.unlisted", wildcard, false));
    }

    @Test
    void explicitChildDenialOverridesWildcardAndGroupGrants() {
        Function<String, Boolean> permissions = explicit(Map.of(
                JustTeamsPermissions.ALL, true,
                JustTeamsPermissions.USER, true,
                JustTeamsPermissions.COMMAND_CREATE, false
        ));

        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.USER, permissions, false));
        assertFalse(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_CREATE, permissions, false));
        assertTrue(JustTeamsPermissionDefaults.resolve(JustTeamsPermissions.COMMAND_INFO, permissions, false));
    }

    @Test
    void everyDeclaredUserCommandHasTheCorrectParentAndDefault() {
        Function<String, Boolean> user = explicit(Map.of(JustTeamsPermissions.USER, true));
        for (String permission : JustTeamsPermissions.USER_COMMANDS) {
            assertEquals("true", JustTeamsPermissionDefaults.declaredDefault(permission), permission);
            assertEquals(java.util.List.of(JustTeamsPermissions.USER),
                    JustTeamsPermissionDefaults.declaredParents(permission), permission);
            assertTrue(JustTeamsPermissionDefaults.resolve(permission, user, false), permission);
        }
    }

    @Test
    void unlistedPermissionsKeepTheOperatorOnlyFallback() {
        assertFalse(JustTeamsPermissionDefaults.resolve("justteams.unlisted", UNDEFINED, false));
        assertTrue(JustTeamsPermissionDefaults.resolve("justteams.unlisted", UNDEFINED, true));
    }

    private static Function<String, Boolean> explicit(Map<String, Boolean> values) {
        return values::get;
    }
}
