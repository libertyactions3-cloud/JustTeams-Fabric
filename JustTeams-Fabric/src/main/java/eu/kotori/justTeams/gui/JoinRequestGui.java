package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
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

/** Server-side 54-slot GUI for approving or denying pending team join requests. */
public final class JoinRequestGui {
    private static final int[] REQUEST_SLOTS = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final int PRIMARY_START = 0x4C9DDE;
    private static final int PRIMARY_END = 0x4C96D2;

    private JoinRequestGui() {}

    public static void open(PlayerEntity player, Team team) {
        if (!team.hasElevatedPermissions(player.getUuid())) {
            player.sendMessage(Text.literal("Only the owner or co-owners can access join requests."), true);
            return;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player, team),
                Text.literal("ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs").setStyle(Style.EMPTY.withItalic(false))));
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menu = new SimpleInventory(54);
        private final PlayerEntity viewer;
        private final Team team;
        private final List<UUID> requests = new ArrayList<>();

        Handler(int syncId, PlayerInventory inventory, PlayerEntity viewer, Team team) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewer = viewer;
            this.team = team;
            populate();
            for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
                addSlot(new MenuSlot(menu, row * 9 + col, 8 + col * 18, 18 + row * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }

        private void populate() {
            for (int i = 0; i < 54; i++) menu.setStack(i, namedPlain(Items.GRAY_STAINED_GLASS_PANE, " "));

            ItemStack header = namedBold(Items.SOUL_LANTERN, "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs", PRIMARY_START, PRIMARY_END);
            menu.setStack(4, header);

            requests.clear();
            for (UUID uuid : team.getJoinRequests()) {
                if (!team.isMember(uuid)) requests.add(uuid);
            }

            for (int i = 0; i < REQUEST_SLOTS.length && i < requests.size(); i++) {
                menu.setStack(REQUEST_SLOTS[i], createRequestHead(requests.get(i)));
            }

            if (requests.isEmpty()) {
                ItemStack empty = new ItemStack(Items.PAPER);
                empty.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("No Join Requests").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
                empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        plainLine("There are currently no pending requests.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Players can request to join if your", Formatting.DARK_GRAY),
                        plainLine("team is set to public.", Formatting.DARK_GRAY)
                )));
                menu.setStack(22, empty);
            }

            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to the main menu.", Formatting.YELLOW)
            )));
            menu.setStack(49, back);
        }

        private ItemStack createRequestHead(UUID uuid) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, ProfileComponent.ofDynamic(uuid));

            boolean online = false;
            String playerName = resolveName(uuid);
            if (viewer instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayerEntity target = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(uuid);
                online = target != null;
                if (target != null) playerName = target.getName().getString();
            }

            MutableText name = gradientTextStatic(playerName, true, PRIMARY_START, PRIMARY_END);
            MutableText rebuilt = Text.empty();
            rebuilt.append(Text.literal("● ").setStyle(Style.EMPTY
                    .withColor(online ? Formatting.GREEN : Formatting.RED)
                    .withBold(false)
                    .withItalic(false)));
            rebuilt.append(name);
            head.set(DataComponentTypes.CUSTOM_NAME, rebuilt);
            head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("This player wants to join your team.", Formatting.GRAY),
                    plainLine("", Formatting.GRAY),
                    plainLine("Left-Click to Accept", Formatting.GREEN),
                    plainLine("Right-Click to Deny", Formatting.RED)
            )));
            return head;
        }

        private String resolveName(UUID uuid) {
            if (viewer instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayerEntity online = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(uuid);
                if (online != null) return online.getName().getString();
            }
            return uuid.toString().substring(0, 8);
        }

        @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (action == SlotActionType.QUICK_MOVE || action == SlotActionType.SWAP || action == SlotActionType.THROW || action == SlotActionType.CLONE) return;
            if (slot == 49) { TeamGuiManager.openMain(player); return; }
            if (slot < 0 || slot >= 54 || !team.hasElevatedPermissions(player.getUuid())) return;
            int requestIndex = requestIndex(slot);
            if (requestIndex < 0 || requestIndex >= requests.size()) return;
            UUID uuid = requests.get(requestIndex);
            if (button == 0) approve(player, uuid);
            else if (button == 1) deny(player, uuid);
        }

        private void approve(PlayerEntity player, UUID uuid) {
            if (JustTeamsFabric.teams().isInTeam(uuid)) {
                team.removeJoinRequest(uuid);
                save();
                populate();
                sendContentUpdates();
                return;
            }
            team.removeJoinRequest(uuid);
            JustTeamsFabric.teams().addMember(team, new TeamPlayer(uuid, TeamRole.MEMBER, java.time.Instant.now(), false, false, false, true));
            save();
            JustTeamsFabric.glow().refreshAll(player.getEntityWorld().getServer());
            notifyPlayer(uuid, "Your request to join " + team.getName() + " was accepted.");
            populate();
            sendContentUpdates();
        }

        private void deny(PlayerEntity player, UUID uuid) {
            team.removeJoinRequest(uuid);
            save();
            notifyPlayer(uuid, "Your request to join " + team.getName() + " was denied.");
            populate();
            sendContentUpdates();
        }

        private void notifyPlayer(UUID uuid, String message) {
            if (viewer instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayerEntity target = serverPlayer.getEntityWorld().getServer().getPlayerManager().getPlayer(uuid);
                if (target != null) target.sendMessage(Text.literal(message), false);
            }
        }

        private int requestIndex(int slot) {
            for (int i = 0; i < REQUEST_SLOTS.length; i++) if (REQUEST_SLOTS[i] == slot) return i;
            return -1;
        }

        private void save() {
            try { JustTeamsFabric.storage().save(JustTeamsFabric.teams()); }
            catch (IOException e) { JustTeamsFabric.LOGGER.error("Failed to save join request change", e); }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewer.getUuid()) && team.hasElevatedPermissions(player.getUuid()); }

        private static ItemStack namedPlain(net.minecraft.item.Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static MutableText plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static ItemStack namedBold(net.minecraft.item.Item item, String name, int startColor, int endColor) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientTextStatic(name, true, startColor, endColor));
            return stack;
        }

        private static MutableText gradientTextStatic(String value, boolean bold, int start, int end) {
            MutableText result = Text.empty();
            if (value.isEmpty()) return result;
            int length = Math.max(1, value.codePointCount(0, value.length()) - 1);
            int index = 0;
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                double t = (double) index / length;
                int sr = (start >> 16) & 0xFF, sg = (start >> 8) & 0xFF, sb = start & 0xFF;
                int er = (end >> 16) & 0xFF, eg = (end >> 8) & 0xFF, eb = end & 0xFF;
                int r = (int) Math.round(sr + (er - sr) * t);
                int g = (int) Math.round(sg + (eg - sg) * t);
                int b = (int) Math.round(sb + (eb - sb) * t);
                int rgb = (r << 16) | (g << 8) | b;
                result.append(Text.literal(new String(Character.toChars(codePoint)))
                        .setStyle(Style.EMPTY.withColor(rgb).withBold(bold).withItalic(false)));
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
