package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.util.PlayerNameResolver;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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
import java.util.List;
import java.util.WeakHashMap;

/** In-place member management view for the persistent team inventory. */
public final class TeamInPlaceMemberGui {
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> MAIN_SNAPSHOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, TeamPlayer> TARGETS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Integer> MAIN_SLOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Boolean> TRANSFER_CONFIRM = new WeakHashMap<>();

    private TeamInPlaceMemberGui() {}

    public static boolean isOpen(TeamMenuHandler menu) { return TARGETS.containsKey(menu); }

    public static void enter(TeamMenuHandler menu, PlayerEntity viewer, Team team, TeamPlayer target, int mainSlot) {
        snapshot(menu);
        TARGETS.put(menu, target);
        MAIN_SLOTS.put(menu, mainSlot);
        TRANSFER_CONFIRM.put(menu, false);
        renderEditor(menu, viewer, team, target);
    }

    public static void back(TeamMenuHandler menu) {
        restore(menu);
        TARGETS.remove(menu);
        MAIN_SLOTS.remove(menu);
        TRANSFER_CONFIRM.remove(menu);
    }

    public static boolean handle(TeamMenuHandler menu, PlayerEntity player, Team team, int slot) {
        TeamPlayer target = TARGETS.get(menu);
        if (target == null) return false;
        boolean confirming = Boolean.TRUE.equals(TRANSFER_CONFIRM.get(menu));

        if (confirming) {
            if (slot == 21) { applyTransfer(menu, player, team, target); return true; }
            if (slot == 23) { TRANSFER_CONFIRM.put(menu, false); renderEditor(menu, player, team, target); return true; }
            if (slot == 49) { back(menu); return true; }
            return true;
        }

        if (slot == 49) { back(menu); return true; }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (!team.isOwner(player.getUuid()) || target.getPlayerUuid().equals(player.getUuid())) return true;

        switch (slot) {
            case 19 -> {
                TeamRank next = target.getRank().promote();
                if (next != target.getRank()) {
                    target.setRank(next);
                    notifyRankChange(serverPlayer, team, target, true);
                }
            }
            case 20 -> {
                TeamRank next = target.getRank().demote();
                if (next != target.getRank()) {
                    target.setRank(next);
                    notifyRankChange(serverPlayer, team, target, false);
                }
            }
            case 22 -> {
                TeamChatManager.disable(target.getPlayerUuid());
                TeamEnderChestGui.closeViewer(serverPlayer.getEntityWorld().getServer(), team, target.getPlayerUuid());
                JustTeamsFabric.glow().stopGlowForPlayer(serverPlayer.getEntityWorld().getServer(), target.getPlayerUuid());
                JustTeamsFabric.teams().removeMember(team, target.getPlayerUuid());
                save();
                TeamNotificationManager.notifyKick(serverPlayer.getEntityWorld().getServer(), team, player.getUuid(), target.getPlayerUuid());
                TARGETS.remove(menu); MAIN_SLOTS.remove(menu); TRANSFER_CONFIRM.remove(menu);
                TeamInPlaceGui.returnToMain(menu);
                TeamInPlaceGui.refreshMainMembers(menu, player, team);
                return true;
            }
            case 25 -> { TRANSFER_CONFIRM.put(menu, true); renderTransferConfirmation(menu, player, team, target); return true; }
            case 37 -> {
                boolean enable = !(target.canWithdraw() && target.canUseAutoBank());
                target.setCanWithdraw(enable);
                target.setCanUseAutoBank(enable);
            }
            case 38 -> target.setCanSetWarps(!target.canSetWarps());
            case 39 -> target.setCanUseEnderChest(!target.canUseEnderChest());
            case 41 -> target.setCanSetHome(!target.canSetHome());
            case 42 -> target.setCanInvite(!target.canInvite());
            case 43 -> target.setCanUseHome(!target.canUseHome());
            default -> { return true; }
        }

        save();
        renderEditor(menu, player, team, target);
        return true;
    }

