package craftmineap.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import craftmineap.core.ApState;
import craftmineap.core.Logic;
import craftmineap.core.SlotData;
import craftmineap.core.Tracker;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrackerTabTest {
	private static final long KEY_ID = 0x434D0000L + 3000;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/** A slot with every location the apworld knows, one key received and door 1 done. */
	private static List<Tracker.Entry> entries() throws Exception {
		JsonObject logic;
		try (var in = TrackerTabTest.class.getResourceAsStream("/craftmineap/logic.json")) {
			logic = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		Map<Long, String> locations = new HashMap<>();
		long id = 1;
		for (String key : logic.getAsJsonObject("locations").keySet()) {
			locations.put(id++, key);
		}
		ApState state = new ApState();
		state.slotData = new SlotData("special_mines", 9, true, 48, true, false, false, locations, Map.of());
		state.acceptItem(1, KEY_ID);
		locations.forEach((locationId, key) -> {
			if (key.equals("door:1")) {
				state.markChecked(locationId);
			}
		});
		return Tracker.entries(Logic.bundled(), state);
	}

	private static List<AdvancementNode> all(AdvancementNode root) {
		List<AdvancementNode> nodes = new ArrayList<>();
		List<AdvancementNode> queue = new ArrayList<>(List.of(root));
		while (!queue.isEmpty()) {
			AdvancementNode node = queue.remove(0);
			nodes.add(node);
			node.children().forEach(queue::add);
		}
		return nodes;
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("craftmineap", "tracker/" + path);
	}

	@Test
	void everyCheckGetsANodeInOneTree() throws Exception {
		List<Tracker.Entry> entries = entries();
		TrackerTab.Built built = TrackerTab.build(new TrackerTab.Snapshot(entries, Map.of()));
		List<AdvancementNode> nodes = all(built.root());
		Set<ResourceLocation> ids = new HashSet<>();
		nodes.forEach(n -> ids.add(n.holder().id()));
		for (Tracker.Entry entry : entries) {
			assertTrue(ids.contains(id(entry.key().replace(':', '/'))), entry.key());
		}
		// every check hangs off a group band, every band off the root
		for (AdvancementNode band : built.root().children()) {
			assertTrue(band.holder().id().getPath().startsWith("tracker/band/"), band.holder().id().toString());
			band.children().forEach(check -> assertFalse(check.children().iterator().hasNext(), check.holder().id().toString()));
		}
	}

	@Test
	void fitsAFewScreensNotAHundred() throws Exception {
		TrackerTab.Built built = TrackerTab.build(new TrackerTab.Snapshot(entries(), Map.of()));
		float maxX = 0;
		float maxY = 0;
		for (AdvancementNode node : all(built.root())) {
			DisplayInfo display = node.advancement().display().orElseThrow();
			maxX = Math.max(maxX, display.getX());
			maxY = Math.max(maxY, display.getY());
		}
		assertTrue(maxX <= 12, "columns " + maxX);
		assertTrue(maxY <= 45, "rows " + maxY);
	}

	@Test
	void nodesDoNotOverlap() throws Exception {
		TrackerTab.Built built = TrackerTab.build(new TrackerTab.Snapshot(entries(), Map.of()));
		Set<String> spots = new HashSet<>();
		for (AdvancementNode node : all(built.root())) {
			DisplayInfo display = node.advancement().display().orElseThrow();
			assertTrue(spots.add(display.getX() + "," + display.getY()), node.holder().id().toString());
		}
	}

	@Test
	void statusShowsAsDoneAndDimmed() throws Exception {
		TrackerTab.Built built = TrackerTab.build(new TrackerTab.Snapshot(entries(), Map.of()));
		assertTrue(built.done().contains(id("door/1")));
		assertFalse(built.done().contains(id("door/2")));
		assertTrue(built.dimmed().contains(id("door/3")));
		assertFalse(built.dimmed().contains(id("door/1")));
		assertFalse(built.dimmed().contains(id("crafter/1")));
		DisplayInfo door3 = all(built.root()).stream().filter(n -> n.holder().id().equals(id("door/3"))).findFirst()
			.orElseThrow().advancement().display().orElseThrow();
		assertTrue(door3.getDescription().getString().contains("Shimmering Key x3"), door3.getDescription().getString());
	}

	@Test
	void iconsComeFromTheGame() throws Exception {
		TrackerTab.Built built = TrackerTab.build(new TrackerTab.Snapshot(entries(), Map.of()));
		Map<ResourceLocation, DisplayInfo> displays = new HashMap<>();
		all(built.root()).forEach(n -> displays.put(n.holder().id(), n.advancement().display().orElseThrow()));
		assertTrue(displays.get(id("door/1")).getIcon().is(Items.SHIMMERING_KEY));
		assertTrue(displays.get(id("ingredient/foxes")).getIcon().is(Items.MINE_INGREDIENT));
		assertNotNull(displays.get(id("ingredient/foxes")).getIcon().get(DataComponents.WORLD_MODIFIERS));
		assertFalse(displays.get(id("node/archer")).getIcon().is(Items.EXPERIENCE_BOTTLE));
	}
}
