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
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import web.TaskResultStore;

/**
 * A task that runs a single time. The task is dependent scoped, which lets
 * the individual test case make multiple instances of it. The reference to
 * the result store is application scoped so that all instances are updating
 * the same result store.
 */
@Dependent
public class SingleRunTask implements Runnable, Serializable {

    /**  */
    private static final long serialVersionUID = 4346070586196596777L;

    /**
     * Default task delay, in milliseconds.
     */
    public static final int DEFAULT_DELAY = 500;

    /**
     * Result ID
     */
    private int resultID;

    /**
     * Test result tracker.
     */
    @Inject
    private TaskResultStore taskResultStore;

    /**
     * Constructor for CDI
     */
    public SingleRunTask() {}

    /**
     * Initialize the task by getting an ID from the result store, and creating
     * a Result object in the result store which will hold the results once
     * the task runs.
     */
    @PostConstruct
    public void initialize() {
        resultID = taskResultStore.createResult(new Result(DEFAULT_DELAY));

        if (taskResultStore.isDebugMode())
            System.out.println(" !!TDK single run task (" + System.identityHashCode(this) + ") ID: " + resultID);
    }

    /**
     * Driven when the persistent scheduled executor runs this task.
     */
    @Override
    public void run() {
        taskResultStore.taskExecuted(resultID, true, null);
    }

    /**
     * The result for the single run task.
     */
    public static class Result implements TaskResultStore.Result {

        /**
         * The amount of time before or after this task was supposed to run, that
         * we're willing to accept as 'success' (in milliseconds).
         * 
         * The current value of "30 seconds" is pretty lenient and necessary due to
         * the fact that the windows and solaris build machines are much much slower
         * than the linux machines.
         */
        private static int SUCCESS_DELTA_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);

        /**
         * The time that this task is expected to run.
         */
        private final Date expectedRunTime;

        /**
         * The actual run time.
         */
        private Date actualRunTime = null;

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
         */
        private Result(int delayMillis) {
            expectedRunTime = new Date(System.currentTimeMillis() + delayMillis);
        }

        /**
         * Stores the result of this one-shot task.
         */
        @Override
        public void add(Date executionTime, boolean success, Object data) {
            actualRunTime = executionTime;
            runSuccess = success;
            runCount++;
        }

        @Override
        public boolean check() {
            if ((runSuccess == false) || (actualRunTime == null) || (runCount != 1))
                return false;
            else
                return true;
        }

        /**
         * Print the results.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("SingleRunTask.Result ");
            sb.append("expectedRunTime = ").append(expectedRunTime.toString());
            sb.append(", actualRunTime = ").append(actualRunTime.toString());
            sb.append(", runCount = ").append(runCount);
            sb.append(", runSuccess = ").append(runSuccess);
            return sb.toString();
        }
    }
}
