package eu.kotori.justTeams.permission;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Fabric-side equivalent of the permission hierarchy declared by the Paper JustTeams descriptor.
 *
 * LuckPerms on Fabric accepts arbitrary permission identifiers but does not import Bukkit's
 * plugin.yml defaults or child relationships. This resolver mirrors those rules: an explicit value
 * on the requested node wins, then an explicit value on a declared parent, then the defaults.
 */
public final class JustTeamsPermissionDefaults {
    private static final Map<String, Node> NODES = new HashMap<>();

    static {
        node(JustTeamsPermissions.ALL, Default.OP);
        node(JustTeamsPermissions.USER, Default.OP, JustTeamsPermissions.ALL);
        node(JustTeamsPermissions.ADMIN, Default.OP, JustTeamsPermissions.ALL);

        for (String child : JustTeamsPermissions.USER_COMMANDS) {
            node(child, Default.TRUE, JustTeamsPermissions.USER);
        }

        for (String child : Arrays.asList(
                JustTeamsPermissions.COMMAND_ADMIN,
                JustTeamsPermissions.COMMAND_RELOAD,
                JustTeamsPermissions.BYPASS_BANK_WITHDRAW,
                JustTeamsPermissions.BYPASS_ENDERCHEST_USE,
                JustTeamsPermissions.BYPASS_HOME_COOLDOWN,
                JustTeamsPermissions.BYPASS_WARP_COOLDOWN
        )) {
            node(child, Default.OP, JustTeamsPermissions.ADMIN);
        }

        // These are operator-only individually in the Paper descriptor; they are not children
        // of justteams.admin and therefore must not be inherited from that group.
        node(JustTeamsPermissions.CHAT_SPY, Default.OP);
        node(JustTeamsPermissions.ADMIN_PERFORMANCE, Default.OP);
    }

    private JustTeamsPermissionDefaults() {
    }

    /**
     * Resolves explicit permission values using the Optional-based Fabric Permissions API lookup
     * used by Essential Commands, then applies JustTeams' declared parent relationships and defaults.
     *
     * @param permission the node being checked
     * @param explicit the LuckPerms value, or null when LuckPerms has no value for that node
     * @param operator whether the player has the server's game-master permission level
     */
    public static boolean resolve(
            String permission,
            Function<String, Boolean> explicit,
            boolean operator
    ) {
        Boolean value = explicitValue(permission, explicit, new HashSet<>());
        return value != null ? value : grantedByDefault(permission, operator, new HashSet<>());
    }

    private static Boolean explicitValue(
            String permission,
            Function<String, Boolean> explicit,
            Set<String> visited
    ) {
        if (!visited.add(permission)) {
            return null;
        }

        Boolean direct = explicit.apply(permission);
        if (direct != null) {
            return direct;
        }

        Node node = NODES.get(permission);
        if (node != null) {
            for (String parent : node.parents) {
                Boolean inherited = explicitValue(parent, explicit, visited);
                if (inherited != null) {
                    return inherited;
                }
            }
        }
        return null;
    }

    private static boolean grantedByDefault(String permission, boolean operator, Set<String> visited) {
        if (!visited.add(permission)) {
            return false;
        }

        Node node = NODES.get(permission);
        Default value = node == null ? Default.OP : node.value;
        if (value == Default.TRUE || (value == Default.OP && operator)) {
            return true;
        }

        if (node != null) {
            for (String parent : node.parents) {
                if (grantedByDefault(parent, operator, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Declared nodes and their defaults, for permission integration tests and diagnostics. */
    public static Set<String> declared() {
        return Collections.unmodifiableSet(NODES.keySet());
    }

    /**
     * Builds the LuckPerms suggestion nodes for all declared permissions, including each nested
     * wildcard prefix, following Essential Commands' permission registration pattern.
     */
    public static Set<String> wildcardNodes(Collection<String> permissions) {
        Set<String> wildcardNodes = new LinkedHashSet<>();
        String prefix = "justteams.";

        for (String permission : permissions) {
            if (permission == null || !permission.startsWith(prefix)) {
                continue;
            }

            String[] segments = permission.substring(prefix.length()).split("\\.");
            for (int segmentCount = 1; segmentCount < segments.length; segmentCount++) {
                wildcardNodes.add(prefix + String.join(".", Arrays.copyOf(segments, segmentCount)) + ".*");
            }
        }

        wildcardNodes.add(JustTeamsPermissions.ALL);
        return Collections.unmodifiableSet(wildcardNodes);
    }

    public static String declaredDefault(String permission) {
        Node node = NODES.get(permission);
        if (node == null) {
            throw new IllegalArgumentException("Unknown JustTeams permission: " + permission);
        }
        return node.value.name().toLowerCase(Locale.ROOT);
    }

    public static List<String> declaredParents(String permission) {
        Node node = NODES.get(permission);
        if (node == null) {
            throw new IllegalArgumentException("Unknown JustTeams permission: " + permission);
        }
        return node.parents;
    }

    private static void node(String permission, Default value, String... parents) {
        NODES.put(permission, new Node(value, Collections.unmodifiableList(Arrays.asList(parents))));
    }

    private enum Default {
        TRUE,
        OP
    }

    private static final class Node {
        private final Default value;
        private final List<String> parents;

        private Node(Default value, List<String> parents) {
            this.value = value;
            this.parents = parents;
        }
    }
}
