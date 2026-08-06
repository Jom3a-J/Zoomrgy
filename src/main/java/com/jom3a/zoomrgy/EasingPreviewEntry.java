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
import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A live preview of the configured easing curves.
 *
 * <p>Draws the inward and outward curves as graphs and runs a marker along them on a loop, timed
 * from the configured speeds, so the effect of a change is visible without leaving the screen.
 *
 * <p>This is drawn rather than shipped as animated images on purpose. Minecraft has no GIF
 * decoder, and pre-rendered clips for every curve would be a pile of assets that could silently
 * drift from {@link ZoomTransition}. Plotting the real function cannot disagree with it.
 */
@Environment(EnvType.CLIENT)
public class EasingPreviewEntry extends TooltipListEntry<Object> {

    private static final int GRAPH_HEIGHT = 46;
    private static final int GRAPH_GAP = 8;
    private static final int LABEL_HEIGHT = 11;

    /** Pause at each end of the loop, in milliseconds, so the ends are readable. */
    private static final long HOLD_MS = 450L;

    /** Curves overshoot, so the plot covers a little beyond 0..1 to keep the whole shape visible. */
    private static final double PLOT_MIN = -0.25;
    private static final double PLOT_MAX = 1.4;

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
        return LABEL_HEIGHT + GRAPH_HEIGHT + GRAPH_GAP + GRAPH_HEIGHT + 6;
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

        int width = Math.max(80, entryWidth - 12);
        int left = x;

        // Ticks to travel, converted to milliseconds, so the preview runs at the real speed.
        double inTicks = 1.0 / Math.max(0.05, inSpeed.getAsDouble());
        double outTicks = 1.0 / Math.max(0.05, outSpeed.getAsDouble());
        long inMs = (long) (inTicks * 50.0);
        long outMs = (long) (outTicks * 50.0);
        long cycle = inMs + HOLD_MS + outMs + HOLD_MS;

        long now = System.currentTimeMillis() % Math.max(1L, cycle);

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

        int graphY = y + LABEL_HEIGHT;
        drawCurve(extractor, font, left, graphY, width, "Zoom in", inType.get(), inTicks,
            phaseOut ? -1.0 : progress);

        int outY = graphY + GRAPH_HEIGHT + GRAPH_GAP;
        drawCurve(extractor, font, left, outY, width, "Zoom out", outType.get(), outTicks,
            phaseOut ? progress : -1.0);
    }

    /**
     * Plots one curve. {@code marker} is the progress of the animated dot, or negative when this
     * curve is not the one currently running.
     */
    private void drawCurve(GuiGraphicsExtractor extractor, Font font, int left, int top, int width,
                           String label, ZoomTransition.Type type, double ticks, double marker) {
        ZoomTransition.Type curve = ZoomTransition.normalize(type);

        int bottom = top + GRAPH_HEIGHT;

        extractor.fill(left, top, left + width, bottom, 0x40000000);
        extractor.fill(left, top, left + width, top + 1, 0x60FFFFFF);
        extractor.fill(left, bottom - 1, left + width, bottom, 0x60FFFFFF);

        // Guides for the 0 and 1 levels, since the overshooting curves cross them.
        int zeroY = valueToY(0.0, top, bottom);
        int oneY = valueToY(1.0, top, bottom);
        for (int gx = left + 2; gx < left + width - 2; gx += 4) {
            extractor.fill(gx, zeroY, gx + 2, zeroY + 1, 0x30FFFFFF);
            extractor.fill(gx, oneY, gx + 2, oneY + 1, 0x30FFFFFF);
        }

        int plotLeft = left + 3;
        int plotWidth = Math.max(1, width - 6);

        for (int px = 0; px < plotWidth; px++) {
            double t = (double) px / (plotWidth - 1);
            int py = valueToY(ZoomTransition.apply(t, curve), top, bottom);
            extractor.fill(plotLeft + px, py, plotLeft + px + 1, py + 1, 0xFF7FC7FF);
        }

        if (marker >= 0.0) {
            double clamped = Math.max(0.0, Math.min(1.0, marker));
            int mx = plotLeft + (int) (clamped * (plotWidth - 1));
            int my = valueToY(ZoomTransition.apply(clamped, curve), top, bottom);

            // Vertical tracker plus a dot, so the position reads at a glance.
            extractor.fill(mx, top + 1, mx + 1, bottom - 1, 0x40FFFFFF);
            extractor.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFFFDD55);
        }

        String caption = String.format(Locale.US, "%s  -  %s  (%.0f ticks)",
            label, curve.getDisplayName(), ticks);
        extractor.text(font, caption, left, top - LABEL_HEIGHT + 2, 0xFFBBBBBB, false);
    }

    private static int valueToY(double value, int top, int bottom) {
        double normalised = (value - PLOT_MIN) / (PLOT_MAX - PLOT_MIN);
        normalised = Math.max(0.0, Math.min(1.0, normalised));
        int usable = (bottom - top) - 4;
        return bottom - 2 - (int) Math.round(normalised * usable);
    }
}
