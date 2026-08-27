package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.WeakHashMap;

/** Two-stage persistent confirmation flow for team disbanding. */
public final class TeamDisbandConfirmationGui {
    private static final WeakHashMap<TeamMenuHandler, Integer> STAGES = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> SNAPSHOTS = new WeakHashMap<>();

    private TeamDisbandConfirmationGui() {}

    public static void openFirst(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        if (!team.isOwner(player.getUuid())) return;
        snapshot(menu);
        STAGES.put(menu, 1);
        render(menu, team, 1);
    }

    public static boolean isOpen(TeamMenuHandler menu) { return STAGES.containsKey(menu); }

    public static void handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        Integer stage = STAGES.get(menu);
        if (stage == null) return;
        if (slot == 15 || slot == 49) { closeToMain(menu); return; }
        if (slot != 11) return;
        if (!team.isOwner(player.getUuid())) { closeToMain(menu); return; }
        if (stage == 1) { STAGES.put(menu, 2); render(menu, team, 2); }
        else { STAGES.remove(menu); SNAPSHOTS.remove(menu); TeamGuiManager.performDisband(player, team); }
    }

    private static void render(TeamMenuHandler menu, Team team, int stage) {
        Inventory inventory = menu.getMenuInventory();
        ItemStack filler = pane();
        for (int slot = 0; slot < 54; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, filler.copy());
        for (int slot = 45; slot < 54; slot++) inventory.setStack(slot, filler.copy());

        ItemStack warning = new ItemStack(Items.TNT);
        warning.set(DataComponentTypes.CUSTOM_NAME, Text.literal(stage == 1 ? "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ" : "ᴄᴏɴғɪʀᴍ ᴅɪsʙᴀɴᴅ").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
        warning.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Team: " + team.getName()).setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)),
                Text.literal("").setStyle(net.minecraft.text.Style.EMPTY.withItalic(false)),
                Text.literal(stage == 1 ? "This permanently deletes the team." : "This is your final confirmation.").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)),
                Text.literal("All members, settings, warps, bank data, and team data will be removed.").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.DARK_RED).withItalic(false)))));
        inventory.setStack(13, warning);

        ItemStack confirm = new ItemStack(stage == 1 ? Items.GREEN_WOOL : Items.RED_WOOL);
        confirm.set(DataComponentTypes.CUSTOM_NAME, Text.literal(stage == 1 ? "ᴄᴏɴᴛɪɴᴜᴇ" : "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ").setStyle(net.minecraft.text.Style.EMPTY.withColor(stage == 1 ? Formatting.GREEN : Formatting.RED).withBold(true).withItalic(false)));
        confirm.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(stage == 1 ? "Continue to the final confirmation." : "Permanently delete the team.").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))));
        inventory.setStack(11, confirm);

        ItemStack cancel = new ItemStack(Items.RED_WOOL);
        cancel.set(DataComponentTypes.CUSTOM_NAME, Text.literal("ᴄᴀɴᴄᴇʟ").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
        cancel.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Return to the team menu.").setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))));
        inventory.setStack(15, cancel);
        menu.sendContentUpdates();
    }

    private static void snapshot(TeamMenuHandler menu) {
        ItemStack[] snapshot = new ItemStack[54];
        for (int slot = 0; slot < 54; slot++) snapshot[slot] = menu.getMenuInventory().getStack(slot).copy();
        SNAPSHOTS.put(menu, snapshot);
    }

    private static void closeToMain(TeamMenuHandler menu) {
        STAGES.remove(menu);
        ItemStack[] snapshot = SNAPSHOTS.remove(menu);
        if (snapshot == null) return;
        Inventory inventory = menu.getMenuInventory();
        for (int slot = 0; slot < snapshot.length; slot++) inventory.setStack(slot, snapshot[slot].copy());
        menu.sendContentUpdates();
    }

    private static ItemStack pane() {
        ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" ").setStyle(net.minecraft.text.Style.EMPTY.withItalic(false)));
        return pane;
    }
}
