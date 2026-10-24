from BaseClasses import Location

from . import names
from .data import ADVANCEMENTS, INGREDIENTS, ITEM_INGREDIENTS, SPECIAL_MINES, TREE_UNLOCKS, UNLOCKS
from .items import BASE_ID
from .rules_data import HUB_DOORS, NO_CHECK_NODES, SKIPPED_ADVANCEMENTS


class CraftmineLocation(Location):
    game = "Craftmine"


INGREDIENT_LOCATION = {k: names.ingredient_location(INGREDIENTS[k]["name"]) for k in ITEM_INGREDIENTS}
NODE_LOCATION = {k: names.node_location(k) for k in TREE_UNLOCKS if k not in NO_CHECK_NODES}
MINE_LOCATION = {k: names.special_mine_location(m["name"]) for k, m in SPECIAL_MINES.items()}
ADVANCEMENT_LOCATION = {k: a["name"] for k, a in ADVANCEMENTS.items() if k not in SKIPPED_ADVANCEMENTS}

# ids index the full dumped lists, skipped entries included, so skipping or excluding one keeps every other id
_INGREDIENT_INDEX = {k: i for i, k in enumerate(INGREDIENTS)}
_UNLOCK_INDEX = {k: i for i, k in enumerate(UNLOCKS)}
LOCATION_NAME_TO_ID = {name: BASE_ID + 1000 + _INGREDIENT_INDEX[k] for k, name in INGREDIENT_LOCATION.items()}
LOCATION_NAME_TO_ID.update({name: BASE_ID + 2000 + _UNLOCK_INDEX[k] for k, name in NODE_LOCATION.items()})
LOCATION_NAME_TO_ID.update({a["name"]: BASE_ID + 3000 + i for i, (k, a) in enumerate(ADVANCEMENTS.items())
                            if k not in SKIPPED_ADVANCEMENTS})
LOCATION_NAME_TO_ID.update({name: BASE_ID + 4000 + i for i, name in enumerate(MINE_LOCATION.values())})
LOCATION_NAME_TO_ID.update({names.crafter_level_location(n): BASE_ID + 5000 + n for n in range(1, 49)})
LOCATION_NAME_TO_ID.update({names.door_location(n): BASE_ID + 6000 + n for n in range(1, HUB_DOORS + 1)})

# what the mod sees: location name -> "<kind>:<game key>"
LOCATION_KEY = {name: f"ingredient:{k}" for k, name in INGREDIENT_LOCATION.items()}
LOCATION_KEY.update({name: f"node:{k}" for k, name in NODE_LOCATION.items()})
LOCATION_KEY.update({name: f"advancement:{k}" for k, name in ADVANCEMENT_LOCATION.items()})
LOCATION_KEY.update({name: f"mine:{k}" for k, name in MINE_LOCATION.items()})
LOCATION_KEY.update({names.crafter_level_location(n): f"crafter:{n}" for n in range(1, 49)})
LOCATION_KEY.update({names.door_location(n): f"door:{n}" for n in range(1, HUB_DOORS + 1)})
