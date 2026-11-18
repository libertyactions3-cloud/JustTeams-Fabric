package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * JustTeams' permission registry and Fabric permission checks.
 *
 * This follows the ECPerms pattern used by Essential Commands: permissions are declared in a
 * registry, published to LuckPerms for suggestions, and checked through the Fabric Permissions
 * API. The requested node's explicit value always wins. Group nodes are used only as the fallback
 * for commands that belong to those groups.
 */
public final class JustTeamsPermissions {
    private JustTeamsPermissions() {
    }

    @SuppressWarnings({"checkstyle:constantname", "checkstyle:staticvariablename"})
    public static final class Registry {
        public static final String all = "justteams.*";
        public static final String chat_spy = "justteams.chatspy";
        public static final String admin_performance = "justteams.admin.performance";

        public static final String bypass_bank_withdraw = "justteams.bypass.bank.withdraw";
        public static final String bypass_enderchest_use = "justteams.bypass.enderchest.use";
        public static final String bypass_home_cooldown = "justteams.bypass.home.cooldown";
        public static final String bypass_warp_cooldown = "justteams.bypass.warp.cooldown";

        public static final String command_gui = "justteams.command.gui";
        public static final String command_admin = "justteams.command.admin";
        public static final String command_reload = "justteams.command.reload";
        public static final String command_create = "justteams.command.create";
        public static final String command_disband = "justteams.command.disband";
        public static final String command_invite = "justteams.command.invite";
        public static final String command_accept = "justteams.command.accept";
        public static final String command_deny = "justteams.command.deny";
        public static final String command_leave = "justteams.command.leave";
        public static final String command_kick = "justteams.command.kick";
        public static final String command_info = "justteams.command.info";
        public static final String command_chat = "justteams.command.chat";
        public static final String command_sethome = "justteams.command.sethome";
        public static final String command_home = "justteams.command.home";
        public static final String command_settag = "justteams.command.settag";
        public static final String command_setdescription = "justteams.command.setdescription";
        public static final String command_transfer = "justteams.command.transfer";
        public static final String command_promote = "justteams.command.promote";
        public static final String command_demote = "justteams.command.demote";
        public static final String command_pvp = "justteams.command.pvp";
        public static final String command_friendlyfire = "justteams.command.friendlyfire";
        public static final String command_bank = "justteams.command.bank";
        public static final String command_enderchest = "justteams.command.enderchest";
        public static final String command_top = "justteams.command.top";
        public static final String command_teammsg = "justteams.command.teammsg";
        public static final String command_join = "justteams.command.join";
        public static final String command_unjoin = "justteams.command.unjoin";
        public static final String command_public = "justteams.command.public";
        public static final String command_requests = "justteams.command.requests";
        public static final String command_setwarp = "justteams.command.setwarp";
        public static final String command_delwarp = "justteams.command.delwarp";
        public static final String command_warp = "justteams.command.warp";
        public static final String command_warps = "justteams.command.warps";
        public static final String command_glow = "justteams.command.glow";
        public static final String command_invites = "justteams.command.invites";
        public static final String command_blacklist = "justteams.command.blacklist";
        public static final String command_settings = "justteams.command.settings";
        public static final String command_autobank = "justteams.command.autobank";

        public static final class Group {
            public static final String[] user_commands = {
                    command_gui, command_create, command_disband, command_invite, command_accept, command_deny,
                    command_leave, command_kick, command_info, command_chat, command_sethome,
                    command_home, command_settag, command_setdescription, command_transfer,
                    command_promote, command_demote, command_pvp, command_friendlyfire,
                    command_bank, command_enderchest, command_top, command_teammsg, command_join,
                    command_unjoin, command_public, command_requests, command_setwarp,
                    command_delwarp, command_warp, command_warps, command_glow, command_invites,
                    command_blacklist, command_settings, command_autobank
            };

            public static final String[] admin_commands = {
                    command_admin, command_reload, bypass_bank_withdraw, bypass_enderchest_use,
                    bypass_home_cooldown, bypass_warp_cooldown
            };
        }
    }

    public static final String ALL = Registry.all;
    public static final String CHAT_SPY = Registry.chat_spy;

