package com.jom3a.zoomrgy;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Draws the divider and the preview panel beside the settings rows.
 *
 * <p>Takes no height of its own: the panel is drawn in {@code lateRender}, which runs after the
 * list, so it can sit in the column the rows leave free rather than pushing them down. The row
 * still has to exist for the list to call it.
 */
@Environment(EnvType.CLIENT)
public class SplitPreviewEntry extends TooltipListEntry<Object> {

    /** Where the panel sits below the top of the list. */
    private static final int TOP_INSET = 4;

    private final Supplier<ZoomTransition.Type> inType;
    private final Supplier<ZoomTransition.Type> outType;
    private final DoubleSupplier inSpeed;
    private final DoubleSupplier outSpeed;

    private int listTop = -1;

    public SplitPreviewEntry(Supplier<ZoomTransition.Type> inType,
                             Supplier<ZoomTransition.Type> outType,
                             DoubleSupplier inSpeed,
                             DoubleSupplier outSpeed) {
        super(Component.empty(), null);
        this.inType = inType;
        this.outType = outType;
        this.inSpeed = inSpeed;
        this.outSpeed = outSpeed;
    }

    @Override
    public int getItemHeight() {
        return 0;
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
        // Nothing here; this row is invisible. Remember where the list starts so the panel beside
        // it can line up with the first setting.
        listTop = y;
    }

    @Override
    public void lateRender(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        if (listTop < 0) return;

        ZoomPreviewImage.ensureAvailable();

        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int dividerX = SplitLayout.dividerX();
        int dividerTop = Math.max(0, listTop - TOP_INSET);
        int dividerBottom = Math.max(dividerTop + 1, screenHeight - 34);
        extractor.fill(dividerX, dividerTop, dividerX + 2, dividerBottom, 0xFFFFFFFF);

        int previewLeft = SplitLayout.previewLeft();
        int previewWidth = SplitLayout.previewWidth();
        int previewHeight = Math.min(previewWidth * 9 / 16, dividerBottom - dividerTop - 8);
        int previewTop = listTop + TOP_INSET;

        EasingPreview.render(extractor, Minecraft.getInstance().font,
            previewLeft, previewTop, previewWidth, previewHeight,
            inType.get(), outType.get(), inSpeed.getAsDouble(), outSpeed.getAsDouble());
    }
}
