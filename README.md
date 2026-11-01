# Craftmine AP

An [Archipelago](https://archipelago.gg) randomizer for Craftmine, the Minecraft April Fools 2025 snapshot
(`25w14craftmine`).

Mine ingredients and skill-tree upgrades are shuffled into the multiworld. Unlocking an ingredient, buying a
skill-tree node, earning an advancement, winning a special mine, leveling the Mine Crafter and opening a hub door
send checks.

Singleplayer only for now, multiplayer planned for the future.

## What you need

- Minecraft `25w14craftmine` with Fabric Loader 0.16.14 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) `0.119.10+25w14craftmine`
- Archipelago 0.6.4 or newer (tested on 0.6.7)
- `craftmineap-0.2.0.jar` and `craftmine.apworld` from the [releases page](../../releases)

## Install

1. In your launcher, make a new instance for Minecraft `25w14craftmine` (it is a snapshot) with the Fabric loader.
2. Put Fabric API and `craftmineap-0.2.0.jar` in that instance's `mods` folder.
3. Install `craftmine.apworld` like any other apworld.

## Options

| Option | Default | What it does |
| --- | --- | --- |
| `goal` | `special_mines` | `special_mines`: win a number of different special mines. `ender_dragon`: win the Ender Dragon mine. |
| `special_mines_needed` | 9 | How many special mines the `special_mines` goal needs (1 to 9). |
| `advancement_checks` | on | Advancements are checks (36). |
| `crafter_level_checks` | 10 | Mine Crafter levels 1 up to this number are checks (0 to 48). Later levels take a very long time. |
| `hub_door_checks` | on | Opening a hub door is a check (10 doors), and Shimmering Keys only come from the multiworld. |
| `upgrade_tabs` | on | Each skill-tree tab has its own item, and its upgrades can't be bought until you have it. One random tab starts open. |
| `death_link` | off | Only deaths in a mine are sent, and a received death only kills you while you are in a mine. |

With the defaults a seed has 299 checks: 81 ingredients, 153 skill-tree nodes, 36 advancements, 9 special mines,
10 Mine Crafter levels and 10 hub doors.

## Play

1. Make a new singleplayer world. Cheats are not needed.
2. Type `/connect <address:port> <slot name> [password]`, for example `/connect archipelago.gg:38281 Steve`.
   Put the slot name in quotes if it has spaces.
3. Type `/start`. Items you receive are only applied once the randomizer is started in that world.

`/connect` without arguments reconnects with the saved details. `/disconnect` and `/sync` are there too. A world belongs to the first room and slot it connects to, so use a new world for
each seed.

Server messages show up in chat, colored like the text client.

### How the shuffle works in game

- An ingredient's normal unlock action (for example picking sweet berries for Foxes) sends its check. The ingredient
  itself unlocks when its item arrives.
- Buying a skill-tree node spends your XP, marks it bought and opens its children as usual, and sends its check.
  The node's effect only turns on when its item arrives. Long chains such as the swords, jumps and Learning are
  progressive items. School of Hard Knocks nodes are checks with no items, so buying them never applies their
  penalties.
- The skill tree (`U`) draws a green glow around every upgrade you have received. The normal gold frame still means
  you bought that node.
- With `upgrade_tabs` on, nodes you can't buy yet get a red glow and name the tab item they need. Upgrades you receive
  work even while their tab is closed. Nodes bought before `/start` still count.
- Shimmering Keys and Bags of XP wait until you are back in the hub, since a mine converts or clears your inventory.
  A Bag of XP gives 5 levels.
- Some nodes stay hidden until the game reveals them. For example, Let's get Pearling shows as `?` until you kill an
  enderman. The logic knows about this.

### Tracker

The advancements screen (`L`) has a Tracker tab that lists every check in your seed, grouped by type, with the icon
of the ingredient, node or advancement it belongs to. Checks you have done have a gold frame, checks that are in
logic look normal, and checks that are out of logic are grayed out. Hovering an out-of-logic check shows what it
still needs. The tab updates each time you open the screen.

## Known limits

- Singleplayer only for now. The mod relies on the game's server and client sharing one process.
- Not every check has been played through in game yet. If something does not send or does not unlock, please open
  an issue with your `latest.log`.

## Building from source

The mod needs JDK 21: `cd mod && ./gradlew build` puts the jar in `mod/build/libs/`. `tools/build_apworld.sh` zips
the apworld into `build/craftmine.apworld`.

For the tests, `tools/setup_ap.sh` checks out Archipelago and makes a virtual environment in `tools-ext/`. Then:

- apworld tests: `cd tools-ext/ap && SKIP_REQUIREMENTS_UPDATE=1 ../apvenv/bin/python -m pytest worlds/craftmine/test`
- mod tests: `cd mod && ./gradlew test`
- `tools/gen_matrix.sh` generates a batch of seeds with different options.
- `tools/smoke.sh` runs a local Archipelago server and a headless Minecraft server and checks that items and checks
  go both ways (`PROD=1` uses the built jar on a real Fabric server, after `tools/setup_prod_server.sh`).

The game data the apworld uses (`apworld/craftmine/data/craftmine.json`) comes from the game itself:
`cd mod && ./gradlew runDump`. The mod carries its own copy of the logic for the Tracker tab; after changing the
rules, run `tools/export_logic.sh` to update it (the apworld tests fail until you do).

## Credits

The mod jar bundles [Java-Client](https://github.com/ArchipelagoMW/Java-Client) (MIT),
[Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) (MIT) and
[Apache HttpComponents Core](https://hc.apache.org) (Apache License 2.0). Their license texts ship in the jar, see
[THIRD_PARTY_LICENSES.txt](mod/src/main/resources/THIRD_PARTY_LICENSES.txt).

## License

MIT, see [LICENSE](LICENSE).
