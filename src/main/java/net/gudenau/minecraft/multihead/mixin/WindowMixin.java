package net.gudenau.minecraft.multihead.mixin;

import net.gudenau.minecraft.multihead.WindowDuck;
import net.gudenau.minecraft.multihead.WindowExtension;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowDuck {
    @Unique private WindowExtension multihead_extension;

    @Override
    public WindowExtension multihead_extension() {
        return multihead_extension;
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        multihead_extension = new WindowExtension((Window)(Object) this, monitorTracker, title);
        multihead_extension.updateWindowRegions();
    }

    @ModifyVariable(
        method = "updateWindowRegion",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/util/MonitorTracker;getMonitor(Lnet/minecraft/client/util/Window;)Lnet/minecraft/client/util/Monitor;"
        )
    )
    private Monitor replacePrimaryMonitor(Monitor original) {
        return multihead_extension.getMiddleMonitor();
    }

    @Inject(
        method = "updateWindowRegion",
        at = @At("TAIL")
    )
    private void updateWindowRegion(CallbackInfo ci) {
        if(multihead_extension != null) {
            multihead_extension.updateWindowRegions();
        }
    }
}
