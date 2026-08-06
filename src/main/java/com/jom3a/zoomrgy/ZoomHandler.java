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
                return;
            }
            tickZoom();
        });
    }

    /**
     * Snaps the interpolated zoom back to "not zoomed". Without this, leaving a world while
     * zoomed leaves currentZoom at 1.0, so the next world you join renders fully zoomed in
     * and then animates back out.
     */
    private static void resetZoom() {
        ZoomState.currentZoom = 0.0;
        ZoomState.lastZoom = 0.0;
        ZoomState.targetedEntity = null;
    }

    private static void tickZoom() {
        // Keep the scroll level in range - lowering maxScrollLevel in the config screen
        // would otherwise leave a previously scrolled level stuck above the new maximum.
        int maxLevel = Math.max(1, ZoomConfig.get().maxScrollLevel);
        ZoomState.scrollLevel = Math.max(1, Math.min(maxLevel, ZoomState.scrollLevel));

        ZoomState.lastZoom = ZoomState.currentZoom;

        double target = ZoomState.getTargetZoom();
        double current = ZoomState.currentZoom;
        double speed = ZoomConfig.get().zoomSpeed;

        if (current < target) {
            ZoomState.currentZoom = Math.min(target, current + speed);
        } else if (current > target) {
            ZoomState.currentZoom = Math.max(target, current - speed);
        }
    }

    public static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }
}
