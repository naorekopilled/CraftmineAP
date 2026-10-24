package craftmineap.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** The game's own "active unlocks" rule, applied to received unlocks instead of bought ones (server and client). */
public final class ActiveUnlocks {
	private ActiveUnlocks() {
	}

	/**
	 * Whether applying received items runs an unlock's activation. Exclusive ones (pets) switch the others in their group
	 * off, so only a newly received one or the current choice: the game sets up a pet's spawn on activation and forgets
	 * that setup when it closes, like it does for bought ones on login.
	 */
	public static boolean activateOnApply(boolean exclusive, boolean newlyGranted, boolean activeExclusive) {
		return !exclusive || newlyGranted || activeExclusive;
	}

	public static <T> Set<T> compute(Collection<T> granted, Function<T, Collection<T>> disables, Predicate<T> inactiveExclusive) {
		Set<T> active = new HashSet<>(granted);
		for (T unlock : granted) {
			disables.apply(unlock).forEach(active::remove);
			if (inactiveExclusive.test(unlock)) {
				active.remove(unlock);
			}
		}
		return active;
	}
}
