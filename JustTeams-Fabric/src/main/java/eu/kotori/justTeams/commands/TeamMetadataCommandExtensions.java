package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Adds metadata/settings command paths backed by TeamManager. */
public final class TeamMetadataCommandExtensions {
    private TeamMetadataCommandExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        team.addChild(CommandManager.literal("settag")
                .then(CommandManager.argument("tag", StringArgumentType.word())
                        .executes(context -> setTag(context.getSource(), StringArgumentType.getString(context, "tag"))))
                .build());

        team.addChild(CommandManager.literal("setdescription")
                .then(CommandManager.argument("description", StringArgumentType.greedyString())
                        .executes(context -> setDescription(context.getSource(), StringArgumentType.getString(context, "description"))))
                .build());

        team.addChild(CommandManager.literal("public")
                .executes(context -> togglePublic(context.getSource()))
                .build());
    }

    private static int setTag(ServerCommandSource source, String tag) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_SETTAG);
            String value = JustTeamsFabric.teams().setTag(player.getUuid(), tag);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            JustTeamsFabric.glow().refreshAll(source.getServer());
            source.sendFeedback(() -> Text.literal("Team tag updated to " + value + "."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to update the team tag." : exception.getMessage()));
            return 0;
        }
    }

    private static int setDescription(ServerCommandSource source, String description) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_SETDESCRIPTION);
            JustTeamsFabric.teams().setDescription(player.getUuid(), description);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team description updated."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to update the team description." : exception.getMessage()));
            return 0;
        }
    }

    private static int togglePublic(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            requirePermission(player, JustTeamsPermissions.COMMAND_PUBLIC);
            boolean enabled = JustTeamsFabric.teams().togglePublic(player.getUuid());
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            source.sendFeedback(() -> Text.literal("Team is now " + (enabled ? "public" : "private") + "."), false);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Unable to update team visibility." : exception.getMessage()));
            return 0;
        }
    }

    private static void requirePermission(ServerPlayerEntity player, String permission) {
        if (!JustTeamsFabric.permissions().has(player, permission)) {
            throw new IllegalStateException("You do not have permission to use this command.");
        }
    }
}
