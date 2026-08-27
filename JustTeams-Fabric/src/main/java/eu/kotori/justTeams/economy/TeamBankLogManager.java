package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.PlayerNameResolver;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent seven-day team-bank audit log. Entries are capped at 10,000 per
 * team and older entries are pruned on load/write.
 */
public final class TeamBankLogManager {
    public static final long RETENTION_SECONDS = 7L * 24L * 60L * 60L;
    public static final int MAX_ENTRIES = 10_000;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Map<Integer, List<Entry>> CACHE = new HashMap<>();

    private TeamBankLogManager() {}

    public enum Kind {
        AUTOBANK,
        MANUAL_WITHDRAWAL
    }

    public record Entry(long timestampMillis, UUID playerUuid, String playerName, long amount, Kind kind, String action) {}
    public record TopSpender(UUID playerUuid, String playerName, long amount) {}

    public static synchronized void record(MinecraftServer server, Team team, ServerPlayerEntity player, long amount, Kind kind, String action) {
        if (team == null || player == null || amount <= 0L || kind == null) return;
        PlayerNameResolver.remember(server, player);
        List<Entry> entries = load(team);
        entries.add(new Entry(System.currentTimeMillis(), player.getUuid(), player.getName().getString(), amount, kind, action == null ? "bank" : action));
        prune(entries, System.currentTimeMillis());
        save(team, entries);
    }

    public static synchronized List<Entry> recent(Team team) {
        return List.copyOf(load(team));
    }

    public static synchronized TopSpender topAutoBankSpender(Team team) {
        long cutoff = System.currentTimeMillis() - RETENTION_SECONDS * 1000L;
        Map<UUID, Long> totals = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        for (Entry entry : load(team)) {
            if (entry.timestampMillis() < cutoff || entry.kind() != Kind.AUTOBANK) continue;
            totals.merge(entry.playerUuid(), entry.amount(), Long::sum);
            names.put(entry.playerUuid(), entry.playerName());
        }
        return totals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> new TopSpender(e.getKey(), names.getOrDefault(e.getKey(), "Unknown"), e.getValue()))
                .orElse(null);
    }

    public static String formatTimestamp(long millis) {
        return DISPLAY_TIME.format(Instant.ofEpochMilli(millis));
    }

    public static synchronized void pruneAll() {
        long now = System.currentTimeMillis();
        for (Integer id : new ArrayList<>(CACHE.keySet())) {
            prune(CACHE.get(id), now);
            Team team = JustTeamsFabric.teams().getTeam(id);
            if (team != null) save(team, CACHE.get(id));
        }
        try {
            Path directory = logDirectory();
            if (!Files.exists(directory)) return;
            try (var stream = Files.list(directory)) {
                stream.filter(path -> path.getFileName().toString().startsWith("team-") && path.getFileName().toString().endsWith(".dat"))
                        .forEach(path -> { /* loaded lazily; stale-file pruning is handled when the team is loaded */ });
            }
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.warn("Unable to inspect JustTeams bank-log directory", exception);
        }
    }

    private static List<Entry> load(Team team) {
        return CACHE.computeIfAbsent(team.getId(), ignored -> {
            List<Entry> entries = new ArrayList<>();
            Path path = file(team.getId());
            try {
                if (Files.exists(path)) {
                    NbtCompound root = NbtIo.read(path);
                    NbtList list = root.getListOrEmpty("entries");
                    for (int i = 0; i < list.size(); i++) {
                        NbtCompound tag = list.getCompoundOrEmpty(i);
                        try {
                            UUID uuid = UUID.fromString(tag.getString("uuid").orElseThrow());
                            Kind kind = Kind.valueOf(tag.getString("kind").orElse(Kind.MANUAL_WITHDRAWAL.name()));
                            entries.add(new Entry(tag.getLong("timestamp", 0L), uuid, tag.getString("name").orElse("Unknown"), tag.getLong("amount", 0L), kind, tag.getString("action").orElse("bank")));
                        } catch (IllegalArgumentException ignoredEntry) { }
                    }
                }
            } catch (IOException exception) {
                JustTeamsFabric.LOGGER.warn("Unable to load bank logs for team {}", team.getId(), exception);
            }
            prune(entries, System.currentTimeMillis());
            return entries;
        });
    }

    private static void save(Team team, List<Entry> entries) {
        if (team == null) return;
        try {
            Path path = file(team.getId());
            Files.createDirectories(path.getParent());
            NbtCompound root = new NbtCompound();
            NbtList list = new NbtList();
            for (Entry entry : entries) {
                NbtCompound tag = new NbtCompound();
                tag.putLong("timestamp", entry.timestampMillis());
                tag.putString("uuid", entry.playerUuid().toString());
                tag.putString("name", entry.playerName());
                tag.putLong("amount", entry.amount());
                tag.putString("kind", entry.kind().name());
                tag.putString("action", entry.action());
                list.add(tag);
            }
            root.put("entries", list);
            NbtIo.write(root, path);
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.error("Unable to save bank logs for team {}", team.getId(), exception);
        }
    }

    private static void prune(List<Entry> entries, long now) {
        long cutoff = now - RETENTION_SECONDS * 1000L;
        entries.removeIf(entry -> entry.timestampMillis() < cutoff || entry.amount() <= 0L);
        entries.sort(Comparator.comparingLong(Entry::timestampMillis));
        if (entries.size() > MAX_ENTRIES) entries.subList(0, entries.size() - MAX_ENTRIES).clear();
    }

    private static Path logDirectory() { return JustTeamsFabric.config().getDirectory().resolve("bank-logs"); }
    private static Path file(int teamId) { return logDirectory().resolve("team-" + teamId + ".dat"); }
}
