package craftmineap.mixin;

import craftmineap.ap.ApSession;
import net.minecraft.world.MineData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With hub door checks on, shimmering keys only come from the multiworld. */
@Mixin(MineData.class)
public abstract class MineDataMixin {
	@Inject(method = "addKeysToRoll", at = @At("HEAD"), cancellable = true)
	private void craftmineap$noNaturalKeys(int keys, CallbackInfo ci) {
		ApSession session = ApSession.current();
		if (session != null && session.isActive() && session.slotData().hubDoorChecks()) {
			ci.cancel();
		}
	}
}
