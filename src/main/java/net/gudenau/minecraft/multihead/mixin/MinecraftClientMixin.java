package net.gudenau.minecraft.multihead.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gudenau.minecraft.multihead.DummyScreen;
import net.gudenau.minecraft.multihead.DummyTitleScreen;
import net.gudenau.minecraft.multihead.WindowDuck;
import net.gudenau.minecraft.multihead.WindowExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL33;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Final private Window window;
    @Shadow @Final private Framebuffer framebuffer;
    @Shadow private Profiler profiler;
    @Shadow public boolean skipGameRender;
    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow private volatile boolean paused;
    @Shadow private float pausedTickDelta;
    @Shadow @Final private RenderTickCounter renderTickCounter;
    @Shadow @Final public GameOptions options;
    @Shadow @Nullable public Screen currentScreen;

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void render(boolean tick, CallbackInfo ci, long timer) {
        var camera = gameRenderer.getCamera();
        var entity = camera.getFocusedEntity();

        var tickDelta = paused ? pausedTickDelta : renderTickCounter.tickDelta;

        float yaw;
        double fov;
        if(entity != null) {
            yaw = entity.getYaw();
            fov = ((GameRendererAccessor) gameRenderer).invokeGetFov(camera, tickDelta, true) * 1.5;
        } else {
            yaw = Float.NaN;
            fov = Double.NaN;
        }

        var extension = ((WindowDuck)(Object) window).multihead_extension();

        var hideHud = options.hudHidden;
        options.hudHidden = true;

        var screen = currentScreen;
        if(screen != null) {
            if(screen instanceof TitleScreen title) {
                DummyTitleScreen.INSTANCE.setup(title);
            } else {
                DummyScreen.INSTANCE.setup(screen);
            }
        }

        var pass = new AtomicInteger(1);

        extension.forExtraContexts((window, location) -> {
            if(entity != null) {
                entity.setYaw((float) (yaw + fov * (location == WindowExtension.Location.RIGHT ? 1 : -1)));
            }

            if(currentScreen instanceof DummyTitleScreen) {
                DummyTitleScreen.INSTANCE.location(location);
            }

            framebuffer.beginWrite(true);
            GL33.glClear(GL33.GL_DEPTH_BUFFER_BIT | GL33.GL_COLOR_BUFFER_BIT);
            BackgroundRenderer.clearFog();
            profiler.push("display");
            RenderSystem.enableCull();
            profiler.pop();
            if (!skipGameRender) {
                profiler.swap("gameRenderer" + pass.getAndIncrement());
                gameRenderer.render(tickDelta, timer, tick);
                profiler.pop();
            }
            framebuffer.endWrite();

            extension.doInContext(window, () -> window.blitFramebuffer(framebuffer, this.window.getFramebufferWidth(), this.window.getFramebufferHeight()));
        });

        currentScreen = screen;
        options.hudHidden = hideHud;
        if(entity != null) {
            entity.setYaw(yaw);
        }
    }
}
