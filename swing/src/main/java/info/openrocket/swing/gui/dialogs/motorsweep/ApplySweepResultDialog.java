package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.motorsweep.MotorSweepApplicator;
import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepRunner;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.FlightConfigurationId;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;

/**
 * Modal dialog for selecting a motor from passing sweep results and applying
 * it to a flight configuration.
 */
public class ApplySweepResultDialog extends JDialog {

	private static final Color BEST_COLOR = new Color(200, 255, 200);

	private static final String[] COLUMNS = {
		"Motor", "Manufacturer", "Class", "Apogee (m)",
		"Delta (m)", "TWR", "Total Impulse (Ns)"
	};

	private final Rocket rocket;
	private final List<MotorSweepResult> passingResults;
	private final MotorSweepResult bestResult;

	private final MotorTableModel tableModel;
	private final JTable motorTable;
	private final JComboBox<MountItem> mountCombo;
	private final JRadioButton newConfigRadio;
	private final JRadioButton existingConfigRadio;
	private final JComboBox<ConfigItem> configCombo;
	private final JButton okButton;

	private boolean applied;

	public ApplySweepResultDialog(Window parent, Rocket rocket,
			List<MotorSweepResult> passingResults, MotorSweepResult bestResult) {
		super(parent, "Apply Motor to Configuration", ModalityType.APPLICATION_MODAL);
		this.rocket = rocket;
		this.passingResults = new ArrayList<>(passingResults);
		this.bestResult = bestResult;

		JPanel mainPanel = new JPanel(new MigLayout("fill, ins 10", "[grow]", "[][][][]"));

		// Motor table
		mainPanel.add(new JLabel("Select motor:"), "wrap");
		tableModel = new MotorTableModel();
		motorTable = new JTable(tableModel);
		motorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		motorTable.setDefaultRenderer(Object.class, new BestCellRenderer());
		motorTable.setDefaultRenderer(Number.class, new BestCellRenderer());
		motorTable.setFillsViewportHeight(true);
		motorTable.getSelectionModel().addListSelectionListener(e -> updateOkButton());

		JScrollPane tableScroll = new JScrollPane(motorTable);
		mainPanel.add(tableScroll, "grow, push, h 200lp, wrap");

		// Mount selection
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		mountCombo = new JComboBox<>();
		for (MotorMount m : mounts) {
			mountCombo.addItem(new MountItem(m));
		}

		if (mounts.size() > 1) {
			mainPanel.add(new JLabel("Motor mount:"), "split 2");
			mainPanel.add(mountCombo, "growx, wrap");
		} else if (mounts.size() == 1) {
			mountCombo.setSelectedIndex(0);
		}

		// Configuration choice
		newConfigRadio = new JRadioButton("Create new flight configuration", true);
		existingConfigRadio = new JRadioButton("Apply to existing configuration");
		ButtonGroup configGroup = new ButtonGroup();
		configGroup.add(newConfigRadio);
		configGroup.add(existingConfigRadio);

		configCombo = new JComboBox<>();
		populateConfigCombo();
		configCombo.setEnabled(false);

		newConfigRadio.addActionListener(e -> configCombo.setEnabled(false));
		existingConfigRadio.addActionListener(e -> configCombo.setEnabled(true));

		mainPanel.add(newConfigRadio, "wrap");
		mainPanel.add(existingConfigRadio, "split 2");
		mainPanel.add(configCombo, "growx, wrap");

		// Buttons
		JPanel buttonPanel = new JPanel(new MigLayout("ins 0, right"));
		okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");

		okButton.addActionListener(e -> applyMotor());
		cancelButton.addActionListener(e -> dispose());

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		mainPanel.add(buttonPanel, "right");

		setContentPane(mainPanel);

		// Pre-select best result (must be after okButton is created,
		// since the selection listener calls updateOkButton())
		int bestIndex = this.passingResults.indexOf(bestResult);
		if (bestIndex >= 0) {
			motorTable.setRowSelectionInterval(bestIndex, bestIndex);
		}

		updateOkButton();
		setSize(700, 500);
		setLocationRelativeTo(parent);
	}

	public boolean wasApplied() {
		return applied;
	}

	private void updateOkButton() {
		okButton.setEnabled(motorTable.getSelectedRow() >= 0);
	}

	private void populateConfigCombo() {
		configCombo.removeAllItems();
		for (FlightConfigurationId fcid : rocket.getFlightConfigurations().getIds()) {
			FlightConfiguration fc = rocket.getFlightConfiguration(fcid);
			configCombo.addItem(new ConfigItem(fcid, fc.getName()));
		}
	}

