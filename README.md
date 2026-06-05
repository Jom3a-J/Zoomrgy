# Zoomrgy 🔍

A fully-featured, high-performance, and extremely configurable client-side zoom mod for Minecraft (Fabric).

Zoomrgy provides a smooth, premium zooming experience ("Smith Zoom") featuring highly-optimized easing curves, mouse sensitivity adjustment, scroll-wheel scaling, and built-in telemetry tools like a rangefinder and target highlighter.

---

## 🌟 Key Features

* **Ultra-Smooth Easing Transitions (Smith Zoom):** Eases your field of view dynamically rather than instantly snapping. Supports multiple easing functions (Quadratic, Cubic, Circular, Sine, Elastic, Bounce, Smoothstep, and more) optimized for zero frame-rate stutter.
* **Scroll-Wheel Leveling:** Hold down the zoom key and scroll your mouse wheel up or down to adjust your magnification level on the fly, with optional click audio feedback.
* **Dynamic Mouse Sensitivity Scaling:** Scales mouse look speed down proportionally with the current FOV, preventing your cursor from feeling wildly sensitive at high magnifications.
* **Dual Zoom Presets & Spyglass Integration:** Assign separate hotkeys for two distinct zoom presets (e.g., C for standard 4.5x zoom, V for high-power 14x zoom). Optionally intercepts and enhances vanilla Spyglass usage.
* **Intelligent Telemetry HUD:** Displays current zoom magnification factor, directional compass heading, block/entity rangefinder distance, and entity type markers under your crosshair.
* **Highlight Targeted Entity:** Automatically highlights the entity you are looking at under high magnification with a client-side glowing outline effect.
* **Cinematic Inertia:** Simulates high-inertia camera panning for butter-smooth cinematic tracking shots (fully customizable).
* **Fog Reduction:** Pushes fog distance back proportionally to the zoom multiplier to give you a clear line of sight over long distances.

---

## 🛠 Installation & Dependencies

To run Zoomrgy, place the compiled `.jar` file in your Minecraft `mods` folder. Make sure you have installed the following:

