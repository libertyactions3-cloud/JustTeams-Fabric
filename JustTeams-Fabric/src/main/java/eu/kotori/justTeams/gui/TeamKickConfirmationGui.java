package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.PlayerNameResolver;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.UUID;

/** Server-only single-stage kick confirmation using a separate 27-slot chest handler. */
public final class TeamKickConfirmationGui {
    private TeamKickConfirmationGui() {}

    public static void openFirst(TeamMenuHandler ignored, ServerPlayerEntity player, Team team, UUID targetUuid) {
        if (player == null || team == null || targetUuid == null || !team.isOwner(player.getUuid())
                || player.getUuid().equals(targetUuid) || team.getMember(targetUuid) == null) return;
        player.closeHandledScreen();
        if (player.getEntityWorld().getServer() != null) {
            player.getEntityWorld().getServer().execute(() -> open(player, team, targetUuid));
        } else {
            open(player, team, targetUuid);
        }
    }

    private static void open(ServerPlayerEntity player, Team team, UUID targetUuid) {
        String targetName = PlayerNameResolver.resolve(player.getEntityWorld().getServer(), targetUuid);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new Handler(syncId, playerInventory, player, team, targetUuid),
                Text.literal("Kick " + targetName + "?").setStyle(Style.EMPTY.withItalic(false))
        ));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final ServerPlayerEntity player;
        private final Team team;
        private final UUID targetUuid;

        Handler(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory,
                ServerPlayerEntity player, Team team, UUID targetUuid) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.player = player;
            this.team = team;
            this.targetUuid = targetUuid;
            for (int i = 0; i < 27; i++) {
                addSlot(new MenuSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInventory, col + row * 9 + 9,
                            8 + col * 18, 84 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }
            populate();
        }

        private void populate() {
            ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" ").setStyle(Style.EMPTY.withItalic(false)));
            for (int i = 0; i < 27; i++) menu.setStack(i, filler.copy());

            ItemStack confirm = new ItemStack(Items.GREEN_WOOL);
            confirm.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ᴄᴏɴғɪʀᴍ")
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true).withItalic(false)));
            confirm.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("This action cannot be undone.")
                            .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)) )));
            menu.setStack(11, confirm);

            ItemStack cancel = new ItemStack(Items.RED_WOOL);
            cancel.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ᴄᴀɴᴄᴇʟ")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
            cancel.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("Return to player management.")
                            .setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false)) )));
            menu.setStack(15, cancel);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity clicker) {
            if (slot < 0 || slot >= 27) return;
            if (!(clicker instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.getUuid().equals(player.getUuid())) return;
            if (!team.isOwner(serverPlayer.getUuid()) || team.getMember(targetUuid) == null
                    || serverPlayer.getUuid().equals(targetUuid)) {
                serverPlayer.closeHandledScreen();
                return;
            }
            if (slot == 15) {
                TeamGuiManager.openMemberEditor(serverPlayer, team, targetUuid);
                return;
            }
            if (slot == 11) {
                TeamInPlaceMemberGui.performKick(serverPlayer, team, targetUuid);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.getUuid().equals(this.player.getUuid())
                    && team.isOwner(player.getUuid())
                    && team.getMember(targetUuid) != null
                    && !player.getUuid().equals(targetUuid);
        }
    }

    private static final class MenuSlot extends Slot {
        private MenuSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { return false; }
    }
}
