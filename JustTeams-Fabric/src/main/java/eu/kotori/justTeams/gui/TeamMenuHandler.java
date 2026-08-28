package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamSortType;
import eu.kotori.justTeams.util.PlayerNameResolver;
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

/** Server-side 54-slot Team GUI matching the requested Fabric layout. */
public final class TeamMenuHandler extends ScreenHandler {
    private static final int[] MEMBER_SLOTS = {9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44};
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;
    private final Inventory menuInventory;
    private final UUID viewerUuid;
    private final Team team;
    private final TeamGuiManager.TeamMenuActionHandler actionHandler;

    public TeamMenuHandler(int syncId, PlayerInventory playerInventory, UUID viewerUuid, Team team, TeamGuiManager.TeamMenuActionHandler actionHandler) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.menuInventory = new SimpleInventory(54);
        this.viewerUuid = viewerUuid;
        this.team = team;
        this.actionHandler = actionHandler;
        populate(playerInventory.player);
        for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++) addSlot(new MenuSlot(menuInventory, row * 9 + col, 8 + col * 18, 18 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
    }

    private void populate(PlayerEntity viewer) {
        ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) menuInventory.setStack(slot, filler.copy());
        for (int slot = 9; slot < 45; slot++) menuInventory.setStack(slot, ItemStack.EMPTY);
        List<TeamPlayer> members = orderedMembers(viewer);
        for (int i = 0; i < MEMBER_SLOTS.length && i < members.size(); i++) menuInventory.setStack(MEMBER_SLOTS[i], createMemberHead(viewer, members.get(i)));

        boolean elevated = team.hasElevatedPermissions(viewer.getUuid());
        menuInventory.setStack(6, itemWithLore(namedGradient(Items.WRITABLE_BOOK, "ʙᴀɴᴋ ʟᴏɢs"), List.of(plainLine("View team bank transaction logs.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to view logs.", Formatting.YELLOW))));
        menuInventory.setStack(7, itemWithLore(namedGradient(Items.COMPASS, "ᴛᴇᴀᴍ ᴡᴀʀᴘs"), List.of(plainLine("Manage your team's warps.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to view warps.", Formatting.YELLOW))));
        menuInventory.setStack(8, itemWithLore(elevated ? namedGradient(Items.SOUL_LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs") : namedColored(Items.LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs", Formatting.RED, true), List.of(plainLine(elevated ? "View pending requests to join your team." : "Only the owner or co-owners can access this.", elevated ? Formatting.GRAY : Formatting.RED))));

        ItemStack bank = namedGradient(Items.SUNFLOWER, "ᴛᴇᴀᴍ ʙᴀɴᴋ");
        bank.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(team.getBank().getTotalEmeraldValue() + " total emeralds", Formatting.GREEN), plainLine("", Formatting.GRAY), plainLine("Click to manage the bank.", Formatting.YELLOW))));
        menuInventory.setStack(50, bank);
        ItemStack home = namedGradient(Items.ENDER_PEARL, "ᴛᴇᴀᴍ ʜᴏᴍᴇ");
        home.set(DataComponentTypes.LORE, new LoreComponent(team.getHome() == null ? List.of(plainLine("Click to teleport to your team's home.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Home not set.", Formatting.RED)) : List.of(plainLine("Click to teleport to your team's home.", Formatting.GRAY), plainLine("", Formatting.GRAY), plainLine("Click to teleport!", Formatting.YELLOW))));
        menuInventory.setStack(47, home);

        TeamPlayer viewerMember = team.getMember(viewer.getUuid());
        boolean enderPermission = viewerMember != null && viewerMember.canUseEnderChest();
        ItemStack ender = enderPermission ? namedGradient(Items.ENDER_CHEST, "ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ") : namedColored(Items.GRAY_DYE, "ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ ⨯ ʟᴏᴄᴋᴇᴅ", Formatting.DARK_GRAY, true);
        ender.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(enderPermission ? "A shared inventory for your team." : "You do not have permission for the team ender chest.", Formatting.GRAY))));
        menuInventory.setStack(46, ender);
        updateSortStack(team.getCurrentSortType());
        ItemStack settings = elevated ? namedGradient(Items.COMPARATOR, "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs") : namedColored(Items.COMPARATOR, "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs", Formatting.RED, true);
        settings.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine(elevated ? "Click to manage team settings." : "Only the owner or co-owners can access this.", elevated ? Formatting.YELLOW : Formatting.RED))));
        menuInventory.setStack(52, settings);
        ItemStack pvp = namedGradient(Items.DIAMOND_SWORD, "ᴘᴠᴘ sᴛᴀᴛᴜs");
        pvp.set(DataComponentTypes.LORE, new LoreComponent(List.of(composeLine("Currently: ", team.isPvpEnabled() ? "Enabled" : "Disabled", Formatting.GRAY, team.isPvpEnabled() ? Formatting.GREEN : Formatting.RED), plainLine("", Formatting.GRAY), plainLine("Click to toggle.", Formatting.YELLOW))));
        menuInventory.setStack(45, pvp);
        boolean owner = team.isOwner(viewer.getUuid());
        ItemStack leave = namedColored(owner ? Items.TNT : Items.DARK_OAK_DOOR, owner ? "ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ" : "ʟᴇᴀᴠᴇ ᴛᴇᴀᴍ", Formatting.RED, true);
        leave.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to continue.", Formatting.YELLOW))));
        menuInventory.setStack(53, leave);
    }

    public void updateSortStack(TeamSortType current) {
        ItemStack sort = namedGradient(Items.HOPPER, "sᴏʀᴛ ᴍᴇᴍʙᴇʀs");
        sort.set(DataComponentTypes.LORE, new LoreComponent(List.of(plainLine("Click to change the sorting.", Formatting.GRAY), plainLine("", Formatting.GRAY), sortLine("Online Status", current == TeamSortType.ONLINE_STATUS), sortLine("Rank", current == TeamSortType.RANK), sortLine("Alphabetical", current == TeamSortType.ALPHABETICAL), sortLine("Join Date", current == TeamSortType.JOIN_DATE))));
        menuInventory.setStack(49, sort);
    }

    private List<TeamPlayer> orderedMembers(PlayerEntity viewer) {
        List<TeamPlayer> members = new ArrayList<>(team.getMembers());
        MinecraftServer server = viewer instanceof ServerPlayerEntity sp ? sp.getEntityWorld().getServer() : null;
        Comparator<TeamPlayer> comparator = switch (team.getCurrentSortType()) {
            case ONLINE_STATUS -> Comparator.comparing((TeamPlayer member) -> server != null && server.getPlayerManager().getPlayer(member.getPlayerUuid()) != null).reversed().thenComparing(member -> PlayerNameResolver.resolve(server, member.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
            case RANK -> Comparator.comparingInt((TeamPlayer member) -> member.getRank().ordinal()).reversed().thenComparing(member -> PlayerNameResolver.resolve(server, member.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
            case ALPHABETICAL -> Comparator.comparing(member -> PlayerNameResolver.resolve(server, member.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER);
            case JOIN_DATE -> Comparator.comparing(TeamPlayer::getJoinDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        members.sort(comparator); return members;
    }

    private ItemStack createMemberHead(PlayerEntity viewer, TeamPlayer member) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD); head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(member.getPlayerUuid()));
        MinecraftServer server = viewer instanceof ServerPlayerEntity sp ? sp.getEntityWorld().getServer() : null; boolean online = server != null && server.getPlayerManager().getPlayer(member.getPlayerUuid()) != null;
        String name = PlayerNameResolver.resolve(server, member.getPlayerUuid());
        String symbol = switch (member.getRank()) { case INITIATE -> "+"; case MEMBER -> "›"; case ASSOCIATE -> "»"; case UNDEROFFICER -> "*"; case OFFICER -> "⁑"; case CO_LEADER -> "⁂"; case LEADER -> "★"; };
        MutableText title = Text.literal("●").setStyle(Style.EMPTY.withColor(online ? 0x00FF00 : 0xFF4444).withItalic(false));
        title.append(Text.literal("   ").setStyle(Style.EMPTY.withItalic(false))).append(Text.literal(symbol).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false).withItalic(false))).append(Text.literal(" ").setStyle(Style.EMPTY.withItalic(false))).append(gradientText(name, false));
        head.set(DataComponentTypes.CUSTOM_NAME, title);
        String joined = member.getJoinDate() == null ? "Unknown" : DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(member.getJoinDate());
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(composeLine("Rank: ", member.getRank().getDisplayName(), Formatting.GRAY, Formatting.WHITE), composeLine("Joined: ", joined, Formatting.GRAY, Formatting.WHITE))));
        return head;
    }

    private static MutableText gradientText(String value, boolean bold) { MutableText result = Text.empty(); if (value.isEmpty()) return result; int length = Math.max(1, value.codePointCount(0, value.length()) - 1); int index = 0; for (int offset = 0; offset < value.length();) { int codePoint = value.codePointAt(offset); int rgb = interpolate(PRIMARY_START, PRIMARY_END, (double) index / length); result.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(Style.EMPTY.withColor(rgb).withBold(bold).withItalic(false))); offset += Character.charCount(codePoint); index++; } return result; }
    private static int interpolate(int start, int end, double t) { int sr=(start>>16)&255,sg=(start>>8)&255,sb=start&255,er=(end>>16)&255,eg=(end>>8)&255,eb=end&255; int r=(int)Math.round(sr+(er-sr)*t),g=(int)Math.round(sg+(eg-sg)*t),b=(int)Math.round(sb+(eb-sb)*t); return (r<<16)|(g<<8)|b; }
    private static MutableText plainLine(String text, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false)); }
    private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) { return plainLine(prefix,prefixColor).append(plainLine(value,valueColor)); }
    private static MutableText sortLine(String name, boolean selected) { return Text.literal("▪ " + name).setStyle(Style.EMPTY.withColor(selected ? Formatting.GREEN : Formatting.GRAY).withItalic(false)); }
    private static ItemStack itemWithLore(ItemStack stack, List<Text> lore) { stack.set(DataComponentTypes.LORE, new LoreComponent(lore)); return stack; }
    private static ItemStack namedPlain(Item item, String name) { ItemStack stack=new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false))); return stack; }
    private static ItemStack namedGradient(Item item, String name) { ItemStack stack=new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name,true)); return stack; }
    private static ItemStack namedColored(Item item, String name, Formatting color, boolean bold) { ItemStack stack=new ItemStack(item); stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(false))); return stack; }

    @Override public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) { if (slot >= 0 && slot < menuInventory.size()) { if (actionHandler != null) actionHandler.handle(player, slot, button, actionType, team, this); return; } super.onSlotClick(slot, button, actionType, player); }
    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && team.isMember(viewerUuid); }
    public Inventory getMenuInventory() { return menuInventory; }
    public Team getTeam() { return team; }
    public int getPage() { return 0; }
    public void previousPage() { }
    public void nextPage() { }
    public void refresh() { sendContentUpdates(); }
    public void sendContentUpdates() { super.sendContentUpdates(); }
    private static final class MenuSlot extends Slot { private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); } @Override public boolean canInsert(ItemStack stack) { return false; } @Override public boolean canTakeItems(PlayerEntity player) { return false; } }
}
