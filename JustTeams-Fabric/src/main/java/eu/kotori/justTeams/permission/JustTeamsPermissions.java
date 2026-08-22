package eu.kotori.justTeams.permission;

import java.util.Set;

/** Canonical permission nodes mirrored from the Paper JustTeams plugin.yml. */
public final class JustTeamsPermissions {
    private JustTeamsPermissions() {
    }

    public static final String ALL = "justteams.*";
    public static final String ADMIN = "justteams.admin";
    public static final String USER = "justteams.user";
    public static final String CHAT_SPY = "justteams.chatspy";

    public static final String BYPASS_BANK_WITHDRAW = "justteams.bypass.bank.withdraw";
    public static final String BYPASS_ENDERCHEST_USE = "justteams.bypass.enderchest.use";
    public static final String BYPASS_HOME_COOLDOWN = "justteams.bypass.home.cooldown";
    public static final String BYPASS_WARP_COOLDOWN = "justteams.bypass.warp.cooldown";

    public static final String COMMAND_ADMIN = "justteams.command.admin";
    public static final String COMMAND_RELOAD = "justteams.command.reload";
    public static final String COMMAND_CREATE = "justteams.command.create";
    public static final String COMMAND_DISBAND = "justteams.command.disband";
    public static final String COMMAND_INVITE = "justteams.command.invite";
    public static final String COMMAND_ACCEPT = "justteams.command.accept";
    public static final String COMMAND_DENY = "justteams.command.deny";
    public static final String COMMAND_LEAVE = "justteams.command.leave";
    public static final String COMMAND_KICK = "justteams.command.kick";
    public static final String COMMAND_INFO = "justteams.command.info";
    public static final String COMMAND_CHAT = "justteams.command.chat";
    public static final String COMMAND_SETHOME = "justteams.command.sethome";
    public static final String COMMAND_HOME = "justteams.command.home";
    public static final String COMMAND_SETTAG = "justteams.command.settag";
    public static final String COMMAND_SETDESCRIPTION = "justteams.command.setdescription";
    public static final String COMMAND_TRANSFER = "justteams.command.transfer";
    public static final String COMMAND_PROMOTE = "justteams.command.promote";
    public static final String COMMAND_DEMOTE = "justteams.command.demote";
    public static final String COMMAND_PVP = "justteams.command.pvp";
    public static final String COMMAND_BANK = "justteams.command.bank";
    public static final String COMMAND_ENDERCHEST = "justteams.command.enderchest";
    public static final String COMMAND_TOP = "justteams.command.top";
    public static final String COMMAND_TEAMMSG = "justteams.command.teammsg";
    public static final String COMMAND_JOIN = "justteams.command.join";
    public static final String COMMAND_UNJOIN = "justteams.command.unjoin";
    public static final String COMMAND_PUBLIC = "justteams.command.public";
    public static final String COMMAND_REQUESTS = "justteams.command.requests";
    public static final String COMMAND_SETWARP = "justteams.command.setwarp";
    public static final String COMMAND_DELWARP = "justteams.command.delwarp";
    public static final String COMMAND_WARP = "justteams.command.warp";
    public static final String COMMAND_WARPS = "justteams.command.warps";
    public static final String ADMIN_PERFORMANCE = "justteams.admin.performance";

    public static final Set<String> USER_COMMANDS = Set.of(
            COMMAND_CREATE, COMMAND_DISBAND, COMMAND_INVITE, COMMAND_ACCEPT, COMMAND_DENY,
            COMMAND_LEAVE, COMMAND_KICK, COMMAND_INFO, COMMAND_CHAT, COMMAND_SETHOME,
            COMMAND_HOME, COMMAND_SETTAG, COMMAND_SETDESCRIPTION, COMMAND_TRANSFER,
            COMMAND_PROMOTE, COMMAND_DEMOTE, COMMAND_PVP, COMMAND_BANK, COMMAND_ENDERCHEST,
            COMMAND_TOP, COMMAND_TEAMMSG, COMMAND_JOIN, COMMAND_UNJOIN, COMMAND_PUBLIC,
            COMMAND_REQUESTS, COMMAND_SETWARP, COMMAND_DELWARP, COMMAND_WARP, COMMAND_WARPS
    );
}
