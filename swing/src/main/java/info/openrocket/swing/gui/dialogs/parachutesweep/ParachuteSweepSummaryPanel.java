package info.openrocket.swing.gui.dialogs.parachutesweep;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.parachutesweep.ParachuteSweepResult;
import info.openrocket.core.parachutesweep.ParachuteSweepSummary;

/**
 * Panel displaying aggregate parachute sweep statistics.
 */
public class ParachuteSweepSummaryPanel extends JPanel {

	private final JLabel bestResultLabel;
	private final JLabel passingCountLabel;
	private final JLabel massUsedLabel;
	private final JLabel airDensityLabel;

	public ParachuteSweepSummaryPanel() {
		super(new MigLayout("fill, ins 10", "[][grow]", ""));

		add(new JLabel("Best Result:"));
		bestResultLabel = new JLabel("-");
		add(bestResultLabel, "growx, wrap");

		add(new JLabel("Passing Count:"));
		passingCountLabel = new JLabel("-");
		add(passingCountLabel, "growx, wrap");

		add(new JLabel("Mass Used (kg):"));
		massUsedLabel = new JLabel("-");
		add(massUsedLabel, "growx, wrap");

		add(new JLabel("Air Density (kg/m3):"));
		airDensityLabel = new JLabel("-");
		add(airDensityLabel, "growx, wrap");
	}

	/**
	 * Update summary display with new results.
	 */
	public void updateSummary(ParachuteSweepSummary summary, double targetDescentRate) {
		if (summary == null) {
			bestResultLabel.setText("-");
			passingCountLabel.setText("-");
			massUsedLabel.setText("-");
			airDensityLabel.setText("-");
			return;
		}

		ParachuteSweepResult best = summary.getBestResult();
		if (best != null && best.isSuccess()) {
			double delta = best.getDescentRate() - targetDescentRate;
			bestResultLabel.setText(String.format("%s, %.0f mm, %.2f m/s (%+.2f from target)",
					best.getType().getDisplayName(),
					best.getDiameter() * 1000.0,
					best.getDescentRate(),
					delta));
			massUsedLabel.setText(String.format("%.3f", best.getMass()));
			airDensityLabel.setText(String.format("%.3f", best.getAirDensity()));
		} else {
			bestResultLabel.setText("None");
			massUsedLabel.setText("-");
			airDensityLabel.setText("-");
		}

		passingCountLabel.setText(String.format("%d of %d",
				summary.getPassingCount(), summary.getTotalCount()));
	}
}
