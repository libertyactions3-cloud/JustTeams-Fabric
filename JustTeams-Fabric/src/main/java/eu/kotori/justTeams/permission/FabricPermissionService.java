package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Resolves JustTeams permissions using the same explicit-value lookup pattern as EssentialCommands.
 *
 * The Optional returned by Fabric Permissions API distinguishes an explicit grant or denial from
 * an unset node. Unset values are then resolved through JustTeams' declared parent groups/defaults.
 */
public final class FabricPermissionService implements PermissionService {

    @Override
    public boolean has(ServerCommandSource source, String permission) {
        boolean operator = source.getPermissions().hasPermission(
                new Permission.Level(PermissionLevel.GAMEMASTERS)
        );
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> Permissions.getPermissionValue(source, node).orElse(null),
                operator
        );
    }
}
