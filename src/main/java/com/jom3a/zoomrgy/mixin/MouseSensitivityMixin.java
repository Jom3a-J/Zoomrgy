package com.jom3a.zoomrgy.mixin;

import com.jom3a.zoomrgy.ZoomConfig;
import com.jom3a.zoomrgy.ZoomState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public abstract class MouseSensitivityMixin {

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 1)
    private void modifyTurnArgs(Args args) {
        if (!ZoomConfig.get().reduceSensitivity) return;
        if (ZoomState.currentZoom <= 0.0) return;

        double originalX = args.get(0);
        double originalY = args.get(1);

        // Dynamically calculate the scale based on the FOV ratio
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double baseFov = mc.options.fov().get();
        double targetFov = ZoomState.getTargetFov();
        if (baseFov <= 0.0) return;
        double fovRatio = targetFov / baseFov;

        // Follow the same easing curve the FOV itself uses. Scaling by the raw linear ramp
        // instead would desync sensitivity from what you actually see mid-transition - badly
        // so for the exponential and elastic curves.
        double eased = com.jom3a.zoomrgy.ZoomTransition.apply(
            ZoomState.currentZoom, ZoomConfig.get().transitionType);

        double scale = Math.max(0.01, 1.0 + (fovRatio - 1.0) * eased);

        args.set(0, originalX * scale);
        args.set(1, originalY * scale);
    }

    @Redirect(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"), require = 1)
    private boolean redirectSmoothCamera(net.minecraft.client.Options options) {
        if (ZoomState.currentZoom > 0.0 && ZoomConfig.get().cinematicSmoothness > 0.0) {
            return true;
        }
        return options.smoothCamera;
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SmoothDouble;getNewDeltaValue(DD)D"),
        index = 1,
        require = 1
    )
    private double modifySmoothWeight(double originalWeight) {
        if (ZoomState.currentZoom > 0.0 && ZoomConfig.get().cinematicSmoothness > 0.0) {
            return originalWeight * (1.0 - ZoomConfig.get().cinematicSmoothness);
        }
        return originalWeight;
    }
}
