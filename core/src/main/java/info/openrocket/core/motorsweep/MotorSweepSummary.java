package info.openrocket.core.motorsweep;

import java.util.List;

/**
 * Aggregate statistics computed from a list of motor sweep results.
 */
public class MotorSweepSummary {

	private final MotorSweepResult bestResult;
	private final int passingCount;
	private final int totalCount;
	private final double avgImpulse;
	private final double avgTwr;

	private MotorSweepSummary(MotorSweepResult bestResult, int passingCount, int totalCount,
			double avgImpulse, double avgTwr) {
		this.bestResult = bestResult;
		this.passingCount = passingCount;
		this.totalCount = totalCount;
		this.avgImpulse = avgImpulse;
		this.avgTwr = avgTwr;
	}

	/**
	 * Compute summary statistics from sweep results.
	 *
	 * @param results       the list of sweep results
	 * @param targetApogee  the desired apogee altitude (m)
	 * @param tolerance     one-sided tolerance: passing range is [target, target + tolerance]
	 * @return computed summary
	 */
	public static MotorSweepSummary compute(List<MotorSweepResult> results, double targetApogee, double tolerance) {
		int passingCount = 0;
		double sumImpulse = 0;
		double sumTwr = 0;
		MotorSweepResult bestPassing = null;
		double bestPassingDelta = Double.MAX_VALUE;
		MotorSweepResult closestOverall = null;
		double closestOverallDelta = Double.MAX_VALUE;

		for (MotorSweepResult r : results) {
			if (!r.isSuccess()) {
				continue;
			}

			double apogee = r.getApogee();
			boolean passing = apogee >= targetApogee && apogee <= targetApogee + tolerance;

			if (passing) {
				passingCount++;
				sumImpulse += r.getTotalImpulse();
				sumTwr += r.getTwr();

				double delta = Math.abs(apogee - targetApogee);
				if (delta < bestPassingDelta) {
					bestPassingDelta = delta;
					bestPassing = r;
				}
			}

			// Track closest to target range for fallback
			double distToRange;
			if (apogee < targetApogee) {
				distToRange = targetApogee - apogee;
			} else if (apogee > targetApogee + tolerance) {
				distToRange = apogee - (targetApogee + tolerance);
			} else {
				distToRange = 0;
			}
			if (distToRange < closestOverallDelta) {
				closestOverallDelta = distToRange;
				closestOverall = r;
			}
		}

		MotorSweepResult best = bestPassing != null ? bestPassing : closestOverall;
		double avgImpulse = passingCount > 0 ? sumImpulse / passingCount : 0;
		double avgTwr = passingCount > 0 ? sumTwr / passingCount : 0;

		return new MotorSweepSummary(best, passingCount, results.size(), avgImpulse, avgTwr);
	}

	public MotorSweepResult getBestResult() {
		return bestResult;
	}

	public int getPassingCount() {
		return passingCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public double getAvgImpulse() {
		return avgImpulse;
	}

	public double getAvgTwr() {
		return avgTwr;
	}
}
