package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.TeamBank;
import eu.kotori.justTeams.economy.TeamBankLogManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** Six-row chest handler for a team bank, with per-member withdrawal control. */
public final class TeamBankScreenHandler extends GenericContainerScreenHandler {
    private static final int BANK_SLOTS = TeamBank.SLOT_COUNT;
    private final Team team;

    public TeamBankScreenHandler(int syncId, PlayerInventory playerInventory, Team team) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, team.getBank(), 6);
        this.team = team;
        for (int slotIndex = 0; slotIndex < BANK_SLOTS; slotIndex++) {
            Slot original = slots.get(slotIndex);
            slots.set(slotIndex, new BankSlot(team.getBank(), original.getIndex(), original.x, original.y, team));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        long before = team.getBank().getTotalEmeraldValue();
        super.onSlotClick(slotIndex, button, actionType, player);
        long after = team.getBank().getTotalEmeraldValue();
        if (player instanceof ServerPlayerEntity serverPlayer && before > after) {
            TeamBankLogManager.record(serverPlayer.getEntityWorld().getServer(), team, serverPlayer,
                    before - after, TeamBankLogManager.Kind.MANUAL_WITHDRAWAL, "team-bank withdrawal");
        }
    }

    private static final class BankSlot extends Slot {
        private final Team team;
        private BankSlot(TeamBank bank, int index, int x, int y, Team team) { super(bank, index, x, y); this.team = team; }
        @Override public boolean canInsert(ItemStack stack) { return team.getBank().isCurrency(stack); }
        @Override public boolean canTakeItems(PlayerEntity player) {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return false;
            if (JustTeamsFabric.permissions().has(serverPlayer, JustTeamsPermissions.BYPASS_BANK_WITHDRAW)) return true;
            var member = team.getMember(player.getUuid());
            return member != null && member.canWithdraw();
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getUuid().equals(((PlayerInventory)player.getInventory()).player.getUuid())
                && team.hasElevatedPermissions(player.getUuid());
    }
}
