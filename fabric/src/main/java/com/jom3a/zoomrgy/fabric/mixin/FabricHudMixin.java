package com.jom3a.zoomrgy.fabric.mixin;

import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the HUD decorations around the hotbar while zoomed.
 *
 * <p>Fabric only. NeoForge patches {@code extractHotbarAndDecorations} out of {@code Hud} and
 * renders those pieces through its own overlay system, so this injection has no target there.
 */
@Mixin(Hud.class)
public abstract class FabricHudMixin {

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractHotbarAndDecorations(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomConfig.get().hideHotbar && ZoomState.isZoomActive()) {
            ci.cancel();
        }
    }
}
