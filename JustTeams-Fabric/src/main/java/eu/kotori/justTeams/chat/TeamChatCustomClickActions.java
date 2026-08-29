package eu.kotori.justTeams.chat;

import eu.kotori.justTeams.commands.TeamCommandParityExtension;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Identifier;

import java.util.Optional;

/** Server-side custom chat click actions that avoid vanilla run_command confirmation dialogs. */
public final class TeamChatCustomClickActions {
    public static final Identifier ACCEPT_INVITE = Identifier.of("justteams", "accept_invite");
    public static final Identifier DENY_INVITE = Identifier.of("justteams", "deny_invite");

    private TeamChatCustomClickActions() {}

    public static ClickEvent acceptInvite(String teamName) {
        return new ClickEvent.Custom(ACCEPT_INVITE, Optional.of(payload(teamName)));
    }

    public static ClickEvent denyInvite(String teamName) {
        return new ClickEvent.Custom(DENY_INVITE, Optional.of(payload(teamName)));
    }

    public static boolean handle(ServerPlayerEntity player, CustomClickActionC2SPacket packet) {
        Identifier id = packet.id();
        if (!ACCEPT_INVITE.equals(id) && !DENY_INVITE.equals(id)) return false;
        String teamName = packet.payload().flatMap(element -> element.asCompound())
                .flatMap(compound -> compound.getString("team"))
                .orElse(null);
        if (teamName == null || teamName.isBlank() || teamName.length() > 16 || !teamName.matches("[A-Za-z0-9_]+")) {
            return true;
        }
        if (ACCEPT_INVITE.equals(id)) {
            TeamCommandParityExtension.executeAcceptFromCustomClick(player, teamName);
        } else {
            TeamCommandParityExtension.executeDenyFromCustomClick(player, teamName);
        }
        return true;
    }

    private static NbtCompound payload(String teamName) {
        NbtCompound payload = new NbtCompound();
        payload.putString("team", teamName);
        return payload;
    }
}
