package craftmineap.core;

/** Decides when a death goes out as a Death Link, and keeps a received one from bouncing back. */
public final class DeathLinkGate {
	private boolean receiving;

	public boolean shouldSend(boolean enabled, boolean inMine) {
		return enabled && inMine && !receiving;
	}

	public void receiving(Runnable action) {
		receiving = true;
		try {
			action.run();
		} finally {
			receiving = false;
		}
	}
}
