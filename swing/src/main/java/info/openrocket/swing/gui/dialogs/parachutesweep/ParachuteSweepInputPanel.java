package info.openrocket.swing.gui.dialogs.parachutesweep;

import java.util.EnumSet;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.parachutesweep.ParachuteType;
import info.openrocket.core.parachutesweep.ParachuteSweepMassResolver;

/**
 * Input panel for parachute sweep parameters.
 */
public class ParachuteSweepInputPanel extends JPanel {

	private final JRadioButton burnoutRadio;
	private final JRadioButton manualRadio;
	private final JLabel manualMassLabel;
	private final JSpinner manualMassSpinner;

	private final JSpinner targetDescentRateSpinner;
	private final JSpinner descentRateToleranceSpinner;

	private final JSpinner minDiameterSpinner;
	private final JSpinner maxDiameterSpinner;
	private final JSpinner diameterStepSpinner;

	private final JCheckBox[] typeCheckBoxes;
	private final JLabel customCdLabel;
	private final JSpinner customCdSpinner;

	private final JSpinner airDensitySpinner;

	public ParachuteSweepInputPanel() {
		super(new MigLayout("fill, ins 5", "[][grow]", ""));

		// --- Mass Mode ---
		add(new JLabel("Mass Mode:"));
		JPanel massPanel = new JPanel(new MigLayout("ins 0", "", ""));
		burnoutRadio = new JRadioButton("Burnout Mass", true);
		manualRadio = new JRadioButton("Manual");
		ButtonGroup massGroup = new ButtonGroup();
		massGroup.add(burnoutRadio);
		massGroup.add(manualRadio);
		massPanel.add(burnoutRadio);
		massPanel.add(manualRadio);
		add(massPanel, "growx, wrap");

		manualMassLabel = new JLabel("Manual Mass (kg):");
		add(manualMassLabel);
		manualMassSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 1000.0, 0.1));
		add(manualMassSpinner, "growx, wrap");
		manualMassLabel.setVisible(false);
		manualMassSpinner.setVisible(false);

		burnoutRadio.addActionListener(e -> updateMassMode());
		manualRadio.addActionListener(e -> updateMassMode());

		// --- Target Descent Rate ---
		add(new JLabel("Target Descent Rate (m/s):"));
		targetDescentRateSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 100.0, 0.5));
		add(targetDescentRateSpinner, "growx, wrap");

		// --- Descent Rate Tolerance ---
		add(new JLabel("Descent Rate Tolerance (m/s):"));
		descentRateToleranceSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 50.0, 0.5));
		add(descentRateToleranceSpinner, "growx, wrap");

		// --- Diameter Range ---
		add(new JLabel("Min Diameter (mm):"));
		minDiameterSpinner = new JSpinner(new SpinnerNumberModel(100.0, 10.0, 10000.0, 10.0));
		add(minDiameterSpinner, "growx, wrap");

		add(new JLabel("Max Diameter (mm):"));
		maxDiameterSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 10.0, 10000.0, 10.0));
		add(maxDiameterSpinner, "growx, wrap");

		add(new JLabel("Diameter Step (mm):"));
		diameterStepSpinner = new JSpinner(new SpinnerNumberModel(50.0, 1.0, 1000.0, 5.0));
		add(diameterStepSpinner, "growx, wrap");

		// --- Parachute Types ---
		add(new JLabel("Parachute Types:"));
		JPanel typePanel = new JPanel(new MigLayout("ins 0, wrap 4", "", ""));
		ParachuteType[] types = ParachuteType.values();
		typeCheckBoxes = new JCheckBox[types.length];
		for (int i = 0; i < types.length; i++) {
			ParachuteType t = types[i];
			typeCheckBoxes[i] = new JCheckBox(t.getDisplayName(), !t.isCustom());
			typePanel.add(typeCheckBoxes[i]);
		}
		add(typePanel, "growx, wrap");

		// --- Custom CD ---
		customCdLabel = new JLabel("Custom CD:");
		add(customCdLabel);
		customCdSpinner = new JSpinner(new SpinnerNumberModel(0.80, 0.01, 2.0, 0.01));
		add(customCdSpinner, "growx, wrap");
		customCdLabel.setVisible(false);
		customCdSpinner.setVisible(false);

		// Wire CUSTOM checkbox to show/hide custom CD spinner
		int customIndex = findCustomIndex();
		if (customIndex >= 0) {
			typeCheckBoxes[customIndex].addActionListener(e -> updateCustomCdVisibility());
		}

		// --- Air Density ---
		add(new JLabel("Air Density (kg/m3):"));
		airDensitySpinner = new JSpinner(new SpinnerNumberModel(1.225, 0.1, 2.0, 0.01));
		add(airDensitySpinner, "growx, wrap");
	}

	private void updateMassMode() {
		boolean manual = manualRadio.isSelected();
		manualMassLabel.setVisible(manual);
		manualMassSpinner.setVisible(manual);
	}

	private void updateCustomCdVisibility() {
		int customIndex = findCustomIndex();
		boolean customSelected = customIndex >= 0 && typeCheckBoxes[customIndex].isSelected();
		customCdLabel.setVisible(customSelected);
		customCdSpinner.setVisible(customSelected);
	}

	private int findCustomIndex() {
		ParachuteType[] types = ParachuteType.values();
		for (int i = 0; i < types.length; i++) {
			if (types[i].isCustom()) {
				return i;
			}
		}
		return -1;
	}

	/** Returns target descent rate in m/s. */
	public double getTargetDescentRate() {
		return ((Number) targetDescentRateSpinner.getValue()).doubleValue();
	}

	/** Returns descent rate tolerance in m/s. */
	public double getDescentRateTolerance() {
		return ((Number) descentRateToleranceSpinner.getValue()).doubleValue();
	}

	/** Returns min diameter in meters. */
	public double getMinDiameter() {
		return ((Number) minDiameterSpinner.getValue()).doubleValue() / 1000.0;
	}

	/** Returns max diameter in meters. */
	public double getMaxDiameter() {
		return ((Number) maxDiameterSpinner.getValue()).doubleValue() / 1000.0;
	}

	/** Returns diameter step in meters. */
	public double getDiameterStep() {
		return ((Number) diameterStepSpinner.getValue()).doubleValue() / 1000.0;
	}

	/** Returns the set of selected parachute types. */
	public Set<ParachuteType> getSelectedTypes() {
		Set<ParachuteType> selected = EnumSet.noneOf(ParachuteType.class);
		ParachuteType[] types = ParachuteType.values();
		for (int i = 0; i < types.length; i++) {
			if (typeCheckBoxes[i].isSelected()) {
				selected.add(types[i]);
			}
		}
		return selected;
	}

	/** Returns the selected mass mode. */
	public ParachuteSweepMassResolver.MassMode getMassMode() {
		if (manualRadio.isSelected()) {
			return ParachuteSweepMassResolver.MassMode.MANUAL;
		}
		return ParachuteSweepMassResolver.MassMode.BURNOUT;
	}

	/** Returns the manual mass in kg, or null if not in manual mode. */
	public Double getManualMass() {
		if (!manualRadio.isSelected()) {
			return null;
		}
		return ((Number) manualMassSpinner.getValue()).doubleValue();
	}

	/** Returns the custom CD value, or null if CUSTOM type is not selected. */
	public Double getCustomCd() {
		int customIndex = findCustomIndex();
		if (customIndex < 0 || !typeCheckBoxes[customIndex].isSelected()) {
			return null;
		}
		return ((Number) customCdSpinner.getValue()).doubleValue();
	}

	/** Returns air density in kg/m3. */
	public double getAirDensity() {
		return ((Number) airDensitySpinner.getValue()).doubleValue();
	}
}
