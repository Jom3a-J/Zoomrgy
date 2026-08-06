package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Locale;

/**
 * Draws the easing preview: a picture from the game that crops in and out on a loop, following
 * the configured curves at the configured speeds.
 *
 * <p>Shared by the config screen row and the two-column transition screen so both show the same
 * thing at whatever size they have room for.
 */
@Environment(EnvType.CLIENT)
public final class EasingPreview {

    /** Pause at each end of the loop, in milliseconds, so the ends are readable. */
    private static final long HOLD_MS = 400L;

    /** How far the preview crops in at full zoom. Purely visual. */
    private static final double PREVIEW_GAIN = 2.4;

    private EasingPreview() {
    }

    public static void render(GuiGraphicsExtractor extractor, Font font,
                              int left, int top, int width, int height,
                              ZoomTransition.Type inType, ZoomTransition.Type outType,
                              double inSpeed, double outSpeed) {

        double inTicks = 1.0 / Math.max(0.05, inSpeed);
        double outTicks = 1.0 / Math.max(0.05, outSpeed);
        long inMs = (long) (inTicks * 50.0);
        long outMs = (long) (outTicks * 50.0);
        long cycle = Math.max(1L, inMs + HOLD_MS + outMs + HOLD_MS);

        long now = System.currentTimeMillis() % cycle;

        boolean phaseOut;
        double progress;
        if (now < inMs) {
            phaseOut = false;
            progress = inMs == 0 ? 1.0 : (double) now / inMs;
        } else if (now < inMs + HOLD_MS) {
            phaseOut = false;
            progress = 1.0;
        } else if (now < inMs + HOLD_MS + outMs) {
            phaseOut = true;
            progress = outMs == 0 ? 0.0 : 1.0 - (double) (now - inMs - HOLD_MS) / outMs;
        } else {
            phaseOut = true;
            progress = 0.0;
        }

        ZoomTransition.Type curve = ZoomTransition.normalize(phaseOut ? outType : inType);
        double eased = ZoomTransition.apply(progress, curve);

        if (ZoomPreviewImage.isReady()) {
            drawPicture(extractor, left, top, width, height, eased);
        } else {
            drawPlaceholder(extractor, left, top, width, height, eased);
        }

        drawFrame(extractor, left, top, width, height);

        String caption = String.format(Locale.US, "%s  -  %s",
            phaseOut ? "zooming out" : "zooming in", curve.getDisplayName());
        extractor.text(font, caption, left + 4, top + height - font.lineHeight - 4, 0xFFFFFFFF, true);
    }

    /**
     * Crops into the picture as the eased value rises, which is what zooming does to an image:
     * a narrower field of view magnifying the middle. Overshooting curves crop past their resting
     * point and settle back, exactly as they do in game.
     */
    private static void drawPicture(GuiGraphicsExtractor extractor, int left, int top, int width, int height, double eased) {
        int texWidth = ZoomPreviewImage.width();
        int texHeight = ZoomPreviewImage.height();

        double zoom = Math.max(1.0, 1.0 + eased * PREVIEW_GAIN);
        double boxAspect = (double) width / height;

        // Take the full width at rest and a matching slice of height, so the picture is never
        // stretched to fit the panel.
        int regionWidth = (int) Math.max(1, Math.round(texWidth / zoom));
        int regionHeight = (int) Math.max(1, Math.round(Math.min(texHeight, texWidth / boxAspect) / zoom));

        float u = (texWidth - regionWidth) / 2.0f;
        float v = (texHeight - regionHeight) / 2.0f;

        extractor.blit(RenderPipelines.GUI_TEXTURED, ZoomPreviewImage.textureId(),
            left, top, u, v, width, height, regionWidth, regionHeight, texWidth, texHeight);
    }

    /** Shown until a picture is available: shapes that swell on the same curve. */
    private static void drawPlaceholder(GuiGraphicsExtractor extractor, int left, int top, int width, int height, double eased) {
        int right = left + width;
        int bottom = top + height;
        extractor.fill(left, top, right, bottom, 0xFF101014);

        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        double scale = 1.0 + eased * PREVIEW_GAIN;

        int horizonY = centerY + (int) Math.round(height * 0.15 * scale);
        if (horizonY > top + 1 && horizonY < bottom - 1) {
            extractor.fill(left + 1, horizonY, right - 1, horizonY + 1, 0x40FFFFFF);
        }

        int half = (int) Math.round(height * 0.22 * scale);
        clippedBox(extractor, centerX, centerY, half, half, left, top, right, bottom, 0xFF5B8CD6);
    }

    /** fill() does not clip, so anything spilling past the panel is trimmed by hand. */
    private static void clippedBox(GuiGraphicsExtractor extractor, int centerX, int centerY,
                                   int halfWidth, int halfHeight,
                                   int clipLeft, int clipTop, int clipRight, int clipBottom, int color) {
        int x1 = Math.max(clipLeft + 1, centerX - halfWidth);
        int y1 = Math.max(clipTop + 1, centerY - halfHeight);
        int x2 = Math.min(clipRight - 1, centerX + halfWidth);
        int y2 = Math.min(clipBottom - 1, centerY + halfHeight);

        if (x2 <= x1 || y2 <= y1) return;
        extractor.fill(x1, y1, x2, y2, color);
    }

    private static void drawFrame(GuiGraphicsExtractor extractor, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        extractor.fill(left, top, right, top + 1, 0x80FFFFFF);
        extractor.fill(left, bottom - 1, right, bottom, 0x80FFFFFF);
        extractor.fill(left, top, left + 1, bottom, 0x80FFFFFF);
        extractor.fill(right - 1, top, right, bottom, 0x80FFFFFF);
    }
}
