package craftmineap.ap;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerUnlock;

/** Sends checks for things already done: on connect, on /start and when the player joins. */
public final class CatchUp {
	private CatchUp() {
	}

	public static void all(ApSession session) {
		if (!session.isActive()) {
			return;
		}
		session.sendCrafterLevels(session.server.theGame().overworld().getMineCrafterLevel());
		session.server.theGame().playerList().getPlayers().forEach(player -> player(session, player));
	}

	public static void player(ApSession session, ServerPlayer player) {
		if (!session.isActive()) {
			return;
		}
		for (AdvancementHolder holder : session.server.theGame().getAdvancements().getAllAdvancements()) {
			if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
				session.sendAdvancement(holder);
			}
		}
		for (Holder<PlayerUnlock> unlock : BuiltInRegistries.PLAYER_UNLOCK.listElements().toList()) {
			if (player.isUnlocked(unlock)) {
				session.sendCheck("node:" + unlock.unwrapKey().orElseThrow().location().getPath());
			}
		}
	}
}
