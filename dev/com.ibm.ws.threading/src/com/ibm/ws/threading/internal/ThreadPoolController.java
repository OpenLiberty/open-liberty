/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.CpuInfo;

// @formatter:off
/**
 * A simple controller that observes the throughput of the system and
 * attempts to manage the number of threads in the system in a way that
 * maximizes throughput and minimizes resource consumption associated
 * with managing a high number of threads.
 * <p>
 * Here are the basic ideas and assertions behind the controller:
 * <ol>
 * <li>
 * The throughput of a multi-threaded system follows a pattern where
 * the addition of threads causes the overall throughput to rise until
 * it plateaus. After the throughput plateaus, a combination of resource
 * contention and increased context switching overhead cause the throughput
 * to drop.
 * <pre><tt>
 *   |
 *   |
 * T |        **********
 * h |      **          *******
 * r |     *                   ****************
 * o |     *
 * u |    *
 * g |    *
 * h |   *
 * p |   *
 * u |  *
 * t |  *
 *   | *
 *   +----------------------------------------------------------------------
 *                                 # Threads
 * </tt></pre>
 * </li>
 * <li>
 * The best place to land on the throughput curve is early in the plateau
 * where the throughput is highest and the number of excess threads is low.
 * </li>
 * <li>
 * Forecasting the throughput of a thread pool with a particular size can be
 * accomplished by tracking an exponentially weighted moving average of
 * throughput observations at regular intervals. Observations are only
 * relevant if all threads in the pool are busy. (If threads in the pool are
 * idle, the effective pool size is smaller.)
 * </li>
 * <li>
 * Even with a fixed client load and a fixed number of threads, the throughput
 * of a thread pool will vary. Even if it's not strictly accurate, we can treat
 * this variability as a standard distribution around a mean.
 * </li>
 * <li>
 * By treating the variability of throughput as a standard distribution, we
 * can calculate the probability of observing a throughput lower than an
 * arbitrary value by using the cumulative probability attribute of the
 * standard normal curve.
 * </li>
 * <li>
 * Determining whether to grow the pool, shrink the pool, or maintain the size
 * of the pool can be determined by looking at the probability of a higher
 * throughput than the forecast at the current pool size.
 * </li>
 * <li>
 * A thread pool should only grow in size if there is work to process on the
 * thread pool's queue. If there's no work on the queue, adding a thread
 * is unlikely to improve throughput as there's no pending work to complete.
 * <p><em>Note: Some workload patterns have violated this assertion.</em></p>
 * </li>
 * <li>
 * A thread pool should shrink if there's no work on the queue feeding the pool
 * and the throughput doesn't suffer.
 * </li>
 * </ol>
 *
 * @see {@link http://en.wikipedia.org/wiki/Standard_normal_distribution}
 * @see {@link http://en.wikipedia.org/wiki/Probability_density_function}
 */
// @formatter:on
public final class ThreadPoolController {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(ThreadPoolController.class);

    /**
     * Time in milliseconds between thread management actions.
     */
    private final static long interval;

    /**
     * Time in milliseconds between thread management actions when
     * a hang has been detected
     */
    private final static long hangInterval;

    /**
     * Counter to indicate whether controller is in hang resolution mode and
     * if so, how many more cycles it will stay in hang resolution mode
     */
    private int hangResolutionCountdown = 0;

    /**
     * The starting value for hangResolutionCountdown
     */
    private static final int hangResolutionCycles = 3;

    /**
     * The poolSize used by hang resolution to break the most recently detected hang,
     * plus a buffer amount, used to reduce the likelihood of sending the pool back
     * below the hang threshold.
     */
    private int hangBufferPoolSize = 0;

    /**
     * This will be maintained as the greater of coreThreads and hangBufferPoolSize,
     * and the controller will not shrink the pool below this number of threads.
     */
    private int currentMinimumPoolSize = 0;

    /**
     * The number of consecutive controller cycles with no hang
     */
    private int controllerCyclesWithoutHang = 0;

    /**
     * When a prior hang has set hangResolutionPoolSize, if the controller runs for a
     * number of consecutive cycles below hangResolutionPoolSize without a hang, we will
     * gradually reduce hangResolutionPoolSize, until it reaches coreThreads. This allows
     * the controller to return to its base/default state if the workload changes to a
     * non-hanging config.
     */
    private static final int noHangCyclesThreshold = 8;

    /**
     * How far from current poolSize to consider when evaluating whether to
     * grow or shrink the pool.
     */
    private final static int compareRange;

    /**
     * The number of cpus available to the threadpool is a key input to various controller
     * decisions.
     */
    private final int numberCpus;

    /**
     * Counter of number of cycles the controller has run in which the historical
     * throughput data was updated. This is used to mark the age of the data samples
     * so that old data can be discarded.
     */
    private int controllerCycle = 0;

    /**
     * Records whether the distribution was reset by the outlier handling in the current cycle
     */
    private boolean distributionReset = false;

    /**
     * The amount by which the pool will be incremented and decremented, when the controller
     * decides that a size change is warranted. The size is adjusted as the controller runs,
     * taking into account the number of cpus (hardware threads) available and the current
     * size of the pool. Setting increment and decrement using these factors allows the
     * controller adjustments to be 'right-sized' for the environment and workload in which
     * it is running.
     */
    private int poolIncrement = 1;
    private int poolDecrement = poolIncrement;

    /**
     * Corner case coverage - when maxThreads is set, and data below maxThreads has been pruned,
     * this variable will tell us how much we should decrement to fit into the prior pattern of
     * increment/decrement steps.
     */
    private int maxThreadsPoolDecrement = poolDecrement;

    /**
     * The starting point for making poolSize adjustments. The controller defaults to changing
     * the poolSize by increments of number of cpus (hardware threads) available, but in some
     * special cases may need to use a different value.
     * For example, coreThreads defaults to (NUMBER_CPUS * 2); if coreThreads is instead smaller
     * than (NUMBER_CPUS * 2), the operator must have configured a small coreThreads value. The
     * controller will use that configured small coreThreads value as the guide for increment
     * and decrement sizes.
     */
    private final int poolChangeBasis;

    /**
     * These variables set the poolSize thresholds at which poolIncrement and poolDecrement
     * values change. They default to multiples of the number of cpus available.
     */
    private final int poolIncrementBoundLow;
    private final int poolIncrementBoundMedium;

    /**
     * These variable allow manual limits to be placed on the amount by which the pool will
     * be incremented and decremented. They are intended for diagnostic or triage usage. If
     * the configurable values are not set, the defaults are used.
     */
    private final int poolIncrementMax;
    private final static int poolIncrementMin;
    private final static int POOL_INCREMENT_MIN_DEFAULT = 1;
    private final int POOL_INCREMENT_MAX_DEFAULT;

    /**
     * If coreThreads and maxThreads are configured to a narrow range on a system with more
     * than 1 cpu available, the controller could end up with a very small set of poolSizes
     * available, since the default poolIncrement size is NUMBER_CPUS. In this case, the
     * controller will calculate an alternate increment size, using this parameter as an input.
     */
    private final static int minimumDesiredPoolSizeAdjustments;

    /**
     * When the controller requests the underlying executor change the poolSize, the executor
     * may not complete the request by the time the next controller cycle comes around (this
     * behavior has been seen in testing with large cpu counts and thus large increment /
     * decrement size. These variables are used to track whether the executor has completed
     * the requested thread increase/decrease, and to trigger restatement of the request if
     * it is not completed after some number of controller cycles.
     */
    private int targetPoolSize = -1;
    private int consecutiveTargetPoolSizeWrong = 0;
    private final static int MAX_CONSECUTIVE_TARGET_POOLSIZE_WRONG = 3;

    /**
     * The controller adjusts its inclination to grow/shrink the pool if the current system
     * and/or java process cpu utilization exceeds this threshold.
     */
    private final static int highCpu;

    /**
     * The controller will not grow the pool if the ratio of the current work rate (tput) to the
     * current poolsize (threads) is below this threshold.
     */
    private final static double lowTputThreadsRatio;

    /**
     * The controller will not grow the pool if the ratio of active threads to pool size is
     * below this ratio.
     */
    private final static double activeThreadsGrowthRatio;

    /**
     * The historical set of throughput data maintained by the controller is subject to pruning
     * to remove datapoints which are judged likely no longer valid or useful for making pool
     * sizing decisions.
     *
     * One criterion for pruning a datapoint is when it is judged to be probably unreliable
     * based on a combination of age and standard deviation of the ThroughputDistribution.
     * - age: the longer the datapoint goes without being updated, the less likely that it
     * is representative of the current condition of the system
     * - standard deviation: a high standard deviation means either the poolSize has been
     * rarely visited, or when the pool has been at that size, the throughput has been
     * highly variable ... in either case, the datapoint is not very reliable
     *
     * So if a datapoint falls below a threshold in assessment of age and standard deviation,
     * we are unlikely to lose much predictive value by pruning it.
     */
    private final static double dataAgePruneLevel;
    /**
     * Another criterion for pruning a datapoint is when it represents a poolSize that is far
     * removed from the current poolSize - consideration is given to keeping a wide enough range
     * of datapoints (compareSpan) to provide input for the controller's shrink/grow decisions.
     */
    private final static int compareSpanPruneMultiplier;
    /**
     * Another criterion for pruning a datapoint is how far removed the throughput is from the
     * current system throughput - if the throughput of the datapoint is very different (higher
     * or lower) than the current system throughput, probably there has been some workload or
     * other change since that datapoint was established, and it is no longer reliable.
     */
    private final static double tputRatioPruneLevel;

    /**
     * If comparison with nearby datapoints yields ambivalent guidance for grow/shrink decisions
     * the controller will consider the farthest away datapoints. These ratios are thresholds
     * to influence the controller's proclivity to shrink the pool.
     */
    private final static double poolTputRatioHigh;
    private final static double poolTputRatioLow;

    /**
     * This is used to decide whether a datapoint is far enough away from the current poolSize
     * to be useful in the ratio-based grow/shrink considerations in leanTowardShrinking().
     */
    private final static int compareSpanRatioMultiplier;

    /**
     * Small grow/shrink scores are filtered out to reduce poolSize volatility.
     */
    private final static double growScoreFilterLevel;
    private final static double shrinkScoreFilterLevel;

