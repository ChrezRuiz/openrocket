package info.openrocket.swing.gui.dialogs.parachutesweep;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.parachutesweep.ParachuteType;
import info.openrocket.core.parachutesweep.ParachuteSweepCalculator;
import info.openrocket.core.parachutesweep.ParachuteSweepConfig;
import info.openrocket.core.parachutesweep.ParachuteSweepMassResolver;
import info.openrocket.core.parachutesweep.ParachuteSweepResult;
import info.openrocket.core.parachutesweep.ParachuteSweepStatus;
import info.openrocket.core.parachutesweep.ParachuteSweepSummary;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.Rocket;

/**
 * Main dialog for the Parachute Sweep feature.
 */
public class ParachuteSweepDialog extends JDialog {
	private static final Logger log = LoggerFactory.getLogger(ParachuteSweepDialog.class);

	private final OpenRocketDocument document;
	private final Rocket rocket;

	private final ParachuteSweepInputPanel inputPanel;
	private final ParachuteSweepSummaryPanel summaryPanel;
	private final ParachuteSweepScatterPanel scatterPanel;
	private final ParachuteSweepTablePanel tablePanel;

	private final JButton runButton;
	private final JButton cancelButton;
	private final JButton applyButton;
	private final JLabel statusLabel;

	private ParachuteSweepWorker worker;

	private List<ParachuteSweepResult> lastResults;
	private ParachuteSweepSummary lastSummary;
	private double lastTargetDescentRate;
	private double lastTolerance;

	public ParachuteSweepDialog(OpenRocketDocument document, Window parent) {
		super(parent, "Parachute Sweep", ModalityType.APPLICATION_MODAL);
		this.document = document;
		this.rocket = document.getRocket();

		JPanel mainPanel = new JPanel(new MigLayout("fill, ins 10", "[grow]", "[][][][grow]"));

		// Input panel
		inputPanel = new ParachuteSweepInputPanel();
		mainPanel.add(inputPanel, "growx, wrap");

		// Controls
		JPanel controlPanel = new JPanel(new MigLayout("ins 0", "[][][][grow][]", ""));
		runButton = new JButton("Run Sweep");
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		statusLabel = new JLabel(" ");

		applyButton = new JButton("Apply Parachute...");
		applyButton.setEnabled(false);
		applyButton.addActionListener(e -> openApplyDialog());

		controlPanel.add(runButton);
		controlPanel.add(cancelButton);
		controlPanel.add(applyButton);
		controlPanel.add(statusLabel, "growx");
		mainPanel.add(controlPanel, "growx, wrap");

		// Tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		summaryPanel = new ParachuteSweepSummaryPanel();
		scatterPanel = new ParachuteSweepScatterPanel();
		tablePanel = new ParachuteSweepTablePanel();

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
		Set<ParachuteType> types = inputPanel.getSelectedTypes();
		if (types.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Please select at least one parachute type.",
					"No Parachute Type", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Validate diameter range
		if (inputPanel.getMinDiameter() >= inputPanel.getMaxDiameter()) {
			JOptionPane.showMessageDialog(this,
					"Minimum diameter must be less than maximum diameter.",
					"Invalid Diameter Range", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Resolve mass
		double mass;
		ParachuteSweepMassResolver.MassMode massMode = inputPanel.getMassMode();
		try {
			if (massMode == ParachuteSweepMassResolver.MassMode.BURNOUT) {
				FlightConfiguration fc = rocket.getSelectedConfiguration();
				mass = ParachuteSweepMassResolver.resolveMass(massMode, fc, null);
			} else {
				mass = ParachuteSweepMassResolver.resolveMass(massMode, null,
						inputPanel.getManualMass());
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
					"Failed to resolve mass: " + ex.getMessage(),
					"Mass Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		double targetDescentRate = inputPanel.getTargetDescentRate();
		double tolerance = inputPanel.getDescentRateTolerance();

		ParachuteSweepConfig config;
		try {
			config = new ParachuteSweepConfig(
					targetDescentRate, tolerance,
					inputPanel.getMinDiameter(), inputPanel.getMaxDiameter(),
					inputPanel.getDiameterStep(),
					types, mass, inputPanel.getAirDensity(),
					inputPanel.getCustomCd());
		} catch (IllegalArgumentException ex) {
			JOptionPane.showMessageDialog(this,
					"Invalid configuration: " + ex.getMessage(),
					"Configuration Error", JOptionPane.WARNING_MESSAGE);
			return;
		}

		runButton.setEnabled(false);
		cancelButton.setEnabled(true);
		statusLabel.setText("Running sweep...");

		worker = new ParachuteSweepWorker(config,
				new ParachuteSweepWorker.SweepCallback() {
					@Override
					public void onComplete(List<ParachuteSweepResult> results) {
						displayResults(results, targetDescentRate, tolerance);
						runButton.setEnabled(true);
						cancelButton.setEnabled(false);
						statusLabel.setText("Complete -- " + results.size() + " combinations evaluated");
					}

					@Override
					public void onError(String errorMessage) {
						runButton.setEnabled(true);
						cancelButton.setEnabled(false);
						statusLabel.setText("Error: " + errorMessage);
						log.error("Parachute sweep error: {}", errorMessage);
					}
				});

		worker.execute();
	}

	private void cancelSweep() {
		if (worker != null) {
			worker.cancel(true);
			worker = null;
		}
		runButton.setEnabled(true);
		cancelButton.setEnabled(false);
		statusLabel.setText("Cancelled");
	}

	private void displayResults(List<ParachuteSweepResult> results,
			double targetDescentRate, double tolerance) {
		ParachuteSweepSummary summary = ParachuteSweepSummary.compute(
				results, targetDescentRate, tolerance);

		lastResults = results;
		lastSummary = summary;
		lastTargetDescentRate = targetDescentRate;
		lastTolerance = tolerance;

		summaryPanel.updateSummary(summary, targetDescentRate);
		scatterPanel.updateChart(results, targetDescentRate, tolerance, summary);
		tablePanel.setResults(results, targetDescentRate, tolerance,
				summary.getBestResult());

		applyButton.setEnabled(summary.getPassingCount() > 0);
	}

	private void openApplyDialog() {
		if (lastResults == null || lastSummary == null) {
			return;
		}

		List<ParachuteSweepResult> passingResults = new ArrayList<>();
		for (ParachuteSweepResult r : lastResults) {
			ParachuteSweepStatus status = ParachuteSweepSummary.classifyResult(
					r, lastTargetDescentRate, lastTolerance);
			if (status == ParachuteSweepStatus.PASS) {
				passingResults.add(r);
			}
		}

		if (passingResults.isEmpty()) {
			return;
		}

		ApplyParachuteSweepDialog dialog = new ApplyParachuteSweepDialog(
				this, rocket, passingResults, lastSummary.getBestResult());
		dialog.setVisible(true);
	}
}
