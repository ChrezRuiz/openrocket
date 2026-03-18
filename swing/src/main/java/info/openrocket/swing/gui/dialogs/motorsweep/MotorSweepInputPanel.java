package info.openrocket.swing.gui.dialogs.motorsweep;

import java.util.EnumSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.motor.Motor;
import info.openrocket.core.unit.UnitGroup;

/**
 * Input panel for motor sweep constraints.
 */
public class MotorSweepInputPanel extends JPanel {

	private final JSpinner maxDiameterSpinner;
	private final JSpinner maxLengthSpinner;
	private final JSpinner targetApogeeSpinner;
	private final JSpinner toleranceSpinner;
	private final JCheckBox singleUseCheck;
	private final JCheckBox reloadableCheck;
	private final JCheckBox hybridCheck;

	private final JCheckBox glomEnabled;
	private final JSpinner glomSpinner;
	private final JCheckBox twrEnabled;
	private final JSpinner twrSpinner;
	private final JCheckBox autoFillBallast;
	private final JLabel stabilityCaliLabel;
	private final JSpinner stabilityCaliberSpinner;

	public MotorSweepInputPanel() {
		super(new MigLayout("fill, ins 5", "[][grow]", ""));

		// Max Diameter
		add(new JLabel("Max Motor Diameter (mm):"));
		maxDiameterSpinner = new JSpinner(new SpinnerNumberModel(38.0, 1.0, 200.0, 1.0));
		add(maxDiameterSpinner, "growx, wrap");

		// Max Length
		add(new JLabel("Max Motor Length (mm):"));
		maxLengthSpinner = new JSpinner(new SpinnerNumberModel(300.0, 10.0, 2000.0, 10.0));
		add(maxLengthSpinner, "growx, wrap");

		// Target Apogee
		add(new JLabel("Target Apogee (" + UnitGroup.UNITS_DISTANCE.getDefaultUnit().getUnit() + "):"));
		targetApogeeSpinner = new JSpinner(new SpinnerNumberModel(300.0, 1.0, 100000.0, 10.0));
		add(targetApogeeSpinner, "growx, wrap");

		// Tolerance
		add(new JLabel("Apogee Tolerance (" + UnitGroup.UNITS_DISTANCE.getDefaultUnit().getUnit() + "):"));
		toleranceSpinner = new JSpinner(new SpinnerNumberModel(50.0, 1.0, 10000.0, 5.0));
		add(toleranceSpinner, "growx, wrap");

		// Motor type checkboxes
		add(new JLabel("Motor Types:"));
		JPanel typePanel = new JPanel(new MigLayout("ins 0", "", ""));
		singleUseCheck = new JCheckBox("Single-use", true);
		reloadableCheck = new JCheckBox("Reloadable", true);
		hybridCheck = new JCheckBox("Hybrid", true);
		typePanel.add(singleUseCheck);
		typePanel.add(reloadableCheck);
		typePanel.add(hybridCheck);
		add(typePanel, "growx, wrap");

		// Max GLOM
		glomEnabled = new JCheckBox("Max GLOM (kg):");
		add(glomEnabled);
		glomSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 1000.0, 0.1));
		glomSpinner.setEnabled(false);
		add(glomSpinner, "growx, wrap");

		// Min TWR
		twrEnabled = new JCheckBox("Min Avg TWR:");
		add(twrEnabled);
		twrSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, 100.0, 0.1));
		twrSpinner.setEnabled(false);
		add(twrSpinner, "growx, wrap");

		// Auto-fill Ballast
		add(new JLabel(""));
		autoFillBallast = new JCheckBox("Auto-fill ballast to max GLOM");
		autoFillBallast.setEnabled(false);
		add(autoFillBallast, "growx, wrap");

		// Target Stability Caliber
		stabilityCaliLabel = new JLabel("Target Stability (cal):");
		stabilityCaliLabel.setEnabled(false);
		add(stabilityCaliLabel);
		stabilityCaliberSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 5.0, 0.1));
		stabilityCaliberSpinner.setEnabled(false);
		add(stabilityCaliberSpinner, "growx, wrap");

		// Wire enable/disable logic
		glomEnabled.addActionListener(e -> updateGlomDependents());
		twrEnabled.addActionListener(e -> twrSpinner.setEnabled(twrEnabled.isSelected()));
		autoFillBallast.addActionListener(e -> updateStabilityEnabled());
	}

	private void updateGlomDependents() {
		boolean enabled = glomEnabled.isSelected();
		glomSpinner.setEnabled(enabled);
		autoFillBallast.setEnabled(enabled);
		if (!enabled) {
			autoFillBallast.setSelected(false);
		}
		updateStabilityEnabled();
	}

	private void updateStabilityEnabled() {
		boolean enabled = autoFillBallast.isSelected() && autoFillBallast.isEnabled();
		stabilityCaliLabel.setEnabled(enabled);
		stabilityCaliberSpinner.setEnabled(enabled);
	}

	/** Returns max diameter in meters. */
	public double getMaxDiameter() {
		return ((Number) maxDiameterSpinner.getValue()).doubleValue() / 1000.0;
	}

	/** Returns max length in meters. */
	public double getMaxLength() {
		return ((Number) maxLengthSpinner.getValue()).doubleValue() / 1000.0;
	}

	/** Returns target apogee in the default display unit, converted to meters. */
	public double getTargetApogee() {
		double val = ((Number) targetApogeeSpinner.getValue()).doubleValue();
		return UnitGroup.UNITS_DISTANCE.getDefaultUnit().fromUnit(val);
	}

	/** Returns tolerance in the default display unit, converted to meters. */
	public double getTolerance() {
		double val = ((Number) toleranceSpinner.getValue()).doubleValue();
		return UnitGroup.UNITS_DISTANCE.getDefaultUnit().fromUnit(val);
	}

	public Set<Motor.Type> getAllowedTypes() {
		Set<Motor.Type> types = EnumSet.noneOf(Motor.Type.class);
		if (singleUseCheck.isSelected()) types.add(Motor.Type.SINGLE);
		if (reloadableCheck.isSelected()) types.add(Motor.Type.RELOAD);
		if (hybridCheck.isSelected()) types.add(Motor.Type.HYBRID);
		return types;
	}

	/** Returns max GLOM in kg, or null if disabled. */
	public Double getMaxGLOM() {
		if (!glomEnabled.isSelected()) {
			return null;
		}
		return ((Number) glomSpinner.getValue()).doubleValue();
	}

	/** Returns min TWR, or null if disabled. */
	public Double getMinTWR() {
		if (!twrEnabled.isSelected()) {
			return null;
		}
		return ((Number) twrSpinner.getValue()).doubleValue();
	}

	/** Returns whether auto-fill ballast is enabled. */
	public boolean isAutoFillBallast() {
		return autoFillBallast.isSelected() && autoFillBallast.isEnabled();
	}

	/** Returns target stability caliber. */
	public double getTargetStabilityCaliber() {
		return ((Number) stabilityCaliberSpinner.getValue()).doubleValue();
	}
}
