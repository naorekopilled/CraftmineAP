package craftmineap.ap;

import craftmineap.Text;
import craftmineap.core.ApState;
import craftmineap.core.ChatColors;
import craftmineap.core.ConnectArgs;
import craftmineap.core.DeathLinkGate;
import craftmineap.core.SlotData;
import net.minecraft.advancements.AdvancementHolder;
import io.github.archipelagomw.ClientStatus;
import io.github.archipelagomw.Print.APPrintPart;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Archipelago side of one running world. */
public final class ApSession {
	private static final Logger LOGGER = LoggerFactory.getLogger("craftmineap");
	private static final int RETRY_SECONDS = 30;
	private static ApSession current;

	/** Runs after a successful connect and after a game restart, on the server thread (catch-up checks, item application). */
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
	/** What the current client connects with; saved only once the server accepts it for this world. */
	private ConnectArgs connecting;
	/** After a connection worked, a close the client library won't retry is retried here, quietly. */
	private boolean retry;
	private boolean retrying;
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
		// entering a mine restarts the game inside the same server without a stop event: keep the session and its
		// connection (a reconnect shows as a leave and a join to the whole room), only redo what the new game needs
		if (current != null && current.server == server) {
			ON_CONNECTED.forEach(hook -> hook.accept(current));
			return;
		}
		close();
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

	/** The game logs system messages itself; during a game restart there is no game, so only the log gets it. */
	public void broadcast(Component message) {
		if (gameRunning()) {
			server.theGame().playerList().broadcastSystemMessage(message, false);
		} else {
			LOGGER.info(message.getString());
		}
	}

	private boolean gameRunning() {
		return server.theGame() != null;
	}

	public void connect(String address, String slot, String password) {
		retry = false;
		retrying = false;
		openClient(new ConnectArgs(address, slot, password));
	}

	private void openClient(ConnectArgs args) {
		closeClient();
		connecting = args;
		client = new ApClient(this);
		client.setName(args.slot());
		client.setPassword(args.password() == null ? "" : args.password());
		if (!retrying) {
			broadcast(Text.connecting(args.address()));
		}
		try {
			client.connect(args.address());
		} catch (URISyntaxException e) {
			broadcast(Text.error(e.getMessage()));
		}
	}

	public void disconnect() {
		retry = false;
		retrying = false;
		closeClient();
	}

	private void closeClient() {
		if (client != null) {
			ApClient old = client;
			client = null;
			old.close();
		}
	}

	/** For apdev drop: closes the connection like a server's clean close, which the client library doesn't retry. */
	public void dropConnection() {
		if (client != null) {
			client.close();
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

	/** A special mine won: its check, and the goal once enough are won. */
	public void mineWon(String mineId) {
		if (!isActive()) {
			return;
		}
		state.specialMinesWon.add(mineId);
		save();
		sendCheck("mine:" + mineId);
		sendGoal();
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

	/** Runs on the server thread unless the session closed; anything touching the game checks gameRunning() itself. */
	private void onServer(Runnable task) {
		server.execute(() -> {
			if (!closed) {
				task.run();
			}
		});
	}

	void onConnected(String seedName, SlotData slotData) {
		onServer(() -> {
			if (client == null) {
				return;
			}
			if (!state.claimRoom(seedName, connecting.address(), connecting.slot(), connecting.password())) {
				broadcast(Text.wrongRoom());
				disconnect();
				return;
			}
			retry = true;
			retrying = false;
			state.slotData = slotData;
			save();
			client.setDeathLinkEnabled(slotData.deathLink());
			client.checkLocations(new ArrayList<>(state.checkedLocations));
			broadcast(Text.connected(state.slot));
			if (!state.started) {
				broadcast(Text.typeStart());
				return;
			}
			if (gameRunning()) {
				ON_CONNECTED.forEach(hook -> hook.accept(this));
			}
			sendGoal();
		});
	}

	void onRefused(String reason) {
		onServer(() -> {
			retry = false;
			retrying = false;
			broadcast(Text.refused(reason));
		});
	}

	void onError(String message) {
		// the client library tries wss first and falls back to ws, so errors are only logged; a real failure closes
		LOGGER.warn("Archipelago error: {}", message);
	}

	/** libraryRetrySeconds > 0: the client library reconnects by itself (it does after a lost connection). */
	void onClose(ApClient from, String reason, int libraryRetrySeconds) {
		onServer(() -> {
			if (from != client) {
				return;  // replaced or closed on purpose
			}
			if (!retrying) {
				broadcast(Text.disconnected(reason));
			}
			if (!retry) {
				return;
			}
			retrying = true;
			if (libraryRetrySeconds > 0) {
				return;
			}
			CompletableFuture.delayedExecutor(RETRY_SECONDS, TimeUnit.SECONDS).execute(() -> onServer(() -> {
				if (retry && client == from) {
					openClient(new ConnectArgs(state.address, state.slot, state.password));
				}
			}));
		});
	}

	void onItem(int index, long itemId) {
		onServer(() -> {
			// accepted even during a game restart, so the item list never gets a gap; the restart applies it
			if (!state.acceptItem(index, itemId).isEmpty()) {
				save();
				if (gameRunning()) {
					ON_ITEMS.forEach(hook -> hook.accept(this));
				}
			}
		});
	}

	void onPrint(APPrintPart[] parts) {
		onServer(() -> {
			MutableComponent message = Component.empty();
			for (ChatColors.Segment segment : ChatColors.segments(parts, state.slot)) {
				MutableComponent piece = Component.literal(segment.text());
				message.append(segment.rgb() == null ? piece : piece.withColor(segment.rgb()));
			}
			broadcast(message);
		});
	}

	void onDeathLink(String source, String cause) {
		onServer(() -> {
			broadcast(Text.deathLink(source, cause));
			if (gameRunning()) {
				ON_DEATH_LINK.forEach(hook -> hook.accept(this));
			}
		});
	}
}
