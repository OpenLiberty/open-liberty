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
import javax.inject.Inject;

import web.TaskResultStore;

/**
 * This task is designed to be cancelled. If it runs, that is considered an error.
 */
public class CancelTask implements Runnable, Serializable {
    /**  */
    private static final long serialVersionUID = 919164553234182660L;

    /**
     * Default task delay, in milliseconds.
     */
    public static final long DEFAULT_DELAY = TimeUnit.MINUTES.toMillis(1);

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
    public CancelTask() {}

    /**
     * Initialize the task by getting an ID from the result store, and creating
     * a Result object in the result store which will hold the results once
     * the task runs.
     */
    @PostConstruct
    public void initialize() {
        resultID = taskResultStore.createResult(new Result());

        if (taskResultStore.isDebugMode())
            System.out.println(" !!TDK cancel task (" + System.identityHashCode(this) + ") ID: " + resultID);
    }

    /**
     * This task should not run. If it does, it's a failure.
     */
    @Override
    public void run() {
        taskResultStore.taskExecuted(resultID, false, null);
    }

    /**
     * Gets the result ID.
     */
    public int getResultID() {
        return resultID;
    }

    /**
     * The result for the single run task.
     */
    public static class Result implements TaskResultStore.Result {

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
        private Result() {}

        /**
         * Stores the result of this one-shot task.
         * 
         * @param executionTime The time the result came in.
         * @param success True if the task was cancelled. False otherwise.
         * @param data Should be null.
         */
        @Override
        public void add(Date executionTime, boolean success, Object data) {
            runSuccess = success;
            runCount++;
        }

        /**
         * Check our result. Did the task run?
         */
        @Override
        public boolean check() {
            return ((runSuccess == true) && (runCount == 1));
        }

        /**
         * Print the results.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("CancelTask.Result ");
            sb.append("runCount = ").append(runCount);
            sb.append(", runSuccess = ").append(runSuccess);
            return sb.toString();
        }
    }

}
