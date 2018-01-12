/*This is a part of existing implementation of PCA used to reduce dimension for plotting the result.
 * Reference: https://github.com/CSE601-DataMining/Clustering/blob/master/src/edu/buffalo/cse/clustering/PCA.java
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ScatterAdd extends JFrame {

	private static final int N = 8;
	private static final String title = "Clustering";
	private static final Random rand = new Random();
	private XYSeries added = new XYSeries("Added");

	private ArrayList<double[][]> dataList;

	public ScatterAdd(String s, ArrayList<double[][]> dataList) {
		super(s);
		this.dataList = dataList;
		final ChartPanel chartPanel = createDemoPanel();
		this.add(chartPanel, BorderLayout.CENTER);
		JPanel control = new JPanel();
		control.add(new JButton(new AbstractAction("Add") {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < N; i++) {
					added.add(rand.nextGaussian(), rand.nextGaussian());
				}
			}
		}));
		this.add(control, BorderLayout.SOUTH);
	}

	private ChartPanel createDemoPanel() {
		JFreeChart jfreechart = ChartFactory.createScatterPlot(title, "X", "Y", createSampleData(),
				PlotOrientation.VERTICAL, true, true, false);
		XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
		xyPlot.setDomainCrosshairVisible(true);
		xyPlot.setRangeCrosshairVisible(true);
		XYItemRenderer renderer = xyPlot.getRenderer();
		// renderer.setSeriesPaint(0, Color.blue);
		NumberAxis domain = (NumberAxis) xyPlot.getDomainAxis();
		domain.setVerticalTickLabels(true);
		return new ChartPanel(jfreechart);
	}

	private XYDataset createSampleData() {
		XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
		int i = 0;
		for (double[][] data : dataList) {
			XYSeries series = new XYSeries("Cluster-" + i);
			for (int j = 0; j < data.length; j++) {
				double x = data[j][0];
				double y = data[j][1];
				series.add(x, y);
			}
			xySeriesCollection.addSeries(series);
			i++;
		}
		return xySeriesCollection;
	}

}