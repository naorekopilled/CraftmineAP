package craftmineap.mixin.client;

import craftmineap.Text;
import craftmineap.ap.UnlockGate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.unlocks.PlayerUnlockWidget;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The skill tree's frames show what was bought (the checks); a green glow shows the upgrades received, a red one what
 * can't be bought yet. Red wins: it's the one that says why a click does nothing.
 */
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
	private PlayerUnlock.UnlockVisibility visibility;

	@Shadow
	protected abstract int getX();

	@Shadow
	protected abstract int getY();

	@Inject(method = "draw", at = @At("HEAD"))
	private void craftmineap$glow(GuiGraphics graphics, int left, int top, CallbackInfo ci) {
		if (!UnlockGate.active() || (display.isHidden() && !unlocked)) {
			return;
		}
		boolean closed = !unlocked && UnlockGate.neededTabItem(node) != null;
		if (closed || UnlockGate.granted(node)) {
			// drawn under the 26x26 frame, so only the edge around it shows
			int x = left + getX() + 3;
			int y = top + getY();
			graphics.fill(x - 3, y - 3, x + 29, y + 29, closed ? 0x60E03F3F : 0x603FE06A);
			graphics.fill(x - 1, y - 1, x + 27, y + 27, closed ? 0xFFE03F3F : 0xFF3FE06A);
		}
	}

	@Inject(method = "getDescriptionText", at = @At("RETURN"), cancellable = true)
	private void craftmineap$tabNeeded(CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
		String tabItem = unlocked || visibility != PlayerUnlock.UnlockVisibility.VISIBLE ? null : UnlockGate.neededTabItem(node);
		if (tabItem != null) {
			List<FormattedCharSequence> lines = new ArrayList<>(cir.getReturnValue());
			lines.add(Text.tabClosed(tabItem).getVisualOrderText());
			cir.setReturnValue(lines);
		}
	}
}
