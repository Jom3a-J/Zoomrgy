package com.yourname.zoomrgy.mixin;

import com.yourname.zoomrgy.ZoomConfig;
import com.yourname.zoomrgy.ZoomState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 1)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!(ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive)) return;

        ZoomConfig.Config cfg = ZoomConfig.get();
        int maxLevel = cfg.maxScrollLevel; // e.g. 10

        int prev = ZoomState.scrollLevel;
        if (vertical > 0) {
            ZoomState.scrollLevel = Math.min(ZoomState.scrollLevel + 1, maxLevel);
        } else if (vertical < 0) {
            ZoomState.scrollLevel = Math.max(ZoomState.scrollLevel - 1, 1);
        }

        if (ZoomState.scrollLevel != prev && cfg.scrollAudioFeedback) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.6f
                )
            );
        }

        // Cancel default scroll behavior while zooming
        ci.cancel();
    }
}
