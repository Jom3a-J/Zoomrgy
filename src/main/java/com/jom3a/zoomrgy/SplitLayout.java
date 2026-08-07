package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * Geometry for the split transitions page: settings down the left, a divider, preview on the
 * right.
 *
 * <p>Cloth hands every row the full width of the list, so the rows have to narrow themselves to
 * the left column and the preview is drawn into the space that leaves. Keeping the arithmetic in
 * one place stops the rows and the divider from disagreeing about where the split is.
 */
@Environment(EnvType.CLIENT)
final class SplitLayout {

    /** Share of the width given to the settings column. */
    private static final double SPLIT = 0.68;

    static final int GUTTER = 10;
    static final int RIGHT_MARGIN = 12;

    private SplitLayout() {
    }

    static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    static int dividerX() {
        return (int) (screenWidth() * SPLIT);
    }

    /** Right edge available to a settings row starting at {@code x}. */
    static int columnWidth(int x, int entryWidth) {
        int available = dividerX() - GUTTER - x;
        return Math.max(60, Math.min(entryWidth, available));
    }

    /**
     * Widget bounds for a settings row: the control sits against the right edge of the settings
     * column with the field name to its left, the way the rest of the config screen reads.
     */
    static int controlWidth(int x, int entryWidth) {
        return Math.max(60, Math.min(150, columnWidth(x, entryWidth) / 2));
    }

    static int controlX(int x, int entryWidth) {
        return x + columnWidth(x, entryWidth) - controlWidth(x, entryWidth);
    }

    static int previewLeft() {
        return dividerX() + GUTTER;
    }

    static int previewWidth() {
        return Math.max(80, screenWidth() - previewLeft() - RIGHT_MARGIN);
    }
}
