package craftmineap.mixin.client;

import craftmineap.ap.UnlockGate;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.unlocks.PlayerUnlockWidget;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The skill tree's frames show what was bought (the checks); a green glow shows the upgrades received. */
@Mixin(PlayerUnlockWidget.class)
public abstract class PlayerUnlockWidgetMixin {
	@Shadow
	@Final
	private Holder<PlayerUnlock> node;
	@Shadow
	@Final
	DisplayInfo display;
	@Shadow
	private boolean unlocked;

	@Shadow
	protected abstract int getX();

	@Shadow
	protected abstract int getY();

	@Inject(method = "draw", at = @At("HEAD"))
	private void craftmineap$glowReceived(GuiGraphics graphics, int left, int top, CallbackInfo ci) {
		if (UnlockGate.active() && UnlockGate.granted(node) && (!display.isHidden() || unlocked)) {
			// drawn under the 26x26 frame, so only the edge around it shows
			int x = left + getX() + 3;
			int y = top + getY();
			graphics.fill(x - 3, y - 3, x + 29, y + 29, 0x603FE06A);
			graphics.fill(x - 1, y - 1, x + 27, y + 27, 0xFF3FE06A);
		}
	}
}
