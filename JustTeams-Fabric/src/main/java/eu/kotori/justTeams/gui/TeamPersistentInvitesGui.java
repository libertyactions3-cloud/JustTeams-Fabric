package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
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
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** Pending-invites view rendered inside the persistent 54-slot team handler. */
public final class TeamPersistentInvitesGui {
    private static final int[] INVITE_SLOTS = {
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };
    private static final int PRIMARY_START = 0x4C9D9D;
    private static final int PRIMARY_END = 0x4C96D2;

    private static final WeakHashMap<TeamMenuHandler, Boolean> OPEN = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> MAIN_SNAPSHOTS = new WeakHashMap<>();

    private TeamPersistentInvitesGui() {}

    public static boolean isOpen(TeamMenuHandler menu) {
        return OPEN.getOrDefault(menu, false);
    }

    public static void open(TeamMenuHandler menu, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return;
        snapshotMain(menu);
        OPEN.put(menu, true);
        Inventory inventory = menu.getMenuInventory();
        clear(inventory);

        List<Team> invites = getInvites(player);
        if (invites.isEmpty()) {
            ItemStack empty = namedPlain(Items.PAPER, "No Pending Invites");
            empty.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("No Pending Invites").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("You don't have any pending team invitations.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Team owners can invite you with", Formatting.DARK_GRAY),
                    plainLine("/team invite <player>", Formatting.DARK_GRAY)
            )));
            inventory.setStack(22, empty);
        } else {
            for (int i = 0; i < INVITE_SLOTS.length && i < invites.size(); i++) {
                inventory.setStack(INVITE_SLOTS[i], inviteItem(invites.get(i), player));
            }
        }

        inventory.setStack(49, backItem());
        inventory.setStack(53, closeItem());
        menu.sendContentUpdates();
    }

    public static void close(TeamMenuHandler menu) {
        OPEN.remove(menu);
        MAIN_SNAPSHOTS.remove(menu);
    }

    public static boolean handle(TeamMenuHandler menu, PlayerEntity player, int slot, int button) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (slot == 49) {
            returnToMain(menu);
            return true;
        }
        if (slot == 53) {
            close(menu);
            serverPlayer.closeHandledScreen();
            return true;
        }

        List<Team> invites = getInvites(player);
        int index = indexOf(slot);
        if (index < 0 || index >= invites.size()) return true;
        Team team = invites.get(index);
        if (button == 0) {
            accept(menu, serverPlayer, team);
        } else if (button == 1) {
            deny(menu, serverPlayer, team);
        }
        return true;
    }

    private static void accept(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            team.removeInvite(player.getUuid());
            save();
            player.sendMessage(Text.literal("You are already in a team."), true);
            open(menu, player);
            return;
        }
        team.removeInvite(player.getUuid());
        JustTeamsFabric.teams().addMember(team, new TeamPlayer(
                player.getUuid(), TeamRole.MEMBER, java.time.Instant.now(), false, false, false, true));
        save();
        JustTeamsFabric.glow().refreshAll(player.getEntityWorld().getServer());
        player.sendMessage(Text.literal("You have joined " + team.getName() + "."), false);
        returnToMain(menu);
    }

    private static void deny(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        team.removeInvite(player.getUuid());
        save();
        player.sendMessage(Text.literal("You declined the invitation to join " + team.getName() + "."), false);
        open(menu, player);
    }

    private static List<Team> getInvites(PlayerEntity player) {
        List<Team> invites = new ArrayList<>();
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) return invites;
        for (Team team : JustTeamsFabric.teams().getTeams()) {
            if (team.hasInvite(player.getUuid())) invites.add(team);
        }
        return invites;
    }

    private static ItemStack inviteItem(Team team, PlayerEntity viewer) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(team.getName(), true));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                composeLine("Invited by: ", inviterName(team, viewer), Formatting.GRAY, Formatting.YELLOW),
                composeLine("Members: ", Integer.toString(team.getMembers().size()), Formatting.GRAY, Formatting.WHITE),
                composeLine("Description: ", team.getDescription(), Formatting.GRAY, Formatting.WHITE)
        )));
        return stack;
    }

    private static String inviterName(Team team, PlayerEntity viewer) {
        if (viewer instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayerEntity online = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(team.getOwnerUuid());
            if (online != null) return online.getName().getString();
        }
        return team.getOwnerUuid().toString().substring(0, 8);
    }

    private static void clear(Inventory inventory) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());
    }

    private static ItemStack backItem() {
        ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to return to the main menu.", Formatting.YELLOW))));
        return stack;
    }

    private static ItemStack closeItem() {
        ItemStack stack = namedPlain(Items.BARRIER, "ᴄʟᴏsᴇ");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("ᴄʟᴏsᴇ").setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to close this menu.", Formatting.RED))));
        return stack;
    }

    private static void snapshotMain(TeamMenuHandler menu) {
        if (MAIN_SNAPSHOTS.containsKey(menu)) return;
        ItemStack[] snapshot = new ItemStack[54];
        for (int slot = 0; slot < snapshot.length; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy();
        MAIN_SNAPSHOTS.put(menu, snapshot);
    }

    private static void returnToMain(TeamMenuHandler menu) {
        ItemStack[] snapshot = MAIN_SNAPSHOTS.get(menu);
        if (snapshot != null) {
            Inventory inventory = menu.getMenuInventory();
            for (int slot = 0; slot < snapshot.length; slot++) inventory.setStack(slot, snapshot[slot].copy());
        }
        close(menu);
        menu.sendContentUpdates();
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < INVITE_SLOTS.length; i++) if (INVITE_SLOTS[i] == slot) return i;
        return -1;
    }

    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
        return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
    }

    private static MutableText plainLine(String text, Formatting color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return stack;
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

    private static void save() {
        try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
        catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save team invitation change", exception); }
    }
}
