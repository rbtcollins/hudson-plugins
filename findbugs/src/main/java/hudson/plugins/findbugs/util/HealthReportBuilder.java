package hudson.plugins.findbugs.util;

import hudson.Util;
import hudson.model.HealthReport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;

/**
 * Creates a health report for integer values based on healthy and unhealthy
 * thresholds.
 *
 * @see HealthReport
 * @author Ulli Hafner
 */
public class HealthReportBuilder implements Serializable {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = 5191317904662711835L;
    /** Report health as 100% when the number of warnings is less than this value. */
    private int healthy;
    /** Report health as 0% when the number of warnings is greater than this value. */
    private int unHealthy;
    /** Determines whether to use the provided healthy thresholds. */
    private boolean isHealthEnabled;
    /** Name of the report. */
    private String reportName;
    /** Name of a item. */
    private String itemName;
    /** Determines whether to use the provided unstable threshold. */
    private boolean isThresholdEnabled;
    /** Bug threshold to be reached if a build should be considered as unstable. */
    private int threshold;

    /**
     * Creates a new instance of <code>HealthReportBuilder</code>.
     *
     * @param reportName
     *            the report name
     * @param itemName
     *            the item name
     * @param isFailureThresholdEnabled
     *            determines whether to use the provided unstable threshold
     * @param threshold
     *            bug threshold to be reached if a build should be considered as
     *            unstable.
     * @param isHealthyReportEnabled
     *            determines whether to use the provided healthy thresholds.
     * @param healthy
     *            report health as 100% when the number of warnings is less than
     *            this value
     * @param unHealthy
     *            report health as 0% when the number of warnings is greater
     *            than this value
     */
    public HealthReportBuilder(final String reportName, final String itemName, final boolean isFailureThresholdEnabled, final int threshold, final boolean isHealthyReportEnabled, final int healthy, final int unHealthy) {
        this.reportName = reportName;
        this.itemName = itemName;
        this.threshold = threshold;
        this.healthy = healthy;
        this.unHealthy = unHealthy;
        isThresholdEnabled = isFailureThresholdEnabled;
        isHealthEnabled = isHealthyReportEnabled;
    }

    /**
     * Creates a new dummy instance of <code>HealthReportBuilder</code>.
     */
    public HealthReportBuilder() {
        this("Items", "item", false, 0, false, 0, 0);
    }

    /**
     * Computes the healthiness of a build based on the specified counter.
     * Reports a health of 100% when the specified counter is less than
     * {@link #healthy}. Reports a health of 0% when the specified counter is
     * greater than {@link #unHealthy}.
     *
     * @param counter
     *            the number of items in a build
     * @return the healthiness of a build
     */
    public HealthReport computeHealth(final int counter) {
        if (isHealthEnabled) {
            int percentage;
            if (counter < healthy) {
                percentage = 100;
            }
            else if (counter > unHealthy) {
                percentage = 0;
            }
            else {
                percentage = 100 - ((counter - healthy) * 100 / (unHealthy - healthy));
            }
            return new HealthReport(percentage,
                    reportName + ": " + Util.combine(counter, itemName) + " found.");
        }
        return null;
    }

    /**
     * Returns whether this health report build is enabled, i.e. at least one of
     * the health or failed thresholds are provided.
     *
     * @return <code>true</code> if health or failed thresholds are provided
     */
    public boolean isEnabled() {
        return isHealthEnabled || isThresholdEnabled;
    }

    /**
     * Returns the healthy.
     *
     * @return the healthy
     */
    public final int getHealthy() {
        return healthy;
    }

    /**
     * Sets the healthy to the specified value.
     *
     * @param healthy the value to set
     */
    public final void setHealthy(final int healthy) {
        this.healthy = healthy;
    }

    /**
     * Returns the unHealthy.
     *
     * @return the unHealthy
     */
    public final int getUnHealthy() {
        return unHealthy;
    }

