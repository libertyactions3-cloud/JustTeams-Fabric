package eu.kotori.justTeams.storage;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamEnderChest;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamSortType;
import eu.kotori.justTeams.team.TeamWarp;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class TeamStorage {
    private static final int DATA_VERSION = 10;
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("justteams").resolve("teams.dat");

    public void load(TeamManager manager) throws IOException {
        manager.clear();
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) { save(manager); return; }
        NbtCompound root = NbtIo.read(path);
        int dataVersion = root.getInt("dataVersion", 1);
        NbtList teams = root.getListOrEmpty("teams");
        for (int i = 0; i < teams.size(); i++) {
            NbtCompound teamTag = teams.getCompoundOrEmpty(i);
            Team team = readTeam(teamTag);
            readBank(team, teamTag);
            readEnderChest(team, teamTag);
            if (dataVersion < DATA_VERSION) migrateAutoBankDefaults(team);
            manager.register(team);
        }
        if (dataVersion < DATA_VERSION) save(manager);
    }

    public void save(TeamManager manager) throws IOException {
        Files.createDirectories(path.getParent());
        NbtCompound root = new NbtCompound();
        root.putInt("dataVersion", DATA_VERSION);
        NbtList teams = new NbtList();
        for (Team team : manager.getTeams()) {
            NbtCompound teamTag = writeTeam(team);
            writeBank(team, teamTag);
            writeEnderChest(team, teamTag);
            teams.add(teamTag);
        }
        root.put("teams", teams);
        NbtIo.write(root, path);
    }

    public Path getPath() { return path; }

    private NbtCompound writeTeam(Team team) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("id", team.getId()); tag.putString("name", team.getName()); tag.putString("tag", team.getTag()); tag.putString("description", team.getDescription()); tag.putString("owner", team.getOwnerUuid().toString());
        tag.putLong("creationDate", team.getCreationDate().toEpochMilli()); tag.putBoolean("pvpEnabled", team.isPvpEnabled()); tag.putBoolean("publicTeam", team.isPublic()); tag.putBoolean("glowEnabled", team.isGlowEnabled());
        if (team.getGlowColor() != null) tag.putString("glowColor", team.getGlowColor().getName());
        tag.putDouble("balance", team.getBalance()); tag.putInt("kills", team.getKills()); tag.putInt("deaths", team.getDeaths()); tag.putString("sortType", team.getCurrentSortType().name());
        if (team.getHome() != null) tag.put("home", writeLocation(team.getHome()));
        NbtList warps = new NbtList(); for (TeamWarp warp : team.getWarps()) warps.add(writeWarp(warp)); tag.put("warps", warps);
        NbtList members = new NbtList(); for (TeamPlayer member : team.getMembers()) members.add(writeMember(member)); tag.put("members", members);
        NbtList requests = new NbtList(); for (UUID uuid : team.getJoinRequests()) requests.add(NbtString.of(uuid.toString())); tag.put("joinRequests", requests);
        NbtList invites = new NbtList(); for (UUID uuid : team.getInvites()) invites.add(NbtString.of(uuid.toString())); tag.put("invites", invites);
        NbtList blacklist = new NbtList(); for (BlacklistedPlayer entry : team.getBlacklist()) blacklist.add(writeBlacklistEntry(entry)); tag.put("blacklist", blacklist);
        return tag;
    }

    private NbtCompound writeBlacklistEntry(BlacklistedPlayer entry) {
        NbtCompound tag = new NbtCompound();
        tag.putString("uuid", entry.getPlayerUuid().toString()); tag.putString("name", entry.getPlayerName()); tag.putString("reason", entry.getReason());
        tag.putString("blacklistedBy", entry.getBlacklistedByUuid().toString()); tag.putString("blacklistedByName", entry.getBlacklistedByName()); tag.putLong("blacklistedAt", entry.getBlacklistedAt().toEpochMilli());
        return tag;
    }

    private void writeBank(Team team, NbtCompound teamTag) { NbtList bank = team.getBank().toNbtList(); if (!bank.isEmpty()) teamTag.put("bank", bank); }
    private void readBank(Team team, NbtCompound teamTag) { if (teamTag.contains("bank")) team.getBank().readNbtList(teamTag.getListOrEmpty("bank")); }

    private void writeEnderChest(Team team, NbtCompound teamTag) {
        TeamEnderChest enderChest = team.getEnderChest(); if (enderChest == null) return;
        NbtList inventory = enderChest.toNbtList(); if (!inventory.isEmpty()) teamTag.put("enderChest", inventory);
    }

    private void readEnderChest(Team team, NbtCompound teamTag) {
        if (!teamTag.contains("enderChest")) return;
        TeamEnderChest enderChest = new TeamEnderChest(team, JustTeamsFabric.config().getEnderChestRows());
        enderChest.readNbtList(teamTag.getListOrEmpty("enderChest")); team.setEnderChest(enderChest);
    }

    private NbtCompound writeLocation(TeamLocation location) { NbtCompound tag = new NbtCompound(); tag.putString("dimension", location.getDimension()); tag.putDouble("x", location.getX()); tag.putDouble("y", location.getY()); tag.putDouble("z", location.getZ()); tag.putFloat("yaw", location.getYaw()); tag.putFloat("pitch", location.getPitch()); return tag; }
    private NbtCompound writeWarp(TeamWarp warp) { NbtCompound tag = new NbtCompound(); tag.putString("name", warp.getName()); tag.putString("owner", warp.getOwner().toString()); tag.putString("password", warp.getPassword()); tag.putDouble("cost", warp.getCost()); tag.putBoolean("enabled", warp.isEnabled()); tag.putBoolean("membersCanUse", warp.isMembersCanUse()); tag.putString("world", warp.getWorld()); tag.putDouble("x", warp.getX()); tag.putDouble("y", warp.getY()); tag.putDouble("z", warp.getZ()); tag.putFloat("yaw", warp.getYaw()); tag.putFloat("pitch", warp.getPitch()); return tag; }

    private NbtCompound writeMember(TeamPlayer member) {
        NbtCompound tag = new NbtCompound();
        tag.putString("uuid", member.getPlayerUuid().toString()); tag.putString("role", member.getRole().name()); tag.putString("rank", member.getRank().name()); tag.putLong("joinDate", member.getJoinDate().toEpochMilli());
        if (member.getLastKnownName() != null) tag.putString("lastKnownName", member.getLastKnownName());
        tag.putBoolean("canWithdraw", member.canWithdraw()); tag.putBoolean("canUseEnderChest", member.canUseEnderChest()); tag.putBoolean("canSetHome", member.canSetHome()); tag.putBoolean("canUseHome", member.canUseHome());
        tag.putBoolean("canEditMembers", member.canEditMembers()); tag.putBoolean("canEditCoOwners", member.canEditCoOwners()); tag.putBoolean("canKickMembers", member.canKickMembers()); tag.putBoolean("canPromoteMembers", member.canPromoteMembers()); tag.putBoolean("canDemoteMembers", member.canDemoteMembers());
        tag.putBoolean("canInvite", member.canInvite()); tag.putBoolean("canSetWarps", member.canSetWarps()); tag.putBoolean("canUseAutoBank", member.canUseAutoBank()); tag.putBoolean("autoBankEnabled", member.isAutoBankEnabled()); tag.putBoolean("teamChatEnabled", member.isTeamChatEnabled());
        return tag;
    }

    private Team readTeam(NbtCompound tag) {
        int id = tag.getInt("id", 0); String name = tag.getString("name").orElse("Unnamed Team"); UUID owner = UUID.fromString(tag.getString("owner").orElseThrow());
        Team team = new Team(id, name, tag.getString("tag").orElse(""), owner, tag.getBoolean("pvpEnabled").orElse(true), tag.getBoolean("publicTeam").orElse(false), tag.getBoolean("glowEnabled").orElse(false), Instant.ofEpochMilli(tag.getLong("creationDate", System.currentTimeMillis())));
        tag.getString("glowColor").ifPresent(value -> { try { Formatting color = Formatting.byName(value); if (color != null && color.isColor()) team.setGlowColor(color); } catch (IllegalArgumentException ignored) { } });
        team.setDescription(tag.getString("description").orElse("A new Team!")); team.setBalance(tag.getDouble("balance", 0D)); team.setKills(tag.getInt("kills", 0)); team.setDeaths(tag.getInt("deaths", 0));
        try { team.setSortType(TeamSortType.valueOf(tag.getString("sortType").orElse("ONLINE_STATUS"))); } catch (IllegalArgumentException ignored) { }
        if (tag.contains("home")) team.setHome(readLocation(tag.getCompoundOrEmpty("home")));
        NbtList warps = tag.getListOrEmpty("warps"); for (int i = 0; i < warps.size(); i++) team.addWarp(readWarp(warps.getCompoundOrEmpty(i)));
        NbtList members = tag.getListOrEmpty("members"); for (int i = 0; i < members.size(); i++) team.addMember(readMember(members.getCompoundOrEmpty(i)));
        NbtList requests = tag.getListOrEmpty("joinRequests"); for (int i = 0; i < requests.size(); i++) requests.getString(i).ifPresent(v -> team.addJoinRequest(UUID.fromString(v)));
        NbtList invites = tag.getListOrEmpty("invites"); for (int i = 0; i < invites.size(); i++) invites.getString(i).ifPresent(v -> team.addInvite(UUID.fromString(v)));
        NbtList blacklist = tag.getListOrEmpty("blacklist");
        for (int i = 0; i < blacklist.size(); i++) {
            NbtCompound entry = blacklist.getCompoundOrEmpty(i);
            try { team.addBlacklistEntry(new BlacklistedPlayer(UUID.fromString(entry.getString("uuid").orElseThrow()), entry.getString("name").orElse("Unknown"), entry.getString("reason").orElse(""), UUID.fromString(entry.getString("blacklistedBy").orElseThrow()), entry.getString("blacklistedByName").orElse("Unknown"), Instant.ofEpochMilli(entry.getLong("blacklistedAt", System.currentTimeMillis())))); }
            catch (IllegalArgumentException ignored) { }
        }
        return team;
    }

    private TeamLocation readLocation(NbtCompound tag) { return new TeamLocation(tag.getString("dimension").orElse("minecraft:overworld"), tag.getDouble("x", 0D), tag.getDouble("y", 0D), tag.getDouble("z", 0D), tag.getFloat("yaw", 0F), tag.getFloat("pitch", 0F)); }
    private TeamWarp readWarp(NbtCompound tag) { TeamWarp warp = new TeamWarp(tag.getString("name").orElse("warp"), UUID.fromString(tag.getString("owner").orElseThrow()), tag.getString("world").orElse("minecraft:overworld"), tag.getDouble("x", 0D), tag.getDouble("y", 0D), tag.getDouble("z", 0D), tag.getFloat("yaw", 0F), tag.getFloat("pitch", 0F)); warp.setPassword(tag.getString("password").orElse("")); warp.setCost(tag.getDouble("cost", 0D)); warp.setEnabled(tag.getBoolean("enabled").orElse(true)); warp.setMembersCanUse(tag.getBoolean("membersCanUse").orElse(true)); return warp; }
    private TeamPlayer readMember(NbtCompound tag) {
        UUID uuid = UUID.fromString(tag.getString("uuid").orElseThrow());
        TeamRole role; try { role = TeamRole.valueOf(tag.getString("role").orElse("MEMBER")); } catch (IllegalArgumentException ignored) { role = TeamRole.MEMBER; }
        TeamRank rank; try { rank = TeamRank.valueOf(tag.getString("rank").orElse(role == TeamRole.OWNER ? "LEADER" : role == TeamRole.CO_OWNER ? "CO_LEADER" : "INITIATE")); } catch (IllegalArgumentException ignored) { rank = role == TeamRole.OWNER ? TeamRank.LEADER : role == TeamRole.CO_OWNER ? TeamRank.CO_LEADER : TeamRank.INITIATE; }
        TeamPlayer member = new TeamPlayer(uuid, role, rank, Instant.ofEpochMilli(tag.getLong("joinDate", System.currentTimeMillis())), tag.getBoolean("canWithdraw").orElse(false), tag.getBoolean("canUseEnderChest").orElse(false), tag.getBoolean("canSetHome").orElse(false), tag.getBoolean("canUseHome").orElse(false), tag.getBoolean("canEditMembers").orElse(false), tag.getBoolean("canEditCoOwners").orElse(false), tag.getBoolean("canKickMembers").orElse(false), tag.getBoolean("canPromoteMembers").orElse(false), tag.getBoolean("canDemoteMembers").orElse(false), tag.getBoolean("canInvite").orElse(rank == TeamRank.LEADER || rank == TeamRank.CO_LEADER || rank == TeamRank.OFFICER || rank == TeamRank.UNDEROFFICER), tag.getBoolean("canSetWarps").orElse(true), tag.getBoolean("canUseAutoBank").orElse(false));
        tag.getString("lastKnownName").ifPresent(member::setLastKnownName);
        member.setAutoBankEnabled(tag.getBoolean("autoBankEnabled").orElse(false));
        member.setTeamChatEnabled(tag.getBoolean("teamChatEnabled").orElse(false));
        return member;
    }

    private void migrateAutoBankDefaults(Team team) {
        for (TeamPlayer member : team.getMembers()) {
            if (member.getRank() == TeamRank.LEADER || member.getRank() == TeamRank.CO_LEADER) {
                member.setCanWithdraw(true);
                member.setCanUseAutoBank(true);
                member.setAutoBankEnabled(true);
            }
        }
    }
}
