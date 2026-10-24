package craftmineap.mixin.client;

import craftmineap.ap.UnlockGate;
import net.minecraft.client.multiplayer.ClientPlayerUnlocks;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The client checks unlocks too (crafting grid, inventory slots); it has to agree with the server. */
@Mixin(ClientPlayerUnlocks.class)
public abstract class ClientPlayerUnlocksMixin {
	@Inject(method = "isActive", at = @At("RETURN"), cancellable = true)
	private void craftmineap$onlyGranted(Holder<PlayerUnlock> holder, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && UnlockGate.active() && !UnlockGate.granted(holder)) {
			cir.setReturnValue(false);
		}
	}
}
