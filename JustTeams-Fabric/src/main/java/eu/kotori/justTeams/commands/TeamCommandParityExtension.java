package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.gui.TeamGuiManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Exact command/message parity overrides for the user-facing team commands. */
public final class TeamCommandParityExtension {
    private static final int TEAM_BLUE = 0x4C9DDE;
    private TeamCommandParityExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;
        team.addChild(buildInvite().build());
        team.addChild(buildLeave().build());
        team.addChild(buildAccept().build());
        team.addChild(buildDeny().build());
        team.addChild(buildJoin().build());
        team.addChild(buildUnjoin().build());
        team.addChild(buildPvp().build());
        team.addChild(buildChat().build());
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildInvite() {
        return CommandManager.literal("invite")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(c -> executeInvite(c.getSource(), StringArgumentType.getString(c, "player"))))
                .executes(c -> usage(c.getSource(), "/team invite <player>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildLeave() {
        return CommandManager.literal("leave")
                .then(CommandManager.argument("teamName", StringArgumentType.word())
                        .suggests((context, builder) -> builder.buildFuture())
                        .executes(c -> executeLeave(c.getSource(), StringArgumentType.getString(c, "teamName"))))
                .executes(c -> usage(c.getSource(), "/team leave <teamName>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildAccept() {
        return CommandManager.literal("accept")
                .then(CommandManager.argument("team", StringArgumentType.word())
                        .executes(c -> executeAccept(c.getSource(), StringArgumentType.getString(c, "team"))))
                .executes(c -> usage(c.getSource(), "/team accept <teamName>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildDeny() {
        return CommandManager.literal("deny")
                .then(CommandManager.argument("team", StringArgumentType.word())
                        .executes(c -> executeDeny(c.getSource(), StringArgumentType.getString(c, "team"))))
                .executes(c -> usage(c.getSource(), "/team deny <teamName>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildJoin() {
        return CommandManager.literal("join")
                .then(CommandManager.argument("team", StringArgumentType.word())
                        .executes(c -> executeJoin(c.getSource(), StringArgumentType.getString(c, "team"))))
                .executes(c -> usage(c.getSource(), "/team join <teamName>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildUnjoin() {
        return CommandManager.literal("unjoin")
                .then(CommandManager.argument("team", StringArgumentType.word())
                        .executes(c -> executeUnjoin(c.getSource(), StringArgumentType.getString(c, "team"))))
                .executes(c -> usage(c.getSource(), "/team unjoin <teamName>"));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildPvp() {
        return CommandManager.literal("pvp")
                .executes(c -> executePvp(c.getSource()));
    }

    private static CommandManager.CommandNodeBuilder<ServerCommandSource, ?> buildChat() {
        return CommandManager.literal("chat")
                .executes(c -> executeChat(c.getSource()));
    }

    private static int executeInvite(ServerCommandSource source, String targetName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_INVITE);
            Team team = requireTeam(player);
            TeamPlayer member = team.getMember(player.getUuid());
            if (member == null || !member.canInvite()) throw new IllegalStateException("You do not have permission to invite players.");
            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
            if (target == null) throw new IllegalStateException("Player not found or is not online.");
            if (target.getUuid().equals(player.getUuid())) throw new IllegalStateException("You cannot invite yourself to your team.");
            if (JustTeamsFabric.teams().isInTeam(target.getUuid())) throw new IllegalStateException("That player is already in a team.");
            team.addInvite(target.getUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> prefix().append(Text.literal("You have invited ").setStyle(white())).append(Text.literal(target.getName().getString()).setStyle(white())).append(Text.literal(" to your team.").setStyle(white())), false);
            Text received = prefix()
                    .append(Text.literal("You have been invited to join ").setStyle(white()))
                    .append(Text.literal(team.getName()).setStyle(white()))
                    .append(Text.literal(". ").setStyle(white()))
                    .append(clickable("[Accept]", "/team accept " + team.getName(), Formatting.GREEN))
                    .append(Text.literal(" ").setStyle(white()))
                    .append(Text.literal("or ").setStyle(white()))
                    .append(clickable("[Deny]", "/team deny " + team.getName(), Formatting.RED));
            target.sendMessage(received, false);
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeLeave(ServerCommandSource source, String teamName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_LEAVE);
            Team team = requireTeam(player);
            if (!team.getName().equalsIgnoreCase(teamName)) throw new IllegalStateException("You are not a member of that team.");
            if (team.isOwner(player.getUuid())) throw new IllegalStateException("As the owner, you cannot leave the team. Transfer ownership or disband it instead.");
            TeamPlayer member = team.getMember(player.getUuid());
            if (member != null) { member.setAutoBankEnabled(false); member.setTeamChatEnabled(false); }
            TeamNotificationManager.notifyLeave(source.getServer(), team, player.getUuid());
            JustTeamsFabric.teams().removeMember(team, player.getUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.closeHandledScreen();
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeAccept(ServerCommandSource source, String teamName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_ACCEPT);
            if (JustTeamsFabric.teams().isInTeam(player.getUuid())) throw new IllegalStateException("You are already in a team. Leave your current one first.");
            Team team = findTeam(teamName);
            if (team == null || !team.hasInvite(player.getUuid())) throw new IllegalStateException("You do not have a pending invite from this team.");
            team.removeInvite(player.getUuid());
            TeamPlayer member = new TeamPlayer(player.getUuid(), eu.kotori.justTeams.team.TeamRole.MEMBER, java.time.Instant.now(), false, false, false, false);
            member.setLastKnownName(player.getName().getString());
            JustTeamsFabric.teams().addMember(team, member);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            JustTeamsFabric.glow().refreshAll(source.getServer());
            player.sendMessage(prefix().append(Text.literal("You have joined the team ").setStyle(white())).append(Text.literal(team.getName()).setStyle(white())).append(Text.literal(".").setStyle(white())), false);
            TeamNotificationManager.notifyJoinRequestAccepted(source.getServer(), team, player.getUuid());
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeDeny(ServerCommandSource source, String teamName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_DENY);
            Team team = findTeam(teamName);
            if (team == null || !team.hasInvite(player.getUuid())) throw new IllegalStateException("You do not have a pending invite from this team.");
            team.removeInvite(player.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.sendMessage(prefix().append(Text.literal("You have denied the invitation from ").setStyle(white())).append(Text.literal(team.getName()).setStyle(white())).append(Text.literal(".").setStyle(white())), false);
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeJoin(ServerCommandSource source, String teamName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_JOIN);
            if (JustTeamsFabric.teams().isInTeam(player.getUuid())) throw new IllegalStateException("You are already in a team. Leave your current one first.");
            Team team = findTeam(teamName);
            if (team == null) throw new IllegalStateException("A team with that name does not exist.");
            team.addJoinRequest(player.getUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            TeamNotificationManager.notifyJoinRequestSent(source.getServer(), player, team);
            TeamNotificationManager.notifyJoinRequest(source.getServer(), team, player.getUuid());
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeUnjoin(ServerCommandSource source, String teamName) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_UNJOIN);
            Team team = findTeam(teamName);
            if (team == null) throw new IllegalStateException("A team with that name does not exist.");
            team.removeJoinRequest(player.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.sendMessage(prefix().append(Text.literal("You have withdrawn your request to join ").setStyle(white())).append(Text.literal(team.getName()).setStyle(white())).append(Text.literal(".").setStyle(white())), false);
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executePvp(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow(); requirePermission(player, JustTeamsPermissions.COMMAND_PVP);
            Team team = requireTeam(player); if (!team.isOwner(player.getUuid())) throw new IllegalStateException("Only the team owner can change friendly fire.");
            team.setPvpEnabled(!team.isPvpEnabled()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.sendMessage(prefix().append(Text.literal(team.isPvpEnabled() ? "Team PvP has been enabled." : "Team PvP has been disabled.").setStyle(Style.EMPTY.withColor(team.isPvpEnabled() ? Formatting.GREEN : Formatting.RED).withItalic(false))), false);
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static int executeChat(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow(); requirePermission(player, JustTeamsPermissions.COMMAND_CHAT); Team team = requireTeam(player);
            TeamPlayer member = team.getMember(player.getUuid()); if (member == null) throw new IllegalStateException("You are not a member of that team.");
            boolean enabled = !member.isTeamChatEnabled(); member.setTeamChatEnabled(enabled); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            player.sendMessage(prefix().append(Text.literal(enabled ? "Team chat enabled. All messages will now be sent to your team." : "Team chat disabled. Messages will now be public.").setStyle(Style.EMPTY.withColor(enabled ? Formatting.GREEN : Formatting.RED).withItalic(false))), false);
            return 1;
        } catch (Exception exception) { return failure(source, exception); }
    }

    private static Team requireTeam(ServerPlayerEntity player) { Team team = JustTeamsFabric.teams().getTeam(player.getUuid()); if (team == null) throw new IllegalStateException("You are not in a team."); return team; }
    private static Team findTeam(String name) { return JustTeamsFabric.teams().getTeams().stream().filter(team -> team.getName().equalsIgnoreCase(name)).findFirst().orElse(null); }
    private static void requirePermission(ServerPlayerEntity player, String permission) { if (!JustTeamsFabric.permissions().has(player, permission)) throw new IllegalStateException("You do not have permission to use this command."); }
    private static int usage(ServerCommandSource source, String usage) { source.sendMessage(prefix().append(Text.literal("Usage: " + usage).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))); return 0; }
    private static int failure(ServerCommandSource source, Exception exception) { Text message = exception.getMessage() != null && exception.getMessage().equals("You are already in a team. Leave your current one first.") ? prefix().append(Text.literal(exception.getMessage()).setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false))) : prefix().append(Text.literal(exception.getMessage() == null ? "Command failed." : exception.getMessage()).setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false))); source.sendMessage(message); return 0; }
    private static Text prefix() { return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false)); }
    private static Style white() { return Style.EMPTY.withColor(Formatting.WHITE).withItalic(false); }
    private static Text clickable(String text, String command, Formatting color) { return Text.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run " + command)))); }
}
