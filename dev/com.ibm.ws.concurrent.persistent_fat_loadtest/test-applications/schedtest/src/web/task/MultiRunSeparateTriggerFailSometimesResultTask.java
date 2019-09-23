/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.task;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.inject.Inject;

import web.TaskResultStore;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * A task with a separate trigger. Runs multiple times and returns a result or fails with exception.
 */
public class MultiRunSeparateTriggerFailSometimesResultTask implements ManagedTask, Callable<Long>, Serializable {

    /**  */
    private static final long serialVersionUID = -1518110544430087145L;

    /**
     * The default interval when the task should run, in milliseconds.
     */
    private static final long DEFAULT_INTERVAL = 250L; // Every 1/4 second

    /**
     * The exception string prefix, thrown when the task fails.
     */
    private static final String EXCEPTION_PREFIX = "Throwing the expected exception on run number ";

    /**
     * The 'run complete' object.
     */
    private static final Object RUN_COMPLETE = new Object();

    /**
     * The time this task was initialized. We use this time to base when the Trigger
     * should stop scheduling executions.
     */
    private long initTimeNanos = 0L;

    /**
     * The number of seconds this task should be scheduled for.
     */
    private long durationSeconds = 0;

    /**
     * The value that we return when the task runs.
     */
    private long returnValue;

    /**
     * The ID of the result representing this task in the result store;
     */
    private int resultID = 0;

    /**
     * The number of times this task has run. We assume the task will be re-persisted
     * to the database task store after every successful run.
     */
    private int runCount = 0;

    /**
     * The value of System.nanoTime() from the last successful run. We assume the task
     * will be re-persisted to the database task store after every successful run.
     */
    private long lastSuccessNanoTime = 0L;

    /**
     * Test result tracker.
     */
    @Inject
    private TaskResultStore taskResultStore;

    /**
     * Constructor for CDI
     */
    public MultiRunSeparateTriggerFailSometimesResultTask() {}

    /**
     * Initialize the task by getting an ID from the result store, and creating
     * a Result object in the result store which will hold the results when the
     * task runs.
     * 
     * Note that this task needs to be manually initialized (not a CDI method).
     * 
     * @param durationSeconds The number of seconds this task should be active for.
     * 
     * @return The ID that the task will use in the result store.
     */
    public int initialize(long durationSeconds) {
        initTimeNanos = System.nanoTime();
        returnValue = System.nanoTime();
        this.durationSeconds = durationSeconds;
        resultID = taskResultStore.createResult(new Result(DEFAULT_INTERVAL, durationSeconds, returnValue, taskResultStore.isDebugMode()));
        return resultID;
    }

    /**
     * Create a trigger for this task.
     */
    public Trigger createTrigger() {
        return new Trigger(TimeUnit.SECONDS.toMillis(durationSeconds), initTimeNanos);
    }

    /**
     * Return either a pre-defined value, or an exception, depending on the time that
     * this task runs.
     * 
     * On every 100th run, throw an exception. This is trickier than it sounds. The task
     * itself is serialized after it runs, so it can change its state and keep track of
     * which run we're on. However on a failure, the state is not serialized. What we'll
     * do is keep both the run number and the previous run time in state variables. On
     * every 100th run, we'll throw an exception for the first two seconds. After
     * that, we'll go back to success. The task should run at least twice in the first
     * two seconds. The first run of course the first, then the second occurs immediately
     * since the first retry is immediate. The second retry will occur after the retry
     * interval, which we'll set to be fairly small, but not so small that we blow out
     * the retry count.
     */
    @Override
    public Long call() throws Exception {
        runCount++;
        long secondsSinceLastSuccess = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastSuccessNanoTime);
        if (((runCount % 100) == 0) && (secondsSinceLastSuccess < 2)) {
            // We need to report this exception manually.  The unmanaged checker task can't
            // see the exception, because it's not reported unless the persistent executor
            // decides it's not going to run this task anymore.
            Exception e = new Exception(EXCEPTION_PREFIX + runCount);
            taskResultStore.taskExecuted(resultID, true, e);
            throw e;
        }

