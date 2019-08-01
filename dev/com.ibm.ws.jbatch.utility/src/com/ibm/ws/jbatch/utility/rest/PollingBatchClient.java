/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.rest;

import java.io.IOException;
import java.util.List;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ResourceBundleUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * This class provides some higher-level functionality on top of the
 * BatchRestClient.  It uses polling for functions that require continuously
 * calling the REST api, e.g. waiting for BatchStatus to change.
 * 
 */
public class PollingBatchClient {
    
    /**
     * Ref to the rest client.
     */
    private BatchRestClient batchRestClient;
    
    /**
     * The polling interval, default = 30s.
     */
    private long pollingInterval_ms = 30 * 1000; 
    
    /**
     * If true, it will issue a message containing the JobExecution record
     * every time it polls for status waiting for termination.
     */
    private boolean verboseWait = false;
    
    /**
     * TaskIO for verbose msgs..
     */
    private TaskIO taskIO = null;
    
    /**
     * CTOR.
     */
    public PollingBatchClient(BatchRestClient batchRestClient, long pollingInterval_ms) {
        this.batchRestClient = batchRestClient;
        this.pollingInterval_ms = pollingInterval_ms;
    }

    /**
     * @return the polling interval in milliseconds.
     */
    public long getPollingInterval_ms() {
        return pollingInterval_ms;
    }
    
    /**
     * @return the batch rest client
     */
    public BatchRestClient getBatchRestClient() {
        return batchRestClient;
    }
    
    /**
     * Note: this method will poll (forever) until the first JobExecution record
     * is started.
     * 
     * @return the latest JobExecution for the given JobInstance. 
     */
    public JobExecution waitForLatestJobExecution(JobInstance jobInstance) throws InterruptedException, IOException {
        return waitForLatestJobExecution(jobInstance.getInstanceId());
    }
    
    /**
     * Note: this method will poll (forever) until the first JobExecution record
     * is started.
     * 
     * @return the latest JobExecution for the given JobInstance. 
     */
    public JobExecution waitForLatestJobExecution(long jobInstanceId) throws InterruptedException, IOException {
        
        List<JobExecution> jobExecutions = getBatchRestClient().getJobExecutions(jobInstanceId);

        // Keep trying until the first jobExecution record shows up
        while ( jobExecutions.isEmpty() ) {

            waitForLatestJobExecutionVerbose(jobInstanceId);
            
            Thread.sleep( getPollingInterval_ms() );

            jobExecutions = getBatchRestClient().getJobExecutions(jobInstanceId);
        }
        
        // The most recent is the first one (they are always returned in order).
        return getLatestJobExecution(jobExecutions);
    }
    
    
    
    /**
     *
     * Wait for the given JobInstance to complete.
     * 
     * @return the latest completed JobExecution record.
     */
    public JobExecution waitForTermination(JobInstance jobInstance) throws InterruptedException, IOException {
        return waitForTermination( waitForLatestJobExecution( jobInstance ) );   
    }

    /**
     *
     * Wait for the given JobExecution to complete.
     * 
     * @return the given JobExecution record.
     */
    public JobExecution waitForTermination(JobExecution jobExecution) throws InterruptedException, IOException {
  
        while ( ! isDone( jobExecution.getBatchStatus() ) ) {
            
            waitForTerminationVerbose( jobExecution );
            
            Thread.sleep( getPollingInterval_ms() );

            jobExecution = getBatchRestClient().getJobExecution( jobExecution.getExecutionId() );
        }

        return jobExecution;
    }
    
    /**
     * Set verbose waiting.
     * @return this
     */
    public PollingBatchClient setVerboseWait(boolean verboseWait, TaskIO taskIO) {
        this.verboseWait = verboseWait;
        this.taskIO = taskIO;
        return this;
    }
    
    /**
     * @return true if verbose waiting was requested.
     */
    protected boolean isVerboseWait() {
        return verboseWait && taskIO != null;
    }
    
