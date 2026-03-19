package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

/**
 * Modal confirmation dialog shown after a motor is applied, asking whether
 * to also apply the computed sweep ballast as a MassComponent.
 */
public class ApplyBallastConfirmDialog extends JDialog {

	/**
	 * The action chosen by the user.
	 */
	public enum BallastAction {
		/** Add a new ballast component. */
		APPLY_NEW,
		/** Replace the existing "Sweep Ballast" component. */
		REPLACE_EXISTING,
		/** Do not apply ballast. */
		CANCEL
	}

	private BallastAction result = BallastAction.CANCEL;

	private final JRadioButton replaceRadio;
	private final JRadioButton addNewRadio;

	/**
	 * Create the ballast confirmation dialog.
	 *
	 * @param parent           owner window
	 * @param ballastMass      mass in kg
	 * @param ballastPosition  absolute position from nose in metres
	 * @param bodyTubeName     name of the target body tube
	 * @param stabilityCaliber resulting stability in calibers
	 * @param duplicateExists  true if an existing "Sweep Ballast" was found
	 */
	public ApplyBallastConfirmDialog(Window parent, double ballastMass,
			double ballastPosition, String bodyTubeName,
			double stabilityCaliber, boolean duplicateExists) {
		super(parent, "Apply Sweep Ballast",
				ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel main = new JPanel(
				new MigLayout("fill, ins 10", "[right][grow]"));

		// Info labels
		main.add(new JLabel("Ballast mass:"));
		main.add(new JLabel(String.format("%.1f g", ballastMass * 1000.0)),
				"wrap");

		main.add(new JLabel("Position from nose:"));
		main.add(new JLabel(
				String.format("%.1f mm", ballastPosition * 1000.0)),
				"wrap");

		main.add(new JLabel("Body tube:"));
		main.add(new JLabel(bodyTubeName), "wrap");

		main.add(new JLabel("Stability:"));
		main.add(new JLabel(
				String.format("%.2f cal", stabilityCaliber)), "wrap");

		// Duplicate warning
		replaceRadio = new JRadioButton("Replace existing ballast", true);
		addNewRadio = new JRadioButton("Add new ballast component");

		if (duplicateExists) {
			JPanel warnPanel = new JPanel(
					new MigLayout("ins 5", "[grow]"));
			warnPanel.setBorder(BorderFactory.createTitledBorder(
					"Existing Sweep Ballast Found"));

			ButtonGroup group = new ButtonGroup();
			group.add(replaceRadio);
			group.add(addNewRadio);

			warnPanel.add(replaceRadio, "wrap");
			warnPanel.add(addNewRadio, "wrap");

			main.add(warnPanel, "span 2, growx, gaptop 10, wrap");
		}

		// Buttons
		JPanel buttonPanel = new JPanel(
				new MigLayout("ins 0, right"));

		JButton applyButton = new JButton("Apply Ballast");
		applyButton.addActionListener(e -> {
			if (duplicateExists && replaceRadio.isSelected()) {
				result = BallastAction.REPLACE_EXISTING;
			} else {
				result = BallastAction.APPLY_NEW;
			}
			dispose();
		});

		JButton skipButton = new JButton("Skip");
		skipButton.addActionListener(e -> {
			result = BallastAction.CANCEL;
			dispose();
		});

		buttonPanel.add(applyButton);
		buttonPanel.add(skipButton);

		main.add(buttonPanel, "span 2, right, gaptop 10");

		setContentPane(main);
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Return the action chosen by the user.
	 */
	public BallastAction getResult() {
		return result;
	}
}
