"""Hand-written logic, from the decompiled 25w14craftmine code (WorldEffects, SpecialMines, PlayerUnlocks and the
advancement files). Where the game and the wiki disagree, the game wins.

A rule is a list of alternatives; an alternative is a list of atoms that are ALL needed. `[[]]` = free.
Atoms:
- an ingredient id from craftmine.json (that ingredient's item)
- an item name, optionally `Name:count` for a progressive item
- `@special`: any special mine can be won; `@mine:<id>`: that special mine can be won
- `@node:<unlock id>`: that skill-tree node can be bought; `@tab:<tab>`: every node of that tab can be bought
- `@craft`: can use a 3x3 crafting table (Crafting, plus Inventory Crafting or Crafting table to make the table)

Mines always have the default ingredients (Surface World, Plains, Savannas, Stone, the farm animals), and the
Mine Crafter's random slots fill with "always randomizable" ingredients (zombies, skeletons, spiders, creepers,
slimes, rock types...) even before they are unlocked, so those count as present.
"""

# Never items or checks: No drops? has no way to unlock (needs to be won in a mine nothing puts it in),
# Soul Link is multiplayer only (and crashes).
EXCLUDED_INGREDIENTS = {"no_drops", "soul_link"}

# Ingredient items without a trigger check, and why.
NO_TRIGGER: dict[str, str] = {}

