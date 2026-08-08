package com.jom3a.zoomrgy;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** A cycling picker that keeps itself inside the settings column of the split page. */
public class SplitCycleEntry<T> extends TooltipListEntry<T> {

    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

    private final CycleButton<T> button;
    private final T defaultValue;
    private final Consumer<T> onSave;
    private T current;

    public SplitCycleEntry(Component fieldName, T[] values, T value, T defaultValue,
                           Function<T, Component> naming, Consumer<T> onSave, String tooltip) {
        super(fieldName, tooltip == null ? null
            : () -> java.util.Optional.of(new Component[]{Component.literal(tooltip)}));
        this.defaultValue = defaultValue;
        this.onSave = onSave;
        this.current = value;
        this.button = CycleButton.<T>builder(naming, value)
            .withValues(values)
            .create(0, 0, 100, CONTROL_HEIGHT, Component.empty(), (b, selected) -> this.current = selected);
    }

    @Override
    public int getItemHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public T getValue() {
        return current;
    }

    @Override
    public Optional<T> getDefaultValue() {
        return Optional.of(defaultValue);
    }

    @Override
    public boolean isEdited() {
        return current != defaultValue;
    }

    @Override
    public void save() {
        onSave.accept(current);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int index, int y, int x,
                                   int entryWidth, int entryHeight, int mouseX, int mouseY,
                                   boolean isHovered, float delta) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        extractor.text(font, this.getFieldName(), x, y + (ROW_HEIGHT - font.lineHeight) / 2,
            this.getPreferredTextColor(), false);

        button.setX(SplitLayout.controlX(x, entryWidth));
        button.setY(y + (ROW_HEIGHT - CONTROL_HEIGHT) / 2);
        button.setWidth(SplitLayout.controlWidth(x, entryWidth));
        button.extractRenderState(extractor, mouseX, mouseY, delta);
    }
}
