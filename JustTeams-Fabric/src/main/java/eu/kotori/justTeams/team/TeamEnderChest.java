package eu.kotori.justTeams.team;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Shared, persistent inventory owned by a JustTeams team. */
public final class TeamEnderChest extends SimpleInventory {
    private final Team team;
    private final Set<UUID> viewers = new LinkedHashSet<>();
    private boolean loading;
    private Runnable saveCallback;

    public TeamEnderChest(Team team, int rows) {
        super(rows * 9);
        this.team = team;
    }

    public Team getTeam() { return team; }

    public int getRows() { return size() / 9; }

    public Set<UUID> getViewers() { return Set.copyOf(viewers); }

    public void addViewer(UUID playerUuid) { viewers.add(playerUuid); }

    public void removeViewer(UUID playerUuid) { viewers.remove(playerUuid); }

    public boolean hasViewers() { return !viewers.isEmpty(); }

    public void setSaveCallback(Runnable saveCallback) { this.saveCallback = saveCallback; }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!loading && saveCallback != null) saveCallback.run();
    }

    /** Serializes occupied slots using the ItemStack codec available in the project's 1.21.11 mappings. */
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

    /** Restores occupied slots from serialized item-stack NBT. */
    public void readNbtList(NbtList list) {
        loading = true;
        try {
            clear();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompoundOrEmpty(i);
                int slot = entry.getInt("Slot", -1);
                if (slot < 0 || slot >= size()) continue;
                ItemStack.CODEC.parse(NbtOps.INSTANCE, entry)
                        .result()
                        .ifPresent(stack -> setStack(slot, stack));
            }
        } finally {
            loading = false;
        }
        super.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return team.isMember(player.getUuid());
    }
}
