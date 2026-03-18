package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepSummary;
import info.openrocket.core.motorsweep.SweepStatus;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.unit.UnitGroup;

/**
 * Sortable table displaying motor sweep results.
 */
public class MotorSweepTablePanel extends JPanel {

	private static final Color PASS_COLOR = new Color(200, 255, 200);
	private static final Color ERROR_COLOR = new Color(255, 200, 200);
	private static final Color FILTERED_COLOR = new Color(220, 220, 220);
	private static final Color INFEASIBLE_COLOR = new Color(255, 255, 200);
	private static final Color BEST_COLOR = new Color(255, 140, 0);

	private static final String[] COLUMNS = {
		"Motor", "Manufacturer", "Diameter (mm)", "Length (mm)", "Class",
		"Total Impulse (Ns)", "Max Thrust (N)", "TWR", "Apogee", "Delta",
		"GLOM (kg)", "Ballast (kg)", "Stability (cal)", "Status"
	};

	private final SweepTableModel tableModel;
	private final JTable table;
	private final JToggleButton passOnlyToggle;
	private final JButton exportButton;

	public MotorSweepTablePanel() {
		super(new MigLayout("fill, ins 0"));
		tableModel = new SweepTableModel();
		table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setDefaultRenderer(Object.class, new SweepCellRenderer());
		table.setDefaultRenderer(Number.class, new SweepCellRenderer());
		table.setFillsViewportHeight(true);

		JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 5", "[][]push", ""));
		passOnlyToggle = new JToggleButton("Show Only Passing");
		passOnlyToggle.setEnabled(false);
		exportButton = new JButton("Export CSV...");
		exportButton.setEnabled(false);
		toolbar.add(passOnlyToggle);
		toolbar.add(exportButton);
		add(toolbar, "growx, wrap");

		passOnlyToggle.addActionListener(e -> {
			tableModel.setPassOnly(passOnlyToggle.isSelected());
		});

		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, "grow, push");
	}

	public void setResults(List<MotorSweepResult> results, double targetApogee,
			double tolerance, Double maxGLOM, Double minTWR,
			boolean autoFillBallast, boolean showFiltered,
			MotorSweepResult bestResult) {
		tableModel.setResults(results, targetApogee, tolerance, maxGLOM, minTWR,
				autoFillBallast, showFiltered, bestResult);
		passOnlyToggle.setSelected(false);
		passOnlyToggle.setEnabled(true);
		exportButton.setEnabled(true);
	}

	private class SweepTableModel extends AbstractTableModel {
		private List<MotorSweepResult> displayResults = new ArrayList<>();
		private List<SweepStatus> displayStatuses = new ArrayList<>();
		private List<MotorSweepResult> allResults = new ArrayList<>();
		private List<SweepStatus> allStatuses = new ArrayList<>();
		private boolean passOnly;
		private boolean lastShowFiltered;
		private double target;
		private boolean showBallast;
		private MotorSweepResult bestResult;

		void applyFilter() {
			displayResults = new ArrayList<>();
			displayStatuses = new ArrayList<>();

			for (int i = 0; i < allResults.size(); i++) {
				SweepStatus status = allStatuses.get(i);

				if (passOnly && status != SweepStatus.PASS) {
					continue;
				}

				boolean isFiltered = status == SweepStatus.FILTERED_GLOM
						|| status == SweepStatus.FILTERED_TWR
						|| status == SweepStatus.FILTERED_BOTH
						|| status == SweepStatus.INFEASIBLE;

				if (!isFiltered || lastShowFiltered) {
					displayResults.add(allResults.get(i));
					displayStatuses.add(status);
				}
			}
			fireTableDataChanged();
		}

		void setResults(List<MotorSweepResult> results, double target, double tol,
				Double maxGLOM, Double minTWR, boolean autoFillBallast,
				boolean showFiltered, MotorSweepResult bestResult) {
			this.target = target;
			this.showBallast = autoFillBallast;
			this.bestResult = bestResult;
			this.lastShowFiltered = showFiltered;
			this.passOnly = false;
			this.allResults = new ArrayList<>();
			this.allStatuses = new ArrayList<>();

			for (MotorSweepResult r : results) {
				SweepStatus status = MotorSweepSummary.classifyResult(r, target, tol,
						maxGLOM, minTWR);
				allResults.add(r);
				allStatuses.add(status);
			}
			applyFilter();
		}

		void setPassOnly(boolean passOnly) {
			this.passOnly = passOnly;
			applyFilter();
		}

		@Override
		public int getRowCount() {
			return displayResults.size();
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
			if (col >= 2 && col <= 12) return Number.class;
			return Object.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			MotorSweepResult r = displayResults.get(row);
			SweepStatus status = displayStatuses.get(row);
			ThrustCurveMotor m = r.getMotor();

			switch (col) {
				case 0: return m.getDesignation();
				case 1: return m.getManufacturer().getDisplayName();
				case 2: return Math.round(m.getDiameter() * 1000.0);
				case 3: return Math.round(m.getLength() * 1000.0);
				case 4: return r.getImpulseClass();
				case 5: return r.isSuccess()
						? String.format("%.1f", r.getTotalImpulse()) : "-";
				case 6: return r.isSuccess()
						? String.format("%.1f", r.getMaxThrust()) : "-";
				case 7: return r.isSuccess()
						? String.format("%.2f", r.getTwr()) : "-";
				case 8: return r.isSuccess()
						? UnitGroup.UNITS_DISTANCE.getDefaultUnit()
								.toStringUnit(r.getApogee())
						: "-";
				case 9: return r.isSuccess()
						? String.format("%+.1f", r.getApogee() - target) : "-";
				case 10: return r.isSuccess()
						? String.format("%.3f", r.getLaunchMass()) : "-";
				case 11:
					if (!showBallast) return "-";
					return r.isSuccess()
							? String.format("%.3f", r.getBallastMass()) : "-";
				case 12:
					if (!showBallast) return "-";
					return r.isSuccess() && !Double.isNaN(r.getStabilityCaliber())
							? String.format("%.2f", r.getStabilityCaliber()) : "-";
				case 13: return getStatusText(status, r);
				default: return "";
			}
		}

		private String getStatusText(SweepStatus status, MotorSweepResult r) {
			switch (status) {
				case PASS: return "OK";
				case FILTERED_GLOM: return "Exceeds GLOM";
				case FILTERED_TWR: return "Below min TWR";
				case FILTERED_BOTH: return "Exceeds GLOM & below TWR";
				case OUT_OF_RANGE: return "Out of range";
				case INFEASIBLE: return r.getInfeasibleReason();
				case ERROR: return r.getErrorMessage();
				default: return "";
			}
		}

		SweepStatus getStatus(int row) {
			return displayStatuses.get(row);
		}

		MotorSweepResult getBestResult() {
			return bestResult;
		}
	}

	private class SweepCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			if (!isSelected) {
				int modelRow = table.convertRowIndexToModel(row);
				MotorSweepResult result = tableModel.displayResults.get(modelRow);
				if (result == tableModel.getBestResult()) {
					c.setBackground(BEST_COLOR);
					c.setForeground(Color.WHITE);
				} else {
					SweepStatus status = tableModel.getStatus(modelRow);
					switch (status) {
						case PASS:
							c.setBackground(PASS_COLOR);
							c.setForeground(Color.BLACK);
							break;
						case FILTERED_GLOM:
						case FILTERED_TWR:
						case FILTERED_BOTH:
							c.setBackground(FILTERED_COLOR);
							c.setForeground(Color.GRAY);
							break;
						case INFEASIBLE:
							c.setBackground(INFEASIBLE_COLOR);
							c.setForeground(Color.DARK_GRAY);
							break;
						case ERROR:
							c.setBackground(ERROR_COLOR);
							c.setForeground(Color.BLACK);
							break;
						default:
							c.setBackground(Color.WHITE);
							c.setForeground(Color.BLACK);
							break;
					}
				}
			}
			return c;
		}
	}
}
