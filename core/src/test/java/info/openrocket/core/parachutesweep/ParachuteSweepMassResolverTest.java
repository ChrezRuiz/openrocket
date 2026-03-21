package info.openrocket.core.parachutesweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import info.openrocket.core.parachutesweep.ParachuteSweepMassResolver.MassMode;

public class ParachuteSweepMassResolverTest {

	@Test
	public void testManualModeReturnsValue() {
		double mass = ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, 2.5);
		assertEquals(2.5, mass, 1e-9);
	}

	@Test
	public void testManualModeWithLargeMass() {
		double mass = ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, 100.0);
		assertEquals(100.0, mass, 1e-9);
	}

	@Test
	public void testManualModeNullMassThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, null));
	}

	@Test
	public void testManualModeZeroMassThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, 0.0));
	}

	@Test
	public void testManualModeNegativeMassThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, -1.0));
	}

	@Test
	public void testBurnoutModeNullConfigThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> ParachuteSweepMassResolver.resolveMass(MassMode.BURNOUT, null, null));
	}

	@Test
	public void testManualModeIgnoresConfig() {
		// Passing null config should not affect MANUAL mode
		double mass = ParachuteSweepMassResolver.resolveMass(MassMode.MANUAL, null, 3.14);
		assertEquals(3.14, mass, 1e-9);
	}
}
