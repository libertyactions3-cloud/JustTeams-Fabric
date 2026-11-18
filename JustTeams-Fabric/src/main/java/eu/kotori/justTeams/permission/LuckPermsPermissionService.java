package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * LuckPerms-backed permission resolver through Fabric's shared permissions API.
 *
 * Using the platform API routes each check through LuckPerms' monitored permission cache, which
 * also records the node in LuckPerms' permission tree for /lp tab completion.
 */
public final class LuckPermsPermissionService implements PermissionService {

    @Override
    public boolean has(ServerPlayerEntity player, String permission) {
        boolean operator = player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> permissionValue(player, node),
                operator
        );
    }

    private Boolean permissionValue(ServerPlayerEntity player, String permission) {
        TriState value = Permissions.getPermissionValue(player, permission);
        if (value == TriState.DEFAULT) {
            return null;
        }
        return value == TriState.TRUE;
    }
}
