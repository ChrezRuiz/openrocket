package info.openrocket.core.motorsweep;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import info.openrocket.core.database.motor.ThrustCurveMotorSet;
import info.openrocket.core.database.motor.ThrustCurveMotorSetDatabase;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;

/**
 * Filters motors from the database by dimension and type constraints.
 */
public class MotorSweepFilter {

	private double maxDiameter;
	private double maxLength;
	private final Set<Motor.Type> allowedTypes;

	public MotorSweepFilter(double maxDiameter, double maxLength, Set<Motor.Type> allowedTypes) {
		this.maxDiameter = maxDiameter;
		this.maxLength = maxLength;
		this.allowedTypes = EnumSet.copyOf(allowedTypes);
	}

	/**
	 * Filter motors from the database. Returns one representative motor per set
	 * that matches the dimension and type constraints.
	 */
	public List<ThrustCurveMotor> filter(ThrustCurveMotorSetDatabase db) {
		List<ThrustCurveMotor> result = new ArrayList<>();

		for (ThrustCurveMotorSet set : db.getMotorSets()) {
			if (!allowedTypes.contains(set.getType())) {
				continue;
			}

			List<ThrustCurveMotor> motors = set.getMotors();
			if (motors.isEmpty()) {
				continue;
			}

			ThrustCurveMotor motor = motors.get(0);
			if (motor.getDiameter() <= maxDiameter && motor.getLength() <= maxLength) {
				result.add(motor);
			}
		}

		return result;
	}

	public double getMaxDiameter() {
		return maxDiameter;
	}

	public void setMaxDiameter(double maxDiameter) {
		this.maxDiameter = maxDiameter;
	}

	public double getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(double maxLength) {
		this.maxLength = maxLength;
	}

	public Set<Motor.Type> getAllowedTypes() {
		return EnumSet.copyOf(allowedTypes);
	}
}
