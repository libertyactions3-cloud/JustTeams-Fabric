package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.gui.TeamBankGui;
import eu.kotori.justTeams.gui.TeamDisbandConfirmationGui;
import eu.kotori.justTeams.gui.TeamEnderChestGui;
import eu.kotori.justTeams.gui.TeamGuiManager;
import eu.kotori.justTeams.gui.TeamInPlaceGui;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.time.Instant;

public final class TeamCommand {
    private TeamCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("team")
                .executes(c -> run(c.getSource(), JustTeamsPermissions.USER, () -> openGui(c.getSource())))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("tag", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_CREATE,
                                                () -> create(c.getSource(), StringArgumentType.getString(c, "name"), StringArgumentType.getString(c, "tag")))))))
                .then(CommandManager.literal("gui").executes(c -> run(c.getSource(), JustTeamsPermissions.USER, () -> openGui(c.getSource()))))
                .then(CommandManager.literal("info").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_INFO, () -> info(c.getSource()))))
                .then(CommandManager.literal("leave").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_LEAVE, () -> leave(c.getSource()))))
                .then(CommandManager.literal("disband").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_DISBAND, () -> disband(c.getSource()))))
                .then(CommandManager.literal("pvp").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_PVP, () -> togglePvp(c.getSource()))))
                .then(CommandManager.literal("glow").executes(c -> run(c.getSource(), JustTeamsPermissions.USER, () -> toggleGlow(c.getSource()))))
                .then(CommandManager.literal("bank").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_BANK, () -> openBank(c.getSource()))))
                .then(CommandManager.literal("enderchest").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_ENDERCHEST, () -> openEnderChest(c.getSource()))))
                .then(CommandManager.literal("ec").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_ENDERCHEST, () -> openEnderChest(c.getSource()))))
                .then(CommandManager.literal("sethome")
                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_SETHOME, () -> setHome(c.getSource()))))
                .then(CommandManager.literal("home")
                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_HOME, () -> useHome(c.getSource())))
                        .then(CommandManager.literal("clear").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_SETHOME, () -> clearHome(c.getSource())))))
                .then(CommandManager.literal("warp")
                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_WARPS, () -> listWarps(c.getSource())))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_SETWARP,
                                                () -> setWarp(c.getSource(), StringArgumentType.getString(c, "name"))))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_DELWARP,
                                                () -> removeWarp(c.getSource(), StringArgumentType.getString(c, "name"))))))
                        .then(CommandManager.literal("list").executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_WARPS, () -> listWarps(c.getSource()))))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_WARP,
                                        () -> useWarp(c.getSource(), StringArgumentType.getString(c, "name"), "")))
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_WARP,
                                                () -> useWarp(c.getSource(), StringArgumentType.getString(c, "name"), StringArgumentType.getString(c, "password")))))))
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_INVITE,
                                        () -> invite(c.getSource(), EntityArgumentType.getPlayer(c, "player"))))))
                .then(CommandManager.literal("accept")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_ACCEPT,
                                        () -> acceptInvite(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("deny")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_DENY,
                                        () -> denyInvite(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_JOIN,
                                        () -> requestJoin(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("unjoin")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_UNJOIN,
                                        () -> cancelJoinRequest(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("requests")
                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_REQUESTS, () -> openRequests(c.getSource()))))
                .then(CommandManager.literal("chat")
                        .executes(c -> run(c.getSource(), JustTeamsPermissions.COMMAND_CHAT, () -> toggleChat(c.getSource())))));
    }

    @FunctionalInterface
    private interface CommandAction { int run() throws Exception; }

    private static int run(ServerCommandSource source, String permission, CommandAction action) {
        try {
            if (source.getEntity() instanceof ServerPlayerEntity p && !JustTeamsFabric.permissions().has(p, permission)) {
                source.sendError(Text.literal("You do not have permission to use this command."));
                return 0;
            }
            return action.run();
        } catch (Exception e) {
            source.sendError(Text.literal(e.getMessage() == null ? "Command failed." : e.getMessage()));
            JustTeamsFabric.LOGGER.error("JustTeams command failed", e);
            return 0;
        }
    }

    private static int openGui(ServerCommandSource s) throws Exception { TeamGuiManager.openMain(s.getPlayerOrThrow()); return 1; }
    private static int openBank(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamBankGui.open(p, t); return 1; }
    private static int openEnderChest(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; } TeamEnderChestGui.open(p, t); return 1; }
    private static int toggleChat(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; } boolean enabled = TeamChatManager.toggle(p); s.sendFeedback(() -> Text.literal(enabled ? "Team chat enabled. Your chat messages will only be sent to team members." : "Team chat disabled. Your chat messages are public again."), false); return 1; }
    private static int create(ServerCommandSource s, String name, String tag) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); if (name.length() > 16 || tag.length() > 4) { s.sendError(Text.literal("Team name must be 16 characters or fewer and tag 4 characters or fewer.")); return 0; } try { Team t = JustTeamsFabric.teams().createTeam(name, tag, p.getUuid(), true, false, false); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Created team " + t.getName() + " [" + t.getTag() + "]"), false); return 1; } catch (IllegalStateException e) { s.sendError(Text.literal(e.getMessage())); return 0; } }
    private static int info(ServerCommandSource s) throws Exception { Team t = JustTeamsFabric.teams().getTeam(s.getPlayerOrThrow().getUuid()); if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; } s.sendFeedback(() -> Text.literal("Team: " + t.getName() + " [" + t.getTag() + "]"), false); s.sendFeedback(() -> Text.literal("Members: " + t.getMembers().size() + " | Friendly fire: " + (t.isPvpEnabled() ? "ON" : "OFF")), false); return 1; }
    private static int toggleGlow(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); boolean enabled = JustTeamsFabric.teams().toggleGlow(p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); JustTeamsFabric.glow().refreshAll(s.getServer()); s.sendFeedback(() -> Text.literal("Team glow is now " + (enabled ? "ON" : "OFF") + "."), false); return 1; }
    private static int leave(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; } if (t.isOwner(p.getUuid())) { s.sendError(Text.literal("The owner cannot leave the team. Use /team disband.")); return 0; } TeamChatManager.disable(p.getUuid()); JustTeamsFabric.glow().stopGlowForPlayer(s.getServer(), p.getUuid()); p.closeHandledScreen(); JustTeamsFabric.teams().removeMember(t, p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); TeamNotificationManager.notifyLeave(s.getServer(), t, p.getUuid()); return 1; }
    private static int disband(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null || !t.isOwner(p.getUuid())) { s.sendError(Text.literal("You do not own a team.")); return 0; } if (p.currentScreenHandler instanceof eu.kotori.justTeams.gui.TeamMenuHandler menu) TeamDisbandConfirmationGui.openFirst(menu, p, t); else { TeamGuiManager.openMain(p); if (p.currentScreenHandler instanceof eu.kotori.justTeams.gui.TeamMenuHandler menu) TeamDisbandConfirmationGui.openFirst(menu, p, t); } return 1; }
    private static int togglePvp(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null || !t.isOwner(p.getUuid())) { s.sendError(Text.literal("Only the team owner can change friendly fire.")); return 0; } t.setPvpEnabled(!t.isPvpEnabled()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Friendly fire is now " + (t.isPvpEnabled() ? "ON" : "OFF") + "."), false); return 1; }
    private static int setHome(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamPlayer m = t.getMember(p.getUuid()); if (m == null || !m.canSetHome()) { s.sendError(Text.literal("You do not have permission to set the team home.")); return 0; } if (!FeatureCostManager.charge(p, "sethome")) return 0; t.setHome(TeamLocation.fromPlayer(p)); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Team home set at your current location."), false); return 1; }
    private static int clearHome(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamPlayer m = t.getMember(p.getUuid()); if (m == null || !m.canSetHome()) { s.sendError(Text.literal("You do not have permission to clear the team home.")); return 0; } if (t.getHome() == null) { s.sendError(Text.literal("Your team does not have a home set.")); return 0; } t.clearHome(); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Team home cleared."), false); return 1; }
    private static int useHome(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamPlayer m = t.getMember(p.getUuid()); if (m == null || !m.canUseHome()) { s.sendError(Text.literal("You do not have permission to use the team home.")); return 0; } if (t.getHome() == null) { Text message = Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(Formatting.BLUE).withItalic(false)).append(Text.literal("Your team does not have a home set. An Owner or Co-Owner can set one with /team sethome").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false))); s.sendFeedback(() -> message, false); return 0; } JustTeamsFabric.teleports().requestHome(p, t.getHome()); return 1; }
    private static int setWarp(ServerCommandSource s, String name) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamPlayer m = t.getMember(p.getUuid()); if (m == null || !m.canSetWarps()) { s.sendError(Text.literal("You do not have permission to create team warps.")); return 0; } if (name.length() > 32) { s.sendError(Text.literal("Warp name must be 32 characters or fewer.")); return 0; } if (t.getWarp(name) != null) { s.sendError(Text.literal("A warp with that name already exists.")); return 0; } if (!FeatureCostManager.charge(p, "setwarp")) return 0; TeamLocation l = TeamLocation.fromPlayer(p); t.addWarp(new TeamWarp(name, p.getUuid(), l.getDimension(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch())); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Team warp '" + name + "' created."), false); return 1; }
    private static int removeWarp(ServerCommandSource s, String name) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamWarp w = t.getWarp(name); if (w == null) { s.sendError(Text.literal("Warp not found.")); return 0; } if (!t.isOwner(p.getUuid()) && !teamCoOwner(t, p.getUuid())) { s.sendError(Text.literal("Only the team owner or Co-Leader can remove this warp.")); return 0; } t.removeWarp(name); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Team warp '" + name + "' removed."), false); return 1; }
    private static boolean teamCoOwner(Team team, java.util.UUID uuid) { TeamPlayer member = team.getMember(uuid); return member != null && member.getRole() == TeamRole.CO_OWNER; }
    private static int listWarps(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); if (t.getWarps().isEmpty()) { s.sendFeedback(() -> Text.literal("Your team has no warps."), false); return 1; } s.sendFeedback(() -> Text.literal("Team warps:"), false); for (TeamWarp w : t.getWarps()) s.sendFeedback(() -> Text.literal("- " + w.getName() + (w.isEnabled() ? "" : " (disabled)")), false); return 1; }
    private static int useWarp(ServerCommandSource s, String name, String password) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamWarp w = t.getWarp(name); if (w == null) { s.sendError(Text.literal("Warp not found.")); return 0; } if (!w.isEnabled()) { s.sendError(Text.literal("This warp is disabled.")); return 0; } if (JustTeamsFabric.teleports().checkWarpCooldown(p)) return 0; if (!w.getPassword().isEmpty() && !w.getPassword().equals(password)) { s.sendError(Text.literal("Incorrect warp password.")); return 0; } JustTeamsFabric.teleports().requestWarp(p, new TeamLocation(w.getWorld(), w.getX(), w.getY(), w.getZ(), w.getYaw(), w.getPitch()), w.getCost()); return 1; }
    private static int invite(ServerCommandSource s, ServerPlayerEntity target) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); TeamPlayer member = t.getMember(p.getUuid()); if (member == null || !member.canInvite()) { s.sendError(Text.literal("You do not have permission to invite players.")); return 0; } if (target.getUuid().equals(p.getUuid())) { s.sendError(Text.literal("You cannot invite yourself.")); return 0; } if (JustTeamsFabric.teams().isInTeam(target.getUuid())) { s.sendError(Text.literal("That player is already in a team.")); return 0; } t.addInvite(target.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal(target.getName().getString() + " has been invited to join " + t.getName() + "."), false); target.sendMessage(Text.literal("You have been invited to join " + t.getName() + ". Use /team accept " + t.getName() + "."), false); return 1; }
    private static int acceptInvite(ServerCommandSource s, String teamName) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); if (JustTeamsFabric.teams().isInTeam(p.getUuid())) { s.sendError(Text.literal("You are already in a team.")); return 0; } Team t = JustTeamsFabric.teams().getTeams().stream().filter(x -> x.getName().equalsIgnoreCase(teamName)).findFirst().orElse(null); if (t == null || !t.getInvites().contains(p.getUuid())) { s.sendError(Text.literal("You do not have an invite from that team.")); return 0; } t.removeInvite(p.getUuid()); JustTeamsFabric.teams().addMember(t, new TeamPlayer(p.getUuid(), TeamRole.MEMBER, Instant.now(), false, false, false, false)); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); JustTeamsFabric.glow().refreshAll(s.getServer()); p.sendMessage(Text.literal("You have joined the team " + t.getName() + "."), false); for (TeamPlayer member : t.getMembers()) { if (member.getPlayerUuid().equals(p.getUuid())) continue; ServerPlayerEntity memberPlayer = s.getServer().getPlayerManager().getPlayer(member.getPlayerUuid()); if (memberPlayer != null) memberPlayer.sendMessage(Text.literal(p.getName().getString() + " has joined the team."), false); } return 1; }
    private static int denyInvite(ServerCommandSource s, String teamName) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeams().stream().filter(x -> x.getName().equalsIgnoreCase(teamName)).findFirst().orElse(null); if (t == null || !t.getInvites().contains(p.getUuid())) { s.sendError(Text.literal("You do not have an invite from that team.")); return 0; } t.removeInvite(p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Invitation from " + t.getName() + " denied."), false); return 1; }
    private static int requestJoin(ServerCommandSource s, String teamName) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); if (JustTeamsFabric.teams().isInTeam(p.getUuid())) { s.sendError(Text.literal("You are already in a team.")); return 0; } Team t = JustTeamsFabric.teams().getTeams().stream().filter(x -> x.getName().equalsIgnoreCase(teamName)).findFirst().orElse(null); if (t == null) { s.sendError(Text.literal("Team not found.")); return 0; } t.addJoinRequest(p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Your request to join " + t.getName() + " has been sent."), false); return 1; }
    private static int cancelJoinRequest(ServerCommandSource s, String teamName) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeams().stream().filter(x -> x.getName().equalsIgnoreCase(teamName)).findFirst().orElse(null); if (t == null) { s.sendError(Text.literal("Team not found.")); return 0; } t.removeJoinRequest(p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("Your join request to " + t.getName() + " was cancelled."), false); return 1; }
    private static int openRequests(ServerCommandSource s) throws Exception { ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = requireTeam(s, p); if (!t.hasElevatedPermissions(p.getUuid())) { s.sendError(Text.literal("You do not have permission to manage join requests.")); return 0; } TeamGuiManager.openPersistentView(p, TeamInPlaceGui.View.JOIN_REQUESTS); return 1; }
    private static Team requireTeam(ServerCommandSource s, ServerPlayerEntity p) throws Exception { Team t = JustTeamsFabric.teams().getTeam(p.getUuid()); if (t == null) throw new IllegalStateException("You are not in a team."); return t; }
}
