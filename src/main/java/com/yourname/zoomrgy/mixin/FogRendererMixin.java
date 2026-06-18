package com.yourname.zoomrgy.mixin;

import com.yourname.zoomrgy.ZoomConfig;
import com.yourname.zoomrgy.ZoomState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true, require = 1)
    private static void onSetupFog(Camera camera, int fogType, DeltaTracker deltaTracker, float farPlaneDistance, ClientLevel level, CallbackInfoReturnable<FogData> info) {
        if (!ZoomConfig.get().reduceFog) return;

        double renderZoom = com.yourname.zoomrgy.ZoomHandler.lerp(
            ZoomState.lastZoom,
            ZoomState.currentZoom,
            (double) deltaTracker.getGameTimeDeltaPartialTick(true)
        );
        if (renderZoom <= 0.0) return;

        FogData data = info.getReturnValue();
        if (data != null) {
            // Calculate a multiplier to scale the fog starting and ending distances.
            // Pushing the fog back proportionally to the zoom multiplier.
            double multiplier = 1.0 + (ZoomState.scrollLevel * 0.5) * renderZoom;

            data.renderDistanceStart = (float) (data.renderDistanceStart * multiplier);
            data.renderDistanceEnd = (float) (data.renderDistanceEnd * multiplier);
            data.environmentalStart = (float) (data.environmentalStart * multiplier);
            data.environmentalEnd = (float) (data.environmentalEnd * multiplier);
        }
    }
}
