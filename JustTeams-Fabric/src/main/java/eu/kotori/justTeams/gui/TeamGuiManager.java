package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;

/** Entry point for the server-side JustTeams inventory GUI system. */
public final class TeamGuiManager {
    private static final int[] MEMBER_SLOTS = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

    private TeamGuiManager() {}

    public static void openMain(PlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) { NoTeamGui.open(player); return; }
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new TeamMenuHandler(syncId, inventory, player.getUuid(), team, TeamGuiManager::handleMainClick),
                Text.literal("Team - " + team.getMembers().size() + "/Infinity")
        ));
    }

    private static void handleMainClick(PlayerEntity player, int slot, int button, SlotActionType actionType, Team team, TeamMenuHandler menu) {
        if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE) return;
        int memberIndex = memberIndexForSlot(slot);
        if (memberIndex >= 0 && memberIndex < team.getMembers().size()) { MemberManagementGui.open(player, team, team.getMembers().get(memberIndex)); return; }
        switch (slot) {
            case 45 -> togglePvp(player, team, menu);
            case 53 -> leaveOrDisband(player, team);
            case 49 -> { team.cycleSortType(); save(); menu.refresh(); }
            case 52 -> { if (team.hasElevatedPermissions(player.getUuid())) TeamSettingsGui.open(player, team); else player.sendMessage(Text.literal("Only the owner or co-owners can access team settings."), true); }
            case 8 -> { if (team.hasElevatedPermissions(player.getUuid())) JoinRequestGui.open(player, team); else player.sendMessage(Text.literal("Only the owner or co-owners can access join requests."), true); }
            case 46 -> TeamEnderChestGui.open(player, team);
            case 47 -> TeamHomeGui.open(player, team);
            case 48 -> { }
            case 50 -> { if (player instanceof ServerPlayerEntity serverPlayer && JustTeamsFabric.permissions().has(serverPlayer, JustTeamsPermissions.COMMAND_BANK)) TeamBankGui.open(player, team); else player.sendMessage(Text.literal("You do not have permission to use the team bank."), true); }
            case 7 -> TeamWarpGui.open(player, team);
            default -> { }
        }
    }

    private static void togglePvp(PlayerEntity player, Team team, TeamMenuHandler menu) {
        if (!team.isOwner(player.getUuid())) { player.sendMessage(Text.literal("Only the team owner can change PvP."), true); return; }
        team.setPvpEnabled(!team.isPvpEnabled()); save(); menu.refresh();
    }

    private static void leaveOrDisband(PlayerEntity player, Team team) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (team.isOwner(player.getUuid())) TeamEnderChestGui.closeAndRelease(serverPlayer.getEntityWorld().getServer(), team);
            else TeamEnderChestGui.closeViewer(serverPlayer.getEntityWorld().getServer(), team, player.getUuid());
        }
        if (team.isOwner(player.getUuid())) {
            for (TeamPlayer member : team.getMembers()) {
                TeamChatManager.disable(member.getPlayerUuid());
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    JustTeamsFabric.glow().stopGlowForPlayer(serverPlayer.getEntityWorld().getServer(), member.getPlayerUuid());
                }
            }
            JustTeamsFabric.teams().unregister(team);
            save(); close(player);
            player.sendMessage(Text.literal("Team disbanded."), false);
        } else {
            TeamChatManager.disable(player.getUuid());
            if (player instanceof ServerPlayerEntity serverPlayer) {
                JustTeamsFabric.glow().stopGlowForPlayer(serverPlayer.getEntityWorld().getServer(), player.getUuid());
            }
            JustTeamsFabric.teams().removeMember(team, player.getUuid());
            save(); close(player);
            player.sendMessage(Text.literal("You left the team."), false);
        }
    }

    private static void close(PlayerEntity player) { if (player instanceof ServerPlayerEntity serverPlayer) serverPlayer.closeHandledScreen(); }
    private static int memberIndexForSlot(int slot) { for (int i = 0; i < MEMBER_SLOTS.length; i++) if (MEMBER_SLOTS[i] == slot) return i; return -1; }
    private static void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save JustTeams data after GUI action", exception); } }
    @FunctionalInterface public interface TeamMenuActionHandler { void handle(PlayerEntity player, int slot, int button, SlotActionType actionType, Team team, TeamMenuHandler menu); }
}
