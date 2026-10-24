package craftmineap.mixin.client;

import craftmineap.ap.UnlockGate;
import craftmineap.core.ActiveUnlocks;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientPlayerUnlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.players.PlayerUnlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The client checks unlocks too (crafting grid, inventory and armor slots); it has to agree with the server. */
@Mixin(ClientPlayerUnlocks.class)
public abstract class ClientPlayerUnlocksMixin {
	@Shadow
	public abstract boolean isActiveExclusive(Holder<PlayerUnlock> holder);

	@Inject(method = "getActiveUnlocks", at = @At("RETURN"), cancellable = true)
	private void craftmineap$activeFromGranted(CallbackInfoReturnable<Set<Holder<PlayerUnlock>>> cir) {
		if (UnlockGate.active()) {
			cir.setReturnValue(ActiveUnlocks.compute(
				BuiltInRegistries.PLAYER_UNLOCK.listElements().filter(UnlockGate::granted).map(h -> (Holder<PlayerUnlock>) h).toList(),
				h -> h.value().disables(),
				h -> !h.value().exclusiveKey().isEmpty() && !isActiveExclusive(h)));
		}
	}
}
