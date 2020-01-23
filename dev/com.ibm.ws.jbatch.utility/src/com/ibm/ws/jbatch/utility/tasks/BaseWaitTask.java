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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ResourceBundleUtils;

/**
 * Encapsulates --wait and --getJobLog logic
 *
 * This class can be extended by any tasks that need --wait and --getJobLog logic.
 */
public abstract class BaseWaitTask<T extends BaseWaitTask> extends BaseBatchRestTask<T> {
    
    /**
     * The RegEx to use for parsing the return code from the job's exitStatus.
     */
    private static final Pattern ExitStatusRC = Pattern.compile("^\\d+");
    
    /**
     * CTOR.
     */
    public BaseWaitTask(String taskName, String scriptName) {
        super(taskName, scriptName);
    }

    /**
     * Add 'wait' options to the list.
     * 
     * @return List of 'optional' option messages, to be displayed with task help.
     */
    @Override
    protected List<String> getOptionalOptionsMessages() {
        List<String> retMe = super.getOptionalOptionsMessages();
        
        retMe.add( buildOptionsMessage("wait.optional-key", "wait.optional-desc") );
        
        return retMe;
    }
    
    /**
     * Add 'wait' options to the list.
     * 
     * @return the list of command line options for this task.  
     */         
    @Override
    protected List<String> getTaskOptions() {
        
        List<String> retMe = super.getTaskOptions();
        
        retMe.addAll( getNlsOptionNames( "wait.optional-key." ) );
        
        return retMe;
    }

    /**
     * Wait for the given jobInstance to complete.
     * 
     * Dump the joblogs to stdout if requested.
     * 
     * @return the completed JobExecution record.
     */
    protected JobExecution waitForTermination(JobInstance jobInstance) throws IOException, InterruptedException {
        return waitForTermination(jobInstance, null);
    }
    
    /**
     * Wait for the given jobExecution to complete.
     * 
     * Dump the joblogs to stdout if requested.
     * 
     * @param jobInstance - will wait on this only if jobExecution == null
     * @param jobExecution - waits on this jobExecution to complete.
     * 
     * @return the completed JobExecution record.
     */
    protected JobExecution waitForTermination(JobInstance jobInstance, JobExecution jobExecution) throws IOException, InterruptedException {
        
        if (shouldGetJobLog()) {
            issueJobLogLocationMessage(jobInstance);
        }
        
        if (jobExecution == null) {
            // Wait on the job instance (which, under the covers, just gets the latest 
            // jobExecution record associated with the jobInstance and waits on that).
            jobExecution = getPollingBatchRestClient().waitForTermination(jobInstance);
        } else {
            jobExecution = getPollingBatchRestClient().waitForTermination( jobExecution );
        }

        return finishUp(jobInstance, jobExecution);
    }
    
    /**
     * Issue job finished/job execution messages.
     * 
     * Dump the joblog to stdout if requested.
     * 
     * @return jobExecution
     */
    protected JobExecution finishUp(JobInstance jobInstance, JobExecution jobExecution) throws IOException {
        
        issueJobFinishedMessage(jobInstance, jobExecution);
        issueJobExecutionMessage(jobExecution);
        
        if (shouldGetJobLog()) {
            issueDownloadingJobLogMessage(jobInstance);
            
            // TODO: optimize - get zipped response, inflate it, then dump it
            getBatchRestClient().getJobLogsForJobInstance( jobInstance.getInstanceId(), "text" )
                                     .copyToStream( getTaskIO().getStdout() );
        }
        
        return jobExecution;
    }

    
    /**
     * Issue the job.log.location to stdout.
     */
    protected void issueJobLogLocationMessage(JobInstance jobInstance) {
        getTaskIO().info( ResourceBundleUtils.getMessage("joblog.location", 
                                      jobInstance.getJobName(),
                                      jobInstance.getInstanceId(),
                                      getBatchRestClient().buildJobLogsRestLink(jobInstance, null) ) );
    }
    
    /**
     * Issue the downloading.job.log to stdout.
     */
    protected void issueDownloadingJobLogMessage(JobInstance jobInstance) {
        getTaskIO().info( ResourceBundleUtils.getMessage("joblog.download", 
                                      jobInstance.getJobName(),
                                      jobInstance.getInstanceId(),
                                      getBatchRestClient().buildJobLogsRestLink(jobInstance, "text") ) );
    }
    
    /**
     * @return true if --getJobLog is specified.
     */
    protected boolean shouldGetJobLog() {
        return getTaskArgs().isSpecified("--getJobLog");
    }

    /**
     * @return true if --wait was specified
     */
    protected boolean shouldWaitForTermination() {
        return getTaskArgs().isSpecified("--wait");
    } 
    
    /**
     * @return the RC for the jbatch process (either the BatchStatus ordinal or atoi(exitStatus)
     */
    protected int getProcessReturnCode( JobExecution jobExecution ) {
        return shouldReturnExitStatus() 
                    ? parseExitStatusReturnCode( jobExecution.getExitStatus() )
                    : getBatchStatusReturnCode( jobExecution.getBatchStatus() );
    }

    /**
     * @return true if --returnExitStatus is specified.
     */
    protected boolean shouldReturnExitStatus() {
        return getTaskArgs().isSpecified("--returnExitStatus");
    }
    
    /**
     * @return the int RC parsed from the given exitStatus.
     */
    protected int parseExitStatusReturnCode( String exitStatus ) {
        Matcher matcher = ExitStatusRC.matcher( exitStatus );
        if (matcher.find()) {
            return Integer.parseInt( matcher.group() );
        } else {
            try {
                return getBatchStatusReturnCode( BatchStatus.valueOf(exitStatus) );
            } catch (IllegalArgumentException iae) {
                throw new NumberFormatException("The exitStatus, '" + exitStatus + "', does not begin with a parsable integer");
            }
        }
    }
    
    /**
     * @return true if --stopOnShutdown is specified; false otherwise
     */
    protected boolean shouldAddShutdownHook() {
        return getTaskArgs().isSpecified("--stopOnShutdown");
    }
    
    /**
     * Add shutdown hook, which gets called if the JVM is terminated.
     * The shutdown hook sends a stop request for the given job.
     * 
     * This method is called on the "--wait" paths.
     */
    protected Thread addShutdownHook(JobInstance jobInstance) {
        
        Thread hook = new StopJobShutdownHook(jobInstance);
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }
    
    /**
     * Remove a previously added shutdown hook.
     */
    protected boolean removeShutdownHook(Thread hook) {
        return (hook != null) ? Runtime.getRuntime().removeShutdownHook(hook) : false;
    }
    
    
    /**
     * The shutdownhook that we register while the utility waits for a job to finish.
     * If the utility is forcefully terminated (CTRL-C), the shutdown hook gets control
     * and tries to stop the job we're waiting on.
     */
    private class StopJobShutdownHook extends Thread {
        
        /**
         * Keep track of the jobInstance / latest jobExecution so we can stop it 
         * when the hook is invoked.
         */
        private JobInstance jobInstance;
        
        /**
         * CTOR.
         */
        public StopJobShutdownHook(JobInstance jobInstance) {
            this.jobInstance = jobInstance;
        }
            
        /**
         * Send a stop request for the job
         */
        @Override
        public void run() {

            try {

                issueShutdownHookMessage(jobInstance);
            
                getBatchRestClient().stop( jobInstance );

                issueJobStopSubmittedMessage(jobInstance);
                
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            
         }
    }


}


