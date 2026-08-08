package com.jom3a.zoomrgy;

import java.nio.file.Path;

/**
 * The handful of things the shared code needs that every mod loader spells differently.
 *
 * <p>Deliberately tiny. Events and key registration are not here: the loader entry points call
 * into the shared code instead, which keeps this to the few values that genuinely have no common
 * API.
 */
public interface Platform {

    /** Where {@code zoomrgy.json} lives. */
    Path configDir();

    /** The Minecraft instance directory, used to find the screenshots folder. */
    Path gameDir();

    static Platform get() {
        Platform platform = Holder.current;
        if (platform == null) {
            throw new IllegalStateException(
                "Platform used before the loader entry point set it");
        }
        return platform;
    }

    static void set(Platform platform) {
        Holder.current = platform;
    }

    /** Nested classes in an interface are ordinary classes, so this can hold the field. */
    final class Holder {
        private static volatile Platform current;

        private Holder() {
        }
    }
}
