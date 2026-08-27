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
            ItemStack stack = getStack(slot);
            int value = currencyValue(stack.getItem());
            if (value > 0 && isCurrency(stack)) total += (long) stack.getCount() * value;
        }
        return total;
    }

    public boolean canWithdrawValue(long value) {
        if (value <= 0L) return true;
        if (findWithdrawal(value) != null) return true;
        return canMakeOreChange(value);
    }

    public boolean tryWithdrawValue(long value) {
        if (value <= 0L) return true;
        int[] use = findWithdrawal(value);
        if (use != null) {
            removeValue(Items.DEEPSLATE_EMERALD_ORE, use[0]);
            removeValue(Items.EMERALD_BLOCK, use[1]);
            removeValue(Items.EMERALD, use[2]);
            markDirty();
            return true;
        }
        return tryWithdrawUsingOreChange(value);
    }

    /**
     * Same denomination rule as the player item economy: when the bank has
     * only deepslate emerald ore, break the minimum number of ore items and
     * retain the remainder as emerald blocks + emeralds inside the team bank.
     * The bank therefore loses exactly the requested value rather than the
     * whole ore denomination.
     */
    private boolean tryWithdrawUsingOreChange(long value) {
        int ore = countCurrency(Items.DEEPSLATE_EMERALD_ORE);
        int blocks = countCurrency(Items.EMERALD_BLOCK);
        int emeralds = countCurrency(Items.EMERALD);
        if (blocks != 0 || emeralds != 0 || ore <= 0) return false;
        long oreNeededLong = (value + 80L) / 81L;
        if (oreNeededLong > ore || oreNeededLong > Integer.MAX_VALUE) return false;
        long change = oreNeededLong * 81L - value;
        int changeBlocks = (int) (change / 9L);
        int changeEmeralds = (int) (change % 9L);
        if (!canFitChange(Items.EMERALD_BLOCK, changeBlocks, oreNeededLong)
                || !canFitChange(Items.EMERALD, changeEmeralds, oreNeededLong)) return false;

        removeValue(Items.DEEPSLATE_EMERALD_ORE, (int) oreNeededLong);
        addCurrency(Items.EMERALD_BLOCK, changeBlocks);
        addCurrency(Items.EMERALD, changeEmeralds);
        markDirty();
        return true;
    }

    private boolean canMakeOreChange(long value) {
        int ore = countCurrency(Items.DEEPSLATE_EMERALD_ORE);
        int blocks = countCurrency(Items.EMERALD_BLOCK);
        int emeralds = countCurrency(Items.EMERALD);
        if (blocks != 0 || emeralds != 0 || ore <= 0) return false;
        long oreNeeded = (value + 80L) / 81L;
        if (oreNeeded > ore) return false;
        long change = oreNeeded * 81L - value;
        return canFitChange(Items.EMERALD_BLOCK, (int) (change / 9L), oreNeeded)
                && canFitChange(Items.EMERALD, (int) (change % 9L), oreNeeded);
    }

    private boolean canFitChange(Item item, int amount, long oreRemoved) {
        if (amount <= 0) return true;
        int existingCapacity = 0;
        int emptySlots = 0;
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) emptySlots++;
            else if (stack.getItem() == item) existingCapacity += stack.getMaxCount() - stack.getCount();
        }
        int removedEmptySlots = 0;
        int remainingOre = countCurrency(Items.DEEPSLATE_EMERALD_ORE) - (int) Math.min(Integer.MAX_VALUE, oreRemoved);
        if (remainingOre == 0) removedEmptySlots = 1;
        emptySlots += removedEmptySlots;
        int remaining = Math.max(0, amount - existingCapacity);
        int neededSlots = (remaining + 63) / 64;
        return neededSlots <= emptySlots;
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

    private int[] findWithdrawal(long value) {
        if (value <= 0L) return new int[]{0, 0, 0};
        int ore = countCurrency(Items.DEEPSLATE_EMERALD_ORE);
        int blocks = countCurrency(Items.EMERALD_BLOCK);
        int emeralds = countCurrency(Items.EMERALD);
        int maxOre = (int) Math.min(ore, Math.min(Integer.MAX_VALUE, value / 81L));
        for (int o = maxOre; o >= 0; o--) {
            long afterOre = value - (long) o * 81L;
            int maxBlocks = (int) Math.min(blocks, Math.min(Integer.MAX_VALUE, afterOre / 9L));
            for (int b = maxBlocks; b >= 0; b--) {
                long remainder = afterOre - (long) b * 9L;
                if (remainder >= 0L && remainder <= emeralds) return new int[]{o, b, (int) remainder};
            }
        }
        return null;
    }

    private int countCurrency(Item item) {
        int total = 0;
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot);
            if (stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private void removeValue(Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.getItem() != item) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.decrement(removed); remaining -= removed;
            if (stack.isEmpty()) setStack(slot, ItemStack.EMPTY);
        }
    }

    public NbtList toNbtList() {
        NbtList list = new NbtList();
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot); if (stack.isEmpty()) continue;
            NbtElement encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
            if (!(encoded instanceof NbtCompound entry)) continue;
            entry.putInt("Slot", slot); list.add(entry);
        }
        return list;
    }

    public void readNbtList(NbtList list) {
        clear();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompoundOrEmpty(i);
            int slot = entry.getInt("Slot", -1); if (slot < 0 || slot >= size()) continue;
            ItemStack.CODEC.parse(NbtOps.INSTANCE, entry).result().ifPresent(stack -> { if (isCurrency(stack)) setStack(slot, stack); });
        }
        markDirty();
    }
}
