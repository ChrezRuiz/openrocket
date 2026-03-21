package info.openrocket.core.parachutesweep;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ParachuteSweepConfigTest {

	@Test
	public void testValidConfig() {
		assertDoesNotThrow(() -> new ParachuteSweepConfig(
				5.0, 1.0,
				0.3, 0.5, 0.1,
				Set.of(ParachuteType.FLAT_CIRCULAR),
				1.0, 1.225, null));
	}

	@Test
	public void testMinDiameterEqualToMaxDiameter() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.5, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("minDiameter"));
	}

	@Test
	public void testMinDiameterGreaterThanMaxDiameter() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.6, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("minDiameter"));
	}

	@Test
	public void testDiameterStepZero() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.0,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("diameterStep"));
	}

	@Test
	public void testDiameterStepNegative() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, -0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("diameterStep"));
	}

	@Test
	public void testMassZero() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						0.0, 1.225, null));
		assertTrue(ex.getMessage().contains("mass"));
	}

	@Test
	public void testMassNegative() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						-1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("mass"));
	}

	@Test
	public void testAirDensityZero() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, 0.0, null));
		assertTrue(ex.getMessage().contains("airDensity"));
	}

	@Test
	public void testAirDensityNegative() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						Set.of(ParachuteType.FLAT_CIRCULAR),
						1.0, -1.225, null));
		assertTrue(ex.getMessage().contains("airDensity"));
	}

	@Test
	public void testSelectedTypesNull() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						null,
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("selectedTypes"));
	}

	@Test
	public void testSelectedTypesEmpty() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> new ParachuteSweepConfig(
						5.0, 1.0,
						0.3, 0.5, 0.1,
						Collections.emptySet(),
						1.0, 1.225, null));
		assertTrue(ex.getMessage().contains("selectedTypes"));
	}
}
