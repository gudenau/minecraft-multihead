package net.gudenau.minecraft.multihead.mixin;

import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
    @Accessor RotatingCubeMapRenderer getBackgroundRenderer();
    @Accessor boolean getDoBackgroundFade();
    @Accessor long getBackgroundFadeStart();
}
