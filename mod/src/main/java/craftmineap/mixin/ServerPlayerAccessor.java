package craftmineap.mixin;

import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
	@Accessor("playerUnlocks")
	ServerPlayerUnlocks craftmineap$unlocks();
}
