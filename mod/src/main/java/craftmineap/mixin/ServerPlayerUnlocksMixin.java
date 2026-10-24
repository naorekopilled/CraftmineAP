package craftmineap.mixin;

import craftmineap.ap.ApSession;
import craftmineap.ap.UnlockGate;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.players.PlayerUnlock;
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

	/** Vanilla only runs mine-start effects for bought nodes; here every active (granted) one runs. */
	@Inject(method = "onMineEnter()V", at = @At("HEAD"), cancellable = true)
	private void craftmineap$mineEnterGranted(CallbackInfo ci) {
		if (UnlockGate.active()) {
			ServerPlayerUnlocksInvoker self = (ServerPlayerUnlocksInvoker) this;
			getActiveUnlocks().forEach(self::craftmineap$onMineEnter);
			ci.cancel();
		}
	}

	@Inject(method = "getActiveUnlocks", at = @At("RETURN"), cancellable = true)
	private void craftmineap$activeFromGranted(CallbackInfoReturnable<Set<Holder<PlayerUnlock>>> cir) {
		if (!UnlockGate.active()) {
			return;
		}
		Set<Holder<PlayerUnlock>> granted = new HashSet<>();
		BuiltInRegistries.PLAYER_UNLOCK.listElements().filter(UnlockGate::granted).forEach(granted::add);
		Set<Holder<PlayerUnlock>> active = new HashSet<>(granted);
		for (Holder<PlayerUnlock> holder : granted) {
			holder.value().disables().forEach(active::remove);
			if (!holder.value().exclusiveKey().isEmpty() && !isActiveExclusive(holder)) {
				active.remove(holder);
			}
		}
		cir.setReturnValue(active);
	}
}
