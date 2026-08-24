package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamConfirmationGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds the ownership-transfer command using the verified 2.5.3 confirmation flow. */
public final class TeamOwnershipCommandExtensions {
    private TeamOwnershipCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("transfer")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> transfer(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .build());
    }

    private static int transfer(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.COMMAND_TRANSFER)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }

            Team team = JustTeamsFabric.teams().getTeam(actor.getUuid());
            if (team == null || !team.isOwner(actor.getUuid())) {
                source.sendError(Text.literal("Only the team owner can transfer ownership."));
                return 0;
            }

            PlayerConfigEntry targetEntry = resolveTarget(source, targetName);
            if (targetEntry == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }
            if (targetEntry.id().equals(actor.getUuid())) {
                source.sendError(Text.literal("You are already the team owner."));
                return 0;
            }

            TeamPlayer target = team.getMember(targetEntry.id());
            if (target == null) {
                source.sendError(Text.literal("That player is not in your team."));
                return 0;
            }

            String targetDisplayName = targetEntry.name();
            TeamConfirmationGui.open(
                    actor,
                    "Transfer Ownership",
                    "Transfer ownership to " + targetDisplayName + "?",
                    () -> applyTransfer(actor, team, target),
                    () -> actor.sendMessage(Text.literal("Ownership transfer cancelled."), false));
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to begin ownership transfer."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to begin ownership transfer to {}", targetName, exception);
            return 0;
        }
    }

    private static PlayerConfigEntry resolveTarget(ServerCommandSource source, String targetName) {
        ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(targetName);
        if (online != null) return new PlayerConfigEntry(online.getUuid(), online.getName().getString());
        return source.getServer().getUserCache().findByName(targetName).orElse(null);
    }

    private static void applyTransfer(ServerPlayerEntity oldOwner, Team team, TeamPlayer target) {
        ServerPlayerEntity onlineTarget = oldOwner.getServer().getPlayerManager().getPlayer(target.getPlayerUuid());

        TeamPlayer oldOwnerMember = team.getMember(oldOwner.getUuid());
        if (oldOwnerMember == null || target == null || target.getPlayerUuid().equals(oldOwner.getUuid())) {
            oldOwner.sendMessage(Text.literal("Ownership transfer could not be completed."), false);
            return;
        }

        team.setOwnerUuid(target.getPlayerUuid());
        setOwnerPermissions(target);
        setMemberPermissions(oldOwnerMember);

        try {
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        } catch (Exception exception) {
            JustTeamsFabric.LOGGER.error("Failed to save ownership transfer for team {}", team.getName(), exception);
            oldOwner.sendMessage(Text.literal("Ownership transfer could not be saved."), false);
            return;
        }

        JustTeamsFabric.glow().refreshAll(oldOwner.getServer());

        oldOwner.sendMessage(Text.literal("You transferred ownership of " + team.getName() + " to "
                + (onlineTarget != null ? onlineTarget.getName().getString() : target.getPlayerUuid()) + "."), false);
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Text.literal("You are now the owner of " + team.getName() + "."), false);
        }
        for (TeamPlayer member : team.getMembers()) {
            ServerPlayerEntity player = oldOwner.getServer().getPlayerManager().getPlayer(member.getPlayerUuid());
            if (player != null && !player.getUuid().equals(oldOwner.getUuid())
                    && (onlineTarget == null || !player.getUuid().equals(onlineTarget.getUuid()))) {
                player.sendMessage(Text.literal(onlineTarget != null
                        ? onlineTarget.getName().getString() + " is now the owner of the team."
                        : "Team ownership was transferred."), false);
            }
        }
    }

    private static void setOwnerPermissions(TeamPlayer player) {
        player.setRole(TeamRole.OWNER);
        player.setCanWithdraw(true);
        player.setCanUseEnderChest(true);
        player.setCanSetHome(true);
        player.setCanUseHome(true);
        player.setCanEditMembers(true);
        player.setCanEditCoOwners(true);
        player.setCanKickMembers(true);
        player.setCanPromoteMembers(true);
        player.setCanDemoteMembers(true);
    }

    private static void setMemberPermissions(TeamPlayer player) {
        player.setRole(TeamRole.MEMBER);
        player.setCanWithdraw(false);
        player.setCanUseEnderChest(true);
        player.setCanSetHome(false);
        player.setCanUseHome(true);
        player.setCanEditMembers(false);
        player.setCanEditCoOwners(false);
        player.setCanKickMembers(false);
        player.setCanPromoteMembers(false);
        player.setCanDemoteMembers(false);
    }
}
