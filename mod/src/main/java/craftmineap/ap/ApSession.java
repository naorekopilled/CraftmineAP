package craftmineap.ap;

import craftmineap.Text;
import craftmineap.core.ApState;
import craftmineap.core.DeathLinkGate;
import craftmineap.core.SlotData;
import net.minecraft.advancements.AdvancementHolder;
import io.github.archipelagomw.ClientStatus;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Archipelago side of one running world. */
public final class ApSession {
	private static final Logger LOGGER = LoggerFactory.getLogger("craftmineap");
	private static ApSession current;

	/** Runs after a successful connect, on the server thread (catch-up checks, item application). */
	public static final List<Consumer<ApSession>> ON_CONNECTED = new ArrayList<>();
	/** Runs on the server thread after new items were accepted. */
	public static final List<Consumer<ApSession>> ON_ITEMS = new ArrayList<>();
	/** Runs on the server thread for a received Death Link. */
	public static final List<Consumer<ApSession>> ON_DEATH_LINK = new ArrayList<>();

	public final MinecraftServer server;
	public final ApState state;
	public final DeathLinkGate deathLinkGate = new DeathLinkGate();
	private final Path file;
	private ApClient client;
	private boolean closed;

	private ApSession(MinecraftServer server) {
		this.server = server;
		this.file = server.getWorldPath(LevelResource.ROOT).resolve("craftmineap.json");
		this.state = ApState.load(file);
	}

	public static ApSession current() {
		return current;
	}

	public static void open(MinecraftServer server) {
		current = new ApSession(server);
		ItemApplier.refreshGate(current);
		String auto = System.getProperty("craftmineap.autoconnect");
		if (auto != null) {
			String[] parts = auto.split(",", 3);
			current.connect(parts[0], parts[1], parts.length > 2 ? parts[2] : null);
		} else if (current.state.address != null) {
			current.connect(current.state.address, current.state.slot, current.state.password);
		}
	}

	public static void close() {
		if (current != null) {
			UnlockGate.set(false, java.util.Set.of());
			current.closed = true;
			current.disconnect();
			current.save();
			current = null;
		}
	}

	public boolean isActive() {
		return state.started && state.slotData != null;
	}

	public boolean isConnected() {
		return client != null && client.isConnected();
	}

	public SlotData slotData() {
		return state.slotData;
	}

	public void save() {
		state.save(file);
	}

	public void broadcast(Component message) {
		server.theGame().playerList().broadcastSystemMessage(message, false);
		LOGGER.info(message.getString());
	}

	public void connect(String address, String slot, String password) {
		disconnect();
		state.address = address;
		state.slot = slot;
		state.password = password;
		save();
		client = new ApClient(this);
		client.setName(slot);
		client.setPassword(password == null ? "" : password);
		broadcast(Text.connecting(address));
		try {
			client.connect(address);
		} catch (URISyntaxException e) {
			broadcast(Text.error(e.getMessage()));
		}
	}

	public void disconnect() {
		if (client != null) {
			ApClient old = client;
			client = null;
			old.close();
		}
	}

	public void sync() {
		if (isConnected()) {
			client.sync();
		}
	}

	public void start() {
		state.started = true;
		save();
		broadcast(Text.started());
		ON_CONNECTED.forEach(hook -> hook.accept(this));
	}

	/** Sends the check for a game key like "node:archer"; remembered offline and sent on the next connect. */
	public void sendCheck(String gameKey) {
		if (!isActive()) {
			return;
		}
		Long id = state.locationId(gameKey);
		if (id == null || !state.markChecked(id)) {
			return;
		}
		save();
		if (isConnected()) {
			client.checkLocation(id);
		}
		if (state.goalReached()) {
			sendGoal();
		}
	}

	public void sendCrafterLevels(int level) {
		if (!isActive()) {
			return;
		}
		for (int n = 1; n <= Math.min(level, state.slotData.crafterLevelChecks()); n++) {
			sendCheck("crafter:" + n);
		}
	}

	public void sendAdvancement(AdvancementHolder holder) {
		if (holder.id().getNamespace().equals("minecraft")) {
			sendCheck("advancement:" + holder.id().getPath());
		}
	}

	public void sendGoal() {
		if (isActive() && state.goalReached() && !state.goalSent && isConnected()) {
			client.setGameState(ClientStatus.CLIENT_GOAL);
			state.goalSent = true;
			save();
		}
	}

	public void sendDeathLink(String cause) {
		if (isConnected()) {
			client.sendDeathlink(state.slot, cause);
		}
	}

	public void sendChat(String message) {
		if (isConnected()) {
			client.sendChat(message);
		}
	}

	private void onServer(Runnable task) {
		server.execute(() -> {
			if (!closed && server.theGame() != null) {
				task.run();
			}
		});
	}

	void onConnected(SlotData slotData) {
		onServer(() -> {
			if (client == null) {
				return;
			}
			state.slotData = slotData;
			save();
			client.setDeathLinkEnabled(slotData.deathLink());
			client.checkLocations(new ArrayList<>(state.checkedLocations));
			broadcast(Text.connected(state.slot));
			if (!state.started) {
				broadcast(Text.typeStart());
				return;
			}
			ON_CONNECTED.forEach(hook -> hook.accept(this));
			sendGoal();
		});
	}

	void onRefused(String reason) {
		onServer(() -> broadcast(Text.refused(reason)));
	}

	void onError(String message) {
		// the client library tries wss first and falls back to ws, so errors are only logged; a real failure closes
		LOGGER.warn("Archipelago error: {}", message);
	}

	void onClose(String reason) {
		onServer(() -> broadcast(Text.disconnected(reason)));
	}

	void onItem(int index, long itemId) {
		onServer(() -> {
			if (!state.acceptItem(index, itemId).isEmpty()) {
				save();
				ON_ITEMS.forEach(hook -> hook.accept(this));
			}
		});
	}

	void onPrint(String text) {
		onServer(() -> server.theGame().playerList().broadcastSystemMessage(Component.literal(text), false));
	}

	void onDeathLink(String source, String cause) {
		onServer(() -> {
			broadcast(Text.deathLink(source, cause));
			ON_DEATH_LINK.forEach(hook -> hook.accept(this));
		});
	}
}
