# CurseForge listing kit

## Project fields

- **Name:** Worlds Rekindled
- **Suggested slug:** `worlds-rekindled` (availability must be checked when updating the project)
- **Summary:** Expand the Pale Garden with underground ruins, a new boss, craftable guardians and useful survival rewards, all in a vanilla style.
- **Description:** paste [description.md](description.md) into the Markdown editor.
- **Avatar:** [icon.png](icon.png), 400×400, opaque PNG.
- **Gallery:** use the eight optimized PNGs in [screenshots/](screenshots/), each below 2 MB.
- **Game / class:** Minecraft: Java Edition / Mods
- **Suggested categories:** Adventure and RPG; World Gen; Mobs; Structures (choose the available equivalents).
- **License:** MIT
- **Source:** https://github.com/DenisMaryanskiy/worlds-rekindled
- **Issues:** https://github.com/DenisMaryanskiy/worlds-rekindled/issues

## First file

- **File:** `build/libs/worlds-rekindled-0.5.1.jar`, not a sources or test JAR. After the merged
  release is tagged, the same version is also available from the GitHub release.
- **Display name:** Worlds Rekindled 0.5.1 — Pale Garden Beta
- **Release type:** Beta
- **Minecraft:** 26.3 only
- **Loader:** Fabric
- **Environment:** client and server
- **Java:** 25
- **Required dependency:** Fabric API
- **Changelog:** paste [release-notes.md](release-notes.md), which introduces the complete beta
  to first-time CurseForge users and explains the change since 0.5.0.

Version 0.5.1 includes the replacement embedded icon and keeps 0.5.0's gameplay. It is compatible
with existing 0.5.0 worlds; no registered content IDs or saved-data formats changed.

Use **Worlds Rekindled** as the project title. The version and beta label belong in the file's
display name and release fields. The JAR's display name is updated too; its internal mod ID
and content namespace retain `vanillareforged` for compatibility. Replace an already-uploaded
file with the newly built JAR if it still shows the old name in its metadata. The public rename
does not change the version or create a release.

## Gallery titles

Upload these exports without further cropping or retouching.

The exports retain their original 2880×1800 resolution and color profiles. Three use lossless
RGB compression; the other five use optimized palettes. All are below 2,000,000 bytes.

Suggested order and public titles:

1. `Elderwood.png` — The Elderwood
2. `Pale Ruin Entrance.png` — Discover the Pale Ruin
3. `Sentries.png` — Guardians of the Ruin
4. `Pale Mask.png` — The Pale Mask
5. `Freiendly Sentry.png` — A Heartwood Sentry of Your Own
6. `Pavillion.png` — The Summoning Hall
7. `Treasury.png` — Archaeology in the Ruin
8. `Dungeon Trunk.png` — Roots Through the Ruin

## Artwork source and provenance

The icon depicts a grass block resting on an anvil, with an amber seam and a spark. Its 40×40
source is defined in [tools/mod_icon.py](../../../tools/mod_icon.py); the export uses integer
nearest-neighbor scaling, with no smoothing or extracted Minecraft textures. Regenerate it with:

```sh
python3 tools/mod_icon.py
```

It writes this 400×400 avatar and the 160×160 embedded mod icon. The design and pixel placement
were authored with Codex assistance; this is AI-assisted artwork, even though it uses an explicit
pixel grid rather than an image-generation model. The description and suggested summary were
also prepared with Codex. Review both before publishing.

## Readiness

- The author accepted the existing eight gameplay screenshots for the first listing.
- The author confirmed the asset-origin/credit review and installed-JAR/client/server checks
  were completed before finalizing these release files. Version 0.5.1 changes packaging and
  presentation only.
- The full build runs the automated checks before the release files are handed over.

This kit targets CurseForge only. Creating the project, uploading its files and submitting it
for moderation are handled by the author.
