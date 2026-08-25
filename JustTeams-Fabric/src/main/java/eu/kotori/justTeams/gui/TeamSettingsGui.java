package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.ChatInputManager;
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

/** Team settings menu corresponding to the Paper JustTeams settings GUI. */
public final class TeamSettingsGui {
    private TeamSettingsGui() {}

    public static void open(PlayerEntity player, Team team) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player, team),
                Text.literal("ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs")
        ));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final PlayerEntity viewer;
        private final Team team;

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer, Team team) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.viewer = viewer;
            this.team = team;
            populate();
            for (int i = 0; i < 27; i++) addSlot(new MenuSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        private void populate() {
            for (int i = 0; i < 27; i++) menu.setStack(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            menu.setStack(11, named(Items.NAME_TAG, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴛᴀɢ", "Current: " + team.getTag(), "", "Click to change the team tag."));
            menu.setStack(13, named(Items.OAK_SIGN, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴅᴇsᴄʀɪᴘᴛɪᴏɴ", "Current: " + team.getDescription(), "", "Click to change the team description."));
            menu.setStack(15, named(team.isPublic() ? Items.LIME_DYE : Items.GRAY_DYE,
                    "ᴛᴇᴀᴍ sᴛᴀᴛᴜs", "Currently: " + (team.isPublic() ? "Public" : "Private"), "", "Click to toggle public/private."));
            menu.setStack(22, named(Items.ARROW, "ʙᴀᴄᴋ", "Click to return to the main menu."));
        }

        @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP
                    || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            if (slot < 0 || slot >= 27) return;
            if (!team.hasElevatedPermissions(player.getUuid())) {
                player.sendMessage(Text.literal("Only the owner or co-owners can change team settings."), true);
                return;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            switch (slot) {
                case 11 -> ChatInputManager.begin(serverPlayer,
                        "Enter the new team tag (1-4 characters, or type cancel):", input -> {
                            try {
                                JustTeamsFabric.teams().setTag(serverPlayer.getUuid(), input);
                                save();
                                serverPlayer.sendMessage(Text.literal("Team tag updated."), false);
                                refresh();
                            } catch (IllegalArgumentException | IllegalStateException exception) {
                                serverPlayer.sendMessage(Text.literal(exception.getMessage()), false);
                            }
                        });
                case 13 -> ChatInputManager.begin(serverPlayer,
                        "Enter the new team description (1-256 characters, or type cancel):", input -> {
                            try {
                                JustTeamsFabric.teams().setDescription(serverPlayer.getUuid(), input);
                                save();
                                serverPlayer.sendMessage(Text.literal("Team description updated."), false);
                                refresh();
                            } catch (IllegalArgumentException | IllegalStateException exception) {
                                serverPlayer.sendMessage(Text.literal(exception.getMessage()), false);
                            }
                        });
                case 15 -> {
                    try {
                        boolean enabled = JustTeamsFabric.teams().togglePublic(serverPlayer.getUuid());
                        save();
                        refresh();
                        serverPlayer.sendMessage(Text.literal("Team is now " + (enabled ? "public" : "private") + "."), false);
                    } catch (IllegalStateException exception) {
                        serverPlayer.sendMessage(Text.literal(exception.getMessage()), false);
                    }
                }
                case 22 -> TeamGuiManager.openMain(serverPlayer);
                default -> { }
            }
        }

        private void refresh() {
            populate();
            sendContentUpdates();
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) {
            return player.getUuid().equals(viewer.getUuid()) && team.isMember(player.getUuid());
        }

        private void save() {
            try {
                JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            } catch (IOException e) {
                JustTeamsFabric.LOGGER.error("Failed to save team settings", e);
            }
        }

        private static ItemStack named(net.minecraft.item.Item item, String name, String... lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            if (lore.length > 0) {
                java.util.List<Text> lines = java.util.Arrays.stream(lore)
                        .<Text>map(Text::literal)
                        .toList();
                stack.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lines));
            }
            return stack;
        }

        private static final class MenuSlot extends Slot {
            MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}