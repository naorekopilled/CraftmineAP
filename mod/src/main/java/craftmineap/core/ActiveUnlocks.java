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
