package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamWarp;
import eu.kotori.justTeams.util.ChatInputManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/** In-place 54-slot views used by the main team inventory without opening a second chest screen. */
public final class TeamInPlaceGui {
    private static final int[] JOIN_REQUEST_SLOTS = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final int[] WARP_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final int[] MEMBER_HEAD_SLOTS = {9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44};
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    public enum View { MAIN, JOIN_REQUESTS, WARPS, SETTINGS }

    private static final WeakHashMap<TeamMenuHandler, View> VIEWS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> MAIN_SNAPSHOTS = new WeakHashMap<>();

    private TeamInPlaceGui() {}

    public static View view(TeamMenuHandler menu) { return VIEWS.getOrDefault(menu, View.MAIN); }

    public static void enterJoinRequests(TeamMenuHandler menu, PlayerEntity player, Team team) {
        snapshotMain(menu); VIEWS.put(menu, View.JOIN_REQUESTS);
        Inventory inventory = menu.getMenuInventory(); clearForSubmenu(inventory);
        inventory.setStack(4, namedGradient(Items.SOUL_LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs"));
        List<UUID> requests = new ArrayList<>();
        for (UUID uuid : team.getJoinRequests()) if (!team.isMember(uuid)) requests.add(uuid);
        for (int i = 0; i < JOIN_REQUEST_SLOTS.length && i < requests.size(); i++) inventory.setStack(JOIN_REQUEST_SLOTS[i], requestItem(player, requests.get(i)));
        if (requests.isEmpty()) {
            ItemStack empty = namedPlain(Items.PAPER, "No Join Requests");
            empty.set(DataComponentTypes.CUSTOM_NAME, Text.literal("No Join Requests").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("There are currently no pending requests.", Formatting.GRAY), plainLine("", Formatting.GRAY),
                    plainLine("Players can request to join if your", Formatting.DARK_GRAY), plainLine("team is set to public.", Formatting.DARK_GRAY))));
            inventory.setStack(22, empty);
        }
        inventory.setStack(49, backItem()); menu.sendContentUpdates();
    }