        lastSuccessNanoTime = System.nanoTime();
        return returnValue;
    }

    /**
     * Get our execution properties. Always auto-purge the result (it's our cue to stop).
     * 
     * @see javax.enterprise.concurrent.ManagedTask#getExecutionProperties()
     */
    @Override
    public Map<String, String> getExecutionProperties() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        return props;
    }

    /**
     * We don't have a listener.
     * 
     * @see javax.enterprise.concurrent.ManagedTask#getManagedTaskListener()
     */
    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    /**
     * An unmanaged task which generates the results for the MultiRunTriggerResultTask.
     */
    public static class ResultGenerator implements Runnable {

        public static final int INTERVAL_SECONDS = 5;
        private final int resultID;
        private final long taskID;
        private final TaskResultStore resultStore;
        private boolean gotFirstResult = false;

        /**
         * Constructor.
         */
        public ResultGenerator(int resultID, long taskID, TaskResultStore resultStore) {
            this.resultID = resultID;
            this.taskID = taskID;
            this.resultStore = resultStore;
        }

        /**
         * Post the result to the result store. Schedule another run 5 seconds from now.
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if (resultStore.isDebugMode()) {
                System.out.println(" !!TDK unmanaged run at " + new Date());
            }

            PersistentExecutor pe = resultStore.getPeristentExecutor();
            ScheduledExecutorService se = resultStore.getUnmanagedScheduledExecutor();

            // Is the task present in the persistent executor's tables?
            TaskStatus<Long> status = pe.getStatus(taskID);
            if (status != null) {
                if (status.hasResult() == true) {
                    if (resultStore.isDebugMode()) {
                        System.out.println(" !!TDK got a result");
                    }
                    gotFirstResult = true;
                    try {
                        // Add our result.  Then re-schedule until the task is removed
                        // from the persistent store.
                        Long result = status.getResult();
                        if (result != null) {
                            resultStore.taskExecuted(resultID, true, result);
                        }
                        se.schedule(this, INTERVAL_SECONDS, TimeUnit.SECONDS);
                    } catch (ExecutionException ee) {
                        // Something went wrong...
                        Throwable t = ee.getCause();
                        if ((t != null) && (t instanceof Exception) && (t.getMessage().contains(EXCEPTION_PREFIX))) {
                            resultStore.taskExecuted(resultID, true, t);
                            se.schedule(this, INTERVAL_SECONDS, TimeUnit.SECONDS);
                        } else {
                            if (resultStore.isDebugMode())
                                System.out.println(" !!TDK MultiRunTriggerResultTask failed (" + System.identityHashCode(this) + ") Result: " + ee.toString());
                            resultStore.taskExecuted(resultID, false, ee.toString());
                        }
                    }
                } else {
                    // Re-schedule if we're waiting for our first result.
                    if (gotFirstResult == false) {
                        se.schedule(this, INTERVAL_SECONDS, TimeUnit.SECONDS);
                    } else {
                        // TODO: Is there a window where the returned TaskStatus can go away?
                        resultStore.taskExecuted(resultID, false, "No result, subsequent iteration");
                    }
                }
            } else {
                // Not known to persistent executor, post a failure if this is the first run.
                if (gotFirstResult == false) {
                    resultStore.taskExecuted(resultID, false, "No results at all.");
                } else {
                    // The run is finished - the task was removed from the task store.
                    resultStore.taskExecuted(resultID, true, RUN_COMPLETE);
                }
            }
        }
    }

    /**
     * A separate trigger object.
     */
    public static class Trigger implements javax.enterprise.concurrent.Trigger, Serializable {

        /**  */
        private static final long serialVersionUID = 8554707338314706024L;

        /**
         * The nanos value after which we should not schedule more executions.
         * initTime + durationTime - DEFAULT_INTERVAL
         */
        private final long lastRunTimeNanos;

        /**
         * Constructor
         */
        private Trigger(long durationMillis, long initTimeNanos) {
            final long durationNanos = TimeUnit.MILLISECONDS.toNanos(durationMillis);
            final long intervalNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_INTERVAL);
            lastRunTimeNanos = initTimeNanos + durationNanos - intervalNanos;
        }

        /**
         * Schedule our next run time. If we think we can run before the test duration ends, then
         * schedule another run. Otherwise, return null, which is our signal that the task is
         * finished.
         * 
         * @see javax.enterprise.concurrent.Trigger#getNextRunTime(javax.enterprise.concurrent.LastExecution, java.util.Date)
         */
        @Override
        public Date getNextRunTime(LastExecution arg0, Date arg1) {
            // We don't really care when the last execution was.  We're always going to schedule our next
            // run to be after the default interval from now.
            long nextRunEpoch = System.currentTimeMillis() + DEFAULT_INTERVAL;
            return (System.nanoTime() < lastRunTimeNanos) ? new Date(nextRunEpoch) : null;
        }

        /**
         * We never skip a run.
         * 
         * @see javax.enterprise.concurrent.Trigger#skipRun(javax.enterprise.concurrent.LastExecution, java.util.Date)
         */
        @Override
        public boolean skipRun(LastExecution arg0, Date arg1) {
            return false;
        }
    }

    /**
     * The result for the multi-run separate trigger fail sometimes task (with result). The result value should be
     * the current value of System.nanoTime()
     */
    public static class Result implements TaskResultStore.Result {

        /**
         * The number of milliseconds between (expected) task runs.
         */
        private final long intervalMillis;

        /**
         * The number of seconds that we expect the task to be 'active' for. By active, we
         * mean scheduling itself to run again, before it ends and ceases to run.
         */
        private final long durationSeconds;

        /**
         * An indicator that the run is complete.
         */
        private boolean runEnded = false;

        /**
         * A list representing each time the task was executed.
         */
        private final List<Object> successfulRunTimes = new LinkedList<Object>() {
            private static final long serialVersionUID = 78L;

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(this.size()).append(" : [");
                for (Object o : this) {
                    if (o instanceof Long)
                        sb.append(o).append(", ");
                    else if (o instanceof Exception)
                        sb.append("Exception").append(", ");
                    else
                        sb.append("*UNKNOWN*").append(", ");
                }
                sb.setCharAt(sb.lastIndexOf(","), ']');
                return sb.toString().trim();
            };
        };

        /**
         * The time of the first error.
         */
        private Date firstErrorTime = null;

        /**
         * The details of the first error.
         */
        private String firstErrorDetails = null;

        /**
         * The total number of errors;
         */
        private int errorCount = 0;

        /**
         * The return value we'll be checking for in the results.
         */
        private final long returnValue;

        /**
         * Are we in debugging mode?
         */
        private final boolean debugMode;

        /**
         * The last reason the error check failed.
         */
        private String lastErrorCheckReason = null;

        /**
         * Constructor.
         * 
         * @param interval The number of milliseconds between task executions.
         * @param duration The number of seconds that we expect the task to run.
         * @param returnValue The value that should be in the result when it's added.
         */
        private Result(long interval, long duration, long returnValue, boolean debugMode) {
            intervalMillis = interval;
            durationSeconds = duration;
            this.returnValue = returnValue;
            this.debugMode = debugMode;
        }

        /**
         * Stores the result of this recurring task.
         */
        @Override
        public void add(Date executionTime, boolean success, Object data) {
            if (debugMode) {
                System.out.println(" !!TDK adding result: " + executionTime + ", " + success + ", " + data);
            }

            // If successful, and if data is a Long, we'll add the run to our results.
            if ((success) && (data != null)) {
                if (runEnded == false) {
                    if ((data instanceof Long) || (data instanceof Exception)) {
                        successfulRunTimes.add(data);
                    } else if (data.equals(RUN_COMPLETE)) {
                        runEnded = true;
                    }
                } else {
                    errorCount++;
                    if (firstErrorTime == null) {
                        firstErrorTime = executionTime;
                        firstErrorDetails = "Result received after completion: " + data.toString();
                    }
                }
            } else {
                // This is an unsuccessful run.
                errorCount++;
                if (firstErrorTime == null) {
                    firstErrorTime = executionTime;
                    firstErrorDetails = (data instanceof String) ? (String) data : "No details provided";
                }
            }
        }

        @Override
        public boolean check() {
            lastErrorCheckReason = null;

            // Check for obvious errors.
            if ((errorCount > 0) || (runEnded == false)) {
                lastErrorCheckReason = "errorCount greater than zero or runEnded equals false";
                return false;
            }

            return true;
        }

        /**
         * Print the results.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("MultiRunSeparateTriggerFailSometimesResultTask.Result ");
            sb.append("durationSeconds = ").append(durationSeconds);
            sb.append(", intervalMillis = ").append(intervalMillis);
            sb.append(", runEnded = ").append(runEnded);
            sb.append(", errorCount = ").append(errorCount);
            sb.append(", lastErrorCheckReason = ").append(lastErrorCheckReason);
            if (firstErrorTime != null) {
                sb.append(", firstErrorTime = ").append(firstErrorTime);
                sb.append(", firstErrorDetails = ").append(firstErrorDetails);
            }
            sb.append(", returnValue = ").append(returnValue);
            sb.append(", successfulRunTimes = ").append(successfulRunTimes.toString());
            return sb.toString();
        }
    }

}
