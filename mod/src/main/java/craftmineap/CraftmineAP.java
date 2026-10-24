package craftmineap;

import craftmineap.ap.ApSession;
import craftmineap.command.ApCommands;
import craftmineap.dev.DataDump;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class CraftmineAP implements ModInitializer {
	@Override
	public void onInitialize() {
		String dumpPath = System.getProperty("craftmineap.dump");
		if (dumpPath != null) {
			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				DataDump.write(server, Path.of(dumpPath));
				server.halt(false);
			});
			return;
		}
		ServerLifecycleEvents.SERVER_STARTED.register(ApSession::open);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ApSession.close());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ApCommands.register(dispatcher));
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			ApSession session = ApSession.current();
			if (session != null) {
				session.sendChat(message.signedContent());
			}
		});
	}
}
