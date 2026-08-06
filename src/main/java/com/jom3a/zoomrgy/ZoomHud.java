package com.jom3a.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * The zoom overlay: magnification and heading on one line, rangefinder telemetry on the next.
 *
 * <p>Every glyph used here has to live inside the Basic Multilingual Plane. Minecraft only loads
 * the plane 0 unifont files, so a supplementary-plane character renders as a missing-glyph box.
 */
@Environment(EnvType.CLIENT)
public final class ZoomHud {

    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private static final String ICON_BLOCK = "⛶";  // ⛶
    private static final String ICON_RANGE = "⌖";  // ⌖
    private static final String ICON_ENTITY = "☺"; // ☺
    private static final String ICON_MONSTER = "☠"; // ☠
    private static final String ICON_ANIMAL = "❤";  // ❤

    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int LINE_GAP = 4;

    private ZoomHud() {
    }

    /**
     * Draws the overlay. The caller has already established that a zoom is in progress, the HUD is
     * not hidden and the player exists.
     */
    public static void render(GuiGraphicsExtractor extractor, Minecraft mc, double renderZoom, float partialTicks) {
        ZoomConfig.Config cfg = ZoomConfig.get();

        double magnification = ZoomState.getMagnification(renderZoom);
        String label = String.format(Locale.US, "%.1fx  |  %s", magnification, heading(mc));
        String telemetry = cfg.showTelemetryHud ? telemetry(mc, magnification, partialTicks) : "";

        Font font = mc.font;
        int lineHeight = font.lineHeight;
        int labelWidth = font.width(label);
        int telemetryWidth = telemetry.isEmpty() ? 0 : font.width(telemetry);

        // Slide the panel up slightly as the zoom comes in.
        int slide = (int) ((1.0 - renderZoom) * 10.0);

        int labelX = (extractor.guiWidth() - labelWidth) / 2;
        int labelY = extractor.guiHeight() - 60 - lineHeight + slide;
        int telemetryX = (extractor.guiWidth() - telemetryWidth) / 2;
        int telemetryY = labelY + lineHeight + LINE_GAP;

        int alpha = (int) (renderZoom * 255.0) & 0xFF;
        int textColor = (cfg.zoomHudColor & 0xFFFFFF) | (alpha << 24);

        if (cfg.zoomHudBackground) {
            int left = (telemetry.isEmpty() ? labelX : Math.min(labelX, telemetryX)) - PADDING_X;
            int right = (telemetry.isEmpty() ? labelX + labelWidth
                : Math.max(labelX + labelWidth, telemetryX + telemetryWidth)) + PADDING_X;
            int top = labelY - PADDING_Y;
            int bottom = (telemetry.isEmpty() ? labelY + lineHeight : telemetryY + lineHeight) + PADDING_Y;

            drawPanel(extractor, left, top, right, bottom, renderZoom);
        }

        extractor.text(font, label, labelX, labelY, textColor, true);
        if (!telemetry.isEmpty()) {
            extractor.text(font, telemetry, telemetryX, telemetryY, textColor, true);
        }
    }

    private static void drawPanel(GuiGraphicsExtractor extractor, int left, int top, int right, int bottom, double renderZoom) {
        int bgAlpha = (int) (renderZoom * 0x60) & 0xFF;
        int fill = bgAlpha << 24;
        int border = (bgAlpha << 24) | 0x808080;

        extractor.fill(left, top, right, bottom, fill);

        extractor.fill(left, top, right, top + 1, border);
        extractor.fill(left, bottom - 1, right, bottom, border);
        extractor.fill(left, top, left + 1, bottom, border);
        extractor.fill(right - 1, top, right, bottom, border);
    }

    /** Minecraft yaw runs 0=S, 90=W, 180=N, 270=E. */
    private static String heading(Minecraft mc) {
        float degrees = (mc.player.getYRot() % 360.0f + 360.0f) % 360.0f;
        int sector = (int) Math.floor((degrees + 22.5f) / 45.0f) % COMPASS.length;
        return String.format(Locale.US, "%.0f° %s", degrees, COMPASS[sector]);
    }

    private static String telemetry(Minecraft mc, double magnification, float partialTicks) {
        HitResult hit = ZoomTargeting.raycast(mc, ZoomTargeting.rangeFor(magnification), partialTicks);
        if (hit == null || hit.getType() == HitResult.Type.MISS) return "";

        Vec3 eye = mc.player.getEyePosition(partialTicks);

        if (hit instanceof BlockHitResult blockHit) {
            if (mc.level == null) return "";
            BlockPos pos = blockHit.getBlockPos();
            String name = mc.level.getBlockState(pos).getBlock().getName().getString();
            if (name.isEmpty()) return "";

            return String.format(Locale.US, "%s %s  |  %s %.1fm  |  %d %d %d",
                ICON_BLOCK, name, ICON_RANGE, eye.distanceTo(blockHit.getLocation()),
                pos.getX(), pos.getY(), pos.getZ());
        }

        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            String name = entity.getName().getString();
            if (name.isEmpty()) return "";

            String base = String.format(Locale.US, "%s %s  |  %s %.1fm",
                iconFor(entity), name, ICON_RANGE, eye.distanceTo(entityHit.getLocation()));

            if (entity instanceof LivingEntity living) {
                return base + String.format(Locale.US, "  |  %s %.0f/%.0f",
                    ICON_ANIMAL, living.getHealth(), living.getMaxHealth());
            }
            return base;
        }

        return "";
    }

    private static String iconFor(Entity entity) {
        if (entity instanceof Monster) return ICON_MONSTER;
        if (entity instanceof Animal) return ICON_ANIMAL;
        return ICON_ENTITY;
    }
}
