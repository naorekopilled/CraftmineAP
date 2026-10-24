from typing import Any

from BaseClasses import ItemClassification, Region
from worlds.AutoWorld import WebWorld, World

from . import names
from .data import ITEM_INGREDIENTS, TREE_UNLOCKS, unlock_item_name
from .items import INGREDIENT_ITEM, ITEM_INFO, ITEM_NAME_TO_ID, CraftmineItem
from .locations import (ADVANCEMENT_LOCATION, INGREDIENT_LOCATION, LOCATION_KEY, LOCATION_NAME_TO_ID, MINE_LOCATION,
                        NODE_LOCATION, CraftmineLocation)
from .options import CraftmineOptions
from .rules import progression_item_names, set_all_rules
from .rules_data import HUB_DOORS

PROGRESSION = progression_item_names()


class CraftmineWeb(WebWorld):
    theme = "stone"


class CraftmineWorld(World):
    """Craftmine is Minecraft's April Fools snapshot from 2025, where you build mines out of ingredients and then
    clear them. Mine ingredients and skill-tree upgrades are shuffled into the multiworld."""

    game = "Craftmine"
    web = CraftmineWeb()
    options_dataclass = CraftmineOptions
    options: CraftmineOptions
    item_name_to_id = ITEM_NAME_TO_ID
    location_name_to_id = LOCATION_NAME_TO_ID
    origin_region_name = "Menu"

    door_count = 0

    def generate_early(self) -> None:
        # without both nothing can be crafted: Crafting opens the 3x3 menu, Inventory Crafting makes the table
        self.multiworld.early_items[self.player]["Crafting"] = 1
        self.multiworld.early_items[self.player]["Inventory Crafting"] = 1
        self.door_count = HUB_DOORS if self.options.hub_door_checks else 0

    def create_regions(self) -> None:
        menu = Region("Menu", self.player, self.multiworld)
        self.multiworld.regions.append(menu)
        location_names = [INGREDIENT_LOCATION[k] for k in ITEM_INGREDIENTS]
        location_names += list(NODE_LOCATION.values())
        location_names += list(MINE_LOCATION.values())
        if self.options.advancement_checks:
            location_names += list(ADVANCEMENT_LOCATION.values())
        location_names += [names.crafter_level_location(n) for n in range(1, self.options.crafter_level_checks + 1)]
        location_names += [names.door_location(n) for n in range(1, self.door_count + 1)]
        menu.locations = [CraftmineLocation(self.player, name, LOCATION_NAME_TO_ID[name], menu)
                          for name in location_names]

    def create_item(self, name: str) -> CraftmineItem:
        if name == names.XP_BUNDLE:
            classification = ItemClassification.filler
        elif name in PROGRESSION:
            classification = ItemClassification.progression
        else:
            classification = ItemClassification.useful
        return CraftmineItem(name, classification, ITEM_NAME_TO_ID[name], self.player)

    def create_items(self) -> None:
        pool = [self.create_item(INGREDIENT_ITEM[k]) for k in ITEM_INGREDIENTS]
        pool += [self.create_item(unlock_item_name(u)) for u in TREE_UNLOCKS]
        pool += [self.create_item(names.SHIMMERING_KEY) for _ in range(self.door_count)]
        missing = len(self.multiworld.get_unfilled_locations(self.player)) - len(pool)
        pool += [self.create_filler() for _ in range(missing)]
        self.multiworld.itempool += pool

    def set_rules(self) -> None:
        set_all_rules(self)

    def get_filler_item_name(self) -> str:
        return names.XP_BUNDLE

    def fill_slot_data(self) -> dict[str, Any]:
        return {
            **self.options.as_dict("special_mines_needed", "crafter_level_checks", "death_link"),
            "goal": self.options.goal.current_key,
            "advancement_checks": bool(self.options.advancement_checks),
            "hub_door_checks": bool(self.options.hub_door_checks),
            "death_link": bool(self.options.death_link),
            "world_version": "0.1.0",
            "locations": {str(loc.address): LOCATION_KEY[loc.name]
                          for loc in self.multiworld.get_locations(self.player)},
            "items": {str(ITEM_NAME_TO_ID[name]): info for name, info in ITEM_INFO.items()},
        }
