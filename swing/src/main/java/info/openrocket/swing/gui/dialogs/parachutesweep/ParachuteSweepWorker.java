package info.openrocket.swing.gui.dialogs.parachutesweep;

import java.util.List;

import javax.swing.SwingWorker;

import info.openrocket.core.parachutesweep.ParachuteSweepCalculator;
import info.openrocket.core.parachutesweep.ParachuteSweepConfig;
import info.openrocket.core.parachutesweep.ParachuteSweepResult;

/**
 * Background worker that runs the parachute sweep calculation.
 *
 * <p>The analytical calculation is fast, but using a SwingWorker keeps
 * the pattern consistent with the motor sweep and avoids blocking
 * the EDT during mass resolution.</p>
 */
public class ParachuteSweepWorker extends SwingWorker<List<ParachuteSweepResult>, Void> {

	/**
	 * Callback for sweep completion or error.
	 */
	public interface SweepCallback {
		void onComplete(List<ParachuteSweepResult> results);
		void onError(String errorMessage);
	}

	private final ParachuteSweepConfig config;
	private final SweepCallback callback;

	public ParachuteSweepWorker(ParachuteSweepConfig config, SweepCallback callback) {
		this.config = config;
		this.callback = callback;
	}

	@Override
	protected List<ParachuteSweepResult> doInBackground() {
		ParachuteSweepCalculator calculator = new ParachuteSweepCalculator();
		return calculator.calculate(config);
	}

	@Override
	protected void done() {
		try {
			callback.onComplete(get());
		} catch (Exception e) {
			callback.onError(e.getMessage());
		}
	}
}
