package craftmineap.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** What the received items add up to. */
public record Grants(Set<String> ingredients, Set<String> unlocks, Set<String> tabs, int keys, int xpBundles) {
	public static Grants from(ApState state) {
		Set<String> ingredients = new HashSet<>();
		Set<String> unlocks = new HashSet<>();
		Set<String> tabs = new HashSet<>();
		int keys = 0;
		int xp = 0;
		Map<Long, Integer> copies = new HashMap<>();
		for (long id : state.receivedItems) {
			SlotData.ItemInfo info = state.slotData == null ? null : state.slotData.items().get(id);
			if (info == null) {
				continue;
			}
			int n = copies.merge(id, 1, Integer::sum);
			switch (info.kind()) {
				case "ingredient" -> ingredients.addAll(info.keys());
				case "unlock" -> unlocks.addAll(info.keys().subList(0, Math.min(n, info.keys().size())));
				case "tab" -> tabs.addAll(info.keys());
				case "key" -> keys++;
				case "xp" -> xp++;
				default -> {
				}
			}
		}
		return new Grants(ingredients, unlocks, tabs, keys, xp);
	}
}
