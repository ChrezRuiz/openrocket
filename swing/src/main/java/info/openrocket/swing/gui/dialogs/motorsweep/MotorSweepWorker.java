package info.openrocket.swing.gui.dialogs.motorsweep;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.motorsweep.MotorSweepProgressListener;
import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepRunner;
import info.openrocket.core.rocketcomponent.Rocket;

/**
 * Background worker that drives the motor sweep simulation.
 */
public class MotorSweepWorker extends SwingWorker<List<MotorSweepResult>, Void> {

	public interface SweepCallback {
		void onProgress(int completed, int total);
		void onComplete(List<MotorSweepResult> results);
	}

	private final OpenRocketDocument document;
	private final Rocket rocket;
	private final List<ThrustCurveMotor> motors;
	private final SweepCallback callback;
	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	public MotorSweepWorker(OpenRocketDocument document, Rocket rocket,
			List<ThrustCurveMotor> motors, SweepCallback callback) {
		this.document = document;
		this.rocket = rocket;
		this.motors = motors;
		this.callback = callback;
	}

	@Override
	protected List<MotorSweepResult> doInBackground() {
		MotorSweepRunner runner = new MotorSweepRunner();

		MotorSweepProgressListener listener = new MotorSweepProgressListener() {
			@Override
			public void onProgress(int completed, int total, MotorSweepResult latest) {
				SwingUtilities.invokeLater(() -> callback.onProgress(completed, total));
			}

			@Override
			public boolean isCancelled() {
				return cancelled.get();
			}
		};

		return runner.runSweep(document, rocket, motors, listener);
	}

	@Override
	protected void done() {
		if (!cancelled.get()) {
			try {
				callback.onComplete(get());
			} catch (Exception e) {
				callback.onComplete(List.of());
			}
		}
	}

	public void cancelSweep() {
		cancelled.set(true);
		cancel(true);
	}
}
