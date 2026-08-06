package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The easing settings, laid out as two columns: the four controls on the left, and a preview that
 * stays put on the right rather than scrolling away with them.
 *
 * <p>Hand-built rather than another Cloth category because Cloth's list spans the full width of
 * the screen, so a pinned side panel would be drawn over the controls instead of beside them.
 */
@Environment(EnvType.CLIENT)
public class TransitionScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_HEIGHT = 20;
    private static final int GUTTER = 12;
    private static final int MARGIN = 16;

    private final Screen parent;

    /** Edited live so the preview reflects the pending values, saved when the screen closes. */
    private double speedIn;
    private double speedOut;
    private ZoomTransition.Type curveIn;
    private ZoomTransition.Type curveOut;

    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;

    public TransitionScreen(Screen parent) {
        super(Component.literal("Easing & Transitions"));
        this.parent = parent;

        ZoomConfig.Config cfg = ZoomConfig.get();
        this.speedIn = cfg.zoomSpeed;
        this.speedOut = cfg.zoomSpeedOut;
        this.curveIn = ZoomTransition.normalize(cfg.transitionType);
        this.curveOut = ZoomTransition.normalize(cfg.transitionTypeOut);
    }

    @Override
    protected void init() {
        ZoomPreviewImage.ensureAvailable();

        int columnWidth = Math.max(150, (this.width - MARGIN * 2 - GUTTER) / 2);
        int left = MARGIN;
        int top = 44;

        // Right column: a 16:9 panel filling the remaining width.
        previewWidth = Math.max(160, this.width - MARGIN * 2 - GUTTER - columnWidth);
        previewHeight = previewWidth * 9 / 16;
        previewLeft = this.width - MARGIN - previewWidth;
        previewTop = top;

        addRenderableWidget(new SpeedSlider(left, top, columnWidth,
            "Zoom In Speed", speedIn, value -> speedIn = value));

        addRenderableWidget(CycleButton.<ZoomTransition.Type>builder(
                type -> Component.literal(type.getDisplayName()), curveIn)
            .withValues(ZoomTransition.getSelectableTypes())
            .create(left, top + ROW_HEIGHT, columnWidth, CONTROL_HEIGHT,
                Component.literal("Zoom In Curve"), (button, value) -> curveIn = value));

        addRenderableWidget(new SpeedSlider(left, top + ROW_HEIGHT * 2, columnWidth,
            "Zoom Out Speed", speedOut, value -> speedOut = value));

        addRenderableWidget(CycleButton.<ZoomTransition.Type>builder(
                type -> Component.literal(type.getDisplayName()), curveOut)
            .withValues(ZoomTransition.getSelectableTypes())
            .create(left, top + ROW_HEIGHT * 3, columnWidth, CONTROL_HEIGHT,
                Component.literal("Zoom Out Curve"), (button, value) -> curveOut = value));

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, CONTROL_HEIGHT)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        extractor.text(this.font, this.title,
            this.width / 2 - this.font.width(this.title) / 2, 18, 0xFFFFFFFF, true);

        EasingPreview.render(extractor, this.font, previewLeft, previewTop, previewWidth, previewHeight,
            curveIn, curveOut, speedIn, speedOut);
    }

    @Override
    public void onClose() {
        ZoomConfig.Config cfg = ZoomConfig.get();
        cfg.zoomSpeed = speedIn;
        cfg.zoomSpeedOut = speedOut;
        cfg.transitionType = curveIn;
        cfg.transitionTypeOut = curveOut;
        ZoomConfig.sanitize();
        ZoomConfig.save();

        this.minecraft.setScreenAndShow(parent);
    }

    /** Percentage slider over the same 5..100 range the config screen used. */
    private static class SpeedSlider extends AbstractSliderButton {
        private final String label;
        private final java.util.function.DoubleConsumer sink;

        SpeedSlider(int x, int y, int width, String label, double initial, java.util.function.DoubleConsumer sink) {
            super(x, y, width, CONTROL_HEIGHT, Component.empty(), (initial - 0.05) / 0.95);
            this.label = label;
            this.sink = sink;
            updateMessage();
        }

        private double percent() {
            return 0.05 + this.value * 0.95;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.US, "%s: %.0f%%", label, percent() * 100.0)));
        }

        @Override
        protected void applyValue() {
            sink.accept(percent());
        }
    }
}
