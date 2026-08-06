package com.jom3a.zoomrgy;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** A config row whose button hands off to a screen of its own. */
@Environment(EnvType.CLIENT)
public class OpenScreenEntry extends TooltipListEntry<Object> {

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;

    private final Button button;

    public OpenScreenEntry(Component fieldName, Component buttonLabel, Function<Screen, Screen> screenFactory) {
        super(fieldName, null);
        this.button = Button.builder(buttonLabel, b -> {
            Minecraft mc = Minecraft.getInstance();
            // Hand the current screen over so the new one can come back to it.
            mc.setScreenAndShow(screenFactory.apply(mc.gui.screen()));
        }).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    @Override
    public int getItemHeight() {
        return ROW_HEIGHT;
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
        extractor.text(Minecraft.getInstance().font, this.getFieldName(),
            x, y + (ROW_HEIGHT - Minecraft.getInstance().font.lineHeight) / 2,
            this.getPreferredTextColor(), false);

        button.setX(x + entryWidth - BUTTON_WIDTH);
        button.setY(y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2);
        button.extractRenderState(extractor, mouseX, mouseY, delta);
    }
}
