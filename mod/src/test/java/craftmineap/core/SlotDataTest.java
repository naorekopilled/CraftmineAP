package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SlotDataTest {
	@Test
	void itemIngredientsComeFromTheItemMap() {
		SlotData slotData = ApStateTest.state().slotData;
		assertTrue(slotData.isItemIngredient("foxes"));
		assertFalse(slotData.isItemIngredient("plains"));
		assertFalse(slotData.isItemIngredient("starter_sword"));
	}

	@Test
	void upgradeTabsSurviveSavingAndOldSavesHaveThemOff() {
		JsonObject json = JsonParser.parseString(ApStateTest.SLOT_DATA).getAsJsonObject();
		assertFalse(SlotData.fromJson(json).upgradeTabs());
		json.addProperty("upgrade_tabs", true);
		assertTrue(SlotData.fromJson(SlotData.fromJson(json).toJson()).upgradeTabs());
	}
}
