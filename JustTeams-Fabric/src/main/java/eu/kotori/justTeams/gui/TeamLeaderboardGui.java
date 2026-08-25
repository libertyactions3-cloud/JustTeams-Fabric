package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

/** Two-stage team leaderboard UI matching the supplied 2.5.3 behavior. */
public final class TeamLeaderboardGui {
    public enum Type { KILLS, BALANCE, MEMBERS }

    private TeamLeaderboardGui() {}

    public static void openCategories(PlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new CategoryHandler(syncId, inventory, player),
                Text.literal("ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ")));
    }

    public static void openLeaderboard(PlayerEntity player, Type type) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new LeaderboardHandler(syncId, inventory, player, type),
                Text.literal("ᴛᴏᴘ " + type.name().toLowerCase())));
    }

    private static void fill(Inventory inventory, int size) {
        ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inventory.setStack(i, filler.copy());
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }

    private static void addPlayerInventory(ScreenHandler handler, PlayerInventory inventory, int yStart, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ((BaseHandler) handler).addPlayerInventorySlot(
                        new Slot(inventory, col + row * 9 + 9, 8 + col * 18, yStart + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            ((BaseHandler) handler).addPlayerInventorySlot(new Slot(inventory, col, 8 + col * 18, hotbarY));
        }
    }

    private static abstract class BaseHandler extends ScreenHandler {
        protected final PlayerEntity viewer;
        protected final Inventory menu;
        private final int menuSize;

        BaseHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory inventory, PlayerEntity viewer,
                    int menuSize, int yStart, int hotbarY) {
            super(type, syncId);
            this.viewer = viewer;
            this.menu = new SimpleInventory(menuSize);
            this.menuSize = menuSize;
            for (int i = 0; i < menuSize; i++) {
                int columns = 9;
                addSlot(new MenuSlot(menu, i, 8 + (i % columns) * 18, 18 + (i / columns) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addPlayerInventorySlot(new Slot(inventory, col + row * 9 + 9,
                            8 + col * 18, yStart + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addPlayerInventorySlot(new Slot(inventory, col, 8 + col * 18, hotbarY));
            }
        }

        private void addPlayerInventorySlot(Slot slot) {
            addSlot(slot);
        }

        protected boolean isMenuSlot(int slot) { return slot >= 0 && slot < menuSize; }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()); }
    }

    private static final class CategoryHandler extends BaseHandler {
        CategoryHandler(int syncId, PlayerInventory inventory, PlayerEntity viewer) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, viewer, 27, 84, 142);
            fill(menu, 27);
            menu.setStack(11, named(Items.DIAMOND_SWORD, "ᴛᴏᴘ ᴋɪʟʟs"));
            menu.setStack(13, named(Items.EMERALD, "ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇ"));
            menu.setStack(15, named(Items.PLAYER_HEAD, "ᴛᴏᴘ ᴍᴇᴍʙᴇʀs"));
            menu.setStack(22, named(Items.BARRIER, "ʙᴀᴄᴋ"));
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (!isMenuSlot(slot) || actionType == SlotActionType.QUICK_MOVE
                    || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW
                    || actionType == SlotActionType.CLONE) return;
            if (slot == 11) {
                TeamLeaderboardGui.openLeaderboard(player, Type.KILLS);
            } else if (slot == 13) {
                TeamLeaderboardGui.openLeaderboard(player, Type.BALANCE);
            } else if (slot == 15) {
                TeamLeaderboardGui.openLeaderboard(player, Type.MEMBERS);
            } else if (slot == 22 && player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
                TeamGuiManager.openMain(player);
            }
        }
    }

    private static final class LeaderboardHandler extends BaseHandler {
        private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        private final Type type;

        LeaderboardHandler(int syncId, PlayerInventory inventory, PlayerEntity viewer, Type type) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, inventory, viewer, 54, 140, 198);
            this.type = type;
            populate();
        }

        private void populate() {
            fill(menu, 54);
            List<Team> teams = JustTeamsFabric.teams().getTeams().stream()
                    .sorted(comparator())
                    .limit(SLOTS.length)
                    .toList();

            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                int rank = i + 1;
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(team.getOwnerUuid()));
                head.set(DataComponentTypes.CUSTOM_NAME, Text.literal("#" + rank + " " + team.getName()));
                String value = switch (type) {
                    case KILLS -> "Kills: " + team.getKills();
                    case BALANCE -> "Balance: " + String.format("%.2f", team.getBalance());
                    case MEMBERS -> "Members: " + team.getMembers().size();
                };
                head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        Text.literal("Tag: " + team.getTag()),
                        Text.literal(value))));
                menu.setStack(SLOTS[i], head);
            }
            menu.setStack(49, named(Items.ARROW, "ʙᴀᴄᴋ"));
        }

        private Comparator<Team> comparator() {
            Comparator<Team> comparator = switch (type) {
                case KILLS -> Comparator.comparingInt(Team::getKills).reversed();
                case BALANCE -> Comparator.comparingDouble(Team::getBalance).reversed();
                case MEMBERS -> Comparator.comparingInt((Team team) -> team.getMembers().size()).reversed();
            };
            return comparator.thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (!isMenuSlot(slot) || actionType == SlotActionType.QUICK_MOVE
                    || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW
                    || actionType == SlotActionType.CLONE) return;
            if (slot == 49) {
                TeamLeaderboardGui.openCategories(player);
            }
        }
    }

    private static final class MenuSlot extends Slot {
        MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { return false; }
    }
}
