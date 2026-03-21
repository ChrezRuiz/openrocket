package info.openrocket.core.parachutesweep;

/**
 * Preset parachute types with default drag coefficients.
 */
public enum ParachuteType {

	FLAT_CIRCULAR("Flat Circular", 0.75),
	HEMISPHERICAL("Hemispherical", 0.62),
	CONICAL("Conical", 0.75),
	CRUCIFORM("Cross/Cruciform", 0.70),
	TOROIDAL("Toroidal", 1.00),
	RIBBON("Ribbon/Ring-slot", 0.47),
	ANNULAR("Annular", 0.90),
	CUSTOM("Custom", Double.NaN);

	private final String displayName;
	private final double defaultCd;

	ParachuteType(String displayName, double defaultCd) {
		this.displayName = displayName;
		this.defaultCd = defaultCd;
	}

	public String getDisplayName() {
		return displayName;
	}

	public double getDefaultCd() {
		return defaultCd;
	}

	/**
	 * Returns true when this type requires a user-supplied drag coefficient.
	 */
	public boolean isCustom() {
		return Double.isNaN(defaultCd);
	}
}
