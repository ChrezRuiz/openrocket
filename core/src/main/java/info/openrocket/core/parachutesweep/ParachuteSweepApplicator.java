package info.openrocket.core.parachutesweep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.Parachute;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;

/**
 * Stateless service for applying a parachute sweep result back into a rocket
 * design by updating an existing parachute or adding a new one.
 */
public final class ParachuteSweepApplicator {

	private ParachuteSweepApplicator() {
	}

	/**
	 * Result of applying a parachute configuration.
	 */
	public static class ApplyResult {
		private final boolean success;
		private final String message;

		private ApplyResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		static ApplyResult success(String message) {
			return new ApplyResult(true, message);
		}

		static ApplyResult error(String message) {
			return new ApplyResult(false, message);
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}
	}

	/**
	 * Find all Parachute components in the rocket tree.
	 */
	public static List<Parachute> findAllParachutes(Rocket rocket) {
		List<Parachute> parachutes = new ArrayList<>();
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof Parachute) {
				parachutes.add((Parachute) c);
			}
		}
		return parachutes;
	}

	/**
	 * Find all BodyTube components in the rocket tree (for adding new parachutes).
	 */
	public static List<BodyTube> findAllBodyTubes(Rocket rocket) {
		List<BodyTube> tubes = new ArrayList<>();
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof BodyTube) {
				tubes.add((BodyTube) c);
			}
		}
		return tubes;
	}

	/**
	 * Update an existing parachute's diameter and CD from a sweep result.
	 */
	public static ApplyResult applyToExistingParachute(Parachute parachute,
			double diameter, double cd) {
		if (parachute == null) {
			return ApplyResult.error("Parachute is null");
		}
		if (diameter <= 0) {
			return ApplyResult.error("Diameter must be positive");
		}
		if (Double.isNaN(cd) || cd <= 0) {
			return ApplyResult.error("CD must be positive");
		}

		parachute.setCDAutomatic(false);
		parachute.setCD(cd);
		parachute.setDiameter(diameter);

		return ApplyResult.success("Parachute updated: diameter="
				+ String.format("%.1f mm", diameter * 1000)
				+ ", CD=" + String.format("%.3f", cd));
	}

	/**
	 * Add a new Parachute component to a body tube with the given diameter and CD.
	 */
	public static ApplyResult addNewParachute(BodyTube parent, double diameter, double cd) {
		if (parent == null) {
			return ApplyResult.error("Parent body tube is null");
		}
		if (diameter <= 0) {
			return ApplyResult.error("Diameter must be positive");
		}
		if (Double.isNaN(cd) || cd <= 0) {
			return ApplyResult.error("CD must be positive");
		}

		Parachute chute = new Parachute();
		chute.setCDAutomatic(false);
		chute.setCD(cd);
		chute.setDiameter(diameter);
		chute.setName("Parachute (Sweep)");
		parent.addChild(chute);

		return ApplyResult.success("New parachute added: diameter="
				+ String.format("%.1f mm", diameter * 1000)
				+ ", CD=" + String.format("%.3f", cd));
	}
}
