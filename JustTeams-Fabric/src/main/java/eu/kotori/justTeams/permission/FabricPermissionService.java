package eu.kotori.justTeams.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Resolves JustTeams permissions using the same tri-state permission lookup used by EssentialCommands.
 *
 * Unset nodes remain distinct from explicit denials, allowing JustTeams' declared parent groups
 * and defaults to be applied only when the permission provider has no answer.
 */
public final class FabricPermissionService implements PermissionService {

    @Override
    public boolean has(ServerCommandSource source, String permission) {
        boolean operator = source.getPermissions().hasPermission(
                new Permission.Level(PermissionLevel.GAMEMASTERS)
        );
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> {
                    TriState value = Permissions.getPermissionValue(source, node);
                    return value == TriState.DEFAULT ? null : value == TriState.TRUE;
                },
                operator
        );
    }
}
