package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Drives the interpolated zoom value that everything else reads. */
@Environment(EnvType.CLIENT)
public class ZoomHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // The targeting cache is only valid within a single frame; dropping it every tick
            // also releases the entity, and through it the level, that it holds on to.
            ZoomTargeting.clearCache();

            if (client.player == null || client.level == null) {
                resetZoom();
                inWorldTicks = 0;
                return;
            }

            // Grab the config screen's preview picture from ordinary play, once the world has had
            // a moment to render. Doing it when the screen opens would capture the menu instead,
            // since the GUI is drawn into the same render target as the world.
            if (client.gui.screen() == null) {
                if (++inWorldTicks == PREVIEW_CAPTURE_DELAY_TICKS) {
                    ZoomPreviewImage.captureIfMissing();
                }
            }

            tickZoom();
        });
    }

    /**
     * Snaps the interpolated zoom back to "not zoomed". Without this, leaving a world while
     * zoomed leaves currentZoom at 1.0, so the next world you join renders fully zoomed in
     * and then animates back out.
     */
    /** Set while a zoom is engaged, so the scroll reset fires once per release, not every tick. */
    private static boolean scrollResetPending = false;

    /** Long enough for terrain to be drawn before the preview picture is taken. */
    private static final int PREVIEW_CAPTURE_DELAY_TICKS = 80;

    private static int inWorldTicks = 0;

    private static void resetZoom() {
        ZoomState.currentZoom = 0.0;
        ZoomState.lastZoom = 0.0;
        ZoomState.targetedEntity = null;
        ZoomState.resetScrollLevels();
        ZoomState.clearActiveZoom();
        scrollResetPending = false;
    }

    private static void tickZoom() {
        // Keep the scroll levels in range - lowering maxScrollLevel in the config screen
        // would otherwise leave a previously scrolled level stuck above the new maximum.
        ZoomState.clampScrollLevels();

        // Only re-latches while a zoom is engaged, so the fade-out keeps aiming at the same
        // magnification it was showing rather than jumping to whatever the flags now say.
        ZoomState.refreshActiveZoom();

        if (ZoomState.isZoomActive()) {
            scrollResetPending = true;
        }

        ZoomState.lastZoom = ZoomState.currentZoom;

        double target = ZoomState.getTargetZoom();
        double current = ZoomState.currentZoom;

        // Latch the direction before moving, so the curve and speed in play stay consistent for
        // the whole travel rather than flipping the moment the value settles.
        if (current < target) {
            ZoomState.setEasingOut(false);
        } else if (current > target) {
            ZoomState.setEasingOut(true);
        }

        double speed = ZoomState.activeSpeed();

        if (current < target) {
            ZoomState.currentZoom = Math.min(target, current + speed);
        } else if (current > target) {
            ZoomState.currentZoom = Math.max(target, current - speed);
        }

        // Wait for the fade-out to finish before clearing the scroll level. Clearing it the
        // instant the key comes up moves the target mid-animation, which reads as the view
        // snapping back to the base zoom level and only then travelling out to normal.
        if (scrollResetPending && !ZoomState.isZoomActive() && ZoomState.currentZoom <= 0.0) {
            if (ZoomConfig.get().resetScrollOnRelease) {
                ZoomState.resetScrollLevels();
            }
            ZoomState.clearActiveZoom();
            scrollResetPending = false;
        }
    }

    public static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }
}
