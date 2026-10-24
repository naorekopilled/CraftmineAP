package craftmineap.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The apworld's rules (craftmineap/logic.json, written by the apworld's logic_export.py), evaluated the way its
 * rules.RuleBuilder does. A rule is a list of alternatives, each a list of atoms that are all needed.
 */
public final class Logic {
	private static final String KEY = "Shimmering Key";
	private static Logic bundled;

	public record Location(String key, String name, List<List<String>> rule, List<String> after, String parent, String icon) {
	}

	public record Node(String id, String parent, String tab, List<List<String>> hidden, int learning) {
	}

	private final Map<Long, String> items = new HashMap<>();
	private final Map<String, Location> locations = new LinkedHashMap<>();
	private final Map<String, List<List<String>>> mines = new LinkedHashMap<>();
	private final Map<String, Node> nodes = new LinkedHashMap<>();

	private Logic(JsonObject json) {
		json.getAsJsonObject("items").entrySet().forEach(e -> items.put(Long.parseLong(e.getKey()), e.getValue().getAsString()));
		for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("locations").entrySet()) {
			JsonObject l = e.getValue().getAsJsonObject();
			locations.put(e.getKey(), new Location(e.getKey(), l.get("name").getAsString(), rule(l.getAsJsonArray("rule")),
				l.has("after") ? strings(l.getAsJsonArray("after")) : List.of(), string(l, "parent"), string(l, "icon")));
		}
		json.getAsJsonObject("mines").entrySet().forEach(e -> mines.put(e.getKey(), rule(e.getValue().getAsJsonArray())));
		for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("nodes").entrySet()) {
			JsonObject n = e.getValue().getAsJsonObject();
			nodes.put(e.getKey(), new Node(e.getKey(), string(n, "parent"), n.get("tab").getAsString(), rule(n.getAsJsonArray("hidden")),
				n.get("learning").getAsInt()));
		}
	}

	public static synchronized Logic bundled() {
		if (bundled == null) {
			try (InputStream in = Logic.class.getResourceAsStream("/craftmineap/logic.json")) {
				bundled = new Logic(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return bundled;
	}

	private static String string(JsonObject json, String field) {
		return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : null;
	}

	private static List<String> strings(JsonArray array) {
		List<String> list = new ArrayList<>();
		array.forEach(e -> list.add(e.getAsString()));
		return list;
	}

	private static List<List<String>> rule(JsonArray alternatives) {
		List<List<String>> rule = new ArrayList<>();
		alternatives.forEach(alt -> rule.add(strings(alt.getAsJsonArray())));
		return rule;
	}

	/** An item atom's count: "Name:3" needs 3. Item names can hold a colon too, so only a number counts. */
	public static int amount(String atom) {
		return hasCount(atom) ? Integer.parseInt(atom.substring(atom.lastIndexOf(':') + 1)) : 1;
	}

	/** An item atom's item name, without its count. */
	public static String item(String atom) {
		return hasCount(atom) ? atom.substring(0, atom.lastIndexOf(':')) : atom;
	}

	private static boolean hasCount(String atom) {
		String suffix = atom.substring(atom.lastIndexOf(':') + 1);
		return atom.contains(":") && !suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit);
	}

	public String itemName(long id) {
		return items.get(id);
	}

	public Location location(String key) {
		return locations.get(key);
	}

	public Node node(String id) {
		return nodes.get(id);
	}

	/** have: received item name to count. doorsOn: the slot's hub_door_checks (keys are free logic without it). */
	public Evaluation evaluate(Map<String, Integer> have, boolean doorsOn) {
		return new Evaluation(have, doorsOn);
	}

	/** One item set; node and mine results are cached, so build one per tracker refresh. */
	public final class Evaluation {
		private final Map<String, Integer> have;
		private final boolean doorsOn;
		private final Map<String, Boolean> nodeCache = new HashMap<>();
		private final Map<String, Boolean> mineCache = new HashMap<>();

		private Evaluation(Map<String, Integer> have, boolean doorsOn) {
			this.have = have;
			this.doorsOn = doorsOn;
		}

		public boolean inLogic(String locationKey) {
			return any(locations.get(locationKey).rule());
		}

		/** The unmet atoms of the alternative closest to being met; empty when in logic. */
		public List<String> missing(String locationKey) {
			List<String> best = null;
			for (List<String> alt : locations.get(locationKey).rule()) {
				List<String> unmet = alt.stream().filter(atom -> !atom(atom)).toList();
				if (best == null || unmet.size() < best.size()) {
					best = unmet;
				}
			}
			return best == null ? List.of() : best;
		}

		private boolean any(List<List<String>> rule) {
			return rule.stream().anyMatch(alt -> alt.stream().allMatch(this::atom));
		}

		private boolean has(String item, int amount) {
			return have.getOrDefault(item, 0) >= amount;
		}

		private boolean atom(String atom) {
			if (atom.startsWith("@")) {
				int colon = atom.indexOf(':');
				String kind = colon < 0 ? atom.substring(1) : atom.substring(1, colon);
				String arg = colon < 0 ? "" : atom.substring(colon + 1);
				return switch (kind) {
					case "craft" -> has("Crafting", 1) && (has("Inventory Crafting", 1) || has("Crafting table", 1));
					case "special" -> mines.keySet().stream().anyMatch(this::mine);
					case "mine" -> mine(arg);
					case "node" -> node(arg);
					case "tab" -> nodes.values().stream().filter(n -> n.tab().equals(arg)).allMatch(n -> node(n.id()));
					default -> throw new IllegalArgumentException("unknown rule atom " + atom);
				};
			}
			String item = item(atom);
			return (item.equals(KEY) && !doorsOn) || has(item, amount(atom));
		}

		private boolean mine(String key) {
			Boolean cached = mineCache.get(key);
			if (cached == null) {
				cached = any(mines.get(key));
				mineCache.put(key, cached);
			}
			return cached;
		}

		private boolean node(String id) {
			Boolean cached = nodeCache.get(id);
			if (cached == null) {
				Node node = nodes.get(id);
				cached = (node.parent() == null || node(node.parent())) && any(node.hidden())
					&& (node.learning() == 0 || has("Progressive Learning", node.learning()));
				nodeCache.put(id, cached);
			}
			return cached;
		}
	}
}
