package com.jom3a.zoomrgy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A still of the game, grabbed when the config screen opens, for the easing preview to zoom into.
 *
 * <p>A single capture rather than a live feed on purpose. The GUI is drawn into the main render
 * target, so sampling that target while drawing would be a read/write feedback loop, and the
 * command encoder offers no texture-to-texture copy to sidestep it. Grabbing once as the screen
 * opens costs one readback and shows real game imagery, which is all a preview of an easing curve
 * needs.
 *
 * <p>Opened from the title screen this captures the panorama, which is still a real view.
 */
@Environment(EnvType.CLIENT)
public final class ZoomPreviewSnapshot {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Zoomrgy.MOD_ID);

    private static final Identifier TEXTURE_ID =
        Identifier.fromNamespaceAndPath(Zoomrgy.MOD_ID, "config_preview");

    private static DynamicTexture texture;
    private static int width;
    private static int height;

    private ZoomPreviewSnapshot() {
    }

    /** Grabs the frame currently on screen. Safe to call when there is nothing to grab. */
    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;

        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) return;

        try {
            // The readback completes asynchronously, so install on the client thread.
            Screenshot.takeScreenshot(target, image -> mc.execute(() -> install(mc, image)));
        } catch (Exception e) {
            LOGGER.warn("Could not capture a preview frame", e);
        }
    }

    private static void install(Minecraft mc, NativeImage image) {
        try {
            release();
            width = image.getWidth();
            height = image.getHeight();
            texture = new DynamicTexture(() -> "zoomrgy_config_preview", image);
            mc.getTextureManager().register(TEXTURE_ID, texture);
        } catch (Exception e) {
            LOGGER.warn("Could not install the preview frame", e);
            image.close();
            texture = null;
        }
    }

    /** Drops the capture. The preview falls back to its drawn shapes until the next one lands. */
    public static void release() {
        if (texture == null) return;
        try {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
        } catch (Exception e) {
            LOGGER.warn("Could not release the preview frame", e);
        }
        texture = null;
    }

    public static boolean isReady() {
        return texture != null && width > 0 && height > 0;
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }
}
