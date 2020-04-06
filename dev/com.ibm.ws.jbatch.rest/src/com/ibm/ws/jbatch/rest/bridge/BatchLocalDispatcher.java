/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.bridge;

import java.util.Properties;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchInternalDispatcher;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.Step;

/**
 * Dispatches batch jobs locally (i.e the batch app lives in the same server).
 * 
 * Constructs the proper application runtime context (ComponentMetaData, ClassLoader)
 * in order to dispatch into the app.
 * 
 * The local dispatcher has lower service ranking than the BatchJmsDispatcher so that
 * if Jms is configured for dispatcher, Osgi framework will return the BatchJmsDispatcher
 * 
 * This is a wrapper to BatchInternalDispatcher to maintain the BatchDispatcher contract
 * and not return Future objects for work items started
 */
@Component(service = BatchDispatcher.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "service.ranking:Integer=2", "type=Local" })
public class BatchLocalDispatcher implements BatchDispatcher {
	
    /**
     * JobManager for internal use from REST layer
     */
	private BatchInternalDispatcher internalDispatcher;
	
    /**
     * For publishing job event
     */
    private BatchEventsPublisher eventsPublisher; 
    
    @Reference
    protected void setBatchInternalDispatcher(BatchInternalDispatcher ref){
    	this.internalDispatcher = ref;
    }
    
    protected void unsetInternalDispatcher(BatchInternalDispatcher svc) {
        if (svc == internalDispatcher) {
            internalDispatcher = null;
        }
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
	 * Bridge into the given app's runtime context and start the job.
	 * 
	 * @param jobInstance
	 * @param jobParameters
	 * 
	 */
    @Override
	public void start(WSJobInstance jobInstance, Properties jobParameters, long executionId)
			throws JobStartException, JobSecurityException{
	    
    	try{
    		internalDispatcher.start(jobInstance, jobParameters, executionId);
    	}catch ( JobStartException je ){
    		markInstanceExecutionFailed( jobInstance.getInstanceId(), executionId, this.getCorrelationId(jobParameters) );
    		throw je;
    	}catch ( JobSecurityException jse ){
    		markInstanceExecutionFailed( jobInstance.getInstanceId(), executionId, this.getCorrelationId(jobParameters) );
    		throw jse;
    	}catch ( RuntimeException rte ){
    		markInstanceExecutionFailed( jobInstance.getInstanceId(), executionId, this.getCorrelationId(jobParameters) );
    		throw rte;
    	}catch ( Exception e ){
    		markInstanceExecutionFailed( jobInstance.getInstanceId(), executionId, this.getCorrelationId(jobParameters) );
    	}
	}

    
    /**
	 * If couldn't start instance then mark instance/execution FAILED. 
	 * Example: Maybe couldn't locate application contents (JSL).
	 */
	@Override
    public void markInstanceExecutionFailed( long jobInstanceId, long executionId, String correlationId ){
		//Disregard any attempted transitions out of the ABANDONED state.
		if( internalDispatcher.getJobInstance(jobInstanceId).getBatchStatus() == BatchStatus.ABANDONED ){
			return;
		}
		internalDispatcher.markInstanceExecutionFailed(jobInstanceId, executionId );
		
		//Push event to Topic
		publishEvent( jobInstanceId, BatchEventsPublisher.TOPIC_INSTANCE_FAILED, correlationId);
		publishExecutionEvent( executionId, BatchEventsPublisher.TOPIC_EXECUTION_FAILED, correlationId);
	} 
	
    /**
	 * Lookup the app associated with the given executionId, then bridge into 
	 * the app's runtime context and restart the job.
	 */
	@Override
    public void restartInstance(long instanceId, Properties restartParameters, long executionId)
			throws JobExecutionAlreadyCompleteException,
			       NoSuchJobExecutionException, 
			       JobExecutionNotMostRecentException,
			       JobRestartException, 
			       JobSecurityException {

		try{
			internalDispatcher.restartInstance(instanceId, restartParameters, executionId);
	   	}catch ( JobRestartException jre ){
    		markInstanceExecutionFailed( instanceId, executionId, this.getCorrelationId(restartParameters) );
    		throw jre;
    	}catch ( JobSecurityException jse ){
    		markInstanceExecutionFailed( instanceId, executionId, this.getCorrelationId(restartParameters) );
    		throw jse;
    	}catch ( Exception e ){
    		markInstanceExecutionFailed( instanceId, executionId, this.getCorrelationId(restartParameters) );
    	}
	} 

	/**
	 * Lookup the app associated with the given executionId, then bridge into 
     * the app's runtime context and stop the job.
	 * @throws BatchJobNotLocalException 
	 * @throws BatchDispatcherException 
	 */
	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
			JobExecutionNotRunningException, JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {
	    
        internalDispatcher.stop(executionId);
	}
    
     /**
     * Start a sub-job partition thread.
     * 
     * This method is called only for multi-JVM partitions, which are started 
     * remotely by the top-level thread via JMS. 
     */
    @Override
    public void startPartition( PartitionPlanConfig partitionPlanConfig, 
                                Step step, 
                                PartitionReplyQueue partitionReplyQueue) {
        
        internalDispatcher.startPartition( partitionPlanConfig, step, partitionReplyQueue );
        
    }
    
    /**
     * This method should never be called.  It should only be called on the BatchJmsDispatcher impl.
     */
    @Override
    public PartitionReplyQueue createPartitionReplyQueue() throws BatchDispatcherException {
        throw new IllegalStateException( "Invalid call to BatchLocalDispatcher.createPartitionReplyQueue" );
    }
    
    /**
     * publish event for this instance
     * @param instanceId
     */
    private void publishEvent(long instanceId, String event, String correlationId){
        if (eventsPublisher != null) {
        	WSJobInstance updatedInstance = internalDispatcher.getJobInstance(instanceId);
        	if( updatedInstance != null){
        		eventsPublisher.publishJobInstanceEvent(updatedInstance, event, correlationId);
        	}
        }
    }
    
    /**
     * publish event for this execution
     * @param executionId
     */
    private void publishExecutionEvent(long executionId, String event, String correlationId){
        if (eventsPublisher != null) {
        	WSJobExecution updatedExecution = internalDispatcher.getJobExecution(executionId);
        	if(updatedExecution != null){
        		eventsPublisher.publishJobExecutionEvent(updatedExecution, event, correlationId);
        	}
        }
    }
    
	/**
	 * Find the correlation id if it has been passed.
	 */    
    private String getCorrelationId( Properties jobParameters ){
    	
        if (jobParameters != null){
        	return jobParameters.getProperty("com_ibm_ws_batch_events_correlationId", null);
        }else{
        	return null;
        }
    }
    
}
