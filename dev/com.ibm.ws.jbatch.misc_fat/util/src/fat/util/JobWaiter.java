/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

/**
 * Utility to start a job with a static reference to a
 * {@link javax.batch.operations.JobOperator}
 * and wait for finished state like {@code BatchStatus.COMPLETED} or
 * {@code BatchStatus.FAILED}.
 *
 * The full set of finished statuses is
 * <li>{@code BatchStatus.COMPLETED}
 * <li>{@code BatchStatus.FAILED}.
 * <li>{@code BatchStatus.STOPPED}.
 * <li>{@code BatchStatus.ABANDONED}.
 *
 * It also waits for job exitStatus to be non-null
 */
public class JobWaiter {

    private final static Logger logger = Logger.getLogger("test");

    // Full list:
    //public enum BatchStatus {STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED }
    private static Set<BatchStatus> finishedStatuses = new HashSet<BatchStatus>();
    static {
        finishedStatuses.add(BatchStatus.STOPPED);
        finishedStatuses.add(BatchStatus.FAILED);
        finishedStatuses.add(BatchStatus.COMPLETED);
        finishedStatuses.add(BatchStatus.ABANDONED);
    }

    protected static JobOperator jobOp = BatchRuntime.getJobOperator();

    private final int POLL_INTERVAL = 100; // .1 second

    private long timeout;

    /**
     * @return Time (milliseconds) to wait for job to finish
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout Time (milliseconds) to wait for job to finish
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @param timeout Time (milliseconds) to wait for job to finish
     */
    public JobWaiter() {
        this(DFLT_TIMEOUT);
    }

    /**
     * @param timeout Time (milliseconds) to wait for job to finish
     */
    public JobWaiter(long timeout) {
        super();
        this.timeout = timeout;
    }

    private static long DFLT_TIMEOUT = 10000;

    /**
     * Wait for {@code JobWaiter#timeout} seconds for BOTH of:
     *
     * 1) BatchStatus to be one of: STOPPED ,FAILED , COMPLETED, ABANDONED
     * AND
     * 2) exitStatus to be non-null
     *
     * Returns JobExecution if it ends in <b>COMPLETED</b> status, otherwise throws
     * <b>IllegalStateException</b>
     *
     * @param jobXMLName
     * @param jobParameters
     * @return JobExecution (for successfully completing job)
     * @throws IllegalStateException
     */
    public JobExecution completeNewJob(String jobXMLName, Properties jobParameters) throws IllegalStateException {
        long executionId = jobOp.start(jobXMLName, jobParameters);
        JobExecution jobExec = waitForFinish(executionId);
        if (jobExec.getBatchStatus().equals(BatchStatus.COMPLETED)) {
            logger.finer("Job " + executionId + " successfully completed.");
            return jobExec;
        } else {
            throw new IllegalStateException("Job " + executionId + " finished with non-completed state: " + jobExec.getBatchStatus());
        }
    }

    /**
     * Wait for {@code JobWaiter#timeout} seconds for BOTH of:
     *
     * 1) BatchStatus to be : FAILED
     * AND
     * 2) exitStatus to be non-null
     *
     * Returns JobExecution if it ends in <b>FAILED</b> status, otherwise throws
     * <b>IllegalStateException</b>
     *
     * @param jobXMLName
     * @param jobParameters
     * @return JobExecution (for successfully completing job)
     * @throws IllegalStateException
     */
    public JobExecution submitExpectedFailingJob(String jobXMLName, Properties jobParameters) throws IllegalStateException {
        long executionId = jobOp.start(jobXMLName, jobParameters);
        JobExecution jobExec = waitForFinish(executionId);
        if (jobExec.getBatchStatus().equals(BatchStatus.FAILED)) {
            logger.finer("Job " + executionId + " failed as expected.");
            return jobExec;
        } else {
            throw new IllegalStateException("Job " + executionId + " finished with non-FAILED state: " + jobExec.getBatchStatus());
        }
    }

