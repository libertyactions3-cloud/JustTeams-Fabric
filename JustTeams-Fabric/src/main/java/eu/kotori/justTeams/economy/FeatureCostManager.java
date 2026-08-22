package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Applies the verified 2.5.3 feature-cost model through the Fabric item economy. */
public final class FeatureCostManager {
    private FeatureCostManager() {}

    public static boolean charge(ServerPlayerEntity player, String feature) {
        if (player == null || feature == null || feature.isBlank()) return false;
        if (!JustTeamsFabric.config().isFeatureCostsEnabled()) return true;

        double cost = JustTeamsFabric.config().getFeatureCost(feature);
        if (cost <= 0.0D) return true;
        if (!Double.isFinite(cost) || cost != Math.rint(cost)) {
            player.sendMessage(Text.literal("The configured cost for " + feature + " is invalid for the item economy."), true);
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
