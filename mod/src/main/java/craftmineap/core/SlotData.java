package craftmineap.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The slot data the craftmine apworld sends. */
public record SlotData(
	String goal,
	int specialMinesNeeded,
	boolean advancementChecks,
	int crafterLevelChecks,
	boolean hubDoorChecks,
	boolean deathLink,
	Map<Long, String> locations,
	Map<Long, ItemInfo> items,
	// looked up on every block break, drop and kill, so built once with the slot data
	Set<String> itemIngredients,
	Map<String, Long> locationIds
) {
	public record ItemInfo(String kind, List<String> keys) {
	}

	public SlotData(String goal, int specialMinesNeeded, boolean advancementChecks, int crafterLevelChecks,
		boolean hubDoorChecks, boolean deathLink, Map<Long, String> locations, Map<Long, ItemInfo> items) {
		this(goal, specialMinesNeeded, advancementChecks, crafterLevelChecks, hubDoorChecks, deathLink, locations, items,
			items.values().stream().filter(i -> i.kind().equals("ingredient")).flatMap(i -> i.keys().stream())
				.collect(Collectors.toUnmodifiableSet()),
			locations.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey)));
	}

	/** True for ingredients that are items in this slot (their unlock is a check, the item is what unlocks them). */
	public boolean isItemIngredient(String ingredientId) {
		return itemIngredients.contains(ingredientId);
	}

	public Long locationId(String gameKey) {
		return locationIds.get(gameKey);
	}

	public static SlotData fromJson(JsonObject json) {
		Map<Long, String> locations = new HashMap<>();
		json.getAsJsonObject("locations").entrySet()
			.forEach(e -> locations.put(Long.parseLong(e.getKey()), e.getValue().getAsString()));
		Map<Long, ItemInfo> items = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("items").entrySet()) {
			JsonObject info = e.getValue().getAsJsonObject();
			List<String> keys = new ArrayList<>();
			info.getAsJsonArray("keys").forEach(k -> keys.add(k.getAsString()));
			items.put(Long.parseLong(e.getKey()), new ItemInfo(info.get("kind").getAsString(), keys));
		}
		return new SlotData(
			json.get("goal").getAsString(),
			json.get("special_mines_needed").getAsInt(),
			json.get("advancement_checks").getAsBoolean(),
			json.get("crafter_level_checks").getAsInt(),
			json.get("hub_door_checks").getAsBoolean(),
			json.get("death_link").getAsBoolean(),
			locations,
			items
		);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("goal", goal);
		json.addProperty("special_mines_needed", specialMinesNeeded);
		json.addProperty("advancement_checks", advancementChecks);
		json.addProperty("crafter_level_checks", crafterLevelChecks);
		json.addProperty("hub_door_checks", hubDoorChecks);
		json.addProperty("death_link", deathLink);
		JsonObject locationsJson = new JsonObject();
		locations.forEach((id, key) -> locationsJson.addProperty(id.toString(), key));
		json.add("locations", locationsJson);
		JsonObject itemsJson = new JsonObject();
		items.forEach((id, info) -> {
			JsonObject infoJson = new JsonObject();
			infoJson.addProperty("kind", info.kind());
			JsonArray keys = new JsonArray();
			info.keys().forEach(keys::add);
			infoJson.add("keys", keys);
			itemsJson.add(id.toString(), infoJson);
		});
		json.add("items", itemsJson);
		return json;
	}
}
