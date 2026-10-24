package craftmineap.ap;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;

/**
 * Which skill-tree unlocks have their effect. Static because the client reads it too: singleplayer only, so the
 * integrated server and the client share this JVM.
 */
public final class UnlockGate {
	private static volatile boolean active;
	private static volatile Set<String> granted = Set.of();

	private UnlockGate() {
	}

	public static boolean active() {
		return active;
	}

	public static boolean granted(Holder<PlayerUnlock> holder) {
		return holder.unwrapKey().map(k -> granted.contains(k.location().getPath())).orElse(false);
	}

	static Set<String> grantedIds() {
		return granted;
	}

	static void set(boolean isActive, Set<String> grantedUnlocks) {
		granted = Set.copyOf(grantedUnlocks);
		active = isActive;
	}
}
