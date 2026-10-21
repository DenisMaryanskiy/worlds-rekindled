# Vanilla Reforged

A vanilla+ Fabric mod that gives recent Minecraft updates (starting with the Pale Garden)
meaningful rewards: new items, ingredients and mechanics that fit the base game.

- Minecraft: 26.3 (Java Edition)
- Loader: Fabric Loader 0.19.5 + Fabric API
- Java: 25

- Languages: English, Russian, German, French, Spanish, Italian

What each version added is in [CHANGELOG.md](CHANGELOG.md).

## Development

```bash
./gradlew build        # compile and package -> build/libs/
./gradlew runClient    # launch a dev Minecraft client with the mod
./gradlew runServer    # launch a dev dedicated server (asks you to accept the EULA in run/eula.txt)
./gradlew runGameTest  # run the GameTests in src/gametest (also part of build)
./gradlew genSources   # decompile Minecraft so you can read its code in the IDE
./gradlew vscode       # regenerate VS Code launch configurations
```

The Elderwood's placeholder model and textures are generated from one spec, so their boxes and UVs
always agree. After changing the spec, regenerate both (plain Python 3, no dependencies):

```bash
python3 tools/elderwood_model.py
```

The Pale Ruin's structure templates and worldgen files are generated too, from
`tools/pale_ruin.py`. After changing it, regenerate them and let the GameTests check that the Ruin
still assembles:

```bash
python3 tools/pale_ruin.py
./gradlew runGameTest
```

## Releases

Pushing a version tag that matches `version` in `gradle.properties` (e.g. `v0.4.9`) builds the mod,
runs its GameTests and publishes the jar as a GitHub Release, with that version's section of the
changelog as its notes:

```bash
git tag v0.4.9
git push origin v0.4.9
```

## License

MIT, see [LICENSE](LICENSE).
