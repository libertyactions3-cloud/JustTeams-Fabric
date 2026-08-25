package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Server-side management menu for one team warp. */
public final class TeamWarpManagementGui {
    private TeamWarpManagementGui() {}

    public static void open(ServerPlayerEntity player, Team team, TeamWarp warp) {
        if (player.currentScreenHandler instanceof TeamMenuHandler menu && team.isMember(player.getUuid())) {
            TeamPersistentWarpManagementGui.open(menu, player, team, warp);
            return;
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player.getUuid(), team, warp),
                Text.literal("Warp: " + warp.getName())
        ));
    }

    private static void save() {
        try {
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.error("Failed to save JustTeams data after warp management action", exception);
        }
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menuInventory = new SimpleInventory(54);
        private final UUID viewerUuid;
        private final Team team;
        private final TeamWarp warp;

        private Handler(int syncId, PlayerInventory playerInventory, UUID viewerUuid, Team team, TeamWarp warp) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewerUuid = viewerUuid;
            this.team = team;
            this.warp = warp;
            populate(playerInventory.player);
            for (int row = 0; row < 6; row++) for (int column = 0; column < 9; column++)
                addSlot(new MenuSlot(menuInventory, row * 9 + column, 8 + column * 18, 18 + row * 18));
            addPlayerInventory(playerInventory);
        }

        private void addPlayerInventory(PlayerInventory inventory) {
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 198));
        }

        private void populate(PlayerEntity player) {
            ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < menuInventory.size(); i++) menuInventory.setStack(i, filler.copy());
            menuInventory.setStack(4, named(Items.NAME_TAG, "ᴡᴀʀᴘ: " + warp.getName()));

            setToggle(20, warp.isEnabled(), Items.LIME_DYE, Items.GRAY_DYE, "ᴡᴀʀᴘ ᴇɴᴀʙʟᴇᴅ", "ᴡᴀʀᴘ ᴅɪsᴀʙʟᴇᴅ");
            setToggle(22, warp.isMembersCanUse(), Items.PLAYER_HEAD, Items.BARRIER, "ᴍᴇᴍʙᴇʀs ᴄᴀɴ ᴜsᴇ", "ᴍᴇᴍʙᴇʀs ᴄᴀɴɴᴏᴛ ᴜsᴇ");
            menuInventory.setStack(24, named(Items.PAPER, "ᴄᴏsᴛ: " + formatCost(warp.getCost())));
            menuInventory.setStack(31, named(warp.getPassword().isEmpty() ? Items.IRON_BARS : Items.TRIPWIRE_HOOK,
                    warp.getPassword().isEmpty() ? "ᴘᴀssᴡᴏʀᴅ: ɴᴏɴᴇ" : "ᴘᴀssᴡᴏʀᴅ: sᴇᴛ"));
            menuInventory.setStack(40, named(Items.COMPASS, "ʀᴇsᴇᴛ ᴡᴀʀᴘ ʟᴏᴄᴀᴛɪᴏɴ"));
            menuInventory.setStack(45, named(Items.ARROW, "ʙᴀᴄᴋ"));
            menuInventory.setStack(49, named(Items.RED_DYE, "ʀᴇᴍᴏᴠᴇ ᴡᴀʀᴘ"));
            menuInventory.setStack(53, named(Items.BARRIER, "ᴄʟᴏsᴇ"));
        }

        private void setToggle(int slot, boolean enabled, Item on, Item off, String onName, String offName) {
            menuInventory.setStack(slot, named(enabled ? on : off, enabled ? onName : offName));
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (slotIndex < 0 || slotIndex >= menuInventory.size()) {
                super.onSlotClick(slotIndex, button, actionType, player);
                return;
            }
            if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !player.getUuid().equals(viewerUuid) || !team.hasElevatedPermissions(viewerUuid)) return;

            switch (slotIndex) {
                case 20 -> { warp.setEnabled(!warp.isEnabled()); save(); refresh(serverPlayer); }
                case 22 -> { warp.setMembersCanUse(!warp.isMembersCanUse()); save(); refresh(serverPlayer); }
                case 24 -> TeamStringInputGui.open(serverPlayer, "Warp Cost", "Enter whole-number cost (0 for free)", value -> {
                    try {
                        double cost = Double.parseDouble(value);
                        if (!Double.isFinite(cost) || cost < 0.0D || cost != Math.rint(cost)) throw new NumberFormatException();
                        warp.setCost(cost);
                        save();
                        open(serverPlayer, team, warp);
                    } catch (NumberFormatException exception) {
                        serverPlayer.sendMessage(Text.literal("Enter a non-negative whole number."), true);
                        open(serverPlayer, team, warp);
                    }
                }, () -> open(serverPlayer, team, warp));
                case 31 -> TeamStringInputGui.open(serverPlayer, "Warp Password", "Enter password or NONE", value -> {
                    warp.setPassword(value.equalsIgnoreCase("NONE") ? "" : value);
                    save();
                    open(serverPlayer, team, warp);
                }, () -> open(serverPlayer, team, warp));
                case 40 -> {
                    warp.setLocation(serverPlayer.getEntityWorld().getRegistryKey().getValue().toString(), serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYaw(), serverPlayer.getPitch());
                    save();
                    serverPlayer.sendMessage(Text.literal("Warp location updated."), true);
                    refresh(serverPlayer);
                }
                case 45 -> TeamWarpGui.open(serverPlayer, team);
                case 49 -> {
                    team.removeWarp(warp.getName());
                    save();
                    TeamWarpGui.open(serverPlayer, team);
                }
                case 53 -> serverPlayer.closeHandledScreen();
                default -> { }
            }
        }

        private void refresh(ServerPlayerEntity player) {
            populate(player);
            sendContentUpdates();
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && team.hasElevatedPermissions(viewerUuid); }

        private static String formatCost(double cost) { return cost == Math.rint(cost) ? Long.toString((long) cost) : Double.toString(cost); }
        private static ItemStack named(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(net.minecraft.text.Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static final class MenuSlot extends Slot {
            private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}