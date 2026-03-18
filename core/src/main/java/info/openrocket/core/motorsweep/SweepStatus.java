package info.openrocket.core.motorsweep;

/**
 * Classification of a motor sweep result's status.
 */
public enum SweepStatus {
	/** Meets all criteria: apogee in range, GLOM and TWR within limits. */
	PASS,
	/** Apogee in range but launch mass exceeds max GLOM. */
	FILTERED_GLOM,
	/** Apogee in range but TWR below minimum. */
	FILTERED_TWR,
	/** Apogee in range but fails both GLOM and TWR filters. */
	FILTERED_BOTH,
	/** Apogee outside target range. */
	OUT_OF_RANGE,
	/** Ballast could not be applied (negative mass or unrealizable position). */
	INFEASIBLE,
	/** Simulation failed with an error. */
	ERROR
}
