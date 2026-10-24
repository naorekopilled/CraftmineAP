#!/bin/bash
# Archipelago 0.6.7 source + venv in tools-ext, with apworld/craftmine linked into its worlds folder.
set -e
D=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$D/tools-ext"
[ -d "$D/tools-ext/ap" ] || git clone -q --depth 1 --branch 0.6.7 https://github.com/ArchipelagoMW/Archipelago "$D/tools-ext/ap"
[ -d "$D/tools-ext/apvenv" ] || python3.12 -m venv "$D/tools-ext/apvenv"
"$D/tools-ext/apvenv/bin/pip" install -q -r "$D/tools-ext/ap/requirements.txt" pytest
ln -sfn ../../../apworld/craftmine "$D/tools-ext/ap/worlds/craftmine"
