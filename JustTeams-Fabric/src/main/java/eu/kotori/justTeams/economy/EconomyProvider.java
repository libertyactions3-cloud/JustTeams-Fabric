package eu.kotori.justTeams.economy;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Common economy boundary used by JustTeams features such as warps and the team bank.
 * Providers may represent normal server money or an in-game item currency.
 */
public interface EconomyProvider {
    /** Human-readable currency name used by GUI/messages. */
    String getCurrencyName();

    /** Whether this provider is currently usable by JustTeams. */
    boolean isAvailable();

    /** Returns the player's spendable balance in provider units. */
    double getBalance(ServerPlayerEntity player);

    /** Attempts to remove the requested amount from a player's balance. */
    EconomyTransactionResult withdraw(ServerPlayerEntity player, double amount);

    /** Attempts to add the requested amount to a player's balance. */
    EconomyTransactionResult deposit(ServerPlayerEntity player, double amount);

    /** Returns a display-friendly representation of an amount. */
    default String format(double amount) {
        if (amount == Math.rint(amount)) return String.format("%.0f %s", amount, getCurrencyName());
        return String.format("%.2f %s", amount, getCurrencyName());
    }
}