INGREDIENT_RULES: dict[str, list[list[str]]] = {
    # world type
    "floating_islands_world": [[]],  # win any mine
    "amplified": [["floating_islands_world"]],  # place a block at y >= 100; unlocked after Floating Islands
    "cave_world": [["floating_islands_world"]],  # break a block below y 0; unlocked after Floating Islands
    "shattered_blocks_world": [["amplified", "cave_world", "@special"]],  # win a special mine
    "grid_world": [["shattered_blocks_world", "@special"]],
    "dark_cave_world": [["grid_world", "@special"]],
    # exits
    "cave_exits": [[]],  # 1% from deepslate, or unlocking Cave World
    "rare_surface_exits": [["@craft"]],  # craft an Eye of Exit (copper + iron, 3x3)
    # biomes
    "forests": [[]],  # 1% from leaves
    "taigas": [[]],
    "jungles": [[]],
    "snowy": [["taigas", "amplified"]],  # 5% from snow or ice: snow only falls high up in a cold biome
    "desert": [[]],  # 1% from sand
    "badlands": [[]],  # 5% from terracotta or clay
    "swamps": [["badlands"]],  # break a firefly bush
    "dark_forests": [["forests", "jungles", "taigas"]],  # unlocked after those three
    "ice_spikes": [["snowy"]],  # win a mine with Snowy
    "mushroom_fields": [[]],  # 10% from mushrooms
    "nether_barrens": [["@craft"]],  # obtain obsidian (diamond pickaxe)
    "nether_forests": [["endermen"], ["@mine:enderman_boss"]],  # 20% from killing an enderman
    "deep_dark": [[]],  # 10% from breaking a block in the dark
    "end": [["blazes"]],  # 50% from killing a blaze
    # world types
    "dry_land": [["badlands", "@craft"]],  # pick up a lava bucket in a badlands biome
    "water_world": [["dry_land"]],  # win a mine with Dry Land
    # passive mobs
    "foxes": [["taigas"]],  # obtain sweet berries
    "goats": [["amplified"], ["floating_islands_world"]],  # 10% from breaking a block above y 128
    "ocelots": [[]],  # obtain string
    "axolotls": [["swamps", "@craft"]],  # obtain a bucket of axolotl (lush caves come with Swamps)
    "armadillos": [["@craft"]],  # craft a brush
    "mooshrooms": [[]],  # 10% from mushrooms
    "pandas": [["jungles"]],  # eat bamboo
    "parrots": [["jungles", "@craft"]],  # eat a cookie
    "rabbits": [[]],  # take more than 10 fall damage
    "sniffers": [[]],  # 5% from flowers
    "striders": [[]],  # obtain a saddle (village leatherworker)
    # hostile mobs
    "zombies": [[]],  # 5% from killing any monster
    "skeletons": [[]],
    "spiders": [[]],
    "creepers": [[]],
    "slimes": [[]],
    "cave_spiders": [["spiders"]],  # 20% from killing spiders
    "endermen": [["zombies", "skeletons", "creepers", "spiders"]],  # unlocked after those four
    "witches": [["zombies", "skeletons", "creepers", "spiders"]],
    "magma_cubes": [["nether_barrens"]],  # obtain magma cream or a magma block
    "blazes": [["nether_barrens"], ["nether_forests"]],  # break nether quartz or nether gold ore
    "breezes": [[]],  # obtain a trial key (trial chambers)
    "pillagers": [["@craft"]],  # traitor villager trades (lodestone), novice
    "vindicators": [["@craft"]],  # apprentice
    "evokers": [["@craft"]],  # journeyman
    "ravagers": [["@craft"]],  # expert
    "illusioners": [["@craft"]],  # master
    "guardians": [[]],  # kill an aquatic mob
    "endermites": [["end"], ["base_end_stone"]],  # 10% from breaking end stone
    "shulkers": [["endermites"]],  # 10% from killing an endermite
    "ghasts": [["desert"], ["swamps"]],  # break a bone block (fossils)
    "zombified_piglins": [[]],  # kill a pig that is on fire
    "piglins": [["@craft"]],  # feed a golden carrot to a pig
    "wither_skeletons": [[]],  # 10% from killing a skeleton
    "bees": [[]],  # kill a bee
    "hoglins": [[]],  # 1% from killing a pig
    "zoglins": [["zombies", "nether_forests"]],  # kill a zombie standing on nylium
    # rock types (10% from breaking that rock; most also appear in random slots)
    "base_blackstone": [["nether_barrens"]],
    "base_diorite": [[]],
    "base_andesite": [[]],
    "base_granite": [[]],
    "base_tuff": [[]],
    "base_deepslate": [[]],
    "base_end_stone": [["end", "@craft"], ["end", "Starting pickaxe"]],  # end stone has to drop
    # challenges
    "icy": [["@craft"], ["Progressive Axe:2"]],  # use a boat (Viking gives one)
    "bouncy": [[]],  # kill a slime or magma cube
    "one_hp": [[]],  # kill a mob while at half a heart
    "wednesday_frogs": [[]],  # kill a frog
    "universal_anger": [["bees"]],  # win a mine with Bees
    "eternal_night": [["@mine:enderman_boss"]],  # only comes from winning the Enderman mine (it's in it)
    "eternal_rain": [["eternal_night"]],  # win a mine with Eternal Night
    "eternal_lightning": [["eternal_rain"]],
    "insomniacs": [["eternal_night"]],
    "ultrawarm": [[]],  # take lava damage
    "explosive_traps": [[]],  # win a mine once more than 10 mines exist
    "fish_out_of_water": [["@craft"], ["Angler"]],  # use a fishing rod
    # special event
    "warden_boss_fight": [[]],  # break amethyst
}

