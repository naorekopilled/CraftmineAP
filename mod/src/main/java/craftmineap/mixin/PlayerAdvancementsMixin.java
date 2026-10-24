package craftmineap.mixin;

import craftmineap.ap.ApSession;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	public abstract net.minecraft.advancements.AdvancementProgress getOrStartProgress(AdvancementHolder holder);

	@Inject(method = "award", at = @At("RETURN"))
	private void craftmineap$advancementDone(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		ApSession session = ApSession.current();
		if (cir.getReturnValueZ() && session != null && getOrStartProgress(holder).isDone()) {
			session.sendAdvancement(holder);
		}
	}
}
