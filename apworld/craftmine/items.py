from BaseClasses import Item, ItemClassification

from . import names
from .data import INGREDIENTS, ITEM_INGREDIENTS, TREE_UNLOCKS, unlock_item_name

BASE_ID = 0x434D0000


class CraftmineItem(Item):
    game = "Craftmine"


INGREDIENT_ITEM = {k: INGREDIENTS[k]["name"] for k in ITEM_INGREDIENTS}
UNLOCK_ITEMS = sorted({unlock_item_name(u) for u in TREE_UNLOCKS})

ITEM_NAME_TO_ID = {name: BASE_ID + 1000 + i for i, name in enumerate(INGREDIENT_ITEM.values())}
ITEM_NAME_TO_ID.update({name: BASE_ID + 2000 + i for i, name in enumerate(UNLOCK_ITEMS)})
ITEM_NAME_TO_ID[names.SHIMMERING_KEY] = BASE_ID + 3000
ITEM_NAME_TO_ID[names.XP_BUNDLE] = BASE_ID + 3001
