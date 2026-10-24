package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TrackerTest {
	private static final long KEY_ID = 0x434D0000L + 3000;

	private static ApState state() {
		ApState state = new ApState();
		state.slotData = SlotData.fromJson(JsonParser.parseString("""
			{"goal": "special_mines", "special_mines_needed": 9, "advancement_checks": true, "crafter_level_checks": 1,
			 "hub_door_checks": true, "death_link": false,
			 "locations": {"1": "door:1", "2": "door:2", "3": "crafter:1"}, "items": {}}
			""").getAsJsonObject());
		return state;
	}

	private static Map<String, Tracker.Entry> byKey(List<Tracker.Entry> entries) {
		return entries.stream().collect(Collectors.toMap(Tracker.Entry::key, e -> e));
	}

	@Test
	void marksDoneInLogicAndOutOfLogic() {
		ApState state = state();
		state.acceptItem(1, KEY_ID);
		state.acceptItem(2, 999L); // an id this logic doesn't know is just ignored
		state.markChecked(1);
		Map<String, Tracker.Entry> entries = byKey(Tracker.entries(Logic.bundled(), state));
		assertEquals(3, entries.size());
		assertEquals(Tracker.Status.DONE, entries.get("door:1").status());
		assertEquals(Tracker.Status.OUT_OF_LOGIC, entries.get("door:2").status());
		assertEquals(List.of("Shimmering Key:2"), entries.get("door:2").missing());
		assertEquals(Tracker.Status.IN_LOGIC, entries.get("crafter:1").status());
		assertEquals("Hub Door 2", entries.get("door:2").name());
	}

	@Test
	void doneWinsOverLogic() {
		ApState state = state();
		state.markChecked(2);
		assertEquals(Tracker.Status.DONE, byKey(Tracker.entries(Logic.bundled(), state)).get("door:2").status());
	}

	@Test
	void noSlotDataNoEntries() {
		assertTrue(Tracker.entries(Logic.bundled(), new ApState()).isEmpty());
	}
}
