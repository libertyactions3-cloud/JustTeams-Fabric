package eu.kotori.justTeams.permission;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustTeamsPermissionDefaultsTest {

    @Test
    void registryContainsAllUserCommandPermissions() {
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_CREATE));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_HOME));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_WARP));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_AUTOBANK));

        for (String permission : JustTeamsPermissions.USER_COMMANDS) {
            assertEquals(JustTeamsPermissions.USER, JustTeamsPermissions.parentGroup(permission));
        }
    }

    @Test
    void adminGroupContainsOnlyAdministrativeAndBypassPermissions() {
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_ADMIN));
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_RELOAD));
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.BYPASS_WARP_COOLDOWN));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_CREATE));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.CHAT_SPY));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.ADMIN_PERFORMANCE));

        for (String permission : JustTeamsPermissions.ADMIN_COMMANDS) {
            assertEquals(JustTeamsPermissions.ADMIN, JustTeamsPermissions.parentGroup(permission));
        }
    }

    @Test
    void globalAndGroupNodesKeepTheirDeclaredParentBehavior() {
        assertEquals(JustTeamsPermissions.ALL, JustTeamsPermissions.parentGroup(JustTeamsPermissions.USER));
        assertEquals(JustTeamsPermissions.ALL, JustTeamsPermissions.parentGroup(JustTeamsPermissions.ADMIN));
        assertNull(JustTeamsPermissions.parentGroup(JustTeamsPermissions.ALL));
        assertNull(JustTeamsPermissions.parentGroup(JustTeamsPermissions.CHAT_SPY));
    }

    @Test
    void registrationIncludesAllDeclaredStringAndGroupArrayNodes() {
        Set<String> declared = JustTeamsPermissions.declared();

        assertTrue(declared.contains(JustTeamsPermissions.ALL));
        assertTrue(declared.contains(JustTeamsPermissions.USER));
        assertTrue(declared.contains(JustTeamsPermissions.ADMIN));
        assertTrue(declared.contains(JustTeamsPermissions.CHAT_SPY));
        assertTrue(declared.contains(JustTeamsPermissions.ADMIN_PERFORMANCE));
        assertTrue(declared.containsAll(JustTeamsPermissions.USER_COMMANDS));
        assertTrue(declared.containsAll(JustTeamsPermissions.ADMIN_COMMANDS));
    }

    @Test
    void luckPermsSuggestionsIncludeAllNestedWildcardPrefixes() {
        Set<String> nodes = JustTeamsPermissions.wildcardNodes(JustTeamsPermissions.declared());

        assertTrue(nodes.contains("justteams.*"));
        assertTrue(nodes.contains("justteams.command.*"));
        assertTrue(nodes.contains("justteams.bypass.*"));
        assertTrue(nodes.contains("justteams.bypass.bank.*"));
        assertTrue(nodes.contains("justteams.bypass.enderchest.*"));
    }

    @Test
    void wildcardBuilderIgnoresNodesOutsideTheJustTeamsNamespace() {
        Set<String> nodes = JustTeamsPermissions.wildcardNodes(
                Set.of("othermod.command.use", "justteams.command.create")
        );

        assertEquals(Set.of("justteams.*", "justteams.command.*"), nodes);
    }
}
