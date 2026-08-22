package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/** Server-side 54-slot Team GUI matching the Paper gui.yml layout. */
public final class TeamMenuHandler extends ScreenHandler {
    private static final int[] MEMBER_SLOTS = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final Inventory menuInventory;
    private final UUID viewerUuid;
    private final Team team;
    private final TeamGuiManager.TeamMenuActionHandler actionHandler;

    public TeamMenuHandler(int syncId, PlayerInventory playerInventory, UUID viewerUuid, Team team,
                           TeamGuiManager.TeamMenuActionHandler actionHandler) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.menuInventory = new SimpleInventory(54);
        this.viewerUuid = viewerUuid;
        this.team = team;
        this.actionHandler = actionHandler;
        populate(playerInventory.player);
        for (int row = 0; row < 6; row++) for (int column = 0; column < 9; column++)
            addSlot(new MenuSlot(menuInventory, row * 9 + column, 8 + column * 18, 18 + row * 18));
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(PlayerInventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 198));
    }

    private void populate(PlayerEntity viewer) {
        ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < menuInventory.size(); i++) menuInventory.setStack(i, filler.copy());
        menuInventory.setStack(4, named(Items.NAME_TAG, "ᴛᴇᴀᴍ - " + team.getMembers().size() + "/∞"));
        List<TeamPlayer> members = team.getMembers();
        for (int i = 0; i < MEMBER_SLOTS.length && i < members.size(); i++) {
            TeamPlayer member = members.get(i);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(member.getPlayerUuid()));
            head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(resolveName(viewer, member.getPlayerUuid())));
            menuInventory.setStack(MEMBER_SLOTS[i], head);
        }
        menuInventory.setStack(8, named(Items.SOUL_LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇs"));
        menuInventory.setStack(7, named(Items.COMPASS, "ᴛᴇᴀᴍ ᴡᴀʀᴘs"));
        menuInventory.setStack(50, named(Items.SUNFLOWER, "ᴛᴇᴀᴍ ʙᴀɴᴋ"));
        menuInventory.setStack(47, named(Items.ENDER_PEARL, "ᴛᴇᴀᴍ ʜᴏᴍᴇ"));
        menuInventory.setStack(46, named(Items.ENDER_CHEST, "ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ"));
        menuInventory.setStack(49, named(Items.HOPPER, "sᴏʀᴛ ᴍᴇᴍʙᴇʀs"));
        menuInventory.setStack(52, named(Items.COMPARATOR, "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs"));
        ItemStack pvp = named(Items.DIAMOND_SWORD, "ᴘᴠᴘ sᴛᴀᴛᴜs");
        pvp.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(List.of(
                Text.literal("Toggle PvP between team members."), Text.literal(""),
                Text.literal("Currently: " + (team.isPvpEnabled() ? "Enabled" : "Disabled")),
                Text.literal(""), Text.literal("Click to toggle."))));
        menuInventory.setStack(45, pvp);
        menuInventory.setStack(53, named(team.isOwner(viewer.getUuid()) ? Items.TNT : Items.DARK_OAK_DOOR,
                team.isOwner(viewer.getUuid()) ? "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ" : "ʟᴇᴀᴠᴇ ᴛᴇᴀᴍ"));
    }

    private String resolveName(PlayerEntity viewer, UUID uuid) {
        if (viewer instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getEntityWorld().getServer();
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
            if (online != null) return online.getName().getString();
        }
        return uuid.toString().substring(0, 8);
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }

    @Override public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < menuInventory.size()) {
            if (actionHandler != null) actionHandler.handle(player, slotIndex, button, actionType, team, this);
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }
    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && team.isMember(viewerUuid); }
    public Inventory getMenuInventory() { return menuInventory; }
    public Team getTeam() { return team; }
    public int getPage() { return 0; }
    public void previousPage() { }
    public void nextPage() { }
    public void refresh() { sendContentUpdates(); }
    public void sendContentUpdates() { super.sendContentUpdates(); }

    private static final class MenuSlot extends Slot {
        private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { return false; }
    }
}