package com.yourname.zoomrgy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class ZoomConfig {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Zoomrgy.MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("zoomrgy.json");

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

                // Check if key fields were missing in the JSON to drive accurate migration
                boolean hasZoomSpeed = json != null && json.has("zoomSpeed");
                boolean hasTransitionType = json != null && json.has("transitionType");

                // Migrate split zoomInSpeed/zoomOutSpeed back to zoomSpeed
                if (!hasZoomSpeed) {
                    if (instance.zoomInSpeed != 0.1) {
                        instance.zoomSpeed = instance.zoomInSpeed;
                    } else {
                        instance.zoomSpeed = 0.1;
                    }
                }
                // Migrate split transitionType back to single transitionType
                if (!hasTransitionType) {
                    if (instance.zoomInTransition != null) {
                        instance.transitionType = instance.zoomInTransition;
                    } else {
                        instance.transitionType = ZoomTransition.Type.SMOOTHSTEP;
                    }
                }
                // Migrate boolean cinematicCamera to slider cinematicSmoothness
                if (instance.cinematicCamera) {
                    if (instance.cinematicSmoothness == 0.0) {
                        double derived = 1.0 - instance.cinematicCameraMultiplier * 0.5;
                        instance.cinematicSmoothness = Math.max(0.1, Math.min(0.95, derived));
                    }
                    instance.cinematicCamera = false; // Reset to avoid re-migration
                }
                if (instance.cinematicCameraMultiplier <= 0.05) {
                    instance.cinematicCameraMultiplier = 1.0;
                }
                // Migrate FOV values to Multipliers
                if (instance.zoomedFov != 15.0 && instance.zoomMultiplier == 4.5) {
                    instance.zoomMultiplier = Math.round((70.0 / instance.zoomedFov) * 10.0) / 10.0;
                }
                if (instance.zoomedFovPreset2 != 5.0 && instance.zoomMultiplierPreset2 == 14.0) {
                    instance.zoomMultiplierPreset2 = Math.round((70.0 / instance.zoomedFovPreset2) * 10.0) / 10.0;
                }
                if (instance.spyglassZoomFov != 7.0 && instance.spyglassZoomMultiplier == 10.0) {
                    instance.spyglassZoomMultiplier = Math.round((70.0 / instance.spyglassZoomFov) * 10.0) / 10.0;
                }
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

        c.zoomSpeed             = sane(c.zoomSpeed,             0.05, 1.00, DEFAULTS.zoomSpeed);
        c.zoomMultiplier        = sane(c.zoomMultiplier,        1.5,  20.0, DEFAULTS.zoomMultiplier);
        c.zoomMultiplierPreset2 = sane(c.zoomMultiplierPreset2, 2.0,  50.0, DEFAULTS.zoomMultiplierPreset2);
        c.spyglassZoomMultiplier= sane(c.spyglassZoomMultiplier,2.0,  35.0, DEFAULTS.spyglassZoomMultiplier);
        c.cinematicSmoothness   = sane(c.cinematicSmoothness,   0.0,  0.95, DEFAULTS.cinematicSmoothness);
        c.movementFovDamping    = sane(c.movementFovDamping,    0.0,  1.00, DEFAULTS.movementFovDamping);
        c.zoomVignetteOpacity   = sane(c.zoomVignetteOpacity,   0.0,  1.00, DEFAULTS.zoomVignetteOpacity);

        c.maxScrollLevel = Math.max(1, Math.min(20, c.maxScrollLevel));
        c.zoomHudColor  &= 0xFFFFFF;
    }

    private static double sane(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static class Config {
        @Deprecated
        public double  zoomedFov             = 15.0;
        public double  zoomSpeed             = 0.1;
        public ZoomTransition.Type transitionType = ZoomTransition.Type.EASE_IN_OUT_SINE;

        @Deprecated
        public double  zoomInSpeed           = 0.1;
        @Deprecated
        public double  zoomOutSpeed          = 0.1;
        @Deprecated
        public ZoomTransition.Type zoomInTransition  = ZoomTransition.Type.SMOOTHSTEP;
        @Deprecated
        public ZoomTransition.Type zoomOutTransition = ZoomTransition.Type.SMOOTHSTEP;
        public int     maxScrollLevel        = 10;
        public boolean resetScrollOnRelease  = true;
        public boolean affectHandFov         = true;
        
        @Deprecated
        public boolean cinematicCamera       = false;
        @Deprecated
        public double  cinematicCameraMultiplier = 1.0;
        
        public double  cinematicSmoothness   = 0.0; // 0.0 = OFF, otherwise smoothness factor (e.g. 0.05 to 0.95)
        public boolean reduceSensitivity     = true;
        public boolean zoomToggleMode        = false;
        public boolean doubleTapToLock       = true;
        public boolean showZoomHud           = true;
        public boolean zoomHudBackground     = true;
        public int     zoomHudColor          = 0xFFFFFF;
        public boolean reduceFog             = true;
        public double  zoomVignetteOpacity   = 0.0;
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
        
        @Deprecated
        public double  zoomedFovPreset2      = 5.0;
        @Deprecated
        public double  spyglassZoomFov       = 7.0;

        public double  zoomMultiplier        = 4.5;
        public double  zoomMultiplierPreset2 = 14.0;
        public double  spyglassZoomMultiplier = 10.0;
    }
}
