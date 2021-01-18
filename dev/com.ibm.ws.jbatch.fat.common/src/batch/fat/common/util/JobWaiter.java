/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.common.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

/**
 * Captures some of the common logic in waiting for certain states but failing on others,
 * with a limited, configurable amount of polling.
 */
public class JobWaiter {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    /*
     * This might be the most typical expectation, but perhaps certain tests will want to consider
     * STOPPING a valid interim state and STOPPED a valid final state.
     */
    public final static BatchStatus[] STARTED_OR_STARTING = { BatchStatus.STARTED, BatchStatus.STARTING };
    public final static BatchStatus[] STOPPING_OR_STOPPED_STATES = { BatchStatus.STOPPING, BatchStatus.STOPPED };
    public final static BatchStatus[] COMPLETED_STATE_ONLY = { BatchStatus.COMPLETED };
    public final static BatchStatus[] FAILED_STATE_ONLY = { BatchStatus.FAILED };
    public final static BatchStatus[] COMPLETED_OR_FAILED_STATES = { BatchStatus.COMPLETED, BatchStatus.FAILED };

    // 180 seconds hopefully is long enough for a real build without being ridiculously long
    //static long DFLT_SLEEP_TIME = 180000;
    static long DFLT_SLEEP_TIME = 15000;

    // These two are not currently configurable
    static int NUM_TRIES = 90;
    static long SLEEP_TIME_BETWEEN_FINAL_STATUS_CHECKS = 2000;

    long sleepTime;
    Set<BatchStatus> validInterimStatesSet;
    Set<BatchStatus> validFinalStatesSet;

    public JobWaiter() {
        this(DFLT_SLEEP_TIME, STARTED_OR_STARTING, COMPLETED_STATE_ONLY);
    }

    /*
     * This ctor takes the default for everything but the final states.
     */
    public JobWaiter(BatchStatus[] validFinalStates) {
        this(DFLT_SLEEP_TIME, STARTED_OR_STARTING, validFinalStates);
    }

    /*
     * This ctor takes the default for the sleep time.
     */
    public JobWaiter(BatchStatus[] validInterimStates, BatchStatus[] validFinalStates) {
        this(DFLT_SLEEP_TIME, validInterimStates, validFinalStates);
    }

    public JobWaiter(long sleepTime, BatchStatus[] validInterimStates, BatchStatus[] validFinalStates) {
        super();
        this.sleepTime = sleepTime;
        this.validInterimStatesSet = new HashSet<BatchStatus>(Arrays.asList(validInterimStates));
        this.validFinalStatesSet = new HashSet<BatchStatus>(Arrays.asList(validFinalStates));
    }

    /**
     * 
     * Waits <code>sleepTime</code> for notification of the batch.fat.artifacts.EndOfJobNotificationListener</code>
     * class object lock.
     * 
     * This is an important, non-standard technique. The goal is to reduce the window in which
     * we might time out waiting for the job to complete and wrongly mark the tet as failed.
     * 
     * It depends on the invocation happening in the same
     * classloader since we're using a static class (so obviously the same JVM as well) !!!!
     * 
     * @param execId
     * @return
     * @throws TestFailureException
     */
    public void waitForAfterJobNotification(Object lock) throws TestFailureException {
        synchronized (lock) {
            try {
                logger.fine("Sleeping waiting for afterJob() for: " + sleepTime + " msec");
                lock.wait(sleepTime);
            } catch (InterruptedException e) {
                throw new TestFailureException("ERROR: InterruptedException message " + e.getMessage());
            }
        }
    }

    /**
     * 
     * Waits <code>sleepTime</code> for notification of the batch.fat.artifacts.EndOfJobNotificationListener</code>
     * class object lock, and then begins polling for the final execution status.
     * 
     * @param execId
     * @return
     * @throws TestFailureException
     */
    public JobExecution waitForAfterJobNotificationThenFinalState(Object lock, long execId) throws TestFailureException {
        waitForAfterJobNotification(lock);
        return pollForFinalState(execId);
    }

    public JobExecution pollForFinalState(long execId) throws TestFailureException {

        logger.fine("Polling for finalState for execId:  " + execId);

        BatchStatus status = null;
        JobOperator jobOperator = BatchRuntime.getJobOperator();

        for (int i = 0; i < NUM_TRIES; i++) {
            JobExecution jobExec = jobOperator.getJobExecution(execId);

            // Verify batch status
            status = jobExec.getBatchStatus();
            logger.fine("For execId:  " + execId + " found status: " + status);
            if (validFinalStatesSet.contains(status)) {
                logger.finer("Valid final status");
                return jobExec;
            } else if (validInterimStatesSet.contains(status)) {
                try {
                    logger.finer("Valid interim status, sleeping.");
                    Thread.sleep(SLEEP_TIME_BETWEEN_FINAL_STATUS_CHECKS);
                } catch (InterruptedException e) {
                    throw new TestFailureException("ERROR: InterruptedException message " + e.getMessage());
                }
                continue;
            } else {
                String msg = "ERROR: Job executed but resulted in invalid status: " + status;
                logger.severe(msg);
                throw new TestFailureException(msg);
            }
        }
        String msg = "ERROR: Loop ended with batch status: " + status + " after maximum number of tries without reaching any expected valid final status";
        logger.severe(msg);
        throw new TestFailureException(msg);
    }

    public class Lock {

    }

}