    public static final String BYPASS_BANK_WITHDRAW = Registry.bypass_bank_withdraw;
    public static final String BYPASS_ENDERCHEST_USE = Registry.bypass_enderchest_use;
    public static final String BYPASS_HOME_COOLDOWN = Registry.bypass_home_cooldown;
    public static final String BYPASS_WARP_COOLDOWN = Registry.bypass_warp_cooldown;

    public static final String COMMAND_GUI = Registry.command_gui;
    public static final String COMMAND_ADMIN = Registry.command_admin;
    public static final String COMMAND_RELOAD = Registry.command_reload;
    public static final String COMMAND_CREATE = Registry.command_create;
    public static final String COMMAND_DISBAND = Registry.command_disband;
    public static final String COMMAND_INVITE = Registry.command_invite;
    public static final String COMMAND_ACCEPT = Registry.command_accept;
    public static final String COMMAND_DENY = Registry.command_deny;
    public static final String COMMAND_LEAVE = Registry.command_leave;
    public static final String COMMAND_KICK = Registry.command_kick;
    public static final String COMMAND_INFO = Registry.command_info;
    public static final String COMMAND_CHAT = Registry.command_chat;
    public static final String COMMAND_SETHOME = Registry.command_sethome;
    public static final String COMMAND_HOME = Registry.command_home;
    public static final String COMMAND_SETTAG = Registry.command_settag;
    public static final String COMMAND_SETDESCRIPTION = Registry.command_setdescription;
    public static final String COMMAND_TRANSFER = Registry.command_transfer;
    public static final String COMMAND_PROMOTE = Registry.command_promote;
    public static final String COMMAND_DEMOTE = Registry.command_demote;
    public static final String COMMAND_PVP = Registry.command_pvp;
    public static final String COMMAND_FRIENDLYFIRE = Registry.command_friendlyfire;
    public static final String COMMAND_BANK = Registry.command_bank;
    public static final String COMMAND_ENDERCHEST = Registry.command_enderchest;
    public static final String COMMAND_TOP = Registry.command_top;
    public static final String COMMAND_TEAMMSG = Registry.command_teammsg;
    public static final String COMMAND_JOIN = Registry.command_join;
    public static final String COMMAND_UNJOIN = Registry.command_unjoin;
    public static final String COMMAND_PUBLIC = Registry.command_public;
    public static final String COMMAND_REQUESTS = Registry.command_requests;
    public static final String COMMAND_SETWARP = Registry.command_setwarp;
    public static final String COMMAND_DELWARP = Registry.command_delwarp;
    public static final String COMMAND_WARP = Registry.command_warp;
    public static final String COMMAND_WARPS = Registry.command_warps;
    public static final String ADMIN_PERFORMANCE = Registry.admin_performance;
    public static final String COMMAND_GLOW = Registry.command_glow;
    public static final String COMMAND_INVITES = Registry.command_invites;
    public static final String COMMAND_BLACKLIST = Registry.command_blacklist;
    public static final String COMMAND_SETTINGS = Registry.command_settings;
    public static final String COMMAND_AUTOBANK = Registry.command_autobank;

    public static final Set<String> USER_COMMANDS =
            Set.copyOf(Arrays.asList(Registry.Group.user_commands));
    public static final Set<String> ADMIN_COMMANDS =
            Set.copyOf(Arrays.asList(Registry.Group.admin_commands));

    /**
     * Publishes all declared JustTeams nodes and wildcard prefixes to LuckPerms' suggestion tree.
     * Calling the API with the server command source mirrors Essential Commands' registration.
     */
    public static void registerPermissionNodes(MinecraftServer server) {
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            return;
        }

        Set<String> permissionNodes = new LinkedHashSet<>();
        collectPermissionNodes(Registry.class, permissionNodes, false);
        collectPermissionNodes(Registry.Group.class, permissionNodes, true);
        permissionNodes.addAll(wildcardNodes(permissionNodes));

        for (String permission : permissionNodes) {
            Permissions.getPermissionValue(server.getCommandSource(), permission);
        }

