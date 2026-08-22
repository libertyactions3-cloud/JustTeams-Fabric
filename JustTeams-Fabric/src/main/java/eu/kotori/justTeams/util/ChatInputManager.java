package eu.kotori.justTeams.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Small server-side chat input session manager used by GUI actions that need
 * text from a player. A session expires when explicitly completed/cancelled
 * or when replaced by another session for the same player.
 */
public final class ChatInputManager {
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private ChatInputManager() {}

    public static void begin(ServerPlayerEntity player, String prompt, Consumer<String> consumer) {
        SESSIONS.put(player.getUuid(), new Session(consumer));
        player.sendMessage(Text.literal(prompt), false);
        player.sendMessage(Text.literal("Type your response in chat, or type cancel."), false);
    }

    public static boolean isWaiting(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static boolean handle(ServerPlayerEntity player, String message) {
        Session session = SESSIONS.remove(player.getUuid());
        if (session == null) return false;
        if (!message.equalsIgnoreCase("cancel")) session.consumer.accept(message);
        else player.sendMessage(Text.literal("Input cancelled."), false);
        return true;
    }

    public static void cancel(UUID playerUuid) {
        SESSIONS.remove(playerUuid);
    }

    private record Session(Consumer<String> consumer) {}
}
