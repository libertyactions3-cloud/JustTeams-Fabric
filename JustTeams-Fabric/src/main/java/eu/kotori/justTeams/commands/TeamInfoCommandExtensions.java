package eu.kotori.justTeams.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Replaces the minimal /team info executable with the verified 2.5.3-style information output. */
public final class TeamInfoCommandExtensions {
    private TeamInfoCommandExtensions() {}

    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        LiteralArgumentBuilder<ServerCommandSource> replacement = CommandManager.literal("info")
                .requires(source -> source.getEntity() instanceof ServerPlayerEntity player
                        && JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_INFO))
                .executes(context -> executeSafely(context.getSource()));

        team.addChild(replacement.build());
    }

    private static int executeSafely(ServerCommandSource source) {
        try {
            return execute(source);
        } catch (Exception exception) {
            String message = exception.getMessage();
            source.sendError(Text.literal(message == null ? "Command failed." : message));
            JustTeamsFabric.LOGGER.error("JustTeams /team info failed", exception);
            return 0;
        }
    }

    private static int execute(ServerCommandSource source) throws Exception {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) {
            source.sendError(Text.literal("You are not in a team."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("TEAM INFO: " + team.getName()), false);
        source.sendFeedback(() -> Text.literal("Tag: " + team.getTag()), false);
        source.sendFeedback(() -> Text.literal("Description: " + team.getDescription()), false);
        source.sendFeedback(() -> Text.literal("Owner: " + resolveName(source.getServer(), team.getOwnerUuid())), false);

        String coOwners = team.getCoOwners().stream()
                .map(member -> resolveName(source.getServer(), member.getPlayerUuid()))
                .collect(Collectors.joining(", "));
        if (!coOwners.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Co-Owners: " + coOwners), false);
        }

        double kdr = team.getDeaths() == 0
                ? team.getKills()
                : (double) team.getKills() / team.getDeaths();
        source.sendFeedback(() -> Text.literal(String.format(Locale.ROOT,
                "Kills: %d | Deaths: %d | KDR: %.2f",
                team.getKills(), team.getDeaths(), kdr)), false);
        source.sendFeedback(() -> Text.literal("Members (" + team.getMembers().size() + "):"), false);

        for (TeamPlayer member : team.getMembers()) {
            String name = resolveName(source.getServer(), member.getPlayerUuid());
            source.sendFeedback(() -> Text.literal("- " + name), false);
        }

        source.sendFeedback(() -> Text.literal("End of team info"), false);
        return 1;
    }

    private static String resolveName(MinecraftServer server, java.util.UUID uuid) {
        if (server == null || uuid == null) return "Unknown";
        Optional<PlayerConfigEntry> profile = server.getApiServices().nameToIdCache().getByUuid(uuid);
        return profile.map(PlayerConfigEntry::name).filter(Objects::nonNull).orElse("Unknown");
    }
}
