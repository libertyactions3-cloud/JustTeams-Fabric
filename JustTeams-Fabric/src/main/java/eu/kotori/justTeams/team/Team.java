package eu.kotori.justTeams.team;

import eu.kotori.justTeams.economy.TeamBank;

import net.minecraft.util.Formatting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Fabric-side representation of a JustTeams team. */
public final class Team {
    private final int id;
    private String name;
    private String tag;
    private String description;
    private UUID ownerUuid;
    private Instant creationDate;
    private boolean pvpEnabled;
    private boolean publicTeam;
    private boolean glowEnabled;
    private Formatting glowColor;
    private double balance;
    private int kills;
    private int deaths;
    private TeamLocation home;
    private final List<TeamWarp> warps = new ArrayList<>();
    private final List<TeamPlayer> members = new ArrayList<>();
    private final List<UUID> joinRequests = new ArrayList<>();
    private final List<UUID> invites = new ArrayList<>();
    private final List<BlacklistedPlayer> blacklist = new ArrayList<>();
    private TeamSortType currentSortType = TeamSortType.ONLINE_STATUS;
    private final TeamBank bank;
    private TeamEnderChest enderChest;

    public Team(int id, String name, String tag, UUID ownerUuid, boolean defaultPvpStatus, boolean defaultPublicStatus, boolean defaultGlowStatus) {
        this(id, name, tag, ownerUuid, defaultPvpStatus, defaultPublicStatus, defaultGlowStatus, Instant.now());
    }