# placeholder: progressive item names are not the game's own text
PROGRESSIVE_CHAINS: dict[str, list[str]] = {
    "Progressive Jump": ["jump_king"] + [f"jump_king_{i}" for i in range(2, 11)],
    "Progressive Learning": ["learning_1"] + [f"learning_{i}" for i in range(2, 21)],
    "Progressive Pockets": ["inventory_slots_1", "inventory_slots_2", "inventory_slots_3"],
    "Progressive Sword": ["starter_sword", "starter_sword_iron", "better_starter_sword", "best_starter_sword"],
    "Progressive Axe": ["chopper", "viking", "berserker", "varangian"],
    "Progressive Armor": ["decked_out", "full_metal"],
    "Progressive Apples": ["starter_apples", "more_starter_apples", "golden_starter_apples"],
    "Progressive Arrows": ["quiver", "fletcher"],
    "Progressive Smelter": ["smelter_1", "smelter_2"],
    "Progressive Magic Kit": ["magic_starter_kit", "magic_master_kit", "runemaster"],
    "Progressive Shovel": ["apprentice_shoveler", "journeyman_shoveler", "expert_shoveler", "master_shoveler"],
    "Progressive Dirt": ["dirt_enjoyer_1", "dirt_enjoyer_2", "dirt_enjoyer_3", "dirt_destroyer_1", "dirt_destroyer_2",
                         "dirt_connoisseur"],
    "Progressive Pearls": ["ender_pearl_starter", "more_pearls", "more_more_pearls"],
    "Progressive Speed": ["speed_1", "speed_2", "speed_3"],
    "Progressive Mining Efficiency": ["mining_efficiency", "mining_efficiency_2", "mining_efficiency_3"],
    "Progressive Sneak": ["sneak_1", "sneak_2"],
}

# placeholder: the game has no such items. With upgrade_tabs on, a tab's nodes can't be bought until its item arrives.
TAB_ITEMS: dict[str, str] = {
    "combatant": "Combat Tab",
    "crafting": "Crafting Tab",
    "exploration": "Exploration Tab",
    "gatherer": "Gatherer Tab",
    "pets": "Pets Tab",
    "school_of_hard_knocks": "School of Hard Knocks Tab",
}

# Skill-tree nodes the game hides until something happens (PlayerUnlock.becomesVisibleWhen, other than "parent
# bought"). "isUnlocked(Spellbook)" in the game = the Spellbook node bought.
HIDDEN_UNLOCK_RULES: dict[str, list[list[str]]] = {
    "ender_pearl_starter": [["endermen"], ["@mine:enderman_boss"]],  # kill an enderman
    "mega_jump": [["desert"], ["taigas"], ["snowy"]],  # kill a rabbit (they live in those biomes)
    "inventory_crafting_3x3": [["@craft"], ["Inventory Crafting"]],  # craft anything
    "crafting_efficiency": [["Inventory Crafting"]],  # craft a crafting table (2x2)
    "fire_wand": [["blazes"]],  # kill a blaze
    "wind_wand": [["breezes"]],  # kill a breeze
    "teleportation_wand": [["endermen"], ["@mine:enderman_boss"]],  # kill an enderman
    "sub_mining_efficiency": [["water_world"]],  # win a Water World mine
    "wing_guardian_lift_off_to_the_sky": [["@node:sorcerer_supreme"]],  # more than 10 fall damage with Spellbook
    "dragon_fire": [["@node:sorcerer_supreme", "@mine:ender_dragon_boss"]],  # dragon fireball damage with Spellbook
    "fire_shield": [["@node:sorcerer_supreme"]],  # fire damage with Spellbook
}

# Nodes the game never shows, so they can't be bought: MEGA JUMP needs a rabbit kill but mines replace every
# creature spawn with the ingredients' mobs and the Rabbits ingredient spawns parrots; Dragon Fire needs damage whose
# entity is a dragon fireball, but the fireball's cloud credits the dragon. Safe Falling sits under MEGA JUMP.
# Their items still exist.
NO_CHECK_NODES = {"mega_jump", "safe_falling", "dragon_fire"}

_PICKAXE = [["@craft"], ["Starting pickaxe"]]

_GEAR_1 = ["Combat", "Armaments", "Progressive Armor:1", "Progressive Sword:2"]
_GEAR_2 = ["Combat", "Armaments", "Progressive Armor:2", "Progressive Sword:3", "Progressive Apples:2"]

