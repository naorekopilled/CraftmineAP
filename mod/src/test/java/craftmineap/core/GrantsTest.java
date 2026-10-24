package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class GrantsTest {
	@Test
	void countsEachKind() {
		ApState state = ApStateTest.state();
		state.acceptItem(1, 200);
		state.acceptItem(2, 201);
		state.acceptItem(3, 201);
		state.acceptItem(4, 202);
		state.acceptItem(5, 203);
		state.acceptItem(6, 203);
		Grants grants = Grants.from(state);
		assertEquals(Set.of("foxes"), grants.ingredients());
		assertEquals(Set.of("starter_sword", "starter_sword_iron"), grants.unlocks());
		assertEquals(1, grants.keys());
		assertEquals(2, grants.xpBundles());
	}

	@Test
	void progressiveCountIsClampedToTheChain() {
		ApState state = ApStateTest.state();
		for (int i = 1; i <= 5; i++) {
			state.acceptItem(i, 201);
		}
		assertEquals(4, Grants.from(state).unlocks().size());
	}

	@Test
	void unknownItemIdsAreIgnored() {
		ApState state = ApStateTest.state();
		state.acceptItem(1, 999);
		Grants grants = Grants.from(state);
		assertTrue(grants.ingredients().isEmpty());
		assertTrue(grants.unlocks().isEmpty());
	}
}
