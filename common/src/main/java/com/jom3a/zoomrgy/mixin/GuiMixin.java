package com.jom3a.zoomrgy.mixin;

import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomHandler;
import com.jom3a.zoomrgy.ZoomHud;
import com.jom3a.zoomrgy.ZoomState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class GuiMixin {

    // Hiding the surrounding HUD decorations lives in the Fabric subproject: NeoForge patches
    // extractHotbarAndDecorations out of Hud entirely, replacing it with its own overlay system,
    // so an injection into it here would fail to apply on that loader.


    private static final Identifier VIGNETTE_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/misc/vignette.png");
    private static final Identifier SPYGLASS_SCOPE_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/misc/spyglass_scope.png");

    @Shadow
    protected abstract void extractTextureOverlay(GuiGraphicsExtractor extractor, Identifier texture, float alpha);

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractItemHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomConfig.get().hideHotbar && ZoomState.isZoomActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomConfig.get().hideCrosshair && ZoomState.isZoomActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractSpyglassOverlay(GuiGraphicsExtractor extractor, float alpha, CallbackInfo ci) {
        if (ZoomConfig.get().showVanillaSpyglassOverlay) return;

        // Only suppress vanilla's scope while this mod is the thing driving the spyglass zoom.
        // Cancelling unconditionally meant that turning spyglass auto-zoom off still deleted
        // vanilla's overlay, changing behaviour the mod is otherwise not involved in.
        if (ZoomState.isSpyglassActive) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void onExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        double renderZoom = ZoomHandler.lerp(ZoomState.lastZoom, ZoomState.currentZoom, partialTicks);
        if (renderZoom <= 0.0) return;

        Minecraft mc = Minecraft.getInstance();
        // The vignette and scope are HUD elements, so they have to respect F1 as well - vanilla
        // hides its own spyglass overlay with it. This guard has to come before the overlays are
        // drawn, not just before the text.
        if (mc.player == null || mc.gui.hud.isHidden()) return;

        ZoomConfig.Config cfg = ZoomConfig.get();
        float overlayAlpha = (float) renderZoom;
        if (cfg.spyglassScopeOverlay) {
            this.extractTextureOverlay(extractor, SPYGLASS_SCOPE_TEXTURE, overlayAlpha);
        } else if (cfg.zoomVignetteOpacity > 0.0) {
            this.extractTextureOverlay(extractor, VIGNETTE_TEXTURE, overlayAlpha * (float) cfg.zoomVignetteOpacity);
        }

        if (cfg.showZoomHud) {
            ZoomHud.render(extractor, mc, renderZoom, partialTicks);
        }
    }
}
