import json
import pkgutil

from .rules_data import EXCLUDED_INGREDIENTS, PROGRESSIVE_CHAINS

DATA = json.loads(pkgutil.get_data(__name__, "data/craftmine.json"))

INGREDIENTS = {i["id"]: i for i in DATA["ingredients"]}
UNLOCKS = {u["id"]: u for u in DATA["unlocks"]}
SPECIAL_MINES = {m["id"]: m for m in DATA["special_mines"]}
ADVANCEMENTS = {a["id"]: a for a in DATA["advancements"]}

# Ingredients that are items (and checks): the ones the game unlocks at all, minus the bugged ones.
ITEM_INGREDIENTS = [k for k, i in INGREDIENTS.items()
                    if i["unlock_mode"] in ("UNLOCKED_BY_CONDITION", "UNLOCKED_ON_WIN")
                    and k not in EXCLUDED_INGREDIENTS]

TREE_UNLOCKS = list(UNLOCKS)

# Skill-tree nodes that are items too. School of Hard Knocks is all penalties, so its nodes are only checks: without an
# item their effects never turn on.
ITEM_UNLOCKS = [k for k, u in UNLOCKS.items() if u["tab"] != "school_of_hard_knocks"]

CHAIN_OF = {u: chain for chain, members in PROGRESSIVE_CHAINS.items() for u in members}


def unlock_item_name(unlock_id: str) -> str:
    return CHAIN_OF.get(unlock_id, UNLOCKS[unlock_id]["name"])
