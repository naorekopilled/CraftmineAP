package craftmineap;

import craftmineap.ap.ApSession;
import craftmineap.ap.CatchUp;
import craftmineap.command.ApCommands;
import craftmineap.dev.DataDump;
import craftmineap.dev.DevCommands;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ApCommands.register(dispatcher);
			if (System.getProperty("craftmineap.dev") != null) {
				DevCommands.register(dispatcher);
			}
		});
		ApSession.ON_CONNECTED.add(CatchUp::all);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ApSession session = ApSession.current();
			if (session != null) {
				CatchUp.player(session, handler.getPlayer());
			}
		});
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			ApSession session = ApSession.current();
			if (session != null) {
				session.sendChat(message.signedContent());
			}
		});
	}
}
