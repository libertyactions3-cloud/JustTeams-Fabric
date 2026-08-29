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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;

/** Two-stage team leaderboard UI matching the supplied 2.5.3 behavior. */
public final class TeamLeaderboardGui {
    public enum Type { KILLS, BALANCE, MEMBERS }

    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    private TeamLeaderboardGui() {}

    public static void openCategories(PlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new CategoryHandler(syncId, inventory, player),
                Text.literal("ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ").setStyle(Style.EMPTY.withItalic(false))));
    }

    public static void openLeaderboard(PlayerEntity player, Type type) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new LeaderboardHandler(syncId, inventory, player, type),
                Text.literal("ᴛᴏᴘ " + type.name().toLowerCase()).setStyle(Style.EMPTY.withItalic(false))));
    }

    private static void openOnServerThread(ServerPlayerEntity player, Runnable open) {
        if (player.getEntityWorld().getServer() != null) {
            player.getEntityWorld().getServer().execute(open);
        } else {
            open.run();
        }
    }

    private static void closeThenOpen(ServerPlayerEntity player, Runnable open) {
        player.closeHandledScreen();
        openOnServerThread(player, open);
    }

    private static void fill(Inventory inventory, int size) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inventory.setStack(i, filler.copy());
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
            menu.setStack(11, categoryItem(Items.NETHERITE_SWORD, "ᴛᴏᴘ ᴋɪʟʟs",
                    "Shows the top 10 teams with the most kills."));
            menu.setStack(13, categoryItem(Items.DIAMOND, "ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇ",
                    "Shows the top 10 richest teams."));
            menu.setStack(15, categoryItem(Items.PLAYER_HEAD, "ᴛᴏᴘ ᴍᴇᴍʙᴇʀs",
                    "Shows the top 10 teams with the most members."));
            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to your team.", Formatting.YELLOW))));
            menu.setStack(22, back);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (!isMenuSlot(slot) || actionType == SlotActionType.QUICK_MOVE
                    || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW
                    || actionType == SlotActionType.CLONE) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            if (slot == 11) {
                closeThenOpen(serverPlayer, () -> TeamLeaderboardGui.openLeaderboard(serverPlayer, Type.KILLS));
            } else if (slot == 13) {
                closeThenOpen(serverPlayer, () -> TeamLeaderboardGui.openLeaderboard(serverPlayer, Type.BALANCE));
            } else if (slot == 15) {
                closeThenOpen(serverPlayer, () -> TeamLeaderboardGui.openLeaderboard(serverPlayer, Type.MEMBERS));
            } else if (slot == 22) {
                closeThenOpen(serverPlayer, () -> TeamGuiManager.openMain(serverPlayer));
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
                head.set(DataComponentTypes.CUSTOM_NAME,
                        gradientText("#" + rank + " " + team.getName(), true));
                String statisticName;
                String statisticValue;
                switch (type) {
                    case KILLS -> {
                        statisticName = "Kills";
                        statisticValue = String.valueOf(team.getKills());
                    }
                    case BALANCE -> {
                        statisticName = "Balance";
                        statisticValue = String.format("%.2f", team.getBalance());
                    }
                    case MEMBERS -> {
                        statisticName = "Members";
                        statisticValue = String.valueOf(team.getMembers().size());
                    }
                    default -> throw new IllegalStateException("Unexpected leaderboard type: " + type);
                }
                head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                        composeLine(statisticName + ": ", statisticValue, Formatting.GRAY, Formatting.WHITE)
                )));
                menu.setStack(SLOTS[i], head);
            }

            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to category selection.", Formatting.YELLOW))));
            menu.setStack(49, back);
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
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            if (slot == 49) {
                closeThenOpen(serverPlayer, () -> TeamLeaderboardGui.openCategories(serverPlayer));
            }
        }
    }

    private static ItemStack categoryItem(Item item, String name, String loreText) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name, true));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine(loreText, Formatting.GRAY))));
        return stack;
    }

    private static ItemStack namedPlain(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
        return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
    }

    private static MutableText plainLine(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static MutableText gradientText(String value, boolean bold) {
        MutableText result = Text.empty();
        if (value.isEmpty()) return result;
        int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
        int index = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            double t = (double) index / length;
            int sr = (PRIMARY_START >> 16) & 0xFF, sg = (PRIMARY_START >> 8) & 0xFF, sb = PRIMARY_START & 0xFF;
            int er = (PRIMARY_END >> 16) & 0xFF, eg = (PRIMARY_END >> 8) & 0xFF, eb = PRIMARY_END & 0xFF;
            int r = (int) Math.round(sr + (er - sr) * t);
            int g = (int) Math.round(sg + (eg - sg) * t);
            int b = (int) Math.round(sb + (eb - sb) * t);
            result.append(Text.literal(new String(Character.toChars(codePoint)))
                    .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(bold).withItalic(false)));
            offset += Character.charCount(codePoint);
            index++;
        }
        return result;
    }

    private static final class MenuSlot extends Slot {
        MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { return false; }
    }
}
