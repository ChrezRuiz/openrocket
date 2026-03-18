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

	private final Double maxGLOM;
	private final Double minTWR;
	private final boolean autoFillBallast;
	private final double targetStabilityCaliber;

	public MotorSweepWorker(OpenRocketDocument document, Rocket rocket,
			List<ThrustCurveMotor> motors, SweepCallback callback,
			Double maxGLOM, Double minTWR,
			boolean autoFillBallast, double targetStabilityCaliber) {
		this.document = document;
		this.rocket = rocket;
		this.motors = motors;
		this.callback = callback;
		this.maxGLOM = maxGLOM;
		this.minTWR = minTWR;
		this.autoFillBallast = autoFillBallast;
		this.targetStabilityCaliber = targetStabilityCaliber;
	}

	@Override
	protected List<MotorSweepResult> doInBackground() {
		MotorSweepRunner runner = new MotorSweepRunner();
		runner.setMaxGLOM(maxGLOM);
		runner.setMinTWR(minTWR);
		runner.setAutoFillBallast(autoFillBallast);
		runner.setTargetStabilityCaliber(targetStabilityCaliber);

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
