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
package com.ibm.ws.jbatch.rest.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.ExecutionAssignedToServerException;
import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.BatchStatusValidator;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.JobStoppedOnStartException;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobOperator;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.joblog.JobInstanceLog;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;
import com.ibm.ws.jbatch.rest.BatchManager;
import com.ibm.ws.jbatch.rest.JPAQueryHelperImpl;
import com.ibm.ws.jbatch.rest.utils.PurgeStatus;
import com.ibm.ws.jbatch.rest.utils.ResourceBundleRest;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.ws.jbatch.rest.utils.WSPurgeResponse;
import com.ibm.ws.jbatch.rest.utils.WSSearchObject;

/**
 * Batch management.
 * 
 * Called by the REST handler and WOLA handler (future).
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class BatchManagerImpl implements BatchManager {
    
    /**
     * For dispatching job requests.
     */
    private BatchDispatcher batchDispatcher;
    
    /**
     * For job management.
     */
    private WSJobOperator wsJobOperator;
    
    /**
     * The batch persistence layer.
     */
    private WSJobRepository jobRepository;
    
    /**
     * For creating J2EENames.
     */
    private J2EENameFactory j2eeNameFactory;
    
    /**
     * Batch role based authorization server
     */
    private WSBatchAuthService authService;
    
    /**
     * For publishing job event
     */
    private BatchEventsPublisher eventsPublisher; 
    
    /**
     * To access job logs.
     */
    private IJobLogManagerService jobLogManagerService;
    
    /**
     * DS inject.
     */
    @Reference
    protected void setWSJobOperator(WSJobOperator ref) {
        this.wsJobOperator = ref;
    }

    /**
     * DS injection
     * Use "GREEDY" to pick up the higher ranking implementation
     */
    @Reference(policyOption=ReferencePolicyOption.GREEDY)
    protected void setBatchDispatcher(BatchDispatcher ref) {
        this.batchDispatcher = ref;
    }
    
    /**
     * DS injection
     */
    @Reference
    protected void setWSJobRepository(WSJobRepository ref) {
        this.jobRepository = ref;
    }
    
    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (eventsPublisher == publisher)
            eventsPublisher = null;
    }
    
    /**
     * DS injection
     */
    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSBatchAuthService(WSBatchAuthService bas) {
        this.authService = bas;
    }
    
    protected void unsetWSBatchAuthService(WSBatchAuthService bas) {
        if (this.authService == bas) {
        	this.authService = null;
        }
    }
    
    /**
     * @return a J2EEName for the given app / module / comp.
     */
    protected J2EEName createJ2EEName(String app, String module, String comp) {
        return j2eeNameFactory.create( getAppName(app, module),
                                       getModuleName(app, module),
                                       (StringUtils.isEmpty(comp) ? null : comp) );
    }
    
    /**
     * @return app, if not null; otherwise return the module name with the ".war/.jar" suffix removed.
     */
    protected String getAppName(String app, String module) {
        return ( !StringUtils.isEmpty(app) ) 
                    ? app
                    : StringUtils.trimSuffixes(module, ".war", ".jar");
    }
    
    /**
     * @return module, if not null; otherwise return "{app}.war"
     */
    protected String getModuleName(String app, String module) {
        return (!StringUtils.isEmpty(module)) ? module : app + ".war";
    }    
 
    /**
     * DS injection
     * 
     * Note: The dependency is required; however we mark it OPTIONAL to ensure that
     *       the REST handler is started even if the batch container didn't, so we
     *       can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL,
               policy=ReferencePolicy.DYNAMIC,
               policyOption=ReferencePolicyOption.GREEDY)
    protected void setIJobLogManagerService(IJobLogManagerService jobLogManagerService) {
        this.jobLogManagerService = jobLogManagerService;
    }

    /**
     * DS un-setter.
     */
    protected void unsetIJobLogManagerService(IJobLogManagerService jobLogManagerService) {
        if (this.jobLogManagerService == jobLogManagerService) {
            this.jobLogManagerService = null;
        }
    }
    
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) throws Exception {
    }

    protected boolean deactivated=false;
    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> config) throws Exception {
        deactivated = true;
    }

    /**
     * Purge the data for a specified instance id
     * 
     * @param instanceId the job instance to purge
     * @param purgeJobStoreOnly Whether or not to purge only the job store data
     * 
     * @return The results of the purge
     */
    @Override
    public WSPurgeResponse purge(long jobInstanceId, boolean purgeJobStoreOnly) {
    	
    	WSPurgeResponse purgeResponse = new WSPurgeResponse(jobInstanceId,PurgeStatus.COMPLETED, "Successful purge.", null);
    	
    	try {
            if (!jobRepository.isJobInstancePurgeable(jobInstanceId)) {
            	purgeResponse.setMessage("The specified job instance, " + jobInstanceId + ", cannot be purged because it has active job executions.");
            	purgeResponse.setPurgeStatus(PurgeStatus.STILL_ACTIVE);
            	return purgeResponse;
            }
    	} catch (NoSuchJobInstanceException e) {
    		//Defect 195757: changing exception to have a translated message
            purgeResponse.setMessage(ResourceBundleRest.getMessage("job.instance.not.found", jobInstanceId));
            purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            return purgeResponse;
    	} 
    	   	
        if (!purgeJobStoreOnly) { //Try to purge job logs and DB, or send a redirect if not local
        	try {
          	    boolean fileSuccess = purgeJobLogFiles(jobInstanceId);
            	
            	if (fileSuccess) {
            		boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);
            		
                    if (!dbSuccess) {
                    	purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
                                + "The job logs were sucessfully deleted but not all database entries were deleted.");
                    	purgeResponse.setPurgeStatus(PurgeStatus.JOBLOGS_ONLY);
                    }
            	} else {
            		 purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
                             + "Not all job log files were deleted so no attempt was made to delete database entries.");
                     purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            	}
            	
        	} catch (BatchJobNotLocalException e) {
        		
        		purgeResponse.setMessage("The request cannot be completed because the job execution did not run on this server.");
                purgeResponse.setPurgeStatus(PurgeStatus.NOT_LOCAL);
        	}
        	
        	
        } else { //We won't even try to purge job logs. Only purge from database.
        	
        	boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);        	
        	
            if (!dbSuccess) {
            	
            	purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). Not all database entries were deleted.");
                purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            }        	 
        }
        
        return purgeResponse;
    }
    
    /**
     * Purge the data for a range of instance ids
     * 
     * @param purgeJobStoreOnly Whether or not to purge only the job store data
     * @param page Indicates which page (subset of records) to use in the purge query
     * @param pageSize Indicates the number of records per page.
     * @param instanceIds The range of instance ids to use in the purge query
     * @param createTime Date (or range of dates) to use in the purge query
     * @param instanceState Instance state (or list of instances states) to use in the purge query
     * @param exitStatus The exit status string to filter on in the purge query
     * 
     * @return 
     */
    @Override
    public List<WSPurgeResponse> purge(boolean purgeJobStoreOnly, int page, int pageSize, String instanceIds, String createTime, String instanceState, String exitStatus) {
    	
    	WSSearchObject wsso = null;
    	
    	try {
            wsso = new WSSearchObject(instanceIds, createTime, instanceState, exitStatus);
       } catch(Exception e) {
    	   ArrayList<WSPurgeResponse> purgeResponseList = new ArrayList<WSPurgeResponse>(1);
    	   WSPurgeResponse purgeResponse = new WSPurgeResponse(-1,PurgeStatus.FAILED, "An error occurred while processing the specified parameters", null);
    	   purgeResponseList.add(purgeResponse);
    	   return purgeResponseList;
       }
    	
    	// Query for jobs
        List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso), page, pageSize);
        
        // Extract job instance ids returned by the query given the input parameters
        ArrayList<Long> instanceList = new ArrayList<Long>(jobInstances.size());
        for(WSJobInstance job : jobInstances) {
            instanceList.add(job.getInstanceId());
        }
        
        
        // Purge
        ArrayList<WSPurgeResponse> purgeResponseList = new ArrayList<WSPurgeResponse>(instanceList.size());
        for (long instanceId : instanceList) {
            try {
                purgeResponseList.add(purge(instanceId, purgeJobStoreOnly));
            } catch (Exception e) {
                purgeResponseList.add(new WSPurgeResponse(instanceId, 
                        PurgeStatus.FAILED, "Exception ocurred during purge of job instance " + instanceId + " : " + e.getMessage(), 
                        null));
                
            }
        }
        
        return purgeResponseList;
    }

    /**
     * Purge the job log files in the file system
     * 
     * @param jobInstanceId The instance id of the job log files to purge
     * @return true if the purge of the log files was successful, otherwise false
     * @throws BatchJobNotLocalException
     */
	private boolean purgeJobLogFiles(long jobInstanceId) throws BatchJobNotLocalException{
        JobInstanceLog instanceLog = null;
        try {
            instanceLog = jobLogManagerService.getJobInstanceLog(jobInstanceId);
        } catch (NoSuchJobInstanceException e) {
            throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
        }
        boolean fileSuccess = instanceLog.purge();
        
        return fileSuccess;
    }
	
    /*
     * Find the correlation id if it has been passed.
     */
    private String getCorrelationId(Properties jobParameters) {

        if (jobParameters != null) {
            return jobParameters.getProperty("com_ibm_ws_batch_events_correlationId", null);
        } else {
            return null;
        }
    }

	/**
     * Create a new JobInstance and start it.
     * 
     * @param appName the batch app name
     * @param moduleName the target module within the app
     * @param compName the target EJB within the module (may be null for WARs).
     * @param jobXMLName the name of the JSL file
     * @param jobParameters
     * @param jsl the full JSL String 
     * 
     * @return JobInstance of newly started job
     * @throws BatchDispatcherException encounters when dispatching a job
     * 
     * Note:  Inline JSL takes precedence over JSL within .war
     */
    @Override
    @FFDCIgnore( JobStoppedOnStartException.class )
    public WSJobInstance start(String appName,
                             String moduleName,
                             String compName,
                             String jobXMLName, 
                             Properties jobParameters,
                             String jsl) throws BatchDispatcherException {
        
        J2EEName j2eeName = createJ2EEName(appName, moduleName, compName);
        
        String correlationId = getCorrelationId(jobParameters);
       	WSJobInstance jobInstance = wsJobOperator.createJobInstance(j2eeName.toString(), jobXMLName, jsl, correlationId); 
        WSJobExecution jobExecution = jobRepository.createJobExecution(jobInstance.getInstanceId(), jobParameters);
        // Publish start event
        if (eventsPublisher != null) {
        	eventsPublisher.publishJobExecutionEvent(jobExecution, BatchEventsPublisher.TOPIC_EXECUTION_STARTING, correlationId);
        }
        
        try {
            batchDispatcher.start(jobExecution.getJobInstance(), jobParameters, jobExecution.getExecutionId());
        } catch (JobStoppedOnStartException e) {
            // Do not want this exception to flow back to the client.
        }
        // Note making another call to DB has the side effect of populating certain fields, like
        // the jobName, that were not previously present (say in the 'jobInstance' variable above).
        // 
        // This is a significant side effect.  It means in a single server we'll always get back
        // the jobName on submission.
        return jobRepository.getJobInstanceFromExecution(jobExecution.getExecutionId());
    }
   
    
    /**
     * Stop the job with the given jobInstanceId
     * 
     * @param jobInstanceId the job to stop
     * @throws BatchJobNotLocalException 
     * @throws JobSecurityException 
     */
    @Override
    public long stopJobInstance(long jobInstanceId) throws BatchNoSuchJobInstanceException,
                                                           BatchJobExecutionNotRunningException,
                                                           BatchContainerRuntimeException, JobSecurityException, BatchJobNotLocalException {
        
        WSJobExecution jobExec = (WSJobExecution)getMostRecentJobExecutionFromInstance(jobInstanceId);

        //This is a special case where we need to do an auth check before we get to the dispatcher.
        //We don't want to get all the way to the endpoint to perform an authcheck for a restart.
        //Same thing with a stop operation.
        if (jobExec == null && authService != null) {
            authService.authorizedJobStopByInstance(jobInstanceId);
        }

        if (jobExec == null) {
            long execId = -1;
            // No job executions to stop.  Update the status to stopped.
            // Could be old code where there are no executions to stop.
            WSJobInstance jobInstance = jobRepository.getJobInstance(jobInstanceId);

            jobRepository.updateJobInstanceWithInstanceStateAndBatchStatus(jobInstanceId, InstanceState.STOPPED, BatchStatus.STOPPED);

            // Publish stop event
            if (eventsPublisher != null) {
                eventsPublisher.publishJobInstanceEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_STOPPING, null);
                eventsPublisher.publishJobInstanceEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_STOPPED, null);
            }             
            
            return execId;
        } else {
        	try{
        		return stopJobExecution(jobExec.getExecutionId());
        	}catch(BatchJobExecutionNotRunningException e){
                throw new BatchJobExecutionNotRunningException(e,e.getJobExecutionId(),jobInstanceId);
        	}
        }

    }
    
    /**
     * 
     * @return the most recent JobExecution for the given job instance.
     */
    @FFDCIgnore( IllegalStateException.class )
    protected JobExecution getMostRecentJobExecutionFromInstance(long jobInstanceId) throws BatchNoSuchJobInstanceException {
        try {
            return jobRepository.getMostRecentJobExecutionFromInstance(jobInstanceId);
        } catch (NoSuchJobInstanceException e) {
            throw new BatchNoSuchJobInstanceException( e, jobInstanceId );
        } catch (IllegalStateException i) {
            return null;
        }
    }
    
    /**
     * @param jobInstanceId the job to restart
     */
    @Override
    public WSJobInstance restartJobInstance(long jobInstanceId, 
                                          Properties jobParameters) throws BatchNoSuchJobInstanceException, 
                                                                           BatchContainerRuntimeException,
                                                                           BatchJobExecutionAlreadyCompleteException,
                                                                           BatchJobRestartException {
        long execId = -1;
        WSJobExecution jobExec = (WSJobExecution)getMostRecentJobExecutionFromInstance(jobInstanceId);

        //This is a special case where we need to do an auth check before we get to the dispatcher.
        //We don't want to get all the way to the endpoint to perform an authcheck for a restart.
        //Same thing with a stop operation.
        if (authService != null) {
       		authService.authorizedJobRestartByInstance(jobInstanceId);
        }
 
        try {
            // Case of job start with the old code and a restart with the new code where 
            // the execution is created on the Dispatcher at start.
            if (jobExec == null) {
                BatchStatusValidator.validateStatusAtInstanceRestart(jobInstanceId, jobParameters);
            } else {
                execId = jobExec.getExecutionId();
                
                //Checks if job execution or steps are not running
                BatchStatusValidator.validateStatusAtExecutionRestart(execId, jobParameters);
            }
                
            //Set instance state to submitted to be consistent with start            
            jobRepository.updateJobInstanceStateOnRestart(jobInstanceId);
            //Create new execution record
            WSJobExecution exec = jobRepository.createJobExecution(jobInstanceId, jobParameters);
            execId = exec.getExecutionId();
            if (eventsPublisher != null) {
            	String correlationId = getCorrelationId(jobParameters);
           		eventsPublisher.publishJobExecutionEvent(exec, BatchEventsPublisher.TOPIC_EXECUTION_STARTING, correlationId);
            }
            
            
            batchDispatcher.restartInstance(jobInstanceId, jobParameters, exec.getExecutionId());
            
            return jobRepository.getJobInstance(jobInstanceId);

        } catch(JobExecutionAlreadyCompleteException e) {
            throw new BatchJobExecutionAlreadyCompleteException(e, execId, jobInstanceId);
        } catch(JobRestartException e) {
            throw new BatchJobRestartException(e, execId, jobInstanceId);
        }
    }
    
    /**
     * @param jobExecutionId the job execution to stop
     * @throws BatchJobNotLocalException 
     * @throws BatchDispatcherException 
     * @throws JobSecurityException 
     */
    @Override
    public long stopJobExecution(long jobExecutionId) throws JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {
        
        //This is a special case where we need to do an auth check before we get to the dispatcher.
        //We don't want to get all the way to the endpoint to perform an authcheck for a restart.
        //Same thing with a stop operation.
        try {
        	if (authService != null) {
        	    authService.authorizedJobStopByExecution(jobExecutionId);
        	}
        } catch (NoSuchJobExecutionException e ) {
            throw new BatchNoSuchJobExecutionException(e, jobExecutionId);
        }        
        
        WSJobExecution jobExecution = jobRepository.getJobExecution(jobExecutionId);
        try {
            if (!jobExecution.getServerId().equals("")) { 
                batchDispatcher.stop(jobExecutionId);
                return jobExecutionId;
            } else {
                // No job executions to stop.  Update the status to stopped.
                // The execution has not yet reached the endpoint.
                try {
                    jobRepository.updateJobExecutionAndInstanceNotSetToServerYet(jobExecutionId, new Date());
                } catch (ExecutionAssignedToServerException e) {
                	batchDispatcher.stop(jobExecutionId);
                    return jobExecutionId;
                }

                // Publish stop event
                if (eventsPublisher != null) {
                    eventsPublisher.publishJobInstanceEvent(jobExecution.getJobInstance(), BatchEventsPublisher.TOPIC_INSTANCE_STOPPING, null);
                    eventsPublisher.publishJobInstanceEvent(jobExecution.getJobInstance(), BatchEventsPublisher.TOPIC_INSTANCE_STOPPED, null);
                }             
                
                return jobExecutionId;
            }
            
        } catch (JobExecutionNotRunningException e) {
            throw new BatchJobExecutionNotRunningException(e, jobExecutionId);
        } catch (NoSuchJobExecutionException e ) {
            throw new BatchNoSuchJobExecutionException(e, jobExecutionId);
        }   
     }
    
    /**
     * @param jobExecutionId the job execution/instance to restart
     * @param jobParams restart parameters
     * 
     * @return the JobInstance of the restarted job (TODO: return JobExecution? )
     */
    @Override
    public WSJobInstance restartJobExecution(long jobExecutionId, Properties jobParams) {
    	
        long previousExecId = jobExecutionId;
        //This is a special case where we need to do an auth check before we get to the dispatcher.
        //We don't want to get all the way to the endpoint to perform an authcheck for a restart.
        //Same thing with a stop operation.
    	try {
    		if (authService != null) {
    		    authService.authorizedJobRestartByExecution(previousExecId);
    		}
    	} catch (NoSuchJobExecutionException e) {
            throw new BatchNoSuchJobExecutionException(e, previousExecId);
        }
    
        try {
        	//Checks if job execution or steps are not running
        	BatchStatusValidator.validateStatusAtExecutionRestart(previousExecId, jobParams);
        	long instanceId = jobRepository.getJobInstanceFromExecution(previousExecId).getInstanceId();
        	//Set instance state to submitted to be consistent with start     	
            jobRepository.updateJobInstanceStateOnRestart(instanceId);
        	//Create new execution record
        	WSJobExecution exec = jobRepository.createJobExecution(instanceId, jobParams);
        	long newExecId = exec.getExecutionId();
            if (eventsPublisher != null) {
            	String correlationId = getCorrelationId(jobParams);
           		eventsPublisher.publishJobExecutionEvent(exec, BatchEventsPublisher.TOPIC_EXECUTION_STARTING, correlationId);
            }
        	
            batchDispatcher.restartInstance(instanceId, jobParams, newExecId);
            
            return jobRepository.getJobInstanceFromExecution(newExecId);

        } catch(JobExecutionAlreadyCompleteException e) {
            throw new BatchJobExecutionAlreadyCompleteException(e, previousExecId);
        } catch(JobRestartException e) {
            throw new BatchJobRestartException(e, previousExecId);
        } catch (NoSuchJobExecutionException e ) {
            throw new BatchNoSuchJobExecutionException(e, previousExecId);
        }
        
    }
    
}