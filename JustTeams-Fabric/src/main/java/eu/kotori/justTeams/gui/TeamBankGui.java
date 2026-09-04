package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Opens the team's item-backed bank as a vanilla 6-row chest interface. */
public final class TeamBankGui {
    private TeamBankGui() {}

    public static void open(PlayerEntity player, Team team) {
        if (!JustTeamsFabric.config().isBankEnabled()) {
            player.sendMessage(Text.literal("The team bank is disabled."), true);
            return;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!team.hasElevatedPermissions(serverPlayer.getUuid())) {
                player.sendMessage(Text.literal("Only the Leader or Co-Leader can use the team bank."), true);
                return;
            }
            if (!JustTeamsFabric.permissions().has(serverPlayer, JustTeamsPermissions.COMMAND_BANK)) {
                player.sendMessage(Text.literal("You do not have permission to use the team bank."), true);
                return;
            }
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new TeamBankScreenHandler(syncId, playerInventory, team),
                Text.literal("Team Bank - " + team.getName())
        ));
    }
}