    private static void renderEditor(TeamMenuHandler menu, PlayerEntity viewer, Team team, TeamPlayer target) {
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofDynamic(target.getPlayerUuid()));
        head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(resolveName(viewer, target)).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Rank: ", target.getRank().getDisplayName(), Formatting.GRAY, Formatting.WHITE),
                composeLine("Joined: ", formatDate(target), Formatting.GRAY, Formatting.WHITE))));
        inventory.setStack(4, head);

        inventory.setStack(19, target.getRank() == TeamRank.CO_LEADER || target.getRank() == TeamRank.LEADER
                ? lockedItem(Items.GRAY_DYE, "ʟᴏᴄᴋᴇᴅ", "This rank cannot be promoted further.")
                : actionItem(Items.LIME_DYE, "ᴘʀᴏᴍᴏᴛᴇ ᴛᴏ " + smallCaps(target.getRank().promote().getDisplayName()), List.of(
                        plainLine("Move this player one rank higher.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to promote.", Formatting.YELLOW))));
        inventory.setStack(20, target.getRank() == TeamRank.LEADER || target.getRank() == TeamRank.INITIATE
                ? lockedItem(Items.RED_DYE, "ʟᴏᴄᴋᴇᴅ", "This rank cannot be demoted further.")
                : actionItem(Items.RED_DYE, "ᴅᴇᴍᴏᴛᴇ ᴛᴏ " + smallCaps(target.getRank().demote().getDisplayName()), List.of(
                        plainLine("Move this player one rank lower.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to demote.", Formatting.YELLOW))));

        inventory.setStack(22, actionItem(Items.RED_WOOL, "ᴋɪᴄᴋ ᴍᴇᴍʙᴇʀ", List.of(
                plainLine("Removes this player from the team.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to kick.", Formatting.YELLOW))));
        inventory.setStack(25, actionItem(Items.BEACON, "ᴛʀᴀɴsғᴇʀ ᴏᴡɴᴇʀsʜɪᴘ", List.of(
                plainLine("Transfer team ownership to this player.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to continue.", Formatting.YELLOW))));

        boolean bankWithdraw = target.canWithdraw();
        boolean autoBankPermission = target.canUseAutoBank();
        inventory.setStack(37, bankPermissionItem(bankWithdraw, autoBankPermission, target.isAutoBankEnabled()));
        inventory.setStack(38, permissionItem(Items.COMPASS, "ᴄʀᴇᴀᴛᴇ ᴛᴇᴀᴍ ᴡᴀʀᴘs", "Allow this member to create team warps.", target.canSetWarps()));
        inventory.setStack(39, permissionItem(Items.ENDER_CHEST, "ᴜsᴇ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ", "Allow this member to use the team ender chest.", target.canUseEnderChest()));
        inventory.setStack(41, permissionItem(Items.GRASS_BLOCK, "sᴇᴛ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "Allow this member to set the team home location.", target.canSetHome()));
        inventory.setStack(42, permissionItem(Items.ALLAY_SPAWN_EGG, "ɪɴᴠɪᴛᴇ ᴘʟᴀʏᴇʀs", "Allow this member to invite players to the team.", target.canInvite()));
        inventory.setStack(43, permissionItem(Items.ENDER_PEARL, "ᴜsᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "Allow this member to teleport to the team home.", target.canUseHome()));
        inventory.setStack(49, backItem());
        menu.sendContentUpdates();
    }

    private static ItemStack bankPermissionItem(boolean withdrawEnabled, boolean autoBankPermission, boolean autoBankEnabled) {
        ItemStack stack = new ItemStack(Items.GOLD_INGOT);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ ᴀɴᴅ ᴜsᴇ ᴀᴜᴛᴏʙᴀɴᴋ").setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Allow this member to withdraw from the team bank.", Formatting.GRAY),
                composeLine("Bank Withdraw: ", withdrawEnabled ? "ENABLED" : "DISABLED", Formatting.GRAY, withdrawEnabled ? Formatting.GREEN : Formatting.RED),
                plainLine("Allow this member to use team AutoBank.", Formatting.GRAY),
                composeLine("AutoBank Permission: ", autoBankPermission ? "ENABLED" : "DISABLED", Formatting.GRAY, autoBankPermission ? Formatting.GREEN : Formatting.RED),
                composeLine("AutoBank: ", autoBankEnabled ? "ON" : "OFF", Formatting.GRAY, autoBankEnabled ? Formatting.GREEN : Formatting.RED))));
        return stack;
    }

    private static void renderTransferConfirmation(TeamMenuHandler menu, PlayerEntity viewer, Team team, TeamPlayer target) {
        Inventory inventory = menu.getMenuInventory(); clear(inventory);
        inventory.setStack(4, actionItem(Items.BEACON, "ᴛʀᴀɴsғᴇʀ ᴏᴡɴᴇʀsʜɪᴘ", List.of(plainLine("Transfer ownership to " + resolveName(viewer, target) + "?", Formatting.WHITE), plainLine("This cannot be undone without another transfer.", Formatting.RED))));
        inventory.setStack(21, actionItem(Items.GREEN_WOOL, "ᴄᴏɴғɪʀᴍ", List.of(plainLine("Transfer ownership.", Formatting.YELLOW))));
        inventory.setStack(23, actionItem(Items.RED_WOOL, "ᴄᴀɴᴄᴇʟ", List.of(plainLine("Return to member management.", Formatting.YELLOW))));
        inventory.setStack(49, backItem()); menu.sendContentUpdates();
    }

    private static void applyTransfer(TeamMenuHandler menu, PlayerEntity player, Team team, TeamPlayer target) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !team.isOwner(player.getUuid()) || target.getPlayerUuid().equals(player.getUuid())) return;
        TeamPlayer oldOwner = team.getMember(player.getUuid()); if (oldOwner == null) return;
        team.setOwnerUuid(target.getPlayerUuid());
        target.setRank(TeamRank.LEADER); setOwnerPermissions(target);
        oldOwner.setRank(TeamRank.CO_LEADER); setMemberPermissions(oldOwner);
        save(); JustTeamsFabric.glow().refreshAll(serverPlayer.getEntityWorld().getServer());
        player.sendMessage(Text.literal("You transferred ownership of " + team.getName() + " to " + resolveName(player, target) + "."), false);
        ServerPlayerEntity onlineTarget = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(target.getPlayerUuid());
        if (onlineTarget != null) onlineTarget.sendMessage(Text.literal("You are now the owner of " + team.getName() + "."), false);
        for (TeamPlayer member : team.getMembers()) { ServerPlayerEntity memberPlayer = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(member.getPlayerUuid()); if (memberPlayer != null && !memberPlayer.getUuid().equals(player.getUuid()) && !memberPlayer.getUuid().equals(target.getPlayerUuid())) memberPlayer.sendMessage(Text.literal(resolveName(player, target) + " is now the owner of the team."), false); }
        TARGETS.remove(menu); MAIN_SLOTS.remove(menu); TRANSFER_CONFIRM.remove(menu); TeamInPlaceGui.returnToMain(menu); TeamInPlaceGui.refreshMainMembers(menu, player, team);
    }

    private static void setOwnerPermissions(TeamPlayer member) {
        member.setRole(TeamRole.OWNER); member.setCanWithdraw(true); member.setCanUseEnderChest(true); member.setCanSetHome(true); member.setCanUseHome(true);
        member.setCanEditMembers(true); member.setCanEditCoOwners(true); member.setCanKickMembers(true); member.setCanPromoteMembers(true); member.setCanDemoteMembers(true); member.setCanInvite(true); member.setCanSetWarps(true); member.setCanUseAutoBank(true);
    }
    private static void setMemberPermissions(TeamPlayer member) {
        member.setRole(TeamRole.CO_OWNER); member.setCanWithdraw(false); member.setCanUseEnderChest(true); member.setCanSetHome(false); member.setCanUseHome(true);
        member.setCanEditMembers(false); member.setCanEditCoOwners(false); member.setCanKickMembers(false); member.setCanPromoteMembers(false); member.setCanDemoteMembers(false);
    }
    private static void notifyRankChange(ServerPlayerEntity actor, Team team, TeamPlayer target, boolean promoted) {
        var server = actor.getEntityWorld().getServer(); ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(target.getPlayerUuid()); String name = PlayerNameResolver.resolve(server, target.getPlayerUuid());
        String text = (promoted ? "promoted to " : "demoted to ") + target.getRank().getDisplayName(); actor.sendMessage(Text.literal("You " + text + ": " + name + "."), false);
        if (targetPlayer != null) targetPlayer.sendMessage(Text.literal("You were " + text + " in " + team.getName() + "."), false);
    }

    private static void snapshot(TeamMenuHandler menu) { if (MAIN_SNAPSHOTS.containsKey(menu)) return; ItemStack[] snapshot = new ItemStack[54]; for (int slot = 0; slot < 54; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy(); MAIN_SNAPSHOTS.put(menu, snapshot); }
    private static void restore(TeamMenuHandler menu) { ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu); if (snapshot == null) return; for (int slot = 0; slot < snapshot.length; slot++) menu.getMenuInventory().setStack(slot, snapshot[slot].copy()); MAIN_SNAPSHOTS.remove(menu); menu.sendContentUpdates(); }
    private static void clear(Inventory inventory) { ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "); for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY); for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy()); for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy()); }
    private static String resolveName(PlayerEntity viewer, TeamPlayer target) { if (viewer instanceof ServerPlayerEntity serverPlayer) return PlayerNameResolver.resolve(serverPlayer.getEntityWorld().getServer(), target.getPlayerUuid()); return "Unknown"; }
    private static String formatDate(TeamPlayer member) { return member.getJoinDate() == null ? "Unknown" : DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate()); }
    private static ItemStack permissionItem(Item item, String name, String description, boolean enabled) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(description, Formatting.GRAY), composeLine("Status: ", enabled ? "ENABLED" : "DISABLED", Formatting.GRAY, enabled ? Formatting.GREEN : Formatting.RED)))); return stack; }
    private static ItemStack lockedItem(Item item, String name, String description) { return permissionItem(item, name, description, false); }
    private static ItemStack actionItem(Item item, String name, List<Text> lore) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(lore)); return stack; }
    private static ItemStack backItem() { ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ"); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to return to the main menu.", Formatting.YELLOW)))); return stack; }
    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return plainLine(prefix, prefixColor).append(plainLine(value, valueColor)); }
    private static MutableText plainLine(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static ItemStack namedPlain(Item item, String name) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false))); return stack; }
    private static void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save in-place member change", e); } }
    private static String smallCaps(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (char c : value.toLowerCase().toCharArray()) {
            result.append(switch (c) {
                case 'a' -> 'ᴀ'; case 'b' -> 'ʙ'; case 'c' -> 'ᴄ'; case 'd' -> 'ᴅ'; case 'e' -> 'ᴇ'; case 'f' -> 'ғ';
                case 'g' -> 'ɢ'; case 'h' -> 'ʜ'; case 'i' -> 'ɪ'; case 'j' -> 'ᴊ'; case 'k' -> 'ᴋ'; case 'l' -> 'ʟ';
                case 'm' -> 'ᴍ'; case 'n' -> 'ɴ'; case 'o' -> 'ᴏ'; case 'p' -> 'ᴘ'; case 'q' -> 'ǫ'; case 'r' -> 'ʀ';
                case 's' -> 's'; case 't' -> 'ᴛ'; case 'u' -> 'ᴜ'; case 'v' -> 'ᴠ'; case 'w' -> 'ᴡ'; case 'x' -> 'x';
                case 'y' -> 'ʏ'; case 'z' -> 'ᴢ'; default -> c;
            });
        }
        return result.toString();
    }
}
