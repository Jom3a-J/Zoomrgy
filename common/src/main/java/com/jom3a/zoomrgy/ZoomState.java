package com.jom3a.zoomrgy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class ZoomState {

    /** Current interpolated zoom progress (0.0 - 1.0). */
    public static double currentZoom = 0.0;

    /** Zoom progress from the previous tick. */
    public static double lastZoom = 0.0;

    /** Whether the zoom key is held. */
    public static boolean isZooming = false;

    /** Whether the zoom is locked active. */
    public static boolean isZoomLocked = false;

    public static boolean isZoomingPreset2 = false;
    public static boolean isSpyglassActive = false;
    public static Entity targetedEntity = null;

    // Each zoom source keeps its own scroll level, so dialling in a long shot on preset 2 does
    // not throw away where the primary zoom was left.
    private static int scrollPrimary = 1;
    private static int scrollPreset2 = 1;
    private static int scrollSpyglass = 1;

    /** True while any zoom source - key, preset 2, lock or spyglass - is engaged. */
    public static boolean isZoomActive() {
        return isZooming || isZoomingPreset2 || isZoomLocked || isSpyglassActive;
    }

    public static double getTargetZoom() {
        return isZoomActive() ? 1.0 : 0.0;
    }

    /** The scroll level belonging to whichever zoom source is currently driving. */
    public static int getScrollLevel() {
        if (isSpyglassActive) return scrollSpyglass;
        if (isZoomingPreset2) return scrollPreset2;
        return scrollPrimary;
    }

    /** Sets the active source's scroll level, clamped to the configured maximum. */
    public static void setScrollLevel(int level) {
        int clamped = Math.max(1, Math.min(maxScrollLevel(), level));
        if (isSpyglassActive) {
            scrollSpyglass = clamped;
        } else if (isZoomingPreset2) {
            scrollPreset2 = clamped;
        } else {
            scrollPrimary = clamped;
        }
    }

    public static void resetScrollLevels() {
        scrollPrimary = 1;
        scrollPreset2 = 1;
        scrollSpyglass = 1;
    }

    /** Pulls every stored level back into range after the maximum is lowered in the config. */
    public static void clampScrollLevels() {
        int max = maxScrollLevel();
        scrollPrimary = Math.max(1, Math.min(max, scrollPrimary));
        scrollPreset2 = Math.max(1, Math.min(max, scrollPreset2));
        scrollSpyglass = Math.max(1, Math.min(max, scrollSpyglass));
    }

    private static int maxScrollLevel() {
        return Math.max(1, ZoomConfig.get().maxScrollLevel);
    }

    /**
     * Extra magnification contributed by the scroll wheel.
     *
     * <p>This is geometric rather than linear. Dividing the FOV by the raw level made the first
     * notch halve it and the tenth barely register; raising a ratio to the level instead makes
     * every notch the same proportional step, so the wheel feels even across its whole range.
     */
    public static double getScrollFactor() {
        return Math.pow(ZoomConfig.get().scrollStepRatio, getScrollLevel() - 1);
    }

    // Magnification of the zoom currently being displayed, latched while one is engaged and held
    // through the fade-out. Releasing flips the state flags and clears the scroll level in the
    // same tick, so reading those live made the target FOV leap back to the primary preset's base
    // level before the zoom-out had even started - a visible snap on every release.
    private static double activeMultiplier = 1.0;
    private static double activeScrollFactor = 1.0;

    /** Re-latches the zoom parameters. Does nothing once the zoom is released, by design. */
    public static void refreshActiveZoom() {
        if (!isZoomActive()) return;

        ZoomConfig.Config cfg = ZoomConfig.get();
        if (isSpyglassActive) {
            activeMultiplier = cfg.spyglassZoomMultiplier;
        } else if (isZoomingPreset2) {
            activeMultiplier = cfg.zoomMultiplierPreset2;
        } else {
            activeMultiplier = cfg.zoomMultiplier;
        }
        activeScrollFactor = getScrollFactor();
    }

    /** Clears the latch once nothing is on screen, so the next zoom starts from a clean slate. */
    public static void clearActiveZoom() {
        activeMultiplier = 1.0;
        activeScrollFactor = 1.0;
    }

    // Which way the zoom is currently travelling, so the inward and outward curves and speeds
    // can differ. Held at its last value while the zoom is at rest.
    private static boolean easingOut = false;

    public static void setEasingOut(boolean out) {
        easingOut = out;
    }

    public static boolean isEasingOut() {
        return easingOut;
    }

    /** The easing curve for the direction currently being travelled. */
    public static ZoomTransition.Type activeTransition() {
        ZoomConfig.Config cfg = ZoomConfig.get();
        return easingOut ? cfg.transitionTypeOut : cfg.transitionType;
    }

    /** The interpolation speed for the direction currently being travelled. */
    public static double activeSpeed() {
        ZoomConfig.Config cfg = ZoomConfig.get();
        return easingOut ? cfg.zoomSpeedOut : cfg.zoomSpeed;
    }

    public static double getTargetFov() {
        Minecraft mc = Minecraft.getInstance();
        double baseFov = mc.options.fov().get();
        return baseFov / Math.max(1.0e-4, activeMultiplier * activeScrollFactor);
    }

    /**
     * The magnification actually on screen at the given interpolated zoom progress, derived the
     * same way {@code CameraMixin} derives the FOV so the two never disagree. Returns 1.0 when
     * not zoomed.
     */
    public static double getMagnification(double renderZoom) {
        if (renderZoom <= 0.0) return 1.0;

        Minecraft mc = Minecraft.getInstance();
        double baseFov = mc.options.fov().get();
        if (baseFov <= 0.0) return 1.0;

        double t = ZoomTransition.apply(renderZoom, activeTransition());
        double fov = Math.max(0.1, baseFov + (getTargetFov() - baseFov) * t);
        return Math.max(1.0, baseFov / fov);
    }
}
