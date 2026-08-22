package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamEnderChest;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.UUID;

/** Opens and manages the shared persistent Ender Chest belonging to a team. */
public final class TeamEnderChestGui {
    private TeamEnderChestGui() {}

    public static void open(PlayerEntity player, Team team) {
        if (!JustTeamsFabric.config().isEnderChestEnabled()) {
            player.sendMessage(Text.literal("The team Ender Chest is disabled."), true);
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null) {
            player.sendMessage(Text.literal("You are not in this team."), true);
            return;
        }
        if (!member.canUseEnderChest()
                && !JustTeamsFabric.permissions().has(serverPlayer, JustTeamsPermissions.BYPASS_ENDERCHEST_USE)) {
            player.sendMessage(Text.literal("You do not have permission to use the team Ender Chest."), true);
            return;
        }
        if (!FeatureCostManager.charge(serverPlayer, "enderchest")) return;

        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null) {
            enderChest = new TeamEnderChest(team, JustTeamsFabric.config().getEnderChestRows());
            team.setEnderChest(enderChest);
        }
        enderChest.setSaveCallback(TeamEnderChestGui::save);
        enderChest.addViewer(player.getUuid());
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new TeamEnderChestScreenHandler(syncId, playerInventory, team),
                Text.literal("Team Ender Chest - " + team.getName())
        ));
    }

    static void handleClosed(PlayerEntity player, Team team) {
        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null) return;
        enderChest.removeViewer(player.getUuid());
        if (!enderChest.hasViewers()) release(team);
    }

    /** Removes a disconnected player's viewer registration and releases the chest when appropriate. */
    public static void handleDisconnect(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) return;
        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null || !enderChest.getViewers().contains(player.getUuid())) return;
        enderChest.removeViewer(player.getUuid());
        if (!enderChest.hasViewers()) release(team);
    }

    /** Closes one viewer before membership removal so its shared chest state is released safely. */
    public static void closeViewer(MinecraftServer server, Team team, UUID viewerUuid) {
        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null) return;
        ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerUuid);
        if (viewer != null && viewer.currentScreenHandler instanceof TeamEnderChestScreenHandler) {
            viewer.closeHandledScreen();
        } else {
            enderChest.removeViewer(viewerUuid);
            if (!enderChest.hasViewers()) release(team);
        }
    }

    /** Saves the chest and closes every tracked viewer before team removal. */
    public static void closeAndRelease(MinecraftServer server, Team team) {
        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null) return;
        for (UUID viewerUuid : enderChest.getViewers()) closeViewer(server, team, viewerUuid);
        release(team);
    }

    private static void release(Team team) {
        TeamEnderChest enderChest = team.getEnderChest();
        if (enderChest == null) return;
        save();
        enderChest.setSaveCallback(null);
        team.setEnderChest(null);
    }

    private static void save() {
        try {
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        } catch (IOException exception) {
            JustTeamsFabric.LOGGER.error("Failed to save team Ender Chest", exception);
        }
    }
}
