package eu.kotori.justTeams.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

/** Player-name suggestions without selector syntax such as @a. */
public final class TeamCommandSuggestions {
    private TeamCommandSuggestions() {}

    public static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS = (context, builder) ->
            CommandSource.suggestMatching(
                    context.getSource().getServer().getPlayerManager().getPlayerList().stream()
                            .map(player -> player.getGameProfile().getName()),
                    builder
            );
}
