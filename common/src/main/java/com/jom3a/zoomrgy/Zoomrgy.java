package com.jom3a.zoomrgy;

/** Shared start-up. Each loader's entry point calls {@link #init()} once the platform is set. */
public final class Zoomrgy {

    public static final String MOD_ID = "zoomrgy";

    private Zoomrgy() {
    }

    public static void init() {
        ZoomConfig.load();
        ZoomKeyBindings.createKeyMappings();
    }
}
