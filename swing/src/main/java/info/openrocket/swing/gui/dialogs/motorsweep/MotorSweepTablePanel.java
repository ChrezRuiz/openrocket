package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.unit.UnitGroup;

/**
 * Sortable table displaying motor sweep results.
 */
public class MotorSweepTablePanel extends JPanel {

	private static final Color PASS_COLOR = new Color(200, 255, 200);
	private static final Color ERROR_COLOR = new Color(255, 200, 200);

	private static final String[] COLUMNS = {
		"Motor", "Manufacturer", "Diameter (mm)", "Length (mm)", "Class",
		"Total Impulse (Ns)", "Max Thrust (N)", "TWR", "Apogee", "Delta", "Status"
	};

	private final SweepTableModel tableModel;
	private final JTable table;
	private double targetApogee;

	public MotorSweepTablePanel() {
		super(new MigLayout("fill, ins 0"));
		tableModel = new SweepTableModel();
		table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setDefaultRenderer(Object.class, new SweepCellRenderer());
		table.setDefaultRenderer(Number.class, new SweepCellRenderer());
		table.setFillsViewportHeight(true);

		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, "grow, push");
	}

	public void setResults(List<MotorSweepResult> results, double targetApogee, double tolerance) {
		this.targetApogee = targetApogee;
		tableModel.setResults(results, targetApogee, tolerance);
	}

	private class SweepTableModel extends AbstractTableModel {
		private List<MotorSweepResult> results = new ArrayList<>();
		private double target;
		private double tol;

		void setResults(List<MotorSweepResult> results, double target, double tol) {
			this.results = new ArrayList<>(results);
			this.target = target;
			this.tol = tol;
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return results.size();
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
			if (col >= 2 && col <= 9) return Number.class;
			return Object.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			MotorSweepResult r = results.get(row);
			ThrustCurveMotor m = r.getMotor();

			switch (col) {
				case 0: return m.getDesignation();
				case 1: return m.getManufacturer().getDisplayName();
				case 2: return Math.round(m.getDiameter() * 1000.0);
				case 3: return Math.round(m.getLength() * 1000.0);
				case 4: return r.getImpulseClass();
				case 5: return r.isSuccess() ? String.format("%.1f", r.getTotalImpulse()) : "-";
				case 6: return r.isSuccess() ? String.format("%.1f", r.getMaxThrust()) : "-";
				case 7: return r.isSuccess() ? String.format("%.2f", r.getTwr()) : "-";
				case 8: return r.isSuccess()
						? UnitGroup.UNITS_DISTANCE.getDefaultUnit().toStringUnit(r.getApogee())
						: "-";
				case 9: return r.isSuccess()
						? String.format("%+.1f", r.getApogee() - target)
						: "-";
				case 10: return r.isSuccess() ? "OK" : r.getErrorMessage();
				default: return "";
			}
		}

		MotorSweepResult getResult(int row) {
			return results.get(row);
		}

		boolean isPassing(int row) {
			MotorSweepResult r = results.get(row);
			if (!r.isSuccess()) return false;
			double a = r.getApogee();
			return a >= target && a <= target + tol;
		}
	}

	private class SweepCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				int modelRow = table.convertRowIndexToModel(row);
				MotorSweepResult r = tableModel.getResult(modelRow);
				if (!r.isSuccess()) {
					c.setBackground(ERROR_COLOR);
				} else if (tableModel.isPassing(modelRow)) {
					c.setBackground(PASS_COLOR);
				} else {
					c.setBackground(Color.WHITE);
				}
			}
			return c;
		}
	}
}
