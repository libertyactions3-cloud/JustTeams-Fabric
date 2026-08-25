package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamNotificationManager;
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
import java.util.List;
import java.util.WeakHashMap;

/** In-place member management view for the persistent team inventory. */
public final class TeamInPlaceMemberGui {
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> MAIN_SNAPSHOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, TeamPlayer> TARGETS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Integer> MAIN_SLOTS = new WeakHashMap<>();
    private static final int VIEW_OFFSET = 18;
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    private TeamInPlaceMemberGui() {}

    public static boolean isOpen(TeamMenuHandler menu) { return TARGETS.containsKey(menu); }

    public static void enter(TeamMenuHandler menu, PlayerEntity viewer, Team team, TeamPlayer target, int mainSlot) {
        snapshot(menu);
        TARGETS.put(menu, target);
        MAIN_SLOTS.put(menu, mainSlot);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(target.getPlayerUuid()));
        String playerName = resolveName(viewer, target);
        head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(playerName).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Role: ", roleName(target.getRole()), Formatting.GRAY, Formatting.WHITE),
                composeLine("Joined: ", formatDate(target), Formatting.GRAY, Formatting.WHITE)
        )));
        inventory.setStack(VIEW_OFFSET + 4, head);

        inventory.setStack(VIEW_OFFSET + 10, roleItem(target.getRole()));
        inventory.setStack(VIEW_OFFSET + 11, actionItem(Items.LIME_DYE, "PROMOTE TO CO-OWNER", List.of(
                plainLine("Gives this player more permissions.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to promote.", Formatting.YELLOW))));
        inventory.setStack(VIEW_OFFSET + 12, actionItem(Items.GRAY_DYE, "DEMOTE TO MEMBER", List.of(
                plainLine("Removes co-owner permissions.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to demote.", Formatting.YELLOW))));
        inventory.setStack(VIEW_OFFSET + 14, actionItem(Items.RED_WOOL, "KICK MEMBER", List.of(
                plainLine("Removes this player from the team.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to kick", Formatting.YELLOW))));
        inventory.setStack(VIEW_OFFSET + 16, permissionItem(Items.GOLD_INGOT, "ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ", "Allow this member to withdraw from the team bank.", target.canWithdraw()));
        inventory.setStack(VIEW_OFFSET + 17, permissionItem(Items.ENDER_CHEST, "ᴜsᴇ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ", "Allow this member to use the team ender chest.", target.canUseEnderChest()));
        inventory.setStack(VIEW_OFFSET + 18, permissionItem(Items.GRASS_BLOCK, "sᴇᴛ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "Allow this member to set the team home location.", target.canSetHome()));
        inventory.setStack(VIEW_OFFSET + 19, permissionItem(Items.ENDER_PEARL, "ᴜsᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "Allow this member to teleport to the team home.", target.canUseHome()));
        inventory.setStack(VIEW_OFFSET + 20, permissionItem(Items.IRON_SWORD, "ᴋɪᴄᴋ ᴍᴇᴍʙᴇʀs", "Allow this member to manage and remove team members.", target.canKickMembers()));
        inventory.setStack(VIEW_OFFSET + 22, backItem());
        menu.sendContentUpdates();
    }

    public static void back(TeamMenuHandler menu) {
        restore(menu); TARGETS.remove(menu); MAIN_SLOTS.remove(menu);
    }

    public static boolean handle(TeamMenuHandler menu, PlayerEntity player, Team team, int slot) {
        TeamPlayer target = TARGETS.get(menu);
        if (target == null) return false;
        if (slot == VIEW_OFFSET + 22) { back(menu); return true; }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (!team.isOwner(player.getUuid()) || target.getPlayerUuid().equals(player.getUuid())) return true;
        switch (slot - VIEW_OFFSET) {
            case 11 -> { if (target.getRole() == TeamRole.MEMBER) target.setRole(TeamRole.CO_OWNER); }
            case 12 -> { if (target.getRole() == TeamRole.CO_OWNER) target.setRole(TeamRole.MEMBER); }
            case 14 -> {
                TeamChatManager.disable(target.getPlayerUuid());
                TeamEnderChestGui.closeViewer(serverPlayer.getEntityWorld().getServer(), team, target.getPlayerUuid());
                JustTeamsFabric.glow().stopGlowForPlayer(serverPlayer.getEntityWorld().getServer(), target.getPlayerUuid());
                JustTeamsFabric.teams().removeMember(team, target.getPlayerUuid());
                save();
                TeamNotificationManager.notifyKick(serverPlayer.getEntityWorld().getServer(), team, player.getUuid(), target.getPlayerUuid());
                ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu);
                Integer mainSlot = MAIN_SLOTS.get(menu);
                if (snapshot != null && mainSlot != null) snapshot[mainSlot] = ItemStack.EMPTY.copy();
                back(menu);
                return true;
            }
            case 16 -> target.setCanWithdraw(!target.canWithdraw());
            case 17 -> target.setCanUseEnderChest(!target.canUseEnderChest());
            case 18 -> target.setCanSetHome(!target.canSetHome());
            case 19 -> target.setCanUseHome(!target.canUseHome());
            case 20 -> target.setCanKickMembers(!target.canKickMembers());
            default -> { return true; }
        }
        save(); enter(menu, player, team, target, MAIN_SLOTS.getOrDefault(menu, 0)); return true;
    }

    private static void snapshot(TeamMenuHandler menu) {
        if (MAIN_SNAPSHOTS.containsKey(menu)) return;
        ItemStack[] snapshot = new ItemStack[54];
        for (int slot = 0; slot < 54; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy();
        MAIN_SNAPSHOTS.put(menu, snapshot);
    }

    private static void restore(TeamMenuHandler menu) {
        ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu);
        if (snapshot == null) return;
        for (int slot = 0; slot < snapshot.length; slot++) menu.getMenuInventory().setStack(slot, snapshot[slot].copy());
        MAIN_SNAPSHOTS.remove(menu);
        menu.sendContentUpdates();
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static String resolveName(PlayerEntity viewer, TeamPlayer target) {
        if (viewer instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayerEntity online = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(target.getPlayerUuid());
            if (online != null) return online.getName().getString();
        }
        return target.getPlayerUuid().toString().substring(0, 8);
    }
    private static String roleName(TeamRole role) { return switch (role) { case OWNER -> "Owner"; case CO_OWNER -> "Co-Owner"; case MEMBER -> "Member"; }; }
    private static String formatDate(TeamPlayer member) { return member.getJoinDate() == null ? "Unknown" : DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate()); }
    private static ItemStack roleItem(TeamRole role) { return actionItem(Items.GOLDEN_HELMET, "Role: " + roleName(role), List.of(plainLine("Current role: " + roleName(role), Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Member management permissions are controlled below.", Formatting.YELLOW))); }
    private static ItemStack permissionItem(Item item, String name, String description, boolean enabled) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(description, Formatting.GRAY), composeLine("Status: ", enabled ? "ENABLED" : "DISABLED", Formatting.GRAY, enabled ? Formatting.GREEN : Formatting.RED)))); return stack; }
    private static ItemStack actionItem(Item item, String name, List<Text> lore) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(lore)); return stack; }
    private static ItemStack backItem() { ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ"); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false))); stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to return to the main menu.", Formatting.YELLOW)))); return stack; }
    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return plainLine(prefix, prefixColor).append(plainLine(value, valueColor)); }
    private static MutableText plainLine(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static ItemStack namedPlain(Item item, String name) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false))); return stack; }
    private static void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save in-place member change", e); } }
}
