"""The rules as plain data for the mod's in-game tracker, which ships them as craftmineap/logic.json.

Same rule format as rules_data.py, with ingredient ids turned into their item names. The mod evaluates it the way
rules.RuleBuilder does; test_logic_export keeps the shipped file and the mod's test fixture current.
"""
from . import names
from .data import ADVANCEMENTS, INGREDIENTS, SPECIAL_MINES, TREE_UNLOCKS, UNLOCKS
from .items import INGREDIENT_ITEM, ITEM_NAME_TO_ID
from .locations import LOCATION_KEY
from .rules import learning_needed
from .rules_data import ADVANCEMENT_RULES, HIDDEN_UNLOCK_RULES, INGREDIENT_RULES, SPECIAL_MINE_RULES

# a special mine's icon is its own ingredient, not one it shares with the others
_SHARED_MINE_EFFECTS = {"event_exit", "floating_islands_world", "surface_world", "cave_world", "plains", "desert"}


def _atom(atom: str) -> str:
    if atom.startswith("@"):
        return atom
    name, sep, count = atom.partition(":")
    return INGREDIENT_ITEM.get(name, name) + sep + count


def _alts(alts: list[list[str]]) -> list[list[str]]:
    return [[_atom(a) for a in alt] for alt in alts]


def _location(name: str, key: str) -> dict:
    kind, _, arg = key.partition(":")
    entry: dict = {"name": name}
    if kind == "ingredient":
        entry["rule"] = _alts(INGREDIENT_RULES[arg])
        entry["after"] = INGREDIENTS[arg]["unlocked_after"]
    elif kind == "node":
        entry["rule"] = [[f"@node:{arg}"]]
    elif kind == "mine":
        entry["rule"] = [[f"@mine:{arg}"]]
        effects = SPECIAL_MINES[arg]["required_effects"]
        entry["icon"] = next((e for e in effects if e not in _SHARED_MINE_EFFECTS), effects[0])
    elif kind == "advancement":
        entry["rule"] = _alts(ADVANCEMENT_RULES[arg])
        entry["parent"] = ADVANCEMENTS[arg]["parent"]
    elif kind == "door":
        entry["rule"] = [[f"{names.SHIMMERING_KEY}:{arg}"]]
    else:  # crafter levels only need mines, which logic doesn't count
        entry["rule"] = [[]]
    return entry


def export() -> dict:
    return {
        "items": {str(item_id): name for name, item_id in ITEM_NAME_TO_ID.items()},
        "locations": {key: _location(name, key) for name, key in LOCATION_KEY.items()},
        "mines": {k: _alts(SPECIAL_MINE_RULES[k]) for k in SPECIAL_MINES},
        "nodes": {k: {"parent": UNLOCKS[k]["parent"], "tab": UNLOCKS[k]["tab"],
                      "hidden": _alts(HIDDEN_UNLOCK_RULES.get(k, [[]])),
                      "learning": learning_needed(UNLOCKS[k]["cost"])}
                  for k in TREE_UNLOCKS},
    }
