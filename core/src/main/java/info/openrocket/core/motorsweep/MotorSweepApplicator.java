package info.openrocket.core.motorsweep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import info.openrocket.core.motor.MotorConfiguration;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.FlightConfigurationId;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;

/**
 * Stateless service for applying a motor sweep result back into a rocket's
 * flight configuration.
 */
public final class MotorSweepApplicator {

	private MotorSweepApplicator() {
	}

	/**
	 * Result of applying a motor to a configuration.
	 */
	public static class ApplyResult {
		private final boolean success;
		private final String message;
		private final FlightConfigurationId appliedConfigId;

		private ApplyResult(boolean success, String message, FlightConfigurationId appliedConfigId) {
			this.success = success;
			this.message = message;
			this.appliedConfigId = appliedConfigId;
		}

		static ApplyResult success(String message, FlightConfigurationId fcid) {
			return new ApplyResult(true, message, fcid);
		}

		static ApplyResult error(String message) {
			return new ApplyResult(false, message, null);
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}

		public FlightConfigurationId getAppliedConfigId() {
			return appliedConfigId;
		}
	}

	/**
	 * Find all active motor mounts in the rocket.
	 */
	public static List<MotorMount> findAllMotorMounts(Rocket rocket) {
		List<MotorMount> mounts = new ArrayList<>();
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof MotorMount) {
				MotorMount mount = (MotorMount) c;
				if (mount.isMotorMount()) {
					mounts.add(mount);
				}
			}
		}
		return mounts;
	}

	/**
	 * Get the default ejection delay for a motor.
	 * Returns the first standard delay, or 0.0 if none defined.
	 */
	public static double getDefaultEjectionDelay(ThrustCurveMotor motor) {
		double[] delays = motor.getStandardDelays();
		return delays.length > 0 ? delays[0] : 0.0;
	}

	/**
	 * Apply a motor to a new flight configuration on the given mount.
	 */
	public static ApplyResult applyToNewConfiguration(Rocket rocket, MotorMount mount,
			ThrustCurveMotor motor, double ejectionDelay) {
		if (rocket == null) {
			return ApplyResult.error("Rocket is null");
		}
		if (mount == null) {
			return ApplyResult.error("Motor mount is null");
		}
		if (motor == null) {
			return ApplyResult.error("Motor is null");
		}

		FlightConfigurationId fcid = new FlightConfigurationId();
		rocket.createFlightConfiguration(fcid);

		MotorConfiguration mc = new MotorConfiguration(mount, fcid);
		mc.setMotor(motor);
		mc.setEjectionDelay(ejectionDelay);
		mount.setMotorConfig(mc, fcid);

		FlightConfiguration config = rocket.getFlightConfiguration(fcid);
		config.setAllStages();
		rocket.setSelectedConfiguration(fcid);

		return ApplyResult.success("Motor " + motor.getDesignation()
				+ " applied to new configuration", fcid);
	}

	/**
	 * Apply a motor to an existing flight configuration on the given mount.
	 */
	public static ApplyResult applyToExistingConfiguration(Rocket rocket, MotorMount mount,
			ThrustCurveMotor motor, double ejectionDelay, FlightConfigurationId existingFcid) {
		if (rocket == null) {
			return ApplyResult.error("Rocket is null");
		}
		if (mount == null) {
			return ApplyResult.error("Motor mount is null");
		}
		if (motor == null) {
			return ApplyResult.error("Motor is null");
		}
		if (existingFcid == null) {
			return ApplyResult.error("Flight configuration ID is null");
		}
		if (!rocket.containsFlightConfigurationID(existingFcid)) {
			return ApplyResult.error("Flight configuration does not exist");
		}

		MotorConfiguration mc = new MotorConfiguration(mount, existingFcid);
		mc.setMotor(motor);
		mc.setEjectionDelay(ejectionDelay);
		mount.setMotorConfig(mc, existingFcid);

		rocket.setSelectedConfiguration(existingFcid);

		return ApplyResult.success("Motor " + motor.getDesignation()
				+ " applied to existing configuration", existingFcid);
	}
}
