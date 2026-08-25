package eu.kotori.justTeams.config;

import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Small dependency-free server configuration for JustTeams. */
public final class JustTeamsConfig {
    private static final String DEFAULT_CURRENCY_ITEMS =
            "minecraft:emerald,minecraft:emerald_block,minecraft:deepslate_emerald_ore";

    private final Path file;
    private final Properties properties = new Properties();
    private Set<Item> currencyItems = Set.of();
    private final EnumMap<TeamRole, Formatting> glowColors = new EnumMap<>(TeamRole.class);

    public JustTeamsConfig(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        this.file = configDirectory.resolve("justteams.properties");
        load();
    }

    public void load() throws IOException {
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
        }

        boolean changed = false;
        changed |= putDefault("bank.enabled", "true");
        changed |= putDefault("bank.currency-items", DEFAULT_CURRENCY_ITEMS);
        changed |= putDefault("glow.colors.owner", "RED");
        changed |= putDefault("glow.colors.co-owner", "DARK_RED");
        changed |= putDefault("glow.colors.member", "WHITE");
        changed |= putDefault("enderchest.enabled", "true");
        changed |= putDefault("enderchest.rows", "3");

        changed |= putDefault("feature-costs.enabled", "true");
        changed |= putDefault("feature-costs.sethome", "100");
        changed |= putDefault("feature-costs.home", "50");
        changed |= putDefault("feature-costs.enderchest", "25");
        changed |= putDefault("feature-costs.setwarp", "200");
        changed |= putDefault("feature-costs.warp", "75");
        changed |= putDefault("feature-costs.bank-withdraw", "10");
        changed |= putDefault("feature-costs.rename", "500");

        changed |= putDefault("team-creation.min-name-length", "3");
        changed |= putDefault("team-creation.max-name-length", "16");
        changed |= putDefault("team-creation.max-tag-length", "6");
        changed |= putDefault("team-creation.default-pvp", "true");
        changed |= putDefault("team-creation.default-public", "false");

        changed |= putDefault("team_home.warmup_seconds", "5");
        changed |= putDefault("team_home.cooldown_seconds", "300");
        changed |= putDefault("team_warps.warmup_seconds", "5");
        changed |= putDefault("team_warps.cooldown_seconds", "300");
        changed |= putDefault("effects.sounds.enabled", "true");
        changed |= putDefault("effects.sounds.success", "BLOCK_NOTE_BLOCK_PLING");
        changed |= putDefault("effects.sounds.error", "BLOCK_NOTE_BLOCK_BASS");
        changed |= putDefault("effects.sounds.teleport", "BLOCK_BEACON_ACTIVATE");
        changed |= putDefault("effects.particles.enabled", "true");
        changed |= putDefault("effects.particles.teleport_warmup", "PORTAL");
        changed |= putDefault("effects.particles.teleport_success", "END_ROD");

        if (changed || !Files.exists(file)) save();

        currencyItems = parseCurrencyItems(properties.getProperty("bank.currency-items", DEFAULT_CURRENCY_ITEMS));
        glowColors.clear();
        for (TeamRole role : TeamRole.values()) {
            glowColors.put(role, parseFormatting(properties.getProperty("glow.colors." + roleKey(role), "WHITE")));
        }
    }

    private boolean putDefault(String key, String value) {
        if (properties.containsKey(key)) return false;
        properties.setProperty(key, value);
        return true;
    }

    public void save() throws IOException {
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "JustTeams Fabric configuration");
        }
    }

    public boolean isBankEnabled() { return Boolean.parseBoolean(properties.getProperty("bank.enabled", "true")); }
    public boolean isEnderChestEnabled() { return Boolean.parseBoolean(properties.getProperty("enderchest.enabled", "true")); }
    public int getEnderChestRows() {
        int rows;
        try { rows = Integer.parseInt(properties.getProperty("enderchest.rows", "3")); }
        catch (NumberFormatException ignored) { rows = 3; }
        return Math.max(1, Math.min(6, rows));
    }
    public Set<Item> getCurrencyItems() { return currencyItems; }
    public Path getFile() { return file; }
    public Formatting getGlowColor(TeamRole role) { return glowColors.getOrDefault(role, Formatting.WHITE); }

    public boolean isFeatureCostsEnabled() { return Boolean.parseBoolean(properties.getProperty("feature-costs.enabled", "true")); }
    public double getFeatureCost(String feature) { return getDouble("feature-costs." + feature, 0.0D); }

    public int getMinTeamNameLength() { return getInt("team-creation.min-name-length", 3, 1); }
    public int getMaxTeamNameLength() { return getInt("team-creation.max-name-length", 16, 1); }
    public int getMaxTeamTagLength() { return getInt("team-creation.max-tag-length", 6, 2); }
    public boolean getDefaultTeamPvp() { return Boolean.parseBoolean(properties.getProperty("team-creation.default-pvp", "true")); }
    public boolean getDefaultTeamPublic() { return Boolean.parseBoolean(properties.getProperty("team-creation.default-public", "false")); }

    public int getHomeWarmupSeconds() { return getInt("team_home.warmup_seconds", 5, 0); }
    public int getHomeCooldownSeconds() { return getInt("team_home.cooldown_seconds", 300, 0); }
    public int getWarpWarmupSeconds() { return getInt("team_warps.warmup_seconds", 5, 0); }
    public int getWarpCooldownSeconds() { return getInt("team_warps.cooldown_seconds", 300, 0); }
    public boolean isSoundsEnabled() { return Boolean.parseBoolean(properties.getProperty("effects.sounds.enabled", "true")); }
    public boolean isParticlesEnabled() { return Boolean.parseBoolean(properties.getProperty("effects.particles.enabled", "true")); }
    public String getSuccessSound() { return properties.getProperty("effects.sounds.success", "BLOCK_NOTE_BLOCK_PLING"); }
    public String getErrorSound() { return properties.getProperty("effects.sounds.error", "BLOCK_NOTE_BLOCK_BASS"); }
    public String getTeleportSound() { return properties.getProperty("effects.sounds.teleport", "BLOCK_BEACON_ACTIVATE"); }
    public String getWarmupParticle() { return properties.getProperty("effects.particles.teleport_warmup", "PORTAL"); }
    public String getSuccessParticle() { return properties.getProperty("effects.particles.teleport_success", "END_ROD"); }
    public int getWarmupParticleCount() { return 10; }
    public int getSuccessParticleCount() { return 30; }

    private int getInt(String key, int fallback, int minimum) {
        try { return Math.max(minimum, Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private double getDouble(String key, double fallback) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
            return Double.isFinite(value) ? Math.max(0.0D, value) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String roleKey(TeamRole role) {
        return role.name().toLowerCase().replace('_', '-');
    }

    private static Formatting parseFormatting(String value) {
        try {
            Formatting formatting = Formatting.byName(value.toLowerCase());
            return formatting != null && formatting.isColor() ? formatting : Formatting.WHITE;
        } catch (IllegalArgumentException ignored) {
            return Formatting.WHITE;
        }
    }

    private static Set<Item> parseCurrencyItems(String value) {
        Set<Item> result = new LinkedHashSet<>();
        for (String rawId : value.split(",")) {
            String idText = rawId.trim();
            if (idText.isEmpty()) continue;
            Identifier id;
            try { id = Identifier.of(idText); } catch (IllegalArgumentException ignored) { continue; }
            if (!Registries.ITEM.containsId(id)) continue;
            result.add(Registries.ITEM.get(id));
        }
        return Set.copyOf(result);
    }
}
