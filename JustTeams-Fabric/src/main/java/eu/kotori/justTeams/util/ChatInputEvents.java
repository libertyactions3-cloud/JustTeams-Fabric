package eu.kotori.justTeams.util;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/** Bridges Fabric server chat to pending JustTeams GUI text-input sessions. */
public final class ChatInputEvents {
    private ChatInputEvents() {}

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, params) -> {
            String text = message.getContent().getString();
            if (!ChatInputManager.isWaiting(player.getUuid())) return true;
            ChatInputManager.handle((ServerPlayerEntity) player, text);
            return false;
        });
    }
}
