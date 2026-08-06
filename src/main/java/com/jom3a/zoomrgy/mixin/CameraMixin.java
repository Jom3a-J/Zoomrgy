package com.jom3a.zoomrgy.mixin;

import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomState;
import com.jom3a.zoomrgy.ZoomTransition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true, require = 1)
    private void modifyFov(float partialTick, CallbackInfoReturnable<Float> info) {
        double renderZoom = zoomrgy$lerp(ZoomState.lastZoom, ZoomState.currentZoom, (double) partialTick);
        if (renderZoom <= 0.0) {
            ZoomState.targetedEntity = null;
            return;
        }

        float original = info.getReturnValue();
        ZoomConfig.Config cfg = ZoomConfig.get();

        // Target FOV from active zoom state / preset
        float targetFov = (float) ZoomState.getTargetFov();

        // Calculate transition start FOV with dynamic movement damping
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float baseFov = (float) (double) mc.options.fov().get();
        float startFov = original;
        if (mc.player != null && mc.player.isUsingItem() && mc.player.getUseItem().is(net.minecraft.world.item.Items.SPYGLASS)) {
            startFov = baseFov;
        } else {
            float movementFovOffset = original - baseFov;
            float dampedMovementOffset = (float) (movementFovOffset * (1.0 - renderZoom * cfg.movementFovDamping));
            startFov = baseFov + dampedMovementOffset;
        }

        // Apply transition easing
        ZoomTransition.Type transitionType = cfg.transitionType;
        float t = (float) ZoomTransition.apply(renderZoom, transitionType);

        // Update targeted entity on render frame for smooth glowing outline
        if (cfg.highlightTargetEntity && mc.level != null) {
            net.minecraft.world.phys.HitResult hit = com.jom3a.zoomrgy.ZoomHandler.customRaycast(mc, 150.0, partialTick);
            if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                ZoomState.targetedEntity = entityHit.getEntity();
            } else {
                ZoomState.targetedEntity = null;
            }
        } else {
            ZoomState.targetedEntity = null;
        }

        info.setReturnValue(zoomrgy$safeFov(zoomrgy$lerp(startFov, targetFov, t)));
    }

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true, require = 1)
    private void modifyHudFov(float partialTick, CallbackInfoReturnable<Float> info) {
        if (!ZoomConfig.get().affectHandFov) return;

        double renderZoom = zoomrgy$lerp(ZoomState.lastZoom, ZoomState.currentZoom, (double) partialTick);
        if (renderZoom <= 0.0) return;

        float original = info.getReturnValue();
        ZoomConfig.Config cfg = ZoomConfig.get();

        // Target FOV from active zoom state / preset
        float targetFov = (float) ZoomState.getTargetFov();

        // Apply transition easing
        ZoomTransition.Type transitionType = cfg.transitionType;
        float t = (float) ZoomTransition.apply(renderZoom, transitionType);

        info.setReturnValue(zoomrgy$safeFov(zoomrgy$lerp(original, targetFov, t)));
    }

    /**
     * The BACK and ELASTIC curves overshoot past 1.0, which at high magnification pushes the
     * interpolated FOV straight through zero into negative territory and breaks the projection
     * matrix. Clamp to something small but still valid.
     */
    private static float zoomrgy$safeFov(float fov) {
        if (!Float.isFinite(fov)) return 1.0f;
        return Math.max(0.1f, Math.min(179.0f, fov));
    }

    private static float zoomrgy$lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static double zoomrgy$lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
