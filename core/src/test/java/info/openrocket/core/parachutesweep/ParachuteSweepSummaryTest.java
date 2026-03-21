package info.openrocket.core.parachutesweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ParachuteSweepSummaryTest {

	private static ParachuteSweepResult makeSuccess(double descentRate) {
		double diameter = 0.5;
		double cd = 0.75;
		double area = Math.PI * 0.25 * 0.25;
		return ParachuteSweepResult.success(ParachuteType.FLAT_CIRCULAR,
				diameter, cd, area, descentRate, 1.0, 1.225);
	}

	@Test
	public void testAllPassing() {
		// Target 5.0 m/s, tolerance 1.0 -> passing range [4.0, 6.0]
		List<ParachuteSweepResult> results = List.of(
				makeSuccess(5.0),
				makeSuccess(5.5),
				makeSuccess(4.5));

		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(results, 5.0, 1.0);

		assertEquals(3, summary.getPassingCount());
		assertEquals(3, summary.getTotalCount());
		assertNotNull(summary.getBestResult());
		// Best is closest to target (5.0)
		assertEquals(5.0, summary.getBestResult().getDescentRate(), 0.001);
	}

	@Test
	public void testNonePassing() {
		// Target 5.0 m/s, tolerance 1.0 -> passing range [4.0, 6.0]
		List<ParachuteSweepResult> results = List.of(
				makeSuccess(2.0),   // too slow
				makeSuccess(8.0));  // too fast

		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(results, 5.0, 1.0);

		assertEquals(0, summary.getPassingCount());
		assertEquals(2, summary.getTotalCount());
		// Fallback to closest overall: 2.0 is 3.0 away, 8.0 is 3.0 away
		assertNotNull(summary.getBestResult());
	}

	@Test
	public void testNonePassingFallbackToClosest() {
		// Target 5.0 m/s, tolerance 1.0 -> passing range [4.0, 6.0]
		List<ParachuteSweepResult> results = List.of(
				makeSuccess(2.0),    // 3.0 away from target
				makeSuccess(6.5));   // 1.5 away from target

		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(results, 5.0, 1.0);

		assertEquals(0, summary.getPassingCount());
		// Closest overall is 6.5 (delta 1.5 vs 3.0)
		assertEquals(6.5, summary.getBestResult().getDescentRate(), 0.001);
	}

	@Test
	public void testMixed() {
		// Target 5.0, tolerance 1.0 -> passing range [4.0, 6.0]
		List<ParachuteSweepResult> results = List.of(
				makeSuccess(5.2),    // passing
				makeSuccess(2.0),    // too slow
				makeSuccess(4.8));   // passing

		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(results, 5.0, 1.0);

		assertEquals(2, summary.getPassingCount());
		assertEquals(3, summary.getTotalCount());
		// Best from passing: 4.8 is 0.2 away, 5.2 is 0.2 away; first found wins
		assertNotNull(summary.getBestResult());
	}

	@Test
	public void testEmptyResults() {
		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(
				Collections.emptyList(), 5.0, 1.0);

		assertEquals(0, summary.getPassingCount());
		assertEquals(0, summary.getTotalCount());
		assertNull(summary.getBestResult());
	}

	@Test
	public void testClassifyPass() {
		ParachuteSweepResult r = makeSuccess(5.0);
		assertEquals(ParachuteSweepStatus.PASS,
				ParachuteSweepSummary.classifyResult(r, 5.0, 1.0));
	}

	@Test
	public void testClassifyTooFast() {
		ParachuteSweepResult r = makeSuccess(7.0);
		assertEquals(ParachuteSweepStatus.TOO_FAST,
				ParachuteSweepSummary.classifyResult(r, 5.0, 1.0));
	}

	@Test
	public void testClassifyTooSlow() {
		ParachuteSweepResult r = makeSuccess(3.0);
		assertEquals(ParachuteSweepStatus.TOO_SLOW,
				ParachuteSweepSummary.classifyResult(r, 5.0, 1.0));
	}

	@Test
	public void testClassifyError() {
		ParachuteSweepResult r = ParachuteSweepResult.error(
				ParachuteType.FLAT_CIRCULAR, 0.5, "calculation failed");
		assertEquals(ParachuteSweepStatus.ERROR,
				ParachuteSweepSummary.classifyResult(r, 5.0, 1.0));
	}

	@Test
	public void testClassifyBoundaryValues() {
		// Exactly at upper boundary -> PASS
		ParachuteSweepResult atUpper = makeSuccess(6.0);
		assertEquals(ParachuteSweepStatus.PASS,
				ParachuteSweepSummary.classifyResult(atUpper, 5.0, 1.0));

		// Exactly at lower boundary -> PASS
		ParachuteSweepResult atLower = makeSuccess(4.0);
		assertEquals(ParachuteSweepStatus.PASS,
				ParachuteSweepSummary.classifyResult(atLower, 5.0, 1.0));
	}

	@Test
	public void testErrorResultsExcludedFromBest() {
		List<ParachuteSweepResult> results = List.of(
				makeSuccess(5.2),
				ParachuteSweepResult.error(ParachuteType.CUSTOM, 0.5, "fail"));

		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(results, 5.0, 1.0);

		assertEquals(1, summary.getPassingCount());
		assertEquals(2, summary.getTotalCount());
		assertNotNull(summary.getBestResult());
		assertEquals(5.2, summary.getBestResult().getDescentRate(), 0.001);
	}
}
