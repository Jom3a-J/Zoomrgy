package com.jom3a.zoomrgy.mixin;

import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true, require = 1)
    private static void onSetupFog(Camera camera, int fogType, DeltaTracker deltaTracker, float farPlaneDistance, ClientLevel level, CallbackInfoReturnable<FogData> info) {
        if (!ZoomConfig.get().reduceFog) return;

        double renderZoom = com.jom3a.zoomrgy.ZoomHandler.lerp(
            ZoomState.lastZoom,
            ZoomState.currentZoom,
            (double) deltaTracker.getGameTimeDeltaPartialTick(true)
        );
        if (renderZoom <= 0.0) return;

        FogData data = info.getReturnValue();
        if (data != null) {
            // Push the fog back in proportion to how far you are actually magnified, rather than
            // by the raw scroll level, which ignored the preset multiplier entirely. Capped so a
            // very high zoom cannot shove the fog planes somewhere absurd.
            double magnification = ZoomState.getMagnification(renderZoom);
            double multiplier = 1.0 + Math.min(8.0, (magnification - 1.0) * 0.35);

            data.renderDistanceStart = (float) (data.renderDistanceStart * multiplier);
            data.renderDistanceEnd = (float) (data.renderDistanceEnd * multiplier);
            data.environmentalStart = (float) (data.environmentalStart * multiplier);
            data.environmentalEnd = (float) (data.environmentalEnd * multiplier);
        }
    }
}
