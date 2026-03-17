package info.openrocket.core.motorsweep;

/**
 * Callback interface for motor sweep progress reporting and cancellation.
 */
public interface MotorSweepProgressListener {

	/**
	 * Called after each motor simulation completes.
	 *
	 * @param completed number of completed simulations
	 * @param total     total number of motors to simulate
	 * @param latest    the result of the most recently completed simulation
	 */
	void onProgress(int completed, int total, MotorSweepResult latest);

	/**
	 * Returns whether the sweep has been cancelled.
	 */
	boolean isCancelled();
}