        eu.kotori.justTeams.JustTeamsFabric.LOGGER.info(
                "Published {} JustTeams permission nodes to LuckPerms tab completions.",
                permissionNodes.size()
        );
    }

    private static void collectPermissionNodes(
            Class<?> owner,
            Set<String> permissionNodes,
            boolean includeArrays
    ) {
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                if (field.getType() == String.class) {
                    String permission = (String) field.get(null);
                    if (permission != null && permission.startsWith("justteams.")) {
                        permissionNodes.add(permission);
                    }
                } else if (includeArrays && field.getType() == String[].class) {
                    String[] permissions = (String[]) field.get(null);
                    if (permissions != null) {
                        for (String permission : permissions) {
                            if (permission != null && permission.startsWith("justteams.")) {
                                permissionNodes.add(permission);
                            }
                        }
                    }
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(
                        "Unable to read JustTeams permission registry field " + field.getName(),
                        exception
                );
            }
        }
    }

    /** Builds every nested wildcard prefix, plus the global JustTeams wildcard. */
    public static Set<String> wildcardNodes(Collection<String> permissions) {
        Set<String> wildcardNodes = new LinkedHashSet<>();
        String prefix = "justteams.";

        for (String permission : permissions) {
            if (permission == null || !permission.startsWith(prefix)) {
                continue;
            }

            String[] segments = permission.substring(prefix.length()).split("\\.");
            for (int segmentCount = 1; segmentCount < segments.length; segmentCount++) {
                wildcardNodes.add(
                        prefix + String.join(".", Arrays.copyOf(segments, segmentCount)) + ".*"
                );
            }
        }

        wildcardNodes.add(ALL);
        return Set.copyOf(wildcardNodes);
    }

    /**
     * Checks one permission using the same API toggle and operator fallback behavior as
     * Essential Commands' ECPerms implementation.
     */
    public static boolean check(
            @NotNull ServerCommandSource source,
            @NotNull String permission,
            int defaultRequireLevel
    ) {
        // LuckPerms on Fabric provides permission values through Fabric Permissions API v0.
        // Do not let the legacy default=false setting silently bypass LuckPerms checks.
        boolean usePermissionsApi =
                eu.kotori.justTeams.JustTeamsFabric.config().isUsePermissionsApi()
                        || FabricLoader.getInstance().isModLoaded("luckperms");

        if (usePermissionsApi) {
            try {
                return Permissions.getPermissionValue(source, permission).orElse(
                        source.getPermissions().hasPermission(
                                new Permission.Level(PermissionLevel.fromLevel(Math.max(2, defaultRequireLevel)))
                        )
                );
            } catch (Exception exception) {
                eu.kotori.justTeams.JustTeamsFabric.LOGGER.error(
                        "Unable to check JustTeams permission " + permission,
                        exception
                );
                return false;
            }
        }

        return source.getPermissions().hasPermission(
                new Permission.Level(PermissionLevel.fromLevel(defaultRequireLevel))
        );
    }

    /** Uses the same default require level as ECPerms.check(source, permission, 2). */
    public static boolean checkWithOperatorFallback(
            @NotNull ServerCommandSource source,
            @NotNull String permission
    ) {
        return check(source, permission, 2);
    }

    /** Uses the same default require level as ECPerms.check(source, permission). */
    public static boolean check(@NotNull ServerCommandSource source, @NotNull String permission) {
        return check(source, permission, 4);
    }

    public static boolean checkAny(
            @NotNull ServerCommandSource source,
            @NotNull String[] permissions,
            int defaultRequireLevel
    ) {
        for (String permission : permissions) {
            if (check(source, permission, defaultRequireLevel)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkAny(
            @NotNull ServerCommandSource source,
            @NotNull String... permissions
    ) {
        return checkAny(source, permissions, 4);
    }

    public static @NotNull Predicate<ServerCommandSource> require(
            @NotNull String permission,
            int defaultRequireLevel
    ) {
        return source -> check(source, permission, defaultRequireLevel);
    }

    public static @NotNull Predicate<ServerCommandSource> requireAny(
            @NotNull String[] permissions,
            int defaultRequireLevel
    ) {
        return source -> checkAny(source, permissions, defaultRequireLevel);
    }

    /** Returns every concrete node in the registry, including nodes stored in group arrays. */
    public static Set<String> declared() {
        Set<String> permissions = new LinkedHashSet<>();
        collectPermissionNodes(Registry.class, permissions, false);
        collectPermissionNodes(Registry.Group.class, permissions, true);
        return Set.copyOf(permissions);
    }
}
