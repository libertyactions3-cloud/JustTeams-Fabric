package eu.kotori.justTeams.permission;

import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central permission facade for JustTeams.
 *
 * Permission values are queried against the active command source so Fabric permission providers
 * such as LuckPerms can apply the source's player context. Built-in defaults mirror the Paper
 * permission hierarchy declared by JustTeams.
 */
public interface PermissionService {
    boolean has(ServerCommandSource source, String permission);

    default boolean has(ServerPlayerEntity player, String permission) {
        return has(player.getCommandSource(), permission);
    }

    default boolean hasAny(ServerCommandSource source, String... permissions) {
        for (String permission : permissions) {
            if (has(source, permission)) {
                return true;
            }
        }
        return false;
    }

    static PermissionService defaults() {
        return (source, permission) -> JustTeamsPermissionDefaults.resolve(
                permission,
                ignored -> null,
                source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS))
        );
    }
}
