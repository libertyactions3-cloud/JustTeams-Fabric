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

    private TeamInvitesGui() {}

    public static void open(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player),
                Text.literal("ᴘᴇɴᴅɪɴɢ ɪɴᴠɪᴛᴇs")));
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
            for (int slot = 0; slot < 54; slot++) menu.setStack(slot, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            menu.setStack(49, named(Items.ARROW, "ʙᴀᴄᴋ"));
            menu.setStack(53, named(Items.BARRIER, "ᴄʟᴏsᴇ"));

            invites.clear();
            UUID viewerUuid = viewer.getUuid();
            if (!JustTeamsFabric.teams().isInTeam(viewerUuid)) {
                for (Team team : JustTeamsFabric.teams().getTeams()) {
                    if (team.hasInvite(viewerUuid)) invites.add(team);
                }
            }

            if (invites.isEmpty()) {
                menu.setStack(22, named(Items.PAPER, "No Pending Invites"));
                return;
            }

            for (int i = 0; i < INVITE_SLOTS.length && i < invites.size(); i++) {
                menu.setStack(INVITE_SLOTS[i], inviteItem(invites.get(i)));
            }
        }

        private ItemStack inviteItem(Team team) {
            ItemStack stack = new ItemStack(Items.DIAMOND);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(team.getName()).formatted(Formatting.BOLD).setStyle(
                            Text.literal(team.getName()).getStyle().withItalic(false).withBold(true)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("Tag: ").formatted(Formatting.GRAY).append(Text.literal(team.getTag()).formatted(Formatting.WHITE)),
                    Text.literal("Invited by: ").formatted(Formatting.GRAY).append(Text.literal(inviterName(team)).formatted(Formatting.YELLOW)),
                    Text.literal("Members: ").formatted(Formatting.GRAY).append(Text.literal(Integer.toString(team.getMembers().size())).formatted(Formatting.WHITE)),
                    Text.literal("Description: ").formatted(Formatting.GRAY).append(Text.literal(team.getDescription()).formatted(Formatting.WHITE)),
                    Text.empty(),
                    Text.literal("Left-Click to Accept").formatted(Formatting.GREEN),
                    Text.literal("Right-Click to Deny").formatted(Formatting.RED)
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

        private static ItemStack named(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Text.literal(name).getStyle().withItalic(false)));
            return stack;
        }

        private static final class MenuSlot extends Slot {
            private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}
