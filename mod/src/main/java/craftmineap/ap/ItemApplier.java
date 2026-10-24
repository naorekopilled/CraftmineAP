package craftmineap.ap;

/** Applies received items to the world and players. */
public final class ItemApplier {
	private static final ThreadLocal<Boolean> GRANTING = ThreadLocal.withInitial(() -> false);

	private ItemApplier() {
	}

	/** True while the mod itself unlocks something, so the check hooks let it through. */
	public static boolean isGranting() {
		return GRANTING.get();
	}

	static void granting(Runnable action) {
		GRANTING.set(true);
		try {
			action.run();
		} finally {
			GRANTING.set(false);
		}
	}
}