    /**
     * Sets the unHealthy to the specified value.
     *
     * @param unHealthy the value to set
     */
    public final void setUnHealthy(final int unHealthy) {
        this.unHealthy = unHealthy;
    }

    /**
     * Returns the isHealthyReportEnabled.
     *
     * @return the isHealthyReportEnabled
     */
    public final boolean isHealthyReportEnabled() {
        return isHealthEnabled;
    }

    /**
     * Sets the isHealthyReportEnabled to the specified value.
     *
     * @param isHealthyReportEnabled the value to set
     */
    public final void setHealthyReportEnabled(final boolean isHealthyReportEnabled) {
        isHealthEnabled = isHealthyReportEnabled;
    }

    /**
     * Returns the isThresholdEnabled.
     *
     * @return the isThresholdEnabled
     */
    public boolean isFailureThresholdEnabled() {
        return isThresholdEnabled;
    }

    /**
     * Sets the isThresholdEnabled to the specified value.
     *
     * @param isFailureThresholdEnabled the value to set
     */
    public void setFailureThresholdEnabled(final boolean isFailureThresholdEnabled) {
        isThresholdEnabled = isFailureThresholdEnabled;
    }

    /**
     * Returns the reportName.
     *
     * @return the reportName
     */
    public final String getReportName() {
        return reportName;
    }

    /**
     * Sets the reportName to the specified value.
     *
     * @param reportName the value to set
     */
    public final void setReportName(final String reportName) {
        this.reportName = reportName;
    }

    /**
     * Returns the itemName.
     *
     * @return the itemName
     */
    public final String getItemName() {
        return itemName;
    }

    /**
     * Sets the itemName to the specified value.
     *
     * @param itemName the value to set
     */
    public final void setItemName(final String itemName) {
        this.itemName = itemName;
    }

    /**
     * Returns the threshold.
     *
     * @return the threshold
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Sets the threshold to the specified value.
     *
     * @param threshold the value to set
     */
    public void setThreshold(final int threshold) {
        this.threshold = threshold;
    }

    /**
     * Creates a list of integer values used to create a three color graph
     * showing the items per build.
     *
     * @param totalCount
     *            total number of items
     * @return the list of values
     */
    public List<Integer> createSeries(final int totalCount) {
        List<Integer> series = new ArrayList<Integer>(3);
        int remainder = totalCount;

        if (isHealthEnabled) {
            series.add(Math.min(remainder, healthy));

            int range = unHealthy - healthy;
            remainder -= healthy;
            if (remainder > 0) {
                series.add(Math.min(remainder, range));
            }
            else {
                series.add(0);
            }

            remainder -= range;
            if (remainder > 0) {
                series.add(remainder);
            }
            else {
                series.add(0);
            }
        }
        else if (isThresholdEnabled) {
            series.add(Math.min(remainder, threshold));

            remainder -= threshold;
            if (remainder > 0) {
                series.add(remainder);
            }
            else {
                series.add(0);
            }
        }

        return series;
    }

    /**
     * Creates a trend graph for the corresponding action using the thresholds
     * of this health builder.
     *
     * @param useHealthBuilder
     *            if the health thresholds should be used at all
     * @param url
     *            the URL shown in the tool tips
     * @param dataset
     *            the data set of the values to render
     * @return the created graph
     */
    public JFreeChart createGraph(final boolean useHealthBuilder, final String url, final CategoryDataset dataset) {
        StackedAreaRenderer renderer;
        if (useHealthBuilder && isEnabled()) {
            renderer = new ResultAreaRenderer(url, itemName);
        }
        else {
            renderer = new PrioritiesAreaRenderer(url, itemName);
        }

        ChartBuilder chartBuilder = new ChartBuilder();
        return chartBuilder.createChart(dataset, renderer, getThreshold(),
                isHealthyReportEnabled() || !isFailureThresholdEnabled() || !useHealthBuilder);
    }
}

