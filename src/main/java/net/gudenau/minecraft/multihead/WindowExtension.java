package net.gudenau.minecraft.multihead;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class WindowExtension {
    private final long primaryHandle;
    private final GLCapabilities primaryCapabilities;

    private final Monitor leftMonitor;
    private final Monitor centerMonitor;
    private final Monitor rightMonitor;

    private final SecondaryWindow leftWindow;
    private final Window centerWindow;
    private final SecondaryWindow rightWindow;

    private Location currentContext = Location.CENTER;

    public WindowExtension(Window window, MonitorTracker monitorTracker, String title) {
        this.centerWindow = window;
        primaryHandle = window.getHandle();
        primaryCapabilities = GL.getCapabilities();

        var monitorsPointer = glfwGetMonitors();
        if(monitorsPointer == null || monitorsPointer.capacity() != 3) {
            throw new RuntimeException("You must have three monitors!");
        }
        var monitors = new ArrayList<Monitor>(monitorsPointer.capacity());
        while(monitorsPointer.hasRemaining()) {
            monitors.add(monitorTracker.getMonitor(monitorsPointer.get()));
        }
        monitors.sort(Comparator.comparingInt(Monitor::getViewportX));

        leftMonitor = monitors.get(0);
        centerMonitor = monitors.get(1);
        rightMonitor = monitors.get(2);

        leftWindow = new SecondaryWindow(window, "Left " + title);
        rightWindow = new SecondaryWindow(window, "Right " + title);
        setContext(Location.CENTER);
    }

    public void updateWindowRegions() {
        if(centerWindow.isFullscreen()) {
            leftWindow.fullscreen(leftMonitor);
            rightWindow.fullscreen(rightMonitor);
            glfwIconifyWindow(primaryHandle);
            glfwFocusWindow(primaryHandle);
        } else {
            leftWindow.fullscreen(null);
            rightWindow.fullscreen(null);
        }
    }

    public void flipFrame() {
        leftWindow.flipFrame();
        rightWindow.flipFrame();
    }

    public Monitor getMiddleMonitor() {
        return centerMonitor;
    }

    public void setContext(Location location) {
        currentContext = location;
        switch (location) {
            case LEFT -> leftWindow.makeCurrent();
            case CENTER -> {
                glfwMakeContextCurrent(primaryHandle);
                GL.setCapabilities(primaryCapabilities);
            }
            case RIGHT -> rightWindow.makeCurrent();
        }
    }

    public void doInExtraContexts(Consumer<SecondaryWindow> action) {
        setContext(Location.LEFT);
        action.accept(leftWindow);
        setContext(Location.RIGHT);
        action.accept(rightWindow);
        setContext(Location.CENTER);
    }

    public void forExtraContexts(BiConsumer<SecondaryWindow, Location> action) {
        action.accept(leftWindow, Location.LEFT);
        action.accept(rightWindow, Location.RIGHT);
    }

    public void doInContext(SecondaryWindow window, Runnable action) {
        window.makeCurrent();
        action.run();
        setContext(Location.CENTER);
    }

    public Location currentContext() {
        return currentContext;
    }

    public enum Location {
        LEFT, CENTER, RIGHT
    }
}
