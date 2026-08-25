package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side GUI for viewing and accepting/denying pending team invitations. */
public final class TeamInvitesGui {
    private static final int[] INVITE_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int PRIMARY_START = 0x4C9D9D;
    private static final int PRIMARY_END = 0x4C96D2;

    private TeamInvitesGui() {}

    public static void open(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player),
                Text.literal("ᴘᴇɴᴅɪɴɢ ɪɴᴠɪᴛᴇs").setStyle(Style.EMPTY.withItalic(false))));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(54);
        private final PlayerEntity viewer;
        private final List<Team> invites = new ArrayList<>();

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewer = viewer;
            populate();
            for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
                addSlot(new MenuSlot(menu, row * 9 + col, 8 + col * 18, 18 + row * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }

        private void populate() {
            for (int slot = 0; slot < 54; slot++) menu.setStack(slot, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));

            invites.clear();
            UUID viewerUuid = viewer.getUuid();
            if (!JustTeamsFabric.teams().isInTeam(viewerUuid)) {
                for (Team team : JustTeamsFabric.teams().getTeams()) {
                    if (team.hasInvite(viewerUuid)) invites.add(team);
                }
            }

            if (invites.isEmpty()) {
                ItemStack empty = new ItemStack(Items.PAPER);
                empty.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("No Pending Invites").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
                empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        plainLine("You don't have any pending team invitations.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Team owners can invite you with", Formatting.DARK_GRAY),
                        plainLine("/team invite <player>", Formatting.DARK_GRAY)
                )));
                menu.setStack(22, empty);
            } else {
                for (int i = 0; i < INVITE_SLOTS.length && i < invites.size(); i++) {
                    menu.setStack(INVITE_SLOTS[i], inviteItem(invites.get(i)));
                }
            }

            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to the main menu.", Formatting.YELLOW)
            )));
            menu.setStack(49, back);

            ItemStack close = namedPlain(Items.BARRIER, "ᴄʟᴏsᴇ");
            close.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ᴄʟᴏsᴇ").setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true).withItalic(false)));
            close.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to close this menu.", Formatting.RED)
            )));
            menu.setStack(53, close);
        }

        private ItemStack inviteItem(Team team) {
            ItemStack stack = new ItemStack(Items.DIAMOND);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(team.getName(), true));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Tag: ", team.getTag(), Formatting.GRAY, Formatting.WHITE),
                    composeLine("Invited by: ", inviterName(team), Formatting.GRAY, Formatting.YELLOW),
                    composeLine("Members: ", Integer.toString(team.getMembers().size()), Formatting.GRAY, Formatting.WHITE),
                    composeLine("Description: ", team.getDescription(), Formatting.GRAY, Formatting.WHITE)
            )));
            return stack;
        }

        private String inviterName(Team team) {
            ServerPlayerEntity viewerPlayer = (ServerPlayerEntity) viewer;
            ServerPlayerEntity online = viewerPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(team.getOwnerUuid());
            return online != null ? online.getName().getString() : team.getOwnerUuid().toString().substring(0, 8);
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP
                    || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            if (!player.getUuid().equals(viewer.getUuid())) return;
            if (slot == 49) {
                TeamGuiManager.openMain(player);
                return;
            }
            if (slot == 53) {
                ((ServerPlayerEntity) player).closeHandledScreen();
                return;
            }

            int inviteIndex = inviteIndex(slot);
            if (inviteIndex < 0 || inviteIndex >= invites.size()) return;
            Team team = invites.get(inviteIndex);
            if (button == 0) accept(player, team);
            else if (button == 1) deny(player, team);
        }

        private void accept(PlayerEntity player, Team team) {
            UUID uuid = player.getUuid();
            if (JustTeamsFabric.teams().isInTeam(uuid)) {
                team.removeInvite(uuid);
                save();
                player.sendMessage(Text.literal("You are already in a team."), true);
                populate();
                sendContentUpdates();
                return;
            }
            team.removeInvite(uuid);
            JustTeamsFabric.teams().addMember(team, new TeamPlayer(uuid, TeamRole.MEMBER, java.time.Instant.now(), false, false, false, true));
            save();
            JustTeamsFabric.glow().refreshAll(((ServerPlayerEntity) player).getEntityWorld().getServer());
            player.sendMessage(Text.literal("You have joined " + team.getName() + "."), false);
            TeamGuiManager.openMain(player);
        }

        private void deny(PlayerEntity player, Team team) {
            team.removeInvite(player.getUuid());
            save();
            player.sendMessage(Text.literal("You declined the invitation to join " + team.getName() + "."), false);
            populate();
            sendContentUpdates();
        }

        private int inviteIndex(int slot) {
            for (int i = 0; i < INVITE_SLOTS.length; i++) if (INVITE_SLOTS[i] == slot) return i;
            return -1;
        }

        private void save() {
            try {
                JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            } catch (IOException exception) {
                JustTeamsFabric.LOGGER.error("Failed to save team invitation change", exception);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()); }

        private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
            return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
        }

        private static MutableText plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static MutableText gradientText(String value, boolean bold) {
            MutableText result = Text.empty();
            if (value.isEmpty()) return result;
            int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
            int index = 0;
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                double t = (double) index / length;
                int sr = (PRIMARY_START >> 16) & 0xFF, sg = (PRIMARY_START >> 8) & 0xFF, sb = PRIMARY_START & 0xFF;
                int er = (PRIMARY_END >> 16) & 0xFF, eg = (PRIMARY_END >> 8) & 0xFF, eb = PRIMARY_END & 0xFF;
                int r = (int) Math.round(sr + (er - sr) * t);
                int g = (int) Math.round(sg + (eg - sg) * t);
                int b = (int) Math.round(sb + (eb - sb) * t);
                result.append(Text.literal(new String(Character.toChars(codePoint)))
                        .setStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(bold).withItalic(false)));
                offset += Character.charCount(codePoint);
                index++;
            }
            return result;
        }

        private static final class MenuSlot extends Slot {
            private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}