1. **[Fabric Loader](https://fabricmc.net/)** (version `>=0.19.2` or newer)
2. **[Fabric API](https://modrinth.com/mod/fabric-api)** (version `>=0.150.0`)
3. **[Cloth Config API](https://modrinth.com/mod/cloth-config)** (Required for the configuration UI screen)
4. **[Mod Menu](https://modrinth.com/mod/modmenu)** (Highly recommended; provides access to the config screen GUI)

---

## 🎮 Controls & Keybindings

Zoomrgy registers a custom controls category. You can rebind these keys in Minecraft's default **Controls > Key Binds** menu:

| Key Bind Description | Default Key | Behavior |
| :--- | :---: | :--- |
| **Zoom** | `C` | Activates primary zoom level (holds state by default, supports toggling / double-tap lock). |
| **Zoom Preset 2** | `V` | Activates secondary high-magnification preset zoom. |
| **Zoom In** | *Unbound* | Increases scroll-wheel zoom step manually. |
| **Zoom Out** | *Unbound* | Decreases scroll-wheel zoom step manually. |
| **Toggle Zoom Lock** | *Unbound* | Manually locks/unlocks the current active zoom state. |

* **Scroll Wheel Mechanics:** While zooming, use `Scroll Up` to zoom in further, and `Scroll Down` to zoom out.
* **Double Tap to Lock:** Double tap the Zoom key within `300ms` to lock your zoom state. Tap again or press any zoom binding to release the lock.

---

## ⚙ Configuration Settings

Configure Zoomrgy directly in-game by clicking the gear icon on the mod list using **Mod Menu**. The JSON configuration is saved to `config/zoomrgy.json`.

Below is a detailed breakdown of all available settings:

### 🎮 Controls & Behavior

| In-Game Setting Name | JSON Field Key | Default Value | Description |
| :--- | :--- | :---: | :--- |
| **Base Zoom Level** | `zoomMultiplier` | `4.5` | The multiplier applied to your base FOV when using primary Zoom (C). |
| **Preset 2 Zoom Level** | `zoomMultiplierPreset2` | `14.0` | The multiplier applied when using Zoom Preset 2 (V). |
| **Max Scroll Level** | `maxScrollLevel` | `10` | The maximum step limit for scroll-wheel zoom scaling. |
| **Reset Scroll on Release**| `resetScrollOnRelease` | `true` | Snaps your scroll multiplier back to `1.0x` when you release the zoom key. |
| **Zoom Toggle Mode** | `zoomToggleMode` | `false` | Pressing the zoom key toggles the state permanently until pressed again (no hold required). |
| **Double Tap to Lock** | `doubleTapToLock` | `true` | Allows double-tapping the hold-zoom key to lock the camera zoom. |
| **Auto Zoom-Out on Damage**| `zoomOutOnDamage` | `true` | Automatically drops zoom mode if you take damage to prevent combat disorientation. |
| **Scroll Audio Feedback** | `scrollAudioFeedback` | `true` | Plays a subtle mechanical click sound when changing zoom levels. |
| **Spyglass Auto-Zoom** | `spyglassAutoZoom` | `true` | Integrates mod easing transitions automatically when looking through a vanilla spyglass. |
| **Spyglass Zoom Level** | `spyglassZoomMultiplier`| `10.0` | The zoom multiplier applied to vanilla spyglass usage. |
| **Show Vanilla Spyglass Overlay**| `showVanillaSpyglassOverlay`| `false` | Retains or hides the default black square spyglass vignette border. |

### 📈 Easing & Transitions

| In-Game Setting Name | JSON Field Key | Default Value | Description |
| :--- | :--- | :---: | :--- |
| **Zoom Speed** | `zoomSpeed` | `0.1` (`10%`) | The rate of transition (interpolation factor). `0.05` is cinematic; `1.0` is instant. |
| **Transition Type** | `transitionType` | `SMOOTHSTEP` | The mathematical easing function used to interpolate FOV transitions. |
| **Dynamic Movement Damping**| `movementFovDamping` | `0.8` (`80%`) | Dampens sudden FOV offsets caused by in-game motion (sprinting, flight) to stabilize view. |

### 🖱 Mouse & Sensitivity

| In-Game Setting Name | JSON Field Key | Default Value | Description |
| :--- | :--- | :---: | :--- |
| **Cinematic Zoom Smoothness**| `cinematicSmoothness`| `0.0` (`OFF`) | Smooths out mouse look inputs (camera inertia). Increase up to `0.95` (`95%`) for cinematic pans. |
| **Reduce Sensitivity** | `reduceSensitivity` | `true` | Proportionally lowers mouse sensitivity as zoom levels increase to maintain pixel-perfect aiming. |
| **Affect Hand FOV** | `affectHandFov` | `true` | Scales first-person hand rendering FOV down during zoom animations. |

### 🎨 Visuals & Overlay

| In-Game Setting Name | JSON Field Key | Default Value | Description |
| :--- | :--- | :---: | :--- |
| **Show Zoom HUD Overlay** | `showZoomHud` | `true` | Renders a clean glassmorphism-styled info bar at the bottom center of your screen. |
| **Zoom HUD Background** | `zoomHudBackground` | `true` | Shows a semi-transparent dark plate with a subtle border outline behind the HUD. |
| **Show Telemetry HUD** | `showTelemetryHud` | `true` | Shows target name (block/entity) and live distance reading under the crosshair. |
| **Highlight Targeted Entity**| `highlightTargetEntity`| `true` | Highlights the entity under your crosshair with a glowing client outline. |
| **Zoom Vignette Opacity** | `zoomVignetteOpacity` | `0.4` (`40%`) | Adds a cinematic dark vignette shadow around screen edges. Set to `0%` to disable. |
| **Reduce Fog during Zoom** | `reduceFog` | `true` | Dynamically pushes environmental fog back during active zoom to maximize sight lines. |
| **Hide Crosshair during Zoom**| `hideCrosshair` | `false` | Hides the crosshair cursor during active zoom. |
| **Spyglass Scope Simulation**| `spyglassScopeOverlay`| `false` | Renders a circular spyglass overlay mask on the screen during zoom. |

---

## 📈 Easing Curve Types

Choose from a variety of transition styles under the **Easing & Transitions** settings:

* **`INSTANT`**: Bypasses transitions entirely (instant snap).
* **`LINEAR`**: Constant, steady animation speed.
* **`SINE` / `QUAD` / `CUBIC`**: Eased transitions (In, Out, or In-Out) for a soft start/end.
* **`SMOOTHSTEP`**: A classic graphics interpolation curve that starts slow, speeds up, and ends slow.
* **`CIRCULAR`**: Eases aggressively using a circular root-based curve.
* **`BACK`**: Slightly overshoots target zoom before pulling back, giving a spring-like feel.
* **`ELASTIC`**: A wavy, physical spring-like oscillation transition.
* **`BOUNCE`**: Bounces slightly upon reaching the target zoom.

---

## 👨‍💻 For Developers (Building from Source)

Zoomrgy is built using Gradle. If you wish to build or compile the mod yourself:

1. Clone or download the repository.
2. Open a terminal in the root directory.
3. Run the Gradle build script:
   ```powershell
   # Windows PowerShell
   .\gradlew.bat build
   
   # Linux / macOS
   ./gradlew build
   ```
4. The compiled `.jar` artifact will be located in:
   ```
   build/libs/zoomrgy-[version].jar
   ```

---

## 📄 License

This mod is available under the **MIT License**. Feel free to fork, modify, or include it in custom modpacks.
