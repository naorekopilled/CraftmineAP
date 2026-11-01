from dataclasses import dataclass

from Options import Choice, DeathLink, DefaultOnToggle, PerGameCommonOptions, Range


class Goal(Choice):
    """What you need to do to win.
    special_mines: win a number of different special mines, set by special_mines_needed.
    ender_dragon: win the Ender Dragon mine."""
    display_name = "Goal"
    option_special_mines = 0
    option_ender_dragon = 1
    default = 0


class SpecialMinesNeeded(Range):
    """How many different special mines you need to win for the special_mines goal."""
    display_name = "Special Mines Needed"
    range_start = 1
    range_end = 9
    default = 9


class AdvancementChecks(DefaultOnToggle):
    """Earning an advancement sends a check."""
    display_name = "Advancement Checks"


class CrafterLevelChecks(Range):
    """Every Mine Crafter level from 1 up to this one sends a check. 0 turns them off.
    Later levels take a long time to reach."""
    display_name = "Mine Crafter Level Checks"
    range_start = 0
    range_end = 48
    default = 10


class HubDoorChecks(DefaultOnToggle):
    """Opening a hub door sends a check. With this on, Shimmering Keys only come from the multiworld."""
    display_name = "Hub Door Checks"


class UpgradeTabs(DefaultOnToggle):
    """Each skill-tree tab has its own item, and you can't buy that tab's upgrades until you have it. One random tab
    starts open. Upgrades you receive still work while their tab is closed."""
    display_name = "Upgrade Tabs"


@dataclass
class CraftmineOptions(PerGameCommonOptions):
    goal: Goal
    special_mines_needed: SpecialMinesNeeded
    advancement_checks: AdvancementChecks
    crafter_level_checks: CrafterLevelChecks
    hub_door_checks: HubDoorChecks
    upgrade_tabs: UpgradeTabs
    death_link: DeathLink
