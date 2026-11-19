package eu.kotori.justTeams.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Pure Brigadier tree transformation kept outside Mixin classes so it is not
 * treated as a method to apply to the Minecraft target class.
 */
public final class VanillaTeamCommandRenamer {
    private VanillaTeamCommandRenamer() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> renameRoot(
            LiteralArgumentBuilder<ServerCommandSource> original
    ) {
        if (!"team".equals(original.getLiteral())) {
            return original;
        }

        LiteralArgumentBuilder<ServerCommandSource> renamed =
                LiteralArgumentBuilder.<ServerCommandSource>literal("scoreboardteam")
                        .requires(original.getRequirement());

        if (original.getCommand() != null) {
            renamed.executes(original.getCommand());
        }

        for (CommandNode<ServerCommandSource> child : original.getArguments()) {
            renamed.then(child);
        }

        return renamed;
    }
}
