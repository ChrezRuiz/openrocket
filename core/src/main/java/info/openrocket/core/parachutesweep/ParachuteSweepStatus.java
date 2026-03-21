package info.openrocket.core.parachutesweep;

/**
 * Classification of a parachute sweep result's status.
 */
public enum ParachuteSweepStatus {
	/** Descent rate within target +/- tolerance. */
	PASS,
	/** Descent rate above target + tolerance (chute too small). */
	TOO_FAST,
	/** Descent rate below target - tolerance (chute too large). */
	TOO_SLOW,
	/** Calculation failed with an error. */
	ERROR
}
