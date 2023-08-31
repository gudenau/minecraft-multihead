package net.gudenau.minecraft.multihead.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gudenau.minecraft.multihead.WindowDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(
        method = "flipFrame",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"
        )
    )
    private static void flipFrame(long window, CallbackInfo ci) {
        WindowDuck.extension().flipFrame();
    }
}
