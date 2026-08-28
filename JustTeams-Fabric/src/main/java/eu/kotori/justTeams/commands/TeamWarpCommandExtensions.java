package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.economy.FeatureCostManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamNotificationManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Adds command-tree extensions whose underlying command already exists in the core /team tree. */
public final class TeamWarpCommandExtensions {
    private static final int TEAM_BLUE = 0x4C9DDE;
    private TeamWarpCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        registerWarpPassword(dispatcher);
        registerMemberManagement(dispatcher);
        TeamMetadataCommandExtensions.register(dispatcher);
    }

    private static void registerWarpPassword(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team=dispatcher.getRoot().getChild("team"); if(team==null)return;
        CommandNode<ServerCommandSource> warp=team.getChild("warp"); if(warp==null)return;
        CommandNode<ServerCommandSource> set=warp.getChild("set"); if(set==null)return;
        CommandNode<ServerCommandSource> name=set.getChild("name"); if(name==null)return;
        name.addChild(CommandManager.argument("password",StringArgumentType.greedyString()).executes(context->setWarpWithPassword(context.getSource(),StringArgumentType.getString(context,"name"),StringArgumentType.getString(context,"password"))).build());
    }

    private static void registerMemberManagement(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team=dispatcher.getRoot().getChild("team"); if(team==null)return;
        team.addChild(CommandManager.literal("kick").executes(context->usage(context.getSource(),"/team kick <player>"))
                .then(CommandManager.argument("player",StringArgumentType.word()).suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context->kick(context.getSource(),StringArgumentType.getString(context,"player")))).build());
        team.addChild(CommandManager.literal("promote").executes(context->usage(context.getSource(),"/team promote <player>"))
                .then(CommandManager.argument("player",StringArgumentType.word()).suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context->promote(context.getSource(),StringArgumentType.getString(context,"player")))).build());
        team.addChild(CommandManager.literal("demote").executes(context->usage(context.getSource(),"/team demote <player>"))
                .then(CommandManager.argument("player",StringArgumentType.word()).suggests(TeamCommandSuggestions.ONLINE_PLAYERS)
                        .executes(context->demote(context.getSource(),StringArgumentType.getString(context,"player")))).build());
    }

    private static int setWarpWithPassword(ServerCommandSource source,String name,String password){
        try{ServerPlayerEntity player=source.getPlayerOrThrow();Team team=requireTeam(player);if(!JustTeamsFabric.permissions().has(player,JustTeamsPermissions.COMMAND_SETWARP))throw new IllegalStateException("You do not have permission to use this command.");TeamPlayer member=team.getMember(player.getUuid());if(member==null||!member.canSetWarps())throw new IllegalStateException("You do not have permission to create team warps.");if(name.length()>32)throw new IllegalArgumentException("Warp name must be 32 characters or fewer.");if(team.getWarp(name)!=null)throw new IllegalArgumentException("A warp with that name already exists.");if(password.length()>64)throw new IllegalArgumentException("Warp passwords may not exceed 64 characters.");if(!FeatureCostManager.charge(player,"setwarp"))return 0;TeamLocation location=TeamLocation.fromPlayer(player);TeamWarp warp=new TeamWarp(name,player.getUuid(),location.getDimension(),location.getX(),location.getY(),location.getZ(),location.getYaw(),location.getPitch());warp.setPassword(password);team.addWarp(warp);JustTeamsFabric.storage().save(JustTeamsFabric.teams());source.sendFeedback(()->Text.literal("Team warp '"+name+"' created."),false);return 1;}
        catch(Exception exception){source.sendError("You are not in a team.".equals(exception.getMessage())?noTeam():Text.literal(exception.getMessage()==null?"Unable to create the team warp.":exception.getMessage()));JustTeamsFabric.LOGGER.error("Failed to create team warp {}",name,exception);return 0;}}

    private static int kick(ServerCommandSource source,String targetName){
        try{ServerPlayerEntity actor=source.getPlayerOrThrow();Team team=requireTeam(actor);TeamPlayer actorMember=team.getMember(actor.getUuid());if(!JustTeamsFabric.permissions().has(actor,JustTeamsPermissions.COMMAND_KICK))throw new IllegalStateException("You do not have permission to use this command.");PlayerConfigEntry targetEntry=resolveTarget(source,targetName);if(targetEntry==null)throw new IllegalStateException("Player not found.");TeamPlayer target=team.getMember(targetEntry.id());if(target==null)throw new IllegalStateException("That player is not in your team.");if(actorMember==null||!actorMember.canKickPlayer(target))throw new IllegalStateException("You do not have permission to kick that player.");JustTeamsFabric.teams().removeMember(team,target.getPlayerUuid());JustTeamsFabric.storage().save(JustTeamsFabric.teams());TeamNotificationManager.notifyKick(actor.getEntityWorld().getServer(),team,actor.getUuid(),target.getPlayerUuid());return 1;}
        catch(Exception exception){source.sendError("You are not in a team.".equals(exception.getMessage())?noTeam():Text.literal(exception.getMessage()==null?"Unable to kick that player.":exception.getMessage()));JustTeamsFabric.LOGGER.error("Failed to kick team member {}",targetName,exception);return 0;}}

    private static int promote(ServerCommandSource source,String targetName){
        try{ServerPlayerEntity actor=source.getPlayerOrThrow();Team team=requireTeam(actor);if(!JustTeamsFabric.permissions().has(actor,JustTeamsPermissions.COMMAND_PROMOTE))throw new IllegalStateException("You do not have permission to use this command.");if(!team.isOwner(actor.getUuid()))throw new IllegalStateException("Only the team owner can promote players.");PlayerConfigEntry targetEntry=resolveTarget(source,targetName);if(targetEntry==null)throw new IllegalStateException("Player not found.");TeamPlayer target=team.getMember(targetEntry.id());if(target==null)throw new IllegalStateException("That player is not in your team.");if(target.getRank()==TeamRank.LEADER)throw new IllegalStateException("The team owner is already the highest rank.");TeamRank next=target.getRank().promote();if(next==target.getRank())throw new IllegalStateException("That player is already at the highest promotable rank.");target.setRank(next);JustTeamsFabric.storage().save(JustTeamsFabric.teams());JustTeamsFabric.glow().refreshAll(source.getServer());notifyRankChange(actor,team,target,true);return 1;}
        catch(Exception exception){source.sendError("You are not in a team.".equals(exception.getMessage())?noTeam():Text.literal(exception.getMessage()==null?"Unable to promote that player.":exception.getMessage()));JustTeamsFabric.LOGGER.error("Failed to promote team member {}",targetName,exception);return 0;}}

    private static int demote(ServerCommandSource source,String targetName){
        try{ServerPlayerEntity actor=source.getPlayerOrThrow();Team team=requireTeam(actor);if(!JustTeamsFabric.permissions().has(actor,JustTeamsPermissions.COMMAND_DEMOTE))throw new IllegalStateException("You do not have permission to use this command.");if(!team.isOwner(actor.getUuid()))throw new IllegalStateException("Only the team owner can demote players.");PlayerConfigEntry targetEntry=resolveTarget(source,targetName);if(targetEntry==null)throw new IllegalStateException("Player not found.");TeamPlayer target=team.getMember(targetEntry.id());if(target==null)throw new IllegalStateException("That player is not in your team.");if(target.getRank()==TeamRank.LEADER)throw new IllegalStateException("You cannot demote the team owner.");TeamRank next=target.getRank().demote();if(next==target.getRank())throw new IllegalStateException("That player is already at the lowest rank.");target.setRank(next);JustTeamsFabric.storage().save(JustTeamsFabric.teams());JustTeamsFabric.glow().refreshAll(source.getServer());notifyRankChange(actor,team,target,false);return 1;}
        catch(Exception exception){source.sendError("You are not in a team.".equals(exception.getMessage())?noTeam():Text.literal(exception.getMessage()==null?"Unable to demote that player.":exception.getMessage()));JustTeamsFabric.LOGGER.error("Failed to demote team member {}",targetName,exception);return 0;}}

    private static Team requireTeam(ServerPlayerEntity player){Team team=JustTeamsFabric.teams().getTeam(player.getUuid());if(team==null)throw new IllegalStateException("You are not in a team.");return team;}
    private static PlayerConfigEntry resolveTarget(ServerCommandSource source,String targetName){ServerPlayerEntity online=source.getServer().getPlayerManager().getPlayer(targetName);if(online!=null)return new PlayerConfigEntry(online.getUuid(),online.getName().getString());return source.getServer().getApiServices().nameToIdCache().findByName(targetName).orElse(null);}
    private static void notifyRankChange(ServerPlayerEntity actor,Team team,TeamPlayer target,boolean promoted){var server=actor.getEntityWorld().getServer();ServerPlayerEntity targetPlayer=server.getPlayerManager().getPlayer(target.getPlayerUuid());String targetName=targetPlayer!=null?targetPlayer.getName().getString():target.getPlayerUuid().toString();actor.sendMessage(Text.literal("You "+(promoted?"promoted ":"demoted ")+targetName+" to "+target.getRank().getDisplayName()+"."),false);if(targetPlayer!=null)targetPlayer.sendMessage(Text.literal("You were "+(promoted?"promoted to ":"demoted to ")+target.getRank().getDisplayName()+" in "+team.getName()+"."),false);for(TeamPlayer member:team.getMembers()){ServerPlayerEntity memberPlayer=server.getPlayerManager().getPlayer(member.getPlayerUuid());if(memberPlayer!=null&&!memberPlayer.getUuid().equals(actor.getUuid())&&!memberPlayer.getUuid().equals(target.getPlayerUuid()))memberPlayer.sendMessage(Text.literal(targetName+" is now "+target.getRank().getDisplayName()+"."),false);}}
    private static int usage(ServerCommandSource source,String text){source.sendMessage(prefix().append(Text.literal("Usage: "+text).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))));return 0;}
    private static Text noTeam(){return prefix().append(Text.literal("You are not in a team.").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false)));}
    private static MutableText prefix(){return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false));}
}
