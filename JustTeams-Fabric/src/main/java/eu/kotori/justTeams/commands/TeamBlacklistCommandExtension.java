package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamBlacklistGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;

public final class TeamBlacklistCommandExtension {
    private TeamBlacklistCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.getRoot().getChild("team").addChild(CommandManager.literal("blacklist")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> blacklist(context.getSource(), StringArgumentType.getString(context, "player"), "Blacklisted by team management."))
                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> blacklist(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "reason")))))
                .build());

        dispatcher.getRoot().getChild("team").addChild(CommandManager.literal("unblacklist")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> unblacklist(context.getSource(), StringArgumentType.getString(context, "player"))))
                .build());

        dispatcher.getRoot().getChild("team").addChild(CommandManager.literal("blacklistgui")
                .executes(context -> openGui(context.getSource()))
                .build());
    }

    private static int blacklist(ServerCommandSource source, String targetName, String reason) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.USER)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            Team team = requireElevatedTeam(actor);
            PlayerConfigEntry target = resolveTarget(source, targetName);
            if (target == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }
            if (target.id().equals(actor.getUuid())) {
                source.sendError(Text.literal("You cannot blacklist yourself."));
                return 0;
            }
            if (team.isMember(target.id())) {
                source.sendError(Text.literal("You cannot blacklist a current team member."));
                return 0;
            }
            String cleanReason = reason == null || reason.isBlank() ? "Blacklisted by team management." : reason.trim();
            team.addBlacklistEntry(new BlacklistedPlayer(
                    target.id(), target.name(), cleanReason, actor.getUuid(), actor.getName().getString(), Instant.now()));
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            ServerPlayerEntity targetPlayer = actor.getEntityWorld().getServer().getPlayerManager().getPlayer(target.id());
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Text.literal("You have been blacklisted from joining " + team.getName() + "."), false);
            }
            actor.sendMessage(Text.literal("Blacklisted " + target.name() + " from " + team.getName() + "."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to blacklist player." : exception.getMessage()));
            return 0;
        }
    }

    private static int unblacklist(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.USER)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            Team team = requireElevatedTeam(actor);
            PlayerConfigEntry target = resolveTarget(source, targetName);
            if (target == null) {
                source.sendError(Text.literal("Player not found."));
                return 0;
            }
            if (!team.removeBlacklistEntry(target.id())) {
                source.sendError(Text.literal("That player is not blacklisted from your team."));
                return 0;
            }
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            actor.sendMessage(Text.literal("Removed " + target.name() + " from the team blacklist."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to remove blacklist entry." : exception.getMessage()));
            return 0;
        }
    }

    private static int openGui(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.USER)) return 0;
            requireElevatedTeam(player);
            TeamBlacklistGui.open(player);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to open blacklist." : exception.getMessage()));
            return 0;
        }
    }

    private static Team requireElevatedTeam(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        if (!team.hasElevatedPermissions(player.getUuid())) {
            throw new IllegalStateException("Only the owner or co-owner can manage the team blacklist.");
        }
        return team;
    }

    private static PlayerConfigEntry resolveTarget(ServerCommandSource source, String targetName) {
        ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(targetName);
        if (online != null) return new PlayerConfigEntry(online.getUuid(), online.getName().getString());
        return source.getServer().getApiServices().nameToIdCache().findByName(targetName).orElse(null);
    }
}
