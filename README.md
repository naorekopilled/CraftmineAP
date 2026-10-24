# Craftmine AP

<!-- placeholder: all text in this file is to be rewritten -->

Archipelago for Craftmine (Minecraft 25w14craftmine): a Fabric mod and the `craftmine` apworld.

## Install (placeholder)

1. Modrinth App: new profile, Minecraft `25w14craftmine` (snapshots shown), loader Fabric `0.16.14`.
2. Add Fabric API `0.119.10+25w14craftmine` and `craftmineap-0.1.0.jar` (`mod/build/libs/`) to the profile's mods.
3. Put `craftmine.apworld` (`build/`) in the Archipelago launcher's `custom_worlds` folder; generate the YAML
   template from the launcher.

## Play (placeholder)

1. Create a singleplayer world (cheats on is not needed).
2. `/connect <address:port> <slot> [password]`, then `/start`.
3. `/connect` alone reconnects with the saved details; `/disconnect`, `/sync`.

## Development

- `tools/setup_ap.sh`: Archipelago 0.6.7 source + venv in `tools-ext/`.
- apworld tests: `cd tools-ext/ap && SKIP_REQUIREMENTS_UPDATE=1 ../apvenv/bin/python -m pytest worlds/craftmine/test`
- `tools/gen_matrix.sh`, `tools/build_apworld.sh`, `tools/smoke.sh` (`PROD=1` after `tools/setup_prod_server.sh`
  runs the built jar on a real Fabric server).
- mod: `cd mod && ./gradlew build` (JDK 21); `./gradlew runDump` regenerates `apworld/craftmine/data/craftmine.json`;
  `./gradlew runAudit` applies every mixin in a client and quits.
