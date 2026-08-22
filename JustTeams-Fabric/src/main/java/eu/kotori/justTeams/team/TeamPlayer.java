package eu.kotori.justTeams.team;

import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * Platform-neutral member state with Fabric-specific online-player lookup
 * supplied by the TeamManager.  Permission semantics mirror Paper JustTeams.
 */
public final class TeamPlayer {
    private final UUID playerUuid;
    private TeamRole role;
    private final Instant joinDate;

    private boolean canWithdraw;
    private boolean canUseEnderChest;
    private boolean canSetHome;
    private boolean canUseHome;
    private boolean canEditMembers;
    private boolean canEditCoOwners;
    private boolean canKickMembers;
    private boolean canPromoteMembers;
    private boolean canDemoteMembers;

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate,
                      boolean canWithdraw, boolean canUseEnderChest,
                      boolean canSetHome, boolean canUseHome) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
        this.canSetHome = canSetHome;
        this.canUseHome = canUseHome;
        setDefaultEditingPermissions();
    }

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate,
                      boolean canWithdraw, boolean canUseEnderChest,
                      boolean canSetHome, boolean canUseHome,
                      boolean canEditMembers, boolean canEditCoOwners,
                      boolean canKickMembers, boolean canPromoteMembers,
                      boolean canDemoteMembers) {
        this.playerUuid = playerUuid;
        this.role = role;
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
    }

    private void setDefaultEditingPermissions() {
        switch (role) {
            case OWNER -> {
                canEditMembers = true;
                canEditCoOwners = true;
                canKickMembers = true;
                canPromoteMembers = true;
                canDemoteMembers = true;
            }
            case CO_OWNER -> {
                canEditMembers = true;
                canEditCoOwners = false;
                canKickMembers = true;
                canPromoteMembers = false;
                canDemoteMembers = false;
            }
            case MEMBER -> {
                canEditMembers = false;
                canEditCoOwners = false;
                canKickMembers = false;
                canPromoteMembers = false;
                canDemoteMembers = false;
            }
        }
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public TeamRole getRole() { return role; }
    public Instant getJoinDate() { return joinDate; }

    public void setRole(TeamRole role) {
        this.role = role;
        setDefaultEditingPermissions();
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
                && role == TeamRole.OWNER && target.role == TeamRole.MEMBER && canPromoteMembers;
    }

    public boolean canDemotePlayer(TeamPlayer target) {
        return target != null && !playerUuid.equals(target.playerUuid)
                && role == TeamRole.OWNER && target.role == TeamRole.CO_OWNER && canDemoteMembers;
    }

    public ServerPlayerEntity getServerPlayer(Function<UUID, ServerPlayerEntity> lookup) {
        return lookup.apply(playerUuid);
    }
}
