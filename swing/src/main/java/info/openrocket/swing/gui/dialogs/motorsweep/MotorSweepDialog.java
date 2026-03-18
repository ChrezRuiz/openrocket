package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.openrocket.core.database.motor.ThrustCurveMotorSetDatabase;
import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.motorsweep.MotorSweepFilter;
import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepRunner;
import info.openrocket.core.motorsweep.MotorSweepSummary;
import info.openrocket.core.motorsweep.SweepStatus;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.startup.Application;

/**
 * Main dialog for the Motor Sweep feature.
 */
public class MotorSweepDialog extends JDialog {
	private static final Logger log = LoggerFactory.getLogger(MotorSweepDialog.class);

	private final OpenRocketDocument document;
	private final Rocket rocket;

	private final MotorSweepInputPanel inputPanel;
	private final MotorSweepSummaryPanel summaryPanel;
	private final MotorSweepScatterPanel scatterPanel;
	private final MotorSweepTablePanel tablePanel;

	private final JButton runButton;
	private final JButton cancelButton;
	private final JProgressBar progressBar;
	private final JLabel statusLabel;
	private final JCheckBox showFilteredCheck;

	private MotorSweepWorker worker;

	private JButton applyButton;
	private List<MotorSweepResult> lastResults;
	private MotorSweepSummary lastSummary;
	private double lastTargetApogee;
	private double lastTolerance;
	private Double lastMaxGLOM;
	private Double lastMinTWR;
	private boolean lastAutoFillBallast;

