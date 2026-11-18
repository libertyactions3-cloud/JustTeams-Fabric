package eu.kotori.justTeams.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Resolves live player permissions directly from LuckPerms' loaded User cache, matching the
 * Fabric implementation used by Fabric-GriefPrevention.
 *
 * Fabric Permissions API remains responsible for advertising known nodes to LuckPerms'
 * permission tree; it is deliberately not used as the runtime permission source here.
 */
public final class LuckPermsPermissionService implements PermissionService {

    @Override
    public boolean has(ServerPlayerEntity player, String permission) {
        boolean operator = player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
        UUID playerId = player.getUuid();
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> permissionValue(playerId, node),
                operator
        );
    }

    private Boolean permissionValue(UUID playerId, String permission) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(playerId);
            if (user == null) {
                return null;
            }

            Tristate value = user.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission);
            return value == Tristate.UNDEFINED ? null : value.asBoolean();
        } catch (IllegalStateException ignored) {
            // LuckPerms may expose its API class before its singleton is ready during startup.
            return null;
        }
    }
}
