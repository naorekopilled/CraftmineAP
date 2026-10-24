package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.archipelagomw.Print.APPrintColor;
import io.github.archipelagomw.Print.APPrintPart;
import io.github.archipelagomw.Print.APPrintType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatColorsTest {
	private static APPrintPart part(APPrintType type, String text, int flags) {
		APPrintPart part = new APPrintPart();
		part.type = type;
		part.text = text;
		part.flags = flags;
		return part;
	}

	@Test
	void itemColorFollowsItsClassification() {
		assertEquals(0xAF99EF, ChatColors.color(part(APPrintType.itemID, "Combat", 0b001), "Miner"));
		assertEquals(0xAF99EF, ChatColors.color(part(APPrintType.itemID, "Combat", 0b011), "Miner"));
		assertEquals(0x6D8BE8, ChatColors.color(part(APPrintType.itemID, "Pickaxe", 0b010), "Miner"));
		assertEquals(0xFA8072, ChatColors.color(part(APPrintType.itemID, "Trap", 0b100), "Miner"));
		assertEquals(0x00EEEE, ChatColors.color(part(APPrintType.itemID, "Bag of XP", 0), "Miner"));
	}

	@Test
	void ownNameIsMagentaOthersYellow() {
		assertEquals(0xEE00EE, ChatColors.color(part(APPrintType.playerID, "Miner", 0), "Miner"));
		assertEquals(0xFAFAD2, ChatColors.color(part(APPrintType.playerID, "Other", 0), "Miner"));
	}

	@Test
	void locationsAreGreenAndPlainTextKeepsTheDefault() {
		assertEquals(0x00FF7F, ChatColors.color(part(APPrintType.locationID, "Ingredient: Foxes", 0), "Miner"));
		assertNull(ChatColors.color(part(APPrintType.text, " found their ", 0), "Miner"));
		assertNull(ChatColors.color(part(null, "chat", 0), "Miner"));
	}

	@Test
	void colorPartsUseTheirOwnColor() {
		APPrintPart red = part(APPrintType.color, "x", 0);
		red.color = APPrintColor.red;
		assertEquals(APPrintColor.red.color.getRGB() & 0xFFFFFF, ChatColors.color(red, "Miner"));
		red.color = APPrintColor.bold;
		assertNull(ChatColors.color(red, "Miner"));
	}

	@Test
	void segmentsKeepTheTextInOrder() {
		List<ChatColors.Segment> segments = ChatColors.segments(new APPrintPart[] {
			part(APPrintType.playerID, "Miner", 0),
			part(APPrintType.text, " found their ", 0),
			part(APPrintType.itemID, "Combat", 0b001),
		}, "Miner");
		assertEquals(List.of(
			new ChatColors.Segment("Miner", 0xEE00EE),
			new ChatColors.Segment(" found their ", null),
			new ChatColors.Segment("Combat", 0xAF99EF)), segments);
	}
}
