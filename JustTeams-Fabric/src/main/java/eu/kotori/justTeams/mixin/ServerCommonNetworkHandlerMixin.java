package eu.kotori.justTeams.mixin;

import eu.kotori.justTeams.chat.TeamChatCustomClickActions;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles JustTeams custom chat click actions entirely on the server. */
@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin {
    @Inject(method = "onCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void justTeams$handleCustomClickAction(CustomClickActionC2SPacket packet, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayNetworkHandler playHandler)) return;
        if (TeamChatCustomClickActions.handle(playHandler.player, packet)) ci.cancel();
    }
}
