package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Resolves JustTeams permissions through Fabric Permissions API, matching the
 * Minecraft 1.21.11 implementation used by Multiverse.
 *
 * The defaults resolver preserves JustTeams' declared parent relationships:
 * justteams.user and justteams.* can grant their child command permissions.
 */
public final class FabricPermissionService implements PermissionService {

    @Override
    public boolean has(ServerCommandSource source, String permission) {
        boolean operator = source.getPermissions().hasPermission(
                new Permission.Level(PermissionLevel.GAMEMASTERS)
        );
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> permissionValue(source, node),
                operator
        );
    }

    private Boolean permissionValue(ServerCommandSource source, String permission) {
        TriState value = Permissions.getPermissionValue(source, permission);
        if (value == TriState.DEFAULT) {
            return null;
        }
        return value == TriState.TRUE;
    }
}
