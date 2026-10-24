#!/bin/bash
# Rewrite the mod's copy of the logic (craftmineap/logic.json) and its test fixture from the apworld's rules.
D=$(cd "$(dirname "$0")/.." && pwd)
cd "$D/tools-ext/ap" && CRAFTMINE_WRITE_LOGIC=1 SKIP_REQUIREMENTS_UPDATE=1 ../apvenv/bin/python -m pytest -q \
  worlds/craftmine/test/test_logic_export.py
