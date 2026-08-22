package eu.kotori.justTeams.permission;

import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central permission facade for JustTeams.
 *
 * The default implementation mirrors the defaults declared by the Paper
 * JustTeams plugin: ordinary user command permissions are granted by default,
 * while admin, bypass, chat-spy, and performance permissions are operator-only.
 */
public interface PermissionService {
    boolean has(ServerPlayerEntity player, String permission);

    static PermissionService defaults() {
        return (player, permission) -> {
            if (JustTeamsPermissions.ALL.equals(permission) || JustTeamsPermissions.ADMIN.equals(permission)) {
                return player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
            }
            if (JustTeamsPermissions.USER.equals(permission)) {
                return true;
            }
            if (JustTeamsPermissions.CHAT_SPY.equals(permission)
                    || permission.startsWith("justteams.bypass.")
                    || permission.equals(JustTeamsPermissions.COMMAND_ADMIN)
                    || permission.equals(JustTeamsPermissions.COMMAND_RELOAD)
                    || permission.equals(JustTeamsPermissions.ADMIN_PERFORMANCE)) {
                return player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
            }
            if (JustTeamsPermissions.USER_COMMANDS.contains(permission)) {
                return true;
            }
            return player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
        };
    }
}
