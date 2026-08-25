package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.ChatInputManager;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Team settings menu corresponding to the Paper JustTeams settings GUI. */
public final class TeamSettingsGui {
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    private TeamSettingsGui() {}

    public static void open(PlayerEntity player, Team team) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player, team),
                Text.literal("ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs").setStyle(Style.EMPTY.withItalic(false))
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
            for (int i = 0; i < 27; i++) menu.setStack(i, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));

            ItemStack tag = namedGradient(Items.NAME_TAG, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴛᴀɢ");
            tag.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Current: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to change the team tag.", Formatting.YELLOW)
            )));
            menu.setStack(11, tag);

            ItemStack description = namedGradient(Items.OAK_SIGN, "ᴄʜᴀɴɢᴇ ᴛᴇᴀᴍ ᴅᴇsᴄʀɪᴘᴛɪᴏɴ");
            description.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Current: ", team.getDescription(), Formatting.GRAY, Formatting.WHITE),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to change the team description.", Formatting.YELLOW)
            )));
            menu.setStack(13, description);

            ItemStack status = namedGradient(Items.ENDER_EYE, "ᴛᴇᴀᴍ sᴛᴀᴛᴜs");
            menu.setStack(15, status);

            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to the main menu.", Formatting.YELLOW)
            )));
            menu.setStack(22, back);
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

        private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static ItemStack namedGradient(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name));
            return stack;
        }

        private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
            return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
        }

        private static MutableText plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static MutableText gradientText(String value) {
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
                        .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(true).withItalic(false)));
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
}
