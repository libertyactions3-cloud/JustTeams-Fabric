package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Server-side 54-slot Team GUI matching the requested Fabric layout and the 2.5.3 presentation. */
public final class TeamMenuHandler extends ScreenHandler {
    private static final int[] MEMBER_SLOTS = {
            37, 38, 39, 40, 41, 42, 43,
            28, 29, 30, 31, 32, 33, 34,
            19, 20, 21, 22, 23, 24, 25
    };
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;
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
        for (int i = 0; i < menuInventory.size(); i++) menuInventory.setStack(i, ItemStack.EMPTY);

        List<TeamPlayer> members = orderedMembers();
        for (int i = 0; i < MEMBER_SLOTS.length && i < members.size(); i++) {
            menuInventory.setStack(MEMBER_SLOTS[i], createMemberHead(viewer, members.get(i)));
        }

        menuInventory.setStack(8, namedGradient(Items.SOUL_LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs"));
        menuInventory.setStack(7, namedGradient(Items.COMPASS, "ᴛᴇᴀᴍ ᴡᴀʀᴘs"));

        ItemStack bank = namedGradient(Items.SUNFLOWER, "ᴛᴇᴀᴍ ʙᴀɴᴋ");
        bank.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Balance: ", String.format("%.2f", team.getBalance()), Formatting.GRAY, Formatting.WHITE),
                plainLine("", Formatting.GRAY),
                plainLine("Click to manage the bank.", Formatting.YELLOW))));
        menuInventory.setStack(50, bank);

        ItemStack home = namedGradient(Items.ENDER_PEARL, "ᴛᴇᴀᴍ ʜᴏᴍᴇ");
        home.set(DataComponentTypes.LORE, new LoreComponent(team.getHome() != null
                ? List.of(
                        plainLine("Click to teleport to your team's home.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Click to teleport!", Formatting.YELLOW))
                : List.of(
                        plainLine("Click to teleport to your team's home.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Home not set.", Formatting.RED))));
        menuInventory.setStack(47, home);

        ItemStack enderChest = namedGradient(Items.ENDER_CHEST, "ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ");
        enderChest.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("A shared inventory for your team.", Formatting.GRAY),
                plainLine("", Formatting.GRAY),
                plainLine("Click to open the ender chest.", Formatting.YELLOW))));
        menuInventory.setStack(46, enderChest);

        ItemStack sort = namedGradient(Items.HOPPER, "sᴏʀᴛ ᴍᴇᴍʙᴇʀs");
        sort.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to change the sorting.", Formatting.GRAY),
                plainLine("", Formatting.GRAY),
                sortLine("Join Date", true),
                sortLine("Alphabetical", false),
                sortLine("Online Status", false))));
        menuInventory.setStack(49, sort);

        ItemStack settings = namedGradient(Items.COMPARATOR, "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs");
        settings.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Click to manage team settings.", Formatting.YELLOW))));
        menuInventory.setStack(52, settings);

        ItemStack pvp = namedGradient(Items.DIAMOND_SWORD, "ᴘᴠᴘ sᴛᴀᴛᴜs");
        pvp.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plainLine("Toggle PvP between team members.", Formatting.GRAY),
                plainLine("", Formatting.GRAY),
                composeLine("Currently: ", team.isPvpEnabled() ? "Enabled" : "Disabled",
                        Formatting.GRAY, team.isPvpEnabled() ? Formatting.GREEN : Formatting.RED),
                plainLine("", Formatting.GRAY),
                plainLine("Click to toggle.", Formatting.YELLOW))));
        menuInventory.setStack(45, pvp);

        boolean owner = team.isOwner(viewer.getUuid());
        ItemStack leaveOrDisband = namedColored(
                owner ? Items.TNT : Items.DARK_OAK_DOOR,
                owner ? "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ" : "ʟᴇᴀᴠᴇ ᴛᴇᴀᴍ",
                Formatting.RED,
                true);
        leaveOrDisband.set(DataComponentTypes.LORE, new LoreComponent(owner
                ? List.of(
                        plainLine("Permanently deletes the team.", Formatting.GRAY),
                        plainLine("This action cannot be undone!", Formatting.DARK_RED))
                : List.of(
                        plainLine("Leave the team and return to the lobby.", Formatting.GRAY),
                        plainLine("This action cannot be undone!", Formatting.RED))));
        menuInventory.setStack(53, leaveOrDisband);
    }

    private List<TeamPlayer> orderedMembers() {
        TeamPlayer owner = team.getMember(team.getOwnerUuid());
        List<TeamPlayer> remaining = new ArrayList<>(team.getMembers());
        remaining.removeIf(member -> member.getPlayerUuid().equals(team.getOwnerUuid()));
        remaining.sort(Comparator.comparing(TeamPlayer::getJoinDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<TeamPlayer> result = new ArrayList<>();
        if (owner != null) result.add(owner);
        result.addAll(remaining);
        return result;
    }

    private ItemStack createMemberHead(PlayerEntity viewer, TeamPlayer member) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(member.getPlayerUuid()));

        MinecraftServer server = viewer instanceof ServerPlayerEntity serverPlayer
                ? serverPlayer.getEntityWorld().getServer() : null;
        ServerPlayerEntity online = server == null ? null
                : server.getPlayerManager().getPlayer(member.getPlayerUuid());
        boolean isOnline = online != null;
        String playerName = isOnline ? online.getName().getString()
                : member.getPlayerUuid().toString().substring(0, 8);

        MutableText name = Text.empty();
        int statusColor = isOnline ? 0x00FF00 : 0xFF4444;
        name.append(Text.literal("● ").setStyle(Style.EMPTY.withColor(statusColor).withItalic(false)));
        String roleIcon = switch (member.getRole()) {
            case OWNER -> "★ ";
            case CO_OWNER -> "◆ ";
            case MEMBER -> "● ";
        };
        name.append(Text.literal(roleIcon).setStyle(Style.EMPTY
                .withColor(roleColor(member.getRole()))
                .withItalic(false)));
        if (isOnline) {
            name.append(gradientText(playerName, false));
        } else {
            name.append(Text.literal(playerName).setStyle(Style.EMPTY
                    .withColor(0x808080)
                    .withItalic(false)));
        }
        head.set(DataComponentTypes.CUSTOM_NAME, name);

        String roleName = switch (member.getRole()) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-Owner";
            case MEMBER -> "Member";
        };
        String joined = member.getJoinDate() == null ? "Unknown"
                : DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate());
        String serverName = isOnline ? "Local" : "Offline";
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                composeLine("Role: ", roleName, Formatting.GRAY, Formatting.WHITE),
                composeLine("Joined: ", joined, Formatting.GRAY, Formatting.WHITE),
                composeLine("Server: ", serverName, Formatting.GRAY, Formatting.WHITE))));

        if (member.getRole() == TeamRole.OWNER) {
            head.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return head;
    }

    private static int roleColor(TeamRole role) {
        return switch (role) {
            case OWNER -> 0xFFD700;
            case CO_OWNER -> 0xFF6B6B;
            case MEMBER -> 0xFFFFFF;
        };
    }

    private static MutableText gradientText(String value, boolean bold) {
        MutableText result = Text.empty();
        if (value.isEmpty()) return result;
        int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
        int index = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            int rgb = interpolate(PRIMARY_START, PRIMARY_END, (double) index / length);
            MutableText charText = Text.literal(new String(Character.toChars(codePoint)))
                    .setStyle(Style.EMPTY.withColor(rgb).withBold(bold).withItalic(false));
            result.append(charText);
            offset += Character.charCount(codePoint);
            index++;
        }
        return result;
    }

    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
        return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
    }

    private static MutableText sortLine(String name, boolean selected) {
        return Text.literal("▪ " + name).setStyle(Style.EMPTY
                .withColor(selected ? 0x00FF00 : 0x808080)
                .withItalic(false));
    }

    private static MutableText plainLine(String text, Formatting color) {
        return Text.literal(text)
                .formatted(color)
                .styled(style -> style.withItalic(false));
    }

    private static ItemStack namedGradient(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name, true));
        return stack;
    }

    private static ItemStack namedColored(Item item, String name, Formatting color, boolean bold) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color).styled(style -> style
                .withBold(bold)
                .withItalic(false)));
        return stack;
    }

    private static int interpolate(int start, int end, double t) {
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        int r = (int) Math.round(sr + (er - sr) * t);
        int g = (int) Math.round(sg + (eg - sg) * t);
        int b = (int) Math.round(sb + (eb - sb) * t);
        return (r << 16) | (g << 8) | b;
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