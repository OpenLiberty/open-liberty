/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.jmx;

import java.beans.ConstructorProperties;

/**
 * Represents a snapshot of a {@link StatisticsMeter}. A StatisticsReading holds the current values of the StatisticsMeter at the time
 * it was obtained and will not change.
 * 
 * @ibm-api
 */
public class StatisticsReading {

    protected long timestamp, count, minimumValue, maximumValue;
    protected double total, mean, variance, standardDeviation;
    protected String unit;

    /**
     * Constructor used during construction of proxy objects for MXBeans.
     */
    @ConstructorProperties({ "timestamp", "count", "minimumValue", "maximumValue", "total", "mean", "variance", "standardDeviation", "unit" })
    public StatisticsReading(long timestamp, long count, long min, long max, double total, double mean, double variance, double stddev, String unit) {
        this.timestamp = timestamp;
        this.count = count;
        this.minimumValue = min;
        this.maximumValue = max;
        this.total = total;
        this.mean = mean;
        this.variance = variance;
        this.standardDeviation = stddev;
        this.unit = unit;
    }

    /**
     * @return timestamp of the statistics reading
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the number of data points that had been added to the meter at the time the reading was taken.
     * 
     * @return the number of data points
     */
    public long getCount() {
        return count;
    }

    /**
     * Get the minimum data point value that had been added to the meter at the time this reading was taken.
     * 
     * @return the minimum data point or 0 if no data points had been added
     */
    public long getMinimumValue() {
        return minimumValue;
    }

    /**
     * Get the maximum data point value that had been added to the meter at the time this reading was taken.
     * 
     * @return the maximum data point or 0 if no data points had been added
     */
    public long getMaximumValue() {
        return maximumValue;
    }

    /**
     * Get the total or <em>sum</em> of the data points that had been added to the meter at the time this reading was taken.
     * 
     * @return the total or <em>sum</em> of the data points
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the mean or <em>average</em> of the data points that had been added to the meter at the time this reading was taken.
     * 
     * @return the mean or <em>average</em> of the data points
     */
    public double getMean() {
        return mean;
    }

    /**
     * Get the variance of the data points that had been added to the meter at the time this reading was taken.
     * 
     * @return the variance of the data
     */
    public double getVariance() {
        return variance;
    }

    /**
     * Get the standard deviation of the data points that had been added to the meter at the time this reading was taken.
     * 
     * @return the standard deviation of the data
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * @return unit of measurement of the data points
     */
    public String getUnit() {
        return unit;
    }

}
