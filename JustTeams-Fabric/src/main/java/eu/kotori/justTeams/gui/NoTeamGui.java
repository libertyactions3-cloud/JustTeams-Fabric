package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
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
import java.util.List;

/** 27-slot GUI shown to players who are not currently in a team. */
public final class NoTeamGui {
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    private NoTeamGui() {}

    public static void open(PlayerEntity player) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            TeamGuiManager.openMain(player);
            return;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player),
                Text.literal("ᴛᴇᴀᴍ ᴍᴇɴᴜ").setStyle(Style.EMPTY.withItalic(false))
        ));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final PlayerEntity viewer;

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer) {
            super(ScreenHandlerType.GENERIC_9X3, syncId);
            this.viewer = viewer;
            populate();
            for (int i = 0; i < 27; i++) {
                addSlot(new MenuSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, 8 + column * 18, 142));
            }
        }

        private void populate() {
            ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 27; i++) menu.setStack(i, filler.copy());

            ItemStack create = namedGradient(Items.WRITABLE_BOOK, "ᴄʀᴇᴀᴛᴇ ᴀ ᴛᴇᴀᴍ");
            create.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Start your own team and invite your friends!", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to begin the creation process.", Formatting.YELLOW)
            )));
            menu.setStack(12, create);

            ItemStack leaderboards = namedGradient(Items.EMERALD, "ᴠɪᴇᴡ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅs");
            leaderboards.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("See the top teams on the server.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to view leaderboards.", Formatting.YELLOW)
            )));
            menu.setStack(14, leaderboards);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (slot < 0 || slot >= 27) {
                super.onSlotClick(slot, button, action, player);
                return;
            }
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP
                    || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            if (slot == 12) {
                serverPlayer.closeHandledScreen();
                beginCreation(serverPlayer);
            } else if (slot == 14) {
                serverPlayer.sendMessage(Text.literal("Leaderboards are the next GUI module."), true);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) {
            return player.getUuid().equals(viewer.getUuid()) && !JustTeamsFabric.teams().isInTeam(player.getUuid());
        }
    }

    private static void beginCreation(ServerPlayerEntity player) {
        TeamStringInputGui.open(player, "Create Team", "Enter your new team's name (1-16 characters)", name -> {
            String cleanName = name.trim();
            if (cleanName.isBlank() || cleanName.length() > 16 || cleanName.contains(" ")) {
                player.sendMessage(Text.literal("Invalid team name. Use 1-16 non-space characters."), false);
                open(player);
                return;
            }
            TeamStringInputGui.open(player, "Create Team", "Enter your team's tag (1-4 characters)", tag -> {
                String cleanTag = tag.trim();
                if (cleanTag.isBlank() || cleanTag.length() > 4 || cleanTag.contains(" ")) {
                    player.sendMessage(Text.literal("Invalid team tag. Use 1-4 non-space characters."), false);
                    open(player);
                    return;
                }
                try {
                    if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
                        player.sendMessage(Text.literal("You are already in a team."), false);
                        return;
                    }
                    JustTeamsFabric.teams().createTeam(cleanName, cleanTag, player.getUuid(), true, false, false);
                    JustTeamsFabric.storage().save(JustTeamsFabric.teams());
                    player.sendMessage(Text.literal("Team created successfully."), false);
                    TeamGuiManager.openMain(player);
                } catch (IllegalStateException | IOException exception) {
                    JustTeamsFabric.LOGGER.error("Failed to create team", exception);
                    player.sendMessage(Text.literal("Unable to create the team."), false);
                    open(player);
                }
            }, () -> open(player));
        }, () -> open(player));
    }

    private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static ItemStack namedGradient(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name));
        return stack;
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