	public MotorSweepDialog(OpenRocketDocument document, Window parent) {
		super(parent, "Motor Sweep", ModalityType.APPLICATION_MODAL);
		this.document = document;
		this.rocket = document.getRocket();

		JPanel mainPanel = new JPanel(new MigLayout("fill, ins 10", "[grow]", "[][][][grow]"));

		// Input panel
		inputPanel = new MotorSweepInputPanel();
		mainPanel.add(inputPanel, "growx, wrap");

		// Controls
		JPanel controlPanel = new JPanel(new MigLayout("ins 0", "[][][][][][grow][]", ""));
		runButton = new JButton("Run Sweep");
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		statusLabel = new JLabel(" ");

		applyButton = new JButton("Apply Motor...");
		applyButton.setEnabled(false);
		applyButton.addActionListener(e -> openApplyDialog());

		showFilteredCheck = new JCheckBox("Show filtered results", true);
		showFilteredCheck.setEnabled(false);
		showFilteredCheck.addActionListener(e -> refreshFilteredDisplay());

		controlPanel.add(runButton);
		controlPanel.add(cancelButton);
		controlPanel.add(applyButton);
		controlPanel.add(progressBar, "wmin 200lp, growx");
		controlPanel.add(statusLabel, "growx");
		controlPanel.add(showFilteredCheck);
		mainPanel.add(controlPanel, "growx, wrap");

		// Tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		summaryPanel = new MotorSweepSummaryPanel();
		scatterPanel = new MotorSweepScatterPanel();
		tablePanel = new MotorSweepTablePanel();

		tabbedPane.addTab("Summary", summaryPanel);
		tabbedPane.addTab("Scatter Plot", scatterPanel);
		tabbedPane.addTab("Results Table", tablePanel);
		mainPanel.add(tabbedPane, "grow, push, wrap");

		setContentPane(mainPanel);

		// Actions
		runButton.addActionListener(e -> startSweep());
		cancelButton.addActionListener(e -> cancelSweep());

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelSweep();
			}
		});

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(900, 700);
		setLocationRelativeTo(parent);
	}

	private void startSweep() {
		// Validate motor mount exists
		MotorMount mount = MotorSweepRunner.findMotorMount(rocket);
		if (mount == null) {
			JOptionPane.showMessageDialog(this,
					"No motor mount found in the rocket design.\nPlease add a motor mount before running a sweep.",
					"No Motor Mount", JOptionPane.WARNING_MESSAGE);
			return;
		}

		Set<Motor.Type> types = inputPanel.getAllowedTypes();
		if (types.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Please select at least one motor type.",
					"No Motor Type", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Get motor database and filter
		ThrustCurveMotorSetDatabase motorDB = Application.getThrustCurveMotorSetDatabase();
		MotorSweepFilter filter = new MotorSweepFilter(
				inputPanel.getMaxDiameter(),
				inputPanel.getMaxLength(),
				types);

		List<ThrustCurveMotor> motors = filter.filter(motorDB);
		if (motors.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"No motors match the specified constraints.\nTry increasing dimensions or selecting more types.",
					"No Motors Found", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Start sweep
		runButton.setEnabled(false);
		cancelButton.setEnabled(true);
		progressBar.setValue(0);
		progressBar.setMaximum(motors.size());
		statusLabel.setText("Simulating " + motors.size() + " motors...");

		double targetApogee = inputPanel.getTargetApogee();
		double tolerance = inputPanel.getTolerance();
		Double maxGLOM = inputPanel.getMaxGLOM();
		Double minTWR = inputPanel.getMinTWR();
		boolean autoFillBallast = inputPanel.isAutoFillBallast();
		double targetStabilityCaliber = inputPanel.getTargetStabilityCaliber();

		worker = new MotorSweepWorker(document, rocket, motors,
				new MotorSweepWorker.SweepCallback() {
					@Override
					public void onProgress(int completed, int total) {
						progressBar.setValue(completed);
						statusLabel.setText(String.format("Simulated %d of %d motors...",
								completed, total));
					}

					@Override
					public void onComplete(List<MotorSweepResult> results) {
						displayResults(results, targetApogee, tolerance, maxGLOM, minTWR,
								autoFillBallast);
						runButton.setEnabled(true);
						cancelButton.setEnabled(false);
						statusLabel.setText("Complete — " + results.size() + " motors simulated");
					}
				},
				maxGLOM, minTWR, autoFillBallast, targetStabilityCaliber);

		worker.execute();
	}

	private void cancelSweep() {
		if (worker != null) {
			worker.cancelSweep();
			worker = null;
		}
		runButton.setEnabled(true);
		cancelButton.setEnabled(false);
		statusLabel.setText("Cancelled");
	}

	private void displayResults(List<MotorSweepResult> results, double targetApogee,
			double tolerance, Double maxGLOM, Double minTWR, boolean autoFillBallast) {
		MotorSweepSummary summary = MotorSweepSummary.compute(results, targetApogee,
				tolerance, maxGLOM, minTWR);

		lastResults = results;
		lastSummary = summary;
		lastTargetApogee = targetApogee;
		lastTolerance = tolerance;
		lastMaxGLOM = maxGLOM;
		lastMinTWR = minTWR;
		lastAutoFillBallast = autoFillBallast;

		boolean showFiltered = showFilteredCheck.isSelected();
		showFilteredCheck.setEnabled(true);

		summaryPanel.updateSummary(summary, targetApogee, autoFillBallast);
		scatterPanel.updateChart(results, targetApogee, tolerance, summary,
				maxGLOM, minTWR, showFiltered);
		tablePanel.setResults(results, targetApogee, tolerance, maxGLOM, minTWR,
				autoFillBallast, showFiltered, summary.getBestResult());

		applyButton.setEnabled(summary.getPassingCount() > 0);
	}

	private void refreshFilteredDisplay() {
		if (lastResults == null) {
			return;
		}
		boolean showFiltered = showFilteredCheck.isSelected();
		scatterPanel.updateChart(lastResults, lastTargetApogee, lastTolerance,
				lastSummary, lastMaxGLOM, lastMinTWR, showFiltered);
		tablePanel.setResults(lastResults, lastTargetApogee, lastTolerance,
				lastMaxGLOM, lastMinTWR, lastAutoFillBallast, showFiltered,
				lastSummary != null ? lastSummary.getBestResult() : null);
	}

	private void openApplyDialog() {
		if (lastResults == null || lastSummary == null) {
			return;
		}

		// Filter to fully passing results
		List<MotorSweepResult> passingResults = new ArrayList<>();
		for (MotorSweepResult r : lastResults) {
			SweepStatus status = MotorSweepSummary.classifyResult(r, lastTargetApogee,
					lastTolerance, lastMaxGLOM, lastMinTWR);
			if (status == SweepStatus.PASS) {
				passingResults.add(r);
			}
		}

		if (passingResults.isEmpty()) {
			return;
		}

		ApplySweepResultDialog dialog = new ApplySweepResultDialog(
				this, rocket, passingResults, lastSummary.getBestResult());
		dialog.setVisible(true);
	}
}
