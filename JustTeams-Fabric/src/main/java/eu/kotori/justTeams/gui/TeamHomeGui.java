package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
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

import java.io.IOException;
import java.util.UUID;

/** Server-side Home GUI following the inventory pattern used by Fabric claim mods such as Flan. */
public final class TeamHomeGui {
    private TeamHomeGui() {}

    public static void open(PlayerEntity player, Team team) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player.getUuid(), team),
                Text.literal("Team Home")
        ));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menuInventory = new SimpleInventory(27);
        private final UUID viewerUuid;
        private final Team team;

        private Handler(int syncId, PlayerInventory playerInventory, UUID viewerUuid, Team team) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.viewerUuid = viewerUuid;
            this.team = team;
            populate();
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
                addSlot(new MenuSlot(menuInventory, row * 9 + column, 8 + column * 18, 18 + row * 18));
            addPlayerInventory(playerInventory);
        }

        private void addPlayerInventory(PlayerInventory inventory) {
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        private void populate() {
            ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < menuInventory.size(); i++) menuInventory.setStack(i, filler.copy());
            menuInventory.setStack(4, named(Items.ENDER_PEARL, "ᴛᴇᴀᴍ ʜᴏᴍᴇ"));
            menuInventory.setStack(11, named(Items.ENDER_EYE, team.getHome() == null ? "ɴᴏ ʜᴏᴍᴇ sᴇᴛ" : "ᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ʜᴏᴍᴇ"));
            menuInventory.setStack(13, named(Items.LODESTONE, "sᴇᴛ ʜᴏᴍᴇ"));
            menuInventory.setStack(15, named(Items.BARRIER, "ᴄʟᴇᴀʀ ʜᴏᴍᴇ"));
            menuInventory.setStack(22, named(Items.ARROW, "ʙᴀᴄᴋ"));
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (slotIndex < 0 || slotIndex >= menuInventory.size()) {
                super.onSlotClick(slotIndex, button, actionType, player);
                return;
            }
            if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE)
                return;
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !player.getUuid().equals(viewerUuid) || !team.isMember(viewerUuid)) return;
            switch (slotIndex) {
                case 11 -> useHome(serverPlayer);
                case 13 -> setHome(serverPlayer);
                case 15 -> clearHome(serverPlayer);
                case 22 -> TeamGuiManager.openMain(serverPlayer);
                default -> { }
            }
        }

        private void useHome(ServerPlayerEntity player) {
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canUseHome()) { player.sendMessage(Text.literal("You do not have permission to use the team home."), true); return; }
            TeamLocation home = team.getHome();
            if (home == null) { player.sendMessage(Text.literal("Your team does not have a home set."), true); return; }
            if (!FeatureCostManager.charge(player, "home")) return;
            JustTeamsFabric.teleports().requestHome(player, home);
        }

        private void setHome(ServerPlayerEntity player) {
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canSetHome()) { player.sendMessage(Text.literal("You do not have permission to set the team home."), true); return; }
            if (!FeatureCostManager.charge(player, "sethome")) return;
            team.setHome(TeamLocation.fromPlayer(player));
            save();
            player.sendMessage(Text.literal("Team home set at your current location."), true);
            populate();
            sendContentUpdates();
        }

        private void clearHome(ServerPlayerEntity player) {
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canSetHome()) { player.sendMessage(Text.literal("You do not have permission to clear the team home."), true); return; }
            if (team.getHome() == null) { player.sendMessage(Text.literal("Your team does not have a home set."), true); return; }
            team.clearHome();
            save();
            player.sendMessage(Text.literal("Team home cleared."), true);
            populate();
            sendContentUpdates();
        }

        private void save() {
            try {
                JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            } catch (IOException exception) {
                JustTeamsFabric.LOGGER.error("Failed to save JustTeams data after Home GUI action", exception);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && team.isMember(viewerUuid); }

        private static ItemStack named(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }

        private static final class MenuSlot extends Slot {
            private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}
