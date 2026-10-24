package craftmineap.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** What the in-game tracker shows: every check of the slot, done, in logic or not, from the saved state. */
public final class Tracker {
	public enum Status { DONE, IN_LOGIC, OUT_OF_LOGIC }

	/** missing: the unmet rule atoms (see Logic), empty unless out of logic. */
	public record Entry(String key, String name, Status status, List<String> missing) {
	}

	private Tracker() {
	}

	public static List<Entry> entries(Logic logic, ApState state) {
		if (state.slotData == null) {
			return List.of();
		}
		Map<String, Integer> have = new HashMap<>();
		for (long id : state.receivedItems) {
			String name = logic.itemName(id);
			if (name != null) {
				have.merge(name, 1, Integer::sum);
			}
		}
		Logic.Evaluation evaluation = logic.evaluate(have, state.slotData.hubDoorChecks());
		List<Entry> entries = new ArrayList<>();
		state.slotData.locations().forEach((id, key) -> {
			Logic.Location location = logic.location(key);
			if (location == null) {
				return;
			}
			Status status = state.checkedLocations.contains(id) ? Status.DONE
				: evaluation.inLogic(key) ? Status.IN_LOGIC : Status.OUT_OF_LOGIC;
			entries.add(new Entry(key, location.name(), status, status == Status.OUT_OF_LOGIC ? evaluation.missing(key) : List.of()));
		});
		return entries;
	}
}
