package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamEnderChest;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

/** Vanilla chest-style handler backed by a team's shared persistent inventory. */
public final class TeamEnderChestScreenHandler extends GenericContainerScreenHandler {
    private final Team team;

    public TeamEnderChestScreenHandler(int syncId, PlayerInventory playerInventory, Team team) {
        super(typeFor(team.getEnderChest().getRows()), syncId, playerInventory, team.getEnderChest(), team.getEnderChest().getRows());
        this.team = team;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        TeamEnderChestGui.handleClosed(player, team);
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> typeFor(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }
}
