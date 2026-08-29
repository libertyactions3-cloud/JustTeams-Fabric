package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Implements the local 2.5.3-style one-shot /teammsg command. */
public final class TeamMessageCommandExtension {
    private static final long MESSAGE_COOLDOWN_MILLIS=2_000L;
    private static final int MAX_MESSAGES_PER_MINUTE=20;
    private static final int MAX_MESSAGE_LENGTH=200;
    private static final long MESSAGE_WINDOW_MILLIS=60_000L;
    private static final Map<UUID,Long> LAST_MESSAGE=new ConcurrentHashMap<>();
    private static final Map<UUID,MessageWindow> MESSAGE_WINDOWS=new ConcurrentHashMap<>();
    private static final String[] BLOCKED_TERMS={"admin","mod","staff","owner","server","minecraft","bukkit","spigot","hack","cheat","exploit","bug","glitch","dupe","duplicate"};
    private static final int TEAM_BLUE=0x4C9DDE;
    private TeamMessageCommandExtension() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("teammsg")
                .executes(context->usage(context.getSource()))
                .then(CommandManager.argument("message",StringArgumentType.greedyString())
                        .executes(context->execute(context.getSource(),StringArgumentType.getString(context,"message")))));
    }

    private static int execute(ServerCommandSource source,String message){
        try{
            ServerPlayerEntity player=source.getPlayerOrThrow();
            if(!JustTeamsFabric.permissions().has(player,JustTeamsPermissions.USER)){source.sendError(Text.literal("You do not have permission to use team messages."));return 0;}
            if(message.isEmpty())return usage(source);
            Team team=JustTeamsFabric.teams().getTeam(player.getUuid());if(team==null){source.sendError(noTeam());return 0;}
            if(!checkSpam(player.getUuid())){source.sendError(Text.literal("You are sending team messages too quickly."));return 0;}
            if(message.length()>MAX_MESSAGE_LENGTH){source.sendError(Text.literal("Team messages may not exceed 200 characters."));return 0;}
            if(containsBlockedContent(message)){source.sendError(Text.literal("That message contains prohibited content."));return 0;}
            Text formatted=Text.literal("[Team] "+player.getName().getString()+": "+message);
            for(TeamPlayer member:team.getMembers()){ServerPlayerEntity recipient=source.getServer().getPlayerManager().getPlayer(member.getPlayerUuid());if(recipient!=null)recipient.sendMessage(formatted,false);}
            return 1;
        }catch(Exception exception){String msg=exception.getMessage();source.sendError("You are not in a team.".equals(msg)?noTeam():Text.literal(msg==null?"Unable to send the team message.":msg));JustTeamsFabric.LOGGER.error("Failed to send team message",exception);return 0;}
    }
    private static int usage(ServerCommandSource source){source.sendMessage(prefix().append(Text.literal("Usage: /teammsg <message>").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))));return 0;}
    private static Text noTeam(){return prefix().append(Text.literal("You are not in a team.").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false)));}
    private static MutableText prefix(){return Text.literal("[ᴛᴇᴀᴍꜱ] ").setStyle(Style.EMPTY.withColor(TEAM_BLUE).withItalic(false));}
    private static boolean checkSpam(UUID playerUuid){long now=System.currentTimeMillis();Long last=LAST_MESSAGE.get(playerUuid);if(last!=null&&now-last<MESSAGE_COOLDOWN_MILLIS)return false;MessageWindow window=MESSAGE_WINDOWS.get(playerUuid);if(window==null||now-window.windowStart>=MESSAGE_WINDOW_MILLIS)window=new MessageWindow(now,0);if(window.count>=MAX_MESSAGES_PER_MINUTE)return false;LAST_MESSAGE.put(playerUuid,now);MESSAGE_WINDOWS.put(playerUuid,new MessageWindow(window.windowStart,window.count+1));return true;}
    private static boolean containsBlockedContent(String message){String lower=message.toLowerCase(java.util.Locale.ROOT);for(String blockedTerm:BLOCKED_TERMS)if(lower.contains(blockedTerm))return true;return false;}
    private record MessageWindow(long windowStart,int count){}
}