package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamSortType;
import eu.kotori.justTeams.util.PlayerNameResolver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Entry point for the server-side JustTeams inventory GUI system. */
public final class TeamGuiManager {
    private static final int[] MEMBER_SLOTS = {
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };

    private TeamGuiManager() {}

    public static void openMain(PlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) {
            if (player instanceof ServerPlayerEntity serverPlayer) TeamPersistentNoTeamGui.openMain(serverPlayer);
            return;
        }
        if (player.currentScreenHandler instanceof TeamMenuHandler menu
                && menu.getTeam().getId() == team.getId() && team.isMember(player.getUuid())) {
            TeamPersistentWarpManagementGui.close(menu);
            TeamPersistentBlacklistGui.close(menu);
            TeamPersistentLeaderboardGui.close(menu);
            TeamInPlaceMemberGui.back(menu);
            TeamBankLogsGui.close(menu);
            TeamInPlaceGui.returnToMain(menu);
            TeamInPlaceGui.refreshMainMembers(menu, player, team);
            return;
        }
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new TeamMenuHandler(syncId, inventory, player.getUuid(), team, TeamGuiManager::handleMainClick),
                Text.literal("ᴛᴇᴀᴍ ᴍᴇɴᴜ")));
        if (player.currentScreenHandler instanceof TeamMenuHandler menu) {
            TeamInPlaceGui.refreshMainMembers(menu, player, team);
            TeamInPlaceGui.updateMainSortItem(menu, team);
        }
    }

    public static void openPersistentView(ServerPlayerEntity player, TeamInPlaceGui.View view) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) { TeamPersistentNoTeamGui.openMain(player); return; }
        TeamMenuHandler menu;
        if (player.currentScreenHandler instanceof TeamMenuHandler existing && existing.getTeam().getId() == team.getId() && team.isMember(player.getUuid())) menu = existing;
        else { openMain(player); if (!(player.currentScreenHandler instanceof TeamMenuHandler opened)) return; menu = opened; }
        TeamPersistentWarpManagementGui.close(menu); TeamPersistentBlacklistGui.close(menu); TeamPersistentLeaderboardGui.close(menu); TeamBankLogsGui.close(menu);
        switch (view) {
            case MAIN -> TeamInPlaceGui.returnToMain(menu);
            case JOIN_REQUESTS -> TeamInPlaceGui.enterJoinRequests(menu, player, team);
            case WARPS -> TeamInPlaceGui.enterWarps(menu, player, team);
            case SETTINGS -> TeamInPlaceGui.enterSettings(menu, player, team);
        }
    }

    public static void openPersistentLeaderboard(ServerPlayerEntity player, TeamPersistentLeaderboardGui.View view,
                                                 TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType type) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) { if (view == TeamPersistentLeaderboardGui.View.CATEGORIES) TeamPersistentNoTeamGui.openLeaderboardCategories(player); else TeamPersistentNoTeamGui.openLeaderboard(player, type); return; }
        TeamMenuHandler menu;
        if (player.currentScreenHandler instanceof TeamMenuHandler existing && existing.getTeam().getId() == team.getId() && team.isMember(player.getUuid())) menu = existing;
        else { openMain(player); if (!(player.currentScreenHandler instanceof TeamMenuHandler opened)) return; menu = opened; }
        TeamPersistentWarpManagementGui.close(menu); TeamPersistentBlacklistGui.close(menu); TeamBankLogsGui.close(menu);
        if (view == TeamPersistentLeaderboardGui.View.CATEGORIES) TeamPersistentLeaderboardGui.openCategories(menu); else TeamPersistentLeaderboardGui.openLeaderboard(menu, type);
    }

    private static void handleMainClick(PlayerEntity player, int slot, int button, SlotActionType actionType, Team team, TeamMenuHandler menu) {
        if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE) return;
        if (TeamBankLogsGui.isOpen(menu)) { if (player instanceof ServerPlayerEntity sp) TeamBankLogsGui.handle(menu, sp, team, slot); return; }
        if (TeamDisbandConfirmationGui.isOpen(menu)) { if (player instanceof ServerPlayerEntity sp) TeamDisbandConfirmationGui.handle(menu, sp, team, slot); return; }
        if (TeamPersistentWarpManagementGui.isOpen(menu)) { if (player instanceof ServerPlayerEntity sp) TeamPersistentWarpManagementGui.handle(menu, sp, team, slot); return; }
        if (TeamPersistentBlacklistGui.isOpen(menu)) { if (player instanceof ServerPlayerEntity sp) TeamPersistentBlacklistGui.handle(menu, sp, team, slot); return; }
        if (TeamPersistentLeaderboardGui.isOpen(menu)) { if (player instanceof ServerPlayerEntity sp) TeamPersistentLeaderboardGui.handle(menu, sp, team, slot); return; }

        TeamInPlaceGui.View view = TeamInPlaceGui.view(menu);
        if (TeamInPlaceMemberGui.isOpen(menu)) { TeamInPlaceMemberGui.handle(menu, player, team, slot); return; }
        if (view == TeamInPlaceGui.View.JOIN_REQUESTS) { if (slot == 49) TeamInPlaceGui.returnToMain(menu); else if (player instanceof ServerPlayerEntity sp) TeamInPlaceGui.handleJoinRequestClick(menu, sp, team, slot, button); return; }
        if (view == TeamInPlaceGui.View.WARPS) { if (slot == 49) TeamInPlaceGui.returnToMain(menu); else if (player instanceof ServerPlayerEntity sp) TeamInPlaceGui.handleWarpClick(menu, sp, team, slot, button); return; }
        if (view == TeamInPlaceGui.View.SETTINGS) { if (player instanceof ServerPlayerEntity sp) TeamInPlaceGui.handleSettingsClick(menu, sp, team, slot); return; }

        int memberIndex = memberIndexForSlot(slot);
        if (memberIndex >= 0) {
            List<TeamPlayer> displayedMembers = orderedMembers(player, team);
            if (memberIndex >= displayedMembers.size()) return;
            TeamPlayer target = displayedMembers.get(memberIndex);
            if (target.getPlayerUuid().equals(player.getUuid())) return;
            TeamPlayer viewerMember = team.getMember(player.getUuid());
            if (viewerMember == null || viewerMember.getRank().ordinal() > TeamRank.UNDEROFFICER.ordinal()) return;
            if (target.getRank().ordinal() <= viewerMember.getRank().ordinal()) return;
            TeamInPlaceMemberGui.enter(menu, player, team, target, slot);
            return;
        }

        switch (slot) {
            case 45 -> togglePvp(player, team, menu);
            case 53 -> { if (team.isOwner(player.getUuid())) TeamDisbandConfirmationGui.openFirst(menu, (ServerPlayerEntity) player, team); else leaveTeam(player, team); }
            case 49 -> { team.cycleSortType(); save(); TeamInPlaceGui.updateMainSortItem(menu, team); TeamInPlaceGui.refreshMainMembers(menu, player, team); }
            case 52 -> { if (team.hasElevatedPermissions(player.getUuid())) TeamInPlaceGui.enterSettings(menu, player, team); else player.sendMessage(Text.literal("Only the owner or co-owners can access team settings."), true); }
            case 8 -> { if (team.hasElevatedPermissions(player.getUuid())) TeamInPlaceGui.enterJoinRequests(menu, player, team); else player.sendMessage(Text.literal("Only the owner or co-owners can access join requests."), true); }
            case 6 -> { if (player instanceof ServerPlayerEntity sp) TeamBankLogsGui.open(menu, sp, team); }
            case 46 -> TeamEnderChestGui.open(player, team);
            case 47 -> useHome(player, team);
            case 50 -> { if (player instanceof ServerPlayerEntity sp && JustTeamsFabric.permissions().has(sp, JustTeamsPermissions.COMMAND_BANK)) TeamBankGui.open(player, team); else player.sendMessage(Text.literal("You do not have permission to use the team bank."), true); }
            case 7 -> TeamInPlaceGui.enterWarps(menu, player, team);
            default -> { }
        }
    }

    private static List<TeamPlayer> orderedMembers(PlayerEntity viewer, Team team) {
        List<TeamPlayer> members = new ArrayList<>(team.getMembers());
        var server = viewer instanceof ServerPlayerEntity sp ? sp.getEntityWorld().getServer() : null;
        Comparator<TeamPlayer> comparator = switch (team.getCurrentSortType()) {
            case ONLINE_STATUS -> Comparator.comparing((TeamPlayer m) -> server != null && server.getPlayerManager().getPlayer(m.getPlayerUuid()) != null).reversed()
                    .thenComparing(m -> PlayerNameResolver.resolve(server, m.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
            case ALPHABETICAL -> Comparator.comparing(m -> PlayerNameResolver.resolve(server, m.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
            case RANK -> Comparator.comparingInt((TeamPlayer m) -> m.getRank().ordinal())
                    .thenComparing(m -> PlayerNameResolver.resolve(server, m.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
        };
        members.sort(comparator); return members;
    }

    private static void useHome(PlayerEntity player, Team team) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canUseHome()) { player.sendMessage(Text.literal("You do not have permission to use the team home."), true); return; }
        if (team.getHome() == null) {
            Text message = Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(Formatting.BLUE).withItalic(false))
                    .append(Text.literal("Your team does not have a home set. An Owner or Co-Owner can set one with /team sethome.").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false)));
            player.sendMessage(message, false); return;
        }
        JustTeamsFabric.teleports().requestHome(serverPlayer, team.getHome());
    }

    private static void togglePvp(PlayerEntity player, Team team, TeamMenuHandler menu) {
        if (!team.isOwner(player.getUuid())) { player.sendMessage(Text.literal("Only the team owner can change PvP."), true); return; }
        team.setPvpEnabled(!team.isPvpEnabled()); save(); TeamInPlaceGui.updateMainPvpItem(menu, team);
    }

    private static void leaveTeam(PlayerEntity player, Team team) {
        TeamChatManager.disable(player.getUuid());
        if (player instanceof ServerPlayerEntity sp) {
            var server = sp.getEntityWorld().getServer();
            TeamEnderChestGui.closeViewer(server, team, player.getUuid());
            JustTeamsFabric.glow().stopGlowForPlayer(server, player.getUuid());
        }
        JustTeamsFabric.teams().removeMember(team, player.getUuid()); save(); close(player); player.sendMessage(Text.literal("You left the team."), false);
    }

    /** Shared final disband operation used by both the GUI and /team disband confirmation path. */
    public static void performDisband(ServerPlayerEntity player, Team team) {
        if (player == null || team == null || !team.isOwner(player.getUuid())) return;
        var server = player.getEntityWorld().getServer();
        TeamEnderChestGui.closeAndRelease(server, team);
        for (TeamPlayer member : team.getMembers()) {
            TeamChatManager.disable(member.getPlayerUuid());
            JustTeamsFabric.glow().stopGlowForPlayer(server, member.getPlayerUuid());
        }
        JustTeamsFabric.teams().unregister(team); save(); player.closeHandledScreen(); player.sendMessage(Text.literal("You have successfully disbanded your team."), false);
    }

    private static void close(PlayerEntity player) { if (player instanceof ServerPlayerEntity sp) sp.closeHandledScreen(); }
    private static int memberIndexForSlot(int slot) { for (int i = 0; i < MEMBER_SLOTS.length; i++) if (MEMBER_SLOTS[i] == slot) return i; return -1; }
    private static void save() { try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); } catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save JustTeams data after GUI action", exception); } }
    @FunctionalInterface public interface TeamMenuActionHandler { void handle(PlayerEntity player, int slot, int button, SlotActionType actionType, Team team, TeamMenuHandler menu); }
}
