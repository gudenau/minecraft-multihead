package net.gudenau.minecraft.multihead;

import net.gudenau.minecraft.multihead.mixin.RotatingCubeMapRendererAccessor;
import net.gudenau.minecraft.multihead.mixin.TitleScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class DummyTitleScreen extends Screen {
    public static final DummyTitleScreen INSTANCE = new DummyTitleScreen();
    private TitleScreen parent;
    private WindowExtension.Location location;

    private DummyTitleScreen() {
        super(Text.of(""));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var parent = (TitleScreen & TitleScreenAccessor) this.parent;
        var renderer = (RotatingCubeMapRenderer & RotatingCubeMapRendererAccessor) parent.getBackgroundRenderer();
        renderer.getCubeMap().draw(client, 10, -((renderer.getPitch() + (90 * (location == WindowExtension.Location.RIGHT ? 1 : -1))) % 360), 1);
    }

    public void setup(TitleScreen screen) {
        width = screen.width;
        height = screen.height;
        client = MinecraftClient.getInstance();
        parent = screen;
        client.currentScreen = this;
        init(client, width, height);
    }

    public void location(WindowExtension.Location location) {
        this.location = location;
    }
}
