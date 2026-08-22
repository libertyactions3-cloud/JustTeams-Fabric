package eu.kotori.justTeams.economy;

import net.minecraft.server.network.ServerPlayerEntity;

/** Safe no-op provider used when the configured economy integration is unavailable. */
public final class UnavailableEconomyProvider implements EconomyProvider {
    @Override
    public String getCurrencyName() {
        return "currency";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public double getBalance(ServerPlayerEntity player) {
        return 0.0D;
    }

    @Override
    public EconomyTransactionResult withdraw(ServerPlayerEntity player, double amount) {
        return EconomyTransactionResult.unavailable();
    }

    @Override
    public EconomyTransactionResult deposit(ServerPlayerEntity player, double amount) {
        return EconomyTransactionResult.unavailable();
    }
}
