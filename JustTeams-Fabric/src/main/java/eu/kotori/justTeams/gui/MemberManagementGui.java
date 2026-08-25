package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Member-management inventory GUI. Actions are authorized server-side. */
public final class MemberManagementGui {
    private MemberManagementGui() {}

    public static void open(PlayerEntity viewer, Team team, TeamPlayer target) {
        String targetName = resolveName(viewer, target);
        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, viewer, team, target),
                Text.literal("Edit: " + targetName).setStyle(Style.EMPTY.withItalic(false))));
    }

    private static String resolveName(PlayerEntity viewer, TeamPlayer target) {
        if (viewer instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayerEntity online = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(target.getPlayerUuid());
            if (online != null) return online.getName().getString();
        }
        return target.getPlayerUuid().toString().substring(0, 8);
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
            for (int i = 0; i < 27; i++) menu.setStack(i, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(target.getPlayerUuid()));
            String playerName = resolveName(viewer, target);
            head.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(playerName).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
            head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Role: ", roleName(target.getRole()), Formatting.GRAY, Formatting.WHITE),
                    composeLine("Joined: ", formatJoinDate(target), Formatting.GRAY, Formatting.WHITE)
            )));
            menu.setStack(4, head);

            menu.setStack(10, roleItem(target.getRole()));
            menu.setStack(11, actionItem(Items.LIME_DYE, "PROMOTE TO CO-OWNER", List.of(
                    plainLine("Gives this player more permissions.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to promote.", Formatting.YELLOW))));
            menu.setStack(12, actionItem(Items.GRAY_DYE, "DEMOTE TO MEMBER", List.of(
                    plainLine("Removes co-owner permissions.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to demote.", Formatting.YELLOW))));
            menu.setStack(14, actionItem(Items.RED_WOOL, "KICK MEMBER", List.of(
                    plainLine("Removes this player from the team.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to kick", Formatting.YELLOW))));

            menu.setStack(16, permissionItem(Items.GOLD_INGOT, "ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ",
                    "Allow this member to withdraw from the team bank.", target.canWithdraw()));
            menu.setStack(17, permissionItem(Items.ENDER_CHEST, "ᴜsᴇ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ",
                    "Allow this member to use the team ender chest.", target.canUseEnderChest()));
            menu.setStack(18, permissionItem(Items.GRASS_BLOCK, "sᴇᴛ ᴛᴇᴀᴍ ʜᴏᴍᴇ",
                    "Allow this member to set the team home location.", target.canSetHome()));
            menu.setStack(19, permissionItem(Items.ENDER_PEARL, "ᴜsᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ",
                    "Allow this member to teleport to the team home.", target.canUseHome()));
            menu.setStack(20, permissionItem(Items.IRON_SWORD, "ᴋɪᴄᴋ ᴍᴇᴍʙᴇʀs",
                    "Allow this member to manage and remove team members.", target.canKickMembers()));

            menu.setStack(22, backItem());
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

        private static String roleName(TeamRole role) {
            return switch (role) {
                case OWNER -> "Owner";
                case CO_OWNER -> "Co-Owner";
                case MEMBER -> "Member";
            };
        }

        private static String formatJoinDate(TeamPlayer member) {
            if (member.getJoinDate() == null) return "Unknown";
            return DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate());
        }

        private static ItemStack roleItem(TeamRole role) {
            return actionItem(Items.GOLDEN_HELMET, "Role: " + roleName(role), List.of(
                    plainLine("Current role: " + roleName(role), Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Member management permissions are controlled below.", Formatting.YELLOW)));
        }

        private static ItemStack permissionItem(net.minecraft.item.Item item, String name, String description, boolean enabled) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true).withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine(description, Formatting.GRAY),
                    composeLine("Status: ", enabled ? "ENABLED" : "DISABLED", Formatting.GRAY,
                            enabled ? Formatting.GREEN : Formatting.RED)
            )));
            return stack;
        }

        private static ItemStack actionItem(net.minecraft.item.Item item, String name, List<Text> lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true).withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            return stack;
        }

        private static ItemStack backItem() {
            ItemStack stack = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to the main menu.", Formatting.YELLOW)
            )));
            return stack;
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

        private static void close(PlayerEntity player) { if (player instanceof ServerPlayerEntity serverPlayer) serverPlayer.closeHandledScreen(); }
        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()) && team.isMember(player.getUuid()); }
        private void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save team member change", e); } }
        private static final class MenuSlot extends Slot { MenuSlot(Inventory i, int n, int x, int y) { super(i,n,x,y); } @Override public boolean canInsert(ItemStack s) { return false; } @Override public boolean canTakeItems(PlayerEntity p) { return false; } }
    }
}
