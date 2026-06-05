package com.yourname.zoomrgy.mixin;

import com.yourname.zoomrgy.ZoomConfig;
import com.yourname.zoomrgy.ZoomState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true, require = 1)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue()) return; // Already glowing

        ZoomConfig.Config cfg = ZoomConfig.get();
        if (cfg.highlightTargetEntity && (ZoomState.isZooming || ZoomState.isZoomingPreset2 || ZoomState.isZoomLocked || ZoomState.isSpyglassActive)) {
            Entity thisEntity = (Entity) (Object) this;
            if (thisEntity == ZoomState.targetedEntity) {
                info.setReturnValue(true);
            }
        }
    }
}
