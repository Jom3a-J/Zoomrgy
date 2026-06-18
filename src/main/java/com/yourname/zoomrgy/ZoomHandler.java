package com.yourname.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class ZoomHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            tickZoom();
        });
    }

    private static void tickZoom() {
        ZoomState.lastZoom = ZoomState.currentZoom;

        double target = ZoomState.getTargetZoom();
        double current = ZoomState.currentZoom;
        double speed = ZoomConfig.get().zoomSpeed;

        if (current < target) {
            ZoomState.currentZoom = Math.min(target, current + speed);
        } else if (current > target) {
            ZoomState.currentZoom = Math.max(target, current - speed);
        }
    }

    private static net.minecraft.world.phys.Vec3 lastStartPos = null;
    private static net.minecraft.world.phys.Vec3 lastLookVec = null;
    private static double lastMaxDistance = 0.0;
    private static net.minecraft.world.phys.HitResult cachedHitResult = null;

    public static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }

    public static net.minecraft.world.phys.HitResult customRaycast(net.minecraft.client.Minecraft client, double maxDistance, float partialTicks) {
        if (client.player == null || client.level == null) return null;

        net.minecraft.world.phys.Vec3 startPos = client.player.getEyePosition(partialTicks);
        net.minecraft.world.phys.Vec3 lookVec = client.player.getViewVector(partialTicks);

        // Check if values match cached run
        if (cachedHitResult != null 
            && maxDistance == lastMaxDistance 
            && startPos.equals(lastStartPos) 
            && lookVec.equals(lastLookVec)) {
            return cachedHitResult;
        }

        net.minecraft.world.phys.Vec3 endPos = startPos.add(
            lookVec.x * maxDistance,
            lookVec.y * maxDistance,
            lookVec.z * maxDistance
        );

        // Raycast blocks
        net.minecraft.world.level.ClipContext clipContext = new net.minecraft.world.level.ClipContext(
            startPos,
            endPos,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            client.player
        );
        net.minecraft.world.phys.BlockHitResult blockHitResult = client.level.clip(clipContext);

        double blockDistanceSq = endPos.distanceToSqr(startPos);
        if (blockHitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            blockDistanceSq = blockHitResult.getLocation().distanceToSqr(startPos);
        }

        // Raycast entities
        net.minecraft.world.phys.AABB boundingBox = client.player.getBoundingBox()
            .expandTowards(lookVec.scale(maxDistance))
            .inflate(1.0, 1.0, 1.0);

        net.minecraft.world.phys.EntityHitResult entityHitResult = null;
        double closestEntityDistanceSq = blockDistanceSq;

        for (net.minecraft.world.entity.Entity entity : client.level.getEntities(client.player, boundingBox, e -> e != null && e.isPickable())) {
            net.minecraft.world.phys.AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            java.util.Optional<net.minecraft.world.phys.Vec3> clipResult = entityBox.clip(startPos, endPos);

            if (entityBox.contains(startPos)) {
                if (closestEntityDistanceSq >= 0.0) {
                    entityHitResult = new net.minecraft.world.phys.EntityHitResult(entity, startPos);
                    closestEntityDistanceSq = 0.0;
                }
            } else if (clipResult.isPresent()) {
                net.minecraft.world.phys.Vec3 hitVec = clipResult.get();
                double distSq = startPos.distanceToSqr(hitVec);
                if (distSq < closestEntityDistanceSq) {
                    entityHitResult = new net.minecraft.world.phys.EntityHitResult(entity, hitVec);
                    closestEntityDistanceSq = distSq;
                }
            }
        }

        net.minecraft.world.phys.HitResult result = entityHitResult != null ? entityHitResult : blockHitResult;
        
        // Cache this run
        lastStartPos = startPos;
        lastLookVec = lookVec;
        lastMaxDistance = maxDistance;
        cachedHitResult = result;

        return result;
    }
}