    /**
     * Wait for {@code JobWaiter#timeout} seconds for BOTH of:
     *
     * 1) BatchStatus to be one of: STOPPED ,FAILED , COMPLETED, ABANDONED
     * AND
     * 2) exitStatus to be non-null
     *
     * The job is expected to fail on the first attempt, and then a number of restarts up to the
     * restartAttempts parameter will be tried. The job may complete before that many restarts
     * are attempted.
     *
     * Returns JobExecution if it ends in <b>COMPLETED</b> status, otherwise throws
     * <b>IllegalStateException</b>
     *
     * @param jobXMLName
     * @param jobParameters
     * @param restartAttempts
     * @return JobExecution (for successfully completing job)
     * @throws IllegalStateException
     */
    public JobExecution completeNewJobWithRestart(String jobXMLName, Properties jobParameters, int restartAttempts) throws IllegalStateException {
        long executionId = jobOp.start(jobXMLName, jobParameters);
        JobExecution jobExec = waitForFinish(executionId);

        if (jobExec.getBatchStatus().equals(BatchStatus.FAILED)) {

            for (int i = 0; i < restartAttempts; i++) {
                jobParameters.put("restartCount", Integer.toString(i + 1));

                executionId = jobOp.restart(executionId, jobParameters);
                jobExec = waitForFinish(executionId);

                if (jobExec.getBatchStatus().equals(BatchStatus.COMPLETED)) {
                    logger.finer("Job " + executionId + " successfully completed.");
                    return jobExec;
                }
            }

            throw new IllegalStateException("Job " + executionId + " failed to complete within the allowed number of restarts (" + restartAttempts + ")."
                                            + " Last execution status is: " + jobExec.getBatchStatus());

        } else {
            throw new IllegalStateException("Job " + executionId + " expected to fail and be restartable, but status is: " + jobExec.getBatchStatus());
        }
    }

    /**
     * Wait for {@code JobWaiter#timeout} seconds for BOTH of:
     *
     * 1) BatchStatus to be one of: STOPPED ,FAILED , COMPLETED, ABANDONED
     * AND
     * 2) exitStatus to be non-null
     *
     * @return JobExecution
     */
    public JobExecution waitForFinish(long executionId) {
        logger.fine("Entering waitForFinish for executionId = " + executionId);
        JobExecution jobExecution = null;

        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                logger.finer("Sleeping for " + POLL_INTERVAL);
                long curTime = System.currentTimeMillis();
                timeOutIfExpired(startTime, curTime);
                Thread.sleep(POLL_INTERVAL);
                logger.finer("Wake up, check for Finished.");
                jobExecution = jobOp.getJobExecution(executionId);
                if (isfinished(jobExecution)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborting on interrupt", e);
            }
        }
        return jobExecution;
    }

    private boolean isfinished(JobExecution jobExecution) {
        boolean retVal = false;
        BatchStatus bs = jobExecution.getBatchStatus();
        if (finishedStatuses.contains(bs)) {
            logger.fine("Found finishing batch status of: " + jobExecution.getBatchStatus().name());
            if (jobExecution.getExitStatus() != null) {
                logger.fine("Found exit status of: " + jobExecution.getExitStatus());
                retVal = true;
            } else {
                logger.fine("Exit status is still 'null'.  Poll again.");
                retVal = false;
            }
        } else {
            logger.finer("Found non-finished batch status of: " + jobExecution.getBatchStatus().name());
            retVal = false;
        }
        return retVal;
    }

    private void timeOutIfExpired(long startTime, long curTime) {
        long diff = curTime - startTime;
        if (diff > timeout) {
            logger.warning("Timed out waiting for JobExecution to reach finished status.  Time elapsed (long msec) = " + diff + ", and timeout = " + timeout);
            throw new IllegalStateException("Timed out waiting for JobExecution to reach finished status.  Time elapsed (long msec) = " + diff + ", and timeout = "
                                            + timeout);
        } else {
            logger.finer("Still waiting for Job Execution to reach finished status.  Time elapsed (long msec) = " + diff + ", and timeout = " + timeout);
        }
    }

}
