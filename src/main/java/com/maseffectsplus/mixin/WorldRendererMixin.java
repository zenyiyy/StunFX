package com.maseffectsplus.mixin;

import com.maseffectsplus.render.EffectRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.memory.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the effects at the end of the world render. Here the world projection is still active; after
 * GameRenderer.renderWorld the hand projection (different FOV) is set, which made effects drift away
 * from their target towards the screen edges.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void maseffectsplus$renderEffects(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                              boolean renderBlockOutline, Camera camera,
                                              Matrix4f positionMatrix, Matrix4f basicProjectionMatrix,
                                              Matrix4f projectionMatrix, GpuBufferSlice fogBuffer,
                                              Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        EffectRenderer.render(tickCounter, camera, positionMatrix);
    }
}
