package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.List;
import java.util.WeakHashMap;

/** Persistent two-stage disband confirmation rendered inside the existing 54-slot team GUI. */
public final class TeamDisbandConfirmationGui {
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> SNAPSHOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Integer> STAGES = new WeakHashMap<>();

    private TeamDisbandConfirmationGui() {}

    public static void openFirst(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        if (menu == null || player == null || team == null || !team.isOwner(player.getUuid())) return;
        snapshot(menu);
        STAGES.put(menu, 1);
        render(menu, team, 1);
    }

    public static boolean isOpen(TeamMenuHandler menu) {
        return menu != null && STAGES.containsKey(menu);
    }

    public static boolean handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        if (!isOpen(menu)) return false;
        if (player == null || team == null || !team.isOwner(player.getUuid())) {
            close(menu);
            return true;
        }
        int stage = STAGES.getOrDefault(menu, 1);
        if (slot == 11) {
            if (stage == 1) {
                STAGES.put(menu, 2);
                render(menu, team, 2);
            } else {
                close(menu);
                TeamGuiManager.performDisband(player, team);
            }
            return true;
        }
        if (slot == 15) {
            close(menu);
            return true;
        }
        return true;
    }

    public static void close(TeamMenuHandler menu) {
        STAGES.remove(menu);
        ItemStack[] snapshot = SNAPSHOTS.remove(menu);
        if (snapshot == null) return;
        Inventory inventory = menu.getMenuInventory();
        for (int slot = 0; slot < snapshot.length; slot++) inventory.setStack(slot, snapshot[slot].copy());
        menu.sendContentUpdates();
    }

    private static void snapshot(TeamMenuHandler menu) {
        if (SNAPSHOTS.containsKey(menu)) return;
        ItemStack[] snapshot = new ItemStack[54];
        for (int slot = 0; slot < snapshot.length; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy();
        SNAPSHOTS.put(menu, snapshot);
    }

    private static void render(TeamMenuHandler menu, Team team, int stage) {
        Inventory inventory = menu.getMenuInventory();
        ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ", Formatting.WHITE, false);
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, filler.copy());

        if (stage == 1) {
            inventory.setStack(4, namedGradient(Items.TNT, "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ"));
            inventory.setStack(22, itemWithLore(Items.PAPER, "ᴀʀᴇ ʏᴏᴜ sᴜʀᴇ?",
                    List.of(line("This will permanently delete your team.", Formatting.GRAY),
                            line("This action cannot be undone.", Formatting.DARK_RED))));
        } else {
            inventory.setStack(4, namedGradient(Items.TNT, "ᴅɪsʙᴀɴᴅ " + team.getName()));
            inventory.setStack(22, itemWithLore(Items.PAPER, "ᴄᴏɴғɪʀᴍ ᴅɪsʙᴀɴᴅ?",
                    List.of(line("Permanently delete team " + team.getName() + ".", Formatting.GRAY),
                            line("This action cannot be undone.", Formatting.DARK_RED))));
        }

        inventory.setStack(11, action(Items.GREEN_WOOL, "ᴄᴏɴғɪʀᴍ", Formatting.GREEN,
                "Confirm and continue."));
        inventory.setStack(15, action(Items.RED_WOOL, "ᴄᴀɴᴄᴇʟ", Formatting.RED,
                "Return to your team menu."));
        menu.sendContentUpdates();
    }

    private static ItemStack action(net.minecraft.item.Item item, String name, Formatting color, String lore) {
        return itemWithLore(item, name, List.of(line(lore, color)));
    }

    private static ItemStack itemWithLore(net.minecraft.item.Item item, String name, List<Text> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(name.equals("ᴄᴏɴғɪʀᴍ") ? Formatting.GREEN : name.equals("ᴄᴀɴᴄᴇʟ") ? Formatting.RED : Formatting.WHITE).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private static ItemStack named(net.minecraft.item.Item item, String name, Formatting color, boolean bold) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(false)));
        return stack;
    }

    private static ItemStack namedGradient(net.minecraft.item.Item item, String value) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(value));
        return stack;
    }

    private static MutableText line(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static MutableText gradientText(String value) {
        int start = 0x4C9DDE;
        int end = 0x4C96D2;
        MutableText result = Text.empty();
        if (value.isEmpty()) return result;
        int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
        int index = 0;
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            double t = (double) index / length;
            int sr = (start >> 16) & 255, sg = (start >> 8) & 255, sb = start & 255;
            int er = (end >> 16) & 255, eg = (end >> 8) & 255, eb = end & 255;
            int r = (int) Math.round(sr + (er - sr) * t);
            int g = (int) Math.round(sg + (eg - sg) * t);
            int b = (int) Math.round(sb + (eb - sb) * t);
            result.append(Text.literal(new String(Character.toChars(cp)))
                    .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(true).withItalic(false)));
            offset += Character.charCount(cp);
            index++;
        }
        return result;
    }
}
