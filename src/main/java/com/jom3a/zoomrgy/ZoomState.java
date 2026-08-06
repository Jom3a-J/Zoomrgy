package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ZoomState {

    // Current interpolated zoom progress (0.0 – 1.0)
    public static double currentZoom = 0.0;

    // Zoom progress from the previous tick
    public static double lastZoom = 0.0;

    // The discrete zoom multiplier set by scroll (1 = base, higher = more zoom)
    public static int scrollLevel = 1;

    // Whether the zoom key is held
    public static boolean isZooming = false;

    // Whether the zoom is locked active
    public static boolean isZoomLocked = false;

    // Advanced zoom states
    public static boolean isZoomingPreset2 = false;
    public static boolean isSpyglassActive = false;
    public static net.minecraft.world.entity.Entity targetedEntity = null;

    /** True while any zoom source - key, preset 2, lock or spyglass - is engaged. */
    public static boolean isZoomActive() {
        return isZooming || isZoomingPreset2 || isZoomLocked || isSpyglassActive;
    }

    public static double getTargetZoom() {
        return isZoomActive() ? 1.0 : 0.0;
    }

    public static double getTargetFov() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double baseFov = mc.options.fov().get();
        ZoomConfig.Config cfg = ZoomConfig.get();
        if (isSpyglassActive) {
            return (baseFov / cfg.spyglassZoomMultiplier) / scrollLevel;
        }
        if (isZoomingPreset2) {
            return (baseFov / cfg.zoomMultiplierPreset2) / scrollLevel;
        }
        return (baseFov / cfg.zoomMultiplier) / scrollLevel;
    }

    /**
     * The magnification actually on screen at the given interpolated zoom progress, derived the
     * same way {@code CameraMixin} derives the FOV so the two never disagree. Returns 1.0 when
     * not zoomed.
     */
    public static double getMagnification(double renderZoom) {
        if (renderZoom <= 0.0) return 1.0;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double baseFov = mc.options.fov().get();
        if (baseFov <= 0.0) return 1.0;

        double t = ZoomTransition.apply(renderZoom, ZoomConfig.get().transitionType);
        double fov = Math.max(0.1, baseFov + (getTargetFov() - baseFov) * t);
        return Math.max(1.0, baseFov / fov);
    }
}
