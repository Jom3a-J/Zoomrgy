# Changelog

## 1.2.0

### New

- Runs on **NeoForge** as well as Fabric.
- Zoom in and zoom out now have **separate speeds and easing curves**.
- **Live preview** in the settings, showing a picture from your own game zooming with the curve
  you picked. Press F2 on a view you like and it becomes the preview.
- The zoom HUD can be **moved** to any of nine screen positions.
- Rangefinder now shows block coordinates and mob health.

### Changed

- Scroll steps are even. Every notch is the same amount of zoom, instead of the first notch doing
  far more than the last.
- Each zoom key remembers its own scroll level.
- Mouse sensitivity and fog now follow how far you are actually zoomed in.

### Fixed

- Crash on startup if the config file was damaged.
- Broken view when using the Back or Elastic curves at high zoom.
- Zoom jumping outward for a moment when you let go of the key.
- Scroll wheel not working in the inventory and chat while zoom was locked.
- Rejoining a world while zoomed left the view stuck zoomed in.
- F1 not hiding the zoom overlay.
- Vanilla spyglass overlay disappearing even with the mod's spyglass zoom turned off.
- Keybind category showing a raw name in Controls.
- A broken character in the rangefinder text.

### Note for updating

- The download is now split per loader: `zoomrgy-fabric-1.2.0.jar` and
  `zoomrgy-neoforge-1.2.0.jar`.
- Your settings carry over. Some old unused entries are cleaned out of `zoomrgy.json`.

### Not supported

- **Quilt.** Not tested and not supported. Quilted Fabric API has not been updated past
  Minecraft 1.21, so the usual way of getting Fabric API on Quilt is unavailable.
- On NeoForge, "Hide Hotbar during Zoom" hides the hotbar but not the health and hunger bars
  around it.

## 1.1.0

- Initial tracked release.
