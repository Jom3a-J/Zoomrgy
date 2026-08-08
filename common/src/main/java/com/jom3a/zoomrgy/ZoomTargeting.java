package com.jom3a.zoomrgy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Finds whatever the player is looking at, for the rangefinder HUD and the target highlight.
 *
 * <p>This runs on the render thread, so it is deliberately frugal: the result is cached for the
 * duration of a frame, the ray length follows the current magnification rather than always
 * reaching for the horizon, and the entity sweep stops at the first solid block.
 */
public final class ZoomTargeting {

    /** Shortest ray we bother casting, in blocks. */
    private static final double MIN_RANGE = 32.0;
    /** Longest ray we will cast no matter how far the player is zoomed in. */
    private static final double MAX_RANGE = 256.0;

    private static Vec3 cachedStart;
    private static Vec3 cachedLook;
    private static double cachedRange;
    private static HitResult cachedResult;

    private ZoomTargeting() {
    }

    /** Drops the cached hit so it cannot keep a stale entity, and through it a level, alive. */
    public static void clearCache() {
        cachedStart = null;
        cachedLook = null;
        cachedRange = 0.0;
        cachedResult = null;
    }

    /**
     * How far to look for a target at the given magnification. At 1x there is no point scanning
     * far past normal view distance; the range only opens up as the zoom does.
     */
    public static double rangeFor(double magnification) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, MIN_RANGE * magnification));
    }

    /**
     * Returns the block or entity under the crosshair, or null when there is no level. The entity
     * takes precedence when it is closer than the block.
     */
    public static HitResult raycast(Minecraft client, double range, float partialTicks) {
        if (client.player == null || client.level == null) {
            clearCache();
            return null;
        }

        Vec3 start = client.player.getEyePosition(partialTicks);
        Vec3 look = client.player.getViewVector(partialTicks);

        if (cachedResult != null
            && range == cachedRange
            && start.equals(cachedStart)
            && look.equals(cachedLook)) {
            return cachedResult;
        }

        Vec3 end = start.add(look.x * range, look.y * range, look.z * range);

        BlockHitResult blockHit = client.level.clip(new ClipContext(
            start,
            end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            client.player
        ));

        // Anything past the first solid block is hidden, so the entity sweep only has to cover
        // the part of the ray that is actually visible. Looking at a wall a few blocks away turns
        // this from a level-wide query into a tiny one.
        Vec3 searchEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : end;
        double blockDistanceSq = start.distanceToSqr(searchEnd);

        EntityHitResult entityHit = null;
        double closestSq = blockDistanceSq;

        AABB searchBox = client.player.getBoundingBox()
            .expandTowards(searchEnd.subtract(start))
            .inflate(1.0, 1.0, 1.0);

        for (Entity entity : client.level.getEntities(client.player, searchBox, e -> e != null && e.isPickable())) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());

            if (entityBox.contains(start)) {
                entityHit = new EntityHitResult(entity, start);
                closestSq = 0.0;
                break; // Nothing can be nearer than an entity we are standing inside.
            }

            Optional<Vec3> clip = entityBox.clip(start, searchEnd);
            if (clip.isPresent()) {
                double distSq = start.distanceToSqr(clip.get());
                if (distSq < closestSq) {
                    entityHit = new EntityHitResult(entity, clip.get());
                    closestSq = distSq;
                }
            }
        }

        HitResult result = entityHit != null ? entityHit : blockHit;

        cachedStart = start;
        cachedLook = look;
        cachedRange = range;
        cachedResult = result;

        return result;
    }
}
