package craftmineap.ap;

import com.google.gson.JsonObject;
import craftmineap.core.SlotData;
import io.github.archipelagomw.Client;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ConnectionResultEvent;
import io.github.archipelagomw.events.DeathLinkEvent;
import io.github.archipelagomw.events.PrintJSONEvent;
import io.github.archipelagomw.events.ReceiveItemEvent;
import io.github.archipelagomw.flags.ItemsHandling;
import io.github.archipelagomw.network.ConnectionResult;

/** Runs on the client library's network thread; everything is handed to the session, which hops to the server thread. */
public class ApClient extends Client {
	private final ApSession session;

	public ApClient(ApSession session) {
		this.session = session;
		setGame("Craftmine");
		setItemsHandlingFlags(ItemsHandling.SEND_ITEMS | ItemsHandling.SEND_OWN_ITEMS | ItemsHandling.SEND_STARTING_INVENTORY);
		getEventManager().registerListener(this);
	}

	@Override
	public void onError(Exception ex) {
		session.onError(ex.getMessage());
	}

	@Override
	public void onClose(String reason, int attemptingReconnect) {
		session.onClose(reason);
	}

	@ArchipelagoEventListener
	public void onConnectionResult(ConnectionResultEvent event) {
		if (event.getResult() == ConnectionResult.Success) {
			session.onConnected(event.getSeedName(), SlotData.fromJson(event.getSlotData(JsonObject.class)));
		} else {
			session.onRefused(event.getResult().name());
		}
	}

	@ArchipelagoEventListener
	public void onItem(ReceiveItemEvent event) {
		session.onItem((int) event.getIndex(), event.getItemID());
	}

	@ArchipelagoEventListener
	public void onPrint(PrintJSONEvent event) {
		session.onPrint(event.apPrint.parts);
	}

	@ArchipelagoEventListener
	public void onDeathLink(DeathLinkEvent event) {
		session.onDeathLink(event.source, event.cause);
	}
}
