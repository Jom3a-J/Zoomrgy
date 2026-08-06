package com.jom3a.zoomrgy;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A live preview of the configured easing, shown as the zoom itself rather than as a graph.
 *
 * <p>A framed viewport sits on the right of the row, like any other Cloth widget, and the shapes
 * inside it swell and shrink on a loop driven by the real easing functions at the configured
 * speeds. Overshooting curves visibly push past their resting size and settle back.
 *
 * <p>Drawn rather than shipped as animated images: Minecraft has no GIF decoder, and pre-rendered
 * clips for every curve could silently drift from {@link ZoomTransition}.
 */
@Environment(EnvType.CLIENT)
public class EasingPreviewEntry extends TooltipListEntry<Object> {

    private static final int BOX_WIDTH = 118;
    private static final int BOX_HEIGHT = 40;
    private static final int ROW_PADDING = 4;

    /** Pause at each end of the loop, in milliseconds, so the ends are readable. */
    private static final long HOLD_MS = 400L;

    /** How much the preview shapes grow at full zoom. Purely visual. */
    private static final double PREVIEW_GAIN = 2.4;

    private final Supplier<ZoomTransition.Type> inType;
    private final Supplier<ZoomTransition.Type> outType;
    private final DoubleSupplier inSpeed;
    private final DoubleSupplier outSpeed;

    public EasingPreviewEntry(Component fieldName,
                              Supplier<ZoomTransition.Type> inType,
                              Supplier<ZoomTransition.Type> outType,
                              DoubleSupplier inSpeed,
                              DoubleSupplier outSpeed) {
        super(fieldName, null);
        this.inType = inType;
        this.outType = outType;
        this.inSpeed = inSpeed;
        this.outSpeed = outSpeed;
    }

    @Override
    public int getItemHeight() {
        return BOX_HEIGHT + ROW_PADDING * 2;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int index, int y, int x,
                                   int entryWidth, int entryHeight, int mouseX, int mouseY,
                                   boolean isHovered, float delta) {
        Font font = Minecraft.getInstance().font;

        // Ticks to travel, converted to milliseconds, so the loop runs at the configured speed.
        double inTicks = 1.0 / Math.max(0.05, inSpeed.getAsDouble());
        double outTicks = 1.0 / Math.max(0.05, outSpeed.getAsDouble());
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

        ZoomTransition.Type curve = ZoomTransition.normalize(phaseOut ? outType.get() : inType.get());
        double eased = ZoomTransition.apply(progress, curve);

        int boxLeft = x + entryWidth - BOX_WIDTH;
        int boxTop = y + ROW_PADDING;

        extractor.text(font, this.getFieldName(), x, y + (getItemHeight() - font.lineHeight) / 2,
            this.getPreferredTextColor(), false);

        String caption = String.format(Locale.US, "%s  %s",
            phaseOut ? "out" : "in", curve.getDisplayName());
        int captionWidth = font.width(caption);
        extractor.text(font, caption, boxLeft - captionWidth - 8,
            y + (getItemHeight() - font.lineHeight) / 2, 0xFF999999, false);

        drawViewport(extractor, boxLeft, boxTop, eased);
    }

    /** A framed scene whose contents scale with the eased value, so the curve is felt not read. */
    private void drawViewport(GuiGraphicsExtractor extractor, int left, int top, double eased) {
        int right = left + BOX_WIDTH;
        int bottom = top + BOX_HEIGHT;

        extractor.fill(left, top, right, bottom, 0xFF101014);
        extractor.fill(left, top, right, top + 1, 0x80FFFFFF);
        extractor.fill(left, bottom - 1, right, bottom, 0x80FFFFFF);
        extractor.fill(left, top, left + 1, bottom, 0x80FFFFFF);
        extractor.fill(right - 1, top, right, bottom, 0x80FFFFFF);

        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;

        // Overshoot is allowed to push past 1.0 here on purpose - that is the whole point of
        // previewing BACK and ELASTIC - so the shapes are clipped to the frame instead.
        double scale = 1.0 + eased * PREVIEW_GAIN;

        // A horizon, so growth reads as moving closer rather than a shape merely resizing.
        int horizonY = centerY + (int) Math.round(6 * scale);
        if (horizonY > top + 1 && horizonY < bottom - 1) {
            extractor.fill(left + 1, horizonY, right - 1, horizonY + 1, 0x40FFFFFF);
        }

        drawClippedBox(extractor, centerX, centerY, (int) Math.round(9 * scale),
            (int) Math.round(9 * scale), left, top, right, bottom, 0xFF5B8CD6);

        int satelliteOffset = (int) Math.round(22 * scale);
        drawClippedBox(extractor, centerX - satelliteOffset, centerY - (int) Math.round(4 * scale),
            (int) Math.round(4 * scale), (int) Math.round(4 * scale), left, top, right, bottom, 0xFF3E6699);
        drawClippedBox(extractor, centerX + satelliteOffset, centerY + (int) Math.round(2 * scale),
            (int) Math.round(5 * scale), (int) Math.round(5 * scale), left, top, right, bottom, 0xFF3E6699);
    }

    /** fill() does not clip, so anything spilling past the frame is trimmed by hand. */
    private void drawClippedBox(GuiGraphicsExtractor extractor, int centerX, int centerY,
                                int halfWidth, int halfHeight,
                                int clipLeft, int clipTop, int clipRight, int clipBottom, int color) {
        int x1 = Math.max(clipLeft + 1, centerX - halfWidth);
        int y1 = Math.max(clipTop + 1, centerY - halfHeight);
        int x2 = Math.min(clipRight - 1, centerX + halfWidth);
        int y2 = Math.min(clipBottom - 1, centerY + halfHeight);

        if (x2 <= x1 || y2 <= y1) return;
        extractor.fill(x1, y1, x2, y2, color);
    }
}