    /**
     * The grow/shrink scores must differ by this level to affect the poolSize
     */
    private final static double growShrinkDiffFilter;

    /**
     * When a new throughput value is observed for an existing ThroughputDistribution, if
     * the new value is sufficiently different from the existing datapoint, the new value
     * is considered an 'outlier'. Receiving an outlier may be cause to reset the datapoint,
     * discarding the historical moving average in favor of treating the outlier as the 'new
     * normal'. These variables provide the criteria for making the reset decision.
     */
    private final static double resetDistroStdDevEwmaRatio;
    private final static double resetDistroNewTputEwmaRatio;
    private final static double resetDistroConsecutiveOutliers;

    /**
     * Read in applicable system properties, use defaults if the property is not present
     * These system properties will not be documented, and are intended for diagnostic and/or
     * triage use by support.
     */
    static {
        String tpcResetDistroStdDevEwmaRatio = getSystemProperty("tpcResetDistroStdDevEwmaRatio");
        resetDistroStdDevEwmaRatio = (tpcResetDistroStdDevEwmaRatio == null) ? 0.10 : Double.parseDouble(tpcResetDistroStdDevEwmaRatio);

        String tpcResetDistroNewTputEwmaRatio = getSystemProperty("tpcResetDistroNewTputEwmaRatio");
        resetDistroNewTputEwmaRatio = (tpcResetDistroNewTputEwmaRatio == null) ? 0.50 : Double.parseDouble(tpcResetDistroNewTputEwmaRatio);

        String tpcResetDistroConsecutiveOutliers = getSystemProperty("tpcResetDistroConsecutiveOutliers");
        resetDistroConsecutiveOutliers = (tpcResetDistroConsecutiveOutliers == null) ? 5 : Integer.parseInt(tpcResetDistroConsecutiveOutliers);

        String tpcMinimumDesiredPoolSizeAdjustments = getSystemProperty("tpcMinimumDesiredPoolSizeAdjustments");
        minimumDesiredPoolSizeAdjustments = (tpcMinimumDesiredPoolSizeAdjustments == null) ? 10 : Integer.parseInt(tpcMinimumDesiredPoolSizeAdjustments);

        String tpcTputRatioPruneLevel = getSystemProperty("tpcTputRatioPruneLevel");
        tputRatioPruneLevel = (tpcTputRatioPruneLevel == null) ? 5.0 : Double.parseDouble(tpcTputRatioPruneLevel);

        String tpcPoolTputRatioHigh = getSystemProperty("tpcPoolTputRatioHigh");
        poolTputRatioHigh = (tpcPoolTputRatioHigh == null) ? 5.00 : Double.parseDouble(tpcPoolTputRatioHigh);

        String tpcPoolTputRatioLow = getSystemProperty("tpcPoolTputRatioLow");
        poolTputRatioLow = (tpcPoolTputRatioLow == null) ? 3.00 : Double.parseDouble(tpcPoolTputRatioLow);

        String tpcCompareSpanRatioMultiplier = getSystemProperty("tpcCompareSpanRatioMultiplier");
        compareSpanRatioMultiplier = (tpcCompareSpanRatioMultiplier == null) ? 2 : Integer.parseInt(tpcCompareSpanRatioMultiplier);

        String tpcGrowScorePruneLevel = getSystemProperty("tpcGrowScorePruneLevel");
        growScoreFilterLevel = (tpcGrowScorePruneLevel == null) ? 0.50 : Double.parseDouble(tpcGrowScorePruneLevel);

        String tpcShrinkScorePruneLevel = getSystemProperty("tpcShrinkScorePruneLevel");
        shrinkScoreFilterLevel = (tpcShrinkScorePruneLevel == null) ? 0.50 : Double.parseDouble(tpcShrinkScorePruneLevel);

        String tpcGrowShrinkDiffFilter = getSystemProperty("tpcGrowShrinkDiffFilter");
        growShrinkDiffFilter = (tpcGrowShrinkDiffFilter == null) ? 0.25 : Double.parseDouble(tpcGrowShrinkDiffFilter);

        String tpcPoolIncrementMin = getSystemProperty("tpcPoolIncrementMin");
        poolIncrementMin = (tpcPoolIncrementMin == null) ? POOL_INCREMENT_MIN_DEFAULT : Integer.parseInt(tpcPoolIncrementMin);

        String tpcHighCpu = getSystemProperty("tpcHighCpu");
        highCpu = (tpcHighCpu == null) ? 90 : Integer.parseInt(tpcHighCpu);

        String tpcLowTputThreadsRatio = getSystemProperty("tpcLowTputThreadsRatio");
        lowTputThreadsRatio = (tpcLowTputThreadsRatio == null) ? 1.00 : Double.parseDouble(tpcLowTputThreadsRatio);

        String tpcActiveThreadsGrowthRatio = getSystemProperty("tpcActiveThreadsGrowthRatio");
        activeThreadsGrowthRatio = (tpcActiveThreadsGrowthRatio == null) ? 0.75 : Double.parseDouble(tpcActiveThreadsGrowthRatio);

        String tpcDataAgePruneLevel = getSystemProperty("tpcDataAgePruneLevel");
        dataAgePruneLevel = (tpcDataAgePruneLevel == null) ? 5.0 : Double.parseDouble(tpcDataAgePruneLevel);

        String tpcCompareSpanPruneMultiplier = getSystemProperty("tpcCompareSpanPruneMultiplier");
        compareSpanPruneMultiplier = (tpcCompareSpanPruneMultiplier == null) ? 2 : Integer.parseInt(tpcCompareSpanPruneMultiplier);

        String tpcInterval = getSystemProperty("tpcInterval");
        interval = (tpcInterval == null) ? 1500 : Integer.parseInt(tpcInterval);

        String tpcHangInterval = getSystemProperty("tpcHangInterval");
        hangInterval = (tpcHangInterval == null) ? 750 : Integer.parseInt(tpcHangInterval);

        String tpcCompareRange = getSystemProperty("tpcCompareRange");
        compareRange = (tpcCompareRange == null) ? 4 : Integer.parseInt(tpcCompareRange);

    }

    /**
     * The controller uses cpu utilization as an input to grow/shrink decisions.
     * - processCpuUtil is the cpu usage of the JVM the controller is running in
     * as a percentage of the number of cpus the JVM believes are available
     * - systemCpuUtil is the cpu usage of the full (physical or virtual) machine
     * the JVM is running on
     */

    private double processCpuUtil = -1.0;
    private double systemCpuUtil = -1.0;
    private double cpuUtil = -1.0;
    private static DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * The controller uses the threadpool queue depth as an input to some of the
     * decisions it makes.
     */
    private int queueDepth = 0;

    /**
     * How many threads are active (running tasks) at the current controller cycle
     */
    private int activeThreads = 0;

    /**
     * Maximum intervals that we'll allow without changing the thread pool
     * size. By forcing the pool size to change every few intervals we can
     * prevent the pool from getting stuck in one spot when historic data
     * indicates the current size is the best for throughput.
     * <p>
     * If this value is too small, the pool will oscillate and may cause
     * significant throughput variation in CPU bound workloads; if the value
     * is too large, we won't react to workload changes or resource constraints
     * in a timely manner.
     */
    // 8/11/2012: Changed from 10 to 5
    private final static int MAX_INTERVALS_WITHOUT_CHANGE = 5;

    /**
     * An arbitrary <em>magic</em> value to increase the shrink score when
     * the queue feeding the thread pool is empty. When the thread pool queue
     * is empty, that implies we have more threads than we need.
     * <p>
     * If this value is too large, a small dip in workload will cause the
     * pool to shrink very rapidly.
     */
    // 8/13/2012: Shrink magic goes from 5% to 10%
    // 8/15/2012: 1/MAX_INTERVALS_WITHOUT_CHANGE/2 (10% with MAX = 5)
    // 8/22/2012: Halving back to 5%
    final static double EMPTY_QUEUE_SHRINK_MAGIC_PER_INTERVAL = 1.0 / MAX_INTERVALS_WITHOUT_CHANGE / 5.0;

    /**
     * The number of consecutive idle thread pool intervals of the controller
     * will remain active before pausing.
     */
    private final static int IDLE_INTERVALS_BEFORE_PAUSE = 3;

    /**
     * If we repeatedly encounter a situation where the first throughput
     * measurement after changing the size of the thread pool is an outlier,
     * the workload (or something on the system) may have changed enough to
     * warrant a complete reset of the historical data. This value represents
     * the number of times that we encounter that situation before we do the
     * reset.
     */
    private final static int MAX_OUTLIER_AFTER_CHANGE_BEFORE_RESET = (compareRange * 2 + 2);

    /**
     * The limit to how many threads we will add to break the executor out of a hang.
     * The throughput algorithm can still add threads beyond this level if maxThreads
     * is set higher, but the hang prevention code will never go past this value
     * regardless of what maxThreads is set to. This is to prevent runaway thread
     * creation in the case where every new task immediately gets blocked by the same
     * underlying condition.
     */
    private final int maxThreadsToBreakHang;

    /**
     * Reference to the configured ExecutorService implementation that
     * delegates to the {@link ThreadPoolExecutorImpl} that is controlled
     * by this controller.
     */
    private final ExecutorServiceImpl executorService;

    /**
     * A representation of the action taken by this controller at the end of the
     * previous interval.
     */
    enum LastAction {
        /** No action taken. */
        NONE,

        /** Thread pool size was increased. */
        GROW,

        /** Thread pool size was reduced. */
        SHRINK,

        /** Thread pool controller was paused. */
        PAUSE
    };

    /**
     * The last action taken by this controller.
     */
    private LastAction lastAction = LastAction.NONE;

    /**
     * A reference to the timer thread that schedules the interval task.
     */
    private final Timer timer = new Timer("Executor Service Control Timer", true);

    /**
     * The active interval {@link TimerTask}.
     */
    private IntervalTask activeTask = null;

    /**
     * An indication of whether or not the controller is paused because
     * the thread pool is idle.
     */
    private boolean paused = false;

    /**
     * A time stamp representing the last time an interval was process.ed
     */
    private long lastTimerPop = 0;

    /**
     * The completed task count from the previous interval.
     */
    private long previousCompleted = 0;

