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
import java.util.Set;

/** Everything the mod keeps per world, saved as craftmineap.json in the world folder. */
public class ApState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String address;
	public String slot;
	public String password;
	/** The room this world belongs to, set on the first connect. */
	public String seedName;
	public String roomSlot;
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
		JsonObject json;
		try {
			json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		state.address = string(json, "address");
		state.slot = string(json, "slot");
		state.password = string(json, "password");
		state.seedName = string(json, "seed_name");
		state.roomSlot = string(json, "room_slot");
		state.started = json.has("started") && json.get("started").getAsBoolean();
		if (json.has("slot_data")) {
			state.slotData = SlotData.fromJson(json.getAsJsonObject("slot_data"));
		}
		array(json, "received_items").forEach(e -> state.receivedItems.add(e.getAsLong()));
		array(json, "checked_locations").forEach(e -> state.checkedLocations.add(e.getAsLong()));
		array(json, "special_mines_won").forEach(e -> state.specialMinesWon.add(e.getAsString()));
		state.doorsOpened = integer(json, "doors_opened");
		state.goalSent = json.has("goal_sent") && json.get("goal_sent").getAsBoolean();
		state.keysGiven = integer(json, "keys_given");
		state.xpGiven = integer(json, "xp_given");
		return state;
	}

	private static String string(JsonObject json, String key) {
		return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
	}

	private static JsonArray array(JsonObject json, String key) {
		return json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
	}

	private static int integer(JsonObject json, String key) {
		return json.has(key) ? json.get(key).getAsInt() : 0;
	}

	public void save(Path file) {
		JsonObject json = new JsonObject();
		json.addProperty("address", address);
		json.addProperty("slot", slot);
		json.addProperty("password", password);
		json.addProperty("seed_name", seedName);
		json.addProperty("room_slot", roomSlot);
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
		return slotData == null ? null : slotData.locationId(gameKey);
	}

	/**
	 * A world belongs to the first room and slot it connects to: its received items, checks and given keys only
	 * make sense there. Returns false for any other room or slot.
	 */
	public boolean claimRoom(String seed, String slotName) {
		if (seedName == null) {
			seedName = seed;
			roomSlot = slotName;
			return true;
		}
		return seedName.equals(seed) && roomSlot.equals(slotName);
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
