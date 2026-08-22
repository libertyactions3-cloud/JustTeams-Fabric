package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
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

/** Member-management inventory GUI. Actions are authorized server-side. */
public final class MemberManagementGui {
    private MemberManagementGui() {}

    public static void open(PlayerEntity viewer, Team team, TeamPlayer target) {
        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, viewer, team, target), Text.literal("Manage Member")));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(27);
        private final PlayerEntity viewer;
        private final Team team;
        private final TeamPlayer target;

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer, Team team, TeamPlayer target) {
            super(ScreenHandlerType.GENERIC_9X3, syncId); this.viewer = viewer; this.team = team; this.target = target; populate();
            for (int i = 0; i < 27; i++) addSlot(new MenuSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        private void populate() {
            for (int i = 0; i < 27; i++) menu.setStack(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            ItemStack head = new ItemStack(Items.PLAYER_HEAD); head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(target.getPlayerUuid())); head.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Member")); menu.setStack(4, head);
            menu.setStack(10, named(Items.GOLDEN_HELMET, "Role: " + target.getRole()));
            menu.setStack(11, named(Items.LIME_DYE, "Promote")); menu.setStack(12, named(Items.YELLOW_DYE, "Demote")); menu.setStack(14, named(Items.RED_DYE, "Kick"));
            menu.setStack(16, named(target.canWithdraw() ? Items.EMERALD : Items.COAL, "Withdraw: " + target.canWithdraw()));
            menu.setStack(17, named(target.canUseEnderChest() ? Items.ENDER_CHEST : Items.CHEST, "Ender Chest: " + target.canUseEnderChest()));
            menu.setStack(18, named(target.canSetHome() ? Items.LIME_WOOL : Items.RED_WOOL, "Set Home: " + target.canSetHome()));
            menu.setStack(19, named(target.canUseHome() ? Items.LIME_WOOL : Items.RED_WOOL, "Use Home: " + target.canUseHome()));
            menu.setStack(20, named(target.canKickMembers() ? Items.IRON_SWORD : Items.STONE_SWORD, "Kick Members: " + target.canKickMembers()));
            menu.setStack(22, named(Items.ARROW, "Back"));
        }

        @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (slot < 0 || slot >= 27) return;
            if (slot == 22) { TeamGuiManager.openMain(player); return; }
            if (!team.isOwner(player.getUuid()) || target.getPlayerUuid().equals(player.getUuid())) return;
            switch (slot) {
                case 11 -> { if (target.getRole() == TeamRole.MEMBER) target.setRole(TeamRole.CO_OWNER); }
                case 12 -> { if (target.getRole() == TeamRole.CO_OWNER) target.setRole(TeamRole.MEMBER); }
                case 14 -> {
                    TeamChatManager.disable(target.getPlayerUuid());
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        TeamEnderChestGui.closeViewer(serverPlayer.getEntityWorld().getServer(), team, target.getPlayerUuid());
                        JustTeamsFabric.glow().stopGlowForPlayer(serverPlayer.getEntityWorld().getServer(), target.getPlayerUuid());
                        JustTeamsFabric.teams().removeMember(team, target.getPlayerUuid());
                        save();
                        TeamNotificationManager.notifyKick(serverPlayer.getEntityWorld().getServer(), team, player.getUuid(), target.getPlayerUuid());
                    } else {
                        JustTeamsFabric.teams().removeMember(team, target.getPlayerUuid());
                        save();
                    }
                    close(player);
                    TeamGuiManager.openMain(player);
                    return;
                }
                case 16 -> target.setCanWithdraw(!target.canWithdraw()); case 17 -> target.setCanUseEnderChest(!target.canUseEnderChest());
                case 18 -> target.setCanSetHome(!target.canSetHome()); case 19 -> target.setCanUseHome(!target.canUseHome()); case 20 -> target.setCanKickMembers(!target.canKickMembers());
                default -> { return; }
            }
            save(); populate(); sendContentUpdates();
        }

        private static void close(PlayerEntity player) { if (player instanceof ServerPlayerEntity serverPlayer) serverPlayer.closeHandledScreen(); }
        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()) && team.isMember(player.getUuid()); }
        private void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save team member change", e); } }
        private static ItemStack named(net.minecraft.item.Item item, String name) { ItemStack s = new ItemStack(item); s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name)); return s; }
        private static final class MenuSlot extends Slot { MenuSlot(Inventory i, int n, int x, int y) { super(i,n,x,y); } @Override public boolean canInsert(ItemStack s) { return false; } @Override public boolean canTakeItems(PlayerEntity p) { return false; } }
    }
}
