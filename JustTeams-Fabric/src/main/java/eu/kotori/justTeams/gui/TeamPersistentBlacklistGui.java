package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** Blacklist view rendered inside the persistent 54-slot team handler. */
public final class TeamPersistentBlacklistGui {
    private static final int[] PLAYER_SLOTS = {
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC);
    private static final WeakHashMap<TeamMenuHandler, Boolean> OPEN = new WeakHashMap<>();

    private TeamPersistentBlacklistGui() {}

    public static boolean isOpen(TeamMenuHandler menu) {
        return OPEN.getOrDefault(menu, false);
    }

    public static void open(TeamMenuHandler menu, PlayerEntity player, Team team) {
        OPEN.put(menu, true);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        inventory.setStack(4, headerItem());

        List<BlacklistedPlayer> entries = new ArrayList<>(team.getBlacklist());
        for (int i = 0; i < PLAYER_SLOTS.length && i < entries.size(); i++) {
            inventory.setStack(PLAYER_SLOTS[i], playerItem(entries.get(i)));
        }

        inventory.setStack(49, backItem());
        menu.sendContentUpdates();
    }

    public static void close(TeamMenuHandler menu) {
        OPEN.remove(menu);
    }

    public static void handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        if (!team.hasElevatedPermissions(player.getUuid())) return;
        if (slot == 49) {
            OPEN.remove(menu);
            TeamInPlaceGui.returnToMain(menu);
            return;
        }

        int index = indexOf(slot);
        List<BlacklistedPlayer> entries = new ArrayList<>(team.getBlacklist());
        if (index < 0 || index >= entries.size()) return;
        BlacklistedPlayer entry = entries.get(index);
        if (!team.removeBlacklistEntry(entry.getPlayerUuid())) return;
        save();
        player.sendMessage(Text.literal("Removed " + entry.getPlayerName() + " from the team blacklist."), false);
        open(menu, player, team);
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static ItemStack headerItem() {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Players who cannot join this team", Formatting.GRAY),
                plainLine("Click on a player head to remove them", Formatting.GRAY)
        )));
        return stack;
    }

    private static ItemStack playerItem(BlacklistedPlayer entry) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(entry.getPlayerUuid()));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(entry.getPlayerName()).setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Blacklisted by: ", entry.getBlacklistedByName(), Formatting.GRAY, Formatting.WHITE),
                composeLine("Date: ", formatDate(entry.getBlacklistedAt()), Formatting.GRAY, Formatting.WHITE),
                plainLine("", Formatting.GRAY),
                plainLine("Click to remove from blacklist", Formatting.YELLOW)
        )));
        return stack;
    }

    private static ItemStack backItem() {
        ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to return to the main menu.", Formatting.YELLOW)
        )));
        return stack;
    }

    private static String formatDate(Instant instant) {
        return instant == null ? "Unknown" : DATE_FORMAT.format(instant);
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < PLAYER_SLOTS.length; i++) if (PLAYER_SLOTS[i] == slot) return i;
        return -1;
    }

    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
        return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
    }

    private static MutableText plainLine(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static void save() {
        try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
        catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save persistent team blacklist change", exception); }
    }
}
