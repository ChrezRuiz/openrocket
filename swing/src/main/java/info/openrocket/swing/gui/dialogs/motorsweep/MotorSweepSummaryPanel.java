package info.openrocket.swing.gui.dialogs.motorsweep;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepSummary;
import info.openrocket.core.unit.UnitGroup;

/**
 * Panel displaying aggregate sweep statistics.
 */
public class MotorSweepSummaryPanel extends JPanel {

	private final JLabel bestMotorLabel;
	private final JLabel passingCountLabel;
	private final JLabel avgImpulseLabel;
	private final JLabel avgTwrLabel;
	private final JLabel bestGlomLabel;
	private final JLabel bestBallastLabel;
	private final JLabel bestGlomRow;
	private final JLabel bestBallastRow;

	public MotorSweepSummaryPanel() {
		super(new MigLayout("fill, ins 10", "[][grow]", ""));

		add(new JLabel("Best Motor:"));
		bestMotorLabel = new JLabel("-");
		add(bestMotorLabel, "growx, wrap");

		add(new JLabel("Motors in Target Range:"));
		passingCountLabel = new JLabel("-");
		add(passingCountLabel, "growx, wrap");

		add(new JLabel("Avg Total Impulse (passing):"));
		avgImpulseLabel = new JLabel("-");
		add(avgImpulseLabel, "growx, wrap");

		add(new JLabel("Avg TWR (passing):"));
		avgTwrLabel = new JLabel("-");
		add(avgTwrLabel, "growx, wrap");

		bestGlomRow = new JLabel("Best Motor GLOM (kg):");
		add(bestGlomRow);
		bestGlomLabel = new JLabel("-");
		add(bestGlomLabel, "growx, wrap");

		bestBallastRow = new JLabel("Best Motor Ballast (kg):");
		add(bestBallastRow);
		bestBallastLabel = new JLabel("-");
		add(bestBallastLabel, "growx, wrap");
	}

	public void updateSummary(MotorSweepSummary summary, double targetApogee,
			boolean autoFillBallast) {
		if (summary == null) {
			bestMotorLabel.setText("-");
			passingCountLabel.setText("-");
			avgImpulseLabel.setText("-");
			avgTwrLabel.setText("-");
			bestGlomLabel.setText("-");
			bestBallastLabel.setText("-");
			return;
		}

		MotorSweepResult best = summary.getBestResult();
		if (best != null) {
			String apogeeStr = UnitGroup.UNITS_DISTANCE.getDefaultUnit()
					.toStringUnit(best.getApogee());
			double delta = best.getApogee() - targetApogee;
			bestMotorLabel.setText(String.format("%s %s — Apogee: %s (%+.1f m from target)",
					best.getMotor().getManufacturer().getDisplayName(),
					best.getMotor().getDesignation(),
					apogeeStr,
					delta));

			if (best.isSuccess()) {
				bestGlomLabel.setText(String.format("%.3f", best.getLaunchMass()));
			} else {
				bestGlomLabel.setText("-");
			}

			if (autoFillBallast && best.isSuccess()) {
				bestBallastLabel.setText(String.format("%.3f", best.getBallastMass()));
			} else {
				bestBallastLabel.setText("-");
			}
		} else {
			bestMotorLabel.setText("None");
			bestGlomLabel.setText("-");
			bestBallastLabel.setText("-");
		}

		// Show/hide ballast row
		bestBallastRow.setVisible(autoFillBallast);
		bestBallastLabel.setVisible(autoFillBallast);

		passingCountLabel.setText(String.format("%d of %d",
				summary.getPassingCount(), summary.getTotalCount()));
		avgImpulseLabel.setText(String.format("%.1f Ns", summary.getAvgImpulse()));
		avgTwrLabel.setText(String.format("%.2f", summary.getAvgTwr()));
	}
}
