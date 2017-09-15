/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;

import com.ibm.jbatch.jsl.model.Step;

/**
 * 
 * This is the interface used by WAS extensions (e.g. the REST interface)
 * into the batch container to perform job management.
 * 
 * Though one design approach would have been to declare this as an extension of
 * the specification's <code>JobOperator</code>, we instead use a separate interface.
 * This together with @see {@link WSJobRepository} basically replace the
 * <code>JobOperator</code> function.
 * 
 * This class handles operations such as start, stop, restart while <code>WSJobRepository</code>
 * provides read-only views of execution data in the persistent store.
 * 
 * @see {@link JobOperator}
 */
public interface WSJobOperator {

    /**
     * Create a new JobInstance using the given parms.
     * 
     * Note: the new job instance is not yet started. It must be started separately.
     * 
     * Note: this method does not need to be run from within the batch app's context.
     * 
     * Note: Inline JSL takes precedence over JSL within .war
     * 
     * @param appName
     * @param jobXMLName
     * @param correlationId
     * 
     * @return newly created JobInstance (Note: job instance must be started separately)
     */
    public WSJobInstance createJobInstance(String appName, String jobXMLName, String jsl, String correlationId);

    /**
     * Start (or "submit") the given job instance, creating the first job execution
     * associated with that job instance.
     * 
     * @param jobInstance
     * @param jobParameters The job parameters @see {@link JobOperator#start(String, Properties)}
     * 
     * @return A Map.Entry with executionId of the newly started job as key and Future object as value.
     * 
     * @throws JobStartException
     * @throws JobSecurityException If current user lacks authority to start a new job.
     */
    public Entry<Long, Future<?>> start(WSJobInstance jobInstance, Properties jobParameters, long executionId)
                    throws JobStartException,
                    JobSecurityException;

    /**
     * Restart an earlier job instance, creating a new, restart job execution.
     * 
     * Closely mirrors the spec method:
     * 
     * @see {@link JobOperator#restart(long, Properties)} with the security authorization checks being WAS-specific, however.
     * 
     * @param instanceId the instance id of the job to be restarted
     * @param restartParameters
     * @param executionId the execution id of the newly created execution
     * @return A Map.Entry with executionId of the newly started job as key and Future object as value.
     * @throws JobExecutionAlreadyCompleteException
     * @throws NoSuchJobExecutionException
     * @throws JobExecutionNotMostRecentException
     * @throws JobRestartException
     * @throws JobSecurityException If the current user id isn't authorized to operate on the given instance & execution.
     */
    public Entry<Long, Future<?>> restartInstance(long instanceId,
                                                  Properties restartParameters, long executionId)
                    throws JobExecutionAlreadyCompleteException,
                    NoSuchJobExecutionException, JobExecutionNotMostRecentException,
                    JobRestartException, JobSecurityException;

    /**
     * Stops a currently executing JobExecution
     * 
     * Closely mirrors:
     * 
     * @see {@link JobOperator#stop(long)}
     * 
     * @param executionId
     * @throws NoSuchJobExecutionException
     * @throws JobExecutionNotRunningException
     * @throws JobSecurityException
     */
    public void stop(long executionId)
                    throws NoSuchJobExecutionException,
                    JobExecutionNotRunningException, JobSecurityException;

    /**
     * 
     * Purge instance and all associated execution data from the persistent store
     * and the job logs in the file system.
     * 
     * This deletes all associated table entries in all tables for all job executions associated
     * with job instance identified by <code>jobInstanceId</code>. It also includes all
     * table entries for related partition and split flow execution data associated with executions
     * of this instance.
     * 
     * It also deletes all related job log entries for executions of this instances from the filesystem.
     * 
     * @param jobInstanceId
     * @throws JobSecurityException If user is not authorized to purge.
     * @throws NoSuchJobInstanceException
     */
    public abstract boolean purgeJobInstance(long jobInstanceId)
                    throws JobSecurityException, NoSuchJobInstanceException;

    /**
     * Start a partition sub-job. This method is called when the partition is being started
     * on a remote executor via JMS (multi-jvm partitions).
     * 
     * @param partitionPlanConfig
     * @param step
     * @param partitionReplyQueue
     * 
     * @return A Future object of the newly started partition.
     */
    public Future<?> startPartition(PartitionPlanConfig partitionPlanConfig,
                                    Step step,
                                    PartitionReplyQueue partitionReplyQueue);

}
