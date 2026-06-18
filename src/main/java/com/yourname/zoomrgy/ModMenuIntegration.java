package com.yourname.zoomrgy;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ZoomConfig.Config cfg = ZoomConfig.get();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Zoomrgy Configuration"));

            ConfigEntryBuilder entry = builder.entryBuilder();

            // Category 1: Controls & Behavior
            ConfigCategory controls = builder.getOrCreateCategory(Component.literal("Controls & Behavior"));

            // Core Zoom Settings
            controls.addEntry(entry
                .startIntSlider(Component.literal("Base Zoom Level"), (int) (cfg.zoomMultiplier * 10), 15, 200)
                .setDefaultValue(45)
                .setTextGetter(val -> Component.literal(String.format(java.util.Locale.US, "%.1fx", val / 10.0)))
                .setTooltip(Component.literal("Magnification multiplier for the primary zoom key (default: C)."))
                .setSaveConsumer(val -> cfg.zoomMultiplier = val / 10.0)
                .build());

            controls.addEntry(entry
                .startIntSlider(Component.literal("Preset 2 Zoom Level"), (int) (cfg.zoomMultiplierPreset2 * 10), 20, 500)
                .setDefaultValue(140)
                .setTextGetter(val -> Component.literal(String.format(java.util.Locale.US, "%.1fx", val / 10.0)))
                .setTooltip(Component.literal("Magnification multiplier for the secondary Preset 2 zoom key (default: V)."))
                .setSaveConsumer(val -> cfg.zoomMultiplierPreset2 = val / 10.0)
                .build());

            controls.addEntry(entry
                .startIntSlider(Component.literal("Max Scroll Level"), cfg.maxScrollLevel, 1, 20)
                .setDefaultValue(10)
                .setTooltip(Component.literal("Maximum zoom steps available via scroll wheel."))
                .setSaveConsumer(val -> cfg.maxScrollLevel = val)
                .build());

            controls.addEntry(entry
                .startBooleanToggle(Component.literal("Reset Scroll on Release"), cfg.resetScrollOnRelease)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Return to base zoom level when the zoom is deactivated."))
                .setSaveConsumer(val -> cfg.resetScrollOnRelease = val)
                .build());

            // Subcategory: Zoom Lock & Toggles
            SubCategoryBuilder toggleGroup = entry.startSubCategory(Component.literal("Zoom Lock & Toggles"));
            toggleGroup.add(entry
                .startBooleanToggle(Component.literal("Zoom Toggle Mode"), cfg.zoomToggleMode)
                .setDefaultValue(false)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Toggle zoom state on key press instead of needing to hold it down."))
                .setSaveConsumer(val -> cfg.zoomToggleMode = val)
                .build());

            toggleGroup.add(entry
                .startBooleanToggle(Component.literal("Double Tap to Lock"), cfg.doubleTapToLock)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Double tap the zoom key in hold mode to lock the zoom state."))
                .setSaveConsumer(val -> cfg.doubleTapToLock = val)
                .build());

            toggleGroup.add(entry
                .startBooleanToggle(Component.literal("Auto Zoom-Out on Damage"), cfg.zoomOutOnDamage)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Automatically disable zoom when taking damage to prevent disorientation."))
                .setSaveConsumer(val -> cfg.zoomOutOnDamage = val)
                .build());

            toggleGroup.add(entry
                .startBooleanToggle(Component.literal("Scroll Audio Feedback"), cfg.scrollAudioFeedback)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Play a subtle click sound when scrolling to change zoom levels."))
                .setSaveConsumer(val -> cfg.scrollAudioFeedback = val)
                .build());
            controls.addEntry(toggleGroup.build());

            // Subcategory: Spyglass Override Settings
            SubCategoryBuilder spyglassGroup = entry.startSubCategory(Component.literal("Spyglass Override Settings"));
            spyglassGroup.add(entry
                .startBooleanToggle(Component.literal("Spyglass Auto-Zoom"), cfg.spyglassAutoZoom)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Automatically trigger mod zoom transitions when holding and looking through a vanilla spyglass."))
                .setSaveConsumer(val -> cfg.spyglassAutoZoom = val)
                .build());

            spyglassGroup.add(entry
                .startIntSlider(Component.literal("Spyglass Zoom Level"), (int) (cfg.spyglassZoomMultiplier * 10), 20, 350)
                .setDefaultValue(100)
                .setTextGetter(val -> Component.literal(String.format(java.util.Locale.US, "%.1fx", val / 10.0)))
                .setTooltip(Component.literal("Target magnification when zooming via the vanilla spyglass."))
                .setSaveConsumer(val -> cfg.spyglassZoomMultiplier = val / 10.0)
                .build());

            spyglassGroup.add(entry
                .startBooleanToggle(Component.literal("Show Vanilla Spyglass Overlay"), cfg.showVanillaSpyglassOverlay)
                .setDefaultValue(false)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Whether to render Minecraft's default black square spyglass vignette border."))
                .setSaveConsumer(val -> cfg.showVanillaSpyglassOverlay = val)
                .build());
            controls.addEntry(spyglassGroup.build());


            // Category 2: Easing & Transitions
            ConfigCategory transitions = builder.getOrCreateCategory(Component.literal("Easing & Transitions"));

            transitions.addEntry(entry
                .startIntSlider(Component.literal("Zoom Speed"), (int) (cfg.zoomSpeed * 100), 5, 100)
                .setDefaultValue(10)
                .setTextGetter(val -> Component.literal(val + "%"))
                .setTooltip(Component.literal("How fast the zoom transitions. 5% = ultra-slow, 100% = instant snap."))
                .setSaveConsumer(val -> cfg.zoomSpeed = val / 100.0)
                .build());

            transitions.addEntry(entry
                .startSelector(
                    Component.literal("Transition Type"),
                    ZoomTransition.getSelectableTypes(),
                    cfg.transitionType
                )
                .setDefaultValue(ZoomTransition.Type.SMOOTHSTEP)
                .setNameProvider(val -> Component.literal(val.getDisplayName()))
                .setTooltip(Component.literal("Easing curve type for the zoom transition."))
                .setSaveConsumer(val -> cfg.transitionType = val)
                .build());

            transitions.addEntry(entry
                .startIntSlider(Component.literal("Dynamic Movement Damping"), (int) (cfg.movementFovDamping * 100), 0, 100)
                .setDefaultValue(80)
                .setTextGetter(val -> Component.literal(val + "%"))
                .setTooltip(Component.literal("Scales down movement FOV adjustments (like sprinting or flying) while zooming to avoid disorienting stutters. 100% = complete motion stabilization."))
                .setSaveConsumer(val -> cfg.movementFovDamping = val / 100.0)
                .build());


            // Category 3: Mouse & Sensitivity
            ConfigCategory mouse = builder.getOrCreateCategory(Component.literal("Mouse & Sensitivity"));

            mouse.addEntry(entry
                .startIntSlider(Component.literal("Cinematic Zoom Smoothness"), (int) (cfg.cinematicSmoothness * 100), 0, 95)
                .setDefaultValue(0)
                .setTextGetter(val -> Component.literal(val == 0 ? "OFF" : val + "%"))
                .setTooltip(Component.literal("Smooth camera movements during zoom. OFF = Disabled, 10% = Low inertia, 95% = High inertia."))
                .setSaveConsumer(val -> cfg.cinematicSmoothness = val / 100.0)
                .build());

            mouse.addEntry(entry
                .startBooleanToggle(Component.literal("Reduce Sensitivity"), cfg.reduceSensitivity)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Scale mouse sensitivity down proportionally with the zoom level."))
                .setSaveConsumer(val -> cfg.reduceSensitivity = val)
                .build());

            mouse.addEntry(entry
                .startBooleanToggle(Component.literal("Affect Hand FOV"), cfg.affectHandFov)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Whether the first-person hand and held item are affected by the zoom FOV."))
                .setSaveConsumer(val -> cfg.affectHandFov = val)
                .build());


            // Category 4: Visuals & Overlay
            ConfigCategory visuals = builder.getOrCreateCategory(Component.literal("Visuals & Overlay"));

            // Subcategory: HUD Overlay Settings
            SubCategoryBuilder hudGroup = entry.startSubCategory(Component.literal("HUD Overlay Settings"));
            hudGroup.add(entry
                .startBooleanToggle(Component.literal("Show Zoom HUD Overlay"), cfg.showZoomHud)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Display a clean zoom level multiplier text overlay when active."))
                .setSaveConsumer(val -> cfg.showZoomHud = val)
                .build());

            hudGroup.add(entry
                .startBooleanToggle(Component.literal("Zoom HUD Background"), cfg.zoomHudBackground)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Show a semi-transparent dark background box with a styled border behind the HUD text."))
                .setSaveConsumer(val -> cfg.zoomHudBackground = val)
                .build());

            hudGroup.add(entry
                .startBooleanToggle(Component.literal("Show Telemetry HUD"), cfg.showTelemetryHud)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Display rangefinder distance and localized name of the targeted block/entity under the crosshair."))
                .setSaveConsumer(val -> cfg.showTelemetryHud = val)
                .build());

            hudGroup.add(entry
                .startColorField(Component.literal("Zoom HUD Text Color"), cfg.zoomHudColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Component.literal("Color of the text displayed in the zoom HUD overlay."))
                .setSaveConsumer(val -> cfg.zoomHudColor = val)
                .build());
            visuals.addEntry(hudGroup.build());

            // Subcategory: Zoom Screen Effects
            SubCategoryBuilder effectsGroup = entry.startSubCategory(Component.literal("Zoom Screen Effects"));
            effectsGroup.add(entry
                .startBooleanToggle(Component.literal("Highlight Targeted Entity"), cfg.highlightTargetEntity)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Highlight the targeted entity under your crosshair with a client-side glowing outline effect."))
                .setSaveConsumer(val -> cfg.highlightTargetEntity = val)
                .build());

            effectsGroup.add(entry
                .startBooleanToggle(Component.literal("Hide Hotbar during Zoom"), cfg.hideHotbar)
                .setDefaultValue(false)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Hide the hotbar and other HUD elements when zoom is active."))
                .setSaveConsumer(val -> cfg.hideHotbar = val)
                .build());

            effectsGroup.add(entry
                .startIntSlider(Component.literal("Zoom Vignette Opacity"), (int) (cfg.zoomVignetteOpacity * 100), 0, 100)
                .setDefaultValue(40)
                .setTextGetter(val -> Component.literal(val + "%"))
                .setTooltip(Component.literal("Opacity of the dark vignette border when zoomed. Set to 0% to disable."))
                .setSaveConsumer(val -> cfg.zoomVignetteOpacity = val / 100.0)
                .build());

            effectsGroup.add(entry
                .startBooleanToggle(Component.literal("Reduce Fog during Zoom"), cfg.reduceFog)
                .setDefaultValue(true)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Push fog further back when zoomed in to improve visibility of distant areas."))
                .setSaveConsumer(val -> cfg.reduceFog = val)
                .build());

            effectsGroup.add(entry
                .startBooleanToggle(Component.literal("Hide Crosshair during Zoom"), cfg.hideCrosshair)
                .setDefaultValue(false)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Hide the center crosshair cursor when zoom is active."))
                .setSaveConsumer(val -> cfg.hideCrosshair = val)
                .build());

            effectsGroup.add(entry
                .startBooleanToggle(Component.literal("Spyglass Scope Simulation"), cfg.spyglassScopeOverlay)
                .setDefaultValue(false)
                .setYesNoTextSupplier(bool -> bool ? Component.literal("ON") : Component.literal("OFF"))
                .setTooltip(Component.literal("Render the circular spyglass scope texture overlay while zooming."))
                .setSaveConsumer(val -> cfg.spyglassScopeOverlay = val)
                .build());


            visuals.addEntry(effectsGroup.build());

            builder.setSavingRunnable(ZoomConfig::save);
            return builder.build();
        };
    }
}
