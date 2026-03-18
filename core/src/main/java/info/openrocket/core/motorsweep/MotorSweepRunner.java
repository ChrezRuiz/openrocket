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

import info.openrocket.core.aerodynamics.BarrowmanCalculator;
import info.openrocket.core.aerodynamics.FlightConditions;
import info.openrocket.core.logging.WarningSet;
import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.masscalc.MassCalculator;
import info.openrocket.core.masscalc.RigidBody;
import info.openrocket.core.motor.MotorConfiguration;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.FlightConfigurationId;
import info.openrocket.core.rocketcomponent.MassComponent;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.position.AxialMethod;
import info.openrocket.core.simulation.FlightData;
import info.openrocket.core.simulation.exception.SimulationException;
import info.openrocket.core.util.CoordinateIF;

/**
 * Orchestrates batch motor sweep simulation with multithreading.
 */
public class MotorSweepRunner {
	private static final Logger log = LoggerFactory.getLogger(MotorSweepRunner.class);
	private static final double G = 9.80665;

	private Double maxGLOM;
	private Double minTWR;
	private boolean autoFillBallast;
	private double targetStabilityCaliber = 1.0;

	public void setMaxGLOM(Double maxGLOM) {
		this.maxGLOM = maxGLOM;
	}

	public void setMinTWR(Double minTWR) {
		this.minTWR = minTWR;
	}

	public void setAutoFillBallast(boolean autoFillBallast) {
		this.autoFillBallast = autoFillBallast;
	}

