package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.economy.TeamBankLogManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.util.PlayerNameResolver;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/** Persistent 54-slot team-bank audit log view and one-week AutoBank top-spender view. */
public final class TeamBankLogsGui {
    private static final int[] LOG_SLOTS = {9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44};
    private static final WeakHashMap<TeamMenuHandler, ItemStack[]> SNAPSHOTS = new WeakHashMap<>();
    private static final WeakHashMap<TeamMenuHandler, Boolean> TOP_VIEW = new WeakHashMap<>();
    private TeamBankLogsGui() {}

    public static void open(TeamMenuHandler menu, ServerPlayerEntity player, Team team) { snapshot(menu); TOP_VIEW.put(menu, false); renderLogs(menu, player, team); }
    public static boolean isOpen(TeamMenuHandler menu) { return SNAPSHOTS.containsKey(menu); }
    public static boolean handle(TeamMenuHandler menu, ServerPlayerEntity player, Team team, int slot) {
        if (!isOpen(menu)) return false;
        if (slot == 49) { boolean top = !Boolean.TRUE.equals(TOP_VIEW.get(menu)); TOP_VIEW.put(menu, top); if (top) renderTop(menu, player, team); else renderLogs(menu, player, team); return true; }
        if (slot == 53) { close(menu); return true; }
        return true;
    }
    public static void close(TeamMenuHandler menu) { ItemStack[] snapshot = SNAPSHOTS.remove(menu); TOP_VIEW.remove(menu); if (snapshot == null) return; for (int slot=0;slot<snapshot.length;slot++) menu.getMenuInventory().setStack(slot,snapshot[slot].copy()); menu.sendContentUpdates(); }

    private static void renderLogs(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        Inventory inventory = menu.getMenuInventory(); clear(inventory);
        inventory.setStack(4, named(Items.WRITABLE_BOOK, "ᴛᴇᴀᴍ ʟᴏɢs", Formatting.AQUA, true));
        List<TeamBankLogManager.Entry> entries = new ArrayList<>(TeamBankLogManager.recent(team)); entries.sort(Comparator.comparingLong(TeamBankLogManager.Entry::timestampMillis).reversed());
        for (int i=0;i<LOG_SLOTS.length && i<entries.size();i++) inventory.setStack(LOG_SLOTS[i], logHead(player, team, entries.get(i)));
        if (entries.isEmpty()) { ItemStack empty=named(Items.PAPER,"ɴᴏ ʙᴀɴᴋ ʟᴏɢs",Formatting.GRAY,true); empty.set(DataComponentTypes.LORE,new LoreComponent(List.of(line("No team-bank withdrawals were recorded in the last 7 days.",Formatting.GRAY)))); inventory.setStack(22,empty); }
        TeamBankLogManager.TopSpender top=TeamBankLogManager.topAutoBankSpender(team);
        ItemStack topButton=named(Items.EMERALD_BLOCK,"ᴛᴏᴘ ᴀᴜᴛᴏʙᴀɴᴋ sᴘᴇɴᴅᴇʀ",Formatting.AQUA,true);
        topButton.set(DataComponentTypes.LORE,new LoreComponent(top==null?List.of(line("No AutoBank withdrawals were recorded in the last 7 days.",Formatting.GRAY),line("",Formatting.GRAY),line("Click to view the top spender.",Formatting.YELLOW)):List.of(compose("Top: ",top.playerName(),Formatting.GRAY,Formatting.WHITE),compose("Withdrawn: ",top.amount()+" total emeralds",Formatting.GRAY,Formatting.GREEN),line("",Formatting.GRAY),line("Click to view the top spender.",Formatting.YELLOW)))); inventory.setStack(49,topButton); inventory.setStack(53,back()); menu.sendContentUpdates();
    }

