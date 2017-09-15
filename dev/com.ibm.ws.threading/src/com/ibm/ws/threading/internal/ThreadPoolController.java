/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

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
    final static long INTERVAL = 1500;

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
    final static int MAX_INTERVALS_WITHOUT_CHANGE = 5;

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
     * A <em>magic</em> factor that can increase the grow score when we
     * repeatedly observe a non-empty queue feeding the thread pool. This
     * will help push the pool towards growth.
     * <p>
     * If this value is too large, the pool will grow too large for optimal
     * throughput.
     */
    // 8/17/2012: Introduced
    final static double NON_EMPTY_QUEUE_GROW_MAGIC_PER_INTERVAL = EMPTY_QUEUE_SHRINK_MAGIC_PER_INTERVAL / 2.0;

    /**
     * The number of consecutive idle thread pool intervals of the controller
     * will remain active before pausing.
     */
    final static int IDLE_INTERVALS_BEFORE_PAUSE = 3;

    /**
     * If we repeatedly encounter a situation where the first throughput
     * measurement after changing the size of the thread pool is an outlier,
     * the workload (or something on the system) may have changed enough to
     * warrant a complete reset of the historical data. This value represents
     * the number of times that we encounter that situation before we do the
     * reset.
     */
    final static int MAX_OUTLIER_AFTER_CHANGE_BEFORE_RESET = 3;

    /**
     * The limit to how many threads we will add to break the executor out of a hang.
     * The throughput algorithm can still add threads beyond this level if maxThreads
     * is set higher, but the hang prevention code will never go past this value
     * regardless of what maxThreads is set to. This is to prevent runaway thread
     * creation in the case where every new task immediately gets blocked by the same
     * underlying condition.
     */
    final static int MAX_THREADS_TO_BREAK_HANG = 1000;

    /**
     * Reference to the configured ExecutorService implementation that
     * delegates to the {@link ThreadPoolExecutorImpl} that is controlled
     * by this controller.
     */
    final ExecutorServiceImpl executorService;

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
    LastAction lastAction = LastAction.NONE;

    /**
     * A reference to the timer thread that schedules the interval task.
     */
    final Timer timer = new Timer("Executor Service Control Timer", true);

    /**
     * The active interval {@link TimerTask}.
     */
    IntervalTask activeTask = null;

    /**
     * An indication of whether or not the controller is paused because
     * the thread pool is idle.
     */
    boolean paused = false;

    /**
     * A time stamp representing the last time an interval was process.ed
     */
    long lastTimerPop = 0;

    /**
     * The completed task count from the previous interval.
     */
    long previousCompleted = 0;

    /**
     * The throughput (completed / second) observed over the previous interval.
     */
    double previousThroughput = 0;

    /**
     * A count of the consecutive intervals where we've observed an empty
     * queue. This value is used to influence the shrink score of an
     * interval in a way that attracts the pool to a smaller size.
     */
    int consecutiveQueueEmptyCount = 0;

    /**
     * A count of the consecutive intervals where not pool size adjustment
     * has been made. This value is used to determine when the controller
     * forces a change to the size of the pool to prevent getting "stuck"
     * at a position in the throughput curve that is no longer optimal.
     */
    int consecutiveNoAdjustment = 0;

    /**
     * A count of the number of consecutive times we've encountered an
     * abnormal data point after changing the pool size. This value is used
     * to determine when an abrupt workload change has occurred that should
     * cause historical data to be abandoned and the pool size to be reset.
     */
    int consecutiveOutlierAfterAdjustment = 0;

    /**
     * A count of the consecutive intervals where the thread pool has been
     * completely idle. This is used to determine when the interval monitoring
     * task should be paused.
     */
    int consecutiveIdleCount = 0;

    /**
     * The limit on the number of threads we can grow the pool to.
     */
    int maxThreads = Integer.MAX_VALUE;

    /**
     * The lower bound on the number of threads we can shrink the pool to.
     */
    int coreThreads = getDefaultCoreThreadSize();

    /**
     * The thread pool executor associated with the active executor service.
     */
    ThreadPoolExecutor threadPool;

    /**
     * Array of historical data representing observed throughput. The index
     * into the array is the number of active threads when the data was
     * collected.
     */
    ThroughputDistribution[] threadStats = new ThroughputDistribution[0];

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
     * Provides the default core thread size for when the controller is in
     * a deactivated state.
     */
    private static int getDefaultCoreThreadSize() {
        // Hard code a lower bound to make sure there's something
        return Math.max(Runtime.getRuntime().availableProcessors() / 2 - 1, 3);
    }

    /**
     * Constructor.
     * 
     * @param executorServce the configured OSGi component that's associated with
     *            the managed thread pool.
     */
    ThreadPoolController(ExecutorServiceImpl executorService) {
        this.executorService = executorService;
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
            threadStats = new ThroughputDistribution[0];
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
        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        int factor = 2500 * availableProcessors / Math.max(1, (int) previousThroughput);
        factor = Math.min(factor, 4);
        factor = Math.max(factor, 2);
        int newThreads = Math.min(factor * availableProcessors, maxThreads);
        newThreads = Math.max(newThreads, coreThreads);

        setPoolSize(newThreads);

        resetStatistics(true);
    }

    /**
     * Activate the controller. A timer will be scheduled to pop
     * at regular intervals to monitor the behavior of the pool and
     * make appropriate adjustments.
     */
    synchronized void activate(ThreadPoolExecutor pool) {
        this.threadPool = pool;
        this.coreThreads = pool.getCorePoolSize();
        this.maxThreads = pool.getMaximumPoolSize();
        resetStatistics(true);
        activeTask = new IntervalTask(this);
        timer.schedule(activeTask, INTERVAL, INTERVAL);
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
        this.coreThreads = getDefaultCoreThreadSize();
        this.maxThreads = Integer.MAX_VALUE;
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
            timer.schedule(activeTask, INTERVAL, INTERVAL);
        }
    }

    /**
     * Set the core thread count. The controller will not attempt
     * to shrink below this limit.
     */
    synchronized void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
    }

    /**
     * Set the maximum thread count. The controller will not attempt
     * to grow beyond this limit.
     * 
     * @param maxThreads the maximum number of threads to grow to
     */
    synchronized void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Get the throughput distribution data associated with the specified
     * number of active threads.
     * 
     * @param activeThreads the number of active threads when the data was
     *            collected
     * 
     * @return the data representing the throughput distribution for the
     *         specified number of active threads
     */
    ThroughputDistribution getThroughputDistribution(int activeThreads) {
        if (activeThreads >= threadStats.length) {
            threadStats = Arrays.copyOf(threadStats, activeThreads + 1);
        }
        if (threadStats[activeThreads] == null) {
            threadStats[activeThreads] = new ThroughputDistribution();
        }
        return threadStats[activeThreads];
    }

    /**
     * Determine whether or not the thread pool has been idle long enough to
     * pause the monitoring task.
     * 
     * @param threadPool a reference to the thread pool
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
     * @param throughput the observed throughput
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
        if (currentIsOutlier) {
            distribution.reset(throughput);
        }

        // Check for repeated outliers
        if (lastAction != LastAction.NONE && currentIsOutlier) {
            consecutiveOutlierAfterAdjustment++;
        } else if (lastAction != LastAction.NONE) {
            consecutiveOutlierAfterAdjustment = 0;
        }

        // If we repeated hit an outlier after changing the pool size
        // we should reset the statistics
        if (consecutiveOutlierAfterAdjustment >= MAX_OUTLIER_AFTER_CHANGE_BEFORE_RESET) {
            resetThreadPool();
            return true;
        }

        return false;
    }

    /**
     * Calculate a score that roughly represents the probability that a thread
     * pool with one less thread will have higher throughput than the forecast.
     * In addition to the probability calculated from the distribution, the
     * current size of the pool and the number of consecutive times we've
     * observed an empty thread pool queue will cause the score to change.
     * 
     * @param poolSize the current thread pool size
     * @param queueEmpty indication of whether or not the thread pool queue is empty
     * @param forecast the throughput forecast at the current thread pool size
     * @param throughput the throughput of the current interval
     * 
     * @return the shrink score
     */
    double getShrinkScore(int poolSize, boolean queueEmpty, double forecast, double throughput) {
        ThroughputDistribution shrinkStats = getThroughputDistribution(poolSize - 1);

        // Count the number of consecutive times we've seen an empty queue
        if (!queueEmpty || poolSize <= coreThreads) {
            consecutiveQueueEmptyCount = 0;
        } else if (lastAction != LastAction.SHRINK) { // 9/5/2012
            consecutiveQueueEmptyCount++;
        }

        // Add a little extra weight to shrinking the pool if we keep seeing an empty queue
        double shrinkMagic = Math.min(consecutiveQueueEmptyCount, 0.5 / EMPTY_QUEUE_SHRINK_MAGIC_PER_INTERVAL) * EMPTY_QUEUE_SHRINK_MAGIC_PER_INTERVAL;

        // If we reduced the pool size and the current throughput has gone down,
        // don't add the shrink magic
        if (consecutiveQueueEmptyCount > 0 && lastAction == LastAction.SHRINK && throughput < previousThroughput) {
            shrinkMagic = 0.0;
        }

        double shrinkScore = shrinkStats.getProbabilityGreaterThan(forecast) + shrinkMagic;

        // Don't shrink unless the score exceeds 50% and there are threads to discard
        if (shrinkScore <= 0.5 || poolSize <= coreThreads) {
            shrinkScore = 0.0;
        }

        return shrinkScore;
    }

    /**
     * Calculate a score that roughly represents the probability that a thread
     * pool with one more thread will have higher throughput than the forecast.
     * 
     * @param poolSize the current thread pool size
     * @param queueEmpty indication of whether or not the thread pool queue is empty
     * @param forecast the throughput forecast at the current thread pool size
     * @param throughput the throughput of the current interval
     * 
     * @return the grow score
     */
    double getGrowScore(int poolSize, boolean queueEmpty, double forecast, double throughput) {
        ThroughputDistribution growStats = getThroughputDistribution(poolSize + 1);

        // NOTE: The initial design would set the growScore to 0.0 if the queue was empty but
        // that didn't work well in scenarios where the throughput was higher with more threads
        // even when work wasn't queued.
        double growScore = growStats.getProbabilityGreaterThan(forecast);

        // 8/8/2012: Don't grow if there's a significant probability we'll shrink immediately
        ThroughputDistribution currentStats = getThroughputDistribution(poolSize);
        if (growScore < 0.5 && currentStats.getProbabilityGreaterThan(growStats.getMovingAverage()) >= 0.5) {
            growScore = 0.0;
        }

        // Don't grow beyond max
        if (poolSize >= maxThreads) {
            growScore = 0.0;
        }

        return growScore;
    }

    /**
     * Force an adjustment to the thread pool size if the change wouldn't shrink the
     * pool to zero or grow it beyond {@link maxThreads}.
     * 
     * @param poolSize the current pool size
     * @param calculatedAdjustment the adjustment calculated by grow and shrink scores
     * @param intervalCompleted the number of tasks completed in the current interval
     * 
     * @return the pool adjustment size to use
     */
    int forceVariation(int poolSize, int calculatedAdjustment, long intervalCompleted) {
        // 08/08/2012: Count intervals without change
        if (calculatedAdjustment == 0 && intervalCompleted != 0) {
            consecutiveNoAdjustment++;
        } else {
            consecutiveNoAdjustment = 0;
        }

        int forcedAdjustment = calculatedAdjustment;
        if (consecutiveNoAdjustment >= MAX_INTERVALS_WITHOUT_CHANGE) {
            consecutiveNoAdjustment = 0;
            if (flipCoin() && poolSize < maxThreads) {
                forcedAdjustment = 1;
            } else if (poolSize > coreThreads) {
                forcedAdjustment = -1;
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
     * @param poolSize the current pool size
     * @param poolAdjustment the change to make to the pool
     * 
     * @return the new pool size
     */
    int adjustPoolSize(int poolSize, int poolAdjustment) {
        if (threadPool == null)
            return poolSize; // arguably should return 0, but "least change" is safer... This happens during shutdown. 
        int newPoolSize = poolSize + poolAdjustment;

        if (poolAdjustment == 0) {
            lastAction = LastAction.NONE;
        } else if (poolAdjustment < 0) {
            lastAction = LastAction.SHRINK;
        } else {
            lastAction = LastAction.GROW;
        }

        // 08/08/2012: Count intervals without change
        if (poolAdjustment != 0)
            setPoolSize(newPoolSize);

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

        // we can't even think about adjusting the pool size until the underlying executor has aggressively
        // grown the pool to the coreThreads value, so if that hasn't happened yet we should just bail
        if (poolSize < coreThreads) {
            return "poolSize < coreThreads";
        }

        long currentTime = System.currentTimeMillis();
        long completedWork = threadPool.getCompletedTaskCount();

        // Calculate throughput for the current interval
        long deltaTime = Math.max(currentTime - lastTimerPop, INTERVAL);
        long deltaCompleted = completedWork - previousCompleted;
        double throughput = 1000.0 * deltaCompleted / deltaTime;
        boolean queueEmpty = threadPool.getQueue().isEmpty();

        // Handle pausing the task if the pool has been idle
        if (manageIdlePool(threadPool, deltaCompleted)) {
            return "monitoring paused";
        }

        // Bail when there are no threads in the pool (unlikely)
        if (poolSize <= 0) {
            return "poolSize <= 0";
        }

        ThroughputDistribution currentStats = getThroughputDistribution(poolSize);

        // Reset statistics based on abnormal data points
        if (handleOutliers(currentStats, throughput)) {
            return "aberrant workload";
        }

        if (resolveHang()) {
            return "action take to resolve hang";
        }

        // Only add information if we have a backlog of work because
        // we don't want intervals where nothing happens to skew this.
        //
        // 8/6: We've encountered many intervals where the queue was empty
        //      when we sampled the threads but that had a higher throughput
        //      than the moving average.  This caused us to use a slightly
        //      smaller pool than was optimal.  The hope is that this
        //      change will get us slightly better data points at the top
        //      of the throughput curve.
        if (!queueEmpty || (throughput > 0 && throughput >= currentStats.getMovingAverage())) {
            currentStats.addDataPoint(throughput);
        }

        double forecast = currentStats.getMovingAverage();
        double shrinkScore = getShrinkScore(poolSize, queueEmpty, forecast, throughput);
        double growScore = getGrowScore(poolSize, queueEmpty, forecast, throughput);

        // ASSERTION: If the work queue is empty, adding more threads won't help because we're
        //            already completing everything.
        // 8/8/2012: Added shrinkScore >= 0.5 check on the shrink
        int poolAdjustment = 0;
        if (growScore > shrinkScore) {
            poolAdjustment = 1;
        } else if (shrinkScore > growScore) {
            poolAdjustment = -1;
        }

        // Force some random variation into the pool size algorithm
        poolAdjustment = forceVariation(poolSize, poolAdjustment, deltaCompleted);

        // Format an event level trace point with the most useful data
        if (tc.isEventEnabled()) {
            Tr.event(tc, "Interval data", toIntervalData(throughput, forecast, shrinkScore, growScore, queueEmpty, poolSize, poolAdjustment));
        }

        // Change the pool size
        adjustPoolSize(poolSize, poolAdjustment);

        lastTimerPop = currentTime;
        previousCompleted = completedWork;
        previousThroughput = throughput;
        return "";
    }

    /**
     * Utility method used to format interval level statistic trace points.
     */
    @Trivial
    private String toIntervalData(double throughput, double forecast, double shrinkScore, double growScore, boolean queueEmpty, int poolSize, int poolAdjustment) {
        final int RANGE = 25;

        StringBuilder sb = new StringBuilder();
        sb.append("\nThroughput:");
        sb.append(String.format(" previous = %.6f", Double.valueOf(previousThroughput)));
        sb.append(String.format(" current = %.6f", Double.valueOf(throughput)));
        sb.append(String.format(" forecast = %.6f", Double.valueOf(forecast)));

        sb.append("\nHeuristics:");
        sb.append(String.format(" queueEmpty = %5s", Boolean.toString(queueEmpty)));
        sb.append(String.format(" consecutiveQueueEmptyCount = %2d", Integer.valueOf(consecutiveQueueEmptyCount)));
        sb.append(String.format(" consecutiveNoAdjustment = %2d", Integer.valueOf(consecutiveNoAdjustment)));

        sb.append("\nOutliers:  ");
        sb.append(String.format(" consecutiveOutlierAfterAdjustment = %2d", Integer.valueOf(consecutiveOutlierAfterAdjustment)));

        sb.append("\nAttraction:");
        sb.append(String.format(" shrinkScore = %.6f", Double.valueOf(shrinkScore)));
        sb.append(String.format(" growScore = %.6f", Double.valueOf(growScore)));
        sb.append(String.format(" lastAction = %s", lastAction));

        sb.append("\nStatistics:\n");
        for (int i = Math.max(0, poolSize - RANGE); i < poolSize; i++) {
            ThroughputDistribution distribution = getThroughputDistribution(i);
            sb.append(String.format("   %3d threads: %s%n", Integer.valueOf(i), String.valueOf(distribution)));
        }
        for (int i = poolSize; i < poolSize + RANGE && i < threadStats.length; i++) {
            ThroughputDistribution distribution = getThroughputDistribution(i);
            sb.append(String.format("%s%3d threads: %s%n", (i == poolSize) ? "-->" : "   ", Integer.valueOf(i), String.valueOf(distribution)));
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
     * Detects and resolves a hang in the underlying executor. If a hang is detected, this method
     * will not return until it is resolved or if the pool size has reached maxThreads.
     * 
     * @return true if action was taken to resolve a hang, or false otherwise
     */
    private boolean resolveHang() {
        boolean actionTaken = false;
        if (threadPool.getCompletedTaskCount() == previousCompleted && !threadPool.getQueue().isEmpty()) {
            int poolSize = threadPool.getPoolSize();

            // if this is the first time we detected a given deadlock, record how many threads there are
            // and print a message
            if (poolSizeWhenHangDetected < 0) {
                poolSizeWhenHangDetected = poolSize;
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Executor hang detected at poolSize=" + poolSizeWhenHangDetected, threadPool);
                }
            }

            if (poolSize < maxThreads && poolSize < MAX_THREADS_TO_BREAK_HANG) {
                poolSize += 1;
                setPoolSize(poolSize);
                actionTaken = true;
            }
            else {
                // there's a hang, but we can't add any more threads...  emit a warning the first time this
                // happens for a given hang, but otherwise just bail
                if (hangIntervalCounter == 1) {
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, "unbreakableExecutorHang", poolSizeWhenHangDetected, poolSize);
                    }
                }
            }
            hangIntervalCounter++;
        }
        else {
            // no hang exists, so reset the appropriate variables that track hangs
            poolSizeWhenHangDetected = -1;
            hangIntervalCounter = 0;
        }
        return actionTaken;
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

    synchronized void introspect(PrintWriter out) {
        final String INDENT = "  ";
        out.println(this.getClass().getName());
        out.println(INDENT + "coreThreads = " + coreThreads);
        out.println(INDENT + "maxThreads = " + maxThreads);
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
