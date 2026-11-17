package eu.kotori.justTeams.permission;

import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central permission facade for JustTeams.
 *
 * The built-in defaults mirror the Paper permission defaults: ordinary user commands are available
 * by default, while administrative, bypass, chat-spy, and performance permissions are operator-only.
 */
public interface PermissionService {
    boolean has(ServerPlayerEntity player, String permission);

    static PermissionService defaults() {
        return (player, permission) -> JustTeamsPermissionDefaults.resolve(
                permission,
                ignored -> null,
                player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS))
        );
    }
}
