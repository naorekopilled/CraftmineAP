package craftmineap.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * /connect's arguments, read by hand: Brigadier's own string argument stops at the ':' of host:port.
 * The slot may be quoted; the password is the rest of the line.
 */
public record ConnectArgs(String address, String slot, String password) {
	/** null when the slot is missing or its quote is never closed. */
	public static ConnectArgs parse(String input) {
		StringReader reader = new StringReader(input);
		reader.skipWhitespace();
		String address = readToken(reader);
		reader.skipWhitespace();
		if (!reader.canRead()) return null;
		String slot;
		if (StringReader.isQuotedStringStart(reader.peek())) {
			try {
				slot = reader.readQuotedString();
			} catch (CommandSyntaxException e) {
				return null;
			}
		} else {
			slot = readToken(reader);
		}
		String password = reader.getRemaining().strip();
		return new ConnectArgs(address, slot, password.isEmpty() ? null : password);
	}

	private static String readToken(StringReader reader) {
		int start = reader.getCursor();
		while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
		return reader.getString().substring(start, reader.getCursor());
	}
}
