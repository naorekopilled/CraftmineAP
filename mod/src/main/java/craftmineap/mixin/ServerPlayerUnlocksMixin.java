package craftmineap.mixin;

import craftmineap.ap.ApSession;
import craftmineap.ap.UnlockGate;
import craftmineap.core.ActiveUnlocks;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.players.PlayerUnlock;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bought node stays bought (tree, UI, advancements); only a received item gives its effect. */
@Mixin(ServerPlayerUnlocks.class)
public abstract class ServerPlayerUnlocksMixin {
	@Shadow
	public abstract boolean isActiveExclusive(Holder<PlayerUnlock> holder);

	@Shadow
	public abstract Set<Holder<PlayerUnlock>> getActiveUnlocks();

	/** Buying a skill-tree node is its check. */
	@Inject(method = "unlock", at = @At("RETURN"))
	private void craftmineap$nodeBought(Holder<PlayerUnlock> holder, CallbackInfoReturnable<Boolean> cir) {
		ApSession session = ApSession.current();
		if (cir.getReturnValueZ() && session != null) {
			session.sendCheck("node:" + holder.unwrapKey().orElseThrow().location().getPath());
		}
	}

	@Inject(method = "activate", at = @At("HEAD"), cancellable = true)
	private void craftmineap$activateOnlyGranted(Holder<PlayerUnlock> holder, CallbackInfo ci) {
		if (UnlockGate.active() && !UnlockGate.granted(holder)) {
			ci.cancel();
		}
	}

	@Inject(method = "onMineEnter(Lnet/minecraft/core/Holder;)V", at = @At("HEAD"), cancellable = true)
	private void craftmineap$mineEnterOnlyGranted(Holder<PlayerUnlock> holder, CallbackInfo ci) {
		if (UnlockGate.active() && !UnlockGate.granted(holder)) {
			ci.cancel();
		}
	}

	/**
	 * Vanilla only runs mine-start effects for bought nodes; here every active (granted) one runs. One failing must not
	 * stop the rest, and the game runs this inside a future that drops exceptions, so each is caught and logged.
	 */
	@Inject(method = "onMineEnter()V", at = @At("HEAD"), cancellable = true)
	private void craftmineap$mineEnterGranted(CallbackInfo ci) {
		if (UnlockGate.active()) {
			ServerPlayerUnlocksInvoker self = (ServerPlayerUnlocksInvoker) this;
			for (Holder<PlayerUnlock> holder : getActiveUnlocks()) {
				try {
					self.craftmineap$onMineEnter(holder);
				} catch (RuntimeException e) {
					LoggerFactory.getLogger("craftmineap").error("Mine start effect of {} failed", holder.getRegisteredName(), e);
				}
			}
			ci.cancel();
		}
	}

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
