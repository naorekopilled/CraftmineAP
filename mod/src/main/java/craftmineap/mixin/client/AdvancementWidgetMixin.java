package craftmineap.mixin.client;

import craftmineap.client.TrackerTab;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the tracker tab's nodes: no connecting lines, and checks out of logic grayed out. */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
	@Shadow
	@Final
	private AdvancementNode advancementNode;
	@Shadow
	@Final
	private int x;
	@Shadow
	@Final
	private int y;

	/** The tracker is a grid, not a tree: no lines between its nodes. */
	@Inject(method = "drawConnectivity", at = @At("HEAD"), cancellable = true)
	private void craftmineap$noTrackerLines(GuiGraphics graphics, int left, int top, boolean shadow, CallbackInfo ci) {
		if (TrackerTab.isTracker(advancementNode.holder().id())) {
			ci.cancel();
		}
	}

	@Inject(method = "draw", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"))
	private void craftmineap$dimOutOfLogic(GuiGraphics graphics, int left, int top, CallbackInfo ci) {
		if (TrackerTab.isOutOfLogic(advancementNode.holder().id())) {
			// items draw 150 up; the hovered widget's tooltip draws above 200
			graphics.pose().pushPose();
			graphics.pose().translate(0.0F, 0.0F, 160.0F);
			graphics.fill(left + x + 3, top + y, left + x + 29, top + y + 26, 0xA0000000);
			graphics.pose().popPose();
		}
	}
}
