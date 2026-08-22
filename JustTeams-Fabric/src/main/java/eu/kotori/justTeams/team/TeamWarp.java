package eu.kotori.justTeams.team;

import java.util.UUID;

/** Persistent team warp definition used by the Fabric port. */
public final class TeamWarp {
    private final String name;
    private final UUID owner;
    private String password;
    private double cost;
    private boolean enabled;
    private boolean membersCanUse;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public TeamWarp(String name, UUID owner, String world, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.password = "";
        this.cost = 0.0D;
        this.enabled = true;
        this.membersCanUse = true;
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public String getPassword() { return password; }
    public double getCost() { return cost; }
    public boolean isEnabled() { return enabled; }
    public boolean isMembersCanUse() { return membersCanUse; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public void setPassword(String password) {
        if (password == null || password.equalsIgnoreCase("NONE")) {
            this.password = "";
            return;
        }
        String normalized = password.trim();
        if (normalized.length() > 64) throw new IllegalArgumentException("Warp passwords may not exceed 64 characters.");
        this.password = normalized;
    }

    public void setCost(double cost) { this.cost = Double.isFinite(cost) ? Math.max(0.0D, cost) : 0.0D; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setMembersCanUse(boolean membersCanUse) { this.membersCanUse = membersCanUse; }
    public void setLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
