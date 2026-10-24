package craftmineap.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.players.PlayerUnlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerUnlocks.class)
public interface ServerPlayerUnlocksInvoker {
	@Invoker("activate")
	void craftmineap$activate(Holder<PlayerUnlock> holder);

	@Invoker("onMineEnter")
	void craftmineap$onMineEnter(Holder<PlayerUnlock> holder);
}
