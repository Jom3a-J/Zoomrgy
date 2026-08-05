package com.yourname.zoomrgy.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.yourname.zoomrgy.ZoomConfig;
import com.yourname.zoomrgy.ZoomKeyBindings;
import com.yourname.zoomrgy.ZoomState;
import com.yourname.zoomrgy.ZoomTransition;
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

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            testScrollLevelClampedToMax(context);
            testScrollIgnoredWhileScreenOpen(context);
            testFovStaysPositiveWithOvershootEasing(context);

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

            ZoomConfig.sanitize();

            assertTrue(cfg.zoomMultiplier > 0.0, "zoomMultiplier must be positive, was " + cfg.zoomMultiplier);
            assertTrue(cfg.maxScrollLevel >= 1, "maxScrollLevel must be at least 1, was " + cfg.maxScrollLevel);
            assertTrue(cfg.transitionType != null, "transitionType must not stay null");
            assertTrue(Double.isFinite(cfg.zoomSpeed), "zoomSpeed must be finite, was " + cfg.zoomSpeed);
        } finally {
            cfg.zoomMultiplier = zoomMultiplier;
            cfg.maxScrollLevel = maxScrollLevel;
            cfg.transitionType = transitionType;
            cfg.zoomSpeed = zoomSpeed;
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
            context.runOnClient(mc -> {
                ZoomState.scrollLevel = 15;
                cfg.maxScrollLevel = 3;
            });
            context.waitTicks(3);

            int level = context.computeOnClient(mc -> ZoomState.scrollLevel);
            assertTrue(level == 3, "scroll level should have clamped to 3, was " + level);
        } finally {
            context.runOnClient(mc -> {
                cfg.maxScrollLevel = maxScrollLevel;
                ZoomState.scrollLevel = 1;
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
                ZoomState.scrollLevel = 1;
            });
            context.waitTicks(3);

            // Control: in-world scrolling should still change the zoom level.
            context.getInput().scroll(0.0, 1.0);
            context.waitTicks(3);
            int inWorld = context.computeOnClient(mc -> ZoomState.scrollLevel);
            assertTrue(inWorld > 1, "scrolling in-world while zoomed should raise the level, was " + inWorld);

            // The actual regression: the same scroll with a screen open must be left alone.
            context.setScreen(() -> new ChatScreen("", false));
            context.waitFor(mc -> mc.gui.screen() != null);

            int before = context.computeOnClient(mc -> ZoomState.scrollLevel);
            context.getInput().scroll(0.0, 1.0);
            context.waitTicks(3);
            int after = context.computeOnClient(mc -> ZoomState.scrollLevel);
            assertTrue(before == after,
                "scrolling with a screen open must not change the zoom level (" + before + " -> " + after + ")");

            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(mc -> mc.gui.screen() == null);
        } finally {
            context.runOnClient(mc -> {
                ZoomState.isZoomLocked = false;
                ZoomState.scrollLevel = 1;
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
                ZoomState.scrollLevel = 20;
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
                ZoomState.scrollLevel = 1;
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
