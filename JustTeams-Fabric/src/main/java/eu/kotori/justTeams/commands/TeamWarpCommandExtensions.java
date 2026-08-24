package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamEnderChest;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds command-tree extensions whose underlying command already exists in the core /team tree. */
public final class TeamWarpCommandExtensions {
    private TeamWarpCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        registerWarpPassword(dispatcher);
        registerMemberManagement(dispatcher);
    }

    private static void registerWarpPassword(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        CommandNode<ServerCommandSource> warp = team.getChild("warp");
        if (warp == null) return;
        CommandNode<ServerCommandSource> set = warp.getChild("set");
        if (set == null) return;
        CommandNode<ServerCommandSource> name = set.getChild("name");
        if (name == null) return;

        name.addChild(CommandManager.argument("password", StringArgumentType.greedyString())
                .executes(context -> setWarpWithPassword(
                        context.getSource(),
                        StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "password")))
                .build());
    }

    private static void registerMemberManagement(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("kick")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> kick(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .build());

        team.addChild(CommandManager.literal("promote")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> promote(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .build());

        team.addChild(CommandManager.literal("demote")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> demote(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .build());
    }

    private static int setWarpWithPassword(ServerCommandSource source, String name, String password) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
            if (team == null) {
                source.sendError(Text.literal("You are not in a team."));
                return 0;
            }

            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_SETWARP)) {
                source.sendError(Text.literal("You do not have permission to create team warps."));
                return 0;
            }

            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canSetHome()) {
                source.sendError(Text.literal("You do not have permission to create team warps."));
                return 0;
            }
            if (name.length() > 32) {
                source.sendError(Text.literal("Warp name must be 32 characters or fewer."));
                return 0;
            }
            if (team.getWarp(name) != null) {
                source.sendError(Text.literal("A warp with that name already exists."));
                return 0;
            }
            if (password.length() > 64) {
                source.sendError(Text.literal("Warp passwords may not exceed 64 characters."));
                return 0;
            }
            if (!FeatureCostManager.charge(player, "setwarp")) return 0;

            TeamLocation location = TeamLocation.fromPlayer(player);
            TeamWarp warp = new TeamWarp(
                    name,
                    player.getUuid(),
                    location.getDimension(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch());
            warp.setPassword(password);
            team.addWarp(warp);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team warp '" + name + "' created."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to create the team warp."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to create team warp {}", name, exception);
            return 0;
        }
    }

    private static int kick(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            Team team = requireTeam(actor);
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.COMMAND_KICK)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }

            TeamPlayer actorMember = team.getMember(actor.getUuid());
            PlayerConfigEntry targetEntry = resolveTarget(source, targetName);
            if (targetEntry == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }

            if (actorMember == null || !actorMember.canKickPlayer(team.getMember(targetEntry.id()))) {
                source.sendError(Text.literal("You do not have permission to kick that player."));
                return 0;
            }

            TeamPlayer target = team.getMember(targetEntry.id());
            if (target == null) {
                source.sendError(Text.literal("That player is not in your team."));
                return 0;
            }

            JustTeamsFabric.teams().removeMember(team, target.getPlayerUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            TeamNotificationManager.notifyKick(actor.getServer(), team, actor.getUuid(), target.getPlayerUuid());
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to kick that player."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to kick team member {}", targetName, exception);
            return 0;
        }
    }

    private static int promote(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            Team team = requireTeam(actor);
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.COMMAND_PROMOTE)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            if (!team.isOwner(actor.getUuid())) {
                source.sendError(Text.literal("Only the team owner can promote players."));
                return 0;
            }

            PlayerConfigEntry targetEntry = resolveTarget(source, targetName);
            if (targetEntry == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }
            TeamPlayer target = team.getMember(targetEntry.id());
            if (target == null) {
                source.sendError(Text.literal("That player is not in your team."));
                return 0;
            }
            if (target.getRole() == TeamRole.CO_OWNER) {
                source.sendError(Text.literal("That player is already a co-owner."));
                return 0;
            }
            if (target.getRole() == TeamRole.OWNER) {
                source.sendError(Text.literal("You cannot promote the team owner."));
                return 0;
            }

            setCoOwnerPermissions(target);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            refreshGlow(actor, target);
            notifyRoleChange(actor, team, target, true);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to promote that player."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to promote team member {}", targetName, exception);
            return 0;
        }
    }

    private static int demote(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            Team team = requireTeam(actor);
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.COMMAND_DEMOTE)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            if (!team.isOwner(actor.getUuid())) {
                source.sendError(Text.literal("Only the team owner can demote players."));
                return 0;
            }

            PlayerConfigEntry targetEntry = resolveTarget(source, targetName);
            if (targetEntry == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }
            TeamPlayer target = team.getMember(targetEntry.id());
            if (target == null) {
                source.sendError(Text.literal("That player is not in your team."));
                return 0;
            }
            if (target.getRole() == TeamRole.MEMBER) {
                source.sendError(Text.literal("That player is already a member."));
                return 0;
            }
            if (target.getRole() == TeamRole.OWNER) {
                source.sendError(Text.literal("You cannot demote the team owner."));
                return 0;
            }

            setMemberPermissions(target);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            refreshGlow(actor, target);
            notifyRoleChange(actor, team, target, false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null
                    ? "Unable to demote that player."
                    : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Failed to demote team member {}", targetName, exception);
            return 0;
        }
    }

    private static Team requireTeam(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        return team;
    }

    private static PlayerConfigEntry resolveTarget(ServerCommandSource source, String targetName) {
        ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(targetName);
        if (online != null) return new PlayerConfigEntry(online.getUuid(), online.getName().getString());
        return source.getServer().getUserCache().findByName(targetName).orElse(null);
    }

    private static void setCoOwnerPermissions(TeamPlayer target) {
        target.setRole(TeamRole.CO_OWNER);
        target.setCanWithdraw(true);
        target.setCanUseEnderChest(true);
        target.setCanSetHome(true);
        target.setCanUseHome(true);
        target.setCanEditMembers(true);
        target.setCanEditCoOwners(false);
        target.setCanKickMembers(true);
        target.setCanPromoteMembers(false);
        target.setCanDemoteMembers(false);
    }

    private static void setMemberPermissions(TeamPlayer target) {
        target.setRole(TeamRole.MEMBER);
        target.setCanWithdraw(false);
        target.setCanUseEnderChest(true);
        target.setCanSetHome(false);
        target.setCanUseHome(true);
        target.setCanEditMembers(false);
        target.setCanEditCoOwners(false);
        target.setCanKickMembers(false);
        target.setCanPromoteMembers(false);
        target.setCanDemoteMembers(false);
    }

    private static void refreshGlow(ServerPlayerEntity actor, TeamPlayer target) {
        ServerPlayerEntity online = actor.getServer().getPlayerManager().getPlayer(target.getPlayerUuid());
        if (online != null) JustTeamsFabric.glow().refreshAll(actor.getServer());
    }

    private static void notifyRoleChange(ServerPlayerEntity actor, Team team, TeamPlayer target, boolean promoted) {
        String targetName = actor.getServer().getPlayerManager().getPlayer(target.getPlayerUuid()) != null
                ? actor.getServer().getPlayerManager().getPlayer(target.getPlayerUuid()).getName().getString()
                : target.getPlayerUuid().toString();
        Text actorMessage = Text.literal(promoted
                ? "You promoted " + targetName + " to co-owner."
                : "You demoted " + targetName + " to member.");
        actor.sendMessage(actorMessage, false);
        Text broadcast = Text.literal(promoted
                ? targetName + " is now a team co-owner."
                : targetName + " is now a team member.");
        for (TeamPlayer member : team.getMembers()) {
            ServerPlayerEntity player = actor.getServer().getPlayerManager().getPlayer(member.getPlayerUuid());
            if (player != null && !player.getUuid().equals(actor.getUuid())) player.sendMessage(broadcast, false);
        }
        ServerPlayerEntity targetPlayer = actor.getServer().getPlayerManager().getPlayer(target.getPlayerUuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Text.literal(promoted
                    ? "You were promoted to co-owner of " + team.getName() + "."
                    : "You were demoted to member of " + team.getName() + "."), false);
        }
    }
}