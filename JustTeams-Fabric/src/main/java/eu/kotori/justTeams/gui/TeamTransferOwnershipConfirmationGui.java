package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
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
import eu.kotori.justTeams.util.PlayerNameResolver;

import java.util.List;
import java.util.UUID;

/** Server-only two-stage ownership transfer confirmation using separate 27-slot chest handlers. */
public final class TeamTransferOwnershipConfirmationGui {
    private TeamTransferOwnershipConfirmationGui() {}

    public static void openFirst(TeamMenuHandler ignored, ServerPlayerEntity player, Team team, UUID targetUuid) {
        if (!valid(player, team, targetUuid)) return;
        player.closeHandledScreen();
        if (player.getEntityWorld().getServer() != null) {
            player.getEntityWorld().getServer().execute(() -> open(player, team, targetUuid, 1));
        } else {
            open(player, team, targetUuid, 1);
        }
    }

    private static boolean valid(ServerPlayerEntity player, Team team, UUID targetUuid) {
        return player != null && team != null && targetUuid != null
                && team.isOwner(player.getUuid())
                && !player.getUuid().equals(targetUuid)
                && team.getMember(targetUuid) != null;
    }

    private static String targetName(ServerPlayerEntity player, UUID targetUuid) {
        return PlayerNameResolver.resolve(player.getEntityWorld().getServer(), targetUuid);
    }

    private static void open(ServerPlayerEntity player, Team team, UUID targetUuid, int stage) {
        if (!valid(player, team, targetUuid)) return;
        String name = targetName(player, targetUuid);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new Handler(syncId, playerInventory, player, team, targetUuid, stage),
                Text.literal(stage == 1
                        ? "Are you sure you want to transfer ownership to " + name + "? This cannot be undone."
                        : "Transfer ownership to " + name + "?")
                        .setStyle(Style.EMPTY.withItalic(false))
        ));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final ServerPlayerEntity player;
        private final Team team;
        private final UUID targetUuid;
        private final int stage;

        Handler(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory,
                ServerPlayerEntity player, Team team, UUID targetUuid, int stage) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.player = player;
            this.team = team;
            this.targetUuid = targetUuid;
            this.stage = stage;
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
                    Text.literal("Transfer ownership.")
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
            if (!valid(serverPlayer, team, targetUuid)) {
                serverPlayer.closeHandledScreen();
                return;
            }
            if (slot == 15) {
                TeamGuiManager.openMemberEditor(serverPlayer, team, targetUuid);
                return;
            }
            if (slot != 11) return;
            if (stage == 1) {
                serverPlayer.closeHandledScreen();
                if (serverPlayer.getEntityWorld().getServer() != null) {
                    serverPlayer.getEntityWorld().getServer().execute(() -> open(serverPlayer, team, targetUuid, 2));
                } else {
                    open(serverPlayer, team, targetUuid, 2);
                }
            } else {
                TeamInPlaceMemberGui.performTransfer(serverPlayer, team, targetUuid);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.getUuid().equals(this.player.getUuid()) && valid(this.player, team, targetUuid);
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
