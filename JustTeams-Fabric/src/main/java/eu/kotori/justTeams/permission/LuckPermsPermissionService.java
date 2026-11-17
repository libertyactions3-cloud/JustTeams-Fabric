package eu.kotori.justTeams.permission;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * LuckPerms-backed permission resolver.
 *
 * The provider is looked up when a permission is checked, not during mod initialization. This
 * matches GriefPrevention's Fabric integration and avoids depending on LuckPerms having published
 * its singleton before JustTeams initializes.
 */
public final class LuckPermsPermissionService implements PermissionService {

    @Override
    public boolean has(ServerPlayerEntity player, String permission) {
        UUID playerId = player.getUuid();
        boolean operator = player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
        return JustTeamsPermissionDefaults.resolve(
                permission,
                node -> permissionValue(playerId, node),
                operator
        );
    }

    private Boolean permissionValue(UUID playerId, String permission) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(playerId);
            if (user == null) {
                return null;
            }

            Tristate value = user.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission);
            return value == Tristate.UNDEFINED ? null : value.asBoolean();
        } catch (IllegalStateException ignored) {
            // LuckPerms may be present before its API singleton is ready during server startup.
            return null;
        }
    }
}
