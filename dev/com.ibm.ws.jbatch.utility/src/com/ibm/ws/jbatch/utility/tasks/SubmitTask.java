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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that submits a job.
 * 
 * Note: this task will wait for the job to complete if the --wait option 
 * is specified.
 * 
 * The jobinstanceid of the newly submitted job is printed to stdout.
 *
 * If the --wait option is specified, the jobexecution record is printed
 * when the job completes.
 */
public class SubmitTask extends BaseWaitTask<SubmitTask> {

    /**
     * CTOR.
     */
    public SubmitTask(String scriptName) {
        super("submit", scriptName);
    }
    
    /**
     * Submit the job and print out the job instance record.
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);

        verifyApplicationNameOrModuleName();
        
        String instanceId = getRestartToken();
        JobInstance jobInstance = null;
        boolean restart = false;
        
        if (instanceId != null) {
        	List<JobExecution> jobExecutions = getBatchRestClient().getJobExecutions(Long.decode(instanceId));
        	if (jobExecutions != null && jobExecutions.size() > 0) {
        		restart = isRestartable(jobExecutions.get(0).getBatchStatus());
        	} else {
        		restart = true;
        	}
        	if (!restart) {
        		getTaskArgs().clearRestartTokenFromFile("--restartTokenFile");
        	}
        }
        
        if (!restart) {
        	jobInstance =  getBatchRestClient().start( getApplicationName(),
        													   getModuleName(),
                                                               getComponentName(),
                                                               getJobXMLName(), 
                                                               getJobProperties(),
                                                               getJobXMLFile()) ;
        	issueJobSubmittedMessage(jobInstance);
        	issueJobInstanceMessage(jobInstance);
        	
        } else {
        	Long jobInstanceId = Long.decode(instanceId);
        	jobInstance = getBatchRestClient().restartJobInstance(jobInstanceId,  getJobProperties());
        	
            issueJobRestartedMessage(jobInstance);
            issueJobInstanceMessage(jobInstance);
        }

        if ( shouldWaitForTermination() ) {
            
            // Wait for the first JobExecution to show up
            JobExecution jobExecution = getPollingBatchRestClient().waitForLatestJobExecution( jobInstance );
            
            // Write the restart token here in case the shut down hook gets called.
            writeRestartToken(jobInstance.getInstanceId());
            // Now that we know the job has started, register the shutdown hook
            // that will stop the job we're waiting on if batchManager is abnormally terminated.
            Thread shutdownHook = (shouldAddShutdownHook()) ? addShutdownHook(jobInstance) : null;
            
            jobExecution = waitForTermination(jobInstance, jobExecution);
            // Job is not restartable, remove restart token
            if (!isRestartable(jobExecution.getBatchStatus())) {
            	getTaskArgs().clearRestartTokenFromFile("--restartTokenFile");
            }
            // TODO: try - finally ?
            removeShutdownHook(shutdownHook);
            
            return getProcessReturnCode( jobExecution ) ;

        } else {
        	writeRestartToken(jobInstance.getInstanceId());
            return 0;
        }
    }
    
    /**
     * @return the --jobXMLName arg.
     */
    protected String getJobXMLName() {
        return getTaskArgs().getStringValue("--jobXMLName");
    }
    
    /**
     * @return the --applicationName arg
     */
    protected String getApplicationName() {
        return getTaskArgs().getStringValue("--applicationName");
    }
    
    /**
     * @return the --moduleName arg
     */
    protected String getModuleName() {
        return getTaskArgs().getStringValue("--moduleName");
    }
    
    /**
     * @return the --componentName arg
     */
    protected String getComponentName() {
        return getTaskArgs().getStringValue("--componentName");
    }

    /**
     * @return the job properties, as specified by the --jobPropertiesFile arg.
     */
    protected Properties getJobProperties() throws IOException {
        return getTaskArgs().getJobParameters();
    }
    
    /**
     * @return the restart token, as specified by the --restartTokenFile arg.
     */
    protected String getRestartToken() throws IOException {
    	
    	Properties restartProps = getTaskArgs().getJobPropsFileProps("--restartTokenFile");
    	
        return restartProps.getProperty("restartJob");
    }
    
    /**
     * Write the restart token, if --restartTokenFile was specified.
     */
    protected void writeRestartToken(Long instanceId) throws IOException {
    	
       	getTaskArgs().writeRestartTokenFileValue("--restartTokenFile", instanceId.toString());
    	
     }
    
    /**
     * 
     * @return true if the given BatchStatus is in a "restartable" state (STOPPED, FAILED).
     */
    public boolean isRestartable( BatchStatus batchStatus ) {
        switch (batchStatus) {
            case STOPPED:
            case FAILED:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * @return the JobInstance ID to be restarted.
     */
    protected void verifyApplicationNameOrModuleName() throws IOException {
        
        if ( StringUtils.isEmpty( getApplicationName() )
              && StringUtils.isEmpty( getModuleName() ) ) {
            throw new ArgumentRequiredException("--applicationName or --moduleName");
        }
    }
    
    /**
     * @return the --jobXMLFile arg.
     */
    protected String getJobXMLFile() throws IOException {
        return readJSLFromFile(getTaskArgs().getStringValue("--jobXMLFile")); 
    }
    
    /**
     * Utility method to read a JSL file from the file system
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private String readJSLFromFile(String file) throws IOException
    {
        if (file != null) {
            return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        }
        
        return null;
    }
    
}
