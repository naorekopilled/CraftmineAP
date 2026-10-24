package craftmineap.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.level.mines.SpecialMine;
import net.minecraft.world.level.mines.WorldEffect;

/** Writes the game's ingredients, skill-tree unlocks, special mines and advancements as JSON for the apworld. */
public final class DataDump {
	private static final List<String> ADVANCEMENT_TABS = List.of("feats", "mines", "story", "unlocks");

	private DataDump() {
	}

	public static void write(MinecraftServer server, Path out) {
		JsonObject root = new JsonObject();
		root.add("ingredients", ingredients());
		root.add("unlocks", unlocks());
		root.add("special_mines", specialMines());
		root.add("advancements", advancements(server));
		try {
			Files.createDirectories(out.getParent());
			Files.writeString(out, new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static <T> String id(Registry<T> registry, T value) {
		return registry.getKey(value).getPath();
	}

	private static <T> JsonArray ids(Registry<T> registry, List<T> values) {
		JsonArray array = new JsonArray();
		values.stream().map(v -> id(registry, v)).sorted().forEach(array::add);
		return array;
	}

	private static JsonArray sorted(List<JsonObject> objects) {
		JsonArray array = new JsonArray();
		objects.stream().sorted(Comparator.comparing(o -> o.get("id").getAsString())).forEach(array::add);
		return array;
	}

	private static JsonArray ingredients() {
		Registry<WorldEffect> registry = BuiltInRegistries.WORLD_EFFECT;
		return sorted(registry.stream().map(effect -> {
			JsonObject o = new JsonObject();
			o.addProperty("id", id(registry, effect));
			o.addProperty("name", effect.name().getString());
			o.addProperty("unlock_mode", effect.unlockMode().name());
			o.addProperty("conditions", effect.unlockedBy().size());
			o.add("unlocked_after", ids(registry, effect.unlockedAfter()));
			o.addProperty("required_unlock_count", effect.requiredUnlockCount());
			o.addProperty("randomization", effect.randomizationMode().name());
			o.addProperty("multiplayer_only", effect.multiplayerOnly());
			o.addProperty("xp_multiplier", effect.experienceModifier());
			return o;
		}).toList());
	}

	private static JsonArray unlocks() {
		Registry<PlayerUnlock> registry = BuiltInRegistries.PLAYER_UNLOCK;
		return sorted(registry.listElements().map(holder -> {
			PlayerUnlock unlock = holder.value();
			JsonObject o = new JsonObject();
			o.addProperty("id", holder.key().location().getPath());
			o.addProperty("name", unlock.display().getTitle().getString());
			o.addProperty("tab", PlayerUnlock.getRoot(holder).value().key());
			o.addProperty("parent", unlock.parent().map(p -> p.unwrapKey().orElseThrow().location().getPath()).orElse(null));
			JsonArray disables = new JsonArray();
			unlock.disables().stream().map(h -> h.unwrapKey().orElseThrow().location().getPath()).sorted().forEach(disables::add);
			o.add("disables", disables);
			o.addProperty("cost", unlock.unlockPrice());
			o.addProperty("visibility", unlock.defaultVisibility().name());
			o.addProperty("visibility_conditions", unlock.madeVisibleBy().size());
			o.addProperty("exclusive", unlock.exclusiveKey());
			return o;
		}).toList());
	}

	private static JsonArray specialMines() {
		Registry<SpecialMine> registry = BuiltInRegistries.SPECIAL_MINE;
		return sorted(registry.stream().map(mine -> {
			JsonObject o = new JsonObject();
			o.addProperty("id", id(registry, mine));
			o.addProperty("name", mine.name().getString());
			o.add("required_effects", ids(BuiltInRegistries.WORLD_EFFECT, mine.requiredEffects()));
			o.addProperty("conditions", mine.unlockedBy().size());
			o.add("unlocked_after", ids(registry, mine.unlockedAfter()));
			return o;
		}).toList());
	}

	private static JsonArray advancements(MinecraftServer server) {
		return sorted(server.theGame().getAdvancements().getAllAdvancements().stream()
			.filter(holder -> holder.id().getNamespace().equals("minecraft"))
			.filter(holder -> ADVANCEMENT_TABS.contains(holder.id().getPath().split("/")[0]))
			.filter(holder -> !holder.id().getPath().endsWith("/root"))
			.map(DataDump::advancement)
			.toList());
	}

	private static JsonObject advancement(AdvancementHolder holder) {
		JsonObject o = new JsonObject();
		o.addProperty("id", holder.id().getPath());
		o.addProperty("name", holder.value().display().map(d -> d.getTitle().getString()).orElse(holder.id().getPath()));
		o.addProperty("parent", holder.value().parent().map(p -> p.getPath()).orElse(null));
		JsonArray triggers = new JsonArray();
		holder.value().criteria().values().stream()
			.map(c -> BuiltInRegistries.TRIGGER_TYPES.getKey(c.trigger()).getPath())
			.distinct().sorted().forEach(triggers::add);
		o.add("triggers", triggers);
		return o;
	}
}
