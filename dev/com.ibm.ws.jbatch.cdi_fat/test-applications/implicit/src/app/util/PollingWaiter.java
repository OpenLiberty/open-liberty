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
package app.util;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

/**
 * Uses polling to repeatedly check execution results, thereby
 * waiting for completion. Uses private inner class PollingExecutionWaiter
 * to perform this.
 */
public class PollingWaiter {

    private final static Logger logger = Logger.getLogger("test");

    private final int POLL_INTERVAL = 100; // .1 second

    private long executionId;
    private JobOperator jobOp;
    private long timeout;
    private static long DFLT_TIMEOUT = 10000;

    /**
     * This implementation does no pooling of any kind, it just creates a new instance with new thread each time.
     *
     * @param executionId
     * @param JobOperator
     * @param timeout In milliseconds
     * @return JobExecutionWaiter
     */
    public PollingWaiter(long executionId, JobOperator jobOp, long timeout) {

        logger.fine("Creating waiter for executionId = " + executionId + ", jobOp = " + jobOp + ", timeout = " + timeout);
        this.executionId = executionId;
        this.jobOp = jobOp;
        this.timeout = timeout;
    }

    public PollingWaiter(long executionId, JobOperator jobOp) {
        this(executionId, jobOp, DFLT_TIMEOUT);
    }

    /**
     * Wait for
     * 1) BatchStatus to be one of: STOPPED ,FAILED , COMPLETED, ABANDONED
     * AND
     * 2) exitStatus to be non-null
     *
     * @return JobExceution
     */
    public JobExecution awaitTermination() throws Exception {
        logger.fine("Entering awaitTermination for executionId = " + executionId);
        JobExecution jobExecution = null;

        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                logger.finer("Sleeping for " + POLL_INTERVAL);
                long curTime = System.currentTimeMillis();
                timeOutIfExpired(startTime, curTime);
                Thread.sleep(POLL_INTERVAL);
                logger.finer("Wake up, check for termination.");
                jobExecution = jobOp.getJobExecution(executionId);
                if (isTerminated(jobExecution)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("Aborting on interrupt", e);
            } catch (JobSecurityException e) {
                throw new IllegalStateException("Aborting on security (authorization) exception", e);
            } catch (NoSuchJobExecutionException e) {
                throw new IllegalStateException("JobExecution disappeared for exec id =" + executionId);
            }
        }
        return jobExecution;
    }

    private boolean isTerminated(JobExecution jobExecution) {
        boolean retVal = false;
        BatchStatus bs = jobExecution.getBatchStatus();
        if (terminatedStatuses.contains(bs)) {
            logger.fine("Found terminating batch status of: " + jobExecution.getBatchStatus().name());
            if (jobExecution.getExitStatus() != null) {
                logger.fine("Found exit status of: " + jobExecution.getExitStatus());
                retVal = true;
            } else {
                logger.fine("Exit status is still 'null'.  Poll again.");
                retVal = false;
            }
        } else {
            logger.finer("Found non-terminating batch status of: " + jobExecution.getBatchStatus().name());
            retVal = false;
        }
        return retVal;
    }

    private void timeOutIfExpired(long startTime, long curTime) {
        long diff = curTime - startTime;
        if (diff > timeout) {
            logger.warning("Timed out waiting for Job Execution to reach terminated status.  Time elapsed (long msec) = " + diff + ", and timeout = " + timeout);
            throw new IllegalStateException("Timed out waiting for Job Execution to reach terminated status.  Time elapsed (long msec) = " + diff + ", and timeout = "
                                            + timeout);
        } else {
            logger.finer("Still waiting for Job Execution to reach terminated status.  Time elapsed (long msec) = " + diff + ", and timeout = " + timeout);
        }
    }

    // Full list:
    //public enum BatchStatus {STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED }
    private static Set<BatchStatus> terminatedStatuses = new HashSet<BatchStatus>();
    static {
        terminatedStatuses.add(BatchStatus.STOPPED);
        terminatedStatuses.add(BatchStatus.FAILED);
        terminatedStatuses.add(BatchStatus.COMPLETED);
        terminatedStatuses.add(BatchStatus.ABANDONED);
    }

}
