package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.simulation.FlightDataBranch;
import info.openrocket.core.simulation.FlightDataType;
import info.openrocket.core.simulation.FlightEvent;

/**
 * Chart panel showing time-series flight profile data for selected motors.
 * Supports overlaying up to 3 motor profiles for comparison.
 */
public class MotorSweepProfilePanel extends JPanel {

	private static final FlightDataType[] PROFILE_TYPES = {
		FlightDataType.TYPE_ALTITUDE,
		FlightDataType.TYPE_VELOCITY_TOTAL,
		FlightDataType.TYPE_VELOCITY_Z,
		FlightDataType.TYPE_ACCELERATION_TOTAL,
		FlightDataType.TYPE_STABILITY,
		FlightDataType.TYPE_THRUST_FORCE,
		FlightDataType.TYPE_DRAG_FORCE,
		FlightDataType.TYPE_MASS
	};

	private static final String[] PROFILE_LABELS = {
		"Altitude (m)",
		"Total Velocity (m/s)",
		"Vertical Velocity (m/s)",
		"Acceleration (m/s\u00B2)",
		"Stability (cal)",
		"Thrust (N)",
		"Drag (N)",
		"Mass (kg)"
	};

	private static final Color[] SERIES_COLORS = {
		new Color(0, 102, 204),
		new Color(204, 0, 0),
		new Color(0, 153, 51)
	};

	private static final int MAX_OVERLAYS = 3;
	private static final String CARD_EMPTY = "empty";
	private static final String CARD_CHART = "chart";

	private final JComboBox<String> typeSelector;
	private final JButton clearButton;
	private final ChartPanel chartPanel;
	private final JPanel cardPanel;
	private final CardLayout cardLayout;
	private final JLabel statusLabel;

	private final LinkedList<MotorSweepResult> overlayResults = new LinkedList<>();
	private FlightDataType selectedType = PROFILE_TYPES[0];

	public MotorSweepProfilePanel() {
		super(new MigLayout("fill, ins 5"));

		JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 5", "[][]push", ""));
		typeSelector = new JComboBox<>(PROFILE_LABELS);
		typeSelector.addActionListener(e -> {
			int idx = typeSelector.getSelectedIndex();
			if (idx >= 0) {
				selectedType = PROFILE_TYPES[idx];
				rebuildChart();
			}
		});
		clearButton = new JButton("Clear");
		clearButton.addActionListener(e -> clearOverlays());
		toolbar.add(typeSelector);
		toolbar.add(clearButton);
		add(toolbar, "growx, wrap");

		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		JLabel emptyLabel = new JLabel(
				"<html><center>Click a row to view flight profile"
				+ "<br><small>Ctrl+click to compare up to 3 motors</small>"
				+ "</center></html>");
		emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
		emptyLabel.setForeground(Color.GRAY);
		cardPanel.add(emptyLabel, CARD_EMPTY);

		chartPanel = new ChartPanel(createEmptyChart());
		chartPanel.setMouseWheelEnabled(true);
		cardPanel.add(chartPanel, CARD_CHART);

		cardLayout.show(cardPanel, CARD_EMPTY);
		add(cardPanel, "grow, push, wrap");

		statusLabel = new JLabel(" ");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
		statusLabel.setForeground(Color.GRAY);
		add(statusLabel, "growx");
	}

	/**
	 * Show a single motor profile, replacing any existing overlays.
	 */
	public void setResult(MotorSweepResult result) {
		overlayResults.clear();
		overlayResults.add(result);
		rebuildChart();
	}

	/**
	 * Add a motor to the overlay. Duplicates are ignored.
	 * If already at max overlays, the oldest is removed.
	 */
	public void addOverlay(MotorSweepResult result) {
		if (overlayResults.contains(result)) {
			return;
		}
		if (overlayResults.size() >= MAX_OVERLAYS) {
			overlayResults.removeFirst();
		}
		overlayResults.add(result);
		rebuildChart();
	}

