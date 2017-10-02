/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.meters;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A meter that maintains distribution statistics about the data added
 * to the meter. This meter should be used where more than simple sums
 * and averages are required.
 */
@Trivial
public class StatisticsMeter extends com.ibm.websphere.monitor.jmx.StatisticsMeter implements StatisticsMXBean {

    /**
     * Simple type that holds values required to aggregate statistical
     * information across threads.
     * <p>
     * While instances of this class should only be updated by the thread
     * that owns it, the methods are synchronized to prevent an aggregation
     * function from observing intermediate results. While this adds some
     * overhead, the associated monitor should rarely be contended.
     */
    @Trivial
    final static class StatsData {
        long count;
        long min;
        long max;
        double total;
        double mean;
        double varianceNumeratorSum;

        synchronized double getVariance() {
            return count <= 1 ? 0 : varianceNumeratorSum / (count - 1);
        }

        synchronized void addDataPoint(long value) {
            if (count == 0) {
                min = value;
                max = value;
            }
            double delta = value - mean;
            count++;
            total += value;
            mean = mean + delta / count;
            varianceNumeratorSum += delta * (value - mean);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        synchronized StatsData getCopy() {
            StatsData copy = new StatsData();

            copy.count = this.count;
            copy.min = this.min;
            copy.max = this.max;
            copy.total = this.total;
            copy.mean = this.mean;
            copy.varianceNumeratorSum = this.varianceNumeratorSum;

            return copy;
        }

        @Override
        public String toString() {
            DecimalFormat decimalFormat = new DecimalFormat("0.000");
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
            StringBuilder sb = new StringBuilder();

            StatsData stats = getCopy();
            sb.append("count=").append(stats.count);
            sb.append(" total=").append(Math.round(stats.total));
            sb.append(" mean=").append(decimalFormat.format(stats.mean));
            sb.append(" variance=").append(decimalFormat.format(stats.getVariance()));
            sb.append(" stddev=").append(decimalFormat.format(Math.sqrt(stats.getVariance())));
            sb.append(" min=").append(stats.min);
            sb.append(" max=").append(stats.max);

            return sb.toString();
        }
    }

    /**
     * A class associated with a single thread that references the
     * statistical information accumulated for this thread. This level of
     * indirection is used to allow the thread local data to be garbage
     * collected without losing track of the historical data.
     */
    private final class ThreadStats {
        final StatsData statsData;

        ThreadStats() {
            statsData = new StatsData();
        }
    }

    /**
     * A subclass of {@link WeakReference} that we'll use to detect when the
     * thread specific {@linkplain ThreadStats} is no longer reachable. This
     * reference holds a strong reference to the {@linkplain StatsData} instance that was backing {@linkplain ThreadStats} so we can still
     * access the backing information after thread termination and accumulate
     * it into the data that's held for terminated threads.
     */
    private final class StatsDataReference extends WeakReference<ThreadStats> {
        final StatsData statsData;

        StatsDataReference(ThreadStats threadStats) {
            super(threadStats, statisticsReferenceQueue);
            this.statsData = threadStats.statsData;
        }
    }

    /**
     * Aggregated statistics from terminated threads.
     */
    private final AtomicReference<StatsData> terminatedThreadStats = new AtomicReference<StatsData>(new StatsData());

    /**
     * The set of all {@linkplain StatsDataReference} instances that are active.
     * We iterate over this set when aggregating the information for a caller.
     */
    private final Set<StatsDataReference> allReferences = Collections.synchronizedSet(new HashSet<StatsDataReference>());

    /**
     * Reference queue to track {@code ThreadStat} instances that are no
     * longer reachable through a {@linkplain ThreadLocal}.
     */
    private final ReferenceQueue<ThreadStats> statisticsReferenceQueue = new ReferenceQueue<ThreadStats>();

    /**
     * Container to hold thread specific statistics data.
     */
    private final class StatsThreadLocal extends ThreadLocal<ThreadStats> {
        @Override
        public ThreadStats initialValue() {
            ThreadStats threadStats = new ThreadStats();
            allReferences.add(new StatsDataReference(threadStats));
            cleanup();
            return threadStats;
        }

        public StatsData getStatsData() {
            return get().statsData;
        }
    }

    /**
     * Instance of the container.
     */
    private final StatsThreadLocal threadStats = new StatsThreadLocal();

    /**
     * Create a new {@linkplain StatisticsMeter} that provides statistical
     * information about the various data points that are recorded.
     */
    public StatisticsMeter() {
        super();
    }

    /**
     * Add a data point to be data collected by the meter.
     *
     * @param value the value to add
     */
    public void addDataPoint(long value) {
        threadStats.getStatsData().addDataPoint(value);
    }

