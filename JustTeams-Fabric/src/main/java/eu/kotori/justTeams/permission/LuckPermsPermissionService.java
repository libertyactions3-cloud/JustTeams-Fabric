package eu.kotori.justTeams.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;

/** LuckPerms-backed implementation of the canonical JustTeams permission model. */
public final class LuckPermsPermissionService implements PermissionService {
    private final LuckPerms luckPerms;

    public LuckPermsPermissionService() {
        this.luckPerms = LuckPermsProvider.get();
    }

    @Override
    public boolean has(ServerPlayerEntity player, String permission) {
        User user = luckPerms.getUserManager().getUser(player.getUuid());
        if (user == null) {
            return false;
        }

        if (check(user, permission)) {
            return true;
        }

        // Mirror plugin.yml child permissions: user -> standard commands.
        if (JustTeamsPermissions.USER_COMMANDS.contains(permission)) {
            return check(user, JustTeamsPermissions.USER)
                    || check(user, JustTeamsPermissions.ALL);
        }

        // Mirror plugin.yml child permissions: admin -> admin commands/bypasses.
        if (permission.equals(JustTeamsPermissions.COMMAND_ADMIN)
                || permission.equals(JustTeamsPermissions.COMMAND_RELOAD)
                || permission.startsWith("justteams.bypass.")) {
            return check(user, JustTeamsPermissions.ADMIN)
                    || check(user, JustTeamsPermissions.ALL);
        }

        if (permission.equals(JustTeamsPermissions.USER)
                || permission.equals(JustTeamsPermissions.ADMIN)) {
            return check(user, JustTeamsPermissions.ALL);
        }

        return false;
    }

    private boolean check(User user, String permission) {
        Tristate result = user.getCachedData().getPermissionData().checkPermission(permission);
        return result == Tristate.TRUE;
    }
}
