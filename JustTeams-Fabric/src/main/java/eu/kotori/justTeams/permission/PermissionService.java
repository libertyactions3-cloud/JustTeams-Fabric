package eu.kotori.justTeams.permission;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central permission facade for JustTeams.
 *
 * Every check is delegated to the ECPerms-style registry so command predicates and execution-time
 * checks use the same Fabric Permissions API lookup and fallback rules.
 */
public interface PermissionService {
    boolean has(ServerCommandSource source, String permission);

    default boolean has(ServerPlayerEntity player, String permission) {
        return has(player.getCommandSource(), permission);
    }

    default boolean hasAny(ServerCommandSource source, String... permissions) {
        return JustTeamsPermissions.checkAny(source, permissions, 2);
    }

    static PermissionService defaults() {
        return JustTeamsPermissions::checkWithOperatorFallback;
    }
}
