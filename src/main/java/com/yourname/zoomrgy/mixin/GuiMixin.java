package com.yourname.zoomrgy.mixin;

import com.yourname.zoomrgy.ZoomConfig;
import com.yourname.zoomrgy.ZoomState;
import com.yourname.zoomrgy.ZoomTransition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public abstract class GuiMixin {

    private static final net.minecraft.resources.Identifier VIGNETTE_TEXTURE = 
        net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "textures/misc/vignette.png");
    private static final net.minecraft.resources.Identifier SPYGLASS_SCOPE_TEXTURE = 
        net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "textures/misc/spyglass_scope.png");

    @org.spongepowered.asm.mixin.Shadow
    protected abstract void extractTextureOverlay(GuiGraphicsExtractor extractor, net.minecraft.resources.Identifier texture, float alpha);

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractHotbarAndDecorations(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isSpyglassActive) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractItemHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isSpyglassActive) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ZoomConfig.get().hideCrosshair && (ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractSpyglassOverlay(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, float f, CallbackInfo ci) {
        if (!ZoomConfig.get().showVanillaSpyglassOverlay) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void onExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        ZoomConfig.Config cfg = ZoomConfig.get();
        double renderZoom = zoomrgy$lerp(ZoomState.lastZoom, ZoomState.currentZoom, (double) deltaTracker.getGameTimeDeltaPartialTick(true));

        // Draw overlays if zoom is active
        if (renderZoom > 0.0) {
            float alpha = (float) renderZoom;
            if (cfg.spyglassScopeOverlay) {
                this.extractTextureOverlay(extractor, SPYGLASS_SCOPE_TEXTURE, alpha);
            } else if (cfg.zoomVignetteOpacity > 0.0) {
                this.extractTextureOverlay(extractor, VIGNETTE_TEXTURE, alpha * (float) cfg.zoomVignetteOpacity);
            }
        }

        if (!cfg.showZoomHud) return;
        if (renderZoom <= 0.0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int width = extractor.guiWidth();
        int height = extractor.guiHeight();

        float originalFov = (float) mc.options.fov().get();
        float targetFov = (float) ZoomState.getTargetFov();

        ZoomTransition.Type transitionType = cfg.transitionType;
        float t = (float) ZoomTransition.apply(renderZoom, transitionType);
        float currentFov = zoomrgy$lerp(originalFov, targetFov, t);

        double multiplier = (double) originalFov / currentFov;
        
        // Calculate compass direction
        float yaw = mc.player.getYRot();
        float degrees = (yaw % 360.0f + 360.0f) % 360.0f;
        String dir = "N";
        if (degrees >= 22.5f && degrees < 67.5f) dir = "NE";
        else if (degrees >= 67.5f && degrees < 112.5f) dir = "E";
        else if (degrees >= 112.5f && degrees < 157.5f) dir = "SE";
        else if (degrees >= 157.5f && degrees < 202.5f) dir = "S";
        else if (degrees >= 202.5f && degrees < 247.5f) dir = "SW";
        else if (degrees >= 247.5f && degrees < 292.5f) dir = "W";
        else if (degrees >= 292.5f && degrees < 337.5f) dir = "NW";
        
        String compassString = String.format(java.util.Locale.US, "%.0f° %s", degrees, dir);
        String label = String.format(java.util.Locale.US, "%.1fx  |  %s", multiplier, compassString);

        // Construct telemetry string
        String telemetry = "";
        if (cfg.showTelemetryHud) {
            net.minecraft.world.phys.HitResult hit = com.yourname.zoomrgy.ZoomHandler.customRaycast(mc, 150.0, deltaTracker.getGameTimeDeltaPartialTick(true));
            if (hit != null && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                double distance = 0.0;
                String targetName = "";
                if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                    net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
                    if (mc.level != null) {
                        targetName = mc.level.getBlockState(pos).getBlock().getName().getString();
                    }
                    if (mc.player != null) {
                        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true));
                        distance = eyePos.distanceTo(blockHit.getLocation());
                    }
                    if (!targetName.isEmpty()) {
                        telemetry = String.format(java.util.Locale.US, "⛶ %s  |  ⌖ %.1fm", targetName, distance);
                    }
                } else if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                    net.minecraft.world.entity.Entity entity = entityHit.getEntity();
                    targetName = entity.getName().getString();
                    if (mc.player != null) {
                        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true));
                        distance = eyePos.distanceTo(entityHit.getLocation());
                    }
                    if (!targetName.isEmpty()) {
                        String icon = "👤";
                        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                            icon = "☠";
                        } else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
                            icon = "❤";
                        }
                        telemetry = String.format(java.util.Locale.US, "%s %s  |  ⌖ %.1fm", icon, targetName, distance);
                    }
                }
            }
        }

        net.minecraft.client.gui.Font font = mc.font;
        int textWidth = font.width(label);
        int textHeight = font.lineHeight;

        int telemetryWidth = font.width(telemetry);
        int telemetryHeight = font.lineHeight;

        // Slide up offset based on zoom progress
        int slideOffset = (int) ((1.0 - renderZoom) * 10.0);

        int x = (width - textWidth) / 2;
        int y = height - 60 - textHeight + slideOffset;

        int telemetryX = (width - telemetryWidth) / 2;
        int telemetryY = y + textHeight + 4;

        int bgLeft = Math.min(x, telemetryX) - 6;
        int bgTop = y - 4;
        int bgRight = Math.max(x + textWidth, telemetryX + telemetryWidth) + 6;
        int bgBottom = (telemetry.isEmpty() ? y + textHeight : telemetryY + telemetryHeight) + 4;

        // Eased opacity/fade-in
        int alphaVal = (int) (renderZoom * 255) & 0xFF;
        int color = (cfg.zoomHudColor & 0xFFFFFF) | (alphaVal << 24);

        if (cfg.zoomHudBackground) {
            int bgAlpha = (int) (renderZoom * 0x60) & 0xFF;
            int bgColor = (bgAlpha << 24);
            int borderColor = (bgAlpha << 24) | 0x808080;

            // Draw filled background box
            extractor.fill(bgLeft, bgTop, bgRight, bgBottom, bgColor);
            
            // Draw glassmorphism thin border
            extractor.fill(bgLeft, bgTop, bgRight, bgTop + 1, borderColor);
            extractor.fill(bgLeft, bgBottom - 1, bgRight, bgBottom, borderColor);
            extractor.fill(bgLeft, bgTop, bgLeft + 1, bgBottom, borderColor);
            extractor.fill(bgRight - 1, bgTop, bgRight, bgBottom, borderColor);
        }

        extractor.text(font, label, x, y, color, true);
        if (!telemetry.isEmpty()) {
            extractor.text(font, telemetry, telemetryX, telemetryY, color, true);
        }
    }

    private float zoomrgy$lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private double zoomrgy$lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
