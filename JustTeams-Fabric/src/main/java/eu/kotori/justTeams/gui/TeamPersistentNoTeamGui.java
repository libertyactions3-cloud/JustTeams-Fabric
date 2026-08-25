package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Persistent 54-slot inventory GUI for players who are not currently in a team. */
public final class TeamPersistentNoTeamGui {
    private enum View { MAIN, INVITES, LEADERBOARD_CATEGORIES, LEADERBOARD_RANKED }
    private enum LeaderboardType { KILLS, BALANCE, MEMBERS }
    private static final int[] CONTENT_SLOTS = {
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    private TeamPersistentNoTeamGui() {}

    public static void openMain(ServerPlayerEntity player) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            TeamGuiManager.openMain(player);
            return;
        }
        openOrReuse(player, View.MAIN, null);
    }

    public static void openInvites(ServerPlayerEntity player) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            player.sendMessage(Text.literal("You are already in a team."), true);
            return;
        }
        openOrReuse(player, View.INVITES, null);
    }

    public static void openLeaderboardCategories(ServerPlayerEntity player) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            TeamGuiManager.openPersistentLeaderboard(player,
                    TeamPersistentLeaderboardGui.View.CATEGORIES,
                    TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType.KILLS);
            return;
        }
        openOrReuse(player, View.LEADERBOARD_CATEGORIES, null);
    }

    public static void openLeaderboard(ServerPlayerEntity player,
                                       TeamPersistentLeaderboardGui.TeamPersistentLeaderboardGuiType type) {
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) {
            TeamGuiManager.openPersistentLeaderboard(player, TeamPersistentLeaderboardGui.View.RANKED, type);
            return;
        }
        LeaderboardType localType = switch (type) {
            case KILLS -> LeaderboardType.KILLS;
            case BALANCE -> LeaderboardType.BALANCE;
            case MEMBERS -> LeaderboardType.MEMBERS;
        };
        openOrReuse(player, View.LEADERBOARD_RANKED, localType);
    }

    private static void openOrReuse(ServerPlayerEntity player, View view, LeaderboardType type) {
        if (player.currentScreenHandler instanceof Handler handler && handler.viewerUuid.equals(player.getUuid())) {
            handler.render(view, type);
            return;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player, view, type),
                Text.literal("ᴛᴇᴀᴍ ᴍᴇɴᴜ").setStyle(Style.EMPTY.withItalic(false))));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(54);
        private final ServerPlayerEntity viewer;
        private final UUID viewerUuid;
        private View view;
        private LeaderboardType leaderboardType;

        Handler(int syncId, net.minecraft.entity.player.PlayerInventory inventory, ServerPlayerEntity viewer,
                View view, LeaderboardType leaderboardType) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewer = viewer;
            this.viewerUuid = viewer.getUuid();
            this.view = view;
            this.leaderboardType = leaderboardType;
            for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
                addSlot(new MenuSlot(menu, row * 9 + col, 8 + col * 18, 18 + row * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
            render(view, leaderboardType);
        }

        private void render(View next, LeaderboardType type) {
            view = next;
            leaderboardType = type;
            clear();
            switch (view) {
                case MAIN -> renderMain();
                case INVITES -> renderInvites();
                case LEADERBOARD_CATEGORIES -> renderLeaderboardCategories();
                case LEADERBOARD_RANKED -> renderLeaderboardRanked();
            }
            sendContentUpdates();
        }

        private void renderMain() {
            menu.setStack(12, loreItem(Items.WRITABLE_BOOK, "ᴄʀᴇᴀᴛᴇ ᴀ ᴛᴇᴀᴍ", List.of(
                    plainLine("Start your own team and invite your friends!", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to begin the creation process.", Formatting.YELLOW))));
            menu.setStack(14, loreItem(Items.EMERALD, "ᴠɪᴇᴡ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅs", List.of(
                    plainLine("See the top teams on the server.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Click to view leaderboards.", Formatting.YELLOW))));
        }

        private void renderInvites() {
            List<Team> invites = getInvites();
            if (invites.isEmpty()) {
                menu.setStack(22, loreItem(Items.PAPER, "No Pending Invites", List.of(
                        plainLine("You don't have any pending team invitations.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Team owners can invite you with", Formatting.DARK_GRAY),
                        plainLine("/team invite <player>", Formatting.DARK_GRAY))));
            } else {
                for (int i = 0; i < invites.size() && i < CONTENT_SLOTS.length; i++) {
                    Team team = invites.get(i);
                    menu.setStack(CONTENT_SLOTS[i], loreItem(Items.DIAMOND, team.getName(), List.of(
                            composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                            composeLine("Invited by: ", inviterName(team), Formatting.GRAY, Formatting.YELLOW),
                            composeLine("Members: ", Integer.toString(team.getMembers().size()), Formatting.GRAY, Formatting.WHITE),
                            composeLine("Description: ", team.getDescription(), Formatting.GRAY, Formatting.WHITE))));
                }
            }
            menu.setStack(49, loreItem(Items.ARROW, "ʙᴀᴄᴋ", List.of(plainLine("Click to return to the team menu.", Formatting.YELLOW))));
            menu.setStack(53, loreItem(Items.BARRIER, "ᴄʟᴏsᴇ", List.of(plainLine("Click to close this menu.", Formatting.RED))));
        }

        private void renderLeaderboardCategories() {
            menu.setStack(4, namedGradient(Items.NETHER_STAR, "ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ"));
            menu.setStack(11, loreItem(Items.NETHERITE_SWORD, "ᴛᴏᴘ ᴋɪʟʟs", List.of(plainLine("Shows the top 10 teams with the most kills.", Formatting.GRAY))));
            menu.setStack(13, loreItem(Items.DIAMOND, "ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇ", List.of(plainLine("Shows the top 10 richest teams.", Formatting.GRAY))));
            menu.setStack(15, loreItem(Items.PLAYER_HEAD, "ᴛᴏᴘ ᴍᴇᴍʙᴇʀs", List.of(plainLine("Shows the top 10 teams with the most members.", Formatting.GRAY))));
            menu.setStack(22, loreItem(Items.ARROW, "ʙᴀᴄᴋ", List.of(plainLine("Click to return to the team menu.", Formatting.YELLOW))));
        }

        private void renderLeaderboardRanked() {
            List<Team> teams = JustTeamsFabric.teams().getTeams().stream().sorted(comparator()).limit(CONTENT_SLOTS.length).toList();
            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                String value = switch (leaderboardType) {
                    case KILLS -> Integer.toString(team.getKills());
                    case BALANCE -> String.format("%.2f", team.getBalance());
                    case MEMBERS -> Integer.toString(team.getMembers().size());
                };
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponentTypes.CUSTOM_NAME, gradientText("#" + (i + 1) + " " + team.getName()));
                head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                        composeLine(label() + ": ", value, Formatting.GRAY, Formatting.WHITE))));
                menu.setStack(CONTENT_SLOTS[i], head);
            }
            menu.setStack(49, loreItem(Items.ARROW, "ʙᴀᴄᴋ", List.of(plainLine("Click to return to category selection.", Formatting.YELLOW))));
        }

        private String label() { return switch (leaderboardType) { case KILLS -> "Kills"; case BALANCE -> "Balance"; case MEMBERS -> "Members"; }; }
        private Comparator<Team> comparator() {
            return switch (leaderboardType) {
                case KILLS -> Comparator.comparingInt(Team::getKills).reversed().thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER);
                case BALANCE -> Comparator.comparingDouble(Team::getBalance).reversed().thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER);
                case MEMBERS -> Comparator.comparingInt((Team t) -> t.getMembers().size()).reversed().thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER);
            };
        }

        private List<Team> getInvites() {
            List<Team> invites = new ArrayList<>();
            for (Team team : JustTeamsFabric.teams().getTeams()) if (team.hasInvite(viewerUuid)) invites.add(team);
            return invites;
        }

        private String inviterName(Team team) {
            ServerPlayerEntity owner = viewer.getEntityWorld().getServer().getPlayerManager().getPlayer(team.getOwnerUuid());
            return owner == null ? team.getOwnerUuid().toString().substring(0, 8) : owner.getName().getString();
        }

        private void clear() {
            ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int slot = 0; slot < 54; slot++) menu.setStack(slot, ItemStack.EMPTY);
            for (int slot = 0; slot < 9; slot++) menu.setStack(slot, filler.copy());
            for (int slot = 45; slot < 54; slot++) menu.setStack(slot, filler.copy());
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !player.getUuid().equals(viewerUuid)) return;
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            switch (view) {
                case MAIN -> {
                    if (slot == 12) beginCreation(serverPlayer);
                    else if (slot == 14) render(View.LEADERBOARD_CATEGORIES, null);
                }
                case INVITES -> handleInvites(serverPlayer, slot, button);
                case LEADERBOARD_CATEGORIES -> {
                    if (slot == 11) render(View.LEADERBOARD_RANKED, LeaderboardType.KILLS);
                    else if (slot == 13) render(View.LEADERBOARD_RANKED, LeaderboardType.BALANCE);
                    else if (slot == 15) render(View.LEADERBOARD_RANKED, LeaderboardType.MEMBERS);
                    else if (slot == 22) render(View.MAIN, null);
                }
                case LEADERBOARD_RANKED -> { if (slot == 49) render(View.LEADERBOARD_CATEGORIES, null); }
            }
        }

        private void handleInvites(ServerPlayerEntity player, int slot, int button) {
            if (slot == 49) { render(View.MAIN, null); return; }
            if (slot == 53) { player.closeHandledScreen(); return; }
            int index = indexOf(slot);
            List<Team> invites = getInvites();
            if (index < 0 || index >= invites.size()) return;
            Team team = invites.get(index);
            if (button == 0) {
                team.removeInvite(viewerUuid);
                JustTeamsFabric.teams().addMember(team, new TeamPlayer(viewerUuid, TeamRole.MEMBER, Instant.now(), false, false, false, true));
                save();
                TeamGuiManager.openMain(player);
            } else if (button == 1) {
                team.removeInvite(viewerUuid);
                save();
                render(View.INVITES, null);
            }
        }

        private int indexOf(int slot) { for (int i = 0; i < CONTENT_SLOTS.length; i++) if (CONTENT_SLOTS[i] == slot) return i; return -1; }

        private void beginCreation(ServerPlayerEntity player) {
            TeamStringInputGui.open(player, "Create Team", "Enter your new team's name (1-16 characters)", name -> {
                String cleanName = name.trim();
                if (cleanName.isBlank() || cleanName.length() > 16 || cleanName.contains(" ")) {
                    player.sendMessage(Text.literal("Invalid team name. Use 1-16 non-space characters."), false);
                    render(View.MAIN, null);
                    return;
                }
                TeamStringInputGui.open(player, "Create Team", "Enter your team's tag (1-4 characters)", tag -> {
                    String cleanTag = tag.trim();
                    if (cleanTag.isBlank() || cleanTag.length() > 4 || cleanTag.contains(" ")) {
                        player.sendMessage(Text.literal("Invalid team tag. Use 1-4 non-space characters."), false);
                        render(View.MAIN, null);
                        return;
                    }
                    try {
                        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) return;
                        JustTeamsFabric.teams().createTeam(cleanName, cleanTag, player.getUuid(), true, false, false);
                        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
                        player.sendMessage(Text.literal("Team created successfully."), false);
                        TeamGuiManager.openMain(player);
                    } catch (IllegalStateException | IOException exception) {
                        JustTeamsFabric.LOGGER.error("Failed to create team", exception);
                        player.sendMessage(Text.literal("Unable to create the team."), false);
                        render(View.MAIN, null);
                    }
                }, () -> render(View.MAIN, null));
            }, () -> render(View.MAIN, null));
        }

        private void save() {
            try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
            catch (IOException exception) { JustTeamsFabric.LOGGER.error("Failed to save team invitation change", exception); }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && !JustTeamsFabric.teams().isInTeam(player.getUuid()); }

        private static ItemStack namedPlain(Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static ItemStack namedGradient(Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name));
            return stack;
        }

        private static ItemStack loreItem(Item item, String name, List<Text> lore) {
            ItemStack stack = namedGradient(item, name);
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            return stack;
        }

        private static MutableText plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
            return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
        }

        private static MutableText gradientText(String value) {
            MutableText result = Text.empty();
            if (value.isEmpty()) return result;
            int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
            int index = 0;
            for (int offset = 0; offset < value.length();) {
                int cp = value.codePointAt(offset);
                double t = (double) index / length;
                int r = 76;
                int g = (int) Math.round(157 + (150 - 157) * t);
                int b = (int) Math.round(222 + (210 - 222) * t);
                result.append(Text.literal(new String(Character.toChars(cp)))
                        .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(true).withItalic(false)));
                offset += Character.charCount(cp);
                index++;
            }
            return result;
        }

        private static final class MenuSlot extends Slot {
            MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}