    private static ItemStack logHead(ServerPlayerEntity viewer, Team team, TeamBankLogManager.Entry entry) {
        ItemStack head=new ItemStack(Items.PLAYER_HEAD); head.set(DataComponentTypes.PROFILE,ProfileComponent.ofDynamic(entry.playerUuid()));
        ServerPlayerEntity online=viewer.getEntityWorld().getServer().getPlayerManager().getPlayer(entry.playerUuid()); boolean isOnline=online!=null; TeamPlayer member=team.getMember(entry.playerUuid());
        String name=entry.playerName(); if (online!=null) name=online.getName().getString(); else { String resolved=PlayerNameResolver.resolve(viewer.getEntityWorld().getServer(),entry.playerUuid()); if (!resolved.equals("Unknown")) name=resolved; }
        String symbol=member==null?"+":switch(member.getRank()){case INITIATE->"+";case MEMBER->"›";case ASSOCIATE->"»";case UNDEROFFICER->"*";case OFFICER->"⁑";case CO_LEADER->"⁂";case LEADER->"★";};
        MutableText title=Text.literal("●").setStyle(Style.EMPTY.withColor(isOnline?0x00FF00:0xFF4444).withItalic(false)).append(Text.literal("   ").setStyle(Style.EMPTY.withItalic(false))).append(Text.literal(symbol).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false).withItalic(false))).append(Text.literal(" ").setStyle(Style.EMPTY.withItalic(false))).append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false).withItalic(false))); head.set(DataComponentTypes.CUSTOM_NAME,title);
        head.set(DataComponentTypes.LORE,new LoreComponent(List.of(compose("Date: ",TeamBankLogManager.formatTimestamp(entry.timestampMillis()),Formatting.GRAY,Formatting.WHITE),compose("Amount: ",entry.amount()+" total emeralds",Formatting.GRAY,Formatting.GREEN),compose("Type: ",entry.kind()==TeamBankLogManager.Kind.AUTOBANK?"AutoBank":"Manual withdrawal",Formatting.GRAY,Formatting.WHITE),compose("Action: ",entry.action(),Formatting.GRAY,Formatting.WHITE)))); return head;
    }

    private static void renderTop(TeamMenuHandler menu, ServerPlayerEntity player, Team team) {
        Inventory inventory=menu.getMenuInventory(); clear(inventory); inventory.setStack(4,named(Items.EMERALD_BLOCK,"ᴛᴏᴘ ᴀᴜᴛᴏʙᴀɴᴋ sᴘᴇɴᴅᴇʀ",Formatting.AQUA,true));
        TeamBankLogManager.TopSpender top=TeamBankLogManager.topAutoBankSpender(team);
        if(top==null){ItemStack empty=named(Items.PAPER,"ɴᴏ ᴛᴏᴘ sᴘᴇɴᴅᴇʀ",Formatting.GRAY,true); empty.set(DataComponentTypes.LORE,new LoreComponent(List.of(line("No AutoBank withdrawals were recorded in the last 7 days.",Formatting.GRAY)))); inventory.setStack(22,empty);} else {
            ItemStack head=new ItemStack(Items.PLAYER_HEAD); head.set(DataComponentTypes.PROFILE,ProfileComponent.ofDynamic(top.playerUuid())); TeamPlayer member=team.getMember(top.playerUuid()); ServerPlayerEntity online=player.getEntityWorld().getServer().getPlayerManager().getPlayer(top.playerUuid()); boolean isOnline=online!=null; String name=online!=null?online.getName().getString():PlayerNameResolver.resolve(player.getEntityWorld().getServer(),top.playerUuid()); if(name.equals("Unknown"))name=top.playerName(); String symbol=member==null?"+":switch(member.getRank()){case INITIATE->"+";case MEMBER->"›";case ASSOCIATE->"»";case UNDEROFFICER->"*";case OFFICER->"⁑";case CO_LEADER->"⁂";case LEADER->"★";}; MutableText title=Text.literal("●").setStyle(Style.EMPTY.withColor(isOnline?0x00FF00:0xFF4444).withItalic(false)).append(Text.literal("   ").setStyle(Style.EMPTY.withItalic(false))).append(Text.literal(symbol).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false).withItalic(false))).append(Text.literal(" ").setStyle(Style.EMPTY.withItalic(false))).append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false).withItalic(false))); head.set(DataComponentTypes.CUSTOM_NAME,title); head.set(DataComponentTypes.LORE,new LoreComponent(List.of(compose("Withdrawn: ",top.amount()+" total emeralds",Formatting.GRAY,Formatting.GREEN),line("Calculated from the last 7 days of AutoBank logs.",Formatting.GRAY)))); inventory.setStack(22,head);
        }
        ItemStack toggle=named(Items.WRITABLE_BOOK,"ʙᴀᴄᴋ ᴛᴏ ʟᴏɢs",Formatting.AQUA,true); toggle.set(DataComponentTypes.LORE,new LoreComponent(List.of(line("Return to the team bank logs.",Formatting.YELLOW)))); inventory.setStack(49,toggle); inventory.setStack(53,back()); menu.sendContentUpdates();
    }

    private static void snapshot(TeamMenuHandler menu){if(SNAPSHOTS.containsKey(menu))return;ItemStack[] snapshot=new ItemStack[54];for(int i=0;i<54;i++)snapshot[i]=menu.getMenuInventory().getStack(i).copy();SNAPSHOTS.put(menu,snapshot);}
    private static void clear(Inventory inventory){ItemStack filler=new ItemStack(Items.GRAY_STAINED_GLASS_PANE);filler.set(DataComponentTypes.CUSTOM_NAME,Text.literal(" ").setStyle(Style.EMPTY.withItalic(false)));for(int i=0;i<54;i++)inventory.setStack(i,ItemStack.EMPTY);for(int i=0;i<9;i++)inventory.setStack(i,filler.copy());for(int i=45;i<54;i++)inventory.setStack(i,filler.copy());}
    private static ItemStack named(net.minecraft.item.Item item,String name,Formatting color,boolean bold){ItemStack stack=new ItemStack(item);stack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(name).setStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(false)));return stack;}
    private static ItemStack back(){return named(Items.ARROW,"ʙᴀᴄᴋ",Formatting.GRAY,true);}
    private static MutableText line(String text,Formatting color){return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));}
    private static MutableText compose(String prefix,String value,Formatting prefixColor,Formatting valueColor){return line(prefix,prefixColor).append(line(value,valueColor));}
}
