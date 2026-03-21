package info.openrocket.core.parachutesweep;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Immutable configuration for a parachute sweep calculation.
 */
public class ParachuteSweepConfig {

	private final double targetDescentRate;
	private final double descentRateTolerance;
	private final double minDiameter;
	private final double maxDiameter;
	private final double diameterStep;
	private final Set<ParachuteType> selectedTypes;
	private final double mass;
	private final double airDensity;
	private final Double customCd;

	/**
	 * Construct a parachute sweep configuration.
	 *
	 * @param targetDescentRate   desired descent rate (m/s)
	 * @param descentRateTolerance one-sided tolerance (m/s)
	 * @param minDiameter         minimum canopy diameter (m)
	 * @param maxDiameter         maximum canopy diameter (m)
	 * @param diameterStep        diameter increment (m)
	 * @param selectedTypes       parachute types to sweep
	 * @param mass                total descent mass (kg)
	 * @param airDensity          air density (kg/m3)
	 * @param customCd            drag coefficient for CUSTOM type (nullable)
	 */
	public ParachuteSweepConfig(double targetDescentRate, double descentRateTolerance,
			double minDiameter, double maxDiameter, double diameterStep,
			Set<ParachuteType> selectedTypes, double mass, double airDensity,
			Double customCd) {
		if (minDiameter >= maxDiameter) {
			throw new IllegalArgumentException(
					"minDiameter must be less than maxDiameter");
		}
		if (diameterStep <= 0) {
			throw new IllegalArgumentException(
					"diameterStep must be positive");
		}
		if (mass <= 0) {
			throw new IllegalArgumentException("mass must be positive");
		}
		if (airDensity <= 0) {
			throw new IllegalArgumentException(
					"airDensity must be positive");
		}
		if (selectedTypes == null || selectedTypes.isEmpty()) {
			throw new IllegalArgumentException(
					"selectedTypes must not be null or empty");
		}

		this.targetDescentRate = targetDescentRate;
		this.descentRateTolerance = descentRateTolerance;
		this.minDiameter = minDiameter;
		this.maxDiameter = maxDiameter;
		this.diameterStep = diameterStep;
		this.selectedTypes = Collections.unmodifiableSet(new LinkedHashSet<>(selectedTypes));
		this.mass = mass;
		this.airDensity = airDensity;
		this.customCd = customCd;
	}

	public double getTargetDescentRate() {
		return targetDescentRate;
	}

	public double getDescentRateTolerance() {
		return descentRateTolerance;
	}

	public double getMinDiameter() {
		return minDiameter;
	}

	public double getMaxDiameter() {
		return maxDiameter;
	}

	public double getDiameterStep() {
		return diameterStep;
	}

	public Set<ParachuteType> getSelectedTypes() {
		return selectedTypes;
	}

	public double getMass() {
		return mass;
	}

	public double getAirDensity() {
		return airDensity;
	}

	public Double getCustomCd() {
		return customCd;
	}
}
