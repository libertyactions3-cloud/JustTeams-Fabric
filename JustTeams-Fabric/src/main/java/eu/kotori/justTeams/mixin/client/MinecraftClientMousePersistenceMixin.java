package eu.kotori.justTeams.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves the physical mouse pointer position when Minecraft swaps one container GUI for another. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMousePersistenceMixin {
    @Unique
    private double justTeams$savedMouseX;

    @Unique
    private double justTeams$savedMouseY;

    @Unique
    private boolean justTeams$restoreMousePosition;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void justTeams$captureMousePosition(Screen nextScreen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        Screen currentScreen = client.currentScreen;
        justTeams$restoreMousePosition = currentScreen instanceof HandledScreen<?> && nextScreen instanceof HandledScreen<?>;
        if (!justTeams$restoreMousePosition) return;

        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(client.getWindow().getHandle(), x, y);
        justTeams$savedMouseX = x[0];
        justTeams$savedMouseY = y[0];
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void justTeams$restoreMousePosition(Screen nextScreen, CallbackInfo ci) {
        if (!justTeams$restoreMousePosition || !(nextScreen instanceof HandledScreen<?>)) return;

        MinecraftClient client = (MinecraftClient) (Object) this;
        GLFW.glfwSetCursorPos(client.getWindow().getHandle(), justTeams$savedMouseX, justTeams$savedMouseY);
        justTeams$restoreMousePosition = false;
    }
}