    /**
     * If verbose waiting is requested, issue a "waiting for termination" message
     * with the given jobexecution record.
     */
    public void waitForTerminationVerbose(JobExecution jobExecution) {
        if ( isVerboseWait() ) {
            taskIO.info(ResourceBundleUtils.getMessage("waiting.for.termination", jobExecution.toString()));
        }
    }
    
    /**
     * If verbose waiting is requested, issue a "waiting for latest job execution" message
     * with the given jobinstanceid.
     */
    public void waitForLatestJobExecutionVerbose(long jobInstanceId) {
        if ( isVerboseWait() ) {
            taskIO.info(ResourceBundleUtils.getMessage("waiting.for.latest.job.execution", jobInstanceId));
        }
    }
    
    /**
     * If verbose waiting is requested, issue a "waiting for next job execution" message
     * with the given jobexecution record.
     */
    public void waitForNextJobExecutionVerbose(JobExecution jobExecution) {
        if ( isVerboseWait() ) {
            taskIO.info(ResourceBundleUtils.getMessage("waiting.for.next.job.execution", jobExecution.toString()));
        }
    }

    /**
     * 
     * @return true if the given BatchStatus is in a "done" state (STOPPED, FAILED, COMPLETED, ABANDONED).
     */
    public boolean isDone( BatchStatus batchStatus ) {
        switch (batchStatus) {
            case STOPPED:
            case FAILED:
            case COMPLETED:
            case ABANDONED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Note: this method will poll forever until the next JobExecution record shows up.
     * 
     * @return the next JobExecution record after the given one.
     */
    public JobExecution waitForNextJobExecution(JobInstance jobInstance, JobExecution afterThisJobExecution)
                    throws InterruptedException, IOException {
        
        if (afterThisJobExecution == null) {
            return waitForLatestJobExecution(jobInstance);
        }
        
        JobExecution retMe = getNextJobExecution(getBatchRestClient().getJobExecutions(jobInstance), 
                                                 afterThisJobExecution);
        
        while (retMe == null) {
            
            waitForNextJobExecutionVerbose(afterThisJobExecution);
            
            Thread.sleep( getPollingInterval_ms() );
            
            retMe = getNextJobExecution(getBatchRestClient().getJobExecutions(jobInstance), 
                                        afterThisJobExecution);
        }
        
        return retMe;
    }

    /**
     * @return the jobExecution record from the given list that immediately follows
     *         the given afterThisJobExecution
     */
    protected JobExecution getNextJobExecution(List<JobExecution> jobExecutions,
                                               JobExecution afterThisJobExecution) {
        
        // The "next" one is actually the previous one in the list, since the jobexecutions
        // are returned in reverse chronological order.
        JobExecution prev = null;
        
        for (JobExecution jobExecution : jobExecutions) {
            if (jobExecution.getExecutionId() == afterThisJobExecution.getExecutionId()) {
                return prev;
            }
            prev = jobExecution;
        }
        
        // Didn't find a JobExecution record that matched afterThisJobExecution.
        // Must mean afterThisJobExecution isn't actually associated with the JobInstance.
        throw new IllegalArgumentException( "afterThisJobExecution (" + afterThisJobExecution + ")"
                                            + " not in the given list of JobExecutions (" + jobExecutions + ")" );
    }
    
    /**
     * @return the latest (most recent) jobexecution in the given list.
     */
    public static JobExecution getLatestJobExecution(List<JobExecution> jobExecutions) {
        return (jobExecutions != null && jobExecutions.size() > 0) 
                    ? jobExecutions.get(0)
                    : null;
    }

    /**
     * @return the latest (most recent) job execution for the given job.
     */
    public JobExecution getLatestJobExecution(long jobInstanceId) throws IOException {
        return getLatestJobExecution( getBatchRestClient().getJobExecutions(jobInstanceId) );
    }

}
