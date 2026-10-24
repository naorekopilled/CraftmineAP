package craftmineap;

import craftmineap.dev.DataDump;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class CraftmineAP implements ModInitializer {
	@Override
	public void onInitialize() {
		String dumpPath = System.getProperty("craftmineap.dump");
		if (dumpPath != null) {
			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				DataDump.write(server, Path.of(dumpPath));
				server.halt(false);
			});
		}
	}
}
