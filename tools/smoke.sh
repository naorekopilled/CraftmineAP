#!/bin/bash
# End-to-end smoke test: a local Archipelago server and a headless dev Minecraft server that auto-connects to it,
# both driven through their consoles. usage: smoke.sh [connect|checks|items|all]  (default all)
# PROD=1 runs the built jar on a real Fabric server (tools-ext/prod-server, see README) instead of the dev run.
D=$(cd "$(dirname "$0")/.." && pwd)
STAGE=${1:-all}
W=$D/tools-ext/smoke
: "${JAVA_HOME:?set JAVA_HOME to a JDK 21}"
export JAVA_HOME
rm -rf "$W" "$D/mod/run-smoke/world" "$D/mod/run-smoke/craftmineap.json"
mkdir -p "$W" "$D/mod/run-smoke"
echo eula=true > "$D/mod/run-smoke/eula.txt"
: > "$W/ap.in"; : > "$W/mc.in"

"$D/tools/gen_test.sh" 7 > /dev/null || { echo "seed generation failed"; exit 1; }
zip=$(ls -t "$D"/tools-ext/gen-out/*.zip | head -1)
rm -f "${zip%.zip}.apsave"  # MultiServer resumes from it, and checks from an earlier run would not show again

(cd "$D/tools-ext/ap" && tail -f "$W/ap.in" | SKIP_REQUIREMENTS_UPDATE=1 ../apvenv/bin/python MultiServer.py \
  --port 38281 --host localhost "$zip" > "$W/ap.log" 2>&1) &
if [ -n "$PROD" ]; then
  P=$D/tools-ext/prod-server
  rm -rf "$P/world" && echo eula=true > "$P/eula.txt" && printf 'online-mode=false\n' > "$P/server.properties"
  (cd "$D/mod" && ./gradlew build -q -x test) && cp "$D"/mod/build/libs/craftmineap-*.jar "$P/mods/craftmineap.jar"
  (cd "$P" && tail -f "$W/mc.in" | "$JAVA_HOME/bin/java" -Dcraftmineap.dev=true -jar fabric-server-launch.jar nogui \
    > "$W/mc.log" 2>&1) &
else
  (cd "$D/mod" && tail -f "$W/mc.in" | ./gradlew runSmoke --console=plain > "$W/mc.log" 2>&1) &
fi

mc() { echo "$1" >> "$W/mc.in"; }
ap() { echo "$1" >> "$W/ap.in"; }
wait_for() {
  for _ in $(seq 1 "${3:-180}"); do grep -q -- "$2" "$1" && return 0; sleep 1; done
  echo "TIMEOUT waiting for '$2' in $1"; return 1
}
finish() {
  mc "stop"; ap "/exit"
  for _ in $(seq 1 60); do grep -q 'Stopping the server\|BUILD' "$W/mc.log" && break; sleep 1; done
  sleep 5
  for p in $(jobs -p); do pkill -P "$p" 2>/dev/null; kill "$p" 2>/dev/null; done
  exit "$1"
}

wait_for "$W/ap.log" "server listening" 60 || finish 1
# the production run connects through /connect, the way a player does
if [ -n "$PROD" ]; then wait_for "$W/mc.log" "Done (" 300 || finish 1; mc "connect localhost:38281 Miner"; fi
wait_for "$W/mc.log" "Connected to Archipelago as Miner" 300 || finish 1
[ "$STAGE" = connect ] && { echo "SMOKE OK"; finish 0; }

mc "start"
wait_for "$W/mc.log" "Randomizer started" 30 || finish 1
mc "apdev unlock_effect foxes"
wait_for "$W/ap.log" "Ingredient: Foxes" 30 || finish 1
mc "apdev effect_unlocked foxes"
wait_for "$W/mc.log" "apdev foxes unlocked=false" 30 || finish 1
mc "apdev crafter_xp 300"
wait_for "$W/ap.log" "Mine Crafter Level 2" 30 || finish 1
[ "$STAGE" = checks ] && { echo "SMOKE OK"; finish 0; }

ap "/send Miner Desert Biome"
wait_for "$W/mc.log" "Unlocked new Mine Ingredient: Desert Biome" 30 || finish 1

# a bought node is a check, and its effect waits for its item
mc "apdev active combatant"
wait_for "$W/mc.log" "apdev combatant active=false" 30 || finish 1
mc "apdev buy combatant"
wait_for "$W/mc.log" "apdev combatant bought=true" 30 || finish 1
wait_for "$W/ap.log" "(Upgrade: Combat)" 30 || finish 1
sleep 2
mc "apdev active combatant"
for _ in $(seq 1 30); do [ "$(grep -c 'apdev combatant active=false' "$W/mc.log")" -ge 2 ] && break; sleep 1; done
grep -q 'apdev combatant active=true' "$W/mc.log" && { echo "bought node became active without its item"; finish 1; }
ap "/send Miner Combat"
wait_for "$W/ap.log" 'sending "Combat"' 30 || finish 1
sleep 2
mc "apdev active combatant"
wait_for "$W/mc.log" "apdev combatant active=true" 30 || finish 1

# a received gear unlock gives its items on entering a mine, bought or not
mc "apdev mine_gear ender_pearl"
wait_for "$W/mc.log" "apdev mine_gear ender_pearl=0" 30 || finish 1
ap "/send Miner Progressive Pearls"
wait_for "$W/ap.log" 'sending "Progressive Pearls"' 30 || finish 1
sleep 2
mc "apdev mine_gear ender_pearl"
wait_for "$W/mc.log" "apdev mine_gear ender_pearl=5" 30 || finish 1

# entering a mine restarts the game inside the server; everything must still hold after it
mc "apdev restart"
wait_for "$W/mc.log" "Saving worlds" 60 || finish 1
sleep 8
mc "apdev mine_gear ender_pearl"
for _ in $(seq 1 30); do [ "$(grep -c 'apdev mine_gear ender_pearl=' "$W/mc.log")" -ge 3 ] && break; sleep 1; done
grep 'apdev mine_gear ender_pearl=' "$W/mc.log" | tail -1 | grep -q "=5" || { echo "gear lost after the in-server restart"; finish 1; }
echo "SMOKE OK"
finish 0
