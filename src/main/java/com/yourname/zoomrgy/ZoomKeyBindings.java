package com.yourname.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
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
    private static boolean wasActive = false;

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(Zoomrgy.MOD_ID, "zoomrgy")
    );

    public static void register() {
        ZOOM_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zoomrgy.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,      // Default: C
            CATEGORY
        ));

        ZOOM_IN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zoomrgy.zoom_in",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        ));

        ZOOM_OUT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zoomrgy.zoom_out",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        ));

        ZOOM_PRESET_2_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zoomrgy.zoom_preset_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,      // Default: V
            CATEGORY
        ));

        ZOOM_LOCK_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zoomrgy.zoom_lock",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Default: None
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                if (ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive) {
                    ZoomState.isZooming = false;
                    ZoomState.isZoomingPreset2 = false;
                    ZoomState.isZoomLocked = false;
                    ZoomState.isSpyglassActive = false;
                    ZoomState.targetedEntity = null;
                    ZoomState.scrollLevel = 1;
                    wasPressing = false;
                    wasPressingPreset2 = false;
                    wasActive = false;
                }
                return;
            }

            ZoomConfig.Config cfg = ZoomConfig.get();
            boolean isHurt = cfg.zoomOutOnDamage && client.player.hurtTime > 0;

            // Drive spyglass state
            ZoomState.isSpyglassActive = cfg.spyglassAutoZoom && client.player.isUsingItem() && client.player.getUseItem().is(net.minecraft.world.item.Items.SPYGLASS);

            boolean activeBefore = ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive;

            if (isHurt) {
                ZoomState.isZooming = false;
                ZoomState.isZoomingPreset2 = false;
                ZoomState.isZoomLocked = false;
                while (ZOOM_KEY.consumeClick()) {}
                while (ZOOM_PRESET_2_KEY.consumeClick()) {}
                while (ZOOM_LOCK_KEY.consumeClick()) {}
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

            boolean activeAfter = ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive;

            // Handle Zoom In / Zoom Out keybinds while active
            if (activeAfter) {
                while (ZOOM_IN_KEY.consumeClick()) {
                    int prev = ZoomState.scrollLevel;
                    ZoomState.scrollLevel = Math.min(ZoomState.scrollLevel + 1, cfg.maxScrollLevel);
                    if (ZoomState.scrollLevel != prev && cfg.scrollAudioFeedback) {
                        client.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.6f
                            )
                        );
                    }
                }
                while (ZOOM_OUT_KEY.consumeClick()) {
                    int prev = ZoomState.scrollLevel;
                    ZoomState.scrollLevel = Math.max(ZoomState.scrollLevel - 1, 1);
                    if (ZoomState.scrollLevel != prev && cfg.scrollAudioFeedback) {
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

            // Reset scroll level on total release
            if (!activeAfter && wasActive && cfg.resetScrollOnRelease) {
                ZoomState.scrollLevel = 1;
            }
            wasActive = activeAfter;
        });
    }
}
