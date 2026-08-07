package com.jom3a.zoomrgy;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/** A percentage slider that keeps itself inside the settings column of the split page. */
@Environment(EnvType.CLIENT)
public class SplitSliderEntry extends TooltipListEntry<Integer> {

    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

    private final Slider slider;
    private final int defaultValue;
    private final Consumer<Integer> onSave;

    public SplitSliderEntry(Component fieldName, int value, int min, int max, int defaultValue,
                            IntFunction<String> label, Consumer<Integer> onSave, String tooltip) {
        super(fieldName, tooltip == null ? null
            : () -> java.util.Optional.of(new Component[]{Component.literal(tooltip)}));
        this.defaultValue = defaultValue;
        this.onSave = onSave;
        this.slider = new Slider(value, min, max, label);
    }

    public int getIntValue() {
        return slider.current();
    }

    @Override
    public int getItemHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public Integer getValue() {
        return slider.current();
    }

    @Override
    public Optional<Integer> getDefaultValue() {
        return Optional.of(defaultValue);
    }

    @Override
    public boolean isEdited() {
        return slider.current() != defaultValue;
    }

    @Override
    public void save() {
        onSave.accept(slider.current());
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(slider);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(slider);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int index, int y, int x,
                                   int entryWidth, int entryHeight, int mouseX, int mouseY,
                                   boolean isHovered, float delta) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        extractor.text(font, this.getFieldName(), x, y + (ROW_HEIGHT - font.lineHeight) / 2,
            this.getPreferredTextColor(), false);

        slider.setX(SplitLayout.controlX(x, entryWidth));
        slider.setY(y + (ROW_HEIGHT - CONTROL_HEIGHT) / 2);
        slider.setWidth(SplitLayout.controlWidth(x, entryWidth));
        slider.extractRenderState(extractor, mouseX, mouseY, delta);
    }

    private static class Slider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<String> label;

        Slider(int value, int min, int max, IntFunction<String> label) {
            super(0, 0, 100, CONTROL_HEIGHT, Component.empty(), (double) (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            updateMessage();
        }

        int current() {
            return (int) Math.round(min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.US, "%s", label.apply(current()))));
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }
    }
}
