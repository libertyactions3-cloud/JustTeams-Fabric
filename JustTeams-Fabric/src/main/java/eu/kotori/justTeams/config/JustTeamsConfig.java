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
        } else {
            properties.setProperty("bank.enabled", "true");
            properties.setProperty("bank.currency-items", DEFAULT_CURRENCY_ITEMS);
            properties.setProperty("glow.colors.owner", "RED");
            properties.setProperty("glow.colors.co-owner", "DARK_RED");
            properties.setProperty("glow.colors.member", "WHITE");
            properties.setProperty("enderchest.enabled", "true");
            properties.setProperty("enderchest.rows", "3");
            save();
        }

        currencyItems = parseCurrencyItems(properties.getProperty("bank.currency-items", DEFAULT_CURRENCY_ITEMS));
        glowColors.clear();
        for (TeamRole role : TeamRole.values()) {
            glowColors.put(role, parseFormatting(properties.getProperty("glow.colors." + roleKey(role), "WHITE")));
        }
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
