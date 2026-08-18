# LocalCapes

Client-side Minecraft capes for **1.20.1** (Fabric and Forge). Architectury API required.

Official capes are cosmetics on the player model, not inventory items. This mod bundles 49 vanilla-format capes and lets you pick one from a menu.

## Install

1. Install [Architectury API](https://modrinth.com/mod/architectury-api) for 1.20.1 (same loader as the game).
2. Drop `localcapes-1.0.0+fabric-1.20.1.jar` or `localcapes-1.0.0+forge-1.20.1.jar` into `mods/`.

## Use

- Open survival or creative inventory and click **披风** on the right.
- Or press **H**.
- Or run `/localcapes`.

Click a cape to wear it. **卸下** removes it. F5 to see your back.

Extra `64x32` PNGs in `config/localcapes/` show up under **本地文件**. `YourName.png` still forces that player to that file.

## Build

```bash
./gradlew build
```

Jars land in `fabric/build/libs/` and `forge/build/libs/`.
