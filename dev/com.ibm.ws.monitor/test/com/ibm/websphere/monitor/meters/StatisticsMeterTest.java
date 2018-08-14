/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.monitor.meters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatisticsMeterTest {

    private final static double STANDARD_EPSILON = 0.0001;

    StatisticsMeter statsMeter;

    @Before
    public void setup() {
        statsMeter = new StatisticsMeter();
        statsMeter.setDescription("Test meter");
        statsMeter.setUnit("widgets");
    }

    @After
    public void tearDown() {
        statsMeter = null;
    }

    @Test
    public void testNewStatisticsMeter() {
        assertEquals(0L, statsMeter.getCount());
        assertEquals(0L, statsMeter.getMinimumValue());
        assertEquals(0L, statsMeter.getMaximumValue());
        assertEquals(0.0, statsMeter.getTotal(), STANDARD_EPSILON);
        assertEquals(0.0, statsMeter.getMean(), STANDARD_EPSILON);
        assertEquals(0.0, statsMeter.getVariance(), STANDARD_EPSILON);
        //        assertEquals(0.0, statsMeter.varianceNumeratorSum, STANDARD_EPSILON);
    }

    @Test
    public void testSingleDataPoint() {
        statsMeter.addDataPoint(50);

        assertEquals(1L, statsMeter.getCount());
        assertEquals(50L, statsMeter.getMinimumValue());
        assertEquals(50L, statsMeter.getMaximumValue());
        assertEquals(50.0, statsMeter.getTotal(), STANDARD_EPSILON);
        assertEquals(50.0, statsMeter.getMean(), STANDARD_EPSILON);
        assertEquals(0.0, statsMeter.getVariance(), STANDARD_EPSILON);
        //        assertEquals(0.0, statsMeter.varianceNumeratorSum, STANDARD_EPSILON);
    }

    @Test
    public void testNoVariance() {
        for (int i = 0; i < 10000; i++) {
            statsMeter.addDataPoint(50);
        }

        assertEquals(50L, statsMeter.getMinimumValue());
        assertEquals(50L, statsMeter.getMaximumValue());
        assertEquals(50.0, statsMeter.getMean(), STANDARD_EPSILON);
        assertEquals(0.0, statsMeter.getVariance(), STANDARD_EPSILON);
    }

    @Test
    public void testAllCalculations() {
        int loopCount = 10000;
        int offset = 0;
        int modulo = 10000;

        List<Long> dataPoints = new ArrayList<Long>(loopCount);
        for (int i = 0; i < loopCount; i++) {
            dataPoints.add(Long.valueOf(i % modulo + offset));
            statsMeter.addDataPoint(i % modulo + offset);
        }

        assertEquals("Incorrect count", (long) loopCount, statsMeter.getCount());
        assertEquals("Incorrect min", (long) offset, statsMeter.getMinimumValue());
        assertEquals("Incorrect max", (long) Math.max(offset - 1 + Math.min(loopCount, modulo), offset), statsMeter.getMaximumValue());
        assertEquals("Incorrect total", calculateSum(dataPoints), statsMeter.getTotal(), STANDARD_EPSILON);
    }

    @Test
    public void testCombineReadings() {
        int meterCount = 1000;
        int loopCount = 1000;
        int offset = 1;
        int modulo = 1000;

        StatisticsMeter controlMeter = new StatisticsMeter();
        controlMeter.setDescription("Control meter");
        List<StatisticsMeter> meters = new ArrayList<StatisticsMeter>(loopCount);
        for (int j = 0; j < meterCount; j++) {
            StatisticsMeter statsMeter = new StatisticsMeter();
            statsMeter.setDescription("Test meter " + j);
            for (int i = 0; i < loopCount; i++) {
                statsMeter.addDataPoint(i % modulo + offset);
                controlMeter.addDataPoint(i % modulo + offset);
            }
            meters.add(statsMeter);
        }

        Collection<StatisticsMeter.StatsData> dataSet = new HashSet<StatisticsMeter.StatsData>();
        for (StatisticsMeter meter : meters) {
            dataSet.add(meter.getAggregateStats());
        }

        StatisticsMeter.StatsData combined = StatisticsMeter.aggregateStats(dataSet);
        assertEquals("Incorrect count", controlMeter.getCount(), combined.count);
        assertEquals("Incorrect min", controlMeter.getMinimumValue(), combined.min);
        assertEquals("Incorrect max", controlMeter.getMaximumValue(), combined.max);
        assertEquals("Incorrect total", controlMeter.getTotal(), combined.total, STANDARD_EPSILON);
        assertEquals("Incorrect mean", controlMeter.getMean(), combined.mean, STANDARD_EPSILON);
        assertEquals("Incorrect variance", controlMeter.getVariance(), combined.getVariance(), STANDARD_EPSILON);
    }

    @Test
    public void testToStringNoData() {
        String string = statsMeter.toString();
        assertTrue(string.contains("count=0"));
        assertTrue(string.contains("total=0"));
        assertTrue(string.contains("mean=0.000"));
        assertTrue(string.contains("variance=0.000"));
        assertTrue(string.contains("min=0"));
        assertTrue(string.contains("max=0"));
    }

    @Test
    public void testToString() {
        for (int i = 1; i < 1000; i++) {
            statsMeter.addDataPoint(i);
        }

        String string = statsMeter.toString();
        assertTrue(string.contains("count=999"));
        assertTrue(string.contains("total=499500"));
        assertTrue(string.contains("mean=500.000"));
        assertTrue(string.contains("variance=83250.000"));
        assertTrue(string.contains("min=1"));
        assertTrue(string.contains("max=999"));
    }

    // Calculate the variance using the pre-calculated mean.
    static double calculateVariance(List<Long> dataPoints) {
        double mean = calculateMean(dataPoints);
        double squaredDifferenceSum = 0;
        for (int i = dataPoints.size() - 1; i >= 0; i--) {
            double difference = dataPoints.get(i) - mean;
            squaredDifferenceSum += (difference * difference);
        }

        return squaredDifferenceSum / (dataPoints.size() - 1);
    }

    static double calculateMean(List<Long> dataPoints) {
        if (dataPoints.isEmpty()) {
            return 0;
        }
        return calculateSum(dataPoints) / dataPoints.size();
    }

    static double calculateSum(List<Long> dataPoints) {
        double sum = 0;
        for (int i = dataPoints.size() - 1; i >= 0; i--) {
            sum += dataPoints.get(i);
        }
        return sum;
    }
}
