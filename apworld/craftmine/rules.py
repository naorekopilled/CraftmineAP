import re
from typing import TYPE_CHECKING, Callable

from BaseClasses import CollectionState
from worlds.generic.Rules import set_rule

from . import names
from .data import ITEM_INGREDIENTS, SPECIAL_MINES, TREE_UNLOCKS, UNLOCKS
from .items import INGREDIENT_ITEM
from .locations import ADVANCEMENT_LOCATION, INGREDIENT_LOCATION, MINE_LOCATION, NODE_LOCATION
from .rules_data import (ADVANCEMENT_RULES, HIDDEN_UNLOCK_RULES, INGREDIENT_RULES, SPECIAL_MINE_RULES)

if TYPE_CHECKING:
    from . import CraftmineWorld

Rule = Callable[[CollectionState], bool]


def learning_needed(cost: int) -> int:
    """Expensive skill-tree nodes stay out of logic until the XP boosts make them a reasonable grind."""
    if cost >= 50:
        return 5
    if cost >= 30:
        return 2
    return 0


def progression_item_names() -> set[str]:
    """Every item some rule asks for."""
    found = {"Progressive Learning", names.SHIMMERING_KEY}
    for table in (INGREDIENT_RULES, HIDDEN_UNLOCK_RULES, SPECIAL_MINE_RULES, ADVANCEMENT_RULES):
        for alts in table.values():
            for alt in alts:
                for atom in alt:
                    if not atom.startswith("@"):
                        name = re.sub(r":\d+$", "", atom)
                        found.add(INGREDIENT_ITEM.get(name, name))
    return found


class RuleBuilder:
    def __init__(self, world: "CraftmineWorld"):
        self.player = world.player
        self.doors_on = bool(world.options.hub_door_checks)
        self.mines: dict[str, Rule] = {k: self.alternatives(SPECIAL_MINE_RULES[k]) for k in SPECIAL_MINES}
        self.nodes: dict[str, Rule] = {k: self.node(k) for k in TREE_UNLOCKS}

    def atom(self, atom: str) -> Rule:
        player = self.player
        if atom.startswith("@"):
            kind, _, arg = atom[1:].partition(":")
            if kind == "special":
                return lambda state: any(rule(state) for rule in self.mines.values())
            if kind == "mine":
                return lambda state: self.mines[arg](state)
            if kind == "node":
                return lambda state: self.nodes[arg](state)
            tab = [u for u in TREE_UNLOCKS if UNLOCKS[u]["tab"] == arg]
            return lambda state: all(self.nodes[u](state) for u in tab)
        name, _, count = atom.partition(":")
        name = INGREDIENT_ITEM.get(name, name)
        if name == names.SHIMMERING_KEY and not self.doors_on:
            return lambda state: True
        amount = int(count or 1)
        return lambda state: state.has(name, player, amount)

    def alternatives(self, alts: list[list[str]]) -> Rule:
        compiled = [[self.atom(a) for a in alt] for alt in alts]
        return lambda state: any(all(rule(state) for rule in alt) for alt in compiled)

    def node(self, unlock_id: str) -> Rule:
        unlock = UNLOCKS[unlock_id]
        parent = unlock["parent"]
        hidden = self.alternatives(HIDDEN_UNLOCK_RULES.get(unlock_id, [[]]))
        learning = learning_needed(unlock["cost"])
        player = self.player
        return lambda state: ((parent is None or self.nodes[parent](state)) and hidden(state)
                              and (learning == 0 or state.has("Progressive Learning", player, learning)))


def set_all_rules(world: "CraftmineWorld") -> None:
    builder = RuleBuilder(world)
    get = world.get_location
    for key in ITEM_INGREDIENTS:
        set_rule(get(INGREDIENT_LOCATION[key]), builder.alternatives(INGREDIENT_RULES[key]))
    for key in TREE_UNLOCKS:
        set_rule(get(NODE_LOCATION[key]), builder.nodes[key])
    for key, rule in builder.mines.items():
        set_rule(get(MINE_LOCATION[key]), rule)
    if world.options.advancement_checks:
        for key, name in ADVANCEMENT_LOCATION.items():
            set_rule(get(name), builder.alternatives(ADVANCEMENT_RULES[key]))
    if world.options.hub_door_checks:
        for n in range(1, world.door_count + 1):
            set_rule(get(names.door_location(n)), lambda state, n=n: state.has(names.SHIMMERING_KEY, world.player, n))

    if world.options.goal == "ender_dragon":
        world.multiworld.completion_condition[world.player] = builder.mines["ender_dragon_boss"]
    else:
        needed = world.options.special_mines_needed.value
        mine_rules = list(builder.mines.values())
        world.multiworld.completion_condition[world.player] = \
            lambda state: sum(rule(state) for rule in mine_rules) >= needed
