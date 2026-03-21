package info.openrocket.swing.gui.dialogs.parachutesweep;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.parachutesweep.ParachuteSweepResult;
import info.openrocket.core.parachutesweep.ParachuteSweepStatus;
import info.openrocket.core.parachutesweep.ParachuteSweepSummary;

/**
 * Sortable table displaying parachute sweep results with color coding.
 */
public class ParachuteSweepTablePanel extends JPanel {

	private static final Color PASS_COLOR = new Color(200, 255, 200);
	private static final Color TOO_FAST_COLOR = new Color(255, 200, 200);
	private static final Color TOO_SLOW_COLOR = new Color(255, 255, 200);
	private static final Color ERROR_COLOR = new Color(220, 220, 220);
	private static final Color BEST_COLOR = new Color(255, 140, 0);

	private static final String[] COLUMNS = {
		"Type", "Diameter (mm)", "CD", "Area (m2)",
		"Descent Rate (m/s)", "Delta (m/s)", "Mass (kg)", "Status"
	};

	private final SweepTableModel tableModel;
	private final JTable table;
	private final JToggleButton passOnlyToggle;
	private final JButton exportButton;

	public ParachuteSweepTablePanel() {
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
		exportButton.addActionListener(e -> exportCsv());

		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, "grow, push");
	}

	/**
	 * Update the table with new sweep results.
	 */
	public void setResults(List<ParachuteSweepResult> results,
			double targetDescentRate, double tolerance,
			ParachuteSweepResult bestResult) {
		tableModel.setResults(results, targetDescentRate, tolerance, bestResult);
		passOnlyToggle.setSelected(false);
		passOnlyToggle.setEnabled(true);
		exportButton.setEnabled(true);
	}

	static String csvEscape(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private void exportCsv() {
		String[] options = {"Export All Results", "Export Only Passing Results", "Cancel"};
		int choice = JOptionPane.showOptionDialog(this,
				"Which results do you want to export?",
				"Export CSV",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);

		if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
			return;
		}
		boolean passingOnly = (choice == 1);

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File("parachute-sweep-results.csv"));
		fileChooser.setFileFilter(new FileNameExtensionFilter(
				"CSV files (*.csv)", "csv"));
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = fileChooser.getSelectedFile();
		if (!file.getName().toLowerCase().endsWith(".csv")) {
			file = new File(file.getAbsolutePath() + ".csv");
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			for (int c = 0; c < COLUMNS.length; c++) {
				if (c > 0) writer.write(",");
				writer.write(csvEscape(COLUMNS[c]));
			}
			writer.newLine();

			List<ParachuteSweepResult> exportResults = tableModel.allResults;
			List<ParachuteSweepStatus> exportStatuses = tableModel.allStatuses;

			for (int i = 0; i < exportResults.size(); i++) {
				if (passingOnly && exportStatuses.get(i) != ParachuteSweepStatus.PASS) {
					continue;
				}
				for (int c = 0; c < COLUMNS.length; c++) {
					if (c > 0) writer.write(",");
					Object val = tableModel.getExportValueAt(i, c);
					writer.write(csvEscape(val != null ? val.toString() : ""));
				}
				writer.newLine();
			}
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this,
					"Failed to export CSV: " + ex.getMessage(),
					"Export Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private class SweepTableModel extends AbstractTableModel {
		private List<ParachuteSweepResult> displayResults = new ArrayList<>();
		private List<ParachuteSweepStatus> displayStatuses = new ArrayList<>();
		private List<ParachuteSweepResult> allResults = new ArrayList<>();
		private List<ParachuteSweepStatus> allStatuses = new ArrayList<>();
		private boolean passOnly;
		private double target;
		private ParachuteSweepResult bestResult;

		void applyFilter() {
			displayResults = new ArrayList<>();
			displayStatuses = new ArrayList<>();

			for (int i = 0; i < allResults.size(); i++) {
				ParachuteSweepStatus status = allStatuses.get(i);
				if (passOnly && status != ParachuteSweepStatus.PASS) {
					continue;
				}
				displayResults.add(allResults.get(i));
				displayStatuses.add(status);
			}
			fireTableDataChanged();
		}

		void setResults(List<ParachuteSweepResult> results, double targetRate,
				double tolerance, ParachuteSweepResult best) {
			this.target = targetRate;
			this.bestResult = best;
			this.passOnly = false;
			this.allResults = new ArrayList<>();
			this.allStatuses = new ArrayList<>();

			for (ParachuteSweepResult r : results) {
				ParachuteSweepStatus status = ParachuteSweepSummary.classifyResult(
						r, targetRate, tolerance);
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
			if (col >= 1 && col <= 6) return Number.class;
			return Object.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return computeValue(displayResults.get(row), displayStatuses.get(row), col);
		}

		Object getExportValueAt(int index, int col) {
			return computeValue(allResults.get(index), allStatuses.get(index), col);
		}

		private Object computeValue(ParachuteSweepResult r, ParachuteSweepStatus status, int col) {
			switch (col) {
				case 0: return r.getType().getDisplayName();
				case 1: return r.isSuccess()
						? String.format("%.0f", r.getDiameter() * 1000.0)
						: String.format("%.0f", r.getDiameter() * 1000.0);
				case 2: return r.isSuccess()
						? String.format("%.3f", r.getCd()) : "-";
				case 3: return r.isSuccess()
						? String.format("%.4f", r.getArea()) : "-";
				case 4: return r.isSuccess()
						? String.format("%.2f", r.getDescentRate()) : "-";
				case 5: return r.isSuccess()
						? String.format("%+.2f", r.getDescentRate() - target) : "-";
				case 6: return r.isSuccess()
						? String.format("%.3f", r.getMass()) : "-";
				case 7: return getStatusText(status, r);
				default: return "";
			}
		}

		private String getStatusText(ParachuteSweepStatus status, ParachuteSweepResult r) {
			switch (status) {
				case PASS: return "OK";
				case TOO_FAST: return "Too fast";
				case TOO_SLOW: return "Too slow";
				case ERROR: return r.getErrorMessage();
				default: return "";
			}
		}

		ParachuteSweepStatus getStatus(int row) {
			return displayStatuses.get(row);
		}

		ParachuteSweepResult getBestResult() {
			return bestResult;
		}
	}

	private class SweepCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable tbl, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(tbl, value,
					isSelected, hasFocus, row, column);
			if (!isSelected) {
				int modelRow = tbl.convertRowIndexToModel(row);
				ParachuteSweepResult result = tableModel.displayResults.get(modelRow);
				if (result == tableModel.getBestResult()) {
					c.setBackground(BEST_COLOR);
					c.setForeground(Color.WHITE);
				} else {
					ParachuteSweepStatus status = tableModel.getStatus(modelRow);
					switch (status) {
						case PASS:
							c.setBackground(PASS_COLOR);
							c.setForeground(Color.BLACK);
							break;
						case TOO_FAST:
							c.setBackground(TOO_FAST_COLOR);
							c.setForeground(Color.BLACK);
							break;
						case TOO_SLOW:
							c.setBackground(TOO_SLOW_COLOR);
							c.setForeground(Color.BLACK);
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