	public void setTargetStabilityCaliber(double targetStabilityCaliber) {
		this.targetStabilityCaliber = targetStabilityCaliber;
	}

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
				futures.add(executor.submit(new SimulationTask(doc, baseRocket, motor,
						maxGLOM, autoFillBallast, targetStabilityCaliber)));
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
	 * Find all body tubes in the rocket and return their absolute
	 * fore and aft positions (x-axis bounds).
	 *
	 * @return array of {foremost position, aftmost position} in meters
	 */
	static double[] findBodyBounds(Rocket rocket) {
		double foremost = Double.MAX_VALUE;
		double aftmost = -Double.MAX_VALUE;
		boolean found = false;

		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof BodyTube) {
				CoordinateIF[] locations = c.getComponentLocations();
				for (CoordinateIF loc : locations) {
					double start = loc.getX();
					double end = start + c.getLength();
					if (start < foremost) {
						foremost = start;
					}
					if (end > aftmost) {
						aftmost = end;
					}
					found = true;
				}
			}
		}

		if (!found) {
			return null;
		}
		return new double[] { foremost, aftmost };
	}

	/**
	 * Find the body tube that contains the given axial position.
	 */
	static BodyTube findBodyTubeAt(Rocket rocket, double axialPosition) {
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof BodyTube) {
				CoordinateIF[] locations = c.getComponentLocations();
				for (CoordinateIF loc : locations) {
					double start = loc.getX();
					double end = start + c.getLength();
					if (axialPosition >= start && axialPosition <= end) {
						return (BodyTube) c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Analytically compute the axial position for a ballast mass to achieve
	 * a target stability caliber.
	 *
	 * @param currentCgX     current CG x-position (m)
	 * @param currentMass    current total mass (kg)
	 * @param cpX            center of pressure x-position (m)
	 * @param refDiameter    reference diameter for caliber calculation (m)
	 * @param ballastMass    mass of ballast to add (kg)
	 * @param targetCaliber  desired stability caliber
	 * @return computed axial position for ballast (m), or NaN if ballastMass is 0
	 */
	static double computeBallastPosition(double currentCgX, double currentMass,
			double cpX, double refDiameter, double ballastMass, double targetCaliber) {
		if (ballastMass == 0) {
			return Double.NaN;
		}
		double targetCgX = cpX - targetCaliber * refDiameter;
		double totalMass = currentMass + ballastMass;
		return (totalMass * targetCgX - currentMass * currentCgX) / ballastMass;
	}

	/**
	 * Compute current stability caliber for a flight configuration.
	 */
	static double computeStabilityCaliber(FlightConfiguration config) {
		RigidBody massData = MassCalculator.calculateLaunch(config);
		double cgX = massData.getCM().getX();

		BarrowmanCalculator aeroCalc = new BarrowmanCalculator();
		FlightConditions conditions = new FlightConditions(config);
		WarningSet warnings = new WarningSet();
		CoordinateIF cp = aeroCalc.getCP(config, conditions, warnings);
		double cpX = cp.getX();
		double refDiameter = config.getReferenceLength();

		if (refDiameter <= 0) {
			return Double.NaN;
		}
		return (cpX - cgX) / refDiameter;
	}

	/**
	 * A callable task that simulates a single motor on a copy of the rocket.
	 */
	private static class SimulationTask implements Callable<MotorSweepResult> {
		private final OpenRocketDocument doc;
		private final Rocket baseRocket;
		private final ThrustCurveMotor motor;
		private final Double maxGLOM;
		private final boolean autoFillBallast;
		private final double targetStabilityCaliber;

		SimulationTask(OpenRocketDocument doc, Rocket baseRocket, ThrustCurveMotor motor,
				Double maxGLOM, boolean autoFillBallast, double targetStabilityCaliber) {
			this.doc = doc;
			this.baseRocket = baseRocket;
			this.motor = motor;
			this.maxGLOM = maxGLOM;
			this.autoFillBallast = autoFillBallast;
			this.targetStabilityCaliber = targetStabilityCaliber;
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

				// Ballast logic
				double ballastMass = 0;
				double ballastPosition = Double.NaN;
				double stabilityCaliber = Double.NaN;

				if (autoFillBallast && maxGLOM != null) {
					ballastMass = maxGLOM - launchMass;

					if (ballastMass < 0) {
						return MotorSweepResult.infeasible(motor, "Exceeds max GLOM");
					}

					if (ballastMass == 0) {
						// No ballast needed, compute current stability
						stabilityCaliber = computeStabilityCaliber(config);
					} else {
						// Compute ballast position analytically
						double cgX = launchData.getCM().getX();
						BarrowmanCalculator aeroCalc = new BarrowmanCalculator();
						FlightConditions conditions = new FlightConditions(config);
						WarningSet warnings = new WarningSet();
						CoordinateIF cp = aeroCalc.getCP(config, conditions, warnings);
						double cpX = cp.getX();
						double refDiameter = config.getReferenceLength();

						ballastPosition = computeBallastPosition(
								cgX, launchMass, cpX, refDiameter,
								ballastMass, targetStabilityCaliber);

						// Check body bounds
						double[] bounds = findBodyBounds(rocketCopy);
						if (bounds == null
								|| ballastPosition < bounds[0]
								|| ballastPosition > bounds[1]) {
							return MotorSweepResult.infeasible(motor,
									"Ballast position unrealizable");
						}

						// Find parent body tube and add ballast
						BodyTube parentTube = findBodyTubeAt(rocketCopy, ballastPosition);
						if (parentTube == null) {
							return MotorSweepResult.infeasible(motor,
									"Ballast position unrealizable");
						}

						MassComponent ballast = new MassComponent(
								0.01, parentTube.getInnerRadius(), ballastMass);
						ballast.setName("Sweep Ballast");
						ballast.setAxialMethod(AxialMethod.ABSOLUTE);
						ballast.setAxialOffset(ballastPosition);
						parentTube.addChild(ballast);

						// Recalculate with ballast
						config = rocketCopy.getFlightConfiguration(fcid);
						config.setAllStages();
						launchData = MassCalculator.calculateLaunch(config);
						launchMass = launchData.getCM().getWeight();
						stabilityCaliber = computeStabilityCaliber(config);
					}
				}

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
						maxThrust, twr, launchMass, maxVelocity, flightTime,
						ballastMass, ballastPosition, stabilityCaliber);

			} catch (SimulationException e) {
				log.debug("Simulation failed for motor "
						+ motor.getDesignation() + ": " + e.getMessage());
				return MotorSweepResult.error(motor, e.getMessage());
			} catch (Exception e) {
				log.error("Unexpected error simulating motor " + motor.getDesignation(), e);
				return MotorSweepResult.error(motor, "Unexpected error: " + e.getMessage());
			}
		}
	}
}
