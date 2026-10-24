package craftmineap.mixin;

import craftmineap.ap.ApSession;
import craftmineap.ap.ItemApplier;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.mines.SpecialMine;
import net.minecraft.world.level.mines.WorldEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Shadow
	public abstract Optional<SpecialMine> specialMine();

	@Shadow
	public abstract int getMineCrafterLevel();

	/** Every way the game unlocks an ingredient ends here: that becomes the ingredient's check instead. */
	@Inject(method = "unlockEffect", at = @At("HEAD"), cancellable = true)
	private void craftmineap$checkInsteadOfUnlock(WorldEffect effect, CallbackInfo ci) {
		ApSession session = ApSession.current();
		if (session == null || !session.isActive() || ItemApplier.isGranting(effect)) {
			return;
		}
		String id = BuiltInRegistries.WORLD_EFFECT.getKey(effect).getPath();
		if (session.slotData().isItemIngredient(id)) {
			session.sendCheck("ingredient:" + id);
			ci.cancel();
		}
	}

	@Inject(method = "markMineCompleted", at = @At("HEAD"))
	private void craftmineap$specialMineWon(boolean won, CallbackInfo ci) {
		ApSession session = ApSession.current();
		if (session != null && won) {
			specialMine().ifPresent(mine -> session.mineWon(BuiltInRegistries.SPECIAL_MINE.getKey(mine).getPath()));
		}
	}

	@Inject(method = "addExperienceToMineCrafter", at = @At("RETURN"))
	private void craftmineap$crafterLevels(int experience, CallbackInfo ci) {
		ApSession session = ApSession.current();
		if (session != null) {
			session.sendCrafterLevels(getMineCrafterLevel());
		}
	}
}
