package eu.kotori.justTeams.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * JustTeams permission defaults and parent relationships.
 *
 * LuckPerms on Fabric accepts arbitrary permission nodes, but it does not read Bukkit plugin.yml
 * declarations. This class applies the same child/default semantics explicitly: a node's own value
 * wins, then an explicitly-set parent, then the declared defaults.
 */
public final class JustTeamsPermissionDefaults {
    private static final Map<String, Node> NODES = new HashMap<>();

    static {
        node(JustTeamsPermissions.ALL, Default.OP);
        node(JustTeamsPermissions.USER, Default.TRUE, JustTeamsPermissions.ALL);
        node(JustTeamsPermissions.ADMIN, Default.OP, JustTeamsPermissions.ALL);

        for (String child : JustTeamsPermissions.USER_COMMANDS) {
            node(child, Default.TRUE, JustTeamsPermissions.USER);
        }

        for (String child : Arrays.asList(
                JustTeamsPermissions.COMMAND_ADMIN,
                JustTeamsPermissions.COMMAND_RELOAD,
                JustTeamsPermissions.CHAT_SPY,
                JustTeamsPermissions.ADMIN_PERFORMANCE,
                JustTeamsPermissions.BYPASS_BANK_WITHDRAW,
                JustTeamsPermissions.BYPASS_ENDERCHEST_USE,
                JustTeamsPermissions.BYPASS_HOME_COOLDOWN,
                JustTeamsPermissions.BYPASS_WARP_COOLDOWN
        )) {
            node(child, Default.OP, JustTeamsPermissions.ADMIN);
        }
    }

    private JustTeamsPermissionDefaults() {
    }

    /**
     * Resolves a permission in the same order as GriefPrevention's Fabric permission resolver:
     * explicit node, explicit parent, then node and inherited defaults.
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
        // Treat an explicit global wildcard grant as a root grant. Check it before an individual
        // command or intermediate parent because LuckPerms providers can return a negative result
        // for a child before this resolver has walked the JustTeams parent tree.
        if (Boolean.TRUE.equals(explicit.apply(JustTeamsPermissions.ALL))) {
            return true;
        }

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
