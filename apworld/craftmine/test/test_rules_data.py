import json
import re
import unittest
from pathlib import Path

from .. import rules_data

DATA = json.loads((Path(__file__).parent.parent / "data" / "craftmine.json").read_text())
INGREDIENTS = {i["id"]: i for i in DATA["ingredients"]}
UNLOCKS = {u["id"]: u for u in DATA["unlocks"]}
MINES = {m["id"] for m in DATA["special_mines"]}
TABS = {u["tab"] for u in DATA["unlocks"]} - {"school_of_hard_knocks"}


def item_ingredients():
    return {k for k, i in INGREDIENTS.items()
            if i["unlock_mode"] in ("UNLOCKED_BY_CONDITION", "UNLOCKED_ON_WIN")} - rules_data.EXCLUDED_INGREDIENTS


def unlock_item_names():
    chained = {u for chain in rules_data.PROGRESSIVE_CHAINS.values() for u in chain}
    singles = {u["name"] for u in UNLOCKS.values()
               if u["id"] not in chained and u["tab"] != "school_of_hard_knocks"}
    return singles | set(rules_data.PROGRESSIVE_CHAINS)


def all_rules():
    tables = [rules_data.INGREDIENT_RULES, rules_data.HIDDEN_UNLOCK_RULES,
              rules_data.SPECIAL_MINE_RULES, rules_data.ADVANCEMENT_RULES]
    return [(key, alt) for table in tables for key, alts in table.items() for alt in alts]


class TestRulesData(unittest.TestCase):
    def test_every_item_ingredient_has_a_rule_or_reason(self):
        for key in item_ingredients():
            self.assertTrue(key in rules_data.INGREDIENT_RULES or key in rules_data.NO_TRIGGER, key)

    def test_bugged_ingredients_excluded(self):
        self.assertIn("no_drops", rules_data.EXCLUDED_INGREDIENTS)
        self.assertIn("soul_link", rules_data.EXCLUDED_INGREDIENTS)
        for key in rules_data.EXCLUDED_INGREDIENTS:
            self.assertNotIn(key, rules_data.INGREDIENT_RULES)

    def test_every_atom_resolves(self):
        names = unlock_item_names() | {"Shimmering Key"}
        for key, alt in all_rules():
            for atom in alt:
                if atom.startswith("@"):
                    kind, _, arg = atom[1:].partition(":")
                    ok = {"special": arg == "", "mine": arg in MINES, "node": arg in UNLOCKS,
                          "tab": arg in TABS}.get(kind, False)
                else:
                    name = re.sub(r":\d+$", "", atom)
                    ok = name in item_ingredients() or name in names
                self.assertTrue(ok, f"{key}: {atom}")

    def test_chain_counts_fit(self):
        for key, alt in all_rules():
            for atom in alt:
                m = re.fullmatch(r"(.+):(\d+)", atom)
                if m and m.group(1) in rules_data.PROGRESSIVE_CHAINS:
                    self.assertLessEqual(int(m.group(2)), len(rules_data.PROGRESSIVE_CHAINS[m.group(1)]), atom)

    def test_no_unlock_in_two_chains(self):
        seen = [u for chain in rules_data.PROGRESSIVE_CHAINS.values() for u in chain]
        self.assertEqual(len(seen), len(set(seen)))
        self.assertTrue(set(seen) <= set(UNLOCKS))

    def test_unlock_item_names_unique(self):
        chained = {u for chain in rules_data.PROGRESSIVE_CHAINS.values() for u in chain}
        singles = [u["name"] for u in UNLOCKS.values()
                   if u["id"] not in chained and u["tab"] != "school_of_hard_knocks"]
        self.assertEqual(len(singles), len(set(singles)))

    def test_hidden_unlocks_have_rules(self):
        for key in ("mega_jump", "fire_wand", "wind_wand", "teleportation_wand", "ender_pearl_starter",
                    "fire_shield", "dragon_fire", "wing_guardian_lift_off_to_the_sky",
                    "sub_mining_efficiency", "inventory_crafting_3x3", "crafting_efficiency"):
            self.assertIn(key, rules_data.HIDDEN_UNLOCK_RULES)

    def test_every_special_mine_has_a_rule(self):
        self.assertEqual(set(rules_data.SPECIAL_MINE_RULES), MINES)

    def test_advancements_covered(self):
        ids = {a["id"] for a in DATA["advancements"]}
        self.assertEqual(set(rules_data.ADVANCEMENT_RULES) | rules_data.SKIPPED_ADVANCEMENTS, ids)
        self.assertFalse(set(rules_data.ADVANCEMENT_RULES) & rules_data.SKIPPED_ADVANCEMENTS)


if __name__ == "__main__":
    unittest.main()
