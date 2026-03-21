package info.openrocket.core.parachutesweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ParachuteTypeTest {

	@Test
	public void testPresetTypesHaveValidCd() {
		for (ParachuteType type : ParachuteType.values()) {
			if (type == ParachuteType.CUSTOM) {
				continue;
			}
			assertFalse(Double.isNaN(type.getDefaultCd()),
					type.name() + " should have a non-NaN CD");
			assertTrue(type.getDefaultCd() > 0,
					type.name() + " should have a positive CD");
		}
	}

	@Test
	public void testCustomHasNanCd() {
		assertTrue(Double.isNaN(ParachuteType.CUSTOM.getDefaultCd()));
		assertTrue(ParachuteType.CUSTOM.isCustom());
	}

	@Test
	public void testPresetTypesAreNotCustom() {
		for (ParachuteType type : ParachuteType.values()) {
			if (type == ParachuteType.CUSTOM) {
				continue;
			}
			assertFalse(type.isCustom(), type.name() + " should not be custom");
		}
	}

	@Test
	public void testDisplayNamesAreNonEmpty() {
		for (ParachuteType type : ParachuteType.values()) {
			assertNotNull(type.getDisplayName(), type.name() + " display name should not be null");
			assertFalse(type.getDisplayName().isEmpty(),
					type.name() + " display name should not be empty");
		}
	}

	@Test
	public void testSpecificCdValues() {
		assertEquals(0.75, ParachuteType.FLAT_CIRCULAR.getDefaultCd(), 0.001);
		assertEquals(0.62, ParachuteType.HEMISPHERICAL.getDefaultCd(), 0.001);
		assertEquals(0.75, ParachuteType.CONICAL.getDefaultCd(), 0.001);
		assertEquals(0.70, ParachuteType.CRUCIFORM.getDefaultCd(), 0.001);
		assertEquals(1.00, ParachuteType.TOROIDAL.getDefaultCd(), 0.001);
		assertEquals(0.47, ParachuteType.RIBBON.getDefaultCd(), 0.001);
		assertEquals(0.90, ParachuteType.ANNULAR.getDefaultCd(), 0.001);
	}
}
