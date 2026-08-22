package eu.kotori.justTeams.gui;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Consumer;

/**
 * Server-only text input using the vanilla anvil text field.
 * The client needs no JustTeams-specific screen code.
 */
public final class TeamStringInputGui {
    private TeamStringInputGui() {}

    public static void open(PlayerEntity player, String title, String prompt,
                            Consumer<String> result, Runnable cancelled) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, prompt, result, cancelled),
                Text.literal(title)
        ));
    }

    private static final class Handler extends AnvilScreenHandler {
        private final Consumer<String> result;
        private final Runnable cancelled;
        private boolean completed;

        private Handler(int syncId, PlayerInventory inventory, String prompt,
                        Consumer<String> result, Runnable cancelled) {
            super(syncId, inventory);
            this.result = result;
            this.cancelled = cancelled;
            ItemStack input = new ItemStack(Items.PAPER);
            input.set(DataComponentTypes.CUSTOM_NAME, Text.literal(prompt));
            this.input.setStack(0, input);
            this.updateResult();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player instanceof ServerPlayerEntity;
        }

        @Override
        protected boolean canTakeOutput(PlayerEntity player, boolean present) {
            return true;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            if (slotIndex == 2 && actionType == SlotActionType.PICKUP) {
                String value = outputValue();
                if (!value.isBlank()) {
                    completed = true;
                    serverPlayer.closeHandledScreen();
                    result.accept(value);
                }
                return;
            }
            if (slotIndex == 0 || slotIndex == 1) return;
            super.onSlotClick(slotIndex, button, actionType, player);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            if (slot == 2 && player instanceof ServerPlayerEntity serverPlayer) {
                String value = outputValue();
                if (!value.isBlank()) {
                    completed = true;
                    serverPlayer.closeHandledScreen();
                    result.accept(value);
                }
            }
            return ItemStack.EMPTY;
        }

        private String outputValue() {
            ItemStack output = slots.get(2).getStack();
            Text customName = output.get(DataComponentTypes.CUSTOM_NAME);
            return customName == null ? "" : customName.getString().trim();
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);
            if (!completed && player instanceof ServerPlayerEntity) cancelled.run();
        }
    }
}
