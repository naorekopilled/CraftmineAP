package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DeathLinkGateTest {
	@Test
	void sendsOnlyForOwnDeathsInAMine() {
		DeathLinkGate gate = new DeathLinkGate();
		assertTrue(gate.shouldSend(true, true));
		assertFalse(gate.shouldSend(false, true));
		assertFalse(gate.shouldSend(true, false));
	}

	@Test
	void aReceivedDeathIsNotSentBack() {
		DeathLinkGate gate = new DeathLinkGate();
		boolean[] sent = {true};
		gate.receiving(() -> sent[0] = gate.shouldSend(true, true));
		assertFalse(sent[0]);
		assertTrue(gate.shouldSend(true, true));
	}
}
