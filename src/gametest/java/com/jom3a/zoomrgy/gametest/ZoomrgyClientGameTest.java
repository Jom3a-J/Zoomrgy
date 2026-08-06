package com.jom3a.zoomrgy.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.jom3a.zoomrgy.HudAnchor;
import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomHud;
import com.jom3a.zoomrgy.ZoomKeyBindings;
import com.jom3a.zoomrgy.ZoomState;
import com.jom3a.zoomrgy.ZoomTransition;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;

import java.util.Arrays;
import java.util.List;

/**
 * Regression tests for the zoom fixes. Each method covers one previously broken behaviour;
 * the comment on each says what it would look like if the fix regressed.
 */
public class ZoomrgyClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        // These need no world, so run them before paying for world creation.
        testTransitionNormalisation();
        testConfigSanitisation();
        testKeybindCategoryIsTranslated(context);
        testEveryAnchorLandsOnScreen(context);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            testScrollStepsAreProportional(context);
            testScrollLevelsArePerPreset(context);
            testScrollLevelClampedToMax(context);
            testScrollIgnoredWhileScreenOpen(context);
            testFovStaysPositiveWithOvershootEasing(context);
            testHudRendersAtEveryAnchor(context);

            // Deliberately leave the world at full zoom - see the assertion below.
            context.getInput().holdKey(ZoomKeyBindings.ZOOM_KEY);
            context.waitFor(mc -> ZoomState.currentZoom >= 1.0);
        }

        testZoomResetsAfterLeavingWorld(context);
    }

    /**
     * Legacy easing names must map onto a type the config screen can actually show. A value
     * outside getSelectableTypes() leaves Cloth's selector on index -1, and a null type (what
     * Gson produces for an unrecognised name) used to NPE inside apply().
     */
    private void testTransitionNormalisation() {
        assertTrue(ZoomTransition.normalize(null) != null, "normalize(null) must not return null");
        assertTrue(ZoomTransition.normalize(ZoomTransition.Type.SINE) == ZoomTransition.Type.EASE_IN_OUT_SINE,
            "SINE should normalise to EASE_IN_OUT_SINE");

        // Must not throw.
        ZoomTransition.apply(0.5, null);

        List<ZoomTransition.Type> selectable = Arrays.asList(ZoomTransition.getSelectableTypes());
        for (ZoomTransition.Type type : ZoomTransition.Type.values()) {
            ZoomTransition.Type normalised = ZoomTransition.normalize(type);
            assertTrue(selectable.contains(normalised),
                "normalize(" + type + ") returned " + normalised + ", which the config screen cannot display");
        }
    }

    /** A hand-edited or badly migrated config must not be able to divide the FOV by zero. */
    private void testConfigSanitisation() {
        ZoomConfig.Config cfg = ZoomConfig.get();

        double zoomMultiplier = cfg.zoomMultiplier;
        int maxScrollLevel = cfg.maxScrollLevel;
        ZoomTransition.Type transitionType = cfg.transitionType;
        double zoomSpeed = cfg.zoomSpeed;

        try {
            cfg.zoomMultiplier = 0.0;
            cfg.maxScrollLevel = 0;
            cfg.transitionType = null;
            cfg.zoomSpeed = Double.NaN;
            cfg.hudAnchor = null;
            cfg.hudScale = Double.NaN;

            ZoomConfig.sanitize();

            assertTrue(cfg.zoomMultiplier > 0.0, "zoomMultiplier must be positive, was " + cfg.zoomMultiplier);
            assertTrue(cfg.maxScrollLevel >= 1, "maxScrollLevel must be at least 1, was " + cfg.maxScrollLevel);
            assertTrue(cfg.transitionType != null, "transitionType must not stay null");
            assertTrue(Double.isFinite(cfg.zoomSpeed), "zoomSpeed must be finite, was " + cfg.zoomSpeed);
            assertTrue(cfg.hudAnchor != null, "hudAnchor must not stay null");
            assertTrue(cfg.hudScale > 0.0, "hudScale must be positive, was " + cfg.hudScale);
        } finally {
            cfg.zoomMultiplier = zoomMultiplier;
            cfg.maxScrollLevel = maxScrollLevel;
            cfg.transitionType = transitionType;
            cfg.zoomSpeed = zoomSpeed;
            ZoomConfig.sanitize();
        }
    }

    /**
     * Every anchor must place the HUD somewhere on screen. The offsets used to be raw screen
     * deltas with a default of -60, which put all three top anchors at y = -60 - rendering
     * perfectly happily, entirely out of view. The crash smoke test below cannot see that, so
     * this checks the geometry directly.
     */
    private void testEveryAnchorLandsOnScreen(ClientGameTestContext context) {
        ZoomConfig.Config def = ZoomConfig.defaults();

        // Several sizes, because the interesting failures depend on screen height. At 270 - a
        // 1080p window at GUI scale 4 - a centre anchor that wrongly takes the inset lands within
        // 15px of the bottom anchor, so the position setting looks like it does nothing.
        int[][] sizes = {{854, 480}, {480, 270}, {320, 240}, {1920, 1080}};

        for (int[] size : sizes) {
            int width = size[0];
            int height = size[1];

            for (HudAnchor anchor : HudAnchor.values()) {
                float x = ZoomHud.originX(anchor, width, def.hudOffsetX);
                float y = ZoomHud.originY(anchor, height, def.hudOffsetY);

                assertTrue(x >= 0.0f && x <= width,
                    anchor + " puts the HUD origin at x=" + x + ", outside 0.." + width);
                assertTrue(y >= 0.0f && y <= height,
                    anchor + " puts the HUD origin at y=" + y + ", outside 0.." + height);
            }

            // Distinct anchors must land in visibly distinct places, or picking one silently
            // does nothing.
            HudAnchor[] all = HudAnchor.values();
            for (int i = 0; i < all.length; i++) {
                for (int j = i + 1; j < all.length; j++) {
                    float dx = ZoomHud.originX(all[i], width, def.hudOffsetX)
                        - ZoomHud.originX(all[j], width, def.hudOffsetX);
                    float dy = ZoomHud.originY(all[i], height, def.hudOffsetY)
                        - ZoomHud.originY(all[j], height, def.hudOffsetY);

                    assertTrue(Math.abs(dx) >= 20.0f || Math.abs(dy) >= 20.0f,
                        all[i] + " and " + all[j] + " land " + Math.abs(dx) + "," + Math.abs(dy)
                            + " apart at " + width + "x" + height + " - too close to tell apart");
                }
            }
        }

        int width = 854;
        int height = 480;

        // A centred axis has no edge to inset from, so the inset must not move it.
        assertTrue(ZoomHud.originY(HudAnchor.CENTER, height, 60) == height / 2.0f,
            "CENTER should sit on the vertical midpoint regardless of the inset");
        assertTrue(ZoomHud.originX(HudAnchor.TOP_CENTER, width, 60) == width / 2.0f,
            "TOP_CENTER should sit on the horizontal midpoint regardless of the inset");

        // The inset must move inwards from whichever edge is anchored, not in one fixed direction.
        assertTrue(ZoomHud.originY(HudAnchor.TOP_CENTER, height, 60) > 0.0f,
            "a top anchor should be pushed down from the top edge");
        assertTrue(ZoomHud.originY(HudAnchor.BOTTOM_CENTER, height, 60) < height,
            "a bottom anchor should be pushed up from the bottom edge");
        assertTrue(ZoomHud.originX(HudAnchor.TOP_RIGHT, width, 60) < width,
            "a right anchor should be pushed left from the right edge");
        assertTrue(ZoomHud.originX(HudAnchor.TOP_LEFT, width, 60) > 0.0f,
            "a left anchor should be pushed right from the left edge");
    }

    /**
     * Smoke test for the anchored HUD. Rendering happens on the render thread, so anything that
     * throws in there takes the whole client down - surviving every anchor at both scale extremes
     * with the telemetry line on is the assertion.
     */
    private void testHudRendersAtEveryAnchor(ClientGameTestContext context) {
        ZoomConfig.Config cfg = ZoomConfig.get();

        HudAnchor anchor = cfg.hudAnchor;
        double scale = cfg.hudScale;
        int offsetX = cfg.hudOffsetX;
        int offsetY = cfg.hudOffsetY;
        boolean showHud = cfg.showZoomHud;
        boolean telemetry = cfg.showTelemetryHud;
        boolean background = cfg.zoomHudBackground;

        try {
            context.runOnClient(mc -> {
                cfg.showZoomHud = true;
                cfg.showTelemetryHud = true;
                cfg.zoomHudBackground = true;
                ZoomState.isZoomLocked = true;
            });
            context.waitFor(mc -> ZoomState.currentZoom >= 1.0);

            for (HudAnchor candidate : HudAnchor.values()) {
                context.runOnClient(mc -> {
                    cfg.hudAnchor = candidate;
                    cfg.hudScale = 0.5;
                    cfg.hudOffsetX = 40;
                    cfg.hudOffsetY = -40;
                });
                context.waitTicks(2);

                context.runOnClient(mc -> cfg.hudScale = 2.0);
                context.waitTicks(2);
            }
        } finally {
            context.runOnClient(mc -> {
                cfg.hudAnchor = anchor;
                cfg.hudScale = scale;
                cfg.hudOffsetX = offsetX;
                cfg.hudOffsetY = offsetY;
                cfg.showZoomHud = showHud;
                cfg.showTelemetryHud = telemetry;
                cfg.zoomHudBackground = background;
                ZoomState.isZoomLocked = false;
            });
            context.waitTicks(25);
        }
    }

    /**
     * KeyMapping.Category derives its label from Identifier.toLanguageKey("key.category"), so the
     * lang file has to spell it key.category.zoomrgy.zoomrgy. Get it wrong and the Controls screen
     * shows the raw key.
     */
    private void testKeybindCategoryIsTranslated(ClientGameTestContext context) {
        String key = "key.category.zoomrgy.zoomrgy";
        String translated = context.computeOnClient(mc -> I18n.get(key));
        assertTrue(!key.equals(translated), "keybind category is untranslated; en_us.json is missing " + key);
    }

    /** Lowering the maximum in the config screen must pull an already-higher level back down. */
    private void testScrollLevelClampedToMax(ClientGameTestContext context) {
        ZoomConfig.Config cfg = ZoomConfig.get();
        int maxScrollLevel = cfg.maxScrollLevel;

        try {
            // Raise the ceiling first, so the level really is set high and is not simply rejected
            // on the way in - the clamp under test is the one that runs on tick.
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = 20;
                ZoomState.setScrollLevel(15);
            });
            context.waitTicks(3);
            int raised = context.computeOnClient(mc -> ZoomState.getScrollLevel());
            assertTrue(raised == 15, "scroll level should have reached 15, was " + raised);

            context.runOnClient(mc -> cfg.maxScrollLevel = 3);
            context.waitTicks(3);
            int level = context.computeOnClient(mc -> ZoomState.getScrollLevel());
            assertTrue(level == 3, "scroll level should have clamped to 3, was " + level);
        } finally {
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = maxScrollLevel;
                ZoomState.resetScrollLevels();
            });
        }
    }

    /**
     * Every scroll notch should be the same proportional step. The old linear divisor made the
     * first notch halve the FOV and the last one barely register.
     */
    private void testScrollStepsAreProportional(ClientGameTestContext context) {
        ZoomConfig.Config cfg = ZoomConfig.get();
        int maxScrollLevel = cfg.maxScrollLevel;
        double ratio = cfg.scrollStepRatio;

        try {
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = 20;
                cfg.scrollStepRatio = 1.5;
            });

            double previousStep = -1.0;
            for (int level = 1; level <= 5; level++) {
                final int target = level;
                double factor = context.computeOnClient(mc -> {
                    ZoomState.setScrollLevel(target);
                    return ZoomState.getScrollFactor();
                });

                if (previousStep > 0.0) {
                    double step = factor / previousStep;
                    assertTrue(Math.abs(step - 1.5) < 1.0e-6,
                        "step from level " + (level - 1) + " to " + level + " was " + step + ", expected 1.5");
                }
                previousStep = factor;
            }
        } finally {
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = maxScrollLevel;
                cfg.scrollStepRatio = ratio;
                ZoomState.resetScrollLevels();
            });
        }
    }

    /** Each zoom source keeps its own level, so dialling in preset 2 must not disturb the primary. */
    private void testScrollLevelsArePerPreset(ClientGameTestContext context) {
        ZoomConfig.Config cfg = ZoomConfig.get();
        int maxScrollLevel = cfg.maxScrollLevel;

        try {
            int primary = context.computeOnClient(mc -> {
                cfg.maxScrollLevel = 20;

                ZoomState.isZoomingPreset2 = false;
                ZoomState.setScrollLevel(4);

                ZoomState.isZoomingPreset2 = true;
                ZoomState.setScrollLevel(11);

                ZoomState.isZoomingPreset2 = false;
                return ZoomState.getScrollLevel();
            });
            assertTrue(primary == 4, "primary scroll level should have survived at 4, was " + primary);

            int preset2 = context.computeOnClient(mc -> {
                ZoomState.isZoomingPreset2 = true;
                int level = ZoomState.getScrollLevel();
                ZoomState.isZoomingPreset2 = false;
                return level;
            });
            assertTrue(preset2 == 11, "preset 2 scroll level should have been remembered as 11, was " + preset2);
        } finally {
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = maxScrollLevel;
                ZoomState.isZoomingPreset2 = false;
                ZoomState.resetScrollLevels();
            });
        }
    }

    /**
     * A locked zoom stays active while a screen is open, so the scroll hook has to stand down or it
     * eats inventory, creative-tab and chat scrolling. The first half of this test scrolls with no
     * screen open, so a regression cannot pass by breaking scrolling outright.
     */
    private void testScrollIgnoredWhileScreenOpen(ClientGameTestContext context) {
        try {
            context.runOnClient(mc -> {
                ZoomState.isZoomLocked = true;
                ZoomState.resetScrollLevels();
            });
            context.waitTicks(3);

            // Control: in-world scrolling should still change the zoom level.
            context.getInput().scroll(0.0, 1.0);
            context.waitTicks(3);
            int inWorld = context.computeOnClient(mc -> ZoomState.getScrollLevel());
            assertTrue(inWorld > 1, "scrolling in-world while zoomed should raise the level, was " + inWorld);

            // The actual regression: the same scroll with a screen open must be left alone.
            context.setScreen(() -> new ChatScreen("", false));
            context.waitFor(mc -> mc.gui.screen() != null);

            int before = context.computeOnClient(mc -> ZoomState.getScrollLevel());
            context.getInput().scroll(0.0, 1.0);
            context.waitTicks(3);
            int after = context.computeOnClient(mc -> ZoomState.getScrollLevel());
            assertTrue(before == after,
                "scrolling with a screen open must not change the zoom level (" + before + " -> " + after + ")");

            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(mc -> mc.gui.screen() == null);
        } finally {
            context.runOnClient(mc -> {
                ZoomState.isZoomLocked = false;
                ZoomState.resetScrollLevels();
            });
            context.waitTicks(3);
        }
    }

    /**
     * ELASTIC and BACK overshoot past 1.0. Interpolating toward a sub-degree target FOV with a
     * factor above 1 used to drive the FOV through zero and negative, wrecking the projection.
     */
    private void testFovStaysPositiveWithOvershootEasing(ClientGameTestContext context) {
        ZoomConfig.Config cfg = ZoomConfig.get();

        ZoomTransition.Type transitionType = cfg.transitionType;
        double preset2 = cfg.zoomMultiplierPreset2;
        int maxScrollLevel = cfg.maxScrollLevel;
        double zoomSpeed = cfg.zoomSpeed;

        try {
            context.runOnClient(mc -> {
                cfg.transitionType = ZoomTransition.Type.ELASTIC;
                cfg.zoomMultiplierPreset2 = 50.0; // config screen maximum
                cfg.maxScrollLevel = 20;
                cfg.zoomSpeed = 0.05;             // ~20 ticks, so we sample the whole curve
                ZoomState.setScrollLevel(20);
            });

            context.getInput().holdKey(ZoomKeyBindings.ZOOM_PRESET_2_KEY);

            float worst = Float.MAX_VALUE;
            for (int i = 0; i < 40; i++) {
                context.waitTick();
                float fov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
                worst = Math.min(worst, fov);
            }

            context.getInput().releaseKey(ZoomKeyBindings.ZOOM_PRESET_2_KEY);
            assertTrue(worst > 0.0f, "camera FOV reached " + worst + "; overshooting easing drove it non-positive");
        } finally {
            context.runOnClient(mc -> {
                cfg.transitionType = transitionType;
                cfg.zoomMultiplierPreset2 = preset2;
                cfg.maxScrollLevel = maxScrollLevel;
                cfg.zoomSpeed = zoomSpeed;
                ZoomState.resetScrollLevels();
            });
            context.waitTicks(25); // let the zoom settle back out
        }
    }

    /**
     * The interpolated zoom used to be frozen at whatever it was when the player went away, so the
     * next world you joined rendered fully zoomed in and then animated out.
     */
    private void testZoomResetsAfterLeavingWorld(ClientGameTestContext context) {
        context.getInput().releaseKey(ZoomKeyBindings.ZOOM_KEY);
        context.waitTicks(5);

        double current = context.computeOnClient(mc -> ZoomState.currentZoom);
        double last = context.computeOnClient(mc -> ZoomState.lastZoom);

        assertTrue(current == 0.0, "currentZoom should reset to 0 on leaving the world, was " + current);
        assertTrue(last == 0.0, "lastZoom should reset to 0 on leaving the world, was " + last);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
