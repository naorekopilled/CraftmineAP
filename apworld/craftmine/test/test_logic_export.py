import json
import os
import random
from pathlib import Path

from BaseClasses import CollectionState

from . import CraftmineTestBase
from ..locations import LOCATION_KEY
from ..logic_export import export

# the mod ships the export, and its tests check its own logic against what the real rules say for these states
MOD = Path(os.path.realpath(__file__)).parents[3] / "mod" / "src"
LOGIC_FILE = MOD / "main" / "resources" / "craftmineap" / "logic.json"
FIXTURE_FILE = MOD / "test" / "resources" / "logic_fixture.json"
# CRAFTMINE_WRITE_LOGIC=1 rewrites both files instead of comparing (tools/export_logic.sh)
WRITE = os.environ.get("CRAFTMINE_WRITE_LOGIC") == "1"


def _dump(data) -> str:
    return json.dumps(data, indent=1, sort_keys=True) + "\n"


class TestLogicExport(CraftmineTestBase):
    def test_mod_ships_the_current_export(self):
        text = _dump(export())
        if WRITE:
            LOGIC_FILE.parent.mkdir(parents=True, exist_ok=True)
            LOGIC_FILE.write_text(text)
        self.assertEqual(LOGIC_FILE.read_text(), text, "run tools/export_logic.sh")

    def fixture(self) -> dict:
        rng = random.Random(7)
        counts: dict[str, int] = {}
        for item in self.multiworld.itempool:
            if item.advancement:
                counts[item.name] = counts.get(item.name, 0) + 1
        locations = list(self.multiworld.get_locations(self.player))
        states = []
        for _ in range(60):
            chance = rng.random()
            have = {name: rng.randint(1, most) for name, most in sorted(counts.items()) if rng.random() < chance}
            state = CollectionState(self.multiworld)
            for name, amount in have.items():
                for _ in range(amount):
                    state.collect(self.world.create_item(name), prevent_sweep=True)
            states.append({"have": have,
                           "in_logic": sorted(LOCATION_KEY[loc.name] for loc in locations if loc.can_reach(state))})
        return {"doors_on": bool(self.world.options.hub_door_checks),
                "locations": sorted(LOCATION_KEY[loc.name] for loc in locations),
                "states": states}

    def test_fixture_matches_the_real_rules(self):
        text = _dump(self.fixture())
        if WRITE:
            FIXTURE_FILE.parent.mkdir(parents=True, exist_ok=True)
            FIXTURE_FILE.write_text(text)
        self.assertEqual(FIXTURE_FILE.read_text(), text, "run tools/export_logic.sh")

    def test_fixture_states_vary(self):
        sizes = {len(s["in_logic"]) for s in self.fixture()["states"]}
        self.assertGreater(len(sizes), 20)
