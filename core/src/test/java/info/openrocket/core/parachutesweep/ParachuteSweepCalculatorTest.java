package info.openrocket.core.parachutesweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ParachuteSweepCalculatorTest {

	private final ParachuteSweepCalculator calculator = new ParachuteSweepCalculator();

	@Test
	public void testHandCalculatedDescentRate() {
		// 1 kg mass, flat circular (CD=0.75), 0.5m diameter, sea-level air density
		// area = pi * (0.25)^2 = 0.19635 m^2
		// descentRate = sqrt(2 * 1 * 9.80665 / (0.75 * 0.19635 * 1.225))
		//             = sqrt(19.6133 / 0.18038) = sqrt(108.73) = 10.427 m/s
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,     // target/tolerance (not used in calculator)
				0.5, 0.5, 0.1, // min=max=0.5, step irrelevant
				Set.of(ParachuteType.FLAT_CIRCULAR),
				1.0,          // mass
				1.225,        // air density
				null);        // no custom cd

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(1, results.size());
		ParachuteSweepResult r = results.get(0);
		assertTrue(r.isSuccess());
		assertEquals(ParachuteType.FLAT_CIRCULAR, r.getType());
		assertEquals(0.5, r.getDiameter(), 0.0001);
		assertEquals(0.75, r.getCd(), 0.0001);

		double expectedArea = Math.PI * 0.25 * 0.25;
		assertEquals(expectedArea, r.getArea(), 0.0001);

		double expectedRate = Math.sqrt(2.0 * 1.0 * 9.80665 / (0.75 * expectedArea * 1.225));
		assertEquals(expectedRate, r.getDescentRate(), 0.001);
		assertEquals(10.427, r.getDescentRate(), 0.01);
	}

	@Test
	public void testMultipleTypesSweep() {
		// 2 types, diameters from 0.3 to 0.5 step 0.1 -> 3 diameters each -> 6 results
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,
				0.3, 0.5, 0.1,
				Set.of(ParachuteType.FLAT_CIRCULAR, ParachuteType.HEMISPHERICAL),
				1.0,
				1.225,
				null);

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(6, results.size());
		for (ParachuteSweepResult r : results) {
			assertTrue(r.isSuccess());
		}
	}

	@Test
	public void testCustomTypeWithNullCd() {
		// CUSTOM type with null customCd should produce error results
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,
				0.3, 0.5, 0.1,
				Set.of(ParachuteType.CUSTOM),
				1.0,
				1.225,
				null);

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(3, results.size());
		for (ParachuteSweepResult r : results) {
			assertFalse(r.isSuccess());
			assertTrue(r.getErrorMessage().contains("drag coefficient"));
		}
	}

	@Test
	public void testCustomTypeWithValidCd() {
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,
				0.5, 0.5, 0.1,
				Set.of(ParachuteType.CUSTOM),
				1.0,
				1.225,
				0.80);

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(1, results.size());
		ParachuteSweepResult r = results.get(0);
		assertTrue(r.isSuccess());
		assertEquals(0.80, r.getCd(), 0.0001);
		assertEquals(ParachuteType.CUSTOM, r.getType());
	}

	@Test
	public void testLargerDiameterGivesSlowerDescent() {
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,
				0.3, 0.6, 0.3,
				Set.of(ParachuteType.FLAT_CIRCULAR),
				1.0,
				1.225,
				null);

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(2, results.size());
		// Larger diameter -> larger area -> slower descent
		assertTrue(results.get(0).getDescentRate() > results.get(1).getDescentRate(),
				"Larger diameter should produce slower descent rate");
	}

	@Test
	public void testResultFieldsPopulated() {
		ParachuteSweepConfig config = new ParachuteSweepConfig(
				5.0, 1.0,
				0.5, 0.5, 0.1,
				Set.of(ParachuteType.FLAT_CIRCULAR),
				2.5,
				1.225,
				null);

		List<ParachuteSweepResult> results = calculator.calculate(config);

		assertEquals(1, results.size());
		ParachuteSweepResult r = results.get(0);
		assertEquals(2.5, r.getMass(), 0.0001);
		assertEquals(1.225, r.getAirDensity(), 0.0001);
	}
}
