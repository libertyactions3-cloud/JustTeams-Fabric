package eu.kotori.justTeams.team;

import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/** Platform-neutral member state with Fabric-specific online-player lookup. */
public final class TeamPlayer {
    private final UUID playerUuid;
    private TeamRole role;
    private TeamRank rank;
    private final Instant joinDate;
    private String lastKnownName;

    private boolean canWithdraw;
    private boolean canUseEnderChest;
    private boolean canSetHome;
    private boolean canUseHome;
    private boolean canEditMembers;
    private boolean canEditCoOwners;
    private boolean canKickMembers;
    private boolean canPromoteMembers;
    private boolean canDemoteMembers;
    private boolean canInvite;
    private boolean canSetWarps;
    private boolean canUseAutoBank;
    private boolean autoBankEnabled;
    private boolean teamChatEnabled;

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate,
                      boolean canWithdraw, boolean canUseEnderChest,
                      boolean canSetHome, boolean canUseHome) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.rank = defaultRank(role);
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
        this.canSetHome = canSetHome;
        this.canUseHome = canUseHome;
        this.canSetWarps = true;
        this.canUseAutoBank = defaultAutoBankPermission(this.rank);
        this.autoBankEnabled = this.canUseAutoBank;
        this.teamChatEnabled = false;
        this.canInvite = defaultInvitePermission(this.rank);
        setDefaultEditingPermissions();
    }

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate,
                      boolean canWithdraw, boolean canUseEnderChest,
                      boolean canSetHome, boolean canUseHome,
                      boolean canEditMembers, boolean canEditCoOwners,
                      boolean canKickMembers, boolean canPromoteMembers,
                      boolean canDemoteMembers) {
        this(playerUuid, role, joinDate, canWithdraw, canUseEnderChest, canSetHome, canUseHome);
        this.canEditMembers = canEditMembers;
        this.canEditCoOwners = canEditCoOwners;
        this.canKickMembers = canKickMembers;
        this.canPromoteMembers = canPromoteMembers;
        this.canDemoteMembers = canDemoteMembers;
    }

    public TeamPlayer(UUID playerUuid, TeamRole role, TeamRank rank, Instant joinDate,
                      boolean canWithdraw, boolean canUseEnderChest,
                      boolean canSetHome, boolean canUseHome,
                      boolean canEditMembers, boolean canEditCoOwners,
                      boolean canKickMembers, boolean canPromoteMembers,
                      boolean canDemoteMembers, boolean canInvite,
                      boolean canSetWarps, boolean canUseAutoBank) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.rank = rank == null ? defaultRank(role) : rank;
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
        this.canSetHome = canSetHome;
        this.canUseHome = canUseHome;
        this.canEditMembers = canEditMembers;
        this.canEditCoOwners = canEditCoOwners;
        this.canKickMembers = canKickMembers;
        this.canPromoteMembers = canPromoteMembers;
        this.canDemoteMembers = canDemoteMembers;
        this.canInvite = canInvite;
        this.canSetWarps = canSetWarps;
        this.canUseAutoBank = canUseAutoBank || defaultAutoBankPermission(this.rank);
        this.autoBankEnabled = this.canUseAutoBank && defaultAutoBankPermission(this.rank);
        this.teamChatEnabled = false;
    }

    private static TeamRank defaultRank(TeamRole role) {
        return switch (role) {
            case OWNER -> TeamRank.LEADER;
            case CO_OWNER -> TeamRank.CO_LEADER;
            case MEMBER -> TeamRank.INITIATE;
        };
    }

    private static boolean defaultInvitePermission(TeamRank rank) {
        return rank == TeamRank.LEADER || rank == TeamRank.CO_LEADER
                || rank == TeamRank.OFFICER || rank == TeamRank.UNDEROFFICER;
    }

    private static boolean defaultAutoBankPermission(TeamRank rank) {
        return rank == TeamRank.LEADER || rank == TeamRank.CO_LEADER;
    }

    private void setDefaultEditingPermissions() {
        switch (role) {
            case OWNER -> {
                canEditMembers = true; canEditCoOwners = true; canKickMembers = true;
                canPromoteMembers = true; canDemoteMembers = true;
            }
            case CO_OWNER -> {
                canEditMembers = true; canEditCoOwners = false; canKickMembers = true;
                canPromoteMembers = false; canDemoteMembers = false;
            }
            case MEMBER -> {
                canEditMembers = false; canEditCoOwners = false; canKickMembers = false;
                canPromoteMembers = false; canDemoteMembers = false;
            }
        }
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public TeamRole getRole() { return role; }
    public TeamRank getRank() { return rank; }
    public Instant getJoinDate() { return joinDate; }
    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String name) { if (name != null && !name.isBlank()) lastKnownName = name; }

    public void setRole(TeamRole role) { this.role = role; }

    public void setRank(TeamRank rank) {
        if (rank == null) return;
        this.rank = rank;
        if (rank == TeamRank.LEADER) this.role = TeamRole.OWNER;
        else if (rank == TeamRank.CO_LEADER) this.role = TeamRole.CO_OWNER;
        else this.role = TeamRole.MEMBER;
        if (defaultAutoBankPermission(rank)) {
            this.canUseAutoBank = true;
            this.autoBankEnabled = true;
        }
    }

    public boolean canWithdraw() { return canWithdraw; }
    public void setCanWithdraw(boolean value) { canWithdraw = value; }
    public boolean canUseEnderChest() { return canUseEnderChest; }
    public void setCanUseEnderChest(boolean value) { canUseEnderChest = value; }
    public boolean canSetHome() { return canSetHome; }
    public void setCanSetHome(boolean value) { canSetHome = value; }
    public boolean canUseHome() { return canUseHome; }
    public void setCanUseHome(boolean value) { canUseHome = value; }
    public boolean canEditMembers() { return canEditMembers; }
    public void setCanEditMembers(boolean value) { canEditMembers = value; }
    public boolean canEditCoOwners() { return canEditCoOwners; }
    public void setCanEditCoOwners(boolean value) { canEditCoOwners = value; }
    public boolean canKickMembers() { return canKickMembers; }
    public void setCanKickMembers(boolean value) { canKickMembers = value; }
    public boolean canPromoteMembers() { return canPromoteMembers; }
    public void setCanPromoteMembers(boolean value) { canPromoteMembers = value; }
    public boolean canDemoteMembers() { return canDemoteMembers; }
    public void setCanDemoteMembers(boolean value) { canDemoteMembers = value; }
    public boolean canInvite() { return canInvite; }
    public void setCanInvite(boolean value) { canInvite = value; }
    public boolean canSetWarps() { return canSetWarps; }
    public void setCanSetWarps(boolean value) { canSetWarps = value; }
    public boolean canUseAutoBank() { return canUseAutoBank; }
    public void setCanUseAutoBank(boolean value) { canUseAutoBank = value; if (!value) autoBankEnabled = false; }
    public boolean isAutoBankEnabled() { return autoBankEnabled; }
    public void setAutoBankEnabled(boolean value) { autoBankEnabled = canUseAutoBank && value; }
    public boolean isTeamChatEnabled() { return teamChatEnabled; }
    public void setTeamChatEnabled(boolean value) { teamChatEnabled = value; }

    public boolean canEditPlayer(TeamPlayer target) {
        if (target == null || playerUuid.equals(target.playerUuid)) return false;
        if (role == TeamRole.OWNER) return true;
        if (role == TeamRole.CO_OWNER) {
            if (target.role == TeamRole.MEMBER) return canEditMembers;
            if (target.role == TeamRole.CO_OWNER) return canEditCoOwners;
        }
        return false;
    }

    public boolean canKickPlayer(TeamPlayer target) {
        if (target == null || playerUuid.equals(target.playerUuid)) return false;
        if (role == TeamRole.OWNER) return true;
        return role == TeamRole.CO_OWNER && target.role == TeamRole.MEMBER && canKickMembers;
    }

    public boolean canPromotePlayer(TeamPlayer target) {
        return target != null && !playerUuid.equals(target.playerUuid)
                && role == TeamRole.OWNER && canPromoteMembers;
    }

    public boolean canDemotePlayer(TeamPlayer target) {
        return target != null && !playerUuid.equals(target.playerUuid)
                && role == TeamRole.OWNER && canDemoteMembers;
    }

    public ServerPlayerEntity getServerPlayer(Function<UUID, ServerPlayerEntity> lookup) { return lookup.apply(playerUuid); }
}
