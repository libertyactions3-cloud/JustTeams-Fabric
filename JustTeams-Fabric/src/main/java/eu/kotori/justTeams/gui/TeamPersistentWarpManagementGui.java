package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamWarp;
import eu.kotori.justTeams.util.ChatInputManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.WeakHashMap;

/** Warp-management view rendered inside the persistent 54-slot team handler. */
public final class TeamPersistentWarpManagementGui {
    private static final WeakHashMap<TeamMenuHandler, TeamWarp> OPEN = new WeakHashMap<>();

    private TeamPersistentWarpManagementGui() {}

    public static boolean isOpen(TeamMenuHandler menu) {
        return OPEN.containsKey(menu);
    }

    public static void open(TeamMenuHandler menu, ServerPlayerEntity player, Team team, TeamWarp warp) {
        OPEN.put(menu, warp);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        inventory.setStack(4, named(Items.NAME_TAG, "ᴡᴀʀᴘ: " + warp.getName(), Formatting.AQUA, true));

        inventory.setStack(20, toggleItem(warp.isEnabled(), Items.LIME_DYE, Items.GRAY_DYE,
                "ᴡᴀʀᴘ ᴇɴᴀʙʟᴇᴅ", "ᴡᴀʀᴘ ᴅɪsᴀʙʟᴇᴅ"));
        inventory.setStack(22, toggleItem(warp.isMembersCanUse(), Items.PLAYER_HEAD, Items.BARRIER,
                "ᴍᴇᴍʙᴇʀs ᴄᴀɴ ᴜsᴇ", "ᴍᴇᴍʙᴇʀs ᴄᴀɴɴᴏᴛ ᴜsᴇ"));
        inventory.setStack(24, named(Items.PAPER, "ᴄᴏsᴛ: " + formatCost(warp.getCost()), Formatting.WHITE, true));
        inventory.setStack(31, named(warp.getPassword().isEmpty() ? Items.IRON_BARS : Items.TRIPWIRE_HOOK,
                warp.getPassword().isEmpty() ? "ᴘᴀssᴡᴏʀᴅ: ɴᴏɴᴇ" : "ᴘᴀssᴡᴏʀᴅ: sᴇᴛ", Formatting.WHITE, true));
        inventory.setStack(40, named(Items.COMPASS, "ʀᴇsᴇᴛ ᴡᴀʀᴘ ʟᴏᴄᴀᴛɪᴏɴ", Formatting.YELLOW, true));
        inventory.setStack(45, named(Items.ARROW, "ʙᴀᴄᴋ", Formatting.GRAY, true));
        inventory.setStack(49, named(Items.RED_DYE, "ʀᴇᴍᴏᴠᴇ ᴡᴀʀᴘ", Formatting.RED, true));
        inventory.setStack(53, named(Items.BARRIER, "ᴄʟᴏsᴇ", Formatting.RED, true));
        menu.sendContentUpdates();
    }

    public static void handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        TeamWarp warp = OPEN.get(menu);
        if (warp == null || !team.hasElevatedPermissions(player.getUuid())) return;

        switch (slot) {
            case 20 -> {
                warp.setEnabled(!warp.isEnabled());
                save();
                open(menu, player, team, warp);
            }
            case 22 -> {
                warp.setMembersCanUse(!warp.isMembersCanUse());
                save();
                open(menu, player, team, warp);
            }
            case 24 -> ChatInputManager.begin(player,
                    "Enter whole-number cost (0 for free):", value -> {
                        try {
                            double cost = Double.parseDouble(value);
                            if (!Double.isFinite(cost) || cost < 0.0D || cost != Math.rint(cost)) throw new NumberFormatException();
                            warp.setCost(cost);
                            save();
                            open(menu, player, team, warp);
                        } catch (NumberFormatException exception) {
                            player.sendMessage(Text.literal("Enter a non-negative whole number."), true);
                            open(menu, player, team, warp);
                        }
                    });
            case 31 -> ChatInputManager.begin(player,
                    "Enter password or NONE:", value -> {
                        warp.setPassword(value.equalsIgnoreCase("NONE") ? "" : value);
                        save();
                        open(menu, player, team, warp);
                    });
            case 40 -> {
                warp.setLocation(player.getEntityWorld().getRegistryKey().getValue().toString(),
                        player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                save();
                player.sendMessage(Text.literal("Warp location updated."), true);
                open(menu, player, team, warp);
            }
            case 45 -> {
                OPEN.remove(menu);
                TeamInPlaceGui.enterWarps(menu, player, team);
            }
            case 49 -> {
                team.removeWarp(warp.getName());
                save();
                OPEN.remove(menu);
                TeamInPlaceGui.enterWarps(menu, player, team);
            }
            case 53 -> {
                OPEN.remove(menu);
                player.closeHandledScreen();
            }
            default -> { }
        }
    }

    public static void close(TeamMenuHandler menu) {
        OPEN.remove(menu);
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static ItemStack toggleItem(boolean enabled, Item on, Item off, String onName, String offName) {
        return named(enabled ? on : off, enabled ? onName : offName, enabled ? Formatting.GREEN : Formatting.RED, true);
    }

    private static ItemStack named(Item item, String name, Formatting color, boolean bold) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).setStyle(net.minecraft.text.Style.EMPTY.withColor(color).withBold(bold).withItalic(false)));
        return stack;
    }

    private static ItemStack namedPlain(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).setStyle(net.minecraft.text.Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static String formatCost(double cost) {
        return cost == Math.rint(cost) ? Long.toString((long) cost) : Double.toString(cost);
    }

    private static void save() {
        try {
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.error("Failed to save JustTeams data after persistent warp management action", exception);
        }
    }
}
