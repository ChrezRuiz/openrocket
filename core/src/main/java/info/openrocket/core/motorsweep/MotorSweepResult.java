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

	private final double ballastMass;
	private final double ballastPosition;
	private final double stabilityCaliber;
	private final boolean infeasible;
	private final String infeasibleReason;

	private MotorSweepResult(ThrustCurveMotor motor, double apogee, double totalImpulse,
			double maxThrust, double twr, double launchMass, double maxVelocity,
			double flightTime, boolean success, String errorMessage,
			double ballastMass, double ballastPosition, double stabilityCaliber,
			boolean infeasible, String infeasibleReason) {
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
		this.ballastMass = ballastMass;
		this.ballastPosition = ballastPosition;
		this.stabilityCaliber = stabilityCaliber;
		this.infeasible = infeasible;
		this.infeasibleReason = infeasibleReason;
	}

	public static MotorSweepResult success(ThrustCurveMotor motor, double apogee,
			double totalImpulse, double maxThrust, double twr, double launchMass,
			double maxVelocity, double flightTime) {
		return new MotorSweepResult(motor, apogee, totalImpulse, maxThrust, twr,
				launchMass, maxVelocity, flightTime, true, null,
				0.0, Double.NaN, Double.NaN, false, null);
	}

	public static MotorSweepResult success(ThrustCurveMotor motor, double apogee,
			double totalImpulse, double maxThrust, double twr, double launchMass,
			double maxVelocity, double flightTime,
			double ballastMass, double ballastPosition, double stabilityCaliber) {
		return new MotorSweepResult(motor, apogee, totalImpulse, maxThrust, twr,
				launchMass, maxVelocity, flightTime, true, null,
				ballastMass, ballastPosition, stabilityCaliber, false, null);
	}

	public static MotorSweepResult error(ThrustCurveMotor motor, String errorMessage) {
		return new MotorSweepResult(motor, Double.NaN, motor.getTotalImpulseEstimate(),
				motor.getMaxThrustEstimate(), Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, false, errorMessage,
				0.0, Double.NaN, Double.NaN, false, null);
	}

	public static MotorSweepResult infeasible(ThrustCurveMotor motor, String reason) {
		return new MotorSweepResult(motor, Double.NaN, motor.getTotalImpulseEstimate(),
				motor.getMaxThrustEstimate(), Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, false, null,
				0.0, Double.NaN, Double.NaN, true, reason);
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

	public double getBallastMass() {
		return ballastMass;
	}

	public double getBallastPosition() {
		return ballastPosition;
	}

	public double getStabilityCaliber() {
		return stabilityCaliber;
	}

	public boolean isInfeasible() {
		return infeasible;
	}

	public String getInfeasibleReason() {
		return infeasibleReason;
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
