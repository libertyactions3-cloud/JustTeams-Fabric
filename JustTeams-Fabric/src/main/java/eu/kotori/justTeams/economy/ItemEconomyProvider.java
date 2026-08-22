package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Item-backed player economy using the configured emerald currency items.
 *
 * The default denominations intentionally mirror the existing server Skript:
 * emerald = 1, emerald block = 9, deepslate emerald ore = 81.
 * Withdrawals use the same low-to-high denomination preference and preserve
 * one lower denomination when a higher denomination is available. Overpayment
 * is returned as emerald blocks plus emeralds, matching the Skript behavior.
 */
public final class ItemEconomyProvider implements EconomyProvider {
    private static final int EMERALD_VALUE = 1;
    private static final int EMERALD_BLOCK_VALUE = 9;
    private static final int DEEPSLATE_EMERALD_ORE_VALUE = 81;

    @Override
    public String getCurrencyName() {
        return "Emeralds";
    }

    @Override
    public boolean isAvailable() {
        return !JustTeamsFabric.config().getCurrencyItems().isEmpty();
    }

    @Override
    public double getBalance(ServerPlayerEntity player) {
        if (player == null || !isAvailable()) return 0.0D;

        PlayerInventory inventory = player.getInventory();
        long total = 0L;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            int value = valueOf(stack.getItem());
            if (value > 0 && isConfigured(stack.getItem())) {
                total += (long) stack.getCount() * value;
            }
        }
        return total;
    }

    @Override
    public EconomyTransactionResult withdraw(ServerPlayerEntity player, double amount) {
        if (player == null || !isAvailable()) return EconomyTransactionResult.unavailable();
        if (!Double.isFinite(amount) || amount < 0.0D) return EconomyTransactionResult.invalidAmount();
        if (amount == 0.0D) return EconomyTransactionResult.success(0.0D);
        if (amount != Math.rint(amount)) return EconomyTransactionResult.invalidAmount();

        long price = (long) amount;
        if (getBalance(player) < price) return EconomyTransactionResult.insufficientFunds(amount);

        PlayerInventory inventory = player.getInventory();
        int emeralds = countItem(inventory, Items.EMERALD);
        int blocks = countItem(inventory, Items.EMERALD_BLOCK);
        int ore = countItem(inventory, Items.DEEPSLATE_EMERALD_ORE);

        long remaining = price;
        List<Removal> removals = new ArrayList<>();

        int usableEmeralds = emeralds;
        if (blocks > 0 && emeralds > 0 && emeralds <= remaining) {
            usableEmeralds = emeralds - 1;
        }
        int takeEmeralds = (int) Math.min(remaining, usableEmeralds);
        if (takeEmeralds > 0) {
            removals.add(new Removal(Items.EMERALD, takeEmeralds));
            remaining -= takeEmeralds;
        }

        int usableBlocks = blocks;
        if (ore > 0 && blocks > 0 && (long) blocks * EMERALD_BLOCK_VALUE <= remaining) {
            usableBlocks = blocks - 1;
        }
        int neededBlocks = ceilDiv(remaining, EMERALD_BLOCK_VALUE);
        int takeBlocks = Math.min(neededBlocks, usableBlocks);
        if (takeBlocks > 0) {
            removals.add(new Removal(Items.EMERALD_BLOCK, takeBlocks));
            remaining -= (long) takeBlocks * EMERALD_BLOCK_VALUE;
        }

        int neededOre = ceilDiv(remaining, DEEPSLATE_EMERALD_ORE_VALUE);
        int takeOre = Math.min(neededOre, ore);
        if (takeOre > 0) {
            removals.add(new Removal(Items.DEEPSLATE_EMERALD_ORE, takeOre));
            remaining -= (long) takeOre * DEEPSLATE_EMERALD_ORE_VALUE;
        }

        if (remaining > 0L) {
            return EconomyTransactionResult.insufficientFunds(amount);
        }

        for (Removal removal : removals) {
            removeItem(inventory, removal.item(), removal.amount());
        }

        long paid = price;
        for (Removal removal : removals) {
            paid += (long) removal.amount() * valueOf(removal.item());
        }
        // The previous line includes price once; recompute the actual paid value.
        paid = 0L;
        for (Removal removal : removals) {
            paid += (long) removal.amount() * valueOf(removal.item());
        }

        long change = paid - price;
        if (change > 0L) {
            EconomyTransactionResult changeResult = depositChange(player, change);
            if (!changeResult.successful()) {
                // This should not occur with the default denomination set because
                // every non-negative remainder can be represented by blocks + emeralds.
                for (Removal removal : removals) {
                    player.getInventory().offer(new ItemStack(removal.item(), removal.amount()), false);
                }
                return changeResult;
            }
        }

        return EconomyTransactionResult.success(price);
    }

    @Override
    public EconomyTransactionResult deposit(ServerPlayerEntity player, double amount) {
        if (player == null || !isAvailable()) return EconomyTransactionResult.unavailable();
        if (!Double.isFinite(amount) || amount < 0.0D) return EconomyTransactionResult.invalidAmount();
        if (amount != Math.rint(amount)) return EconomyTransactionResult.invalidAmount();
        if (amount == 0.0D) return EconomyTransactionResult.success(0.0D);

        long value = (long) amount;
        return depositChange(player, value);
    }

    private EconomyTransactionResult depositChange(ServerPlayerEntity player, long value) {
        if (value < 0L) return EconomyTransactionResult.invalidAmount();
        if (value == 0L) return EconomyTransactionResult.success(0.0D);

        if (!isConfigured(Items.EMERALD) || !isConfigured(Items.EMERALD_BLOCK)) {
            return EconomyTransactionResult.unavailable();
        }

        int blocks = (int) (value / EMERALD_BLOCK_VALUE);
        int emeralds = (int) (value % EMERALD_BLOCK_VALUE);
        PlayerInventory inventory = player.getInventory();

        if (blocks > 0) {
            inventory.offerOrDrop(new ItemStack(Items.EMERALD_BLOCK, blocks));
        }
        if (emeralds > 0) {
            inventory.offerOrDrop(new ItemStack(Items.EMERALD, emeralds));
        }
        return EconomyTransactionResult.success(value);
    }

    private static int countItem(PlayerInventory inventory, Item item) {
        if (!isConfigured(item)) return 0;
        int count = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItem(PlayerInventory inventory, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !stack.isOf(item)) continue;

            int remove = Math.min(remaining, stack.getCount());
            inventory.removeStack(slot, remove);
            remaining -= remove;
        }
        inventory.markDirty();
    }

    private static int ceilDiv(long value, int divisor) {
        if (value <= 0L) return 0;
        return (int) ((value + divisor - 1L) / divisor);
    }

    private static int valueOf(Item item) {
        if (item == Items.EMERALD) return EMERALD_VALUE;
        if (item == Items.EMERALD_BLOCK) return EMERALD_BLOCK_VALUE;
        if (item == Items.DEEPSLATE_EMERALD_ORE) return DEEPSLATE_EMERALD_ORE_VALUE;
        return 0;
    }

    private static boolean isConfigured(Item item) {
        Set<Item> configured = JustTeamsFabric.config().getCurrencyItems();
        return configured.contains(item);
    }

    private record Removal(Item item, int amount) {}
}
