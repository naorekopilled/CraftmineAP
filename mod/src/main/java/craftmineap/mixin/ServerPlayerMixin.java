package craftmineap.mixin;

import craftmineap.ap.ApSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "die", at = @At("HEAD"))
	private void craftmineap$deathLink(DamageSource source, CallbackInfo ci) {
		ApSession session = ApSession.current();
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (session != null && session.isActive()
			&& session.deathLinkGate.shouldSend(session.slotData().deathLink(), self.serverLevel().isMine())) {
			session.sendDeathLink(source.getLocalizedDeathMessage(self).getString());
		}
	}
}
