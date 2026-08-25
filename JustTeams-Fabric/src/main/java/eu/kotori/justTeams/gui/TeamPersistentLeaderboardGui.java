package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import eu.kotori.justTeams.team.Team;

import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/** Two-stage leaderboard views rendered inside the persistent 54-slot team handler. */
public final class TeamPersistentLeaderboardGui {
    public enum View { CATEGORIES, RANKED }
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;
    private static final int[] RANK_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

    private static final WeakHashMap<TeamMenuHandler, View> OPEN = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, TeamPersistentLeaderboardGuiType> TYPES = new WeakHashMap<>();

    public enum TeamPersistentLeaderboardGuiType { KILLS, BALANCE, MEMBERS }

    private TeamPersistentLeaderboardGui() {}

    public static boolean isOpen(TeamMenuHandler menu) { return OPEN.containsKey(menu); }

    public static void openCategories(TeamMenuHandler menu) {
        OPEN.put(menu, View.CATEGORIES);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        inventory.setStack(4, namedGradient(Items.NETHER_STAR, "ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ"));
        inventory.setStack(11, categoryItem(Items.NETHERITE_SWORD, "ᴛᴏᴘ ᴋɪʟʟs", "Shows the top 10 teams with the most kills."));
        inventory.setStack(13, categoryItem(Items.DIAMOND, "ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇ", "Shows the top 10 richest teams."));
        inventory.setStack(15, categoryItem(Items.PLAYER_HEAD, "ᴛᴏᴘ ᴍᴇᴍʙᴇʀs", "Shows the top 10 teams with the most members."));
        inventory.setStack(22, backItem("Click to return to the main menu."));
        menu.sendContentUpdates();
    }

    public static void openLeaderboard(TeamMenuHandler menu, TeamPersistentLeaderboardGuiType type) {
        OPEN.put(menu, View.RANKED);
        TYPES.put(menu, type);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);
        List<Team> teams = JustTeamsFabric.teams().getTeams().stream()
                .sorted(comparator(type))
                .limit(RANK_SLOTS.length)
                .toList();

        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            int rank = i + 1;
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(team.getOwnerUuid()));
            head.set(DataComponentTypes.CUSTOM_NAME, gradientText("#" + rank + " " + team.getName(), true));
            String statisticName;
            String statisticValue;
            switch (type) {
                case KILLS -> { statisticName = "Kills"; statisticValue = String.valueOf(team.getKills()); }
                case BALANCE -> { statisticName = "Balance"; statisticValue = String.format("%.2f", team.getBalance()); }
                case MEMBERS -> { statisticName = "Members"; statisticValue = String.valueOf(team.getMembers().size()); }
                default -> throw new IllegalStateException("Unexpected leaderboard type: " + type);
            }
            head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                    composeLine(statisticName + ": ", statisticValue, Formatting.GRAY, Formatting.WHITE)
            )));
            inventory.setStack(RANK_SLOTS[i], head);
        }
        inventory.setStack(49, backItem("Click to return to category selection."));
        menu.sendContentUpdates();
    }

    public static void close(TeamMenuHandler menu) {
        OPEN.remove(menu);
        TYPES.remove(menu);
    }

    public static void handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        View view = OPEN.get(menu);
        if (view == null) return;
        if (view == View.CATEGORIES) {
            switch (slot) {
                case 11 -> openLeaderboard(menu, TeamPersistentLeaderboardGuiType.KILLS);
                case 13 -> openLeaderboard(menu, TeamPersistentLeaderboardGuiType.BALANCE);
                case 15 -> openLeaderboard(menu, TeamPersistentLeaderboardGuiType.MEMBERS);
                case 22 -> { close(menu); TeamInPlaceGui.returnToMain(menu); }
                default -> { }
            }
        } else if (view == View.RANKED && slot == 49) {
            openCategories(menu);
        }
    }

    private static Comparator<Team> comparator(TeamPersistentLeaderboardGuiType type) {
        Comparator<Team> comparator = switch (type) {
            case KILLS -> Comparator.comparingInt(Team::getKills).reversed();
            case BALANCE -> Comparator.comparingDouble(Team::getBalance).reversed();
            case MEMBERS -> Comparator.comparingInt((Team team) -> team.getMembers().size()).reversed();
        };
        return comparator.thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static ItemStack categoryItem(net.minecraft.item.Item item, String name, String loreText) {
        ItemStack stack = namedGradient(item, name);
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(loreText, Formatting.GRAY))));
        return stack;
    }

    private static ItemStack backItem(String loreText) {
        ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(loreText, Formatting.YELLOW))));
        return stack;
    }

    private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static ItemStack namedGradient(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name, true));
        return stack;
    }

    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
        return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
    }

    private static MutableText plainLine(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static MutableText gradientText(String value, boolean bold) {
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
                    .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(bold).withItalic(false)));
            offset += Character.charCount(codePoint);
            index++;
        }
        return result;
    }
}
