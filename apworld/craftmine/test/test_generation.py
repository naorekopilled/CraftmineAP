from . import CraftmineTestBase

BASE = 0x434D0000


class TestDefault(CraftmineTestBase):
    def test_item_and_location_counts_match(self):
        self.assertEqual(len(self.multiworld.itempool), len(self.multiworld.get_unfilled_locations(self.player)))

    def test_ids_pinned(self):
        world = self.world
        self.assertEqual(world.item_name_to_id["Amplified World"], BASE + 1000)
        self.assertEqual(world.item_name_to_id["Shimmering Key"], BASE + 3000)
        self.assertEqual(world.item_name_to_id["XP Bundle"], BASE + 3001)
        self.assertEqual(world.location_name_to_id["Mine Crafter Level 1"], BASE + 5001)
        self.assertEqual(world.location_name_to_id["Open Hub Door 1"], BASE + 6001)

    def test_slot_data_keys(self):
        self.assertEqual(set(self.world.fill_slot_data()), {
            "goal", "special_mines_needed", "advancement_checks", "crafter_level_checks", "hub_door_checks",
            "death_link", "world_version", "locations", "items"})

    def test_slot_data_maps(self):
        slot = self.world.fill_slot_data()
        ids = self.world.item_name_to_id
        self.assertEqual(slot["items"][str(ids["Progressive Sword"])]["keys"],
                         ["starter_sword", "starter_sword_iron", "better_starter_sword", "best_starter_sword"])
        self.assertEqual(slot["items"][str(ids["Foxes"])], {"kind": "ingredient", "keys": ["foxes"]})
        self.assertEqual(slot["items"][str(ids["Shimmering Key"])], {"kind": "key", "keys": []})
        self.assertEqual(slot["items"][str(ids["XP Bundle"])], {"kind": "xp", "keys": []})
        own = {str(loc.address) for loc in self.multiworld.get_locations(self.player)}
        self.assertEqual(set(slot["locations"]), own)
        self.assertEqual(slot["locations"][str(self.world.location_name_to_id["Unlock Foxes"])], "ingredient:foxes")
        self.assertEqual(slot["locations"][str(self.world.location_name_to_id["Open Hub Door 3"])], "door:3")

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
        self.assertEqual(self.world.location_name_to_id["Unlock Foxes"], BASE + 1000 + list(INGREDIENTS).index("foxes"))
        self.assertEqual(self.world.location_name_to_id["Buy Combat"], BASE + 2000 + list(UNLOCKS).index("combatant"))
        self.assertEqual(self.world.item_name_to_id["Progressive Sword"],
                         BASE + 2000 + list(UNLOCKS).index("starter_sword"))
        self.assertEqual(self.world.item_name_to_id["Combat"], BASE + 2000 + list(UNLOCKS).index("combatant"))

    def test_crafting_table_route_is_early(self):
        self.assertEqual(self.multiworld.early_items[self.player].get("Inventory Crafting"), 1)


class TestAllTogglesOff(CraftmineTestBase):
    options = {"advancement_checks": False, "crafter_level_checks": 0, "hub_door_checks": False}

    def test_no_keys_or_door_checks(self):
        self.assertFalse([i for i in self.multiworld.itempool if i.name == "Shimmering Key"])
        names = {loc.name for loc in self.multiworld.get_locations(self.player)}
        self.assertFalse({n for n in names if n.startswith("Open Hub Door") or n.startswith("Mine Crafter Level")})

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
