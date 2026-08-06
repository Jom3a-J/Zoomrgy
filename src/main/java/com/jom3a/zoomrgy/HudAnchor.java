package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Where the zoom overlay sits on screen. The anchor picks the corner or edge it grows from; the
 * configured offsets then nudge it away from that point.
 */
@Environment(EnvType.CLIENT)
public enum HudAnchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    CENTER_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float horizontal;
    private final float vertical;

    HudAnchor(float horizontal, float vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /** 0.0 at the left edge, 0.5 centred, 1.0 at the right edge. */
    public float horizontal() {
        return horizontal;
    }

    /** 0.0 at the top edge, 0.5 centred, 1.0 at the bottom edge. */
    public float vertical() {
        return vertical;
    }

    /**
     * Which way the horizontal inset moves: +1 inwards from the left edge, -1 inwards from the
     * right, and 0 when the axis is centred. A centred axis has no edge to inset from, so
     * applying one there would just drag "centre" off towards a side - far enough, on a short
     * screen, to land on top of the neighbouring anchor.
     */
    public float insetDirectionX() {
        if (horizontal == 0.0f) return 1.0f;
        if (horizontal == 1.0f) return -1.0f;
        return 0.0f;
    }

    /** Vertical counterpart of {@link #insetDirectionX()}. */
    public float insetDirectionY() {
        if (vertical == 0.0f) return 1.0f;
        if (vertical == 1.0f) return -1.0f;
        return 0.0f;
    }

    /** True when the panel should grow outwards from its horizontal centre. */
    public boolean isHorizontallyCentered() {
        return horizontal == 0.5f;
    }

    /** True when the panel hangs off the right edge and should be laid out right to left. */
    public boolean isRightAligned() {
        return horizontal == 1.0f;
    }

    public String getDisplayName() {
        return switch (this) {
            case TOP_LEFT -> "Top Left";
            case TOP_CENTER -> "Top Center";
            case TOP_RIGHT -> "Top Right";
            case CENTER_LEFT -> "Center Left";
            case CENTER -> "Center";
            case CENTER_RIGHT -> "Center Right";
            case BOTTOM_LEFT -> "Bottom Left";
            case BOTTOM_CENTER -> "Bottom Center";
            case BOTTOM_RIGHT -> "Bottom Right";
        };
    }
}
