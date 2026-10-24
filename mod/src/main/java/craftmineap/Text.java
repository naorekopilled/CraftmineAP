package craftmineap;

import net.minecraft.network.chat.Component;

/** Every chat line the mod prints. */
public final class Text {
	private Text() {
	}

	// placeholder
	public static Component connecting(String address) {
		return Component.literal("Connecting to " + address + "...");
	}

	// placeholder
	public static Component connected(String slot) {
		return Component.literal("Connected to Archipelago as " + slot + ".");
	}

	// placeholder
	public static Component typeStart() {
		return Component.literal("Type /start to begin the randomizer in this world.");
	}

	// placeholder
	public static Component started() {
		return Component.literal("The randomizer is on in this world.");
	}

	// placeholder
	public static Component notConnectedYet() {
		return Component.literal("Connect first: /connect <address> <slot> [password]");
	}

	// placeholder
	public static Component noSavedConnection() {
		return Component.literal("No saved connection for this world: /connect <address> <slot> [password]");
	}

	// placeholder
	public static Component refused(String reason) {
		return Component.literal("Archipelago refused the connection: " + reason);
	}

	// placeholder
	public static Component disconnected(String reason) {
		return Component.literal("Disconnected from Archipelago: " + reason);
	}

	// placeholder
	public static Component error(String message) {
		return Component.literal("Archipelago error: " + message);
	}

	// placeholder
	public static Component wrongRoom() {
		return Component.literal("This world belongs to another Archipelago room or slot. Make a new world for this one.");
	}

	// placeholder
	public static Component synced() {
		return Component.literal("Asked Archipelago to resend everything.");
	}

	// placeholder
	public static Component deathLink(String source, String cause) {
		return Component.literal(cause == null || cause.isEmpty() ? source + " died." : cause);
	}
}
