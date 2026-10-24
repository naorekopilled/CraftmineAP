package craftmineap.ap;

import craftmineap.core.Grants;
import craftmineap.mixin.ServerPlayerAccessor;
import craftmineap.mixin.ServerPlayerUnlocksInvoker;
import java.util.List;
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
	private static final ThreadLocal<Boolean> GRANTING = ThreadLocal.withInitial(() -> false);
	static final int XP_BUNDLE_LEVELS = 5;

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

	/** Updates the unlock gate from the saved items; runs when the world loads, before any player joins. */
	public static void refreshGate(ApSession session) {
		UnlockGate.set(session.isActive(), session.isActive() ? Grants.from(session.state).unlocks() : java.util.Set.of());
	}

	/** Applies everything received so far that isn't applied yet. Safe to call any number of times. */
	public static void applyAll(ApSession session) {
		refreshGate(session);
		if (!session.isActive()) {
			return;
		}
		Grants grants = Grants.from(session.state);
		ServerLevel hub = session.server.theGame().overworld();
		for (String id : grants.ingredients()) {
			WorldEffect effect = BuiltInRegistries.WORLD_EFFECT.getValue(ResourceLocation.withDefaultNamespace(id));
			if (effect != null && !hub.isEffectUnlocked(effect)) {
				granting(() -> hub.unlockEffect(effect));
			}
		}
		List<ServerPlayer> players = session.server.theGame().playerList().getPlayers();
		if (players.isEmpty()) {
			return;
		}
		for (ServerPlayer player : players) {
			activateGranted(player, grants);
		}
		ServerPlayer player = players.get(0);
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

	/** Runs every granted unlock's activation (idempotent: attribute modifiers are replaced, not stacked). */
	private static void activateGranted(ServerPlayer player, Grants grants) {
		ServerPlayerUnlocksInvoker unlocks = (ServerPlayerUnlocksInvoker) ((ServerPlayerAccessor) player).craftmineap$unlocks();
		for (Holder<PlayerUnlock> holder : BuiltInRegistries.PLAYER_UNLOCK.listElements().toList()) {
			if (grants.unlocks().contains(holder.unwrapKey().orElseThrow().location().getPath())) {
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
