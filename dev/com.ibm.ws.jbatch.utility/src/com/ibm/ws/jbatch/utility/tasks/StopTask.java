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

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that stops a job.
 * 
 */
public class StopTask extends BaseWaitTask<StopTask> {

    /**
     * CTOR.
     */
    public StopTask(String scriptName) {
        super("stop", scriptName);
    }

    /**
     * 
     * 1. open REST connection to target server
     *           -- need host/port
     *           -- need user/pass
     * 2. call "stop" on the REST api
     *          -- need jobExecutionId or jobInstanceId
     *          
     * 3. print the "getExitStatus" field of the JobExecution object
     * 4. return the batch status as the script RC.
     *
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        JobInstance jobInstance;
        JobExecution jobExecution;
        long executionId = resolveJobExecutionId();
        
        if (executionId >= 0) {
            jobExecution = getBatchRestClient().stop( executionId );
            jobInstance = getBatchRestClient().getJobInstanceForJobExecution(jobExecution.getExecutionId());
        } else {
            jobInstance = getBatchRestClient().getJobInstance(getJobInstanceId());
            jobExecution = getBatchRestClient().stop(jobInstance);
        }
             
        issueJobStopSubmittedMessage(jobInstance);
     
        if ( shouldWaitForTermination() ) { 
            // If there are no job executions, consider the job stopped.  If the status has not been updated
            // because of a database problem, we don't want to sit and wait forever. 
            if (jobExecution.getExecutionId() == -1) {
                
                issueJobStoppedMessage(jobInstance, null);
                
                return 0;
            } else {

                jobExecution = waitForTermination(jobInstance, jobExecution);
           
                return getProcessReturnCode( jobExecution ) ;
            }

        } else {
            return 0;
        }

    }
    
    /**
     * This method is called by BaseWaitTask.  We're overriding the base implementation 
     * (which issues job.finished message) to issue the job.stopped message instead.
     */
    @Override
    protected void issueJobFinishedMessage(JobInstance jobInstance, JobExecution jobExecution) {
        issueJobStoppedMessage(jobInstance, jobExecution);
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
     * @return the JobExecution ID to be stopped.
     */
    protected long resolveJobExecutionId() throws InterruptedException, IOException {
        
        Long jobInstanceId = getJobInstanceId();
        Long jobExecutionId = getJobExecutionId();
        
        if (jobExecutionId != null) {
            return jobExecutionId;
        } else if (jobInstanceId != null) {
            // TODO: eventually should be able to stop the instance directly and let
            //       the batch manager figure out the jobexecution
            return -1;
        } else {
            throw new ArgumentRequiredException("--jobInstanceId or --jobExecutionId");
        }
    }
}

