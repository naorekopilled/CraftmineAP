package craftmineap.mixin.client;

import craftmineap.client.TrackerTab;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tabs open centered on their content; the tracker's grid opens at its top-left corner instead. */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
	@Shadow
	@Final
	private AdvancementNode rootNode;
	@Shadow
	private double scrollX;
	@Shadow
	private double scrollY;
	@Shadow
	private boolean centered;

	@Inject(method = "drawContents", at = @At("HEAD"))
	private void craftmineap$trackerStartsTopLeft(GuiGraphics graphics, int left, int top, CallbackInfo ci) {
		if (!centered && TrackerTab.isTracker(rootNode.holder().id())) {
			scrollX = 0;
			scrollY = 0;
			centered = true;
		}
	}
}
