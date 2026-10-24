from dataclasses import dataclass

from Options import Choice, DeathLink, DefaultOnToggle, PerGameCommonOptions, Range


class Goal(Choice):
    """placeholder: What wins the game. special_mines: win a number of special event mines.
    ender_dragon: win the Ender Dragon mine."""
    display_name = "Goal"
    option_special_mines = 0
    option_ender_dragon = 1
    default = 0


class SpecialMinesNeeded(Range):
    """placeholder: How many different special event mines to win, with the special_mines goal."""
    display_name = "Special Mines Needed"
    range_start = 1
    range_end = 9
    default = 9


class AdvancementChecks(DefaultOnToggle):
    """placeholder: Advancements are checks."""
    display_name = "Advancement Checks"


class CrafterLevelChecks(Range):
    """placeholder: Mine Crafter levels up to this one are checks. 0 = none. Levels get slow fast."""
    display_name = "Mine Crafter Level Checks"
    range_start = 0
    range_end = 48
    default = 10


class HubDoorChecks(DefaultOnToggle):
    """placeholder: Opening hub doors is a check, and shimmering keys only come from the multiworld."""
    display_name = "Hub Door Checks"


@dataclass
class CraftmineOptions(PerGameCommonOptions):
    goal: Goal
    special_mines_needed: SpecialMinesNeeded
    advancement_checks: AdvancementChecks
    crafter_level_checks: CrafterLevelChecks
    hub_door_checks: HubDoorChecks
    death_link: DeathLink
