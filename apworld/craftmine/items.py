from BaseClasses import Item, ItemClassification

from . import names
from .data import CHAIN_OF, INGREDIENTS, ITEM_INGREDIENTS, TREE_UNLOCKS, UNLOCKS, unlock_item_name
from .rules_data import PROGRESSIVE_CHAINS

BASE_ID = 0x434D0000


class CraftmineItem(Item):
    game = "Craftmine"


INGREDIENT_ITEM = {k: INGREDIENTS[k]["name"] for k in ITEM_INGREDIENTS}

# ids index the full dumped lists (which never change), so excluding something or adding a chain keeps every other id;
# a chain takes the id of its first unlock
_INGREDIENT_INDEX = {k: i for i, k in enumerate(INGREDIENTS)}
_UNLOCK_INDEX = {k: i for i, k in enumerate(UNLOCKS)}
ITEM_NAME_TO_ID = {name: BASE_ID + 1000 + _INGREDIENT_INDEX[k] for k, name in INGREDIENT_ITEM.items()}
ITEM_NAME_TO_ID.update({unlock_item_name(u): BASE_ID + 2000 + _UNLOCK_INDEX[PROGRESSIVE_CHAINS[CHAIN_OF[u]][0] if u in CHAIN_OF else u]
                        for u in TREE_UNLOCKS})
ITEM_NAME_TO_ID[names.SHIMMERING_KEY] = BASE_ID + 3000
ITEM_NAME_TO_ID[names.XP_BUNDLE] = BASE_ID + 3001

# what the mod grants for each item: chains list their unlocks in order, the n-th copy grants the first n
ITEM_INFO = {name: {"kind": "ingredient", "keys": [k]} for k, name in INGREDIENT_ITEM.items()}
ITEM_INFO.update({unlock_item_name(u): {"kind": "unlock", "keys": PROGRESSIVE_CHAINS.get(unlock_item_name(u), [u])}
                  for u in TREE_UNLOCKS})
ITEM_INFO[names.SHIMMERING_KEY] = {"kind": "key", "keys": []}
ITEM_INFO[names.XP_BUNDLE] = {"kind": "xp", "keys": []}
