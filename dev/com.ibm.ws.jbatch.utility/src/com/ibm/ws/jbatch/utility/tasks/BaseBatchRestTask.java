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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.http.HttpUtils;
import com.ibm.ws.jbatch.utility.rest.BatchRestClient;
import com.ibm.ws.jbatch.utility.rest.PollingBatchClient;
import com.ibm.ws.jbatch.utility.rest.WSPurgeResponse;
import com.ibm.ws.jbatch.utility.utils.ControlPropsTaskArgs;
import com.ibm.ws.jbatch.utility.utils.StringUtils;

/**
 * Common base class for tasks that use the BatchRestClient.
 */
public abstract class BaseBatchRestTask<T extends BaseBatchRestTask> extends BaseCommandTask {

    /**
     * Handles calls to the batch REST api.
     */
    private BatchRestClient batchRestClient;
    
    /**
     * Wrapper around batchRestClient.  Adds polling functionality.
     */
    private PollingBatchClient pollingBatchRestClient ;
    
    /**
     * Input args.
     */
    private ControlPropsTaskArgs taskArgs;

    /**
     * CTOR.
     */
    public BaseBatchRestTask(String taskName, String scriptName) {
        super(taskName, scriptName);
    }
    
    /**
     * @return task help txt
     */
    @Override
    public String getTaskHelp() {
        String taskName = getTaskName();
        return joinMsgs( getUsage("global.usage.options", scriptName, taskName),
                         getDesc(taskName + ".desc"),
                         collateRequiredOptions( getRequiredOptionsMessages() ),
                         collateOptionalOptions( getOptionalOptionsMessages() ) );
    }
    
    /**
     * @return List of 'required' option messages, to be displayed with task help.
     *         By default all connect.required* and [taskName].required* nls keys are read and returned.
     */
    protected List<String> getRequiredOptionsMessages() {
        List<String> retMe = new ArrayList<String>();
        
        retMe.add(buildOptionsMessage("connect.required-key", "connect.required-desc"));
        retMe.add(buildOptionsMessage(getTaskName() + ".required-key", getTaskName() + ".required-desc") );
        
        return retMe;
    }

    /**
     * @return List of 'optional' option messages, to be displayed with task help.
     *         By default all connect.optional* and [taskName].optional* nls keys are read and returned.
     */
    protected List<String> getOptionalOptionsMessages() {
        List<String> retMe = new ArrayList<String>();
        
        retMe.add( buildOptionsMessage("connect.optional-key", "connect.optional-desc") );
        retMe.add( buildOptionsMessage(getTaskName() + ".optional-key", getTaskName() + ".optional-desc") );
        
        return retMe;
    }
    
    /**
     * @return the list of command line options for this task.  This list is constructed
     *         using the options described in the nlsprops message file.
     */         
    protected List<String> getTaskOptions() {
        
        List<String> retMe = new ArrayList<String>();
        
        retMe.addAll( getNlsOptionNames( "connect.required-key." ) );
        retMe.addAll( getNlsOptionNames( "connect.optional-key." ) );
        retMe.addAll( getNlsOptionNames( getTaskName() + ".required-key." ) );
        retMe.addAll( getNlsOptionNames( getTaskName() + ".optional-key." ) );
        
        return retMe;
    }

    /**
     * Map and cache the input args.
     */
    protected T setTaskArgs( String[] args ) throws IOException {
        taskArgs = new ControlPropsTaskArgs( Arrays.copyOfRange(args, 1, args.length) );
        taskArgs.validateExpectedArgs( getTaskOptions() );
        
        if (taskArgs.isSpecified("--trustSslCertificates")) {
            HttpUtils.setDefaultTrustAllCertificates();
        }
        
        return (T) this;
    }
    
    /**
     * @return the input args map
     */
    protected ControlPropsTaskArgs getTaskArgs()  {
        return taskArgs;
    }
    
    /**
     * Open up a connection to the Batch REST API using the input args.
     * 
     * @return this
     */
    protected BatchRestClient buildBatchRestClient() {

        List<String> targets = StringUtils.split( getTaskArgs().getRequiredString("--batchManager"), ",");
        
        // User/pass is required.
        String user = getTaskArgs().getStringValue("--user");
        String password = getTaskArgs().getStringValue("--password");
        
        boolean reusePreviousParams = taskArgs.isSpecified("--reusePreviousParams");
        
        return new BatchRestClient( targets )
                        .setAuthorization(user, password) 
                        .setHttpTimeout( getHttpTimeout_ms() )
                        .setTaskIO( getTaskIO() )
                        .setReusePreviousParams( reusePreviousParams );
    }
    
    /**
     * @param batchRestClient 
     * 
     * @return this
     */
    protected T setBatchRestClient(BatchRestClient batchRestClient) {
        this.batchRestClient = batchRestClient;
        return (T) this;
    }
    
    /**
     * @return the batch rest api object.
     */
    protected BatchRestClient getBatchRestClient() {
        
        if (batchRestClient == null) {
            setBatchRestClient( buildBatchRestClient() );
        }
        
        return batchRestClient;
    }
    
    /**
     * @param batchRestClient 
     * 
     * @return this
     */
    protected T setPollingBatchRestClient(PollingBatchClient pollingBatchClient) {
        this.pollingBatchRestClient = pollingBatchClient;
        return (T) this;
    }
    
