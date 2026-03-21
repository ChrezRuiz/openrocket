package info.openrocket.core.parachutesweep;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure analytical calculator for parachute sweep analysis.
 *
 * <p>Computes steady-state descent rates for combinations of parachute types
 * and diameters using the drag equation.</p>
 */
public class ParachuteSweepCalculator {

	private static final double G = 9.80665;

	/**
	 * Calculate descent rates for all type/diameter combinations in the config.
	 *
	 * @param config sweep configuration
	 * @return list of results, one per type/diameter combination
	 */
	public List<ParachuteSweepResult> calculate(ParachuteSweepConfig config) {
		List<ParachuteSweepResult> results = new ArrayList<>();

		for (ParachuteType type : config.getSelectedTypes()) {
			double cd;
			if (type.isCustom()) {
				if (config.getCustomCd() == null || Double.isNaN(config.getCustomCd())) {
					addErrorsForType(results, type, config,
							"Custom type requires a valid drag coefficient");
					continue;
				}
				cd = config.getCustomCd();
			} else {
				cd = type.getDefaultCd();
			}

			for (double diameter = config.getMinDiameter();
					diameter <= config.getMaxDiameter() + config.getDiameterStep() * 0.001;
					diameter += config.getDiameterStep()) {
				double radius = diameter / 2.0;
				double area = Math.PI * radius * radius;

				if (area <= 0) {
					results.add(ParachuteSweepResult.error(type, diameter,
							"Canopy area is zero or negative"));
					continue;
				}

				double denominator = cd * area * config.getAirDensity();
				if (denominator <= 0) {
					results.add(ParachuteSweepResult.error(type, diameter,
							"Invalid drag parameters"));
					continue;
				}

				double descentRate = Math.sqrt(
						2.0 * config.getMass() * G / denominator);

				results.add(ParachuteSweepResult.success(type, diameter, cd, area,
						descentRate, config.getMass(), config.getAirDensity()));
			}
		}

		return results;
	}

	private void addErrorsForType(List<ParachuteSweepResult> results,
			ParachuteType type, ParachuteSweepConfig config, String message) {
		for (double diameter = config.getMinDiameter();
				diameter <= config.getMaxDiameter() + config.getDiameterStep() * 0.001;
				diameter += config.getDiameterStep()) {
			results.add(ParachuteSweepResult.error(type, diameter, message));
		}
	}
}
