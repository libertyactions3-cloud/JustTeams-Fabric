package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.economy.TeamBankLogManager;
import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/** Persistent 54-slot team-bank audit-log view and one-week AutoBank top-spender view. */
public final class TeamBankLogsGui {
    private static final int[] LOG_SLOTS = {
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> SNAPSHOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Boolean> TOP_VIEW = new WeakHashMap<>();

    private TeamBankLogsGui() {}

    public static void open(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        snapshot(menu);
        TOP_VIEW.put(menu, false);
        renderLogs(menu, player, team);
    }

    public static boolean isOpen(TeamMenuHandler menu) { return SNAPSHOTS.containsKey(menu); }

    public static boolean handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        if (!isOpen(menu)) return false;
        if (slot == 49) {
            boolean top = !Boolean.TRUE.equals(TOP_VIEW.get(menu));
            TOP_VIEW.put(menu, top);
            if (top) renderTop(menu, player, team); else renderLogs(menu, player, team);
            return true;
        }
        if (slot == 53) { close(menu); return true; }
        return true;
    }

    public static void close(TeamMenuHandler menu) {
        ItemStack[] snapshot = SNAPSHOTS.remove(menu);
        TOP_VIEW.remove(menu);
        if (snapshot == null) return;
        Inventory inventory = menu.getMenuInventory();
        for (int slot = 0; slot < snapshot.length; slot++) inventory.setStack(slot, snapshot[slot].copy());
        menu.sendContentUpdates();
    }

    private static void renderLogs(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        inventory.setStack(4, named(Items.BOOK, "ʙᴀɴᴋ ʟᴏɢs", Formatting.AQUA, true));

        List<TeamBankLogManager.Entry> entries = new ArrayList<>(TeamBankLogManager.recent(team));
        entries.sort((a, b) -> Long.compare(b.timestampMillis(), a.timestampMillis()));
        for (int i = 0; i < LOG_SLOTS.length && i < entries.size(); i++) {
            TeamBankLogManager.Entry entry = entries.get(i);
            ItemStack log = named(entry.kind() == TeamBankLogManager.Kind.AUTOBANK ? Items.EMERALD : Items.GOLD_INGOT,
                    entry.playerName(), Formatting.WHITE, true);
            List<Text> lore = new ArrayList<>();
            lore.add(line(TeamBankLogManager.formatTimestamp(entry.timestampMillis()), Formatting.GRAY));
            lore.add(compose("Amount: ", entry.amount() + " total emeralds", Formatting.GRAY, Formatting.GREEN));
            lore.add(compose("Type: ", entry.kind() == TeamBankLogManager.Kind.AUTOBANK ? "AutoBank" : "Manual withdrawal", Formatting.GRAY, Formatting.WHITE));
            lore.add(compose("Action: ", entry.action(), Formatting.GRAY, Formatting.WHITE));
            lore.add(compose("UUID: ", entry.playerUuid().toString(), Formatting.DARK_GRAY, Formatting.DARK_GRAY));
            log.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inventory.setStack(LOG_SLOTS[i], log);
        }

        if (entries.isEmpty()) {
            ItemStack empty = named(Items.PAPER, "ɴᴏ ʙᴀɴᴋ ʟᴏɢs", Formatting.GRAY, true);
            empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    line("No team-bank withdrawals were recorded in the last 7 days.", Formatting.GRAY))));
            inventory.setStack(22, empty);
        }

        TeamBankLogManager.TopSpender top = TeamBankLogManager.topAutoBankSpender(team);
        ItemStack topButton = named(Items.EMERALD_BLOCK, "ᴛᴏᴘ ᴀᴜᴛᴏʙᴀɴᴋ sᴘᴇɴᴅᴇʀ", Formatting.AQUA, true);
        topButton.set(DataComponentTypes.LORE, new LoreComponent(top == null
                ? List.of(line("No AutoBank withdrawals were recorded in the last 7 days.", Formatting.GRAY), line("", Formatting.GRAY), line("Click to view the top spender.", Formatting.YELLOW))
                : List.of(compose("Top: ", top.playerName(), Formatting.GRAY, Formatting.WHITE), compose("Withdrawn: ", top.amount() + " total emeralds", Formatting.GRAY, Formatting.GREEN), line("", Formatting.GRAY), line("Click to view the top spender.", Formatting.YELLOW))));
        inventory.setStack(49, topButton);
        inventory.setStack(53, back());
        menu.sendContentUpdates();
    }

    private static void renderTop(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        inventory.setStack(4, named(Items.EMERALD_BLOCK, "ᴛᴏᴘ ᴀᴜᴛᴏʙᴀɴᴋ sᴘᴇɴᴅᴇʀ", Formatting.AQUA, true));
        TeamBankLogManager.TopSpender top = TeamBankLogManager.topAutoBankSpender(team);
        if (top == null) {
            ItemStack empty = named(Items.PAPER, "ɴᴏ ᴛᴏᴘ sᴘᴇɴᴅᴇʀ", Formatting.GRAY, true);
            empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(line("No AutoBank withdrawals were recorded in the last 7 days.", Formatting.GRAY))));
            inventory.setStack(22, empty);
        } else {
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            playerHead.set(DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofDynamic(top.playerUuid()));
            playerHead.set(DataComponentTypes.CUSTOM_NAME, Text.literal(top.playerName()).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true).withItalic(false)));
            playerHead.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    compose("Withdrawn: ", top.amount() + " total emeralds", Formatting.GRAY, Formatting.GREEN),
                    line("Calculated from the last 7 days of AutoBank logs.", Formatting.GRAY))));
            inventory.setStack(22, playerHead);
        }
        ItemStack toggle = named(Items.BOOK, "ʙᴀᴄᴋ ᴛᴏ ʟᴏɢs", Formatting.AQUA, true);
        toggle.set(DataComponentTypes.LORE, new LoreComponent(List.of(line("Return to the bank logs.", Formatting.YELLOW))));
        inventory.setStack(49, toggle);
        inventory.setStack(53, back());
        menu.sendContentUpdates();
    }

    private static void snapshot(TeamMenuHandler menu) {
        if (SNAPSHOTS.containsKey(menu)) return;
        ItemStack[] snapshot = new ItemStack[54];
        for (int slot = 0; slot < snapshot.length; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy();
        SNAPSHOTS.put(menu, snapshot);
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" ").setStyle(Style.EMPTY.withItalic(false)));
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static ItemStack named(net.minecraft.item.Item item, String name, Formatting color, boolean bold) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(false)));
        return stack;
    }
    private static ItemStack back() { return named(Items.ARROW, "ʙᴀᴄᴋ", Formatting.GRAY, true); }
    private static MutableText line(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static MutableText compose(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return line(prefix, prefixColor).append(line(value, valueColor)); }
}
