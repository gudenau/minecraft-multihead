package net.gudenau.minecraft.multihead.mixin;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexFormat.class)
public interface VertexFormatAccessor {
    @Accessor void setBuffer(@Nullable VertexBuffer value);
}
