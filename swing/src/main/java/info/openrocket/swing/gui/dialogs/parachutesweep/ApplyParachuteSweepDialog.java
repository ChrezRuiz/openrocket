package info.openrocket.swing.gui.dialogs.parachutesweep;

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

import info.openrocket.core.parachutesweep.ParachuteSweepApplicator;
import info.openrocket.core.parachutesweep.ParachuteSweepResult;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.Parachute;
import info.openrocket.core.rocketcomponent.Rocket;

/**
 * Modal dialog for applying a parachute sweep result to the rocket design.
 */
public class ApplyParachuteSweepDialog extends JDialog {

	private static final Color BEST_COLOR = new Color(200, 255, 200);

	private static final String[] COLUMNS = {
		"Type", "Diameter (mm)", "CD", "Descent Rate (m/s)"
	};

	private final Rocket rocket;
	private final List<ParachuteSweepResult> passingResults;
	private final ParachuteSweepResult bestResult;

	private final ResultTableModel tableModel;
	private final JTable resultTable;
	private final JRadioButton updateExistingRadio;
	private final JRadioButton addNewRadio;
	private final JComboBox<ParachuteItem> parachuteCombo;
	private final JComboBox<BodyTubeItem> bodyTubeCombo;
	private final JButton okButton;

	private boolean applied;

	public ApplyParachuteSweepDialog(Window parent, Rocket rocket,
			List<ParachuteSweepResult> passingResults, ParachuteSweepResult bestResult) {
		super(parent, "Apply Parachute Configuration", ModalityType.APPLICATION_MODAL);
		this.rocket = rocket;
		this.passingResults = new ArrayList<>(passingResults);
		this.bestResult = bestResult;

		JPanel mainPanel = new JPanel(new MigLayout("fill, ins 10", "[grow]", "[][][][]"));

		// Result table
		mainPanel.add(new JLabel("Select parachute configuration:"), "wrap");
		tableModel = new ResultTableModel();
		resultTable = new JTable(tableModel);
		resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultTable.setDefaultRenderer(Object.class, new BestCellRenderer());
		resultTable.setDefaultRenderer(Number.class, new BestCellRenderer());
		resultTable.setFillsViewportHeight(true);
		resultTable.getSelectionModel().addListSelectionListener(e -> updateOkButton());

		JScrollPane tableScroll = new JScrollPane(resultTable);
		mainPanel.add(tableScroll, "grow, push, h 200lp, wrap");

		// Apply mode
		updateExistingRadio = new JRadioButton("Update existing parachute", true);
		addNewRadio = new JRadioButton("Add new parachute to body tube");
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(updateExistingRadio);
		modeGroup.add(addNewRadio);

		// Parachute selection
		List<Parachute> parachutes = ParachuteSweepApplicator.findAllParachutes(rocket);
		parachuteCombo = new JComboBox<>();
		for (Parachute p : parachutes) {
			parachuteCombo.addItem(new ParachuteItem(p));
		}

		// Body tube selection
		List<BodyTube> bodyTubes = ParachuteSweepApplicator.findAllBodyTubes(rocket);
		bodyTubeCombo = new JComboBox<>();
		for (BodyTube bt : bodyTubes) {
			bodyTubeCombo.addItem(new BodyTubeItem(bt));
		}
		bodyTubeCombo.setEnabled(false);

		updateExistingRadio.addActionListener(e -> updateModeState());
		addNewRadio.addActionListener(e -> updateModeState());

		mainPanel.add(updateExistingRadio, "split 2");
		mainPanel.add(parachuteCombo, "growx, wrap");
		mainPanel.add(addNewRadio, "split 2");
		mainPanel.add(bodyTubeCombo, "growx, wrap");

		// If no existing parachutes, default to add new
		if (parachutes.isEmpty()) {
			addNewRadio.setSelected(true);
			updateExistingRadio.setEnabled(false);
			updateModeState();
		}

		// If no body tubes, disable add new
		if (bodyTubes.isEmpty()) {
			addNewRadio.setEnabled(false);
		}

		// Buttons
		JPanel buttonPanel = new JPanel(new MigLayout("ins 0, right"));
		okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");

		okButton.addActionListener(e -> applyResult());
		cancelButton.addActionListener(e -> dispose());

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		mainPanel.add(buttonPanel, "right");

		setContentPane(mainPanel);

		// Pre-select best result
		int bestIndex = this.passingResults.indexOf(bestResult);
		if (bestIndex >= 0) {
			resultTable.setRowSelectionInterval(bestIndex, bestIndex);
		}

		updateOkButton();
		setSize(700, 500);
		setLocationRelativeTo(parent);
	}

	public boolean wasApplied() {
		return applied;
	}

	private void updateOkButton() {
		okButton.setEnabled(resultTable.getSelectedRow() >= 0);
	}

	private void updateModeState() {
		boolean useExisting = updateExistingRadio.isSelected();
		parachuteCombo.setEnabled(useExisting);
		bodyTubeCombo.setEnabled(!useExisting);
	}

	private void applyResult() {
		int selectedRow = resultTable.getSelectedRow();
		if (selectedRow < 0) {
			return;
		}

		ParachuteSweepResult result = passingResults.get(selectedRow);
		double diameter = result.getDiameter();
		double cd = result.getCd();

		ParachuteSweepApplicator.ApplyResult applyResult;
		if (updateExistingRadio.isSelected()) {
			ParachuteItem item = (ParachuteItem) parachuteCombo.getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this,
						"No parachute available to update.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			applyResult = ParachuteSweepApplicator.applyToExistingParachute(
					item.parachute, diameter, cd);
		} else {
			BodyTubeItem item = (BodyTubeItem) bodyTubeCombo.getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this,
						"No body tube available.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			applyResult = ParachuteSweepApplicator.addNewParachute(
					item.bodyTube, diameter, cd);
		}

		if (applyResult.isSuccess()) {
			applied = true;
			JOptionPane.showMessageDialog(this, applyResult.getMessage(),
					"Parachute Applied", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} else {
			JOptionPane.showMessageDialog(this, applyResult.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private class ResultTableModel extends AbstractTableModel {
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
			if (col >= 1 && col <= 3) {
				return Number.class;
			}
			return Object.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			ParachuteSweepResult r = passingResults.get(row);
			switch (col) {
				case 0: return r.getType().getDisplayName();
				case 1: return String.format("%.0f", r.getDiameter() * 1000.0);
				case 2: return String.format("%.3f", r.getCd());
				case 3: return String.format("%.2f", r.getDescentRate());
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
				ParachuteSweepResult r = passingResults.get(row);
				if (r == bestResult) {
					c.setBackground(BEST_COLOR);
				} else {
					c.setBackground(Color.WHITE);
				}
			}
			return c;
		}
	}

	private static class ParachuteItem {
		final Parachute parachute;

		ParachuteItem(Parachute parachute) {
			this.parachute = parachute;
		}

		@Override
		public String toString() {
			return parachute.getName();
		}
	}

	private static class BodyTubeItem {
		final BodyTube bodyTube;

		BodyTubeItem(BodyTube bodyTube) {
			this.bodyTube = bodyTube;
		}

		@Override
		public String toString() {
			return bodyTube.getName();
		}
	}
}
