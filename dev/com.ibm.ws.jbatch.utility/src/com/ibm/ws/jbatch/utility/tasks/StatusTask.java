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
package com.ibm.ws.jbatch.utility.tasks;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.rest.BatchRestClient;
import com.ibm.ws.jbatch.utility.rest.PollingBatchClient;
import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that reports on the status of a job instance/execution.
 */
public class StatusTask extends BaseBatchRestTask<StatusTask> {

    /**
     * CTOR.
     */
    public StatusTask(String scriptName) {
        super("status", scriptName);
    }

    /**
     * 
     * Retrieve and print status for the requested jobinstance/jobexecution.
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        BatchRestClient batchRestClient = getBatchRestClient();
        
        JobInstance jobInstance = batchRestClient.getJobInstance( resolveJobInstanceId() );

        List<JobExecution> jobExecutions = batchRestClient.getJobExecutions(jobInstance);
       
        return handleResult(jobInstance, jobExecutions) ;

    }
    
    /**
     * @return the batchstatus return code of the latest jobExecution.
     */
    protected int handleResult(JobInstance jobInstance, List<JobExecution> jobExecutions) {
        issueJobInstanceMessage(jobInstance);

        for (JobExecution jobExecution : jobExecutions) {
            issueJobExecutionMessage(jobExecution);
        }
        
        JobExecution latestJobExecution = PollingBatchClient.getLatestJobExecution( jobExecutions) ;
        return (latestJobExecution != null) ? getBatchStatusReturnCode( latestJobExecution.getBatchStatus() ) : 0;
    }

    /**
     * @return the --jobExecutionId arg.
     */
    protected Long getJobExecutionId() {
        return getTaskArgs().getLongValue("--jobExecutionId", null);
    }
    
    /**
     * @return the --jobInstanceId arg.
     */
    protected Long getJobInstanceId() {
        return getTaskArgs().getLongValue("--jobInstanceId", null);
    }
    
    /**
     * @return the JobInstance ID to be restarted.
     */
    protected long resolveJobInstanceId() throws IOException {
        
        Long jobInstanceId = getJobInstanceId();
        Long jobExecutionId = getJobExecutionId();
        
        if (jobInstanceId != null) {
            return jobInstanceId;
        } else if (jobExecutionId != null) {
            return getBatchRestClient().getJobInstanceForJobExecution(jobExecutionId).getInstanceId();
        } else {
            throw new ArgumentRequiredException("--jobInstanceId or --jobExecutionId");
        }
    }

}

