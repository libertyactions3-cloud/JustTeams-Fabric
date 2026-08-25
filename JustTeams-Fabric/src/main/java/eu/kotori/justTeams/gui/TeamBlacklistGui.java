package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
import java.util.UUID;

/** Server-side 54-slot GUI for viewing and removing team blacklist entries. */
public final class TeamBlacklistGui {
    private static final int[] PLAYER_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC);

    private TeamBlacklistGui() {}

    public static void open(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) {
            player.sendMessage(Text.literal("You are not in a team."), true);
            return;
        }
        if (!team.hasElevatedPermissions(player.getUuid())) {
            player.sendMessage(Text.literal("Only the owner or co-owner can manage the team blacklist."), true);
            return;
        }
        if (!(player.currentScreenHandler instanceof TeamMenuHandler)) {
            TeamGuiManager.openMain(serverPlayer);
        }
        if (player.currentScreenHandler instanceof TeamMenuHandler menu) {
            TeamPersistentBlacklistGui.open(menu, serverPlayer, team);
            return;
        }
        serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, serverPlayer, team),
                Text.literal("ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ").setStyle(Style.EMPTY.withItalic(false))));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(54);
        private final ServerPlayerEntity viewer;
        private final Team team;
        private final List<BlacklistedPlayer> entries = new ArrayList<>();

        Handler(int syncId, PlayerInventory inventory, ServerPlayerEntity viewer, Team team) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewer = viewer;
            this.team = team;
            populate();
            for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
                addSlot(new MenuSlot(menu, row * 9 + col, 8 + col * 18, 18 + row * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }

        private void populate() {
            for (int slot = 0; slot < 54; slot++) menu.setStack(slot, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));
            menu.setStack(4, headerItem());
            entries.clear();
            entries.addAll(team.getBlacklist());
            for (int i = 0; i < PLAYER_SLOTS.length && i < entries.size(); i++) menu.setStack(PLAYER_SLOTS[i], playerItem(entries.get(i)));
            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to return to the main menu.", Formatting.YELLOW))));
            menu.setStack(49, back);
        }

        private ItemStack headerItem() {
            ItemStack stack = new ItemStack(Items.BARRIER);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true).withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Players who cannot join this team", Formatting.GRAY),
                    plainLine("Click on a player head to remove them", Formatting.GRAY))));
            return stack;
        }

        private ItemStack playerItem(BlacklistedPlayer entry) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(entry.getPlayerUuid()));
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(entry.getPlayerName()).setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Blacklisted by: ", entry.getBlacklistedByName(), Formatting.GRAY, Formatting.WHITE),
                    composeLine("Date: ", formatDate(entry.getBlacklistedAt()), Formatting.GRAY, Formatting.WHITE),
                    plainLine("", Formatting.GRAY), plainLine("Click to remove from blacklist", Formatting.YELLOW))));
            return stack;
        }

        @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            if (!player.getUuid().equals(viewer.getUuid())) return;
            if (slot == 49) { TeamGuiManager.openMain(player); return; }
            int index = playerIndex(slot); if (index < 0 || index >= entries.size()) return;
            BlacklistedPlayer entry = entries.get(index);
            if (!team.removeBlacklistEntry(entry.getPlayerUuid())) return;
            try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
            catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save blacklist removal", exception); return; }
            player.sendMessage(Text.literal("Removed " + entry.getPlayerName() + " from the team blacklist."), false);
            populate(); sendContentUpdates();
        }

        private int playerIndex(int slot) { for (int i = 0; i < PLAYER_SLOTS.length; i++) if (PLAYER_SLOTS[i] == slot) return i; return -1; }
        private static String formatDate(Instant instant) { return instant == null ? "Unknown" : DATE_FORMAT.format(instant); }
        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()) && team.hasElevatedPermissions(player.getUuid()); }
        private static ItemStack namedPlain(net.minecraft.item.Item item, String name) { ItemStack stack = new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false))); return stack; }
        private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return plainLine(prefix, prefixColor).append(plainLine(value, valueColor)); }
        private static MutableText plainLine(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
        private static final class MenuSlot extends Slot { private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); } @Override public boolean canInsert(ItemStack stack) { return false; } @Override public boolean canTakeItems(PlayerEntity player) { return false; } }
    }
}
