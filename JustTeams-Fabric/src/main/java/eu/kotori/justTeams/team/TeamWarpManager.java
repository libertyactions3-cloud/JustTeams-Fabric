package eu.kotori.justTeams.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Team-local warp registry. Storage integration is intentionally kept separate. */
public final class TeamWarpManager {
    private final List<TeamWarp> warps = new ArrayList<>();

    public List<TeamWarp> getWarps() {
        return Collections.unmodifiableList(warps);
    }

    public TeamWarp get(String name) {
        if (name == null) return null;
        String normalized = name.toLowerCase(Locale.ROOT);
        for (TeamWarp warp : warps) {
            if (warp.getName().toLowerCase(Locale.ROOT).equals(normalized)) return warp;
        }
        return null;
    }

    public boolean add(TeamWarp warp) {
        if (warp == null || get(warp.getName()) != null) return false;
        warps.add(warp);
        return true;
    }

    public boolean remove(String name) {
        TeamWarp warp = get(name);
        return warp != null && warps.remove(warp);
    }

    public boolean canManage(UUID player, Team team) {
        return player != null && team.hasElevatedPermissions(player);
    }
}
