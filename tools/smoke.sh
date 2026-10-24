#!/bin/bash
# End-to-end smoke test: a local Archipelago server and a headless dev Minecraft server that auto-connects to it,
# both driven through their consoles. usage: smoke.sh [connect|checks|items|all]  (default all)
D=$(cd "$(dirname "$0")/.." && pwd)
STAGE=${1:-all}
W=$D/tools-ext/smoke
JAVA_HOME=${JAVA_HOME:-$HOME/.local/share/PrismLauncher/java/java-runtime-delta}
export JAVA_HOME
rm -rf "$W" "$D/mod/run-smoke/world" "$D/mod/run-smoke/craftmineap.json"
mkdir -p "$W" "$D/mod/run-smoke"
echo eula=true > "$D/mod/run-smoke/eula.txt"
: > "$W/ap.in"; : > "$W/mc.in"

"$D/tools/gen_test.sh" 7 > /dev/null || { echo "seed generation failed"; exit 1; }
zip=$(ls -t "$D"/tools-ext/gen-out/*.zip | head -1)

(cd "$D/tools-ext/ap" && tail -f "$W/ap.in" | SKIP_REQUIREMENTS_UPDATE=1 ../apvenv/bin/python MultiServer.py \
  --port 38281 --host localhost "$zip" > "$W/ap.log" 2>&1) &
(cd "$D/mod" && tail -f "$W/mc.in" | ./gradlew runSmoke --console=plain > "$W/mc.log" 2>&1) &

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
wait_for "$W/mc.log" "Connected to Archipelago as Miner" 300 || finish 1
[ "$STAGE" = connect ] && { echo "SMOKE OK"; finish 0; }

mc "start"
wait_for "$W/mc.log" "The randomizer is on" 30 || finish 1
mc "unlock_world_effect minecraft:foxes"
wait_for "$W/ap.log" "Unlock Foxes" 30 || finish 1
mc "unlock_world_effect minecraft:foxes"
[ "$STAGE" = checks ] && { echo "SMOKE OK"; finish 0; }

ap "/send Miner Desert Biome"
wait_for "$W/mc.log" "Unlocked new Mine Ingredient: Desert Biome" 30 || finish 1
echo "SMOKE OK"
finish 0
