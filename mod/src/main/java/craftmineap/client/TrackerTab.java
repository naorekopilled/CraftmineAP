package craftmineap.client;

import craftmineap.Text;
import craftmineap.ap.ApSession;
import craftmineap.core.Logic;
import craftmineap.core.Tracker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.level.mines.WorldEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Tracker tab of the advancements screen: every check of the slot as a client-only advancement tree, built from
 * the saved state each time the screen opens. Singleplayer only, like the rest of the mod: it reads the integrated
 * server's session.
 */
public final class TrackerTab {
	private static final Logger LOGGER = LoggerFactory.getLogger("craftmineap");
	private static final List<String> KINDS = List.of("ingredient", "node", "advancement", "mine", "crafter", "door");
	private static final int COLUMNS = 12;
	private static final AdvancementRequirements DONE = AdvancementRequirements.allOf(List.of("done"));
	private static final Map<String, Criterion<?>> CRITERIA =
		Map.of("done", new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()));
	private static final ClientAsset BACKGROUND = new ClientAsset(ResourceLocation.withDefaultNamespace("gui/advancements/backgrounds/stone"));
	private static volatile Set<ResourceLocation> outOfLogic = Set.of();

	private TrackerTab() {
	}

	public static boolean isTracker(ResourceLocation id) {
		return id.getNamespace().equals("craftmineap") && id.getPath().startsWith("tracker/");
	}

	/** Read by AdvancementWidgetMixin, which dims these. */
	public static boolean isOutOfLogic(ResourceLocation id) {
		return outOfLogic.contains(id);
	}

	/** advancements: every advancement's display by id path, for icons and tab names. */
	record Snapshot(List<Tracker.Entry> entries, Map<String, DisplayInfo> advancements) {
	}

	/** The laid-out tree, which nodes show as done, and which as out of logic. */
	record Built(AdvancementNode root, Set<ResourceLocation> done, Set<ResourceLocation> dimmed) {
	}

	/** Taken on the server thread, which owns the session state. */
	private static Snapshot snapshot() {
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		ApSession session = ApSession.current();
		if (server == null || session == null) {
			return null;
		}
		try {
			return server.submit(() -> {
				Map<String, DisplayInfo> advancements = new HashMap<>();
				for (AdvancementHolder holder : server.theGame().getAdvancements().getAllAdvancements()) {
					holder.value().display().ifPresent(display -> advancements.put(holder.id().getPath(), display));
				}
				return new Snapshot(Tracker.entries(Logic.bundled(), session.state), advancements);
			}).get(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.warn("Tracker: could not read the session", e);
			return null;
		}
	}

	public static void addTo(AdvancementsScreen screen) {
		Snapshot snapshot = snapshot();
		if (snapshot == null || snapshot.entries().isEmpty()) {
			return;
		}
		Built built = build(snapshot);
		outOfLogic = built.dimmed();
		screen.onAddAdvancementRoot(built.root());
		Deque<AdvancementNode> queue = new ArrayDeque<>();
		queue.add(built.root());
		while (!queue.isEmpty()) {
			AdvancementNode node = queue.poll();
			if (node != built.root()) {
				screen.onAddAdvancementTask(node);
			}
			AdvancementProgress progress = new AdvancementProgress();
			progress.update(DONE);
			if (built.done().contains(node.holder().id())) {
				progress.grantProgress("done");
			}
			screen.onUpdateAdvancementProgress(node, progress);
			node.children().forEach(queue::add);
		}
	}

	static Built build(Snapshot snapshot) {
		Logic logic = Logic.bundled();
		List<Tracker.Entry> entries = new ArrayList<>(snapshot.entries());
		entries.sort(Comparator.comparing((Tracker.Entry e) -> KINDS.indexOf(kind(e.key())))
			.thenComparing(e -> band(logic, e), Comparator.comparingInt(TrackerTab::bandOrder).thenComparing(b -> b))
			.thenComparing(e -> depth(logic, e)).thenComparing(TrackerTab::order));
		Map<String, List<Tracker.Entry>> bands = new LinkedHashMap<>();
		entries.forEach(e -> bands.computeIfAbsent(band(logic, e), b -> new ArrayList<>()).add(e));

		Set<ResourceLocation> done = new HashSet<>();
		Set<ResourceLocation> dimmed = new HashSet<>();
		List<AdvancementHolder> holders = new ArrayList<>();
		ResourceLocation root = id("root");
		holders.add(holder(root, null, display(new ItemStack(Items.COMPASS), Text.trackerTitle(), progress(entries), AdvancementType.TASK, 0, 0)));
		// a grid like a map: one band per group, its icon on the left and its checks in rows of COLUMNS
		float row = 1.25F;
		for (Map.Entry<String, List<Tracker.Entry>> band : bands.entrySet()) {
			ResourceLocation bandId = id("band/" + band.getKey());
			holders.add(holder(bandId, root, bandDisplay(band.getKey(), snapshot, progress(band.getValue()), row)));
			List<Tracker.Entry> inBand = band.getValue();
			for (int i = 0; i < inBand.size(); i++) {
				Tracker.Entry entry = inBand.get(i);
				ResourceLocation entryId = id(entry.key());
				Component description = switch (entry.status()) {
					case DONE -> Text.trackerDone();
					case IN_LOGIC -> Text.trackerInLogic();
					case OUT_OF_LOGIC -> Text.trackerOutOfLogic().copy().append("\n")
						.append(Text.trackerNeeds(entry.missing().stream().map(atom -> need(logic, atom)).collect(Collectors.joining(", "))));
				};
				AdvancementType frame = kind(entry.key()).equals("mine") ? AdvancementType.CHALLENGE : AdvancementType.TASK;
				holders.add(holder(entryId, bandId, display(icon(logic, entry.key(), snapshot), Component.literal(entry.name()), description,
					frame, 1 + i % COLUMNS, row + i / COLUMNS)));
				if (entry.status() == Tracker.Status.DONE) {
					done.add(entryId);
				} else if (entry.status() == Tracker.Status.OUT_OF_LOGIC) {
					dimmed.add(entryId);
				}
			}
			row += (inBand.size() + COLUMNS - 1) / COLUMNS + 0.25F;
		}

		AdvancementTree tree = new AdvancementTree();
		tree.addAll(holders);
		return new Built(tree.get(root), Set.copyOf(done), Set.copyOf(dimmed));
	}

	/** Skill-tree nodes and advancements get a band per tab, the rest one per kind. */
	private static String band(Logic logic, Tracker.Entry entry) {
		String kind = kind(entry.key());
		return switch (kind) {
			case "node" -> "node/" + Optional.ofNullable(logic.node(arg(entry.key()))).map(Logic.Node::tab).orElse("other");
			case "advancement" -> "advancement/" + tab(entry.key());
			default -> kind;
		};
	}

	/** Skill-tree tabs in the game's order. */
	private static int bandOrder(String band) {
		if (!band.startsWith("node/")) {
			return 0;
		}
		PlayerUnlock tabRoot = BuiltInRegistries.PLAYER_UNLOCK.getValue(ResourceLocation.withDefaultNamespace(band.substring(5)));
		return tabRoot == null ? Integer.MAX_VALUE : BuiltInRegistries.PLAYER_UNLOCK.getId(tabRoot);
	}

	/** A skill-tree node comes after its parent. */
	private static int depth(Logic logic, Tracker.Entry entry) {
		if (!kind(entry.key()).equals("node")) {
			return 0;
		}
		int depth = 0;
		for (Logic.Node node = logic.node(arg(entry.key())); node != null && node.parent() != null; node = logic.node(node.parent())) {
			depth++;
		}
		return depth;
	}

	private static DisplayInfo bandDisplay(String band, Snapshot snapshot, Component progress, float row) {
		String kind = band.contains("/") ? band.substring(0, band.indexOf('/')) : band;
		String tab = band.substring(band.indexOf('/') + 1);
		ItemStack icon = branchIcon(kind);
		Component title = Text.trackerBranch(kind);
		if (kind.equals("node")) {
			PlayerUnlock tabRoot = BuiltInRegistries.PLAYER_UNLOCK.getValue(ResourceLocation.withDefaultNamespace(tab));
			icon = tabRoot == null ? icon : tabRoot.display().getIcon();
			title = tabRoot == null ? Component.literal(tab) : tabRoot.display().getTitle();
		} else if (kind.equals("advancement")) {
			DisplayInfo tabRoot = snapshot.advancements().get(tab + "/root");
			icon = tabRoot == null ? icon : tabRoot.getIcon();
			title = tabRoot == null ? Component.literal(tab) : tabRoot.getTitle();
		}
		return display(icon, title, progress, AdvancementType.GOAL, 0, row);
	}

	private static ResourceLocation id(String key) {
		return ResourceLocation.fromNamespaceAndPath("craftmineap", "tracker/" + key.replace(':', '/'));
	}

	private static String kind(String key) {
		return key.substring(0, key.indexOf(':'));
	}

	private static String arg(String key) {
		return key.substring(key.indexOf(':') + 1);
	}

	/** An advancement's tab is the folder of its id, e.g. feats/angry_ghast. */
	private static String tab(String key) {
		String arg = arg(key);
		return arg.contains("/") ? arg.substring(0, arg.indexOf('/')) : arg;
	}

	/** Numbered checks in number order, the rest by name. */
	private static String order(Tracker.Entry entry) {
		String arg = arg(entry.key());
		return arg.chars().allMatch(Character::isDigit) ? String.format("%05d", Integer.parseInt(arg)) : entry.name();
	}

	private static String need(Logic logic, String atom) {
		if (!atom.startsWith("@")) {
			return Text.trackerNeed("item", Logic.item(atom), Logic.amount(atom));
		}
		String kind = atom.contains(":") ? atom.substring(1, atom.indexOf(':')) : atom.substring(1);
		String arg = atom.contains(":") ? atom.substring(atom.indexOf(':') + 1) : "";
		Logic.Location named = switch (kind) {
			case "mine" -> logic.location("mine:" + arg);
			case "node" -> logic.location("node:" + arg);
			default -> null;
		};
		return Text.trackerNeed(kind, named != null ? named.name() : arg, 1);
	}

	private static ItemStack icon(Logic logic, String key, Snapshot snapshot) {
		String arg = arg(key);
		return switch (kind(key)) {
			case "ingredient" -> ingredientIcon(arg);
			case "mine" -> ingredientIcon(logic.location(key).icon());
			case "node" -> Optional.ofNullable(BuiltInRegistries.PLAYER_UNLOCK.getValue(ResourceLocation.withDefaultNamespace(arg)))
				.map(PlayerUnlock::display).map(DisplayInfo::getIcon).orElse(new ItemStack(Items.EXPERIENCE_BOTTLE));
			case "advancement" -> Optional.ofNullable(snapshot.advancements().get(arg)).map(DisplayInfo::getIcon).orElse(new ItemStack(Items.BOOK));
			case "crafter" -> new ItemStack(Items.MINE_CRAFTER);
			default -> new ItemStack(Items.SHIMMERING_KEY);
		};
	}

	private static ItemStack ingredientIcon(String id) {
		WorldEffect effect = id == null ? null : BuiltInRegistries.WORLD_EFFECT.getValue(ResourceLocation.withDefaultNamespace(id));
		return effect == null ? new ItemStack(Items.MINE_INGREDIENT) : WorldEffects.createEffectItem(effect, false);
	}

	private static ItemStack branchIcon(String branch) {
		return new ItemStack(switch (branch) {
			case "ingredient" -> Items.MINE_INGREDIENT;
			case "node" -> Items.EXPERIENCE_BOTTLE;
			case "advancement" -> Items.BOOK;
			case "mine" -> Items.DRAGON_HEAD;
			case "crafter" -> Items.MINE_CRAFTER;
			default -> Items.SHIMMERING_KEY;
		});
	}

	private static Component progress(List<Tracker.Entry> entries) {
		return Text.trackerProgress((int) entries.stream().filter(e -> e.status() == Tracker.Status.DONE).count(), entries.size());
	}

	private static DisplayInfo display(ItemStack icon, Component title, Component description, AdvancementType type, float x, float y) {
		DisplayInfo display = new DisplayInfo(icon, title, description, Component.empty(), Optional.of(BACKGROUND), type, false, false, false);
		display.setLocation(x, y);
		return display;
	}

	private static AdvancementHolder holder(ResourceLocation id, ResourceLocation parent, DisplayInfo display) {
		return new AdvancementHolder(id, new Advancement(Optional.ofNullable(parent), Optional.of(display), AdvancementRewards.EMPTY,
			CRITERIA, DONE, false));
	}
}
