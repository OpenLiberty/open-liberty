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
import java.util.Properties;

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that restarts a job.
 * 
 * The task will wait for the job to complete if the --wait option is specified.
 * 
 */
public class RestartTask extends BaseWaitTask<RestartTask> {

    /**
     * CTOR.
     */
    public RestartTask(String scriptName) {
        super("restart", scriptName);
    }

    /**
     * 
     * 1. open REST connection to target server
     *           -- need host/port
     *           -- need user/pass
     * 2. call "restart" on the REST api
     *          -- need jobExecutionId or jobInstanceId
     *          -- should return the jobInstance?
     * 3. call "getJobExecutions" passing in the jobInstanceId
     * 4. check "getBatchStatus" field of JobExecution object
     *          -- if batchStatus == STOPPED, FAILED, COMPLETED, ABANDONED
     *             then break;
     *             else sleep, then loop back to #3
     * 5. print the "getExitStatus" field of the JobExecution object
     * 6. return the batch status as the script RC.
     *
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        // Before restart, must get the latest JobExecution record 
        // so that I can detect when a new JobExecution for the restart
        // is started.
        JobExecution latestJobExecution = getPollingBatchRestClient().getLatestJobExecution( resolveJobInstanceId() );

        // Restart the job
        JobInstance jobInstance = restartJob();
        
        issueJobRestartedMessage(jobInstance);
        issueJobInstanceMessage(jobInstance);
        
        if ( shouldWaitForTermination() ) {
            
            // Wait for a new job execution to show up
            latestJobExecution = getPollingBatchRestClient().waitForNextJobExecution( jobInstance, latestJobExecution );

            // Register a shutdown hook to stop the job we're waiting on if 
            // batchManager is abnormally terminated.
            Thread shutdownHook = (shouldAddShutdownHook()) ? addShutdownHook(jobInstance) : null;
            
            latestJobExecution = waitForTermination( jobInstance, latestJobExecution );
            
            removeShutdownHook(shutdownHook);
            
            return getProcessReturnCode( latestJobExecution ) ;
            
        } else {
            return 0;
        }

    }
    
    /**
     * Restart the job via jobinstanceid or jobexecutionid, whichever was provided.
     */
    protected JobInstance restartJob() throws IOException {

        Long jobInstanceId = getJobInstanceId();
        Long jobExecutionId = getJobExecutionId();
        
        if (jobInstanceId != null) {
            return getBatchRestClient().restartJobInstance( jobInstanceId, getJobProperties() );
        } else if (jobExecutionId != null) {
            return getBatchRestClient().restartJobExecution( jobExecutionId, getJobProperties() );
        } else {
            throw new ArgumentRequiredException("--jobInstanceId or --jobExecutionId");
        }
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

    /**
     * @return the job properties, as specified by the --jobPropertiesFile arg.
     */
    protected Properties getJobProperties() throws IOException {
        return getTaskArgs().getJobParameters();
    }
}
