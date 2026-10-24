package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SlotDataTest {
	@Test
	void itemIngredientsComeFromTheItemMap() {
		SlotData slotData = ApStateTest.state().slotData;
		assertTrue(slotData.isItemIngredient("foxes"));
		assertFalse(slotData.isItemIngredient("plains"));
		assertFalse(slotData.isItemIngredient("starter_sword"));
	}
}
