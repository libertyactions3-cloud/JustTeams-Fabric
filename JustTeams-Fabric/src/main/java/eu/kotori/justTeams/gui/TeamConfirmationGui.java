package eu.kotori.justTeams.gui;

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
import net.minecraft.text.Text;

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
                Text.literal(title)));
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

            fill(title, question);
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

        private void fill(String title, String question) {
            for (int i = 0; i < 27; i++) {
                menu.setStack(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            }
            menu.setStack(4, named(Items.PAPER, question));
            menu.setStack(11, named(Items.LIME_DYE, "Confirm"));
            menu.setStack(15, named(Items.RED_DYE, "Cancel"));
            menu.setStack(22, named(Items.BARRIER, title));
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
            if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP
                    || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE) {
                return;
            }
            if (slot == 11) {
                complete(confirm);
            } else if (slot == 15 || slot == 22) {
                complete(cancel);
            }
        }

        private void complete(Runnable action) {
            if (completed) return;
            completed = true;
            if (viewer instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
                serverPlayer.getServer().execute(action);
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
                    serverPlayer.getServer().execute(cancel);
                } else {
                    cancel.run();
                }
            }
        }

        private static ItemStack named(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
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
