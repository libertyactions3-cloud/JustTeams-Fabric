package eu.kotori.justTeams.mixin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.kotori.justTeams.commands.VanillaTeamCommandRenamer;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class VanillaTeamCommandMixinTest {

    @Test
    void renamesVanillaTeamRootAndPreservesItsPermissionAndCommands() {
        Predicate<ServerCommandSource> operatorOnly = source -> false;

        LiteralArgumentBuilder<ServerCommandSource> original =
                LiteralArgumentBuilder.<ServerCommandSource>literal("team")
                        .requires(operatorOnly)
                        .executes(context -> 1)
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list"));

        var renamed = VanillaTeamCommandRenamer.renameRoot(original);

        assertEquals("scoreboardteam", renamed.getLiteral());
        assertSame(operatorOnly, renamed.getRequirement());
        assertSame(original.getCommand(), renamed.getCommand());
        assertEquals(1, renamed.getArguments().size());
        assertEquals("list", renamed.getArguments().iterator().next().getName());
        assertNotSame(original, renamed);
    }

    @Test
    void leavesUnrelatedVanillaCommandRootsUnchanged() {
        LiteralArgumentBuilder<ServerCommandSource> original =
                LiteralArgumentBuilder.<ServerCommandSource>literal("scoreboard");

        assertSame(original, VanillaTeamCommandRenamer.renameRoot(original));
    }
}
