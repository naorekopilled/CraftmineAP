package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActiveUnlocksTest {
	@Test
	void laterChainStepsDisableEarlierOnes() {
		Map<String, List<String>> disables = Map.of("starter_sword_iron", List.of("starter_sword"));
		Set<String> active = ActiveUnlocks.compute(Set.of("starter_sword", "starter_sword_iron", "jump_king"),
			u -> disables.getOrDefault(u, List.of()), u -> false);
		assertEquals(Set.of("starter_sword_iron", "jump_king"), active);
	}

	@Test
	void exclusiveOnesNeedToBeTheActiveChoice() {
		Set<String> active = ActiveUnlocks.compute(Set.of("pet_cat", "pet_cow"), u -> List.of(), u -> u.equals("pet_cow"));
		assertEquals(Set.of("pet_cat"), active);
	}
}