	/**
	 * Remove all overlaid profiles and show the empty state.
	 */
	public void clearOverlays() {
		overlayResults.clear();
		cardLayout.show(cardPanel, CARD_EMPTY);
		updateStatus();
	}

	private void updateStatus() {
		int count = overlayResults.size();
		switch (count) {
			case 0:
				statusLabel.setText(" ");
				break;
			case 1:
				statusLabel.setText(
						"Showing 1 motor \u00b7 Ctrl+click another row to compare");
				break;
			case 2:
				statusLabel.setText(
						"Showing 2/3 \u00b7 Ctrl+click to add another");
				break;
			default:
				statusLabel.setText(
						"Showing 3/3 \u00b7 Click a row to replace, or Clear");
				break;
		}
	}

	private void rebuildChart() {
		if (overlayResults.isEmpty()) {
			cardLayout.show(cardPanel, CARD_EMPTY);
			return;
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		boolean anyData = false;

		for (MotorSweepResult result : overlayResults) {
			XYSeries series = buildSeries(result);
			dataset.addSeries(series);
			if (series.getItemCount() > 0) {
				anyData = true;
			}
		}

		if (!anyData) {
			cardLayout.show(cardPanel, CARD_EMPTY);
			return;
		}

		String label = PROFILE_LABELS[typeSelector.getSelectedIndex()];
		JFreeChart chart = ChartFactory.createXYLineChart(
				null,
				"Time (s)",
				label,
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
		for (int i = 0; i < overlayResults.size(); i++) {
			Color color = SERIES_COLORS[i % SERIES_COLORS.length];
			renderer.setSeriesPaint(i, color);
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}
		plot.setRenderer(renderer);

		for (int i = 0; i < overlayResults.size(); i++) {
			addEventMarkers(plot, overlayResults.get(i), i);
		}

		chartPanel.setChart(chart);
		cardLayout.show(cardPanel, CARD_CHART);
		updateStatus();
	}

	private XYSeries buildSeries(MotorSweepResult result) {
		String name = result.getMotor().getDesignation();
		XYSeries series = new XYSeries(name);

		if (!result.hasFlightData()) {
			return series;
		}

		FlightDataBranch branch = result.getFlightDataBranch();
		List<Double> timeData = branch.get(FlightDataType.TYPE_TIME);
		List<Double> valueData = branch.get(selectedType);

		if (timeData == null || valueData == null) {
			return series;
		}

		int count = Math.min(timeData.size(), valueData.size());
		for (int i = 0; i < count; i++) {
			Double t = timeData.get(i);
			Double v = valueData.get(i);
			if (t != null && v != null && !t.isNaN() && !v.isNaN()) {
				series.add(t.doubleValue(), v.doubleValue());
			}
		}
		return series;
	}

	private void addEventMarkers(XYPlot plot, MotorSweepResult result, int seriesIndex) {
		if (!result.hasFlightData()) {
			return;
		}

		FlightDataBranch branch = result.getFlightDataBranch();
		List<FlightEvent> events = branch.getEvents();
		if (events == null) {
			return;
		}

		Color color = SERIES_COLORS[seriesIndex % SERIES_COLORS.length];
		Color markerColor = new Color(color.getRed(), color.getGreen(),
				color.getBlue(), 128);
		BasicStroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f);
		Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

		for (FlightEvent event : events) {
			FlightEvent.Type type = event.getType();
			if (type == FlightEvent.Type.BURNOUT
					|| type == FlightEvent.Type.APOGEE
					|| type == FlightEvent.Type.RECOVERY_DEVICE_DEPLOYMENT) {
				ValueMarker marker = new ValueMarker(event.getTime());
				marker.setPaint(markerColor);
				marker.setStroke(dashed);
				marker.setLabel(type.toString());
				marker.setLabelFont(labelFont);
				marker.setLabelPaint(markerColor);
				plot.addDomainMarker(marker);
			}
		}
	}

	private JFreeChart createEmptyChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		return ChartFactory.createXYLineChart(
				null,
				"Time (s)",
				"Altitude (m)",
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);
	}
}
