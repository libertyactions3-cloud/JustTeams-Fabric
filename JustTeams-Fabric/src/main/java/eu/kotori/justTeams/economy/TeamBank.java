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

    public TeamBank(Team team) {
        this(team, JustTeamsFabric.config().getCurrencyItems());
    }

    public TeamBank(Team team, Set<Item> currencyItems) {
        super(SLOT_COUNT);
        this.team = team;
        this.currencyItems = Set.copyOf(currencyItems);
    }

    public Team getTeam() { return team; }
    public Set<Item> getCurrencyItems() { return currencyItems; }
    public boolean isCurrency(ItemStack stack) { return !stack.isEmpty() && currencyItems.contains(stack.getItem()); }

    @Override public boolean canInsert(ItemStack stack) { return isCurrency(stack) && super.canInsert(stack); }
    @Override public boolean isValid(int slot, ItemStack stack) { return isCurrency(stack); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return team.isMember(player.getUuid()); }

    /** Returns the configured item-economy value of one currency item. */
    public static int currencyValue(Item item) {
        if (item == Items.DEEPSLATE_EMERALD_ORE) return 81;
        if (item == Items.EMERALD_BLOCK) return 9;
        if (item == Items.EMERALD) return 1;
        return 0;
    }

    /**
     * Withdraws an exact currency value from the team bank.
     * Returns false without changing the bank if no exact denomination combination exists.
     */
    public boolean tryWithdrawValue(long value) {
        if (value <= 0L) return true;
        int ore = count(Items.DEEPSLATE_EMERALD_ORE);
        int blocks = count(Items.EMERALD_BLOCK);
        int emeralds = count(Items.EMERALD);

        long target = value;
        int useOre = (int) Math.min(ore, target / 81L);
        target -= (long) useOre * 81L;
        int useBlocks = (int) Math.min(blocks, target / 9L);
        target -= (long) useBlocks * 9L;
        int useEmeralds = (int) Math.min(emeralds, target);

        if (target > useEmeralds) {
            // Greedy denomination use can fail when a smaller denomination is needed.
            // Search the bounded ore/block combinations (the largest denomination is only 81).
            useOre = -1;
            useBlocks = -1;
            useEmeralds = -1;
            outer:
            for (int o = Math.min(ore, (int) Math.min(Integer.MAX_VALUE, value / 81L)); o >= 0; o--) {
                long afterOre = value - (long) o * 81L;
                for (int b = Math.min(blocks, (int) Math.min(Integer.MAX_VALUE, afterOre / 9L)); b >= 0; b--) {
                    long remainder = afterOre - (long) b * 9L;
                    if (remainder <= emeralds) {
                        useOre = o;
                        useBlocks = b;
                        useEmeralds = (int) remainder;
                        break outer;
                    }
                }
            }
            if (useOre < 0) return false;
        }

        removeValue(Items.DEEPSLATE_EMERALD_ORE, useOre);
        removeValue(Items.EMERALD_BLOCK, useBlocks);
        removeValue(Items.EMERALD, useEmeralds);
        markDirty();
        return true;
    }

    private int count(Item item) {
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
            stack.decrement(removed);
            remaining -= removed;
            if (stack.isEmpty()) setStack(slot, ItemStack.EMPTY);
        }
    }

    /** Serializes occupied slots through the 1.21.11 ItemStack CODEC. */
    public NbtList toNbtList() {
        NbtList list = new NbtList();
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) continue;
            NbtElement encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
            if (!(encoded instanceof NbtCompound entry)) continue;
            entry.putInt("Slot", slot);
            list.add(entry);
        }
        return list;
    }

    /** Restores occupied slots from the serialized item-stack list. */
    public void readNbtList(NbtList list) {
        clear();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompoundOrEmpty(i);
            int slot = entry.getInt("Slot", -1);
            if (slot < 0 || slot >= size()) continue;
            ItemStack.CODEC.parse(NbtOps.INSTANCE, entry).result().ifPresent(stack -> { if (isCurrency(stack)) setStack(slot, stack); });
        }
        markDirty();
    }
}
