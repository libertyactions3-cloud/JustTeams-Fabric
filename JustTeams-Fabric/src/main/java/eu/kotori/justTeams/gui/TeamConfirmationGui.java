package eu.kotori.justTeams.gui;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;

/** Small confirmation GUI used for destructive or ownership-changing team actions. */
public final class TeamConfirmationGui {
    private TeamConfirmationGui() {}

    public static void open(PlayerEntity player, String title, String question, Runnable confirm, Runnable cancel) {
        Objects.requireNonNull(confirm, "confirm");
        Objects.requireNonNull(cancel, "cancel");

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(
                        syncId, inventory, player, title, question, confirm, cancel),
                Text.literal(title).setStyle(Style.EMPTY.withItalic(false))));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final PlayerEntity viewer;
        private final Runnable confirm;
        private final Runnable cancel;
        private boolean completed;

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer, String title, String question,
                Runnable confirm, Runnable cancel) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.viewer = viewer;
            this.confirm = confirm;
            this.cancel = cancel;

            fill();
            for (int i = 0; i < 27; i++) {
                addSlot(new MenuSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, 8 + col * 18, 142));
            }
        }

        private void fill() {
            for (int i = 0; i < 27; i++) {
                menu.setStack(i, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));
            }

            ItemStack confirmItem = new ItemStack(Items.GREEN_WOOL);
            confirmItem.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("CONFIRM").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true).withItalic(false)));
            confirmItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("This action cannot be undone.", Formatting.GRAY)
            )));
            menu.setStack(11, confirmItem);

            ItemStack cancelItem = new ItemStack(Items.RED_WOOL);
            cancelItem.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("CANCEL").setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
            cancelItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Return to the previous menu.", Formatting.GRAY)
            )));
            menu.setStack(15, cancelItem);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP
                    || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE) {
                return;
            }
            if (slot == 11) {
                complete(confirm);
            } else if (slot == 15) {
                complete(cancel);
            }
        }

        private void complete(Runnable action) {
            if (completed) return;
            completed = true;
            if (viewer instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
                serverPlayer.getEntityWorld().getServer().execute(action);
            } else {
                action.run();
            }
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.getUuid().equals(viewer.getUuid());
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);
            if (!completed && player.getUuid().equals(viewer.getUuid())) {
                completed = true;
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.getEntityWorld().getServer().execute(cancel);
                } else {
                    cancel.run();
                }
            }
        }

        private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static Text plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static final class MenuSlot extends Slot {
            MenuSlot(Inventory inventory, int index, int x, int y) {
                super(inventory, index, x, y);
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity player) {
                return false;
            }
        }
    }
}