# Winning each special mine: what unlocks it (SpecialMines) plus gear to survive it. The game picks every 5th mine
# from the unlocked, not yet won ones, so an unlocked one comes up by itself.
SPECIAL_MINE_RULES: dict[str, list[list[str]]] = {
    "enderman_boss": [_GEAR_1],  # unlocked from the start
    "kuiper_belt": [_GEAR_1],
    "small_but_deadly_boss": [_GEAR_1],
    "spooky_scary_skeletons_boss": [_GEAR_1],  # kill a skeleton
    "angry_ghast_boss": [["ghasts"] + _GEAR_1],  # kill a ghast with its own fireball
    "raid": [["pillagers"] + _GEAR_2],  # kill a pillager
    "wither_boss": [["wither_skeletons"] + _GEAR_2],  # obtain a wither skeleton skull
    "ender_dragon_boss": [["end", "Archer"] + _GEAR_2],  # win a mine with End Biomes
    "warden_boss": [_GEAR_2],  # break amethyst
}

# Same action as a special mine or ingredient check, impossible, or multiplayer only.
SKIPPED_ADVANCEMENTS = {
    "feats/angry_ghast", "feats/ender_dragon", "feats/enderman", "feats/kuiper_world", "feats/raid",
    "feats/small_but_deadly", "feats/spooky_scary_skeletons", "feats/warden", "feats/wither",
    "mines/get_mine_ingredient", "mines/cash_in_mine_ingredient",
    "mines/all_mine_ingredients",  # needs every ingredient, No drops? and Soul Link included
    "unlocks/in_it_together",  # Soul Link
    "unlocks/all_exploration", "unlocks/all_crafting",  # need MEGA JUMP, Safe Falling, Dragon Fire (NO_CHECK_NODES)
}

# "Shimmering Key" is swapped for [[]] by the world when hub door checks are off (keys stay vanilla then).
ADVANCEMENT_RULES: dict[str, list[list[str]]] = {
    "feats/crystal": _PICKAXE,
    "feats/dragon_breath": [["@mine:ender_dragon_boss", "@craft"]],
    "feats/end_world": [["end"]],
    "feats/kill_pillager": [["pillagers"]],
    "feats/kill_skeleton": [[]],
    "feats/return_to_sender": [["ghasts"]],
    "feats/wither_skeleton_skull": [["wither_skeletons"]],
    "mines/all_special_mines_completed": [[f"@mine:{m}" for m in SPECIAL_MINE_RULES]],
    "mines/complete_deluge_level": [["eternal_rain", "water_world"]],
    "mines/complete_water_cave_level": [["water_world", "cave_world"]],
    "mines/complete_water_world_level": [["water_world"]],
    "mines/eat_your_veggies": [[]],
    "mines/enter_a_mine": [[]],
    "mines/level_completed": [[]],
    "mines/mine_crafter_upgraded": [[]],
    "mines/mine_revisitor_activated": [[]],
    "mines/player_failed_level": [[]],
    "mines/shimmering_key": [["Shimmering Key"]],
    "mines/special_mine_completed": [["@special"]],
    "story/deflect_arrow": [["Warded"], ["@craft"]],
    "story/follow_exit_eye": [["@craft"]],
    "story/iron_tools": [["@craft"], ["Starting pickaxe"]],
    "story/mine_diamond": _PICKAXE,
    "story/mine_stone": _PICKAXE,
    "story/obtain_armor": [["@craft"], ["Progressive Armor:2"]],
    "story/shiny_gear": [["@craft"]],
    "story/smelt_iron": [["@craft"], ["Progressive Smelter"]],
    "story/upgrade_tools": [["@craft"]],
    "unlocks/all_combatant": [["@tab:combatant", "@special"]],  # checked when a special mine is won
    "unlocks/all_gatherer": [["@tab:gatherer", "@special"]],
    "unlocks/all_school_of_hard_knocks": [["@tab:school_of_hard_knocks", "@special"]],
    "unlocks/free_sticks": [["@node:fire_wand", "@node:wind_wand", "@node:teleportation_wand"]],
    "unlocks/how_do_i_unlock_that": [["@craft"], ["Inventory Crafting"]],  # any hidden node shows up
    "unlocks/jump_king": [["@node:jump_king_10"]],
    "unlocks/kaizen": [[]],
    "unlocks/sigma_grindset": [["@node:learning_20"]],
}

HUB_DOORS = 10