    public Team(int id, String name, String tag, UUID ownerUuid, boolean defaultPvpStatus, boolean defaultPublicStatus, boolean defaultGlowStatus, Instant creationDate) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.pvpEnabled = defaultPvpStatus;
        this.publicTeam = defaultPublicStatus;
        this.glowEnabled = defaultGlowStatus;
        this.creationDate = creationDate;
        this.bank = new TeamBank(this);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getTag() { return tag == null ? "" : tag; }
    public String getDescription() { return description == null ? "A new Team!" : description; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Instant getCreationDate() { return creationDate; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public boolean isPublic() { return publicTeam; }
    public boolean isGlowEnabled() { return glowEnabled; }
    public Formatting getGlowColor() { return glowColor; }
    public double getBalance() { return balance; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public TeamLocation getHome() { return home; }
    public List<TeamWarp> getWarps() { return warps; }
    public TeamWarp getWarp(String name) { return warps.stream().filter(warp -> warp.getName().equalsIgnoreCase(name)).findFirst().orElse(null); }
    public TeamBank getBank() { return bank; }
    public TeamEnderChest getEnderChest() { return enderChest; }
    public void setEnderChest(TeamEnderChest enderChest) { this.enderChest = enderChest; }
    public void setHome(TeamLocation home) { this.home = home; }
    public void clearHome() { this.home = null; }
    public void addWarp(TeamWarp warp) {
        if (getWarp(warp.getName()) != null) throw new IllegalArgumentException("A warp with that name already exists.");
        warps.add(warp);
    }
    public void removeWarp(String name) { warps.removeIf(warp -> warp.getName().equalsIgnoreCase(name)); }
    public void setName(String name) { this.name = name; }
    public void setTag(String tag) { this.tag = tag; }
    public void setDescription(String description) { this.description = description; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public void setCreationDate(Instant creationDate) { this.creationDate = creationDate; }
    public void setPvpEnabled(boolean enabled) { this.pvpEnabled = enabled; }
    public void setPublic(boolean value) { this.publicTeam = value; }
    public void setGlowEnabled(boolean enabled) { this.glowEnabled = enabled; }
    public void setGlowColor(Formatting color) { this.glowColor = color; }
    public void setBalance(double balance) { this.balance = balance; }
    public void addBalance(double amount) { this.balance += amount; }
    public void removeBalance(double amount) { this.balance -= amount; }
    public void setKills(int kills) { this.kills = kills; }
    public void incrementKills() { kills++; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void incrementDeaths() { deaths++; }
    public List<TeamPlayer> getMembers() { return members; }
    public List<UUID> getJoinRequests() { return joinRequests; }
    public List<UUID> getInvites() { return invites; }
    public List<BlacklistedPlayer> getBlacklist() { return blacklist; }
    public void addMember(TeamPlayer player) { if (!isMember(player.getPlayerUuid())) members.add(player); }
    public void removeMember(UUID playerUuid) { members.removeIf(member -> member.getPlayerUuid().equals(playerUuid)); }
    public boolean isMember(UUID playerUuid) { return members.stream().anyMatch(member -> member.getPlayerUuid().equals(playerUuid)); }
    public boolean isOwner(UUID playerUuid) { return ownerUuid.equals(playerUuid); }
    public TeamPlayer getMember(UUID playerUuid) { return members.stream().filter(member -> member.getPlayerUuid().equals(playerUuid)).findFirst().orElse(null); }
    public List<TeamPlayer> getCoOwners() { return members.stream().filter(member -> member.getRole() == TeamRole.CO_OWNER).toList(); }
    public boolean hasElevatedPermissions(UUID playerUuid) { TeamPlayer member = getMember(playerUuid); return member != null && (member.getRole() == TeamRole.OWNER || member.getRole() == TeamRole.CO_OWNER); }
    public void addJoinRequest(UUID playerUuid) {
        if (isBlacklisted(playerUuid)) throw new IllegalStateException("That player is blacklisted from this team.");
        if (!joinRequests.contains(playerUuid)) joinRequests.add(playerUuid);
    }
    public void removeJoinRequest(UUID playerUuid) { joinRequests.remove(playerUuid); }
    public void addInvite(UUID playerUuid) {
        if (isBlacklisted(playerUuid)) throw new IllegalStateException("That player is blacklisted from this team.");
        if (!invites.contains(playerUuid)) invites.add(playerUuid);
    }
    public void removeInvite(UUID playerUuid) { invites.remove(playerUuid); }
    public boolean hasInvite(UUID playerUuid) { return invites.contains(playerUuid); }
    public boolean hasJoinRequest(UUID playerUuid) { return joinRequests.contains(playerUuid); }
    public boolean isBlacklisted(UUID playerUuid) { return blacklist.stream().anyMatch(entry -> entry.getPlayerUuid().equals(playerUuid)); }
    public BlacklistedPlayer getBlacklistedPlayer(UUID playerUuid) { return blacklist.stream().filter(entry -> entry.getPlayerUuid().equals(playerUuid)).findFirst().orElse(null); }
    public void addBlacklistEntry(BlacklistedPlayer entry) {
        blacklist.removeIf(existing -> existing.getPlayerUuid().equals(entry.getPlayerUuid()));
        blacklist.add(entry);
        joinRequests.remove(entry.getPlayerUuid());
        invites.remove(entry.getPlayerUuid());
    }
    public boolean removeBlacklistEntry(UUID playerUuid) { return blacklist.removeIf(entry -> entry.getPlayerUuid().equals(playerUuid)); }
    public TeamSortType getCurrentSortType() { return currentSortType; }
    public void setSortType(TeamSortType sortType) { currentSortType = sortType == null ? TeamSortType.ONLINE_STATUS : sortType; }
    public void cycleSortType() { currentSortType = switch (currentSortType) { case ONLINE_STATUS -> TeamSortType.RANK; case RANK -> TeamSortType.ALPHABETICAL; case ALPHABETICAL -> TeamSortType.ONLINE_STATUS; }; }
    public List<TeamPlayer> getSortedMembers(Comparator<TeamPlayer> comparator) { return members.stream().sorted(comparator).toList(); }
    public String getPlainName() { return stripFormatting(name); }
    public String getPlainTag() { return stripFormatting(getTag()); }
    private static String stripFormatting(String value) { if (value == null) return ""; return value.replaceAll("(?i)&[0-9A-FK-OR]", "").replaceAll("(?i)<#[0-9A-F]{6}>", "").replaceAll("(?i)</#[0-9A-F]{6}>", ""); }
    public Optional<TeamPlayer> findMember(UUID uuid) { return Optional.ofNullable(getMember(uuid)); }
}