    /**
     * Get the minimum data point value added to this meter.
     *
     * @return the minimum data point added to the meter or 0 if no
     *         data points have been added
     */
    @Override
    public long getMinimumValue() {
        return getAggregateStats().min;
    }

    /**
     * Get the maximum data point value added to this meter.
     *
     * @return the maximum data point added to the meter or 0 if no
     *         data points have been added
     */
    @Override
    public long getMaximumValue() {
        return getAggregateStats().max;
    }

    /**
     * Get the total or <em>sum</em> of the data points added to this
     * meter.
     *
     * @return the total or <em>sum</em> of the data points added to
     *         this meter
     */
    @Override
    public double getTotal() {
        return getAggregateStats().total;
    }

    /**
     * Get the mean or <em>average</em> of the data points added to this
     * meter.
     *
     * @return the mean or <em>average</em> of the data points added to
     *         this meter
     */
    @Override
    public double getMean() {
        return getAggregateStats().mean;
    }

    /**
     * Get the variance of the data points added to this meter.
     *
     * @return the variance of the data added to this meter
     */
    @Override
    public double getVariance() {
        return getAggregateStats().getVariance();
    }

    /**
     * Get the standard deviation of the data points added to this meter.
     */
    @Override
    public double getStandardDeviation() {
        return Math.sqrt(getAggregateStats().getVariance());
    }

    /**
     * Get the number of data points that have been added to this meter.
     *
     * @return the number of data points added to this meter
     */
    @Override
    public long getCount() {
        return getAggregateStats().count;
    }

    /**
     * Combine the data from a list of {@code StatsData} instances that
     * each contains a subset of the data points. The meters must contain
     * values from the same data set to be meaningful.
     *
     * @return a {@code StatsData} instance that represents the aggregated
     *         statistics from this meter
     */
    StatsData getAggregateStats() {
        Set<StatsData> dataSet = new HashSet<StatsData>();
        synchronized (allReferences) {
            dataSet.add(terminatedThreadStats.get().getCopy());
            for (StatsDataReference ref : allReferences) {
                dataSet.add(ref.statsData.getCopy());
            }
        }
        cleanup();
        return aggregateStats(dataSet);
    }

    /**
     * Aggregate {@linkplain StatsData} instances.
     *
     * @param dataSet a variable length list of {@linkplain StatsData} instances
     *
     * @return aggregated {@linkplain StatsData}
     */
    static StatsData aggregateStats(StatsData... dataSet) {
        return aggregateStats(Arrays.asList(dataSet));
    }

    /**
     * Aggregate a collection of {@linkplain StatsData} instances.
     *
     * @param dataSet a collection of {@linkplain StatsData} instances
     *
     * @return aggregated {@linkplain StatsData}
     */
    static StatsData aggregateStats(Collection<StatsData> dataSet) {
        StatsData combined = new StatsData();
        for (StatsData stats : dataSet) {
            if (stats.count == 0) {
                continue;
            }
            if (combined.total == 0) {
                combined.min = stats.min;
                combined.max = stats.max;
            }
            combined.total = combined.total + stats.total;
            combined.count = combined.count + stats.count;
            combined.mean = combined.total / combined.count;
            combined.min = Math.min(combined.min, stats.min);
            combined.max = Math.max(combined.max, stats.max);

            // Sum of the squares of the difference from the mean
            double meanDifference = stats.mean - combined.mean;
            combined.varianceNumeratorSum += stats.varianceNumeratorSum + stats.count * meanDifference * meanDifference;
        }
        return combined;
    }

    /**
     * Poll the reference queue looking for statistics data associated with
     * a thread that is no longer reachable. If one is found, update the
     * terminated thread statistics.
     */
    private void cleanup() {
        StatsDataReference ref = null;
        while ((ref = (StatsDataReference) statisticsReferenceQueue.poll()) != null) {
            StatsData oldStats = null;
            StatsData updatedStats = null;
            do {
                oldStats = terminatedThreadStats.get();
                updatedStats = aggregateStats(oldStats, ref.statsData);
            } while (!terminatedThreadStats.compareAndSet(oldStats, updatedStats));
            allReferences.remove(ref);
        }
    }

    @Override
    public StatisticsReading getReading() {
        StatsData sd = getAggregateStats();
        return new StatisticsReading(sd.count, sd.min, sd.max, sd.total, sd.mean, sd.getVariance(), Math.sqrt(sd.getVariance()), getUnit());
    }

    /**
     * Get a human readable string representation of the statistics collected
     * by this meter.
     *
     * @return the human readable data
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(terminatedThreadStats == null ? "not be initialized" : getAggregateStats());
        return sb.toString();
    }

}
