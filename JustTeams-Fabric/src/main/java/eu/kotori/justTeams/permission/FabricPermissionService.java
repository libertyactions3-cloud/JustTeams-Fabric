package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Resolves JustTeams permissions through the Fabric Permissions API.
 *
 * The fallback checks distinguish an explicitly configured value from an unset node, allowing
 * JustTeams' declared parent relationships to behave like the Paper permission hierarchy while
 * using the same Permissions.check API as Multiverse 1.21.11.
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

    /**
     * Returns the explicit permission value, or null when the permission is not set.
     *
     * Fabric Permissions API exposes fallback-aware checks rather than a separate public
     * tri-state result. Checking with both possible fallbacks lets us tell an explicit grant
     * or denial apart from an unset permission.
     */
    private Boolean permissionValue(ServerCommandSource source, String permission) {
        boolean allowWhenUnset = Permissions.check(source, permission, true);
        boolean denyWhenUnset = Permissions.check(source, permission, false);

        if (allowWhenUnset != denyWhenUnset) {
            return null;
        }
        return allowWhenUnset;
    }
}
