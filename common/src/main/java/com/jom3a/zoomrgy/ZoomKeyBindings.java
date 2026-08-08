package com.jom3a.zoomrgy;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ZoomKeyBindings {

    public static KeyMapping ZOOM_KEY;
    public static KeyMapping ZOOM_IN_KEY;
    public static KeyMapping ZOOM_OUT_KEY;
    public static KeyMapping ZOOM_PRESET_2_KEY;
    public static KeyMapping ZOOM_LOCK_KEY;

    private static boolean wasPressing = false;
    private static long lastPressTime = 0;
    private static boolean wasPressingPreset2 = false;
    private static long lastPressTimePreset2 = 0;

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(Zoomrgy.MOD_ID, "zoomrgy")
    );

    /** Builds the mappings. Each loader registers the returned list its own way. */
    public static void createKeyMappings() {
        ZOOM_KEY = new KeyMapping(
            "key.zoomrgy.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,      // Default: C
            CATEGORY
        );

        ZOOM_IN_KEY = new KeyMapping(
            "key.zoomrgy.zoom_in",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        );

        ZOOM_OUT_KEY = new KeyMapping(
            "key.zoomrgy.zoom_out",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        );

        ZOOM_PRESET_2_KEY = new KeyMapping(
            "key.zoomrgy.zoom_preset_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,      // Default: V
            CATEGORY
        );

        ZOOM_LOCK_KEY = new KeyMapping(
            "key.zoomrgy.zoom_lock",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        );

    }

    /** Every registered mapping, for the loader to hand to its own registration hook. */
    public static java.util.List<KeyMapping> all() {
        return java.util.List.of(ZOOM_KEY, ZOOM_IN_KEY, ZOOM_OUT_KEY, ZOOM_PRESET_2_KEY, ZOOM_LOCK_KEY);
    }

    /** Called once per client tick by whichever loader is hosting us. */
    public static void onClientTick(net.minecraft.client.Minecraft client) {
        {
            if (client.player == null) {
                if (ZoomState.isZoomActive()) {
                    ZoomState.isZooming = false;
                    ZoomState.isZoomingPreset2 = false;
                    ZoomState.isZoomLocked = false;
                    ZoomState.isSpyglassActive = false;
                    ZoomState.targetedEntity = null;
                    ZoomState.resetScrollLevels();
                    wasPressing = false;
                    wasPressingPreset2 = false;
                }
                return;
            }

            ZoomConfig.Config cfg = ZoomConfig.get();
            boolean isHurt = cfg.zoomOutOnDamage && client.player.hurtTime > 0;

            // Drive spyglass state
            ZoomState.isSpyglassActive = cfg.spyglassAutoZoom && client.player.isUsingItem() && client.player.getUseItem().is(net.minecraft.world.item.Items.SPYGLASS);

            if (isHurt) {
                ZoomState.isZooming = false;
                ZoomState.isZoomingPreset2 = false;
                ZoomState.isZoomLocked = false;
                while (ZOOM_KEY.consumeClick()) {}
                while (ZOOM_PRESET_2_KEY.consumeClick()) {}
                while (ZOOM_LOCK_KEY.consumeClick()) {}
                // Keep the edge-detection state in step with reality, otherwise a key held
                // through the damage window looks like it was never re-pressed afterwards
                // and the next double-tap is silently dropped.
                wasPressing = ZOOM_KEY.isDown();
                wasPressingPreset2 = ZOOM_PRESET_2_KEY.isDown();
            } else if (cfg.zoomToggleMode) {
                while (ZOOM_KEY.consumeClick()) {
                    ZoomState.isZooming = !ZoomState.isZooming;
                    ZoomState.isZoomLocked = false;
                }
                while (ZOOM_PRESET_2_KEY.consumeClick()) {
                    ZoomState.isZoomingPreset2 = !ZoomState.isZoomingPreset2;
                    ZoomState.isZoomLocked = false;
                }
                while (ZOOM_LOCK_KEY.consumeClick()) {
                    ZoomState.isZoomLocked = !ZoomState.isZoomLocked;
                    if (cfg.scrollAudioFeedback) {
                        client.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f
                            )
                        );
                    }
                }
            } else {
                boolean pressing = ZOOM_KEY.isDown();
                if (pressing && !wasPressing) {
                    if (cfg.doubleTapToLock) {
                        long now = System.currentTimeMillis();
                        if (now - lastPressTime < 300) {
                            ZoomState.isZoomLocked = !ZoomState.isZoomLocked;
                        } else if (ZoomState.isZoomLocked) {
                            ZoomState.isZoomLocked = false;
                        }
                        lastPressTime = now;
                    }
                }
                ZoomState.isZooming = pressing;
                wasPressing = pressing;

                boolean pressingPreset2 = ZOOM_PRESET_2_KEY.isDown();
                if (pressingPreset2 && !wasPressingPreset2) {
                    if (cfg.doubleTapToLock) {
                        long now = System.currentTimeMillis();
                        if (now - lastPressTimePreset2 < 300) {
                            ZoomState.isZoomLocked = !ZoomState.isZoomLocked;
                        } else if (ZoomState.isZoomLocked) {
                            ZoomState.isZoomLocked = false;
                        }
                        lastPressTimePreset2 = now;
                    }
                }
                ZoomState.isZoomingPreset2 = pressingPreset2;
                wasPressingPreset2 = pressingPreset2;

                while (ZOOM_LOCK_KEY.consumeClick()) {
                    ZoomState.isZoomLocked = !ZoomState.isZoomLocked;
                    if (cfg.scrollAudioFeedback) {
                        client.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f
                            )
                        );
                    }
                }
            }

            boolean activeAfter = ZoomState.isZoomActive();

            // Handle Zoom In / Zoom Out keybinds while active
            if (activeAfter) {
                while (ZOOM_IN_KEY.consumeClick()) {
                    int prev = ZoomState.getScrollLevel();
                    ZoomState.setScrollLevel(prev + 1);
                    if (ZoomState.getScrollLevel() != prev && cfg.scrollAudioFeedback) {
                        client.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.6f
                            )
                        );
                    }
                }
                while (ZOOM_OUT_KEY.consumeClick()) {
                    int prev = ZoomState.getScrollLevel();
                    ZoomState.setScrollLevel(prev - 1);
                    if (ZoomState.getScrollLevel() != prev && cfg.scrollAudioFeedback) {
                        client.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.6f
                            )
                        );
                    }
                }
            } else {
                // Clear any queued clicks while inactive
                while (ZOOM_IN_KEY.consumeClick()) {}
                while (ZOOM_OUT_KEY.consumeClick()) {}
            }

            // The scroll level is deliberately not reset here. ZoomHandler waits for the
            // fade-out to finish first, so releasing does not visibly snap the view back to the
            // base zoom level on its way out.
        }
    }
}
