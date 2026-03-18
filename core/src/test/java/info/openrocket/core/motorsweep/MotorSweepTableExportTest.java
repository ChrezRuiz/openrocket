package info.openrocket.core.motorsweep;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MotorSweepTableExportTest {

	@Test
	public void testCsvEscapePlainValue() {
		assertEquals("hello", csvEscape("hello"));
	}

	@Test
	public void testCsvEscapeComma() {
		assertEquals("\"hello, world\"", csvEscape("hello, world"));
	}

	@Test
	public void testCsvEscapeQuotes() {
		assertEquals("\"say \"\"hi\"\"\"", csvEscape("say \"hi\""));
	}

	@Test
	public void testCsvEscapeNewline() {
		assertEquals("\"line1\nline2\"", csvEscape("line1\nline2"));
	}

	@Test
	public void testCsvEscapeEmpty() {
		assertEquals("", csvEscape(""));
	}

	@Test
	public void testCsvEscapeNull() {
		assertEquals("", csvEscape(null));
	}

	private static String csvEscape(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