    /**
     * The throughput (completed / second) observed over the previous interval.
     */
    private double previousThroughput = 0;

    /**
     * A count of the consecutive intervals where we've observed an empty
     * queue. This value is used to influence the shrink score of an
     * interval in a way that attracts the pool to a smaller size.
     */
    private int consecutiveQueueEmptyCount = 0;

    /**
     * A count of the consecutive intervals where not pool size adjustment
     * has been made. This value is used to determine when the controller
     * forces a change to the size of the pool to prevent getting "stuck"
     * at a position in the throughput curve that is no longer optimal.
     */
    private int consecutiveNoAdjustment = 0;

    /**
     * A count of the number of consecutive times we've encountered an
     * abnormal data point after changing the pool size. This value is used
     * to determine when an abrupt workload change has occurred that should
     * cause historical data to be abandoned and the pool size to be reset.
     */
    private int consecutiveOutlierAfterAdjustment = 0;

    /**
     * A count of the consecutive intervals where the thread pool has been
     * completely idle. This is used to determine when the interval monitoring
     * task should be paused.
     */
    private int consecutiveIdleCount = 0;

    /**
     * The limit on the number of threads we can grow the pool to.
     */
    private final int maxThreads;

    /**
     * The lower bound on the number of threads we can shrink the pool to.
     */
    private final int coreThreads;

    /**
     * The range between maxThreads and coreThreads
     */
    private final int threadRange;

    /**
     * The thread pool executor associated with the active executor service.
     */
    ThreadPoolExecutor threadPool;

    /**
     * Use TreeMap for storing historical data, so that next lower/higher
     * threadpool history data can be found with API calls.
     */
    private TreeMap<Integer, ThroughputDistribution> threadStats = new TreeMap<Integer, ThroughputDistribution>();

    /**
     * The number of threads in the pool when a deadlock was first detected.
     */
    private int poolSizeWhenHangDetected = -1;

    /**
     * Indicates how many intervals we've been hung for. This field is
     * used to determine whether we should log a message indicating that
     * the thread pool is hung while at max threads - this message should
     * be output only when a hang has been detected two intervals in a row.
     * The hang detection logic can occur the first time, but it is possible
     * that we could detect a false positive hang on the first occurrence,
     * and so we want to avoid logging a warning in that case. If the hang
     * condition is resolved, this counter should be reset to zero.
     */
    private int hangIntervalCounter = 0;

    /**
     * Hang resolution will add threads to the pool hoping to break the hang. If the pool is
     * already at MAX_THREADS_TO_BREAK_HANG then hang resolution will emit a warning message.
     * This variable is used to limit the warning message to occur only once per hang event.
     */
    private boolean hangMaxThreadsMessageEmitted = false;

    /**
     * Constructor
     *
     * @param executorServce the configured OSGi component that's associated with
     *                           the managed thread pool.
     */
    ThreadPoolController(ExecutorServiceImpl executorService, ThreadPoolExecutor pool) {
        this.executorService = executorService;
        this.threadPool = pool;
        this.coreThreads = pool.getCorePoolSize();
        this.currentMinimumPoolSize = this.coreThreads;
        this.maxThreads = pool.getMaximumPoolSize();
        this.threadRange = this.maxThreads - this.coreThreads;
        setPoolSize(coreThreads);
        targetPoolSize = coreThreads;
        resetStatistics(true);
        // nothing to do if core == max
        if (coreThreads < maxThreads) {
            activeTask = new IntervalTask(this);
            timer.schedule(activeTask, interval, interval);
        }
        numberCpus = CpuInfo.getAvailableProcessors().get();
        /**
         * if coreThreads has been configured to a small value, we will use the
         * configured value as guidance for how large to make poolSize changes
         */
        if (coreThreads < numberCpus * 2) {
            poolChangeBasis = Math.max(1, coreThreads / 2);
        } else {
            poolChangeBasis = numberCpus;
        }

        maxThreadsToBreakHang = Math.max(1000, 128 * numberCpus);
        /**
         * Now that poolChangeBasis is set, we can assign the poolIncrement limit values
         * using poolChangeBasis, rather than NUMBER_CPUS, if the system properties are not present
         */
        String tpcPoolIncrementBoundLow = getSystemProperty("tpcPoolIncrementBoundLow");
        poolIncrementBoundLow = (tpcPoolIncrementBoundLow == null) ? poolChangeBasis * 16 : Integer.parseInt(tpcPoolIncrementBoundLow);

        String tpcPoolIncrementBoundMedium = getSystemProperty("tpcPoolIncrementBoundMedium");
        poolIncrementBoundMedium = (tpcPoolIncrementBoundMedium == null) ? poolChangeBasis * 64 : Integer.parseInt(tpcPoolIncrementBoundMedium);

        POOL_INCREMENT_MAX_DEFAULT = poolChangeBasis * 4;

        String tpcPoolIncrementMax = getSystemProperty("tpcPoolIncrementMax");
        poolIncrementMax = (tpcPoolIncrementMax == null) ? POOL_INCREMENT_MAX_DEFAULT : Integer.parseInt(tpcPoolIncrementMax);

        if (tc.isEventEnabled()) {
            reportSystemProperties();
        }
    }

    /**
     * Reset all statistics associated with the target thread pool.
     */
    void resetStatistics(boolean clearHistory) {
        lastTimerPop = System.currentTimeMillis();
        previousCompleted = threadPool == null ? 0 : threadPool.getCompletedTaskCount();
        previousThroughput = 0;

        consecutiveQueueEmptyCount = 0;
        consecutiveNoAdjustment = 0;
        consecutiveOutlierAfterAdjustment = 0;

        consecutiveIdleCount = 0;

        if (clearHistory) {
            threadStats = new TreeMap<Integer, ThroughputDistribution>();
        }

        lastAction = LastAction.NONE;
    }

    /**
     * Reset all statistics associated with the thread pool and reset the pool
     * size to a value that's based on the number of hardware threads available
     * to the JVM.
     */
    void resetThreadPool() {
        if (threadPool == null)
            return; // if no pool (during shutdown), nothing to retune/reset

        // 8/22/2012: Introduced factor - was hard coded at 2
        final int availableProcessors = numberCpus;

        int factor = 2500 * availableProcessors / Math.max(1, (int) previousThroughput);
        factor = Math.min(factor, 4);
        factor = Math.max(factor, 2);
        int newThreads = Math.min(factor * availableProcessors, maxThreads);
        newThreads = Math.max(newThreads, coreThreads);
        currentMinimumPoolSize = coreThreads;

        targetPoolSize = newThreads;
        setPoolSize(newThreads);

        resetStatistics(true);
    }

    /**
     * Deactivate the controller. Any scheduled tasks will be canceled.
     */
    synchronized void deactivate() {
        paused = false;
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
        this.threadPool = null;
    }

