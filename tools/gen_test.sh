#!/bin/bash
# Generate a solo seed with the dev apworld. usage: gen_test.sh <seed> [option=value ...]
# e.g. gen_test.sh 3 goal=ender_dragon hub_door_checks=false
D=$(cd "$(dirname "$0")/.." && pwd)
seed=$1; shift
dir=$(mktemp -d); out=$D/tools-ext/gen-out; mkdir -p "$out"
{ echo "name: Miner"; echo "game: Craftmine"; echo "Craftmine:"
  echo "  progression_balancing: 50"; for kv in "$@"; do echo "  ${kv%%=*}: ${kv#*=}"; done; } > "$dir/p.yaml"
cd "$D/tools-ext/ap" && SKIP_REQUIREMENTS_UPDATE=1 "$D/tools-ext/apvenv/bin/python" Generate.py \
  --player_files_path "$dir" --outputpath "$out" --seed "$seed" < /dev/null > "$out/last.log" 2>&1
code=$?; rm -rf "$dir"
grep -E 'Done. Enjoy|FillError' "$out/last.log" | head -1; exit $code
