package eu.kotori.justTeams.economy;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    public Team getTeam() {
        return team;
    }

    public Set<Item> getCurrencyItems() {
        return currencyItems;
    }

    public boolean isCurrency(ItemStack stack) {
        return !stack.isEmpty() && currencyItems.contains(stack.getItem());
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return isCurrency(stack) && super.canInsert(stack);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isCurrency(stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return team.isMember(player.getUuid());
    }

    /**
     * Serializes the occupied slots through the 1.21.11 ItemStack CODEC.
     * This avoids relying on ItemStack helper names that vary between mappings.
     */
    public NbtList toNbtList() {
        NbtList list = new NbtList();
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) continue;

            NbtElement encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                    .result()
                    .orElse(null);
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

            ItemStack.CODEC.parse(NbtOps.INSTANCE, entry)
                    .result()
                    .ifPresent(stack -> {
                        if (isCurrency(stack)) setStack(slot, stack);
                    });
        }
        markDirty();
    }
}
