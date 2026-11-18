package eu.kotori.justTeams.permission;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustTeamsPermissionDefaultsTest {

    @Test
    void registryContainsAllUserCommandPermissions() {
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_GUI));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_CREATE));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_HOME));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_WARP));
        assertTrue(JustTeamsPermissions.USER_COMMANDS.contains(JustTeamsPermissions.COMMAND_AUTOBANK));
    }

    @Test
    void adminGroupContainsOnlyAdministrativeAndBypassPermissions() {
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_ADMIN));
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_RELOAD));
        assertTrue(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.BYPASS_WARP_COOLDOWN));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.COMMAND_CREATE));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.CHAT_SPY));
        assertFalse(JustTeamsPermissions.ADMIN_COMMANDS.contains(JustTeamsPermissions.ADMIN_PERFORMANCE));
    }

    @Test
    void registrationIncludesEveryDeclaredPermissionAndGroupArrayEntry() {
        Set<String> declared = JustTeamsPermissions.declared();

        assertTrue(declared.contains(JustTeamsPermissions.ALL));
        assertTrue(declared.contains(JustTeamsPermissions.COMMAND_GUI));
        assertTrue(declared.contains(JustTeamsPermissions.CHAT_SPY));
        assertTrue(declared.contains(JustTeamsPermissions.ADMIN_PERFORMANCE));
        assertTrue(declared.containsAll(JustTeamsPermissions.USER_COMMANDS));
        assertTrue(declared.containsAll(JustTeamsPermissions.ADMIN_COMMANDS));
        assertFalse(declared.contains("justteams.user"));
        assertFalse(declared.contains("justteams.admin"));
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
