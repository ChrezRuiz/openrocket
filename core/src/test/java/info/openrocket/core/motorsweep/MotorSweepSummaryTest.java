package info.openrocket.core.motorsweep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MotorSweepSummaryTest {

	private static ThrustCurveMotor buildMotor(String designation) {
		return new ThrustCurveMotor.Builder()
				.setManufacturer(Manufacturer.getManufacturer("TestMfg"))
				.setDesignation(designation)
				.setDescription("Test motor")
				.setMotorType(Motor.Type.SINGLE)
				.setStandardDelays(new double[] { 5 })
				.setDiameter(0.024)
				.setLength(0.07)
				.setTimePoints(new double[] { 0, 1, 2 })
				.setThrustPoints(new double[] { 0, 10, 0 })
				.setCGPoints(new CoordinateIF[] { Coordinate.NUL, Coordinate.NUL, Coordinate.NUL })
				.setDigest("digest-" + designation)
				.build();
	}

	@Test
	public void testAllPassing() {
		// Target 300m, tolerance 50m -> passing range [300, 350]
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),
				MotorSweepResult.success(buildMotor("B6"), 300, 25, 18, 6.0, 0.6, 55, 11),
				MotorSweepResult.success(buildMotor("C6"), 320, 30, 20, 7.0, 0.7, 60, 12));

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		assertEquals(3, summary.getPassingCount());
		assertEquals(3, summary.getTotalCount());
		// Best is closest to target (300) -> B6 at apogee 300
		assertNotNull(summary.getBestResult());
		assertEquals(300, summary.getBestResult().getApogee(), 0.001);
	}

	@Test
	public void testNonePassing() {
		// Target 300m, tolerance 50m -> passing range [300, 350]
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 200, 20, 15, 5.0, 0.5, 50, 10),
				MotorSweepResult.success(buildMotor("B6"), 400, 25, 18, 6.0, 0.6, 55, 11));

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		assertEquals(0, summary.getPassingCount());
		assertEquals(2, summary.getTotalCount());
		// Best falls back to closest to range: 200 is 100 from [300,350], 400 is 50 from [300,350]
		assertNotNull(summary.getBestResult());
		assertEquals(400, summary.getBestResult().getApogee(), 0.001);
		assertEquals(0, summary.getAvgImpulse(), 0.001);
		assertEquals(0, summary.getAvgTwr(), 0.001);
	}

	@Test
	public void testMixed() {
		// Target 300m, tolerance 50m -> passing range [300, 350]
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),   // passing
				MotorSweepResult.success(buildMotor("B6"), 200, 25, 18, 6.0, 0.6, 55, 11),   // below
				MotorSweepResult.success(buildMotor("C6"), 305, 30, 20, 7.0, 0.7, 60, 12));  // passing

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		assertEquals(2, summary.getPassingCount());
		assertEquals(3, summary.getTotalCount());
		// Best from passing: closest to 300 is C6 at 305
		assertEquals(305, summary.getBestResult().getApogee(), 0.001);
	}

	@Test
	public void testWithErrors() {
		List<MotorSweepResult> results = new ArrayList<>();
		results.add(MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10));
		results.add(MotorSweepResult.error(buildMotor("B6"), "Simulation failed"));
		results.add(MotorSweepResult.success(buildMotor("C6"), 320, 30, 20, 7.0, 0.7, 60, 12));

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		// Error results excluded from passing; totalCount still includes them
		assertEquals(2, summary.getPassingCount());
		assertEquals(3, summary.getTotalCount());
	}

	@Test
	public void testEmptyResults() {
		MotorSweepSummary summary = MotorSweepSummary.compute(Collections.emptyList(), 300, 50);

		assertEquals(0, summary.getPassingCount());
		assertEquals(0, summary.getTotalCount());
		assertNull(summary.getBestResult());
		assertEquals(0, summary.getAvgImpulse(), 0.001);
		assertEquals(0, summary.getAvgTwr(), 0.001);
	}

	@Test
	public void testAvgImpulseAndTwr() {
		// Target 300m, tolerance 50m -> passing range [300, 350]
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 4.0, 0.5, 50, 10),
				MotorSweepResult.success(buildMotor("B6"), 320, 30, 18, 6.0, 0.6, 55, 11),
				MotorSweepResult.success(buildMotor("C6"), 200, 40, 20, 8.0, 0.7, 60, 12));  // not passing

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		assertEquals(2, summary.getPassingCount());
		// avgImpulse of passing: (20 + 30) / 2 = 25
		assertEquals(25.0, summary.getAvgImpulse(), 0.001);
		// avgTwr of passing: (4.0 + 6.0) / 2 = 5.0
		assertEquals(5.0, summary.getAvgTwr(), 0.001);
	}
}
