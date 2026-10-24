#!/bin/bash
# Seeds 1-20 for each goal, with every check group on and with every check group off.
D=$(cd "$(dirname "$0")/.." && pwd)
fail=0
for seed in $(seq 1 20); do
  for goal in special_mines ender_dragon; do
    for groups in "advancement_checks=true crafter_level_checks=10 hub_door_checks=true" \
                  "advancement_checks=false crafter_level_checks=0 hub_door_checks=false"; do
      result=$("$D/tools/gen_test.sh" "$seed" goal=$goal $groups)
      [[ "$result" == *"Done. Enjoy"* ]] || { echo "FAIL seed=$seed goal=$goal $groups: $result"; fail=1; }
    done
  done
done
[ $fail = 0 ] && echo "all 80 seeds generated"
exit $fail
