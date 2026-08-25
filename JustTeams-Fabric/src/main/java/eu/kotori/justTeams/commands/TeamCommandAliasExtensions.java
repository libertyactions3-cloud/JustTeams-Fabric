package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;

/** Adds the command aliases declared by the 2.5.3 reference plugin.yml. */
public final class TeamCommandAliasExtensions {
    private TeamCommandAliasExtensions() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> team = dispatcher.getRoot().getChild("team");
        if (team == null) return;

        registerAlias(dispatcher, "t", team);
        registerAlias(dispatcher, "guild", team);
        registerAlias(dispatcher, "g", team);
        registerAlias(dispatcher, "clan", team);
        registerAlias(dispatcher, "c", team);
        registerAlias(dispatcher, "party", team);
        registerAlias(dispatcher, "p", team);
    }

    private static void registerAlias(CommandDispatcher<ServerCommandSource> dispatcher,
                                      String alias, CommandNode<ServerCommandSource> target) {
        dispatcher.register(CommandManager.literal(alias).redirect(target));
    }
}
