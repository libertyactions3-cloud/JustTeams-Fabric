package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Applies the verified 2.5.3 feature-cost model through the Fabric item economy. */
public final class FeatureCostManager {
    private FeatureCostManager() {}

    public static boolean canAfford(ServerPlayerEntity player, String feature) {
        if (player == null || feature == null || feature.isBlank()) return false;
        if (!JustTeamsFabric.config().isFeatureCostsEnabled()) return true;
        return canAfford(player, JustTeamsFabric.config().getFeatureCost(feature), feature);
    }

    public static boolean charge(ServerPlayerEntity player, String feature) {
        if (player == null || feature == null || feature.isBlank()) return false;
        if (!JustTeamsFabric.config().isFeatureCostsEnabled()) return true;
        return charge(player, JustTeamsFabric.config().getFeatureCost(feature), feature);
    }

    public static boolean canAfford(ServerPlayerEntity player, double cost, String feature) {
        if (player == null) return false;
        if (cost <= 0.0D) return true;
        if (!Double.isFinite(cost) || cost != Math.rint(cost)) {
            player.sendMessage(Text.literal("The configured cost for " + feature + " is invalid for the item economy."), true);
            return false;
        }

        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        TeamPlayer member = team == null ? null : team.getMember(player.getUuid());
        if (member != null && member.canUseAutoBank() && member.isAutoBankEnabled()) {
            if (team.getBank().canWithdrawValue((long) cost)) return true;
            player.sendMessage(Text.literal("The team bank does not contain enough currency for this cost."), true);
            return false;
        }

        if (!JustTeamsFabric.economy().isAvailable()) {
            player.sendMessage(Text.literal("The item economy is unavailable."), true);
            return false;
        }
        if (JustTeamsFabric.economy().getBalance(player) >= cost) return true;
        player.sendMessage(Text.literal(
                "You do not have enough " + JustTeamsFabric.economy().getCurrencyName()
                        + " (cost: " + JustTeamsFabric.economy().format(cost) + ")."), true);
        return false;
    }

    public static boolean charge(ServerPlayerEntity player, double cost, String feature) {
        if (player == null) return false;
        if (cost <= 0.0D) return true;
        if (!Double.isFinite(cost) || cost != Math.rint(cost)) {
            player.sendMessage(Text.literal("The configured cost for " + feature + " is invalid for the item economy."), true);
            return false;
        }

        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        TeamPlayer member = team == null ? null : team.getMember(player.getUuid());
        if (member != null && member.canUseAutoBank() && member.isAutoBankEnabled()) {
            // AutoBank is a transaction against the team's item bank. Any denomination
            // overpayment/change stays in the team bank, exactly like bank-side currency handling.
            if (team.getBank().tryWithdrawValue((long) cost)) {
                TeamBankLogManager.record(player.getEntityWorld().getServer(), team, player,
                        (long) cost, TeamBankLogManager.Kind.AUTOBANK, feature);
                return true;
            }
            player.sendMessage(Text.literal("The team bank does not contain enough currency for this cost."), true);
            return false;
        }

        EconomyTransactionResult result = JustTeamsFabric.economy().withdraw(player, cost);
        if (result.successful()) return true;

        switch (result.reason()) {
            case INSUFFICIENT_FUNDS -> player.sendMessage(Text.literal(
                    "You do not have enough " + JustTeamsFabric.economy().getCurrencyName()
                            + " (cost: " + JustTeamsFabric.economy().format(cost) + ")."), true);
            case UNAVAILABLE -> player.sendMessage(Text.literal("The item economy is unavailable."), true);
            case INVALID_AMOUNT -> player.sendMessage(Text.literal("The configured feature cost is invalid."), true);
            default -> player.sendMessage(Text.literal("Unable to pay the feature cost."), true);
        }
        return false;
    }
}
