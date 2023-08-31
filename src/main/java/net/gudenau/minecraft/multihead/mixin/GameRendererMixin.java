package net.gudenau.minecraft.multihead.mixin;

import net.gudenau.minecraft.multihead.WindowDuck;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
        method = "preloadPrograms",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/resource/ResourceFactory;Ljava/lang/String;Lnet/minecraft/client/render/VertexFormat;)Lnet/minecraft/client/gl/ShaderProgram;"
        )
    )
    private void preloadPrograms(ResourceFactory factory, CallbackInfo ci) {
        WindowDuck.extension().doInExtraContexts(window -> window.loadShaders(factory));
    }
}
