package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Team-owned item bank backed by a real Minecraft inventory. */
public final class TeamBank extends SimpleInventory {
    public static final int SLOT_COUNT = 54;
    private final Team team;
    private final Set<Item> currencyItems;

    public TeamBank(Team team) { this(team, JustTeamsFabric.config().getCurrencyItems()); }
    public TeamBank(Team team, Set<Item> currencyItems) { super(SLOT_COUNT); this.team = team; this.currencyItems = Set.copyOf(currencyItems); }
    public Team getTeam() { return team; }
    public Set<Item> getCurrencyItems() { return currencyItems; }
    public boolean isCurrency(ItemStack stack) { return !stack.isEmpty() && currencyItems.contains(stack.getItem()); }
    @Override public boolean canInsert(ItemStack stack) { return isCurrency(stack) && super.canInsert(stack); }
    @Override public boolean isValid(int slot, ItemStack stack) { return isCurrency(stack); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return team.isMember(player.getUuid()); }

    public static int currencyValue(Item item) {
        if (item == Items.DEEPSLATE_EMERALD_ORE) return 81;
        if (item == Items.EMERALD_BLOCK) return 9;
        if (item == Items.EMERALD) return 1;
        return 0;
    }

    public long getTotalEmeraldValue() {
        long total = 0L;
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot); int value = currencyValue(stack.getItem());
            if (value > 0 && isCurrency(stack)) total += (long) stack.getCount() * value;
        }
        return total;
    }

    public boolean canWithdrawValue(long value) {
        if (value <= 0L || getTotalEmeraldValue() < value) return false;
        return planWithdrawal(value) != null;
    }

    public boolean tryWithdrawValue(long value) {
        if (value <= 0L) return true;
        int[] plan = planWithdrawal(value);
        if (plan == null) return false;
        removeValue(Items.DEEPSLATE_EMERALD_ORE, plan[0]);
        removeValue(Items.EMERALD_BLOCK, plan[1]);
        removeValue(Items.EMERALD, plan[2]);
        addCurrency(Items.EMERALD_BLOCK, plan[3]);
        addCurrency(Items.EMERALD, plan[4]);
        markDirty();
        return true;
    }

    /**
     * Pays from emeralds first, then blocks, then ore. If a higher denomination
     * has to cover a remainder, the excess is returned to this same bank as
     * blocks and emeralds. This mirrors the server's 81/9/1 currency semantics.
     *
     * plan = [takeOre, takeBlocks, takeEmeralds, changeBlocks, changeEmeralds]
     */
    private int[] planWithdrawal(long value) {
        if (value <= 0L) return new int[]{0, 0, 0, 0, 0};
        long total = getTotalEmeraldValue();
        if (total < value) return null;

        int ore = countCurrency(Items.DEEPSLATE_EMERALD_ORE);
        int blocks = countCurrency(Items.EMERALD_BLOCK);
        int emeralds = countCurrency(Items.EMERALD);

        long remaining = value;
        int takeEmeralds = (int) Math.min(emeralds, remaining);
        remaining -= takeEmeralds;

        int takeBlocks = 0;
        int changeBlocks = 0;
        int changeEmeralds = 0;

        if (remaining > 0L && blocks > 0) {
            int neededBlocks = (int) Math.min(Integer.MAX_VALUE, (remaining + 8L) / 9L);
            takeBlocks = Math.min(blocks, neededBlocks);
            long paidByBlocks = (long) takeBlocks * 9L;
            if (paidByBlocks >= remaining) {
                long change = paidByBlocks - remaining;
                changeBlocks = (int) (change / 9L);
                changeEmeralds = (int) (change % 9L);
                remaining = 0L;
            } else {
                remaining -= paidByBlocks;
            }
        }

        int takeOre = 0;
        if (remaining > 0L && ore > 0) {
            long neededOreLong = (remaining + 80L) / 81L;
            if (neededOreLong > ore || neededOreLong > Integer.MAX_VALUE) return null;
            takeOre = (int) neededOreLong;
            long paidByOre = neededOreLong * 81L;
            long change = paidByOre - remaining;
            changeBlocks += (int) (change / 9L);
            changeEmeralds += (int) (change % 9L);
            remaining = 0L;
        }

        if (remaining > 0L) return null;
        if (!canApplyPlan(takeOre, takeBlocks, takeEmeralds, changeBlocks, changeEmeralds)) return null;
        return new int[]{takeOre, takeBlocks, takeEmeralds, changeBlocks, changeEmeralds};
    }

    private boolean canApplyPlan(int takeOre, int takeBlocks, int takeEmeralds, int changeBlocks, int changeEmeralds) {
        List<ItemStack> simulated = new ArrayList<>(size());
        for (int slot = 0; slot < size(); slot++) simulated.add(getStack(slot).copy());
        simulateRemove(simulated, Items.DEEPSLATE_EMERALD_ORE, takeOre);
        simulateRemove(simulated, Items.EMERALD_BLOCK, takeBlocks);
        simulateRemove(simulated, Items.EMERALD, takeEmeralds);
        return simulateAdd(simulated, Items.EMERALD_BLOCK, changeBlocks) && simulateAdd(simulated, Items.EMERALD, changeEmeralds);
    }

    private static void simulateRemove(List<ItemStack> inventory, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : inventory) {
            if (remaining <= 0) break;
            if (stack.getItem() != item) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.decrement(removed); remaining -= removed;
        }
    }

    private static boolean simulateAdd(List<ItemStack> inventory, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : inventory) {
            if (remaining <= 0) return true;
            if (!stack.isEmpty() && stack.getItem() == item) {
                int add = Math.min(remaining, stack.getMaxCount() - stack.getCount());
                stack.increment(add); remaining -= add;
            }
        }
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) continue;
            int add = Math.min(remaining, 64);
            inventory.set(i, new ItemStack(item, add)); remaining -= add;
        }
        return remaining <= 0;
    }

    private void addCurrency(Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            ItemStack stack = getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getCount() < stack.getMaxCount()) {
                int add = Math.min(remaining, stack.getMaxCount() - stack.getCount());
                stack.increment(add); remaining -= add;
            }
        }
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            if (!getStack(slot).isEmpty()) continue;
            int add = Math.min(remaining, 64);
            setStack(slot, new ItemStack(item, add)); remaining -= add;
        }
        if (remaining > 0) throw new IllegalStateException("Not enough team-bank inventory space for currency change.");
    }

    private int countCurrency(Item item) {
        int total = 0;
        for (int slot = 0; slot < size(); slot++) { ItemStack stack = getStack(slot); if (stack.getItem() == item) total += stack.getCount(); }
        return total;
    }

    private void removeValue(Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            ItemStack stack = getStack(slot); if (stack.getItem() != item) continue;
            int removed = Math.min(remaining, stack.getCount()); stack.decrement(removed); remaining -= removed;
            if (stack.isEmpty()) setStack(slot, ItemStack.EMPTY);
        }
    }

    public NbtList toNbtList() {
        NbtList list = new NbtList();
        for (int slot = 0; slot < size(); slot++) { ItemStack stack = getStack(slot); if (stack.isEmpty()) continue; NbtElement encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null); if (!(encoded instanceof NbtCompound entry)) continue; entry.putInt("Slot", slot); list.add(entry); }
        return list;
    }

    public void readNbtList(NbtList list) {
        clear();
        for (int i = 0; i < list.size(); i++) { NbtCompound entry = list.getCompoundOrEmpty(i); int slot = entry.getInt("Slot", -1); if (slot < 0 || slot >= size()) continue; ItemStack.CODEC.parse(NbtOps.INSTANCE, entry).result().ifPresent(stack -> { if (isCurrency(stack)) setStack(slot, stack); }); }
        markDirty();
    }
}
