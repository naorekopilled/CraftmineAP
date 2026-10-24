package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApStateTest {
	static final String SLOT_DATA = """
		{"goal": "special_mines", "special_mines_needed": 2, "advancement_checks": true, "crafter_level_checks": 10,
		 "hub_door_checks": false, "death_link": true, "world_version": "0.1.0",
		 "locations": {"100": "ingredient:foxes", "101": "mine:raid", "102": "mine:wither_boss"},
		 "items": {"200": {"kind": "ingredient", "keys": ["foxes"]},
		           "201": {"kind": "unlock", "keys": ["starter_sword", "starter_sword_iron", "better_starter_sword", "best_starter_sword"]},
		           "202": {"kind": "key", "keys": []}, "203": {"kind": "xp", "keys": []}}}
		""";

	static ApState state() {
		ApState state = new ApState();
		state.slotData = SlotData.fromJson(JsonParser.parseString(SLOT_DATA).getAsJsonObject());
		return state;
	}

	@Test
	void saveLoadRoundTrip(@TempDir Path dir) throws Exception {
		ApState state = state();
		state.address = "localhost:38281";
		state.slot = "Miner";
		state.started = true;
		state.acceptItem(1, 200);
		state.markChecked(100);
		state.doorsOpened = 2;
		state.specialMinesWon.add("raid");
		state.keysGiven = 3;
		state.xpGiven = 4;
		state.claimRoom("seed-1", "Miner");
		Path file = dir.resolve("craftmineap.json");
		state.save(file);
		ApState loaded = ApState.load(file);
		assertEquals("Miner", loaded.slot);
		assertTrue(loaded.started);
		assertEquals(List.of(200L), loaded.receivedItems);
		assertEquals(Set.of(100L), loaded.checkedLocations);
		assertEquals(2, loaded.doorsOpened);
		assertEquals(Set.of("raid"), loaded.specialMinesWon);
		assertEquals(3, loaded.keysGiven);
		assertEquals(4, loaded.xpGiven);
		assertFalse(loaded.claimRoom("seed-2", "Miner"));
		assertEquals(2, loaded.slotData.specialMinesNeeded());
		assertEquals("mine:raid", loaded.slotData.locations().get(101L));
	}

	@Test
	void loadMissingFileGivesEmptyState(@TempDir Path dir) {
		ApState state = ApState.load(dir.resolve("none.json"));
		assertNull(state.slotData);
		assertFalse(state.started);
	}

	@Test
	void loadToleratesMissingFields(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("craftmineap.json");
		Files.writeString(file, "{\"address\": \"localhost:38281\"}");
		ApState state = ApState.load(file);
		assertEquals("localhost:38281", state.address);
		assertEquals(0, state.keysGiven);
		assertTrue(state.receivedItems.isEmpty());
	}

	@Test
	void aWorldStaysWithItsFirstRoom() {
		ApState state = state();
		assertTrue(state.claimRoom("seed-1", "Miner"));
		assertTrue(state.claimRoom("seed-1", "Miner"));
		assertFalse(state.claimRoom("seed-2", "Miner"));
		assertFalse(state.claimRoom("seed-1", "Other"));
	}

	@Test
	void markCheckedOnlyOnce() {
		ApState state = state();
		assertTrue(state.markChecked(100));
		assertFalse(state.markChecked(100));
	}

	@Test
	void resentItemsAreNotAcceptedTwice() {
		ApState state = state();
		assertEquals(List.of(202L), state.acceptItem(1, 202));
		assertEquals(List.of(), state.acceptItem(1, 202));
		assertEquals(List.of(203L), state.acceptItem(2, 203));
		assertEquals(List.of(202L, 203L), state.receivedItems);
	}

	@Test
	void itemAfterAGapIsNotAccepted() {
		ApState state = state();
		state.acceptItem(1, 202);
		assertEquals(List.of(), state.acceptItem(3, 203));
		assertEquals(1, state.receivedItems.size());
	}

	@Test
	void locationIdIsNullForChecksNotInThisSlot() {
		ApState state = state();
		assertEquals(100L, state.locationId("ingredient:foxes"));
		assertNull(state.locationId("door:3"));
	}

	@Test
	void goalSpecialMines() {
		ApState state = state();
		state.specialMinesWon.add("raid");
		assertFalse(state.goalReached());
		state.specialMinesWon.add("wither_boss");
		assertTrue(state.goalReached());
	}

	@Test
	void goalEnderDragon() {
		ApState state = state();
		state.slotData = new SlotData("ender_dragon", 9, true, 10, true, false,
			state.slotData.locations(), state.slotData.items());
		state.specialMinesWon.add("raid");
		assertFalse(state.goalReached());
		state.specialMinesWon.add("ender_dragon_boss");
		assertTrue(state.goalReached());
	}
}
