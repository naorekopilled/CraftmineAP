package craftmineap.ap;

import craftmineap.core.Logic;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;

/**
 * Which skill-tree unlocks have their effect, and which can't be bought yet. Static because the client reads it too:
 * singleplayer only, so the integrated server and the client share this JVM.
 */
public final class UnlockGate {
	private static volatile boolean active;
	private static volatile Set<String> granted = Set.of();
	private static volatile Set<String> closedTabs = Set.of();

	private UnlockGate() {
	}

	public static boolean active() {
		return active;
	}

	public static boolean granted(Holder<PlayerUnlock> holder) {
		return holder.unwrapKey().map(k -> granted.contains(k.location().getPath())).orElse(false);
	}

	/** The tab item this node can't be bought without, or null. */
	public static String neededTabItem(Holder<PlayerUnlock> holder) {
		Logic logic = Logic.bundled();
		return holder.unwrapKey().map(k -> logic.node(k.location().getPath())).map(Logic.Node::tab)
			.filter(closedTabs::contains).map(logic::tabItem).orElse(null);
	}

	static Set<String> grantedIds() {
		return granted;
	}

	static void set(boolean isActive, Set<String> grantedUnlocks, Set<String> closed) {
		granted = Set.copyOf(grantedUnlocks);
		closedTabs = Set.copyOf(closed);
		active = isActive;
	}
}
