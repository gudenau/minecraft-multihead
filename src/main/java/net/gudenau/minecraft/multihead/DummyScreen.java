package net.gudenau.minecraft.multihead;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class DummyScreen extends Screen {
    public static final DummyScreen INSTANCE = new DummyScreen();

    private DummyScreen() {
        super(Text.of(""));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MinecraftClient.getInstance().world != null) {
            context.fillGradient(0, 0, width, height, 0xC0101010, 0xC0101010);
        } else {
            renderBackgroundTexture(context);
        }
    }

    public void setup(Screen screen) {
        width = screen.width;
        height = screen.height;
        client = MinecraftClient.getInstance();
        client.currentScreen = this;
        init(client, width, height);
    }
}
