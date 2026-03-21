package info.openrocket.core.parachutesweep;

/**
 * Per-candidate result from a parachute sweep calculation.
 */
public class ParachuteSweepResult {

	private final ParachuteType type;
	private final double diameter;
	private final double cd;
	private final double area;
	private final double descentRate;
	private final double mass;
	private final double airDensity;
	private final boolean success;
	private final String errorMessage;

	private ParachuteSweepResult(ParachuteType type, double diameter, double cd,
			double area, double descentRate, double mass, double airDensity,
			boolean success, String errorMessage) {
		this.type = type;
		this.diameter = diameter;
		this.cd = cd;
		this.area = area;
		this.descentRate = descentRate;
		this.mass = mass;
		this.airDensity = airDensity;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	/**
	 * Create a successful parachute sweep result.
	 */
	public static ParachuteSweepResult success(ParachuteType type, double diameter,
			double cd, double area, double descentRate, double mass, double airDensity) {
		return new ParachuteSweepResult(type, diameter, cd, area, descentRate,
				mass, airDensity, true, null);
	}

	/**
	 * Create an error parachute sweep result.
	 */
	public static ParachuteSweepResult error(ParachuteType type, double diameter,
			String errorMessage) {
		return new ParachuteSweepResult(type, diameter, Double.NaN, 0, Double.NaN,
				0, 0, false, errorMessage);
	}

	public ParachuteType getType() {
		return type;
	}

	public double getDiameter() {
		return diameter;
	}

	public double getCd() {
		return cd;
	}

	public double getArea() {
		return area;
	}

	public double getDescentRate() {
		return descentRate;
	}

	public double getMass() {
		return mass;
	}

	public double getAirDensity() {
		return airDensity;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
