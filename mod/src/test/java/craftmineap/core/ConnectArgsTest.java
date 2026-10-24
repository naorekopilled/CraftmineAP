package craftmineap.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConnectArgsTest {
	@Test
	void addressKeepsItsPort() {
		assertEquals(new ConnectArgs("archipelago.gg:61083", "CraftMine", null), ConnectArgs.parse("archipelago.gg:61083 CraftMine"));
	}

	@Test
	void quotedSlotAndPasswordWithSpaces() {
		assertEquals(new ConnectArgs("localhost:38281", "My Name", "secret pass"),
			ConnectArgs.parse("localhost:38281 \"My Name\" secret pass"));
	}

	@Test
	void unquotedSlotMayHaveAnyCharacters() {
		assertEquals(new ConnectArgs("localhost:38281", "Sörensen's", null), ConnectArgs.parse("  localhost:38281   Sörensen's  "));
	}

	@Test
	void missingSlotIsNull() {
		assertNull(ConnectArgs.parse("archipelago.gg:61083"));
		assertNull(ConnectArgs.parse("archipelago.gg:61083   "));
		assertNull(ConnectArgs.parse("localhost:38281 \"unclosed"));
	}
}
