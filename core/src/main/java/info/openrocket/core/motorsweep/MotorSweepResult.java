package info.openrocket.core.motorsweep;

import info.openrocket.core.motor.ThrustCurveMotor;

/**
 * Per-motor simulation result from a motor sweep.
 */
public class MotorSweepResult {

	private final ThrustCurveMotor motor;
	private final double apogee;
	private final double totalImpulse;
	private final double maxThrust;
	private final double twr;
	private final double launchMass;
	private final double maxVelocity;
	private final double flightTime;
	private final boolean success;
	private final String errorMessage;

	private MotorSweepResult(ThrustCurveMotor motor, double apogee, double totalImpulse,
			double maxThrust, double twr, double launchMass, double maxVelocity,
			double flightTime, boolean success, String errorMessage) {
		this.motor = motor;
		this.apogee = apogee;
		this.totalImpulse = totalImpulse;
		this.maxThrust = maxThrust;
		this.twr = twr;
		this.launchMass = launchMass;
		this.maxVelocity = maxVelocity;
		this.flightTime = flightTime;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	public static MotorSweepResult success(ThrustCurveMotor motor, double apogee,
			double totalImpulse, double maxThrust, double twr, double launchMass,
			double maxVelocity, double flightTime) {
		return new MotorSweepResult(motor, apogee, totalImpulse, maxThrust, twr,
				launchMass, maxVelocity, flightTime, true, null);
	}

	public static MotorSweepResult error(ThrustCurveMotor motor, String errorMessage) {
		return new MotorSweepResult(motor, Double.NaN, motor.getTotalImpulseEstimate(),
				motor.getMaxThrustEstimate(), Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, false, errorMessage);
	}

	public ThrustCurveMotor getMotor() {
		return motor;
	}

	public double getApogee() {
		return apogee;
	}

	public double getTotalImpulse() {
		return totalImpulse;
	}

	public double getMaxThrust() {
		return maxThrust;
	}

	public double getTwr() {
		return twr;
	}

	public double getLaunchMass() {
		return launchMass;
	}

	public double getMaxVelocity() {
		return maxVelocity;
	}

	public double getFlightTime() {
		return flightTime;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Returns the impulse class letter derived from the motor's common name.
	 */
	public String getImpulseClass() {
		String name = motor.getCommonName();
		if (name != null && !name.isEmpty()) {
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (Character.isLetter(c)) {
					return String.valueOf(Character.toUpperCase(c));
				}
			}
		}
		return "?";
	}
}
