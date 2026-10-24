#!/bin/bash
# A real Fabric server for 25w14craftmine in tools-ext/prod-server, for PROD=1 tools/smoke.sh.
set -e
P=$(cd "$(dirname "$0")/.." && pwd)/tools-ext/prod-server
mkdir -p "$P/mods"
curl -s -o "$P/fabric-server-launch.jar" "https://meta.fabricmc.net/v2/versions/loader/25w14craftmine/0.16.14/1.0.3/server/jar"
curl -s -o "$P/mods/fabric-api.jar" \
  "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.10+25w14craftmine/fabric-api-0.119.10+25w14craftmine.jar"
