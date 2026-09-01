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
            ItemStack stack = getStack(slot); int value = currencyValue(stack.getItem());
            if (value > 0 && isCurrency(stack)) total += (long) stack.getCount() * value;
        }
        return total;
    }

    public boolean canWithdrawValue(long value) {
        if (value <= 0L || getTotalEmeraldValue() < value) return false;
        return planWithdrawal(value) != null;
    }

    /**
     * Withdraws a feature cost from the team bank. Denomination change remains in the team bank.
     * The operation is planned completely before any inventory mutation occurs.
     */
    public boolean tryWithdrawValue(long value) {
        if (value <= 0L) return true;
        int[] plan = planWithdrawal(value);
        if (plan == null) return false;
        if (!canFitChange(plan[3], plan[4])) return false;

        removeValue(Items.DEEPSLATE_EMERALD_ORE, plan[0]);
        removeValue(Items.EMERALD_BLOCK, plan[1]);
        removeValue(Items.EMERALD, plan[2]);
        addCurrency(Items.EMERALD_BLOCK, plan[3]);
        addCurrency(Items.EMERALD, plan[4]);
        markDirty();
        return true;
    }

    /** plan = [takeOre, takeBlocks, takeEmeralds, changeBlocks, changeEmeralds]. */
    private int[] planWithdrawal(long value) {
        if (value <= 0L) return new int[]{0, 0, 0, 0, 0};
        if (getTotalEmeraldValue() < value) return null;

        int oreAmt = countCurrency(Items.DEEPSLATE_EMERALD_ORE);
        int blockAmt = countCurrency(Items.EMERALD_BLOCK);
        int emeraldAmt = countCurrency(Items.EMERALD);
        long remaining = value;
        long paid = 0L;
        int takeEmeralds = 0;
        int takeBlocks = 0;
        int takeOre = 0;

        /* Emeralds: preserve one when a higher denomination exists and consuming all usable emeralds. */
        if (remaining > 0L) {
            int usableEmeralds = emeraldAmt;
            if (blockAmt > 0) {
                if (emeraldAmt >= remaining) {
                    if (emeraldAmt == remaining && emeraldAmt >= 1) usableEmeralds = emeraldAmt - 1;
                } else if (emeraldAmt >= 1) usableEmeralds = emeraldAmt - 1;
            }
            int neededEmeralds = (int) Math.min(remaining, usableEmeralds);
            if (neededEmeralds > 0) {
                takeEmeralds = neededEmeralds; paid += neededEmeralds; remaining -= neededEmeralds;
            }
        }

        /* Emerald blocks: preserve one when ore exists and using the available blocks would exhaust them. */
        if (remaining > 0L) {
            int usableBlocks = blockAmt;
            if (oreAmt > 0) {
                long blockValue = (long) blockAmt * 9L;
                if (blockValue >= remaining) {
                    if (blockValue == remaining && blockAmt >= 1) usableBlocks = blockAmt - 1;
                } else if (blockAmt >= 1) usableBlocks = blockAmt - 1;
            }
            int neededBlocks = (int) Math.min(usableBlocks, (remaining + 8L) / 9L);
            if (neededBlocks > 0) {
                takeBlocks = neededBlocks;
                long blockPaid = (long) neededBlocks * 9L;
                paid += blockPaid;
                remaining = Math.max(0L, remaining - blockPaid);
            }
        }

        /* Deepslate emerald ore is the highest denomination and final fallback. */
        if (remaining > 0L && oreAmt > 0) {
            long neededOre = (remaining + 80L) / 81L;
            if (neededOre > oreAmt || neededOre > Integer.MAX_VALUE) return null;
            takeOre = (int) neededOre;
            paid += neededOre * 81L;
            remaining = 0L;
        }

        if (remaining > 0L || paid < value) return null;
        long change = paid - value;
        int changeBlocks = (int) (change / 9L);
        int changeEmeralds = (int) (change % 9L);
        return new int[]{takeOre, takeBlocks, takeEmeralds, changeBlocks, changeEmeralds};
    }

    private boolean canFitChange(int blocks, int emeralds) {
        int emptySlots = 0;
        int blockSpace = 0;
        int emeraldSpace = 0;
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) {
                emptySlots++;
                continue;
            }
            if (stack.getItem() == Items.EMERALD_BLOCK) blockSpace += stack.getMaxCount() - stack.getCount();
            if (stack.getItem() == Items.EMERALD) emeraldSpace += stack.getMaxCount() - stack.getCount();
        }
        int blockStacksNeeded = Math.max(0, blocks - blockSpace + 63) / 64;
        int emeraldStacksNeeded = Math.max(0, emeralds - emeraldSpace + 63) / 64;
        return blockStacksNeeded + emeraldStacksNeeded <= emptySlots;
    }

    private void addCurrency(Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            ItemStack stack = getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getCount() < stack.getMaxCount()) {
                int add = Math.min(remaining, stack.getMaxCount() - stack.getCount()); stack.increment(add); remaining -= add;
            }
        }
        for (int slot = 0; slot < size() && remaining > 0; slot++) {
            if (!getStack(slot).isEmpty()) continue;
            int add = Math.min(remaining, 64); setStack(slot, new ItemStack(item, add)); remaining -= add;
        }
        if (remaining > 0) throw new IllegalStateException("Not enough team-bank inventory space for currency change.");
    }

    private int countCurrency(Item item) {
        int total = 0; for (int slot=0;slot<size();slot++){ItemStack stack=getStack(slot);if(stack.getItem()==item)total+=stack.getCount();}return total;
    }

    private void removeValue(Item item, int amount) {
        int remaining=amount;for(int slot=0;slot<size()&&remaining>0;slot++){ItemStack stack=getStack(slot);if(stack.getItem()!=item)continue;int removed=Math.min(remaining,stack.getCount());stack.decrement(removed);remaining-=removed;if(stack.isEmpty())setStack(slot,ItemStack.EMPTY);}
    }

    public NbtList toNbtList() {
        NbtList list=new NbtList();for(int slot=0;slot<size();slot++){ItemStack stack=getStack(slot);if(stack.isEmpty())continue;NbtElement encoded=ItemStack.CODEC.encodeStart(NbtOps.INSTANCE,stack).result().orElse(null);if(!(encoded instanceof NbtCompound entry))continue;entry.putInt("Slot",slot);list.add(entry);}return list;
    }

    public void readNbtList(NbtList list) {
        clear();for(int i=0;i<list.size();i++){NbtCompound entry=list.getCompoundOrEmpty(i);int slot=entry.getInt("Slot",-1);if(slot<0||slot>=size())continue;ItemStack.CODEC.parse(NbtOps.INSTANCE,entry).result().ifPresent(stack->{if(isCurrency(stack))setStack(slot,stack);});}markDirty();
    }
}