    /**
     * Pause any recurring timed events related to monitoring and
     * controlling the pool. This is intended to prevent idle CPU
     * consumption when no work is executing.
     */
    synchronized void pause() {
        paused = true;
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    /**
     * Reactivate the thread pool controller if it was paused.
     */
    @Trivial
    void resumeIfPaused() {
        if (paused) {
            resume();
        }
    }

    /**
     * Resume monitoring and control of the associated thread pool.
     */
    synchronized void resume() {
        paused = false;
        if (activeTask == null) {
            activeTask = new IntervalTask(this);
            timer.schedule(activeTask, interval, interval);
        }
    }

    /**
     * Get the throughput distribution data associated with the specified
     * number of active threads.
     *
     * @param numberOfThreads the number of active threads when the data was
     *                            collected
     *
     * @param create          whether to create and return a new throughput distribution
     *                            if none currently exists
     *
     * @return the data representing the throughput distribution for the
     *         specified number of active threads
     */
    @Trivial
    ThroughputDistribution getThroughputDistribution(int numberOfThreads, boolean create) {
        if (numberOfThreads < coreThreads)
            numberOfThreads = coreThreads;
        Integer threads = Integer.valueOf(numberOfThreads);
        ThroughputDistribution throughput = threadStats.get(threads);
        if ((throughput == null) && create) {
            throughput = new ThroughputDistribution();
            throughput.setLastUpdate(controllerCycle);
            threadStats.put(threads, throughput);
        }
        return throughput;
    }

    /**
     * Determine whether or not the thread pool has been idle long enough to
     * pause the monitoring task.
     *
     * @param threadPool        a reference to the thread pool
     * @param intervalCompleted the tasks completed this interval
     *
     * @return true if the controller has been paused
     */
    boolean manageIdlePool(ThreadPoolExecutor threadPool, long intervalCompleted) {

        // Manage the intervalCompleted count
        if (intervalCompleted == 0 && threadPool.getActiveCount() == 0) {
            consecutiveIdleCount++;
        } else {
            consecutiveIdleCount = 0;
        }

        if (consecutiveIdleCount >= IDLE_INTERVALS_BEFORE_PAUSE) {
            pause();
            lastAction = LastAction.PAUSE;
            return true;
        }

        return false;
    }

    /**
     * Detect and handle aberrant data points by resetting the statistics
     * in the throughput distribution.
     *
     * @param distribution the throughput distribution associated with throughput
     * @param throughput   the observed throughput
     *
     * @return true if the thread pool has been reset due to an aberrant
     *         workload
     */
    boolean handleOutliers(ThroughputDistribution distribution, double throughput) {
        if (throughput < 0.0) {
            resetStatistics(false);
            return true;
        } else if (throughput == 0.0) {
            return false;
        }

        double zScore = distribution.getZScore(throughput);
        boolean currentIsOutlier = zScore <= -3.0 || zScore >= 3.0;

        // 8/10/2012: Reset the data for this thread count when we hit an outlier
        // 1/20/2018: refine the distribution reset criteria
        if (currentIsOutlier) {
            /*
             * Decide whether to reset the distribution, which throws away the historical
             * ewma for the poolSize and replaces it with the new throughput.
             * We will use 3 criteria, any of which is sufficient to reset the distribution:
             *
             * 1) How much do we trust the historical data?
             * If the historical ewma is the result of many observations with similar throughput,
             * the standard deviation will be a small fraction of the ewma. If stddev/ewma is
             * greater than 10%, then the historical data is not really strong, let's reset.
             *
             * 2) How much different is the new tput from the ewma?
             * If the new throughput is very very different from the ewma, that suggests the workload
             * may have changed significantly, in which case the historical data would no longer be
             * valid. If the throughput change is greater than 50% of ewma, let's reset.
             *
             * 3) Is the throughput simply unstable?
             * If every new datapoint at this poolSize is more than 3 standard deviations off the
             * historical ewma, then we may as well follow the bouncing ball, rather than averaging
             * points which do not seem to want to cluster around a mean. If we get N outliers in a
             * row at this poolSize, let's reset.
             */
            double ewma = distribution.getMovingAverage();
            double stddev = distribution.getStddev();
            if ((stddev / ewma) > resetDistroStdDevEwmaRatio
                || (Math.abs(throughput - ewma) / ewma) > resetDistroNewTputEwmaRatio
                || distribution.incrementAndGetConsecutiveOutliers() >= resetDistroConsecutiveOutliers) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "reset distribution", (" distribution: " + distribution + ", new throughput: " + throughput));
                }
                distribution.reset(throughput, controllerCycle);
                distributionReset = true;
            } else if (tc.isEventEnabled()) {
                Tr.event(tc, "outlier detected", (" distribution: " + distribution + ", new throughput: " + throughput));
            }
        } else {
            distribution.resetConsecutiveOutliers();
        }

        // Check for repeated outliers
        // 1/20/2018: increment only after resetting a distribution, not a single outlier event
        if (lastAction != LastAction.NONE) {
            if (distributionReset) {
                consecutiveOutlierAfterAdjustment++;
            } else {
                consecutiveOutlierAfterAdjustment = 0;
            }
        }

        // If we repeatedly hit an outlier after changing the pool size
        // we should reset the statistics
        if (consecutiveOutlierAfterAdjustment >= MAX_OUTLIER_AFTER_CHANGE_BEFORE_RESET) {
            resetThreadPool();
            return true;
        }

        return false;
    }

    /**
     * Calculate a score that roughly represents the probability that a
     * thread pool with fewer threads will have higher throughput than the
     * forecast. In addition to the probability calculated from the distribution,
     * the current size of the pool and the number of consecutive times we've
     * observed an empty thread pool queue will cause the score to change.
     *
     * @param poolSize    the current thread pool size
     * @param forecast    the throughput forecast at the current thread pool size
     * @param throughput  the throughput of the current interval
     * @param cpuHigh     true if current cpu usage exceeds the 'high' threshold
     * @param lowActivity true if pool activity is low and queue is empty
     * @param systemCpuNA true if systemCpu is not available/valid
     *
     * @return the shrink score
     */
    double getShrinkScore(int poolSize, double forecast, double throughput, boolean cpuHigh, boolean lowActivity, boolean systemCpuNA) {
        double shrinkScore = 0.0;
        double shrinkMagic = 0.0;
        boolean flippedCoin = false;
        int downwardCompareSpan = 0;

        if (poolSize >= currentMinimumPoolSize + poolDecrement) {
            // compareSpan is poolSize range used for throughput comparison
            downwardCompareSpan = Math.min(compareRange * poolDecrement, poolSize - currentMinimumPoolSize);

            // if poolSize already close to currentMinimumPoolSize, we can skip some shrinkScore tweaks
            boolean smallPool = ((poolSize - currentMinimumPoolSize) <= downwardCompareSpan);

            // average the probabilityGreaterThan results for all valid (not too old or out-of-range)
            // throughput data for compareRange smaller poolSizes ... discard invalid data found enroute
            Integer shrinkKey = threadStats.lowerKey(poolSize);
            Integer priorKey = Integer.valueOf(poolSize);
            int smallerPools = 0;
            int pruneLimit = currentMinimumPoolSize;
            while (true) {
                // stop if we run out of data
                if (shrinkKey == null)
                    break;
                priorKey = shrinkKey;
                shrinkKey = threadStats.lowerKey(shrinkKey);
                int distance = 0;
                boolean pruned = false;
                boolean inHangBuffer = true;
                // discard invalid data (old or out-of-range) found in comparison stats
                ThroughputDistribution priorStats = threadStats.get(priorKey);
                distance = poolSize - priorKey.intValue();
                if (priorKey > pruneLimit) {
                    inHangBuffer = false;
                    if (pruneData(priorStats, forecast)) {
                        threadStats.remove(priorKey);
                        pruned = true;
                    }
                }
                if (!pruned) {
                    // found a valid smaller poolSize, so include that data in the shrinkScore
                    smallerPools++;
                    shrinkScore += priorStats.getProbabilityGreaterThan(forecast);
                    if (inHangBuffer) {
                        // stop when we get compareRange datapoints
                        if (smallerPools >= compareRange)
                            break;
                    } else {
                        // stop if reached/passed compareSpan
                        if (distance >= downwardCompareSpan)
                            break;
                    }
                }
            }

            // if we didn't find compareRange datapoints, flip a coin
            if ((smallerPools < compareRange) && (!smallPool)) {
                shrinkScore += (flipCoin()) ? 0.7 : 0.0;
                smallerPools++;
                flippedCoin = true;
            }
            // average the aggregated shrinkScore
            shrinkScore /= smallerPools;

            if (consecutiveQueueEmptyCount > 0) {
                // Unless we reduced the pool size and tput went down, add the shrink magic
                if (!(lastAction == LastAction.SHRINK && throughput < previousThroughput)) {
                    // Add a little extra weight to shrinking the pool if we keep seeing an empty queue
                    shrinkMagic = Math.min(consecutiveQueueEmptyCount * EMPTY_QUEUE_SHRINK_MAGIC_PER_INTERVAL, 0.5);
                    shrinkScore += shrinkMagic;
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "shrinkMagic info", (" shrinkMagic added: " + shrinkMagic));
                    }
                }
            }

            // lean slightly toward shrinking if pool activity is low
            if (shrinkScore < 0.5 && lowActivity) {
                shrinkScore = (flipCoin()) ? 0.5 : shrinkScore;
            }

            // lean toward shrinking if cpuUtil is high
            if ((shrinkScore < 0.5) && (poolSize > currentMinimumPoolSize) && (!smallPool)) {
                if (cpuHigh || (flippedCoin && systemCpuNA)) {
                    shrinkScore = (flipCoin()) ? 0.7 : shrinkScore;
                } else {
                    if (flippedCoin) {
                        // shrink more eagerly if we randomly decided not to shrink (flippedCoin) and
                        // we have data showing that tput is not much better at much larger pool size
                        try {
                            // potentially different compare span when looking at larger poolSizes
                            int upwardCompareSpan = Math.min(compareRange * poolIncrement, maxThreads - poolSize);
                            Integer largestPoolSize = getLargestValidPoolSize(poolSize, forecast);
                            // only make this check if data is available well beyond compareSpan
                            if ((largestPoolSize - poolSize) > (upwardCompareSpan * compareSpanRatioMultiplier))
                                if (leanTowardShrinking(poolSize, largestPoolSize, forecast, threadStats.get(largestPoolSize).getMovingAverage()))
                                    shrinkScore = 0.7;
                        } catch (Exception e) {
                            FFDCFilter.processException(e, getClass().getName(), "getShrinkScore - largestPoolSize", this);
                        }
                    } else {
                        // shrink more eagerly if we are not very inclined to shrink based on compareRange data,
                        // but we have much smaller poolSize data showing similar tput to current larger size
                        try {
                            Integer smallestPoolSize = getSmallestValidPoolSize(poolSize, forecast);
                            // only make this check if data is available well beyond compareSpan
                            if ((poolSize - smallestPoolSize) > (downwardCompareSpan * compareSpanRatioMultiplier))
                                if (leanTowardShrinking(smallestPoolSize, poolSize, threadStats.get(smallestPoolSize).getMovingAverage(), forecast))
                                    shrinkScore = 0.7;
                        } catch (Exception e) {
                            FFDCFilter.processException(e, getClass().getName(), "getShrinkScore - smallestPoolSize", this);
                        }
                    }
                }
            } else { // shrinkScore > 0.5
                // if we are shrinking based on no data (coin flip) and the queue is deep,
                // be less eager to shrink ...
                if (flippedCoin && queueDepth > poolSize * 4)
                    shrinkScore = (flipCoin()) ? 0.0 : shrinkScore;
            }
            // Filter out small shrinkScores
            /**
             * Nov 2018 - moved this to the growScore/shrinkScore comparison in evaluateInterval
             * if (shrinkScore < shrinkScoreFilterLevel) {
             * if (tc.isEventEnabled()) {
             * Tr.event(tc, "shrinkScore pruning", (" shrinkScore " + shrinkScore + " pruned"));
             * }
             * shrinkScore = 0.0;
             * }
             */

        }
        return shrinkScore;

    }

    /**
     * Calculate a score that roughly represents the probability that a thread
     * pool with up to compareRange more threads will have higher throughput than
     * the forecast.
     *
     * @param poolSize    the current thread pool size
     * @param forecast    the throughput forecast at the current thread pool size
     * @param throughput  the throughput of the current interval
     * @param cpuHigh     true if current cpu usage exceeds the 'high' threshold
     * @param lowActivity true if pool activity is low and queue is empty
     * @param systemCpuNA true if systemCpu is not available/valid
     *
     * @return the grow score
     */
    double getGrowScore(int poolSize, double forecast, double throughput, boolean cpuHigh, boolean lowActivity, boolean systemCpuNA) {
        double growScore = 0.0;
        boolean flippedCoin = false;
        int upwardCompareSpan = 0;
        // Don't grow beyond max or when pool activity is low
        if (poolSize + poolIncrement <= maxThreads && !lowActivity) {
            // compareSpan is the poolSize range used for throughput comparison
            upwardCompareSpan = Math.min(compareRange * poolIncrement, maxThreads - poolSize);
            // average the probabilityGreaterThan results for all valid (not too old or out-of-range)
            // throughput data for compareRange larger poolSizes ... discard invalid data found enroute
            Integer growKey = threadStats.higherKey(poolSize);
            Integer priorKey = Integer.valueOf(poolSize);
            int largerPools = 0;
            while (true) {
                int distance = 0;
                // stop if we run out of data
                if (growKey == null)
                    break;
                priorKey = growKey;
                growKey = threadStats.higherKey(growKey);
                // discard invalid data (old or out-of-range)
                ThroughputDistribution priorStats = threadStats.get(priorKey);
                distance = priorKey - poolSize;
                if (pruneData(priorStats, forecast)) {
                    threadStats.remove(priorKey);
                } else {
                    // found valid data, add it to the score
                    largerPools++;
                    growScore += priorStats.getProbabilityGreaterThan(forecast);
                    // stop if reached/passed compareSpan
                    if (distance >= upwardCompareSpan)
                        break;
                }
            }

            // if we didn't find compareRange datapoints, flip a coin
            if (largerPools < compareRange) {
                growScore += (flipCoin()) ? 0.7 : 0.0;
                largerPools++;
                flippedCoin = true;
            }
            // average the results for the larger poolSize data found
            growScore /= largerPools;

            ThroughputDistribution currentStats = getThroughputDistribution(poolSize, false);
            ThroughputDistribution growStats = getThroughputDistribution(poolSize + poolIncrement, false);
            // 8/8/2012: Don't grow if there's a significant probability we'll shrink immediately
            if (currentStats != null && growStats != null) {
                if (growScore < 0.5 && currentStats.getProbabilityGreaterThan(growStats.getMovingAverage()) >= 0.5) {
                    growScore = 0.0;
                }
            }

            // grow less eagerly based on no data when cpuUtil is high or systemCpu is not available/valid
            if ((cpuHigh || systemCpuNA) && growScore > 0.0 && flippedCoin) {
                if (poolSize > currentMinimumPoolSize) {
                    growScore = (flipCoin()) ? growScore : 0.0;
                }
            } else if (growScore == 0 && flippedCoin && (queueDepth > poolSize * 4)) {
                // if we are not growing due to no larger pool data, and there is a lot of
                // work waiting, lean weakly (0.5) toward growing ...
                growScore = (flipCoin()) ? 0.5 : 0.0;
            } else
            // next are sanity checks to reduce likelihood of random growth, if a broader-scope
            // look at the available data suggests growth is not very likely to help
            if (growScore > 0.0) {
                if (flippedCoin) {
                    // we decided to grow based on no larger poolSize data (flippedCoin), so let's
                    // grow less eagerly if much smaller poolSize data shows not much smaller tput
                    try {
                        // potentially different compare span when looking at smaller poolSizes
                        int downwardCompareSpan = Math.min(compareRange * poolDecrement, poolSize - currentMinimumPoolSize);
                        Integer smallestPoolSize = getSmallestValidPoolSize(poolSize, forecast);
                        // only make this check if data is available well beyond compareSpan
                        if ((poolSize - smallestPoolSize) > (downwardCompareSpan * compareSpanRatioMultiplier)) {
                            if (leanTowardShrinking(smallestPoolSize, poolSize, threadStats.get(smallestPoolSize).getMovingAverage(), forecast))
                                growScore = 0.0;
                        }
                    } catch (Exception e) {
                        FFDCFilter.processException(e, getClass().getName(), "getGrowScore - smallestPoolSize", this);
                    }
                } else {
                    if (growScore < 0.5) {
                        // We are weakly inclined to grow (< 0.5) based on compareRange data .. if we have
                        // data for much larger poolSizes showing tput is not much better, we can be even
                        // less eager to grow
                        try {
                            Integer largestPoolSize = getLargestValidPoolSize(poolSize, forecast);
                            // only make this check if data is available well beyond compareSpan
                            if ((largestPoolSize - poolSize) > (upwardCompareSpan * compareSpanRatioMultiplier)) {
                                if (leanTowardShrinking(poolSize, largestPoolSize, forecast, threadStats.get(largestPoolSize).getMovingAverage()))
                                    growScore = 0.0;
                            }
                        } catch (Exception e) {
                            FFDCFilter.processException(e, getClass().getName(), "getGrowScore - largestPoolSize", this);
                        }
                    }
                }
            }
            /**
             * Nov 2018 - moved this to the growScore/shrinkScore comparison in evaluateInterval
             * // Filter out small growScores
             * if (growScore < growScoreFilterLevel) {
             * if (tc.isEventEnabled()) {
             * Tr.event(tc, "growScore pruning", (" growScore " + growScore + " pruned"));
             * }
             * growScore = 0.0;
             * }
             */
        }
        return growScore;
    }

    /**
     * Force an adjustment to the thread pool size if the change wouldn't shrink the
     * pool to zero or grow it beyond {@link maxThreads}.
     *
     * @param poolSize             the current pool size
     * @param calculatedAdjustment the adjustment calculated by grow and shrink scores
     * @param intervalCompleted    the number of tasks completed in the current interval
     * @param lowActivity          true when pool activity is low and queue is empty
     *
     * @return the pool adjustment size to use
     */
    @Trivial
    int forceVariation(int poolSize, int calculatedAdjustment, long intervalCompleted, boolean lowActivity) {
        // 08/08/2012: Count intervals without change
        if (calculatedAdjustment == 0 && intervalCompleted != 0) {
            consecutiveNoAdjustment++;
        } else {
            consecutiveNoAdjustment = 0;
        }

        int forcedAdjustment = calculatedAdjustment;
        if (consecutiveNoAdjustment >= MAX_INTERVALS_WITHOUT_CHANGE) {
            consecutiveNoAdjustment = 0;
            if (flipCoin() && poolSize + poolIncrement <= maxThreads) {
                // don't force an increase when pool activity is low
                if (!lowActivity) {
                    forcedAdjustment = poolIncrement;
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "force variation", (" forced increase: " + forcedAdjustment));
                    }
                }
            } else if ((poolSize - poolDecrement) >= currentMinimumPoolSize) {
                forcedAdjustment = -poolDecrement;
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "force variation", (" forced decrease: " + forcedAdjustment));
                }
            }
        }

        return forcedAdjustment;
    }

    /**
     * Perform a logical coin flip.
     *
     * @return true if the coin lands heads-up
     */
    boolean flipCoin() {
        return Math.random() >= 0.5;
    }

    /**
     * Adjust the size of the thread pool.
     *
     * @param poolSize       the current pool size
     * @param poolAdjustment the change to make to the pool
     *
     * @return the new pool size
     */
    int adjustPoolSize(int poolSize, int poolAdjustment) {
        if (threadPool == null)
            return poolSize; // arguably should return 0, but "least change" is safer... This happens during shutdown.

        int newPoolSize = poolSize + poolAdjustment;
        lastAction = LastAction.NONE;

        if (poolAdjustment != 0) {
            // don't shrink too far
            if (poolAdjustment < 0 && newPoolSize >= Math.max(activeThreads, currentMinimumPoolSize)) {
                lastAction = LastAction.SHRINK;
                setPoolSize(newPoolSize);
            } else if (poolAdjustment > 0 && newPoolSize <= maxThreads) {
                lastAction = LastAction.GROW;
                setPoolSize(newPoolSize);
            } else {
                newPoolSize = poolSize;
            }
        }

        return newPoolSize;
    }

    /**
     * Evaluate the throughput for the current interval and apply heuristics
     * to modify the thread pool size in an attempt to maximize throughput.
     */
    synchronized String evaluateInterval() {
        // During shutdown, threadpool may have been nulled out. In that case, don't bother analyzing.
        // (We could log this in FFDC, but it isn't clear that it's worth doing so.)
        if (threadPool == null)
            return "threadPool == null";

        int poolSize = threadPool.getPoolSize();

        // Bail when there are no threads in the pool (unlikely)
        if (poolSize <= 0) {
            return "poolSize <= 0";
        }

        // we can't even think about adjusting the pool size until the underlying executor has aggressively
        // grown the pool to the coreThreads value, so if that hasn't happened yet we should just bail
        if (poolSize < coreThreads) {
            return "poolSize < coreThreads";
        }

        long currentTime = System.currentTimeMillis();
        long completedWork = threadPool.getCompletedTaskCount();

        // Calculate work done for the current interval
        long deltaTime = Math.max(currentTime - lastTimerPop, interval);
        long deltaCompleted = completedWork - previousCompleted;
        double throughput = 1000.0 * deltaCompleted / deltaTime;
        try {
            queueDepth = threadPool.getQueue().size();
            boolean queueEmpty = (queueDepth <= 0);
            activeThreads = threadPool.getActiveCount();

            // Count the number of consecutive times we've seen an empty queue
            if (!queueEmpty) {
                consecutiveQueueEmptyCount = 0;
            } else if (lastAction != LastAction.SHRINK) { // 9/5/2012
                consecutiveQueueEmptyCount++;
            }

            // update cpu utilization info
            processCpuUtil = CpuInfo.getJavaCpuUsage();
            systemCpuUtil = CpuInfo.getSystemCpuUsage();
            cpuUtil = Math.max(systemCpuUtil, processCpuUtil);

            boolean cpuHigh = (cpuUtil > highCpu);
            boolean systemCpuNA = (systemCpuUtil < 0);

            // Handle pausing the task if the pool has been idle
            if (manageIdlePool(threadPool, deltaCompleted)) {
                return "monitoring paused";
            }

            if (resolveHang(deltaCompleted, queueEmpty, poolSize, cpuHigh)) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Executor hang detected - poolSize: " + poolSize + ", activeThreads: " + activeThreads +
                                 ", queueDepth: " + queueDepth + ", cpuUtil: " + df.format(cpuUtil) + ", processCpuUtil: " +
                                 df.format(processCpuUtil) + ", systemCpuUtil: " + df.format(systemCpuUtil));
                }
                /**
                 * Sleep the controller thread briefly after increasing the pool size
                 * then update task count before returning to reduce the likelihood
                 * of a false negative hang check next cycle due to a few non-hung
                 * tasks executing on the newly created threads
                 */
                try {
                    Thread.sleep(10);
                } catch (Exception ex) {
                    // do nothing
                }
                completedWork = threadPool.getCompletedTaskCount();
                return "action take to resolve hang";
            }

            if (checkTargetPoolSize(poolSize)) {
                return "poolSize != targetPoolSize";
            }

            controllerCycle++;
            ThroughputDistribution currentStats = getThroughputDistribution(poolSize, true);

            // handleOutliers will mark this 'true' if it resets the distribution
            distributionReset = false;

            // Reset statistics based on abnormal data points
            if (handleOutliers(currentStats, throughput)) {
                return "aberrant workload";
            }

            // If the distribution was reset we don't need to add the datapoint because
            // handleOutliers already did that.
            // If throughput was 0 we will not include that datapoint
            if (!distributionReset && throughput > 0) {
                currentStats.addDataPoint(throughput, controllerCycle);
            }

            boolean lowActivity = false;
            if (queueEmpty && ((throughput < (poolSize * lowTputThreadsRatio)) || (activeThreads < (poolSize * activeThreadsGrowthRatio)))) {
                lowActivity = true;
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "low activity flag set: throughput: " + df.format(throughput) + ", poolSize: "
                                 + poolSize + ", activeThreads: " + activeThreads + ", queueDepth: " + queueDepth +
                                 ", tasks completed: " + deltaCompleted);
                }
            }

            setPoolIncrementDecrement(poolSize);

            double forecast = currentStats.getMovingAverage();
            double shrinkScore = getShrinkScore(poolSize, forecast, throughput, cpuHigh, lowActivity, systemCpuNA);
            double growScore = getGrowScore(poolSize, forecast, throughput, cpuHigh, lowActivity, systemCpuNA);

            // Adjust the poolsize only if one of the scores is both larger than the scoreFilterLevel
            // and sufficiently larger than the other score. These conditions reduce poolsize fluctuation
            // which might arise due to a weak or noisy signal from the historical throughput data.
            int poolAdjustment = 0;
            if (growScore >= growScoreFilterLevel && (growScore - growShrinkDiffFilter) > shrinkScore) {
                poolAdjustment = poolIncrement;
            } else if (shrinkScore >= shrinkScoreFilterLevel && (shrinkScore - growShrinkDiffFilter) > growScore) {
                poolAdjustment = -poolDecrement;
            }

            // Force some random variation into the pool size algorithm
            poolAdjustment = forceVariation(poolSize, poolAdjustment, deltaCompleted, lowActivity);

            // Format an event level trace point with the most useful data
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Interval data", toIntervalData(throughput, forecast, deltaCompleted, shrinkScore, growScore,
                                                             poolSize, poolAdjustment));
            }

            // Change the pool size and save the result, will check it at start of next control cycle
            targetPoolSize = adjustPoolSize(poolSize, poolAdjustment);

        } finally {
            lastTimerPop = currentTime;
            previousCompleted = completedWork;
            previousThroughput = throughput;
        }
        return "";
    }

    /**
     * Utility method used to format interval level statistic trace points.
     */
    @Trivial
    private String toIntervalData(double throughput, double forecast, long deltaCompleted, double shrinkScore,
                                  double growScore, int poolSize, int poolAdjustment) {
        final int RANGE = 25;

        StringBuilder sb = new StringBuilder();
        sb.append("\nThroughput:");
        sb.append(String.format(" previous = %.6f", Double.valueOf(previousThroughput)));
        sb.append(String.format(" current = %.6f", Double.valueOf(throughput)));
        sb.append(String.format(" forecast = %.6f", Double.valueOf(forecast)));
        sb.append(String.format(" tasks this interval = %6d", Long.valueOf(deltaCompleted)));

        sb.append("\nHeuristics:");
        sb.append(String.format(" queueDepth = %8d", Integer.valueOf(queueDepth)));
        sb.append(String.format(" consecutiveQueueEmptyCount = %2d", Integer.valueOf(consecutiveQueueEmptyCount)));
        sb.append(String.format(" consecutiveNoAdjustment = %2d", Integer.valueOf(consecutiveNoAdjustment)));

        sb.append("\nOutliers:  ");
        sb.append(String.format(" consecutiveOutlierAfterAdjustment = %2d", Integer.valueOf(consecutiveOutlierAfterAdjustment)));
        sb.append(String.format(" hangBufferPoolSize = %2d", Integer.valueOf(hangBufferPoolSize)));

        sb.append("\nAttraction:");
        sb.append(String.format(" shrinkScore = %.6f", Double.valueOf(shrinkScore)));
        sb.append(String.format(" growScore = %.6f", Double.valueOf(growScore)));
        sb.append(String.format(" lastAction = %s", lastAction));

        sb.append("\nCPU:");
        sb.append(String.format(" cpuUtil = %.2f", Double.valueOf(cpuUtil)));
        sb.append(String.format(" processCpuUtil = %.2f", Double.valueOf(processCpuUtil)));
        sb.append(String.format(" systemCpuUtil = %.2f", Double.valueOf(systemCpuUtil)));

        sb.append("\nIncrement:");
        sb.append(String.format(" poolSize = %2d", Integer.valueOf(poolSize)));
        sb.append(String.format(" activeThreads = %2d", Integer.valueOf(activeThreads)));
        sb.append(String.format(" poolIncrement = %2d", Integer.valueOf(poolIncrement)));
        sb.append(String.format(" poolDecrement = %2d", Integer.valueOf(poolDecrement)));
        sb.append(String.format(" compareRange = %2d", Integer.valueOf(compareRange)));

        sb.append("\nConfig:");
        sb.append(String.format(" coreThreads = %2d", Integer.valueOf(coreThreads)));
        sb.append(String.format(" maxThreads = %2d", Integer.valueOf(maxThreads)));
        sb.append(String.format(" currentMinimumPoolSize = %2d", Integer.valueOf(currentMinimumPoolSize)));

        sb.append("\nStatistics:\n");

        Integer[] poolSizes = new Integer[2 * RANGE + 1];
        ThroughputDistribution[] tputDistros = new ThroughputDistribution[2 * RANGE + 1];
        Integer poolSizeInteger = Integer.valueOf(poolSize);
        int start = RANGE;
        int end = RANGE;
        poolSizes[RANGE] = poolSizeInteger;
        tputDistros[RANGE] = getThroughputDistribution(poolSize, false);
        Integer prior = threadStats.lowerKey(poolSizeInteger);
        Integer next = threadStats.higherKey(poolSizeInteger);
        for (int i = 1; i <= RANGE; i++) {
            if (prior != null) {
                start--;
                poolSizes[start] = prior;
                tputDistros[start] = getThroughputDistribution(prior, false);
                prior = threadStats.lowerKey(prior);
            }
            if (next != null) {
                end++;
                poolSizes[end] = next;
                tputDistros[end] = getThroughputDistribution(next, false);
                next = threadStats.higherKey(next);
            }
        }
        for (int i = start; i <= end; i++) {
            sb.append(String.format("%s%3d threads: %s%n", (poolSizes[i] == poolSizeInteger) ? "-->" : "   ", poolSizes[i], String.valueOf(tputDistros[i])));
        }

        if (poolAdjustment == 0) {
            sb.append("### No pool adjustment ###");
        } else if (poolAdjustment < 0) {
            sb.append("--- Shrinking to " + (poolSize + poolAdjustment) + " ---");
        } else {
            sb.append("+++ Growing to " + (poolSize + poolAdjustment) + " +++");
        }

        return sb.toString();
    }

    /**
     * Utility method to format pool tput ratio data
     */
    private String poolTputRatioData(double poolTputRatio, double poolRatio, double tputRatio,
                                     double smallerPoolTput, double largerPoolTput, int smallerPoolSize, int largerPoolSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n ");
        sb.append(String.format(" poolTputRatio: %.3f", Double.valueOf(poolTputRatio)));
        sb.append(String.format(" poolRatio: %.3f", Double.valueOf(poolRatio)));
        sb.append(String.format(" tputRatio: %.3f", Double.valueOf(tputRatio)));
        sb.append("\n ");
        sb.append(String.format(" smallerPoolSize: %d", Integer.valueOf(smallerPoolSize)));
        sb.append(String.format(" largerPoolSize: %d", Integer.valueOf(largerPoolSize)));
        sb.append(String.format(" smallerPoolTput: %.3f", Double.valueOf(smallerPoolTput)));
        sb.append(String.format(" largerPoolTput: %.3f", Double.valueOf(largerPoolTput)));
        return sb.toString();
    }

    /**
     * Detects a hang in the underlying executor. When a hang is detected, increases the
     * poolSize in hopes of relieving the hang, unless poolSize has reached maxThreads.
     *
     * @return true if action was taken to resolve a hang, or false otherwise
     */
    private boolean resolveHang(long tasksCompleted, boolean queueEmpty, int poolSize,
                                boolean highCpu) {
        boolean poolHung = (tasksCompleted == 0 && !queueEmpty);
        if (poolHung) {
            /**
             * When a hang is detected the controller enters hang resolution mode.
             * The controller will run on a shorter-than-usual cycle for hangResolutionCycles
             * from the last hang detection, to resolve hang situations more quickly.
             */
            if (hangResolutionCountdown == 0) {
                // cancel regular controller schedule
                activeTask.cancel();
                // restart with shortened interval for quicker hang resolution
                activeTask = new IntervalTask(this);
                timer.schedule(activeTask, hangInterval, hangInterval);
            }
            hangResolutionCountdown = hangResolutionCycles;
            controllerCyclesWithoutHang = 0;

            // if this is the first time we detected a given deadlock, record how many threads there are
            // and print a message
            if (poolSizeWhenHangDetected < 0) {
                poolSizeWhenHangDetected = poolSize;
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Executor hang detected at poolSize=" + poolSizeWhenHangDetected);
                }
            } else if (tc.isEventEnabled()) {
                Tr.event(tc, "Executor hang continued at poolSize=" + poolSize);
            }

            /**
             * If the current pool size does not match the target set last time through, we will skip this
             * cycle to give the system time to adjust the threads to the target number.
             */

            if (!checkTargetPoolSize(poolSize)) {
                if (!highCpu) {
                    setPoolIncrementDecrement(poolSize);
                    if (poolSize + poolIncrement <= maxThreads && poolSize < maxThreadsToBreakHang) {
                        targetPoolSize = adjustPoolSize(poolSize, poolIncrement);
                        if (tc.isEventEnabled()) {
                            Tr.event(tc, "Increasing pool size to resolve hang, from " + poolSize + " to " + targetPoolSize);
                        }
                        // update the poolSize set to resolve the hang, plus one-increment buffer
                        int targetSize = poolSize + poolIncrement;
                        if (hangBufferPoolSize < targetSize) {
                            hangBufferPoolSize = targetSize;
                            currentMinimumPoolSize = hangBufferPoolSize;
                        }

                    } else {
                        // there's a hang, but we can't add any more threads...  emit a warning the first time this
                        // happens for a given hang, but otherwise just bail
                        if (hangMaxThreadsMessageEmitted == false && hangIntervalCounter > 0) {
                            if (tc.isWarningEnabled()) {
                                Tr.warning(tc, "unbreakableExecutorHang", poolSizeWhenHangDetected, poolSize);
                            }
                            hangMaxThreadsMessageEmitted = true;
                        }
                    }
                } else {
                    /**
                     * This is a 'hung-busy' state - work in queue and no tasks completed, but lots of cpu
                     * being used (either by the java process or the underlying system, or both).
                     * Since cpu is high, let's not keep adding threads, since more threads are unlikely to
                     * help accomplish useful work, when there are few free cpu cyles available.
                     */
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "Executor hung but no action taken because highCpu: " + highCpu);
                    }
                }
                hangIntervalCounter++;
            }
        } else {
            // no hang exists, so reset the appropriate variables that track hangs
            poolSizeWhenHangDetected = -1;
            hangIntervalCounter = 0;
            hangMaxThreadsMessageEmitted = false;
            // manage hang resolution mode
            if (hangResolutionCountdown > 0) {
                hangResolutionCountdown--;
                if (hangResolutionCountdown <= 0) {
                    // move out of hang resolution cycle time
                    activeTask.cancel();
                    // restart using regular cycle time
                    activeTask = new IntervalTask(this);
                    timer.schedule(activeTask, interval, interval);
                }
            }
            /**
             * if controller is running at or below hangBufferPoolSize marker without hanging,
             * we can reduce that marker ... the workload must have changed, so prior hang setting
             * is no longer valid. We will reduce it gradually, to maintain a conservative stance
             * toward avoiding hangs.
             */
            if (hangBufferPoolSize > coreThreads) {
                if (hangBufferPoolSize >= poolSize) {
                    controllerCyclesWithoutHang++;
                    if (controllerCyclesWithoutHang > noHangCyclesThreshold) {
                        setPoolIncrementDecrement(poolSize);
                        hangBufferPoolSize -= poolDecrement;
                        currentMinimumPoolSize = hangBufferPoolSize;
                        controllerCyclesWithoutHang = 0;
                    }
                }
            }
        }
        return poolHung;
    }

    private void setPoolSize(int newPoolSize) {
        // As of Java 9, the corePoolSize must be <= maxPoolSize at all times.
        // Need to observe current core pool size to avoid violating this rule.
        if (newPoolSize < threadPool.getCorePoolSize()) {
            threadPool.setCorePoolSize(newPoolSize);
            threadPool.setMaximumPoolSize(newPoolSize);
        } else {
            threadPool.setMaximumPoolSize(newPoolSize);
            threadPool.setCorePoolSize(newPoolSize);
        }
    }

    /**
     * Evaluates a ThroughputDistribution for possible removal from the historical dataset.
     *
     * @param priorStats - ThroughputDistribution under evaluation
     * @param forecast   - expected throughput at the current poolSize
     * @return - true if priorStats should be removed
     */
    private boolean pruneData(ThroughputDistribution priorStats, double forecast) {
        boolean prune = false;

        // if forecast tput is much greater or much smaller than priorStats, we suspect
        // priorStats is no longer relevant, so prune it
        double tputRatio = forecast / priorStats.getMovingAverage();
        if (tputRatio > tputRatioPruneLevel || tputRatio < (1 / tputRatioPruneLevel)) {
            prune = true;
        } else {
            // age & reliability (represented by standard deviation) check
            int age = controllerCycle - priorStats.getLastUpdate();
            double variability = (priorStats.getStddev() / priorStats.getMovingAverage());
            if (age * variability > dataAgePruneLevel)
                prune = true;
        }

        return prune;
    }

    /**
     * Returns the smallest valid poolSize in the current historical dataset.
     *
     * @param poolSize - current poolSize
     * @param forecast - expected throughput at current poolSize
     * @return - smallest valid poolSize found
     */
    private Integer getSmallestValidPoolSize(Integer poolSize, Double forecast) {
        Integer smallestPoolSize = threadStats.firstKey();
        Integer nextPoolSize = threadStats.higherKey(smallestPoolSize);
        Integer pruneSize = -1;
        boolean validSmallData = false;
        while (!validSmallData && nextPoolSize != null) {
            ThroughputDistribution smallestPoolSizeStats = getThroughputDistribution(smallestPoolSize, false);;
            // prune data that is too old or outside believable range
            if (pruneData(smallestPoolSizeStats, forecast)) {
                pruneSize = smallestPoolSize;
                smallestPoolSize = nextPoolSize;
                nextPoolSize = threadStats.higherKey(smallestPoolSize);
                if (pruneSize > currentMinimumPoolSize) {
                    threadStats.remove(pruneSize);
                }
            } else {
                validSmallData = true;
            }
        }
        return smallestPoolSize;
    }

    /**
     * Returns the largest valid poolSize in the current historical dataset.
     *
     * @param poolSize - current poolSize
     * @param forecast - expected throughput at current poolSize
     * @return - largest valid poolSize found
     */
    private Integer getLargestValidPoolSize(Integer poolSize, Double forecast) {
        Integer largestPoolSize = -1;
        // find largest poolSize with valid data
        boolean validLargeData = false;
        while (!validLargeData) {
            largestPoolSize = threadStats.lastKey();
            ThroughputDistribution largestPoolSizeStats = getThroughputDistribution(largestPoolSize, false);;
            // prune any data that is too old or outside believable range
            if (pruneData(largestPoolSizeStats, forecast)) {
                threadStats.remove(largestPoolSize);
            } else {
                validLargeData = true;
            }
        }
        return largestPoolSize;
    }

    /**
     * Evaluate current poolSize against farthest poolSize to decide whether it makes sense
     * to shrink. The final outcome is probabilistic, not deterministic.
     *
     * @param smallerPoolSize - smaller poolSize for comparison
     * @param largerPoolSize  - larger poolSize for comparison
     * @param smallerPoolTput - tput (historical or expected) of smaller poolSize
     * @param largerPoolTput  - tput (historical or expected) of larger poolSize
     * @return - true if the ratios and coinFlips favor shrinking
     */
    private boolean leanTowardShrinking(Integer smallerPoolSize, int largerPoolSize,
                                        double smallerPoolTput, double largerPoolTput) {
        boolean shouldShrink = false;
        double poolRatio = largerPoolSize / smallerPoolSize;
        double tputRatio = largerPoolTput / smallerPoolTput;
        double poolTputRatio = poolRatio / tputRatio;

        // compare the poolSize ratio and tput ratio between current and largest poolSizes
        // if tput no better at larger poolSize, or not much better, lean toward shrinking
        if (tputRatio < 1.0) {
            // much larger poolSize has smaller tput - lean strongly (75%) toward shrinking
            shouldShrink = (flipCoin() && flipCoin()) ? false : true;
        } else if (poolTputRatio > poolTputRatioHigh) {
            // poolSize ratio is much larger than tput ratio - lean strongly (75%) toward shrinking
            shouldShrink = (flipCoin() && flipCoin()) ? false : true;
        } else if (poolTputRatio > poolTputRatioLow) {
            // poolSize ratio is slightly larger than tput ratio - lean weakly (50%) toward shrinking
            shouldShrink = (flipCoin()) ? false : true;
        }
        // Format an event level trace point with the key tput ratio data
        if (tc.isEventEnabled() && shouldShrink)
            Tr.event(tc, "Tput ratio shrinkScore adjustment, larger poolSizes",
                     poolTputRatioData(poolTputRatio, poolRatio, tputRatio, smallerPoolTput,
                                       largerPoolTput, smallerPoolSize, largerPoolSize));
        return shouldShrink;
    }

    private void setPoolIncrementDecrement(int poolSize) {
        /**
         * We need a special case when maxThreads is set relatively close to coreThreads
         * We want the controller to be able to move among a range of values that allow
         * reasonably fine-grained adjustment.
         * threadRange == (maxThreads - coreThreads)
         */

        if (threadRange < (poolChangeBasis * minimumDesiredPoolSizeAdjustments)) {
            if (threadRange < minimumDesiredPoolSizeAdjustments) {
                poolIncrement = 1;
                poolDecrement = 1;
            } else {
                poolIncrement = threadRange / minimumDesiredPoolSizeAdjustments;
                poolDecrement = poolIncrement;
            }
        } else {
            // set poolIncrement/poolDecrement based on current poolSize and number of cpus
            if (poolSize <= poolIncrementBoundLow) {
                poolIncrement = poolChangeBasis;
                poolDecrement = poolIncrement;
            } else if (poolSize <= poolIncrementBoundMedium) {
                poolIncrement = poolChangeBasis * 2;
                poolDecrement = poolIncrement;
                // special case when we are at the edge of increment size change
                if (poolSize == (poolIncrementBoundLow + poolChangeBasis))
                    poolDecrement = poolChangeBasis;
            } else {
                poolIncrement = poolChangeBasis * 4;
                poolDecrement = poolIncrement;
                // special case when we are at the edge of increment size change
                if (poolSize == (poolIncrementBoundMedium + poolChangeBasis * 2))
                    poolDecrement = poolChangeBasis * 2;
            }
        }

        // this allows poolIncrement to be config-bounded
        if (poolIncrementMin != POOL_INCREMENT_MIN_DEFAULT || poolIncrementMax != POOL_INCREMENT_MAX_DEFAULT) {
            if (poolIncrementMin <= poolIncrementMax) {
                poolIncrement = Math.max(poolIncrement, poolIncrementMin);
                poolIncrement = Math.min(poolIncrement, poolIncrementMax);
                poolDecrement = poolIncrement;
            }
        }

        /**
         * ... special case for maxThreads ...
         * We need to make sure poolIncrement will not go past maxThreads if we grow,
         * but will allow us to hit maxThreads exactly, since maxThreads may be a
         * configured value that does not fit exactly into our increment multiples.
         * Also need to make sure that if the poolSize is at maxThreads, poolDecrement is
         * set to the right value to land back on the next lower datapoint when shrinking.
         */

        if (poolSize + poolIncrement > maxThreads) {
            if (poolSize == maxThreads) {
                poolDecrement = maxThreadsPoolDecrement;
            } else {
                poolIncrement = maxThreads - poolSize;
            }
        }

        if (poolSize + poolIncrement == maxThreads) {
            maxThreadsPoolDecrement = poolIncrement;
        }

        /**
         * ... similar special case for currentMinimumPoolSize ...
         * We *should* never hit this, but just in case ... makes sure that we can
         * decrement exactly to currentMinimumPoolSize.
         */

        if (poolSize - poolDecrement < currentMinimumPoolSize) {
            if (poolSize > currentMinimumPoolSize) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "poolDecrement vs currentMinimumPoolSize check", (" poolSize " + poolSize +
                                                                                   " , poolDecrement: " + poolDecrement + ", coreThreads: " + coreThreads
                                                                                   + " , currentMinimumPoolSize: " + currentMinimumPoolSize));
                }
                poolDecrement = poolSize - currentMinimumPoolSize;
            }
        }
    }

    /**
     * We set targetPoolSize last time thru - if pool is not at that size, system may
     * need more time to create/delete the necessary threads, so skip this interval
     * If this happens repeatedly, maybe the prior setPoolSize call failed somehow,
     * so try again
     */
    private boolean checkTargetPoolSize(int poolSize) {
        boolean poolSizeCheckFailed = false;

        if (poolSize != targetPoolSize) {
            poolSizeCheckFailed = true;
            if (tc.isEventEnabled()) {
                Tr.event(tc, "targetPoolSize check", (" poolSize " + poolSize + " != targetPoolSize " + targetPoolSize));
            }
            consecutiveTargetPoolSizeWrong++;
            if (consecutiveTargetPoolSizeWrong >= MAX_CONSECUTIVE_TARGET_POOLSIZE_WRONG) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "consecutiveTargetPoolSize check", (" consecutiveTargetPoolSizeWrong " + consecutiveTargetPoolSizeWrong +
                                                                     " exceeds threshold " + MAX_CONSECUTIVE_TARGET_POOLSIZE_WRONG + ", calling setPoolSize(targetPoolSize)"));
                }
                /**
                 * If we are trying to reduce the pool size and failing repeatedly, probably it means all
                 * the threads in the pool are doing work - threads cannot be removed from the pool while
                 * they are busy. In this case, we probably should not have been trying to remove threads
                 * from the pool anyway, so let's increase our target by an increment (while not exceeding
                 * maxThreads)
                 */
                if (targetPoolSize < poolSize) {
                    setPoolIncrementDecrement(targetPoolSize);
                    if (targetPoolSize + poolIncrement <= maxThreads) {
                        targetPoolSize += poolIncrement;
                    } else {
                        targetPoolSize = maxThreads;
                    }
                }
                setPoolSize(targetPoolSize);
                consecutiveTargetPoolSizeWrong = 0;
            }
        } else {
            consecutiveTargetPoolSizeWrong = 0;
        }

        return poolSizeCheckFailed;
    }

    /**
     * report variable values that may be set by system property
     */
    private void reportSystemProperties() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n interval: ").append(String.format("%6d", Long.valueOf(interval)));
        sb.append(" hangInterval: ").append(String.format("%6d", Long.valueOf(hangInterval)));
        sb.append(" compareRange: ").append(String.format("%6d", Integer.valueOf(compareRange)));
        sb.append(" highCpu: ").append(String.format("%4d", Integer.valueOf(highCpu)));

        sb.append("\n tputRatioPruneLevel: ").append(String.format("%2.2f", Double.valueOf(tputRatioPruneLevel)));
        sb.append(" poolTputRatioHigh: ").append(String.format("%2.2f", Double.valueOf(poolTputRatioHigh)));
        sb.append(" poolTputRatioLow: ").append(String.format("%2.2f", Double.valueOf(poolTputRatioLow)));
        sb.append(" compareSpanRatioMultiplier: ").append(String.format("%3d", Integer.valueOf(compareSpanRatioMultiplier)));

        sb.append("\n growScoreFilterLevel: ").append(String.format("%2.2f", Double.valueOf(growScoreFilterLevel)));
        sb.append(" shrinkScoreFilterLevel: ").append(String.format("%2.2f", Double.valueOf(shrinkScoreFilterLevel)));
        sb.append(" growShrinkDiffFilter: ").append(String.format("%2.2f", Double.valueOf(growShrinkDiffFilter)));
        sb.append(" dataAgePruneLevel: ").append(String.format("%2.2f", Double.valueOf(dataAgePruneLevel)));
        sb.append(" compareSpanPruneMultiplier: ").append(String.format("%3d", Integer.valueOf(compareSpanPruneMultiplier)));

        sb.append("\n poolIncrementMin: ").append(String.format("%3d", Integer.valueOf(poolIncrementMin)));
        sb.append(" poolIncrementMax: ").append(String.format("%4d", Integer.valueOf(poolIncrementMax)));
        sb.append(" poolIncrementBoundLow: ").append(String.format("%4d", Integer.valueOf(poolIncrementBoundLow)));
        sb.append(" poolIncrementBoundMedium: ").append(String.format("%4d", Integer.valueOf(poolIncrementBoundMedium)));
        sb.append(" minimumDesiredPoolSizeAdjustments : ").append(String.format("%4d", Integer.valueOf(minimumDesiredPoolSizeAdjustments)));

        sb.append("\n resetDistroStdDevEwmaRatio: ").append(String.format("%2.2f", Double.valueOf(resetDistroStdDevEwmaRatio)));
        sb.append(" resetDistroNewTputEwmaRatio: ").append(String.format("%2.2f", Double.valueOf(resetDistroNewTputEwmaRatio)));
        sb.append(" resetDistroConsecutiveOutliers: ").append(String.format("%2.2f", Double.valueOf(resetDistroConsecutiveOutliers)));

        Tr.event(tc, "Initial config settings:", sb);
    }

    /**
     * privileged access to read system properties
     *
     */
    private static final String getSystemProperty(final String propName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propName);
            }
        });
    }

    synchronized void introspect(PrintWriter out) {
        final String INDENT = "  ";
        out.println(this.getClass().getName());
        out.println(INDENT + "coreThreads = " + coreThreads);
        out.println(INDENT + "maxThreads = " + maxThreads);
        out.println(INDENT + "currentMinimumPoolSize = " + currentMinimumPoolSize);
        out.println(INDENT + "interval = " + interval);
        out.println(INDENT + "compareRange = " + compareRange);
        out.println(INDENT + "NUMBER_CPUS = " + numberCpus);
        out.println(INDENT + "controllerCycle = " + controllerCycle);
        out.println(INDENT + "poolChangeBasis = " + poolChangeBasis);
        out.println(INDENT + "poolIncrement = " + poolIncrement);
        out.println(INDENT + "poolDecrement = " + poolDecrement);
        out.println(INDENT + "poolIncrementMax = " + poolIncrementMax);
        out.println(INDENT + "poolIncrementMin = " + poolIncrementMin);
        out.println(INDENT + "targetPoolSize = " + targetPoolSize);
        out.println(INDENT + "consecutiveTargetPoolSizeWrong = " + consecutiveTargetPoolSizeWrong);
        out.println(INDENT + "highCpu = " + highCpu);
        out.println(INDENT + "lowTputThreadsRatio = " + lowTputThreadsRatio);
        out.println(INDENT + "dataAgePruneLevel = " + dataAgePruneLevel);
        out.println(INDENT + "growScoreFilterLevel = " + growScoreFilterLevel);
        out.println(INDENT + "shrinkScoreFilterLevel = " + shrinkScoreFilterLevel);
        out.println(INDENT + "growShrinkDiffFilter = " + growShrinkDiffFilter);
        out.println(INDENT + "resetDistroStdDevEwmaRatio = " + resetDistroStdDevEwmaRatio);
        out.println(INDENT + "resetDistroNewTputEwmaRatio = " + resetDistroNewTputEwmaRatio);
        out.println(INDENT + "resetDistroConsecutiveOutliers = " + resetDistroConsecutiveOutliers);
        out.println(INDENT + "paused = " + paused);
        out.println(INDENT + "hangIntervalCounter = " + hangIntervalCounter);
        out.println(INDENT + "poolSizeWhenHangDetected = " + poolSizeWhenHangDetected);
        out.println(INDENT + "lastAction = " + lastAction);
        out.println(INDENT + "lastTimerPop = " + lastTimerPop);
        out.println(INDENT + "previousCompleted = " + previousCompleted);
        out.println(INDENT + "consecutiveIdleCount = " + consecutiveIdleCount);
        out.println(INDENT + "consecutiveNoAdjustment = " + consecutiveNoAdjustment);
        out.println(INDENT + "consecutiveOutlierAfterAdjustment = " + consecutiveOutlierAfterAdjustment);
        out.println(INDENT + "consecutiveQueueEmptyCount = " + consecutiveQueueEmptyCount);
        out.println(INDENT + "threadPool");
        out.println(INDENT + INDENT + "poolSize = " + threadPool.getPoolSize());
        out.println(INDENT + INDENT + "queueDepth = " + queueDepth);
        out.println(INDENT + INDENT + "activeCount = " + threadPool.getActiveCount());
        out.println(INDENT + INDENT + "corePoolSize = " + threadPool.getCorePoolSize());
        out.println(INDENT + INDENT + "maxPoolSize = " + threadPool.getMaximumPoolSize());
        out.println(INDENT + INDENT + "largestPoolSize = " + threadPool.getLargestPoolSize());
        out.println(INDENT + INDENT + "completedTaskCount = " + threadPool.getCompletedTaskCount());

    }
}

/**
 * Timer task that drives the controller each time an interval
 * expires.
 */
class IntervalTask extends TimerTask {

    final ThreadPoolController threadPoolController;

    IntervalTask(ThreadPoolController threadPoolController) {
        this.threadPoolController = threadPoolController;
    }

    @Override
    public void run() {
        try {
            threadPoolController.evaluateInterval();
        } catch (Throwable t) {
            // Don't let any odd exceptions escape. BCI FFDC only.
        }
    }
}
