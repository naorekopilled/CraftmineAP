package craftmineap.mixin.client;

import craftmineap.client.TrackerTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Tracker tab after the game's own tabs, before the screen picks which tab to show. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin {
	@Inject(method = "init", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setListener(Lnet/minecraft/client/multiplayer/ClientAdvancements$Listener;)V"))
	private void craftmineap$addTracker(CallbackInfo ci) {
		TrackerTab.addTo((AdvancementsScreen) (Object) this);
	}
}
