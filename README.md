# Vanilla Reforged

A vanilla+ Fabric mod that gives recent Minecraft updates (starting with the Pale Garden)
meaningful rewards: new items, ingredients and mechanics that fit the base game.

- Minecraft: 26.3 (Java Edition)
- Loader: Fabric Loader 0.19.5 + Fabric API
- Java: 25

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

## License

All rights reserved until a license is chosen.
