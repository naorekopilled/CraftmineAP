from .data import UNLOCKS

_DUPLICATE_UNLOCK_NAMES = {n for n in (u["name"] for u in UNLOCKS.values())
                           if sum(u["name"] == n for u in UNLOCKS.values()) > 1}


def ingredient_location(ingredient_name: str) -> str:
    return f"Ingredient: {ingredient_name}"


def node_location(unlock_id: str) -> str:
    name = UNLOCKS[unlock_id]["name"]
    suffix = unlock_id.rsplit("_", 1)[-1]
    if name in _DUPLICATE_UNLOCK_NAMES and suffix.isdigit():
        name = f"{name} {suffix}"
    return f"Upgrade: {name}"


def special_mine_location(mine_name: str) -> str:
    return f"Special Mine: {mine_name}"


def crafter_level_location(level: int) -> str:
    return f"Mine Crafter Level {level}"


def door_location(number: int) -> str:
    return f"Hub Door {number}"


SHIMMERING_KEY = "Shimmering Key"
XP_BUNDLE = "Bag of XP"
