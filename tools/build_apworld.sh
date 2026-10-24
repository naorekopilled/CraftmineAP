#!/bin/bash
# Zip apworld/craftmine into build/craftmine.apworld and copy it to the Archipelago launcher's custom worlds folder.
D=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$D/build"
python3 - "$D" <<'PY'
import sys, zipfile
from pathlib import Path
d = Path(sys.argv[1])
with zipfile.ZipFile(d / "build/craftmine.apworld", "w", zipfile.ZIP_DEFLATED) as z:
    for f in sorted((d / "apworld/craftmine").rglob("*")):
        if f.is_file() and "__pycache__" not in f.parts:
            z.write(f, f.relative_to(d / "apworld"))
PY
cp "$D/build/craftmine.apworld" ~/.local/share/Archipelago/worlds/craftmine.apworld && echo "built and installed build/craftmine.apworld"
