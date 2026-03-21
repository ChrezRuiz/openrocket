package info.openrocket.core.parachutesweep;

import info.openrocket.core.masscalc.MassCalculator;
import info.openrocket.core.rocketcomponent.FlightConfiguration;

/**
 * Resolves the rocket mass used for parachute descent rate calculations.
 *
 * <p>Supports two modes: BURNOUT (computed from flight configuration) and
 * MANUAL (user-provided value). A flight-event mode may be added later.</p>
 */
public class ParachuteSweepMassResolver {

	/**
	 * Determines how the rocket mass is obtained.
	 */
	public enum MassMode {
		/** Use burnout mass from MassCalculator (structure + empty motors, no propellant). */
		BURNOUT,
		/** User provides mass value directly. */
		MANUAL
	}

	private ParachuteSweepMassResolver() {
		// static utility class
	}

	/**
	 * Resolve the rocket mass for parachute sweep calculations.
	 *
	 * @param mode       the mass determination mode
	 * @param config     flight configuration (required for BURNOUT mode, may be null for MANUAL)
	 * @param manualMass user-provided mass in kg (required for MANUAL mode, may be null for BURNOUT)
	 * @return resolved mass in kg
	 * @throws IllegalArgumentException if required parameters are missing for the selected mode
	 * @throws IllegalStateException    if burnout mass calculation returns non-positive mass
	 */
	public static double resolveMass(MassMode mode, FlightConfiguration config, Double manualMass) {
		switch (mode) {
		case BURNOUT:
			if (config == null) {
				throw new IllegalArgumentException("FlightConfiguration required for BURNOUT mode");
			}
			double burnoutMass = MassCalculator.calculateBurnout(config).getMass();
			if (burnoutMass <= 0) {
				throw new IllegalStateException("Burnout mass is non-positive: " + burnoutMass);
			}
			return burnoutMass;
		case MANUAL:
			if (manualMass == null || manualMass <= 0) {
				throw new IllegalArgumentException("Positive manual mass required for MANUAL mode");
			}
			return manualMass;
		default:
			throw new IllegalArgumentException("Unsupported mass mode: " + mode);
		}
	}
}
