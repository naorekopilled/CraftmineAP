package craftmineap.mixin;

import craftmineap.ap.ApSession;
import net.minecraft.core.Holder;
import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.players.PlayerUnlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerUnlocks.class)
public abstract class ServerPlayerUnlocksMixin {
	/** Buying a skill-tree node is its check. */
	@Inject(method = "unlock", at = @At("RETURN"))
	private void craftmineap$nodeBought(Holder<PlayerUnlock> holder, CallbackInfoReturnable<Boolean> cir) {
		ApSession session = ApSession.current();
		if (cir.getReturnValueZ() && session != null) {
			session.sendCheck("node:" + holder.unwrapKey().orElseThrow().location().getPath());
		}
	}
}
