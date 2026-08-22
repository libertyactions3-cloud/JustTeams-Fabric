package eu.kotori.justTeams.economy;

/** Result of an economy operation, with a stable reason for user-facing handling. */
public record EconomyTransactionResult(boolean successful, Reason reason, double amount) {
    public enum Reason {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        UNAVAILABLE,
        INVALID_AMOUNT
    }

    public static EconomyTransactionResult success(double amount) {
        return new EconomyTransactionResult(true, Reason.SUCCESS, amount);
    }

    public static EconomyTransactionResult insufficientFunds(double amount) {
        return new EconomyTransactionResult(false, Reason.INSUFFICIENT_FUNDS, amount);
    }

    public static EconomyTransactionResult unavailable() {
        return new EconomyTransactionResult(false, Reason.UNAVAILABLE, 0.0D);
    }

    public static EconomyTransactionResult invalidAmount() {
        return new EconomyTransactionResult(false, Reason.INVALID_AMOUNT, 0.0D);
    }
}