	private void applyMotor() {
		int selectedRow = motorTable.getSelectedRow();
		if (selectedRow < 0) {
			return;
		}

		MotorSweepResult result = passingResults.get(selectedRow);
		ThrustCurveMotor motor = result.getMotor();
		double delay = MotorSweepApplicator.getDefaultEjectionDelay(motor);

		MountItem mountItem = (MountItem) mountCombo.getSelectedItem();
		if (mountItem == null) {
			JOptionPane.showMessageDialog(this, "No motor mount available.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		MotorMount mount = mountItem.mount;

		MotorSweepApplicator.ApplyResult applyResult;
		if (newConfigRadio.isSelected()) {
			applyResult = MotorSweepApplicator.applyToNewConfiguration(
					rocket, mount, motor, delay);
		} else {
			ConfigItem configItem = (ConfigItem) configCombo.getSelectedItem();
			if (configItem == null) {
				JOptionPane.showMessageDialog(this,
						"Please select an existing configuration.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			applyResult = MotorSweepApplicator.applyToExistingConfiguration(
					rocket, mount, motor, delay, configItem.fcid);
		}

		if (applyResult.isSuccess()) {
			applied = true;

			if (MotorSweepApplicator.hasBallastData(result)) {
				promptAndApplyBallast(result);
			}

			JOptionPane.showMessageDialog(this, applyResult.getMessage(),
					"Motor Applied", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} else {
			JOptionPane.showMessageDialog(this, applyResult.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void promptAndApplyBallast(MotorSweepResult result) {
		double ballastMass = result.getBallastMass();
		double ballastPosition = result.getBallastPosition();
		double stabilityCaliber = result.getStabilityCaliber();

		BodyTube tube = MotorSweepRunner.findBodyTubeAt(
				rocket, ballastPosition);
		String tubeName = tube != null ? tube.getName() : "Unknown";

		boolean duplicateExists =
				MotorSweepApplicator.findExistingSweepBallast(rocket) != null;

		ApplyBallastConfirmDialog ballastDialog =
				new ApplyBallastConfirmDialog(this, ballastMass,
						ballastPosition, tubeName, stabilityCaliber,
						duplicateExists);
		ballastDialog.setVisible(true);

		ApplyBallastConfirmDialog.BallastAction action =
				ballastDialog.getResult();

		if (action == ApplyBallastConfirmDialog.BallastAction.CANCEL) {
			return;
		}

		boolean replace = (action
				== ApplyBallastConfirmDialog.BallastAction.REPLACE_EXISTING);

		MotorSweepApplicator.ApplyResult ballastResult =
				MotorSweepApplicator.applyBallast(
						rocket, ballastMass, ballastPosition, replace);

		if (!ballastResult.isSuccess()) {
			JOptionPane.showMessageDialog(this,
					"Failed to apply ballast: "
							+ ballastResult.getMessage(),
					"Ballast Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private class MotorTableModel extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return passingResults.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int col) {
			return COLUMNS[col];
		}

		@Override
		public Class<?> getColumnClass(int col) {
			if (col >= 3 && col <= 6) {
				return Number.class;
			}
			return Object.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			MotorSweepResult r = passingResults.get(row);
			ThrustCurveMotor m = r.getMotor();

			switch (col) {
				case 0: return m.getDesignation();
				case 1: return m.getManufacturer().getDisplayName();
				case 2: return r.getImpulseClass();
				case 3: return String.format("%.1f", r.getApogee());
				case 4: return String.format("%+.1f",
						r.getApogee() - (bestResult != null ? bestResult.getApogee() : 0));
				case 5: return String.format("%.2f", r.getTwr());
				case 6: return String.format("%.1f", r.getTotalImpulse());
				default: return "";
			}
		}
	}

	private class BestCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				MotorSweepResult r = passingResults.get(row);
				if (r == bestResult) {
					c.setBackground(BEST_COLOR);
				} else {
					c.setBackground(Color.WHITE);
				}
			}
			return c;
		}
	}

	private static class MountItem {
		final MotorMount mount;

		MountItem(MotorMount mount) {
			this.mount = mount;
		}

		@Override
		public String toString() {
			return ((RocketComponent) mount).getName();
		}
	}

	private static class ConfigItem {
		final FlightConfigurationId fcid;
		final String name;

		ConfigItem(FlightConfigurationId fcid, String name) {
			this.fcid = fcid;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
