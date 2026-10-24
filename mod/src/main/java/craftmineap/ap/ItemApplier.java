package craftmineap.ap;

import craftmineap.core.Grants;
import craftmineap.mixin.ServerPlayerAccessor;
import craftmineap.mixin.ServerPlayerUnlocksInvoker;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;

/** Applies received items to the world and players. */
public final class ItemApplier {
	private static final ThreadLocal<WorldEffect> GRANTING = new ThreadLocal<>();
	static final int XP_BUNDLE_LEVELS = 5;

	private ItemApplier() {
	}

	/**
	 * True while the mod itself unlocks this ingredient, so the check hook lets it through. Only that one: vanilla
	 * can unlock others while reacting to it, and those must still become checks.
	 */
	public static boolean isGranting(WorldEffect effect) {
		return GRANTING.get() == effect;
	}

	private static void grant(ServerLevel hub, WorldEffect effect) {
		GRANTING.set(effect);
		try {
			hub.unlockEffect(effect);
		} finally {
			GRANTING.remove();
		}
	}

	/** Updates the unlock gate from the saved items; runs when the world loads, before any player joins. */
	public static void refreshGate(ApSession session) {
		UnlockGate.set(session.isActive(), session.isActive() ? Grants.from(session.state).unlocks() : Set.of());
	}

	/** Applies everything received so far that isn't applied yet. Safe to call any number of times. */
	public static void applyAll(ApSession session) {
		Set<String> grantedBefore = UnlockGate.grantedIds();
		refreshGate(session);
		if (!session.isActive()) {
			return;
		}
		Grants grants = Grants.from(session.state);
		ServerLevel hub = session.server.theGame().overworld();
		for (String id : grants.ingredients()) {
			WorldEffect effect = BuiltInRegistries.WORLD_EFFECT.getValue(ResourceLocation.withDefaultNamespace(id));
			if (effect != null && !hub.isEffectUnlocked(effect)) {
				grant(hub, effect);
			}
		}
		List<ServerPlayer> players = session.server.theGame().playerList().getPlayers();
		if (players.isEmpty()) {
			return;
		}
		for (ServerPlayer player : players) {
			activateGranted(player, grants, grantedBefore);
		}
		ServerPlayer player = players.get(0);
		// a mine turns leftover items into XP when won and clears the inventory when lost; keys and XP wait for the hub
		if (player.serverLevel().isMine()) {
			return;
		}
		while (session.state.keysGiven < grants.keys()) {
			hub.getRewardKeys(player.blockPosition()).forEach(key -> give(player, key));
			session.state.keysGiven++;
		}
		while (session.state.xpGiven < grants.xpBundles()) {
			player.giveExperienceLevels(XP_BUNDLE_LEVELS);
			session.state.xpGiven++;
		}
		session.save();
	}

	/**
	 * Runs granted unlocks' activation (idempotent: attribute modifiers are replaced, not stacked). Exclusive ones
	 * (pets) only when newly granted: activating one switches the others in its group off, and that is the player's
	 * choice once they have it.
	 */
	private static void activateGranted(ServerPlayer player, Grants grants, Set<String> grantedBefore) {
		ServerPlayerUnlocksInvoker unlocks = (ServerPlayerUnlocksInvoker) ((ServerPlayerAccessor) player).craftmineap$unlocks();
		for (Holder<PlayerUnlock> holder : BuiltInRegistries.PLAYER_UNLOCK.listElements().toList()) {
			String id = holder.unwrapKey().orElseThrow().location().getPath();
			boolean exclusive = !holder.value().exclusiveKey().isEmpty();
			if (grants.unlocks().contains(id) && (!exclusive || !grantedBefore.contains(id))) {
				unlocks.craftmineap$activate(holder);
			}
		}
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}
}
