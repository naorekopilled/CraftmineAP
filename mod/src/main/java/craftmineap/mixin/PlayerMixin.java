package craftmineap.mixin;

import craftmineap.ap.UnlockGate;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Client and server both ask here first, so a refused buy costs no XP and sends no packet or check. */
	@Inject(method = "buyUnlock", at = @At("HEAD"), cancellable = true)
	private void craftmineap$tabOpen(Holder<PlayerUnlock> holder, CallbackInfoReturnable<Boolean> cir) {
		if (UnlockGate.neededTabItem(holder) != null) {
			cir.setReturnValue(false);
		}
	}
}
