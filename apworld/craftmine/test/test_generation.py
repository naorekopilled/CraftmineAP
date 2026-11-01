from . import CraftmineTestBase
from .. import names
from ..rules_data import TAB_ITEMS

BASE = 0x434D0000


class TestDefault(CraftmineTestBase):
    def test_item_and_location_counts_match(self):
        self.assertEqual(len(self.multiworld.itempool), len(self.multiworld.get_unfilled_locations(self.player)))

    def test_ids_pinned(self):
        world = self.world
        self.assertEqual(world.item_name_to_id["Amplified World"], BASE + 1000)
        self.assertEqual(world.item_name_to_id["Shimmering Key"], BASE + 3000)
        self.assertEqual(world.item_name_to_id["Bag of XP"], BASE + 3001)
        self.assertEqual(world.item_name_to_id["Combat Tab"], BASE + 3002)
        self.assertEqual(world.item_name_to_id["Pets Tab"], BASE + 3006)
        self.assertEqual(world.item_name_to_id["School of Hard Knocks Tab"], BASE + 3007)
        self.assertEqual(world.location_name_to_id["Mine Crafter Level 1"], BASE + 5001)
        self.assertEqual(world.location_name_to_id["Hub Door 1"], BASE + 6001)

    def test_slot_data_keys(self):
        self.assertEqual(set(self.world.fill_slot_data()), {
            "goal", "special_mines_needed", "advancement_checks", "crafter_level_checks", "hub_door_checks",
            "upgrade_tabs", "death_link", "world_version", "locations", "items"})

    def test_slot_data_maps(self):
        slot = self.world.fill_slot_data()
        ids = self.world.item_name_to_id
        self.assertEqual(slot["items"][str(ids["Progressive Sword"])]["keys"],
                         ["starter_sword", "starter_sword_iron", "better_starter_sword", "best_starter_sword"])
        self.assertEqual(slot["items"][str(ids["Foxes"])], {"kind": "ingredient", "keys": ["foxes"]})
        self.assertEqual(slot["items"][str(ids["Shimmering Key"])], {"kind": "key", "keys": []})
        self.assertEqual(slot["items"][str(ids["Bag of XP"])], {"kind": "xp", "keys": []})
        own = {str(loc.address) for loc in self.multiworld.get_locations(self.player)}
        self.assertEqual(set(slot["locations"]), own)
        self.assertEqual(slot["locations"][str(self.world.location_name_to_id["Ingredient: Foxes"])], "ingredient:foxes")
        self.assertEqual(slot["locations"][str(self.world.location_name_to_id["Hub Door 3"])], "door:3")

    def test_keys_in_pool(self):
        keys = [i for i in self.multiworld.itempool if i.name == "Shimmering Key"]
        self.assertEqual(len(keys), 10)

    def test_crafting_is_early(self):
        self.assertEqual(self.multiworld.early_items[self.player].get("Crafting"), 1)

    def test_goal_needs_all_special_mines(self):
        self.collect_all_but(["Wither Skeletons"])
        self.assertBeatable(False)

    def test_ids_index_the_full_game_lists(self):
        from ..data import INGREDIENTS, UNLOCKS
        self.assertEqual(self.world.location_name_to_id["Ingredient: Foxes"], BASE + 1000 + list(INGREDIENTS).index("foxes"))
        self.assertEqual(self.world.location_name_to_id["Upgrade: Combat"], BASE + 2000 + list(UNLOCKS).index("combatant"))
        self.assertEqual(self.world.item_name_to_id["Progressive Sword"],
                         BASE + 2000 + list(UNLOCKS).index("starter_sword"))
        self.assertEqual(self.world.item_name_to_id["Combat"], BASE + 2000 + list(UNLOCKS).index("combatant"))

    def test_crafting_table_route_is_early(self):
        self.assertEqual(self.multiworld.early_items[self.player].get("Inventory Crafting"), 1)

    def test_one_tab_starts_open_and_the_rest_are_in_the_pool(self):
        tabs = set(TAB_ITEMS.values())
        start = [i.name for i in self.multiworld.precollected_items[self.player]]
        self.assertEqual(len(start), 1)
        self.assertIn(start[0], tabs)
        self.assertEqual(sorted(i.name for i in self.multiworld.itempool if i.name in tabs),
                         sorted(tabs - set(start)))
        self.assertTrue(all(i.advancement for i in self.multiworld.itempool if i.name in tabs))

    def test_a_closed_tab_needs_its_item(self):
        start = self.multiworld.precollected_items[self.player][0].name
        tab = next(t for t, item in TAB_ITEMS.items() if item != start)
        location = names.node_location(tab)  # each tab's root node has the tab's id
        self.assertFalse(self.can_reach_location(location))
        self.collect_by_name(TAB_ITEMS[tab])
        self.assertTrue(self.can_reach_location(location))

    def test_school_nodes_are_checks_without_items(self):
        from ..data import UNLOCKS
        school = [u for u in UNLOCKS.values() if u["tab"] == "school_of_hard_knocks"]
        self.assertEqual(len(school), 6)
        for unlock in school:
            self.assertEqual(self.world.get_location(names.node_location(unlock["id"])).game, "Craftmine")
            self.assertNotIn(unlock["name"], self.world.item_name_to_id)

    def test_slot_data_maps_tab_items(self):
        slot = self.world.fill_slot_data()
        self.assertTrue(slot["upgrade_tabs"])
        self.assertEqual(slot["items"][str(self.world.item_name_to_id["Exploration Tab"])],
                         {"kind": "tab", "keys": ["exploration"]})


class TestUpgradeTabsOff(CraftmineTestBase):
    options = {"upgrade_tabs": False}

    def test_no_tab_items(self):
        tabs = set(TAB_ITEMS.values())
        self.assertFalse([i for i in self.multiworld.itempool if i.name in tabs])
        self.assertFalse(self.multiworld.precollected_items[self.player])

    def test_nodes_need_no_tab(self):
        self.assertTrue(self.can_reach_location(names.node_location("combatant")))


class TestAllTogglesOff(CraftmineTestBase):
    options = {"advancement_checks": False, "crafter_level_checks": 0, "hub_door_checks": False}

    def test_no_keys_or_door_checks(self):
        self.assertFalse([i for i in self.multiworld.itempool if i.name == "Shimmering Key"])
        names = {loc.name for loc in self.multiworld.get_locations(self.player)}
        self.assertFalse({n for n in names if n.startswith("Hub Door") or n.startswith("Mine Crafter Level")})

    def test_item_and_location_counts_match(self):
        self.assertEqual(len(self.multiworld.itempool), len(self.multiworld.get_unfilled_locations(self.player)))


class TestOneSpecialMine(CraftmineTestBase):
    options = {"special_mines_needed": 1}

    def test_one_mine_is_enough(self):
        self.collect_all_but(["Wither Skeletons"])
        self.assertBeatable(True)


class TestDragonGoal(CraftmineTestBase):
    options = {"goal": "ender_dragon"}

    def test_dragon_needs_end(self):
        self.collect_all_but(["End Biomes"])
        self.assertBeatable(False)


class TestMaxCrafterLevels(CraftmineTestBase):
    options = {"crafter_level_checks": 48}
