package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class LogicTest {
	private static JsonObject read(String resource) throws Exception {
		try (var in = LogicTest.class.getResourceAsStream(resource)) {
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static Map<String, Integer> have(JsonObject state) {
		Map<String, Integer> have = new HashMap<>();
		state.getAsJsonObject("have").entrySet().forEach(e -> have.put(e.getKey(), e.getValue().getAsInt()));
		return have;
	}

	/** logic_fixture.json holds what Archipelago's own rules say for random item sets (test_logic_export.py). */
	@Test
	void agreesWithTheApworldRules() throws Exception {
		Logic logic = Logic.bundled();
		JsonObject fixture = read("/logic_fixture.json");
		boolean doorsOn = fixture.get("doors_on").getAsBoolean();
		boolean tabsOn = fixture.get("tabs_on").getAsBoolean();
		int compared = 0;
		for (JsonElement element : fixture.getAsJsonArray("states")) {
			JsonObject state = element.getAsJsonObject();
			Logic.Evaluation evaluation = logic.evaluate(have(state), doorsOn, tabsOn);
			Set<String> expected = new TreeSet<>();
			state.getAsJsonArray("in_logic").forEach(k -> expected.add(k.getAsString()));
			Set<String> actual = new TreeSet<>();
			for (JsonElement key : fixture.getAsJsonArray("locations")) {
				if (evaluation.inLogic(key.getAsString())) {
					actual.add(key.getAsString());
				}
			}
			assertEquals(expected, actual, "state " + compared);
			compared++;
		}
		assertEquals(60, compared);
	}

	@Test
	void missingIsEmptyExactlyWhenInLogic() throws Exception {
		Logic logic = Logic.bundled();
		JsonObject fixture = read("/logic_fixture.json");
		JsonObject state = fixture.getAsJsonArray("states").get(0).getAsJsonObject();
		Logic.Evaluation evaluation = logic.evaluate(have(state), true, true);
		for (JsonElement key : fixture.getAsJsonArray("locations")) {
			String k = key.getAsString();
			assertEquals(evaluation.inLogic(k), evaluation.missing(k).isEmpty(), k);
		}
	}

	@Test
	void missingNamesTheNeededItems() throws Exception {
		Logic.Evaluation nothing = Logic.bundled().evaluate(Map.of(), true, true);
		assertEquals(java.util.List.of("Shimmering Key:3"), nothing.missing("door:3"));
		assertTrue(Logic.bundled().evaluate(Map.of("Shimmering Key", 3), true, true).inLogic("door:3"));
		assertTrue(Logic.bundled().evaluate(Map.of(), false, true).inLogic("door:3"));
	}

	@Test
	void nodesNeedTheirTabOnlyWithTabsOn() throws Exception {
		Logic logic = Logic.bundled();
		Logic.Evaluation closed = logic.evaluate(Map.of(), true, true);
		assertFalse(closed.inLogic("node:combatant"));
		assertTrue(closed.missing("node:combatant").contains("Combat Tab"));
		assertTrue(logic.evaluate(Map.of("Combat Tab", 1), true, true).inLogic("node:combatant"));
		assertTrue(logic.evaluate(Map.of(), true, false).inLogic("node:combatant"));
	}

	@Test
	void closedTabsAreTheOnesNotOpen() throws Exception {
		assertEquals(Set.of("combatant", "crafting", "gatherer", "pets", "school_of_hard_knocks"),
			Logic.bundled().closedTabs(Set.of("exploration")));
		assertEquals("Exploration Tab", Logic.bundled().tabItem("exploration"));
		assertEquals("School of Hard Knocks Tab", Logic.bundled().tabItem("school_of_hard_knocks"));
	}

	@Test
	void itemNamesComeFromTheirIds() throws Exception {
		assertEquals("Shimmering Key", Logic.bundled().itemName(0x434D0000L + 3000));
		assertEquals("Ingredient: Foxes", Logic.bundled().location("ingredient:foxes").name());
	}
}
