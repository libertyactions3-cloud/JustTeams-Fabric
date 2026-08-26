package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.gui.TeamBlacklistGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;

public final class TeamBlacklistCommandExtension {
    private TeamBlacklistCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("blacklist")
                .executes(context -> openGui(context.getSource()))
                .then(CommandManager.argument("player", StringArgumentType.word()).suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context -> blacklist(context.getSource(), StringArgumentType.getString(context, "player"), "No reason specified"))
                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> blacklist(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "reason")))))
                .build());

        team.addChild(CommandManager.literal("unblacklist")
                .then(CommandManager.argument("player", StringArgumentType.word()).suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context -> unblacklist(context.getSource(), StringArgumentType.getString(context, "player"))))
                .build());
    }

    private static int blacklist(ServerCommandSource source, String targetName, String reason) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.USER)) throw new IllegalStateException("You do not have permission to use this command.");
            Team team = requireElevatedTeam(actor);
            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
            if (target == null) throw new IllegalStateException("Player not found.");
            if (target.getUuid().equals(actor.getUuid())) throw new IllegalStateException("You cannot blacklist yourself.");
            if (team.isMember(target.getUuid())) throw new IllegalStateException("You cannot blacklist a current team member.");
            if (team.isBlacklisted(target.getUuid())) throw new IllegalStateException("That player is already blacklisted from your team.");
            String cleanReason = reason == null || reason.isBlank() ? "No reason specified" : reason.trim();
            team.addBlacklistEntry(new BlacklistedPlayer(target.getUuid(), target.getName().getString(), cleanReason, actor.getUuid(), actor.getName().getString(), Instant.now()));
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            target.sendMessage(Text.literal("You have been blacklisted from joining " + team.getName() + "."), false);
            actor.sendMessage(Text.literal("Blacklisted " + target.getName().getString() + " from " + team.getName() + "."), false); return 1;
        } catch (Exception exception) { source.sendError(Text.literal(exception.getMessage() == null ? "Unable to blacklist player." : exception.getMessage())); return 0; }
    }

    private static int unblacklist(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity actor = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(actor, JustTeamsPermissions.USER)) throw new IllegalStateException("You do not have permission to use this command.");
            Team team = requireElevatedTeam(actor);
            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
            if (target == null) throw new IllegalStateException("Player not found.");
            if (target.getUuid().equals(actor.getUuid())) throw new IllegalStateException("You cannot unblacklist yourself.");
            if (!team.removeBlacklistEntry(target.getUuid())) throw new IllegalStateException("That player is not blacklisted from your team.");
            JustTeamsFabric.storage().save(JustTeamsFabric.teams()); actor.sendMessage(Text.literal("Removed " + target.getName().getString() + " from the team blacklist."), false); return 1;
        } catch (Exception exception) { source.sendError(Text.literal(exception.getMessage() == null ? "Unable to remove blacklist entry." : exception.getMessage())); return 0; }
    }

    private static int openGui(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.USER)) throw new IllegalStateException("You do not have permission to use this command.");
            requireElevatedTeam(player); TeamBlacklistGui.open(player); return 1;
        } catch (Exception exception) { source.sendError(Text.literal(exception.getMessage() == null ? "Unable to open blacklist." : exception.getMessage())); return 0; }
    }

    private static Team requireElevatedTeam(ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        if (!team.hasElevatedPermissions(player.getUuid())) throw new IllegalStateException("Only the owner or co-owner can manage the team blacklist.");
        return team;
    }
}
