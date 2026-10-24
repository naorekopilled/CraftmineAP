package craftmineap.core;

import io.github.archipelagomw.Print.APPrintColor;
import io.github.archipelagomw.Print.APPrintPart;
import java.util.ArrayList;
import java.util.List;

/** Colors the parts of an Archipelago server message the way Archipelago's own text client does. */
public final class ChatColors {
	/** rgb null = the chat's default color. */
	public record Segment(String text, Integer rgb) {}

	private ChatColors() {}

	public static List<Segment> segments(APPrintPart[] parts, String myName) {
		List<Segment> segments = new ArrayList<>();
		for (APPrintPart part : parts) {
			segments.add(new Segment(part.text == null ? "" : part.text, color(part, myName)));
		}
		return segments;
	}

	static Integer color(APPrintPart part, String myName) {
		if (part.type == null) return null;
		return switch (part.type) {
			case playerID, playerName -> part.text != null && part.text.equals(myName) ? 0xEE00EE : 0xFAFAD2;
			case itemID, itemName -> {
				if ((part.flags & 0b001) != 0) yield 0xAF99EF;
				if ((part.flags & 0b010) != 0) yield 0x6D8BE8;
				if ((part.flags & 0b100) != 0) yield 0xFA8072;
				yield 0x00EEEE;
			}
			case locationID, locationName -> 0x00FF7F;
			case entranceName -> 0x6495ED;
			// the library gives none, bold and underline white, which is not a color choice
			case color -> switch (part.color == null ? APPrintColor.none : part.color) {
				case none, bold, underline -> null;
				default -> part.color.color.getRGB() & 0xFFFFFF;
			};
			default -> null;
		};
	}
}
