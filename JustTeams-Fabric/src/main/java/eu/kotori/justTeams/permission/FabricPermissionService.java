package eu.kotori.justTeams.permission;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Fabric adapter for the ECPerms-style JustTeams permission checker.
 */
public final class FabricPermissionService implements PermissionService {

    @Override
    public boolean has(ServerCommandSource source, String permission) {
        return JustTeamsPermissions.check(source, permission, 2);
    }
}
