package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.config.JustTeamsConfig;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Replaces the minimal /team create executable with verified 2.5.3 validation/default behavior. */
public final class TeamCreationCommandExtensions {
    private static final String[] BLOCKED_WORDS = {
            "admin", "mod", "staff", "owner", "server", "minecraft",
            "bukkit", "spigot", "console", "system", "root"
    };

    private TeamCreationCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        LiteralArgumentBuilder<ServerCommandSource> replacement = CommandManager.literal("create")
                .requires(source -> source.getEntity() instanceof ServerPlayerEntity player
                        && JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_CREATE))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .then(CommandManager.argument("tag", StringArgumentType.word())
                                .executes(context -> execute(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "tag")))));

        team.addChild(replacement.build());
    }

    private static int execute(ServerCommandSource source, String name, String tag) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            JustTeamsConfig config = JustTeamsFabric.config();

            if (!isValid(name, config.getMinTeamNameLength(), config.getMaxTeamNameLength(), true)
                    || !isValid(tag, 2, config.getMaxTeamTagLength(), true)) {
                source.sendError(Text.literal(
                        "Invalid team name or tag. Name: 3-16 characters; tag: 2-" + config.getMaxTeamTagLength()
                                + ". Use only letters, numbers, and underscores."));
                return 0;
            }

            Team team = JustTeamsFabric.teams().createTeam(
                    name,
                    tag,
                    player.getUuid(),
                    config.getDefaultTeamPvp(),
                    config.getDefaultTeamPublic(),
                    false);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("You have successfully created the team " + team.getName() + "."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to create team." : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("Team creation command failed", exception);
            return 0;
        }
    }

    private static boolean isValid(String value, int minimumLength, int maximumLength, boolean rejectBlockedWords) {
        if (value == null) return false;
        String plain = value.trim();
        if (plain.length() < minimumLength || plain.length() > maximumLength) return false;
        if (!plain.matches("^[a-zA-Z0-9_]+$")) return false;
        if (plain.matches("^[0-9_]+$")) return false;

        if (rejectBlockedWords) {
            String lower = plain.toLowerCase(Locale.ROOT);
            for (String blocked : BLOCKED_WORDS) {
                if (lower.contains(blocked)) return false;
            }
        }
        return true;
    }
}
