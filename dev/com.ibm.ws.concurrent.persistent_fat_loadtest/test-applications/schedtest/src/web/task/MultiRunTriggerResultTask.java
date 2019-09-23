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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import javax.inject.Inject;

import web.TaskResultStore;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * A task that is also a Trigger. Runs multiple times and returns a result.
 */
public class MultiRunTriggerResultTask implements Trigger, Callable<Long>, Serializable {

    private static final long serialVersionUID = 4295941217592669572L;

    /**
     * The default interval when the task should run.
     */
    private static final long DEFAULT_INTERVAL = 2000L; // Every 2 seconds

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
     * The duration for which we should continue to schedule executions, in milliseconds.
     */
    private long durationMillis;

    /**
     * Test result tracker.
     */
    @Inject
    private TaskResultStore taskResultStore;

    /**
     * Constructor for CDI
     */
    public MultiRunTriggerResultTask() {}

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
        durationMillis = TimeUnit.SECONDS.toMillis(durationSeconds);
        return taskResultStore.createResult(new Result(DEFAULT_INTERVAL, durationSeconds));
    }

    /**
     * Return the current time (in nanos) when called.
     */
    @Override
    public Long call() throws Exception {
        return System.nanoTime();
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
        long estimatedTaskRunTimeNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DEFAULT_INTERVAL);
        return ((estimatedTaskRunTimeNanos - TimeUnit.MILLISECONDS.toNanos(durationMillis)) < initTimeNanos) ? new Date(nextRunEpoch) : null;
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

    /**
     * An unmanaged task which generates the results for the MultiRunTriggerResultTask.
     */
    public static class ResultGenerator implements Runnable {

        private final int resultID;
        private final long taskID;
        private final TaskResultStore resultStore;
        private long lastResult = 0L;

        /**
         * Constructor.
         */
        public ResultGenerator(int resultID, long taskID, TaskResultStore resultStore) {
            this.resultID = resultID;
            this.taskID = taskID;
            this.resultStore = resultStore;
        }

        /**
         * See if there's a result yet. If there is, and it's different from the last,
         * post it to the resultStore.
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            PersistentExecutor pe = resultStore.getPeristentExecutor();
            ScheduledExecutorService se = resultStore.getUnmanagedScheduledExecutor();

            // Is the task present in the persistent executor's tables?
            TaskStatus<Long> status = pe.getStatus(taskID);
            if (status != null) {
                if (status.hasResult() == true) {
                    try {
                        // Add our result if it's new, then re-schedule until the task is removed
                        // from the persistent store.
                        Long result = status.getResult();
                        if ((result != null) && (result != lastResult)) {
                            resultStore.taskExecuted(resultID, true, result);
                            lastResult = result;
                        }
                        se.schedule(this, 1, TimeUnit.SECONDS);
                    } catch (ExecutionException ee) {
                        // Something went wrong...
                        if (resultStore.isDebugMode())
                            System.out.println(" !!TDK MultiRunTriggerResultTask failed (" + System.identityHashCode(this) + ") Result: " + ee.toString());
                        resultStore.taskExecuted(resultID, false, ee.toString());
                    }
                } else {
                    // Re-schedule if we're waiting for our first result.
                    if (lastResult == 0L) {
                        se.schedule(this, 1, TimeUnit.SECONDS);
                    } else {
                        // TODO: Is there a window where the returned TaskStatus can go away?
                        resultStore.taskExecuted(resultID, false, "No result, subsequent iteration");
                    }
                }
            } else {
                // Not known to persistent executor, post a failure if this is the first run.
                if (lastResult == 0L) {
                    resultStore.taskExecuted(resultID, false, "No results at all.");
                } else {
                    // The run is finished - the task was removed from the task store.
                    resultStore.taskExecuted(resultID, true, RUN_COMPLETE);
                }
            }
        }
    }

    /**
     * The result for the multi-run trigger task (with result). The result value should be
     * the current value of System.nanoTime().
     */
    public static class Result implements TaskResultStore.Result {

        /**
         * The number of milliseconds between (expected) task runs.
         */
        private final long intervalMillis;

        /**
         * The number of milliseconds that we expect the task to be 'active' for. By active, we
         * mean scheduling itself to run again, before it ends and ceases to run.
         */
        private final long durationMillis;

        /**
         * An indicator that the run is complete.
         */
        private boolean runEnded = false;

        /**
         * A list representing each time the task was executed.
         */
        private final List<Long> successfulRunTimes = new LinkedList<Long>() {
            private static final long serialVersionUID = 77L;

            @Override
            public String toString() {
                long prev = 0L;
                StringBuilder sb = new StringBuilder();
                sb.append(this.size()).append(" : [");
                for (Long l : this) {
                    sb.append(l).append("(");
                    sb.append(TimeUnit.NANOSECONDS.toMillis(l - prev));
                    sb.append("ms), ");
                    prev = l;
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
         * The last reason the error check failed.
         */
        private String lastErrorCheckReason = null;

        /**
         * Constructor.
         * 
         * @param interval The number of milliseconds between task executions.
         * @param duration The number of seconds that we expect the task to run.
         */
        private Result(long interval, long duration) {
            intervalMillis = interval;
            durationMillis = TimeUnit.SECONDS.toMillis(duration);
        }

        /**
         * Stores the result of this recurring task.
         */
        @Override
        public void add(Date executionTime, boolean success, Object data) {
            // If successful, and if data is a Long, we'll add the run to our results.
            if ((success) && (data != null)) {
                if (runEnded == false) {
                    if (data instanceof Long) {
                        successfulRunTimes.add((Long) data);
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
                lastErrorCheckReason = "errorCount or runEnded";
                return false;
            }

            return true;
        }

        /**
         * Print the results.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("MultiRunTriggerResultTask.Result ");
            sb.append("durationMillis = ").append(durationMillis);
            sb.append(", intervalMillis = ").append(intervalMillis);
            sb.append(", runEnded = ").append(runEnded);
            sb.append(", errorCount = ").append(errorCount);
            if (firstErrorTime != null) {
                sb.append(", firstErrorTime = ").append(firstErrorTime);
                sb.append(", firstErrorDetails = ").append(firstErrorDetails);
            }
            sb.append(", successfulRunTimes = ").append(successfulRunTimes.toString());
            sb.append(", lastErrorCheckREason = ").append(lastErrorCheckReason);
            return sb.toString();
        }
    }

}
