package craftmineap.mixin;

import craftmineap.ap.ApSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.UnlockCondition;
import net.minecraft.world.level.mines.WorldEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The game stops checking an ingredient's trigger once the ingredient is unlocked. Here it's unlocked by its item,
 * which can arrive before the trigger's check is done, so the trigger keeps firing until its check is sent.
 */
@Mixin(UnlockCondition.class)
public interface UnlockConditionMixin {
	@Redirect(
		method = "checkAndUnlockDrop",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isEffectUnlocked(Lnet/minecraft/world/level/mines/WorldEffect;)Z", ordinal = 0)
	)
	private static boolean craftmineap$untilChecked(ServerLevel level, WorldEffect effect) {
		ApSession session = ApSession.current();
		if (session == null || !session.isActive()) {
			return level.isEffectUnlocked(effect);
		}
		String id = BuiltInRegistries.WORLD_EFFECT.getKey(effect).getPath();
		if (!session.slotData().isItemIngredient(id)) {
			return level.isEffectUnlocked(effect);
		}
		Long location = session.state.locationId("ingredient:" + id);
		return location == null || session.state.checkedLocations.contains(location);
	}
}
