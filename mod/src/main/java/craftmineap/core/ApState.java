package craftmineap.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything the mod keeps per world, saved as craftmineap.json in the world folder. */
public class ApState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String address;
	public String slot;
	public String password;
	public boolean started;
	public SlotData slotData;
	public final List<Long> receivedItems = new ArrayList<>();
	public final Set<Long> checkedLocations = new LinkedHashSet<>();
	public int doorsOpened;
	public final Set<String> specialMinesWon = new LinkedHashSet<>();
	public boolean goalSent;
	/** Keys and XP bundles already handed out, so a resend or a restart never gives them twice. */
	public int keysGiven;
	public int xpGiven;

	public static ApState load(Path file) {
		ApState state = new ApState();
		if (!Files.isRegularFile(file)) {
			return state;
		}
		try {
			JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			state.address = string(json, "address");
			state.slot = string(json, "slot");
			state.password = string(json, "password");
			state.started = json.has("started") && json.get("started").getAsBoolean();
			if (json.has("slot_data")) {
				state.slotData = SlotData.fromJson(json.getAsJsonObject("slot_data"));
			}
			json.getAsJsonArray("received_items").forEach(e -> state.receivedItems.add(e.getAsLong()));
			json.getAsJsonArray("checked_locations").forEach(e -> state.checkedLocations.add(e.getAsLong()));
			state.doorsOpened = json.get("doors_opened").getAsInt();
			json.getAsJsonArray("special_mines_won").forEach(e -> state.specialMinesWon.add(e.getAsString()));
			state.goalSent = json.get("goal_sent").getAsBoolean();
			state.keysGiven = json.get("keys_given").getAsInt();
			state.xpGiven = json.get("xp_given").getAsInt();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return state;
	}

	private static String string(JsonObject json, String key) {
		return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
	}

	public void save(Path file) {
		JsonObject json = new JsonObject();
		json.addProperty("address", address);
		json.addProperty("slot", slot);
		json.addProperty("password", password);
		json.addProperty("started", started);
		if (slotData != null) {
			json.add("slot_data", slotData.toJson());
		}
		JsonArray items = new JsonArray();
		receivedItems.forEach(items::add);
		json.add("received_items", items);
		JsonArray checked = new JsonArray();
		checkedLocations.forEach(checked::add);
		json.add("checked_locations", checked);
		json.addProperty("doors_opened", doorsOpened);
		JsonArray mines = new JsonArray();
		specialMinesWon.forEach(mines::add);
		json.add("special_mines_won", mines);
		json.addProperty("goal_sent", goalSent);
		json.addProperty("keys_given", keysGiven);
		json.addProperty("xp_given", xpGiven);
		try {
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(json));
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The id of the location for a game key like "node:archer", or null when this slot doesn't have it. */
	public Long locationId(String gameKey) {
		if (slotData == null) {
			return null;
		}
		for (Map.Entry<Long, String> e : slotData.locations().entrySet()) {
			if (e.getValue().equals(gameKey)) {
				return e.getKey();
			}
		}
		return null;
	}

	public boolean markChecked(long id) {
		return checkedLocations.add(id);
	}

	/**
	 * Takes the item the server sent as its index-th item (1-based). Returns it when it's new, nothing when it was
	 * already received (the server resends everything on reconnect) or when items before it are missing (the
	 * client library asks for a resync then).
	 */
	public List<Long> acceptItem(int index, long itemId) {
		if (index != receivedItems.size() + 1) {
			return List.of();
		}
		receivedItems.add(itemId);
		return List.of(itemId);
	}

	public boolean goalReached() {
		if (slotData == null) {
			return false;
		}
		if (slotData.goal().equals("ender_dragon")) {
			return specialMinesWon.contains("ender_dragon_boss");
		}
		return specialMinesWon.size() >= slotData.specialMinesNeeded();
	}
}
