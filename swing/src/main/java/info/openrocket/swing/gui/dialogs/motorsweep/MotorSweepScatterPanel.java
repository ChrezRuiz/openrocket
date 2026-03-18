package info.openrocket.swing.gui.dialogs.motorsweep;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

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

import info.openrocket.core.motorsweep.MotorSweepResult;
import info.openrocket.core.motorsweep.MotorSweepSummary;
import info.openrocket.core.motorsweep.SweepStatus;

/**
 * Scatter plot of Total Impulse vs. Apogee with target band.
 */
public class MotorSweepScatterPanel extends JPanel {

	private final ChartPanel chartPanel;
	private JFreeChart chart;

	public MotorSweepScatterPanel() {
		super(new MigLayout("fill, ins 0"));
		chart = createEmptyChart();
		chartPanel = new ChartPanel(chart);
		chartPanel.setMouseWheelEnabled(true);
		add(chartPanel, "grow, push");
	}

	private JFreeChart createEmptyChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		return ChartFactory.createScatterPlot(
				"Motor Sweep Results",
				"Total Impulse (Ns)",
				"Apogee (m)",
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);
	}

	public void updateChart(List<MotorSweepResult> results, double targetApogee,
			double tolerance, MotorSweepSummary summary,
			Double maxGLOM, Double minTWR, boolean showFiltered) {

		XYSeries passingSeries = new XYSeries("Passing");
		XYSeries outsideRangeSeries = new XYSeries("Outside Range");
		XYSeries filteredSeries = new XYSeries("Filtered");
		XYSeries infeasibleSeries = new XYSeries("Infeasible");
		XYSeries bestSeries = new XYSeries("Best");

		MotorSweepResult best = summary != null ? summary.getBestResult() : null;

		for (MotorSweepResult r : results) {
			SweepStatus status = MotorSweepSummary.classifyResult(r, targetApogee,
					tolerance, maxGLOM, minTWR);

			if (status == SweepStatus.ERROR) {
				continue;
			}

			if (status == SweepStatus.INFEASIBLE) {
				if (showFiltered) {
					// Use totalImpulse as x, 0 as y since we have no apogee
					infeasibleSeries.add(r.getTotalImpulse(), 0);
				}
				continue;
			}

			double impulse = r.getTotalImpulse();
			double apogee = r.getApogee();

			if (r == best) {
				bestSeries.add(impulse, apogee);
			} else if (status == SweepStatus.PASS) {
				passingSeries.add(impulse, apogee);
			} else if (status == SweepStatus.OUT_OF_RANGE) {
				outsideRangeSeries.add(impulse, apogee);
			} else if (showFiltered) {
				// FILTERED_GLOM, FILTERED_TWR, FILTERED_BOTH
				filteredSeries.add(impulse, apogee);
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(passingSeries);
		dataset.addSeries(outsideRangeSeries);
		dataset.addSeries(bestSeries);
		if (showFiltered) {
			dataset.addSeries(filteredSeries);
			dataset.addSeries(infeasibleSeries);
		}

		chart = ChartFactory.createScatterPlot(
				"Motor Sweep Results",
				"Total Impulse (Ns)",
				"Apogee (m)",
				dataset,
				PlotOrientation.VERTICAL,
				true, true, false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		// Target band
		IntervalMarker targetBand = new IntervalMarker(targetApogee, targetApogee + tolerance);
		targetBand.setPaint(new Color(0, 200, 0, 40));
		targetBand.setOutlinePaint(new Color(0, 150, 0, 80));
		targetBand.setOutlineStroke(new BasicStroke(1.0f));
		plot.addRangeMarker(targetBand, Layer.BACKGROUND);

		// Series rendering
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
		Shape dot = new Ellipse2D.Double(-4, -4, 8, 8);
		Shape bigDot = new Ellipse2D.Double(-6, -6, 12, 12);
		Shape xMark = createXShape(5);

		// 0: Passing = green
		renderer.setSeriesPaint(0, new Color(0, 180, 0));
		renderer.setSeriesShape(0, dot);

		// 1: Outside range = gray
		renderer.setSeriesPaint(1, Color.GRAY);
		renderer.setSeriesShape(1, dot);

		// 2: Best = orange, larger
		renderer.setSeriesPaint(2, new Color(255, 140, 0));
		renderer.setSeriesShape(2, bigDot);

		if (showFiltered) {
			// 3: Filtered = light gray, semi-transparent
			renderer.setSeriesPaint(3, new Color(180, 180, 180, 120));
			renderer.setSeriesShape(3, dot);

			// 4: Infeasible = red X
			renderer.setSeriesPaint(4, new Color(200, 0, 0));
			renderer.setSeriesShape(4, xMark);
		}

		plot.setRenderer(renderer);

		chartPanel.setChart(chart);
	}

	private static Shape createXShape(int size) {
		java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
		path.append(new Line2D.Double(-size, -size, size, size), false);
		path.append(new Line2D.Double(-size, size, size, -size), false);
		return path;
	}
}
