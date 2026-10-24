package craftmineap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class CraftmineAPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// dev check (tools/smoke.sh client): apply every mixin now, then quit
		if (System.getProperty("craftmineap.mixinaudit") != null) {
			ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
				MixinEnvironment.getCurrentEnvironment().audit();
				LoggerFactory.getLogger("craftmineap").info("craftmineap mixin audit done");
				Minecraft.getInstance().stop();
			});
		}
	}
}
