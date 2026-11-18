package eu.kotori.justTeams.mixin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeamCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Moves Minecraft's scoreboard-management /team command to /scoreboardteam.
 *
 * JustTeams owns /team. Brigadier merges literal roots with the same name but
 * keeps the existing root's requirement predicate, which leaves vanilla's
 * operator-only predicate in place and blocks JustTeams before its permission
 * checks can be reached.
 */
@Mixin(TeamCommand.class)
public abstract class VanillaTeamCommandMixin {

    @ModifyArg(
            method = "register(Lcom/mojang/brigadier/CommandDispatcher;Lnet/minecraft/command/CommandRegistryAccess;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;register(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;)Lcom/mojang/brigadier/tree/LiteralCommandNode;"
            ),
            index = 0,
            require = 1
    )
    private static LiteralArgumentBuilder<ServerCommandSource> justTeams$renameVanillaTeamRoot(
            LiteralArgumentBuilder<ServerCommandSource> original
    ) {
        return renameVanillaTeamRoot(original);
    }

    static LiteralArgumentBuilder<ServerCommandSource> renameVanillaTeamRoot(
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
