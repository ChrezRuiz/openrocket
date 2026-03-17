package info.openrocket.core.motorsweep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.masscalc.MassCalculator;
import info.openrocket.core.masscalc.RigidBody;
import info.openrocket.core.motor.MotorConfiguration;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.FlightConfigurationId;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.simulation.FlightData;
import info.openrocket.core.simulation.exception.SimulationException;

/**
 * Orchestrates batch motor sweep simulation with multithreading.
 */
public class MotorSweepRunner {
	private static final Logger log = LoggerFactory.getLogger(MotorSweepRunner.class);
	private static final double G = 9.80665;

	/**
	 * Find the first active motor mount in the rocket.
	 */
	public static MotorMount findMotorMount(Rocket rocket) {
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof MotorMount) {
				MotorMount mount = (MotorMount) c;
				if (mount.isMotorMount()) {
					return mount;
				}
			}
		}
		return null;
	}

	/**
	 * Run sweep simulations for all provided motors.
	 *
	 * @param doc      the OpenRocket document
	 * @param baseRocket the base rocket design
	 * @param motors   motors to simulate
	 * @param listener progress listener (may be null)
	 * @return list of results, one per motor
	 */
	public List<MotorSweepResult> runSweep(OpenRocketDocument doc, Rocket baseRocket,
			List<ThrustCurveMotor> motors, MotorSweepProgressListener listener) {

		int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<MotorSweepResult> results = new ArrayList<>();

		try {
			List<Future<MotorSweepResult>> futures = new ArrayList<>();

			for (ThrustCurveMotor motor : motors) {
				if (listener != null && listener.isCancelled()) {
					break;
				}
				futures.add(executor.submit(new SimulationTask(doc, baseRocket, motor)));
			}

			int completed = 0;
			for (Future<MotorSweepResult> future : futures) {
				if (listener != null && listener.isCancelled()) {
					break;
				}
				try {
					MotorSweepResult result = future.get();
					results.add(result);
					completed++;
					if (listener != null) {
						listener.onProgress(completed, motors.size(), result);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (ExecutionException e) {
					log.error("Unexpected error in sweep task", e);
				}
			}
		} finally {
			executor.shutdownNow();
		}

		return results;
	}

	/**
	 * A callable task that simulates a single motor on a copy of the rocket.
	 */
	private static class SimulationTask implements Callable<MotorSweepResult> {
		private final OpenRocketDocument doc;
		private final Rocket baseRocket;
		private final ThrustCurveMotor motor;

		SimulationTask(OpenRocketDocument doc, Rocket baseRocket, ThrustCurveMotor motor) {
			this.doc = doc;
			this.baseRocket = baseRocket;
			this.motor = motor;
		}

		@Override
		public MotorSweepResult call() {
			try {
				// Deep copy the rocket
				Rocket rocketCopy = baseRocket.copyWithOriginalID();

				// Find motor mount
				MotorMount mount = findMotorMount(rocketCopy);
				if (mount == null) {
					return MotorSweepResult.error(motor, "No motor mount found");
				}

				// Create flight configuration
				FlightConfigurationId fcid = new FlightConfigurationId();
				rocketCopy.createFlightConfiguration(fcid);

				// Set motor on mount
				MotorConfiguration mc = new MotorConfiguration(mount, fcid);
				mc.setMotor(motor);
				double[] delays = motor.getStandardDelays();
				mc.setEjectionDelay(delays.length > 0 ? delays[0] : 0);
				mount.setMotorConfig(mc, fcid);

				// Update flight configuration
				FlightConfiguration config = rocketCopy.getFlightConfiguration(fcid);
				config.setAllStages();

				// Compute launch mass
				RigidBody launchData = MassCalculator.calculateLaunch(config);
				double launchMass = launchData.getCM().getWeight();

				// Create and run simulation
				Simulation sim = new Simulation(rocketCopy);
				sim.setFlightConfigurationId(fcid);
				sim.simulate();

				// Extract results
				FlightData data = sim.getSimulatedData();
				if (data == null) {
					return MotorSweepResult.error(motor, "No flight data returned");
				}

				double apogee = data.getMaxAltitude();
				double maxVelocity = data.getMaxVelocity();
				double flightTime = data.getFlightTime();
				double totalImpulse = motor.getTotalImpulseEstimate();
				double maxThrust = motor.getMaxThrustEstimate();
				double twr = launchMass > 0 ? maxThrust / (launchMass * G) : 0;

				return MotorSweepResult.success(motor, apogee, totalImpulse,
						maxThrust, twr, launchMass, maxVelocity, flightTime);

			} catch (SimulationException e) {
				log.debug("Simulation failed for motor " + motor.getDesignation() + ": " + e.getMessage());
				return MotorSweepResult.error(motor, e.getMessage());
			} catch (Exception e) {
				log.error("Unexpected error simulating motor " + motor.getDesignation(), e);
				return MotorSweepResult.error(motor, "Unexpected error: " + e.getMessage());
			}
		}
	}
}
