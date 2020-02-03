/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import web.TaskResultStore;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * This task runs once and returns a unique result. The result is
 * stored by the persistent executor until it is retrieved and the
 * task is purged from the backing store.
 */
@Dependent
public class SingleRunResultTask implements Callable<Long>, Serializable, ManagedTask {

    /**  */
    private static final long serialVersionUID = -403844797610242052L;

    /**
     * Default task delay, in milliseconds.
     */
    public static final int DEFAULT_DELAY = 500;

    /**
     * Result ID
     */
    private int resultID;

    /**
     * The value that this task should return when it runs.
     */
    private long returnValue;

    /**
     * Test result tracker.
     */
    @Inject
    private TaskResultStore taskResultStore;

    /**
     * Constructor for CDI
     */
    public SingleRunResultTask() {}

    /**
     * Initialize the task by getting an ID from the result store, and creating
     * a Result object in the result store which will hold the results once
     * the task runs.
     */
    @PostConstruct
    public void initialize() {
        returnValue = System.nanoTime();
        resultID = taskResultStore.createResult(new Result(DEFAULT_DELAY, returnValue));

        if (taskResultStore.isDebugMode())
            System.out.println(" !!TDK single run result task (" + System.identityHashCode(this) + ") ID: " + resultID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public Long call() throws Exception {
        // Rather than add the result directly, we'll return and let another task
        // add the result.  This will read the result from the persistent executor
        // task store.
        final long taskId = TaskIdAccessor.get();
        final ScheduledExecutorService e = taskResultStore.getUnmanagedScheduledExecutor();
        final PersistentExecutor pe = taskResultStore.getPeristentExecutor();
        e.submit(new Runnable() {

            @Override
            public void run() {
                // Go get the result from the task store.  If not ready, check back later.
                TaskStatus<Long> status = pe.getStatus(taskId);
                if (status.hasResult() == true) {
                    try {
                        Long result = status.getResult();
                        pe.remove(taskId); // Remove before recording results on success.
                        taskResultStore.taskExecuted(resultID, true, result);
                    } catch (ExecutionException ee) {
                        // Something went wrong...
                        if (taskResultStore.isDebugMode())
                            System.out.println(" !!TDK single run result task failed (" + System.identityHashCode(this) + ") Result: " + ee.toString());
                        taskResultStore.taskExecuted(resultID, false, ee);
                        pe.remove(taskId);
                    }
                } else {
                    e.schedule(this, 100, TimeUnit.MILLISECONDS);
                }
            }

        });

        return returnValue;
    }

    /**
     * Tell the persistent executor that we don't want to auto-purge results.
     * 
     * @see javax.enterprise.concurrent.ManagedTask#getExecutionProperties()
     */
    @Override
    public Map<String, String> getExecutionProperties() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        return props;
    }

    /**
     * We don't have a managed task listener.
     * 
     * @see javax.enterprise.concurrent.ManagedTask#getManagedTaskListener()
     */
    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    /**
     * The result for the single run task.
     */
    public static class Result implements TaskResultStore.Result {

        /**
         * The amount of time before or after this task was supposed to run, that
         * we're willing to accept as 'success' (in milliseconds).
         * 
         * The current value of "1 minute" is pretty lenient and necessary due to
         * the fact that the windows and solaris build machines are much much slower
         * than the linux machines.
         */
        private static int SUCCESS_DELTA_MILLIS = (int) TimeUnit.MINUTES.toMillis(1);

        /**
         * The time that this task is expected to run.
         */
        private final Date expectedRunTime;

        /**
         * The expected result value of the task.
         */
        private final long expectedResult;

        /**
         * The actual run time.
         */
        private Date actualRunTime = null;

        /**
         * The actual result.
         */
        private long actualResult = 0;

        /**
         * Successful run.
         */
        private boolean runSuccess = false;

        /**
         * Total run count.
         */
        private int runCount = 0;

        /**
         * Constructor.
         * 
         * @param delay The number of milliseconds after which the results should be posted.
         * @param result The expected result (returned from task.call()).
         */
        private Result(int delayMillis, long result) {
            expectedRunTime = new Date(System.currentTimeMillis() + delayMillis);
            expectedResult = result;
        }

        /**
         * Stores the result of this one-shot task.
         */
        @Override
        public void add(Date executionTime, boolean success, Object data) {
            actualRunTime = executionTime;
            runSuccess = success;
            runCount++;

            if ((data != null) && (data instanceof Long)) {
                actualResult = (Long) data;
            }
        }

        /**
         * Check our result. Was the return value correct?
         */
        @Override
        public boolean check() {
            // runCount > 1 can mean the task was retried, for example, due to a transaction timeout.
            // If a retry was successful, it should be considered a passing result.
            if ((runSuccess == false) || (actualRunTime == null) || (runCount < 1))
                return false;

            if (actualResult != expectedResult)
                return false;

            return true;
        }

        /**
         * Print the results.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("SingleRunResultTask.Result ");
            sb.append("expectedRunTime = ").append(expectedRunTime.toString());
            sb.append(", expectedResult = ").append(expectedResult);
            sb.append(", actualRunTime = ").append(actualRunTime.toString());
            sb.append(", actualResult = ").append(actualResult);
            sb.append(", runCount = ").append(runCount);
            sb.append(", runSuccess = ").append(runSuccess);
            return sb.toString();
        }
    }
}
