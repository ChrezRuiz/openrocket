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

	// --- GLOM/TWR filter tests ---

	@Test
	public void testGlomFilterExcludesFromPassing() {
		// All in apogee range [300, 350], but one exceeds maxGLOM of 0.6 kg
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),   // mass 0.5
				MotorSweepResult.success(buildMotor("B6"), 320, 30, 18, 6.0, 0.8, 55, 11));  // mass 0.8

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50, 0.6, null);

		assertEquals(1, summary.getPassingCount());
		assertEquals(2, summary.getTotalCount());
		assertEquals(310, summary.getBestResult().getApogee(), 0.001);
	}

	@Test
	public void testTwrFilterExcludesFromPassing() {
		// All in apogee range, but one has TWR below minTWR of 5.5
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),   // TWR 5.0
				MotorSweepResult.success(buildMotor("B6"), 320, 30, 18, 6.0, 0.6, 55, 11));  // TWR 6.0

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50, null, 5.5);

		assertEquals(1, summary.getPassingCount());
		assertEquals(320, summary.getBestResult().getApogee(), 0.001);
	}

	@Test
	public void testBothFiltersSimultaneously() {
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),   // pass both
				MotorSweepResult.success(buildMotor("B6"), 320, 30, 18, 4.0, 0.8, 55, 11),   // fail both
				MotorSweepResult.success(buildMotor("C6"), 305, 25, 16, 6.0, 0.9, 52, 10));  // fail GLOM

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50, 0.6, 4.5);

		assertEquals(1, summary.getPassingCount());
		assertEquals(310, summary.getBestResult().getApogee(), 0.001);
	}

	@Test
	public void testClassifyResultStatuses() {
		ThrustCurveMotor m = buildMotor("A8");

		// PASS
		MotorSweepResult passing = MotorSweepResult.success(m, 310, 20, 15, 5.0, 0.5, 50, 10);
		assertEquals(SweepStatus.PASS,
				MotorSweepSummary.classifyResult(passing, 300, 50, 0.6, 4.0));

		// OUT_OF_RANGE
		MotorSweepResult outOfRange = MotorSweepResult.success(m, 200, 20, 15, 5.0, 0.5, 50, 10);
		assertEquals(SweepStatus.OUT_OF_RANGE,
				MotorSweepSummary.classifyResult(outOfRange, 300, 50, null, null));

		// FILTERED_GLOM
		MotorSweepResult glomFail = MotorSweepResult.success(m, 310, 20, 15, 5.0, 0.8, 50, 10);
		assertEquals(SweepStatus.FILTERED_GLOM,
				MotorSweepSummary.classifyResult(glomFail, 300, 50, 0.6, null));

		// FILTERED_TWR
		MotorSweepResult twrFail = MotorSweepResult.success(m, 310, 20, 15, 3.0, 0.5, 50, 10);
		assertEquals(SweepStatus.FILTERED_TWR,
				MotorSweepSummary.classifyResult(twrFail, 300, 50, null, 4.0));

		// FILTERED_BOTH
		MotorSweepResult bothFail = MotorSweepResult.success(m, 310, 20, 15, 3.0, 0.8, 50, 10);
		assertEquals(SweepStatus.FILTERED_BOTH,
				MotorSweepSummary.classifyResult(bothFail, 300, 50, 0.6, 4.0));

		// ERROR
		MotorSweepResult err = MotorSweepResult.error(m, "fail");
		assertEquals(SweepStatus.ERROR,
				MotorSweepSummary.classifyResult(err, 300, 50, null, null));

		// INFEASIBLE
		MotorSweepResult inf = MotorSweepResult.infeasible(m, "Exceeds max GLOM");
		assertEquals(SweepStatus.INFEASIBLE,
				MotorSweepSummary.classifyResult(inf, 300, 50, null, null));
	}

	@Test
	public void testInfeasibleExcludedFromBest() {
		List<MotorSweepResult> results = List.of(
				MotorSweepResult.success(buildMotor("A8"), 310, 20, 15, 5.0, 0.5, 50, 10),
				MotorSweepResult.infeasible(buildMotor("B6"), "Exceeds max GLOM"));

		MotorSweepSummary summary = MotorSweepSummary.compute(results, 300, 50);

		assertEquals(1, summary.getPassingCount());
		assertEquals(2, summary.getTotalCount());
		assertEquals(310, summary.getBestResult().getApogee(), 0.001);
	}
}
