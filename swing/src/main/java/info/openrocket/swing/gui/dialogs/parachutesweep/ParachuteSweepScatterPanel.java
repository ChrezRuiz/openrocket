package info.openrocket.swing.gui.dialogs.parachutesweep;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import info.openrocket.core.parachutesweep.ParachuteType;
import info.openrocket.core.parachutesweep.ParachuteSweepResult;
import info.openrocket.core.parachutesweep.ParachuteSweepSummary;

/**
 * Scatter plot of Diameter vs. Descent Rate, colored by parachute type.
 */
public class ParachuteSweepScatterPanel extends JPanel {

	private static final Map<ParachuteType, Color> TYPE_COLORS = new LinkedHashMap<>();

	static {
		TYPE_COLORS.put(ParachuteType.FLAT_CIRCULAR, new Color(0, 120, 215));
		TYPE_COLORS.put(ParachuteType.HEMISPHERICAL, new Color(0, 180, 0));
		TYPE_COLORS.put(ParachuteType.CONICAL, new Color(200, 0, 0));
		TYPE_COLORS.put(ParachuteType.CRUCIFORM, new Color(180, 0, 180));
		TYPE_COLORS.put(ParachuteType.TOROIDAL, new Color(255, 140, 0));
		TYPE_COLORS.put(ParachuteType.RIBBON, new Color(0, 180, 180));
		TYPE_COLORS.put(ParachuteType.ANNULAR, new Color(140, 100, 0));
		TYPE_COLORS.put(ParachuteType.CUSTOM, new Color(100, 100, 100));
	}

	private final ChartPanel chartPanel;
	private JFreeChart chart;

	public ParachuteSweepScatterPanel() {
		super(new MigLayout("fill, ins 0"));
		chart = createEmptyChart();
		chartPanel = new ChartPanel(chart);
		chartPanel.setMouseWheelEnabled(true);
		add(chartPanel, "grow, push");
	}

	private JFreeChart createEmptyChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		return ChartFactory.createScatterPlot(
				"Parachute Sweep Results",
				"Diameter (mm)",
				"Descent Rate (m/s)",
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);
	}

	/**
	 * Update the chart with new sweep results.
	 */
	public void updateChart(List<ParachuteSweepResult> results,
			double targetDescentRate, double tolerance,
			ParachuteSweepSummary summary) {

		ParachuteSweepResult best = summary != null ? summary.getBestResult() : null;

		// Create one series per type + one for best
		Map<ParachuteType, XYSeries> typeSeries = new LinkedHashMap<>();
		for (ParachuteSweepResult r : results) {
			if (!r.isSuccess()) {
				continue;
			}
			ParachuteType type = r.getType();
			if (!typeSeries.containsKey(type)) {
				typeSeries.put(type, new XYSeries(type.getDisplayName()));
			}
		}

		XYSeries bestSeries = new XYSeries("Best");

		for (ParachuteSweepResult r : results) {
			if (!r.isSuccess()) {
				continue;
			}
			double diameterMm = r.getDiameter() * 1000.0;
			double rate = r.getDescentRate();

			if (r == best) {
				bestSeries.add(diameterMm, rate);
			} else {
				XYSeries series = typeSeries.get(r.getType());
				if (series != null) {
					series.add(diameterMm, rate);
				}
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		List<ParachuteType> orderedTypes = new java.util.ArrayList<>(typeSeries.keySet());
		for (ParachuteType type : orderedTypes) {
			dataset.addSeries(typeSeries.get(type));
		}
		dataset.addSeries(bestSeries);

		chart = ChartFactory.createScatterPlot(
				"Parachute Sweep Results",
				"Diameter (mm)",
				"Descent Rate (m/s)",
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// Target band
		double lower = targetDescentRate - tolerance;
		double upper = targetDescentRate + tolerance;
		IntervalMarker targetBand = new IntervalMarker(lower, upper);
		targetBand.setPaint(new Color(0, 200, 0, 40));
		targetBand.setOutlinePaint(new Color(0, 150, 0, 80));
		targetBand.setOutlineStroke(new BasicStroke(1.0f));
		plot.addRangeMarker(targetBand, Layer.BACKGROUND);

		// Renderer
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
		Shape dot = new Ellipse2D.Double(-4, -4, 8, 8);
		Shape bigDot = new Ellipse2D.Double(-6, -6, 12, 12);

		int seriesIdx = 0;
		for (ParachuteType type : orderedTypes) {
			Color color = TYPE_COLORS.getOrDefault(type, Color.GRAY);
			renderer.setSeriesPaint(seriesIdx, color);
			renderer.setSeriesShape(seriesIdx, dot);
			seriesIdx++;
		}

		// Best series is last
		renderer.setSeriesPaint(seriesIdx, new Color(255, 140, 0));
		renderer.setSeriesShape(seriesIdx, bigDot);

		plot.setRenderer(renderer);
		chartPanel.setChart(chart);
	}
}
