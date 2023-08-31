package net.gudenau.minecraft.multihead;

import net.minecraft.client.MinecraftClient;

public interface WindowDuck {
    WindowExtension multihead_extension();

    static WindowExtension extension() {
        return ((WindowDuck)(Object) MinecraftClient.getInstance().getWindow()).multihead_extension();
    }
}