    /**
     * @return the batch rest api object.
     */
    protected PollingBatchClient getPollingBatchRestClient() {
        
        if (pollingBatchRestClient == null) {
            setPollingBatchRestClient( new PollingBatchClient( getBatchRestClient(), getPollingInterval_ms() )
                                                .setVerboseWait( getVerbose(), getTaskIO() ) );
        }
        
        return pollingBatchRestClient;
    }

    /**
     * @return the polling sleep time, in ms.
     */
    protected long getPollingInterval_ms() {
        return getTaskArgs().getLongValue("--pollingInterval_s", 30L) * 1000; 
    }
    
    /**
     * @return the --httpTimeout, in ms
     */
    protected int getHttpTimeout_ms() {
        return getTaskArgs().getIntValue("--httpTimeout_s", 30) * 1000;
    }
    
    /**
     * @return true if --verbose was specified
     */
    protected boolean getVerbose() {
        return getTaskArgs().isSpecified("--verbose");
    }
    
  
    /**
     * Issue the job.submitted message to stdout
     */
    protected void issueJobSubmittedMessage(JobInstance jobInstance) {
        getTaskIO().info(getMessage("job.submitted", 
                                  jobInstance.getJobName(), 
                                  String.valueOf(jobInstance.getInstanceId())));
    }
    
    /**
     * Issue the job.finished message to stdout
     */
    protected void issueJobFinishedMessage(JobInstance jobInstance, JobExecution jobExecution) {
        getTaskIO().info(getMessage("job.finished", 
                                  jobInstance.getJobName(), 
                                  String.valueOf(jobInstance.getInstanceId()),
                                  String.valueOf(jobExecution.getBatchStatus()),
                                  jobExecution.getExitStatus()));
    }
    
    /**
     * Issue the job.purged message to stdout
     */
    protected void issueJobPurgedMessage(JobInstance jobInstance) {
        getTaskIO().info(getMessage("job.purged", 
                                  jobInstance.getJobName(), 
                                  String.valueOf(jobInstance.getInstanceId())));
    }
    
    /**
     * Issue the job.finished message to stdout
     */
    protected void issueJobRestartedMessage(JobInstance jobInstance) {
        getTaskIO().info(getMessage("job.restarted", 
                                  jobInstance.getJobName(), 
                                  String.valueOf(jobInstance.getInstanceId())));
    }
    
    /**
     * Issue the job.finished message to stdout
     */
    protected void issueJobStoppedMessage(JobInstance jobInstance, JobExecution jobExecution) {
        if (jobExecution != null) {
            getTaskIO().info(getMessage("job.stopped", 
                        jobInstance.getJobName(), 
                        String.valueOf(jobInstance.getInstanceId()),
                        String.valueOf(jobExecution.getBatchStatus()),
                        jobExecution.getExitStatus()));
        } else {
            getTaskIO().info(getMessage("job.stopped", 
                    jobInstance.getJobName(), 
                    String.valueOf(jobInstance.getInstanceId()),
                    null,
                    null));
        }
    }
    
    /**
     * Issue the job.stop.submitted message to stdout
     */
    protected void issueJobStopSubmittedMessage(JobInstance jobInstance) {
        getTaskIO().info(getMessage("job.stop.submitted", 
                                  jobInstance.getJobName(), 
                                  String.valueOf(jobInstance.getInstanceId())));
    }
    
    /**
     * Issue the shutdown.hook message to stdout
     */
    protected void issueShutdownHookMessage(JobInstance jobInstance) {
        getTaskIO().info(getMessage("shutdown.hook", 
                                    jobInstance.getJobName(), 
                                    String.valueOf(jobInstance.getInstanceId())));
    }
    
    /**
     * print out the given jobexecution record.
     */
    protected void issueJobExecutionMessage(JobExecution jobExecution) {
        getTaskIO().info( getMessage("job.execution.record", jobExecution.toString() ) );
    }
    
    /**
     * print out the given jobinstance record.
     */
    protected void issueJobInstanceMessage(JobInstance jobInstance) {
        getTaskIO().info( getMessage("job.instance.record", jobInstance.toString() ) );
    }
    
    /**
     * Shift BatchStatus ordinal values (0->6) into a higher range when using
     * them as the utility's exit code to avoid conflicting with popular exitStatus 
     * return codes (e.g. 0, 4, 8). 
     * 
     * This range is documented so that customers know which RC ranges they should
     * avoid using in their apps.
     */
    protected static final int BatchStatusOrdinalValueRangeShift = 30;
    
    /**
     * @return the utility RC for the given BatchStatus (BatchStatus.ordinal + 30).
     */
    protected int getBatchStatusReturnCode( BatchStatus batchStatus ) {
        return batchStatus.ordinal() + BatchStatusOrdinalValueRangeShift;
    }
    
    /**
     * Issue the job.purged.multi message to stdout
     */
    protected void issueJobPurgedMessage(WSPurgeResponse response) {
        getTaskIO().info(getMessage("job.purged.multi", 
                                 String.valueOf(response.getInstanceId()),
                                 response.getPurgeStatus(),
                                 response.getMessage(),
                                 response.getRedirectUrl()));
    }

}

