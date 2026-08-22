package eu.kotori.justTeams.team;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/** A dimension-aware location persisted by JustTeams for homes and warps. */
public final class TeamLocation {
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public TeamLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static TeamLocation fromPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        return new TeamLocation(world.getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
    }

    public String getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public Vec3d position() { return new Vec3d(x, y, z); }
}
