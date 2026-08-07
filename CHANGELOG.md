# Changelog

## 1.2.0

### Breaking

- The mod's package moved from `com.yourname.zoomrgy` to `com.jom3a.zoomrgy`, and the Maven
  group is now `com.jom3a`. Anything depending on the old package will need updating.
- Nine deprecated config keys are no longer written (`zoomedFov`, `zoomInSpeed`, `zoomOutSpeed`,
  `zoomInTransition`, `zoomOutTransition`, `cinematicCamera`, `cinematicCameraMultiplier`,
  `zoomedFovPreset2`, `spyglassZoomFov`). Existing files still migrate; the dead keys simply
  disappear on the next save.
- `hudOffsetX` / `hudOffsetY` changed meaning from raw screen deltas to insets measured inwards
  from the anchored edge. Configs carrying the old `-60` are migrated automatically.

### Fixed

- A malformed `zoomrgy.json` crashed the game on startup. Bad files now fall back to defaults and
  are left on disk to be repaired.
- An unrecognised easing name deserialised to `null` and threw on every frame; a legacy easing
  name could also leave the config screen's selector on an invalid entry.
- The `Back` and `Elastic` curves overshoot past their target, which at high magnification drove
  the camera FOV negative and broke the projection. Measured at -0.33 before the fix.
- Zooming, then releasing, snapped the view out to the preset's base level before easing away.
  The zoom parameters are now held through the fade-out.
- The scroll wheel was swallowed while a screen was open, so a locked zoom broke scrolling in the
  inventory, creative tabs and chat.
- Zoom state survived leaving a world, so the next world you joined rendered fully zoomed in and
  then animated out.
- F1 hid the zoom HUD text but left the vignette and scope overlay on screen.
- Vanilla's spyglass scope was removed even with the mod's spyglass zoom turned off.
- The keybind category never translated, showing a raw translation key in Controls.
- The telemetry player icon used a character outside the Basic Multilingual Plane, which Minecraft
  cannot render, so it appeared as a missing-glyph box.
- Mouse sensitivity followed a reconstruction of the transition rather than the FOV actually being
  rendered, and drifted out of step mid-zoom.
- Fog reduction ignored the preset multiplier entirely, so preset 2 pushed fog no further than the
  primary zoom.
- The scroll level could stay above a lowered maximum, and a double-tap was dropped if a key was
  held through the damage cutoff.

### Changed

- Scroll steps are now geometric rather than linear. Previously the first notch halved the FOV and
  the tenth barely registered; every notch is now the same proportional step, with a configurable
  ratio.
- Each zoom source keeps its own scroll level, so working preset 2 no longer discards where the
  primary zoom was left.
- Zoom in and zoom out have separate speeds and separate easing curves.
- Defaults are `SMOOTHSTEP` and a 40% vignette, matching what the documentation always described.
- The targeting raycast stops at the first solid block instead of always spanning the full ray,
  and its range follows current magnification rather than a fixed 150 blocks.

### Added

- The zoom HUD can be anchored to any of nine screen positions, with insets.
- The Easing & Transitions page is split: settings on the left, a live preview on the right that
  zooms a picture from your own game using the configured curves and speeds. Press F2 on a view
  you like and it becomes the preview.
- Telemetry shows block coordinates and, for living entities, current and maximum health.
- A client gametest suite covering the zoom and HUD behaviour, run with `./gradlew runClientGameTest`.

## 1.1.0

- Initial tracked release.
