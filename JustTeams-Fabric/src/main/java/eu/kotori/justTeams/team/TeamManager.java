package eu.kotori.justTeams.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory team registry. Persistence is intentionally separated so the
 * registry can later be backed by the JustTeams storage abstraction.
 */
public final class TeamManager {
    private final Map<Integer, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, Integer> playerTeams = new LinkedHashMap<>();
    private int nextId = 1;

    public Team createTeam(String name, String tag, UUID ownerUuid,
                           boolean defaultPvp, boolean defaultPublic, boolean defaultGlow) {
        if (getTeam(ownerUuid) != null) throw new IllegalStateException("Player already belongs to a team");
        Team team = new Team(nextId++, name, tag, ownerUuid, defaultPvp, defaultPublic, defaultGlow);
        team.addMember(new TeamPlayer(ownerUuid, TeamRole.OWNER, java.time.Instant.now(), true, true, true, true));
        teams.put(team.getId(), team);
        playerTeams.put(ownerUuid, team.getId());
        return team;
    }

    public void register(Team team) {
        teams.put(team.getId(), team);
        nextId = Math.max(nextId, team.getId() + 1);
        for (TeamPlayer member : team.getMembers()) playerTeams.put(member.getPlayerUuid(), team.getId());
    }

    public void unregister(Team team) {
        teams.remove(team.getId());
        for (TeamPlayer member : team.getMembers()) playerTeams.remove(member.getPlayerUuid());
    }

    public Team getTeam(int id) { return teams.get(id); }
    public Team getTeam(UUID playerUuid) { Integer id = playerTeams.get(playerUuid); return id == null ? null : teams.get(id); }
    public boolean isInTeam(UUID playerUuid) { return playerTeams.containsKey(playerUuid); }

    public void addMember(Team team, TeamPlayer player) {
        if (isInTeam(player.getPlayerUuid())) throw new IllegalStateException("Player already belongs to a team");
        if (team.isBlacklisted(player.getPlayerUuid())) throw new IllegalStateException("That player is blacklisted from this team.");
        player.setCanUseAutoBank(false);
        player.setCanUseHome(false);
        team.addMember(player);
        playerTeams.put(player.getPlayerUuid(), team.getId());
    }

    public void removeMember(Team team, UUID playerUuid) {
        TeamPlayer member = team.getMember(playerUuid);
        if (member != null) member.setCanUseAutoBank(false);
        team.removeMember(playerUuid);
        playerTeams.remove(playerUuid);
    }

    public boolean toggleGlow(UUID playerUuid) {
        Team team = getTeam(playerUuid);
        if (team == null) throw new IllegalStateException("You are not in a team.");
        if (!team.hasElevatedPermissions(playerUuid)) throw new IllegalStateException("Only the team owner or co-owner can change team glow.");
        boolean enabled = !team.isGlowEnabled();
        team.setGlowEnabled(enabled);
        return enabled;
    }

    public String setTag(UUID playerUuid, String value) {
        Team team = requireElevatedTeam(playerUuid);
        String tag = value == null ? "" : value.trim();
        if (tag.length() < 1 || tag.length() > 4 || !tag.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Team tag must be 1-4 letters or numbers with no spaces.");
        }
        team.setTag(tag);
        return tag;
    }

    public String setDescription(UUID playerUuid, String value) {
        Team team = requireElevatedTeam(playerUuid);
        String description = value == null ? "" : value.trim();
        if (description.length() < 1 || description.length() > 256) {
            throw new IllegalArgumentException("Team description must be 1-256 characters.");
        }
        team.setDescription(description);
        return description;
    }

    public boolean togglePublic(UUID playerUuid) {
        Team team = requireElevatedTeam(playerUuid);
        boolean enabled = !team.isPublic();
        team.setPublic(enabled);
        return enabled;
    }

    private Team requireElevatedTeam(UUID playerUuid) {
        Team team = getTeam(playerUuid);
        if (team == null) throw new IllegalStateException("You are not in a team.");
        if (!team.hasElevatedPermissions(playerUuid)) {
            throw new IllegalStateException("Only the team owner or co-owner can change team settings.");
        }
        return team;
    }

    public Collection<Team> getTeams() { return new ArrayList<>(teams.values()); }
    public int size() { return teams.size(); }
    public void clear() { teams.clear(); playerTeams.clear(); nextId = 1; }
}
