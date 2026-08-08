package com.jom3a.zoomrgy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The still image the easing preview zooms into.
 *
 * <p>A fixed picture rather than a fresh grab each time the screen opens, so the preview looks the
 * same from one visit to the next and the only thing moving is the easing being previewed.
 *
 * <p>The file is captured from the game once, the first time it is needed, and kept at
 * {@code config/zoomrgy-preview.png}. Delete that file to have a new one taken, or drop in any
 * screenshot of your own to use it instead.
 */
public final class ZoomPreviewImage {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Zoomrgy.MOD_ID);

    private static final Path IMAGE_PATH =
        Platform.get().configDir().resolve("zoomrgy-preview.png");

    private static final Identifier TEXTURE_ID =
        Identifier.fromNamespaceAndPath(Zoomrgy.MOD_ID, "config_preview");

    private static DynamicTexture texture;
    private static int width;
    private static int height;

    /** Set once a capture is in flight, so opening the screen repeatedly does not stack requests. */
    private static boolean capturePending;

    private ZoomPreviewImage() {
    }

    /** Set once we have tried to grab a picture this session, successfully or not. */
    private static boolean captureAttempted;

    private static Path loadedFrom;
    private static long loadedStamp;

    /**
     * Makes a picture available to the config screen, preferring your most recent screenshot so
     * pressing F2 on a view you like is enough to make it the preview.
     *
     * <p>Only ever reads from disk. Taking a picture here would capture the menu already on
     * screen, since the GUI is drawn into the same render target as the world.
     */
    public static void ensureAvailable() {
        if (capturePending) return;

        Path source = newestScreenshot();
        if (source == null && Files.isRegularFile(IMAGE_PATH)) {
            source = IMAGE_PATH;
        }
        if (source == null) return;

        long stamp = lastModified(source);
        if (texture != null && source.equals(loadedFrom) && stamp == loadedStamp) {
            return; // Already showing this exact file.
        }

        if (loadFrom(source)) {
            loadedFrom = source;
            loadedStamp = stamp;
        }
    }

    /** The newest image in the screenshots folder, or null when there are none. */
    private static Path newestScreenshot() {
        Path dir = Platform.get().gameDir().resolve("screenshots");
        if (!Files.isDirectory(dir)) return null;

        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                .max(java.util.Comparator.comparingLong(ZoomPreviewImage::lastModified))
                .orElse(null);
        } catch (Exception e) {
            LOGGER.warn("Could not read the screenshots folder", e);
            return null;
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Takes the picture, if there is not one already. Must be called while in a world with no
     * screen open, otherwise the grab catches whatever menu is being displayed.
     */
    public static void captureIfMissing() {
        if (captureAttempted || capturePending || texture != null) return;
        captureAttempted = true;

        if (Files.isRegularFile(IMAGE_PATH)) return;

        capture();
    }

    private static boolean loadFrom(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            install(NativeImage.read(in));
            return texture != null;
        } catch (Exception e) {
            LOGGER.warn("Could not read the preview image at {}", path, e);
            return false;
        }
    }

    /** Grabs the frame on screen, stores it for next time, and uses it now. */
    private static void capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;

        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) return;

        capturePending = true;
        try {
            // The readback completes asynchronously, so finish on the client thread.
            Screenshot.takeScreenshot(target, image -> mc.execute(() -> {
                writeToDisk(image);
                install(image);
                capturePending = false;
            }));
        } catch (Exception e) {
            LOGGER.warn("Could not capture a preview image", e);
            capturePending = false;
        }
    }

    private static void writeToDisk(NativeImage image) {
        try {
            Files.createDirectories(IMAGE_PATH.getParent());
            image.writeToFile(IMAGE_PATH.toFile());
        } catch (Exception e) {
            // Not fatal: the image still works for this session, it just is not kept.
            LOGGER.warn("Could not store the preview image at {}", IMAGE_PATH, e);
        }
    }

    private static void install(NativeImage image) {
        try {
            release();
            width = image.getWidth();
            height = image.getHeight();
            texture = new DynamicTexture(() -> "zoomrgy_config_preview", image);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
        } catch (Exception e) {
            LOGGER.warn("Could not install the preview image", e);
            image.close();
            texture = null;
        }
    }

    /** Drops the loaded image. The preview falls back to drawn shapes until one is available. */
    public static void release() {
        if (texture == null) return;
        try {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
        } catch (Exception e) {
            LOGGER.warn("Could not release the preview image", e);
        }
        texture = null;
    }

    public static boolean isReady() {
        return texture != null && width > 0 && height > 0;
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }
}