    public static void enterWarps(TeamMenuHandler menu, PlayerEntity player, Team team) {
        snapshotMain(menu); VIEWS.put(menu, View.WARPS);
        Inventory inventory = menu.getMenuInventory(); clearForSubmenu(inventory);
        inventory.setStack(4, namedGradient(Items.COMPASS, "ᴛᴇᴀᴍ ᴡᴀʀᴘs"));
        List<TeamWarp> warps = new ArrayList<>(team.getWarps());
        for (int i = 0; i < WARP_SLOTS.length && i < warps.size(); i++) inventory.setStack(WARP_SLOTS[i], warpItem(warps.get(i)));
        if (warps.isEmpty()) {
            ItemStack empty = namedPlain(Items.PAPER, "No Warps Set");
            empty.set(DataComponentTypes.CUSTOM_NAME, Text.literal("No Warps Set").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Your team has not set any warps yet.", Formatting.GRAY), plainLine("", Formatting.GRAY),
                    plainLine("Use /team setwarp <name> to create one.", Formatting.DARK_GRAY))));
            inventory.setStack(22, empty);
        }
        inventory.setStack(49, backItem()); menu.sendContentUpdates();
    }

    public static void enterSettings(TeamMenuHandler menu, PlayerEntity player, Team team) {
        snapshotMain(menu); VIEWS.put(menu, View.SETTINGS);
        Inventory inventory = menu.getMenuInventory(); clearForSubmenu(inventory);
        ItemStack tag = namedGradient(Items.NAME_TAG, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴛᴀɢ");
        tag.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Current: ", team.getTag(), Formatting.GRAY, Formatting.WHITE), plainLine("", Formatting.GRAY),
                plainLine("Click to change the team tag.", Formatting.YELLOW)))); inventory.setStack(11, tag);
        ItemStack description = namedGradient(Items.OAK_SIGN, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴅᴇsᴄʀɪᴘᴛɪᴏɴ");
        description.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Current: ", team.getDescription(), Formatting.GRAY, Formatting.WHITE), plainLine("", Formatting.GRAY),
                plainLine("Click to change the team description.", Formatting.YELLOW)))); inventory.setStack(13, description);
        ItemStack status = namedGradient(Items.ENDER_EYE, "ᴛᴇᴀᴍ sᴛᴀᴛᴜs");
        status.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(team.isPublic() ? "Public" : "Private", team.isPublic() ? Formatting.GREEN : Formatting.RED)))); inventory.setStack(15, status);
        inventory.setStack(22, backItem()); menu.sendContentUpdates();
    }

    public static void returnToMain(TeamMenuHandler menu) {
        ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu);
        if (snapshot == null) return;
        Inventory inventory = menu.getMenuInventory();
        for (int slot = 0; slot < snapshot.length; slot++) inventory.setStack(slot, snapshot[slot].copy());
        MAIN_SNAPSHOTS.remove(menu);
        VIEWS.put(menu, View.MAIN);
        menu.sendContentUpdates();
    }

    public static void refreshMainMembers(TeamMenuHandler menu, PlayerEntity viewer, Team team) {
        Inventory inventory = menu.getMenuInventory();
        for (int slot : MEMBER_HEAD_SLOTS) inventory.setStack(slot, ItemStack.EMPTY);
        List<TeamPlayer> members = new ArrayList<>(team.getMembers());
        TeamPlayer owner = team.getMember(team.getOwnerUuid());
        members.removeIf(member -> member.getPlayerUuid().equals(team.getOwnerUuid()));
        members.sort(Comparator.comparing(TeamPlayer::getJoinDate, Comparator.nullsLast(Comparator.naturalOrder())));
        if (owner != null) members.add(0, owner);
        for (int i = 0; i < MEMBER_HEAD_SLOTS.length && i < members.size(); i++) {
            inventory.setStack(MEMBER_HEAD_SLOTS[i], createMemberHead(viewer, members.get(i)));
        }
        menu.sendContentUpdates();
    }

    private static ItemStack createMemberHead(PlayerEntity viewer, TeamPlayer member) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(member.getPlayerUuid()));
        ServerPlayerEntity online = viewer instanceof ServerPlayerEntity serverPlayer
                ? serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(member.getPlayerUuid()) : null;
        boolean isOnline = online != null;
        String name = isOnline ? online.getName().getString() : member.getPlayerUuid().toString().substring(0, 8);
        MutableText title = Text.empty();
        title.append(Text.literal("● ").setStyle(Style.EMPTY.withColor(isOnline ? Formatting.GREEN : Formatting.RED).withItalic(false)));
        title.append(Text.literal(name).setStyle(Style.EMPTY.withColor(roleColor(member.getRole())).withBold(member.getRole() == TeamRole.OWNER).withItalic(false)));
        head.set(DataComponentTypes.CUSTOM_NAME, title);
        String role = switch (member.getRole()) { case OWNER -> "Owner"; case CO_OWNER -> "Co-Owner"; case MEMBER -> "Member"; };
        String joined = member.getJoinDate() == null ? "Unknown" : DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate());
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Role: ", role, Formatting.GRAY, Formatting.WHITE),
                composeLine("Joined: ", joined, Formatting.GRAY, Formatting.WHITE))));
        return head;
    }

    private static Formatting roleColor(TeamRole role) { return switch (role) { case OWNER -> Formatting.GOLD; case CO_OWNER -> Formatting.RED; case MEMBER -> Formatting.WHITE; }; }

    public static void updateMainSortItem(TeamMenuHandler menu, Team team) {
        ItemStack sort = namedGradient(Items.HOPPER, "sᴏʀᴛ ᴍᴇᴍʙᴇʀs");
        sort.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to change the sorting.", Formatting.GRAY), plainLine("", Formatting.GRAY),
                sortLine("Join Date", team.getCurrentSortType().name().equals("JOIN_DATE")),
                sortLine("Alphabetical", team.getCurrentSortType().name().equals("ALPHABETICAL")),
                sortLine("Online Status", team.getCurrentSortType().name().equals("ONLINE_STATUS")) )));
        menu.getMenuInventory().setStack(49, sort); ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu); if (snapshot != null) snapshot[49] = sort.copy(); menu.sendContentUpdates();
    }

    public static void updateMainPvpItem(TeamMenuHandler menu, Team team) {
        ItemStack pvp = namedGradient(Items.DIAMOND_SWORD, "ᴘᴠᴘ sᴛᴀᴛᴜs");
        pvp.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Toggle PvP between team members.", Formatting.GRAY), plainLine("", Formatting.GRAY),
                composeLine("Currently: ", team.isPvpEnabled() ? "Enabled" : "Disabled", Formatting.GRAY, team.isPvpEnabled() ? Formatting.GREEN : Formatting.RED),
                plainLine("", Formatting.GRAY), plainLine("Click to toggle.", Formatting.YELLOW))));
        menu.getMenuInventory().setStack(45, pvp); ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu); if (snapshot != null) snapshot[45] = pvp.copy(); menu.sendContentUpdates();
    }

    public static boolean handleJoinRequestClick(TeamMenuHandler menu, PlayerEntity player, Team team, int slot, int button) {
        int index = indexOf(JOIN_REQUEST_SLOTS, slot); if (index < 0) return false;
        List<UUID> requests = new ArrayList<>(); for (UUID uuid : team.getJoinRequests()) if (!team.isMember(uuid)) requests.add(uuid);
        if (index >= requests.size()) return true; UUID uuid = requests.get(index);
        if (button == 0) { team.removeJoinRequest(uuid); if (!JustTeamsFabric.teams().isInTeam(uuid)) { JustTeamsFabric.teams().addMember(team, new TeamPlayer(uuid, TeamRole.MEMBER, java.time.Instant.now(), false, false, false, true)); notifyPlayer(player, uuid, "Your request to join " + team.getName() + " was accepted."); }}
        else if (button == 1) { team.removeJoinRequest(uuid); notifyPlayer(player, uuid, "Your request to join " + team.getName() + " was denied."); }
        save(); enterJoinRequests(menu, player, team); return true;
    }

    public static boolean handleWarpClick(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot, int button) {
        int index = indexOf(WARP_SLOTS, slot); if (index < 0 || index >= team.getWarps().size()) return false;
        TeamWarp warp = team.getWarps().get(index);
        if (button == 1 && team.hasElevatedPermissions(player.getUuid())) { TeamWarpManagementGui.open(player, team, warp); return true; }
        if (!warp.isEnabled()) { player.sendMessage(Text.literal("That warp is disabled."), true); return true; }
        TeamPlayer member = team.getMember(player.getUuid()); if (member == null) return true;
        if (!warp.isMembersCanUse() && !team.isOwner(player.getUuid()) && !warp.getOwner().equals(player.getUuid())) { player.sendMessage(Text.literal("You do not have permission to use that warp."), true); return true; }
        if (!warp.getPassword().isEmpty()) {
            TeamStringInputGui.open(player, "Warp Password", "Enter password", value -> { if (!warp.getPassword().equals(value)) { player.sendMessage(Text.literal("Incorrect warp password."), true); enterWarps(menu, player, team); return; } requestWarp(player, warp); }, () -> enterWarps(menu, player, team)); return true;
        }
        requestWarp(player, warp); return true;
    }

    public static boolean handleSettingsClick(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        if (slot == 22) { returnToMain(menu); return true; }
        if (!team.hasElevatedPermissions(player.getUuid())) { player.sendMessage(Text.literal("Only the owner or co-owners can change team settings."), true); return true; }
        switch (slot) {
            case 11 -> ChatInputManager.begin(player, "Enter the new team tag (1-4 characters, or type cancel):", input -> { try { JustTeamsFabric.teams().setTag(player.getUuid(), input); save(); player.sendMessage(Text.literal("Team tag updated."), false); } catch (IllegalArgumentException | IllegalStateException exception) { player.sendMessage(Text.literal(exception.getMessage()), false); } enterSettings(menu, player, team); });
            case 13 -> ChatInputManager.begin(player, "Enter the new team description (1-256 characters, or type cancel):", input -> { try { JustTeamsFabric.teams().setDescription(player.getUuid(), input); save(); player.sendMessage(Text.literal("Team description updated."), false); } catch (IllegalArgumentException | IllegalStateException exception) { player.sendMessage(Text.literal(exception.getMessage()), false); } enterSettings(menu, player, team); });
            case 15 -> { try { boolean enabled = JustTeamsFabric.teams().togglePublic(player.getUuid()); save(); player.sendMessage(Text.literal("Team is now " + (enabled ? "public" : "private") + "."), false); enterSettings(menu, player, team); } catch (IllegalStateException exception) { player.sendMessage(Text.literal(exception.getMessage()), false); } }
            default -> { }
        }
        return true;
    }

    private static void requestWarp(ServerPlayerEntity player, TeamWarp warp) { JustTeamsFabric.teleports().requestWarp(player, new TeamLocation(warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch()), warp.getCost()); }
    private static void notifyPlayer(PlayerEntity viewer, UUID targetUuid, String message) { if (viewer instanceof ServerPlayerEntity serverPlayer) { ServerPlayerEntity target = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(targetUuid); if (target != null) target.sendMessage(Text.literal(message), false); } }
    private static void snapshotMain(TeamMenuHandler menu) { if (MAIN_SNAPSHOTS.containsKey(menu)) return; ItemStack[] snapshot = new ItemStack[54]; for (int slot = 0; slot < snapshot.length; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy(); MAIN_SNAPSHOTS.put(menu, snapshot); }
    private static void clearForSubmenu(Inventory inventory) { ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "); for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY); for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy()); for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy()); }
    private static ItemStack requestItem(PlayerEntity viewer, UUID uuid) { ItemStack head = new ItemStack(Items.PLAYER_HEAD); head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(uuid)); boolean online = false; String name = uuid.toString().substring(0, 8); if (viewer instanceof ServerPlayerEntity serverPlayer) { ServerPlayerEntity target = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(uuid); if (target != null) { online = true; name = target.getName().getString(); } } MutableText title = Text.empty(); title.append(Text.literal("● ").setStyle(Style.EMPTY.withColor(online ? Formatting.GREEN : Formatting.RED).withItalic(false))); title.append(gradientText(name, true)); head.set(DataComponentTypes.CUSTOM_NAME, title); head.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("This player wants to join your team.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Left-Click to Accept", Formatting.GREEN), plainLine("Right-Click to Deny", Formatting.RED)))); return head; }
    private static ItemStack warpItem(TeamWarp warp) { boolean passwordProtected = !warp.getPassword().isEmpty(); ItemStack stack = new ItemStack(passwordProtected ? Items.IRON_BLOCK : Items.GOLD_BLOCK); stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(warp.getName(), true)); int separator = warp.getWorld().lastIndexOf(':'); String serverName = separator >= 0 && separator + 1 < warp.getWorld().length() ? warp.getWorld().substring(separator + 1) : warp.getWorld(); stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(composeLine("Server: ", serverName, Formatting.GRAY, Formatting.WHITE), plainLine("", Formatting.GRAY), passwordProtected ? plainLine("Password Protected", Formatting.RED) : plainLine("Public", Formatting.GREEN)))); return stack; }
    private static ItemStack backItem() { ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ"); back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false))); back.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to return to the main menu.", Formatting.YELLOW)))); return back; }
    private static ItemStack namedPlain(Item item, String name) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false))); return stack; }
    private static ItemStack namedGradient(Item item, String name) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name, true)); return stack; }
    private static MutableText plainLine(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return plainLine(prefix, prefixColor).append(plainLine(value, valueColor)); }
    private static MutableText sortLine(String name, boolean selected) { return Text.literal("▪ " + name).setStyle(Style.EMPTY.withColor(selected ? Formatting.GREEN : Formatting.GRAY).withItalic(false)); }
    private static MutableText gradientText(String value, boolean bold) { MutableText result = Text.empty(); if (value.isEmpty()) return result; int length = Math.max(1, value.codePointCount(0, value.length()) - 1); int index = 0; for (int offset = 0; offset < value.length();) { int codePoint = value.codePointAt(offset); double t = (double) index / length; int sr = (PRIMARY_START >> 16) & 0xFF, sg = (PRIMARY_START >> 8) & 0xFF, sb = PRIMARY_START & 0xFF; int er = (PRIMARY_END >> 16) & 0xFF, eg = (PRIMARY_END >> 8) & 0xFF, eb = PRIMARY_END & 0xFF; int r = (int) Math.round(sr + (er - sr) * t), g = (int) Math.round(sg + (eg - sg) * t), b = (int) Math.round(sb + (eb - sb) * t); result.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(bold).withItalic(false))); offset += Character.charCount(codePoint); index++; } return result; }
    private static int indexOf(int[] values, int slot) { for (int i = 0; i < values.length; i++) if (values[i] == slot) return i; return -1; }
    private static void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save in-place team GUI change", e); } }
}
