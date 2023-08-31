package net.gudenau.minecraft.multihead;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gudenau.minecraft.multihead.mixin.VertexBufferAccessor;
import net.gudenau.minecraft.multihead.mixin.VertexFormatAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.util.Map;

import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class SecondaryWindow {
    private final Window parent;
    private final long handle;
    private final GLCapabilities capabilities;

    private ShaderProgram blitShader;
    private final Tessellator tessellator;

    public SecondaryWindow(Window window, String title) {
        parent = window;

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        handle = glfwCreateWindow(window.getWidth(), window.getHeight(), title, NULL, window.getHandle());
        if (handle == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        glfwMakeContextCurrent(handle);
        capabilities = GL.createCapabilities();

        tessellator = new Tessellator();
    }

    public void fullscreen(Monitor monitor) {
        if (monitor == null) {
            glfwHideWindow(handle);
        } else {
            glfwShowWindow(handle);
            var videoMode = monitor.findClosestVideoMode(parent.getVideoMode());
            glfwSetWindowMonitor(handle, monitor.getHandle(), 0, 0, videoMode.getWidth(), videoMode.getHeight(), videoMode.getRefreshRate());
        }
    }

    public void flipFrame() {
        glfwSwapBuffers(handle);
    }

    public void makeCurrent() {
        glfwMakeContextCurrent(handle);
        GL.setCapabilities(capabilities);
    }

    public void loadShaders(ResourceFactory factory) {
        try {
            blitShader = new ShaderProgram(factory, "blit_screen", VertexFormats.BLIT_SCREEN);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load blit shader", e);
        }
    }

    public void blitFramebuffer(Framebuffer framebuffer, int width, int height) {
        var color = (System.currentTimeMillis() % 5000) / 5000F;
        glClearColor(color, color, color, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glColorMask(true, true, true, false);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glViewport(0, 0, width, height);
        glDisable(GL_BLEND);

        var shader = blitShader;
        shader.addSampler("DiffuseSampler", framebuffer.getColorAttachment());
        Matrix4f projectionMatrix = new Matrix4f()
            .setOrtho(0, width, height, 0, 1000, 3000);
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_Z);
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(new Matrix4f().translation(0, 0, -2000));
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(projectionMatrix);
        }

        shader.bind();
        var u = (float) framebuffer.viewportWidth / framebuffer.textureWidth;
        var v = (float) framebuffer.viewportHeight / framebuffer.textureHeight;
        var builder = tessellator.getBuffer();
        builder.begin(VertexFormat.DrawMode.QUADS, localFormat(VertexFormats.POSITION_TEXTURE_COLOR));
        builder.vertex(0.0, height, 0.0)
            .texture(0.0F, 0.0F)
            .color(255, 255, 255, 255)
            .next();
        builder.vertex(width, height, 0.0)
            .texture(u, 0.0F)
            .color(255, 255, 255, 255)
            .next();
        builder.vertex(width, 0.0, 0.0)
            .texture(u, v)
            .color(255, 255, 255, 255)
            .next();
        builder.vertex(0.0, 0.0, 0.0)
            .texture(0.0F, v)
            .color(255, 255, 255, 255)
            .next();

        var buffer = builder.end();
        //buffer.getParameters().format().setupState();
        ((VertexBufferAccessor) buffer.getParameters().format().getBuffer()).setVertexFormat(null);
        BufferRenderer.draw(buffer);
        shader.unbind();
        glDepthMask(true);
        glColorMask(true, true, true, true);
    }

    private final Map<VertexFormat, VertexFormat> localFormats = new Object2ObjectOpenHashMap<>();

    @NotNull
    private VertexFormat localFormat(@NotNull VertexFormat format) {
        return localFormats.computeIfAbsent(format, this::createLocalFormat);
    }

    private VertexFormat createLocalFormat(VertexFormat vertexFormat) {
        var copy = Reprobate.copy(vertexFormat);
        ((VertexFormatAccessor) copy).setBuffer(null);
        return copy;
    }
}
