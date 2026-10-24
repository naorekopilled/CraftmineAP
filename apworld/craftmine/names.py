# placeholder: every location name pattern here is to be rewritten by the user
from .data import UNLOCKS

_DUPLICATE_UNLOCK_NAMES = {n for n in (u["name"] for u in UNLOCKS.values())
                           if sum(u["name"] == n for u in UNLOCKS.values()) > 1}


def ingredient_location(ingredient_name: str) -> str:
    return f"Unlock {ingredient_name}"  # placeholder


def node_location(unlock_id: str) -> str:
    name = UNLOCKS[unlock_id]["name"]
    suffix = unlock_id.rsplit("_", 1)[-1]
    if name in _DUPLICATE_UNLOCK_NAMES and suffix.isdigit():
        name = f"{name} {suffix}"
    return f"Buy {name}"  # placeholder


def special_mine_location(mine_name: str) -> str:
    return f"Win {mine_name}"  # placeholder


def crafter_level_location(level: int) -> str:
    return f"Mine Crafter Level {level}"  # placeholder


def door_location(number: int) -> str:
    return f"Open Hub Door {number}"  # placeholder


SHIMMERING_KEY = "Shimmering Key"
XP_BUNDLE = "XP Bundle"  # placeholder
