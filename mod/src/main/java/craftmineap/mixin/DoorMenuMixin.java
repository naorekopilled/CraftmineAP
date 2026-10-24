package craftmineap.mixin;

import craftmineap.ap.ApSession;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DoorMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorMenu.class)
public abstract class DoorMenuMixin {
	@Shadow
	@Final
	private List<DoorMenu.Layout> displays;

	@Shadow
	@Final
	private List<DoorMenu.PlacementInfo> actualPlacements;

	/** Button 2 places the chosen room behind the door. */
	@Inject(method = "clickMenuButton", at = @At("HEAD"))
	private void craftmineap$doorOpened(Player player, int button, CallbackInfoReturnable<Boolean> cir) {
		ApSession session = ApSession.current();
		if (button != 2 || !(player instanceof ServerPlayer) || displays.isEmpty() || actualPlacements == null
			|| session == null || !session.isActive()) {
			return;
		}
		session.state.doorsOpened++;
		session.save();
		session.sendCheck("door:" + session.state.doorsOpened);
	}
}
