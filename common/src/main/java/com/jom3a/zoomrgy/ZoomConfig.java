package com.jom3a.zoomrgy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Path;

public class ZoomConfig {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Zoomrgy.MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        Platform.get().configDir().resolve("zoomrgy.json");

    /** Pristine defaults, used by the config screen so "reset to default" matches a fresh install. */
    private static final Config DEFAULTS = new Config();

    private static Config instance = new Config();

    public static Config get() { return instance; }

    /** Read-only view of the built-in defaults. Never mutate the returned instance. */
    public static Config defaults() { return DEFAULTS; }

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                com.google.gson.JsonObject json = GSON.fromJson(reader, com.google.gson.JsonObject.class);
                instance = GSON.fromJson(json, Config.class);
                if (instance == null) {
                    instance = new Config();
                }

                migrate(json);
            } catch (Exception e) {
                // Malformed JSON throws JsonSyntaxException (unchecked) - letting that escape
                // onInitializeClient would hard-crash the game on startup. Fall back to defaults
                // and leave the broken file alone so the user can still repair it by hand.
                LOGGER.error("Failed to read {}, falling back to default settings", CONFIG_PATH, e);
                instance = new Config();
            }
        }
        sanitize();
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to write {}", CONFIG_PATH, e);
        }
    }

    /**
     * Upgrades settings written by older versions.
     *
     * <p>Legacy values are read straight out of the parsed JSON rather than from fields on
     * {@link Config}. Keeping them as fields meant every save wrote nine dead keys back out,
     * which made a hand-edited config confusing to read. Gson simply ignores the old keys now,
     * so they disappear the first time the config is saved.
     */
    private static void migrate(com.google.gson.JsonObject json) {
        if (json == null) return;

        // zoomInSpeed/zoomOutSpeed were once separate; the single zoomSpeed replaced them.
        if (!json.has("zoomSpeed")) {
            Double legacySpeed = readDouble(json, "zoomInSpeed");
            if (legacySpeed != null) instance.zoomSpeed = legacySpeed;
        }

        // Likewise for the split zoomInTransition/zoomOutTransition.
        if (!json.has("transitionType")) {
            String legacyTransition = readString(json, "zoomInTransition");
            if (legacyTransition != null) {
                try {
                    instance.transitionType = ZoomTransition.Type.valueOf(legacyTransition);
                } catch (IllegalArgumentException ignored) {
                    // Unknown name; sanitize() will fall back to the default.
                }
            }
        }

        // Separate in/out curves and speeds are back. A config from the single-value era should
        // keep behaving identically, so the outward setting inherits the inward one - unless the
        // file is old enough to still carry the original zoomOut* pair, which maps directly.
        if (!json.has("transitionTypeOut")) {
            String legacyOut = readString(json, "zoomOutTransition");
            ZoomTransition.Type resolved = null;
            if (legacyOut != null) {
                try {
                    resolved = ZoomTransition.Type.valueOf(legacyOut);
                } catch (IllegalArgumentException ignored) {
                    // Unknown name; fall through to inheriting the inward curve.
                }
            }
            instance.transitionTypeOut = resolved != null ? resolved : instance.transitionType;
        }
        if (!json.has("zoomSpeedOut")) {
            Double legacyOutSpeed = readDouble(json, "zoomOutSpeed");
            instance.zoomSpeedOut = legacyOutSpeed != null ? legacyOutSpeed : instance.zoomSpeed;
        }

        // The cinematic camera toggle plus its multiplier became one smoothness slider.
        if (!json.has("cinematicSmoothness") && Boolean.TRUE.equals(readBoolean(json, "cinematicCamera"))) {
            Double multiplier = readDouble(json, "cinematicCameraMultiplier");
            double derived = 1.0 - (multiplier == null ? 1.0 : multiplier) * 0.5;
            instance.cinematicSmoothness = Math.max(0.1, Math.min(0.95, derived));
        }

        // Absolute zoomed FOV values became magnification multipliers relative to a 70 FOV.
        migrateFovToMultiplier(json, "zoomedFov", "zoomMultiplier", v -> instance.zoomMultiplier = v);
        migrateFovToMultiplier(json, "zoomedFovPreset2", "zoomMultiplierPreset2", v -> instance.zoomMultiplierPreset2 = v);
        migrateFovToMultiplier(json, "spyglassZoomFov", "spyglassZoomMultiplier", v -> instance.spyglassZoomMultiplier = v);

        // hudOffsetY used to be a raw screen delta, so its default was -60. It is now an inset
        // measured inwards from the anchored edge; leaving the old value alone would push a
        // bottom-anchored HUD straight off the bottom of the screen.
        if (json.has("hudOffsetY") && instance.hudOffsetY == -60) {
            instance.hudOffsetY = 60;
        }
    }

    private static void migrateFovToMultiplier(com.google.gson.JsonObject json, String legacyKey,
                                               String currentKey, java.util.function.DoubleConsumer setter) {
        if (json.has(currentKey)) return; // Already on the new scheme.

        Double fov = readDouble(json, legacyKey);
        if (fov == null || fov <= 0.0) return;

        setter.accept(Math.round((70.0 / fov) * 10.0) / 10.0);
    }

    private static Double readDouble(com.google.gson.JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean readBoolean(com.google.gson.JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readString(com.google.gson.JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Forces every value into the range the config screen exposes. Guards against
     * hand-edited files and against the FOV migrations above producing nonsense
     * (a zoomed FOV of 0 in an old config yields an infinite multiplier), either of
     * which would divide the rendered FOV down to zero, NaN or infinity.
     */
    public static void sanitize() {
        Config c = instance;

        // Gson deserialises an unrecognised enum name to null rather than failing.
        c.transitionType = c.transitionType == null
            ? DEFAULTS.transitionType
            : ZoomTransition.normalize(c.transitionType);

        c.transitionTypeOut = c.transitionTypeOut == null
            ? DEFAULTS.transitionTypeOut
            : ZoomTransition.normalize(c.transitionTypeOut);

        c.zoomSpeed             = sane(c.zoomSpeed,             0.05, 1.00, DEFAULTS.zoomSpeed);
        c.zoomSpeedOut          = sane(c.zoomSpeedOut,          0.05, 1.00, DEFAULTS.zoomSpeedOut);
        c.zoomMultiplier        = sane(c.zoomMultiplier,        1.5,  20.0, DEFAULTS.zoomMultiplier);
        c.zoomMultiplierPreset2 = sane(c.zoomMultiplierPreset2, 2.0,  50.0, DEFAULTS.zoomMultiplierPreset2);
        c.spyglassZoomMultiplier= sane(c.spyglassZoomMultiplier,2.0,  35.0, DEFAULTS.spyglassZoomMultiplier);
        c.cinematicSmoothness   = sane(c.cinematicSmoothness,   0.0,  0.95, DEFAULTS.cinematicSmoothness);
        c.movementFovDamping    = sane(c.movementFovDamping,    0.0,  1.00, DEFAULTS.movementFovDamping);
        c.zoomVignetteOpacity   = sane(c.zoomVignetteOpacity,   0.0,  1.00, DEFAULTS.zoomVignetteOpacity);

        c.scrollStepRatio = sane(c.scrollStepRatio, 1.05, 2.00, DEFAULTS.scrollStepRatio);

        if (c.hudAnchor == null) c.hudAnchor = DEFAULTS.hudAnchor;
        c.hudOffsetX = Math.max(-2000, Math.min(2000, c.hudOffsetX));
        c.hudOffsetY = Math.max(-2000, Math.min(2000, c.hudOffsetY));
        c.maxScrollLevel = Math.max(1, Math.min(20, c.maxScrollLevel));
        c.zoomHudColor  &= 0xFFFFFF;
    }

    private static double sane(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static class Config {
        /** Speed and curve for zooming in. */
        public double  zoomSpeed             = 0.1;
        public ZoomTransition.Type transitionType = ZoomTransition.Type.SMOOTHSTEP;

        /** Speed and curve for zooming back out, which need not match the way in. */
        public double  zoomSpeedOut          = 0.1;
        public ZoomTransition.Type transitionTypeOut = ZoomTransition.Type.SMOOTHSTEP;

        public int     maxScrollLevel        = 10;
        /** Proportional size of one scroll notch. 1.3^9 gives roughly the old range at level 10. */
        public double  scrollStepRatio       = 1.3;
        public boolean resetScrollOnRelease  = true;
        public boolean affectHandFov         = true;

        /** 0.0 = OFF, otherwise a smoothness factor between roughly 0.05 and 0.95. */
        public double  cinematicSmoothness   = 0.0;
        public boolean reduceSensitivity     = true;
        public boolean zoomToggleMode        = false;
        public boolean doubleTapToLock       = true;
        public boolean showZoomHud           = true;
        public boolean zoomHudBackground     = true;
        public int     zoomHudColor          = 0xFFFFFF;
        public HudAnchor hudAnchor           = HudAnchor.BOTTOM_CENTER;
        /** Insets measured inwards from the anchored edge, so one default suits every anchor. */
        public int     hudOffsetX            = 0;
        public int     hudOffsetY            = 60;
        public boolean reduceFog             = true;
        public double  zoomVignetteOpacity   = 0.4;
        public boolean hideCrosshair         = false;
        public boolean hideHotbar            = false;
        public boolean zoomOutOnDamage       = true;
        public boolean spyglassScopeOverlay   = false;
        public boolean scrollAudioFeedback   = true;

        public boolean spyglassAutoZoom      = true;
        public boolean showVanillaSpyglassOverlay = false;
        public boolean highlightTargetEntity = true;
        public boolean showTelemetryHud      = true;
        public double  movementFovDamping    = 0.8;

        public double  zoomMultiplier        = 4.5;
        public double  zoomMultiplierPreset2 = 14.0;
        public double  spyglassZoomMultiplier = 10.0;
    }
}
