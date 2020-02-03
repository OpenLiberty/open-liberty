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
package com.ibm.ws.jbatch.rest;

import java.util.List;
import java.util.Properties;

import javax.batch.operations.JobSecurityException;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.ws.jbatch.rest.utils.WSPurgeResponse;
import com.ibm.ws.jbatch.rest.internal.BatchJobExecutionNotRunningException;
import com.ibm.ws.jbatch.rest.internal.BatchNoSuchJobInstanceException;

/**
 * The BatchManager handles job management requests, e.g. starting, stopping,
 * restarting, etc.
 * 
 * The BatchManager is called by the REST API and by BatchWolaListener.
 * 
 * Because of this, any REST-specific behavior like HTTP redirect must NOT be
 * done at this level, but at the level of the REST code which acts as a BatchManager
 * client.
 */
public interface BatchManager {

    /**
     * BatchJmsDispatcher will return this constant when
     * BatchDispatcher.start() is invoked to indicate that it does not
     * have execution id.
     */
    public static final long REMOTE_DISPATCH_NO_EXECUTION_ID = -1;

    /**
     * Create a new JobInstance and start it.
     * 
     * @param appName the batch app name
     * @param moduleName the target module within the app
     * @param compName the target EJB within the module (may be null for WARs).
     * @param jobXMLName the name of the JSL file
     * @param jobParameters
     * @param jsl the JSL XML String passed in with the job submission request
     * 
     * Note:  Inline JSL takes precedence over JSL within .war
     * 
     * @return JobInstance of newly started job
     */
    WSJobInstance start(String appName, 
                      String moduleName,
                      String compName, 
                      String jobXMLName, 
                      Properties jobParameters,
                      String jsl);
    
    /**
     * Stop the job with the given jobInstanceId
     * 
     * @param jobInstanceId the job to stop
     * 
     * @return the jobExecutionId that was stopped.
     * @throws BatchJobNotLocalException 
     * @throws JobSecurityException 
     * @throws BatchContainerRuntimeException 
     * @throws BatchJobExecutionNotRunningException 
     * @throws BatchNoSuchJobInstanceException 
     */
    long stopJobInstance(long jobInstanceID) throws BatchNoSuchJobInstanceException, BatchJobExecutionNotRunningException, BatchContainerRuntimeException, JobSecurityException, BatchJobNotLocalException;
    
    /**
     * Stop a running job execution.
     * 
     * @param jobExecutionId the job execution to stop
     * 
     * @return the jobExecutionId that was stopped.
     * @throws BatchJobNotLocalException 
     * @throws BatchDispatcherException 
     * @throws JobSecurityException 
     */
    long stopJobExecution(long jobExecutionId) throws JobSecurityException, BatchDispatcherException, BatchJobNotLocalException;

    /**
     * Restart a job instance.
     * 
     * @param jobInstanceId the job to restart
     */
    WSJobInstance restartJobInstance(long jobInstanceID, Properties jobParams);

    /**
     * Restart a job execution.
     * 
     * @param jobExecutionId the job execution/instance to restart
     * @param jobParams restart parameters
     * 
     * @return the JobInstance of the restarted job (TODO: return JobExecution? )
     */
    WSJobInstance restartJobExecution(long jobExecutionId, Properties jobParams);
    
    /**
     * Purge the data for a specified instance id
     * 
     * @param instanceId the job instance to purge
     */
    WSPurgeResponse purge(long jobInstanceID, boolean purgeJobStoreOnly);
    
    /**
     * Purge a range of job instance data
     * 
     * @param purgeJobStoreOnly
     * @param page
     * @param pageSize
     * @param instanceId
     * @param createTime
     * @param instanceState
     * @param exitStatus
     * @return
     * @throws IOException
     */
    List<WSPurgeResponse> purge(boolean purgeJobStoreOnly, int page, int pageSize, String instanceId, String createTime, String instanceState, String exitStatus);
    
}
