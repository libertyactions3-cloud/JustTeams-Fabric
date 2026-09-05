package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side Team Warps inventory GUI. */
public final class TeamWarpGui {
    private static final int[] WARP_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PRIMARY_START = 0x4C9D9D;
    private static final int PRIMARY_END = 0x4C96D2;

    private TeamWarpGui() {}

    public static void open(PlayerEntity player, Team team) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new Handler(syncId, inventory, player.getUuid(), team),
                Text.literal("ᴛᴇᴀᴍ ᴡᴀʀᴘs").setStyle(Style.EMPTY.withItalic(false))
        ));
    }

    private static void beginCreate(ServerPlayerEntity player, Team team) {
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canSetHome()) {
            player.sendMessage(Text.literal("You do not have permission to create team warps."), true);
            return;
        }
        player.closeHandledScreen();
        TeamStringInputGui.open(player, "New Team Warp", "Enter warp name", name -> {
            if (!name.matches("[A-Za-z0-9_-]{1,32}")) {
                player.sendMessage(Text.literal("Warp names may contain only letters, numbers, underscores and hyphens (max 32)."), true);
                open(player, team);
                return;
            }
            if (team.getWarp(name) != null) {
                player.sendMessage(Text.literal("A warp with that name already exists."), true);
                open(player, team);
                return;
            }
            TeamStringInputGui.open(player, "Warp Password", "Enter password or type NONE", password -> {
                createWarp(player, team, name, password.equalsIgnoreCase("NONE") ? "" : password);
                open(player, team);
            }, () -> open(player, team));
        }, () -> open(player, team));
    }

    private static void createWarp(ServerPlayerEntity player, Team team, String name, String password) {
        if (!FeatureCostManager.charge(player, "setwarp")) return;
        var world = player.getEntityWorld();
        TeamWarp warp = new TeamWarp(name, player.getUuid(), world.getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        warp.setPassword(password);
        try {
            team.addWarp(warp);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.sendMessage(Text.literal("Created team warp " + name + "."), true);
        } catch (IllegalArgumentException | IOException exception) {
            player.sendMessage(Text.literal("Unable to save the team warp."), true);
            JustTeamsFabric.LOGGER.error("Failed to create team warp {}", name, exception);
        }
    }

    private static final class Handler extends ScreenHandler {
        private final Inventory menuInventory = new SimpleInventory(54);
        private final UUID viewerUuid;
        private final Team team;

        private Handler(int syncId, PlayerInventory playerInventory, UUID viewerUuid, Team team) {
            super(ScreenHandlerType.GENERIC_9X6, syncId);
            this.viewerUuid = viewerUuid;
            this.team = team;
            populate();
            for (int row = 0; row < 6; row++) for (int column = 0; column < 9; column++)
                addSlot(new MenuSlot(menuInventory, row * 9 + column, 8 + column * 18, 18 + row * 18));
            addPlayerInventory(playerInventory);
        }

        private void addPlayerInventory(PlayerInventory inventory) {
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 198));
        }

        private void populate() {
            ItemStack filler = namedPlain(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < menuInventory.size(); i++) menuInventory.setStack(i, filler.copy());

            ItemStack header = namedGradient(Items.COMPASS, "ᴛᴇᴀᴍ ᴡᴀʀᴘs");
            menuInventory.setStack(4, header);

            List<TeamWarp> warps = new ArrayList<>(team.getWarps());
            if (warps.isEmpty()) {
                ItemStack empty = new ItemStack(Items.PAPER);
                empty.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("No Warps Set").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
                empty.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        plainLine("Your team has not set any warps yet.", Formatting.GRAY),
                        plainLine("", Formatting.GRAY),
                        plainLine("Use /team setwarp <name> to create one.", Formatting.WHITE)
                )));
                menuInventory.setStack(22, empty);
            } else {
                for (int i = 0; i < WARP_SLOTS.length && i < warps.size(); i++) {
                    menuInventory.setStack(WARP_SLOTS[i], createWarpItem(warps.get(i)));
                }
            }

            ItemStack back = namedPlain(Items.ARROW, "ʙᴀᴄᴋ");
            back.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("ʙᴀᴄᴋ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(true).withItalic(false)));
            back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    plainLine("Click to return to the main menu.", Formatting.YELLOW)
            )));
            menuInventory.setStack(49, back);
        }

        private ItemStack createWarpItem(TeamWarp warp) {
            boolean passwordProtected = !warp.getPassword().isEmpty();
            ItemStack stack = new ItemStack(passwordProtected ? Items.IRON_BLOCK : Items.GOLD_BLOCK);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(warp.getName(), true));
            String serverName = serverName(warp.getWorld());
            MutableText protection = passwordProtected
                    ? plainLine("Password Protected", Formatting.RED)
                    : plainLine("Public", Formatting.GREEN);
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    composeLine("Server: ", serverName, Formatting.GRAY, Formatting.WHITE),
                    plainLine("", Formatting.GRAY),
                    protection
            )));
            return stack;
        }

        private String serverName(String world) {
            int separator = world.lastIndexOf(':');
            return separator >= 0 && separator + 1 < world.length() ? world.substring(separator + 1) : world;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (slotIndex < 0 || slotIndex >= menuInventory.size()) {
                super.onSlotClick(slotIndex, button, actionType, player);
                return;
            }
            if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP || actionType == SlotActionType.THROW || actionType == SlotActionType.CLONE)
                return;
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !player.getUuid().equals(viewerUuid) || !team.isMember(viewerUuid)) return;

            if (slotIndex == 49) {
                TeamGuiManager.openMain(serverPlayer);
                return;
            }
            if (slotIndex == 45) {
                beginCreate(serverPlayer, team);
                return;
            }

            for (int i = 0; i < WARP_SLOTS.length; i++) {
                if (WARP_SLOTS[i] == slotIndex && i < team.getWarps().size()) {
                    TeamWarp warp = team.getWarps().get(i);
                    if (button == 1 && team.hasElevatedPermissions(viewerUuid)) {
                        TeamWarpManagementGui.open(serverPlayer, team, warp);
                    } else {
                        useWarp(serverPlayer, warp);
                    }
                    return;
                }
            }
        }

        private void useWarp(ServerPlayerEntity player, TeamWarp warp) {
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null) return;
            if (!warp.isEnabled()) { player.sendMessage(Text.literal("That warp is disabled."), true); return; }
            if (!warp.isMembersCanUse() && !team.isOwner(player.getUuid()) && !warp.getOwner().equals(player.getUuid())) {
                player.sendMessage(Text.literal("You do not have permission to use that warp."), true);
                return;
            }

            if (!warp.getPassword().isEmpty()) {
                TeamStringInputGui.open(player, "Warp Password", "Enter password", value -> {
                    if (!warp.getPassword().equals(value)) {
                        player.sendMessage(Text.literal("Incorrect warp password."), true);
                        open(player, team);
                        return;
                    }
                    requestTeleport(player, warp);
                }, () -> open(player, team));
                return;
            }
            requestTeleport(player, warp);
        }

        private void requestTeleport(ServerPlayerEntity player, TeamWarp warp) {
            JustTeamsFabric.teleports().requestWarp(player,
                    new TeamLocation(warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch()),
                    warp.getCost());
        }

        private static ItemStack namedPlain(Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
            return stack;
        }

        private static ItemStack namedGradient(Item item, String name) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponentTypes.CUSTOM_NAME, gradientText(name, true));
            return stack;
        }

        private static MutableText plainLine(String text, Formatting color) {
            return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
        }

        private static MutableText composeLine(String prefix, String value, Formatting prefixColor, Formatting valueColor) {
            return plainLine(prefix, prefixColor).append(plainLine(value, valueColor));
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
                int rgb = (r << 16) | (g << 8) | b;
                result.append(Text.literal(new String(Character.toChars(codePoint)))
                        .setStyle(Style.EMPTY.withColor(rgb).withBold(bold).withItalic(false)));
                offset += Character.charCount(codePoint);
                index++;
            }
            return result;
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return player.getUuid().equals(viewerUuid) && team.isMember(viewerUuid); }

        private static final class MenuSlot extends Slot {
            private MenuSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { return false; }
        }
    }
}
