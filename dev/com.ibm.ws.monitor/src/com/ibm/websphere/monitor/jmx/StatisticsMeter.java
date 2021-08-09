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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A meter that maintains distribution statistics about the data added
 * to the meter.
 * 
 * @ibm-api
 */
@Trivial
public class StatisticsMeter extends Meter {

    long minimumValue, maximumValue, count;
    double mean, variance, standardDeviation, total;
    StatisticsReading reading;

    /**
     * Default constructor.
     */
    public StatisticsMeter() {
        super();
    }

    /**
     * Constructor used during construction of proxy objects for MXBeans.
     */
    @ConstructorProperties({ "minimumValue", "maximumValue", "count", "mean", "variance", "standardDeviation", "total", "reading", "description", "unit" })
    public StatisticsMeter(long minimumValue, long maximumValue, long count, double mean, double variance, double standardDeviation, double total, StatisticsReading reading,
                           String description, String unit) {
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.count = count;
        this.mean = mean;
        this.variance = variance;
        this.standardDeviation = standardDeviation;
        this.total = total;
        this.reading = reading;

        setDescription(description);
        setUnit(unit);
    }

    /**
     * Get the minimum data point value added to this meter.
     * 
     * @return the minimum data point added to the meter or 0 if no data points have been added
     */
    public long getMinimumValue() {
        return minimumValue;
    }

    /**
     * Get the maximum data point value added to this meter.
     * 
     * @return the maximum data point added to the meter or 0 if no data points have been added
     */
    public long getMaximumValue() {
        return maximumValue;
    }

    /**
     * Get the total or <em>sum</em> of the data points added to this meter.
     * 
     * @return the total or <em>sum</em> of the data points added to this meter
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the mean or <em>average</em> of the data points added to this meter.
     * 
     * @return the mean or <em>average</em> of the data points added to this meter
     */
    public double getMean() {
        return mean;
    }

    /**
     * Get the variance of the data points added to this meter.
     * 
     * @return the variance of the data added to this meter
     */
    public double getVariance() {
        return variance;
    }

    /**
     * Get the standard deviation of the data points added to this meter.
     * 
     * @return the standard deviation of the data added to this meter
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Get the number of data points that have been added to this meter.
     * 
     * @return the number of data points added to this meter
     */
    public long getCount() {
        return count;
    }

    /**
     * @return a snapshot of distribution statistics of the meter
     */
    public StatisticsReading getReading() {
        return reading;
    }

}
