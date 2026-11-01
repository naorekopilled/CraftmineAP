package craftmineap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Every chat line the mod prints. */
public final class Text {
	private Text() {
	}

	public static Component connecting(String address) {
		return Component.literal("Connecting to " + address + "...");
	}

	public static Component connected(String slot) {
		return Component.literal("Connected to Archipelago as " + slot + ".");
	}

	public static Component typeStart() {
		return Component.literal("Type /start to turn the randomizer on for this world.");
	}

	public static Component started() {
		return Component.literal("Randomizer started. Good luck!");
	}

	public static Component notConnectedYet() {
		return Component.literal("You are not connected. Use /connect <address> <slot> [password]");
	}

	public static Component noSavedConnection() {
		return Component.literal("This world has no saved connection. Use /connect <address> <slot> [password]");
	}

	public static Component connectUsage() {
		return Component.literal("Usage: /connect <address> <slot> [password]");
	}

	public static Component refused(String reason) {
		return Component.literal("Connection refused: " + reason);
	}

	public static Component disconnected(String reason) {
		return Component.literal("Disconnected from Archipelago: " + reason);
	}

	public static Component error(String message) {
		return Component.literal("Archipelago error: " + message);
	}

	public static Component wrongRoom() {
		return Component.literal("This world is tied to a different room or slot. Make a new world for this seed.");
	}

	public static Component synced() {
		return Component.literal("Asked the server to send all your items again.");
	}

	public static Component deathLink(String source, String cause) {
		return Component.literal(cause == null || cause.isEmpty() ? source + " died." : cause);
	}

	// the tracker tab in the advancements screen (L)
	public static Component trackerTitle() {
		return Component.literal("Tracker");
	}

	public static Component trackerProgress(int done, int total) {
		return Component.literal(done + "/" + total + " checks");
	}

	// the tracker's group names (skill-tree and advancement groups use the game's tab names);
	// key is one of ingredient, mine, crafter, door
	public static Component trackerBranch(String key) {
		return Component.literal(switch (key) {
			case "ingredient" -> "Mine Ingredients";
			case "mine" -> "Special Mines";
			case "crafter" -> "Mine Crafter Levels";
			default -> "Hub Doors";
		});
	}

	public static Component trackerDone() {
		return Component.literal("Done").withStyle(ChatFormatting.GOLD);
	}

	public static Component trackerInLogic() {
		return Component.literal("In logic").withStyle(ChatFormatting.GREEN);
	}

	// under a skill-tree node's description
	public static Component tabClosed(String tabItem) {
		return Component.literal("Needs " + tabItem).withStyle(ChatFormatting.RED);
	}

	public static Component trackerOutOfLogic() {
		return Component.literal("Out of logic").withStyle(ChatFormatting.RED);
	}

	// needs is the list of what the check still needs, already joined
	public static Component trackerNeeds(String needs) {
		return Component.literal("Needs: " + needs);
	}

	// one thing a check needs
	public static String trackerNeed(String kind, String what, int amount) {
		return switch (kind) {
			case "craft" -> "Crafting";
			case "special" -> "any special mine";
			case "tab" -> "every " + what + " upgrade";
			default -> amount > 1 ? what + " x" + amount : what;
		};
	}
}
