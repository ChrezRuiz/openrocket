package info.openrocket.core.parachutesweep;

import java.util.List;

/**
 * Aggregate statistics computed from a list of parachute sweep results.
 */
public class ParachuteSweepSummary {

	private final ParachuteSweepResult bestResult;
	private final int passingCount;
	private final int totalCount;

	private ParachuteSweepSummary(ParachuteSweepResult bestResult, int passingCount,
			int totalCount) {
		this.bestResult = bestResult;
		this.passingCount = passingCount;
		this.totalCount = totalCount;
	}

	/**
	 * Compute summary statistics from sweep results.
	 *
	 * @param results            the list of sweep results
	 * @param targetDescentRate  the desired descent rate (m/s)
	 * @param tolerance          one-sided tolerance (m/s)
	 * @return computed summary
	 */
	public static ParachuteSweepSummary compute(List<ParachuteSweepResult> results,
			double targetDescentRate, double tolerance) {
		int passingCount = 0;
		ParachuteSweepResult bestPassing = null;
		double bestPassingDelta = Double.MAX_VALUE;
		ParachuteSweepResult closestOverall = null;
		double closestOverallDelta = Double.MAX_VALUE;

		for (ParachuteSweepResult r : results) {
			ParachuteSweepStatus status = classifyResult(r, targetDescentRate, tolerance);

			if (status == ParachuteSweepStatus.PASS) {
				passingCount++;

				double delta = Math.abs(r.getDescentRate() - targetDescentRate);
				if (delta < bestPassingDelta) {
					bestPassingDelta = delta;
					bestPassing = r;
				}
			}

			if (r.isSuccess()) {
				double delta = Math.abs(r.getDescentRate() - targetDescentRate);
				if (delta < closestOverallDelta) {
					closestOverallDelta = delta;
					closestOverall = r;
				}
			}
		}

		ParachuteSweepResult best = bestPassing != null ? bestPassing : closestOverall;

		return new ParachuteSweepSummary(best, passingCount, results.size());
	}

	/**
	 * Classify a single result's status based on descent rate criteria.
	 *
	 * @param result             the result to classify
	 * @param targetDescentRate  the desired descent rate (m/s)
	 * @param tolerance          one-sided tolerance (m/s)
	 * @return the status classification
	 */
	public static ParachuteSweepStatus classifyResult(ParachuteSweepResult result,
			double targetDescentRate, double tolerance) {
		if (!result.isSuccess()) {
			return ParachuteSweepStatus.ERROR;
		}

		double rate = result.getDescentRate();
		if (rate > targetDescentRate + tolerance) {
			return ParachuteSweepStatus.TOO_FAST;
		}
		if (rate < targetDescentRate - tolerance) {
			return ParachuteSweepStatus.TOO_SLOW;
		}

		return ParachuteSweepStatus.PASS;
	}

	public ParachuteSweepResult getBestResult() {
		return bestResult;
	}

	public int getPassingCount() {
		return passingCount;
	}

	public int getTotalCount() {
		return totalCount;
	}
}
