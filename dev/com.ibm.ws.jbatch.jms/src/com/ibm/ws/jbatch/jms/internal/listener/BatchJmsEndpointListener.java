/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal.listener;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchIllegalIDPersistedException;
import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.ws.BatchInternalDispatcher;
import com.ibm.jbatch.container.ws.BatchStatusValidator;
import com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.JobInstanceNotQueuedException;
import com.ibm.jbatch.container.ws.JobStoppedOnStartException;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyMsg.PartitionReplyMsgType;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobOperator;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.BatchGroupSecurityHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.jbatch.jms.internal.BatchJmsEnvHelper;
import com.ibm.ws.jbatch.jms.internal.BatchJmsMessage;
import com.ibm.ws.jbatch.jms.internal.BatchOperationGroup;
import com.ibm.ws.jbatch.jms.internal.dispatcher.PartitionReplyQueueJms;
import com.ibm.ws.jbatch.jms.internal.dispatcher.StartPartitionPayload;
import com.ibm.ws.jbatch.rest.bridge.BatchContainerAppNotFoundException;
import com.ibm.ws.jbatch.rest.internal.BatchGroupSecurityHelperImpl;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializer;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Batch JMS Endpoint listener. The onMessage() method will be invoked when
 * there is message arriving at batch activation spec destination queue. Only
 * messages that satisfied the message selector string (as defined in the
 * activation spec) will be visible. For example, with the activation spec
 * defined below, this listener will receive JMS message with property "AppName"
 * set to either SleepyBatchlet or BonusPayOut
 * 
 * <jmsActivationSpec id="jbatchActivationSpec"> <properties.wasJms
 * messageSelector=
 * "com_ibm_ws_batch_applicationName = 'SleepyBatchlet' OR com_ibm_ws_batch_applicationName = 'BonusPayOut'"
 * destinationRef="jobSubmissionQueue" destinationType="javax.jms.Queue" />
 * </jmsActivationSpec>
 * 
 * TODO: check behavior of message redelivery and transaction roll back.
 */
public class BatchJmsEndpointListener implements MessageListener {

    private static final TraceComponent tc = Tr.register(BatchJmsEndpointListener.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");    
    private static final long NO_INSTANCE_ID = -1;
    private static final long NO_EXECUTION_ID = -1;
    

    /*
     * ConnectionFactory instance to create connections for partitionReplyQueue
     */
    private ConnectionFactory connectionFactory ;
    private BatchOperationGroup batchOperationGroup;
   
    /*
     * @param ConnectionFactory configured to use the partition queue
     */
    public BatchJmsEndpointListener(ConnectionFactory cf, BatchOperationGroup batchOpGrp, WSJobRepository jobRepo){
        
        // track the batch operation group name(s)
        this.batchOperationGroup = batchOpGrp;
        this.connectionFactory = cf;
    }
    
    /**
     * Get BatchLocalDispatcher service
     * 
     * @return BatchInternalDispatcher
     */
    protected synchronized BatchInternalDispatcher getInternalDispatcherInstance() {

        BundleContext bundleContext = FrameworkUtil.getBundle(BatchInternalDispatcher.class).getBundleContext();
        BatchInternalDispatcher retMe = null;
        try {
            Collection<ServiceReference<BatchInternalDispatcher>> srs = bundleContext.getServiceReferences(BatchInternalDispatcher.class, null);
            if (srs.isEmpty()) {
                throw new BatchContainerRuntimeException("Unable to obtain BatchInternalDispatcher");
            }

            ServiceReference<BatchInternalDispatcher> batchDispatcherSR = srs.iterator().next();
            retMe  = bundleContext.getService(batchDispatcherSR);

        } catch (InvalidSyntaxException e) {
            throw new BatchContainerRuntimeException(e);
         }
        return retMe;
    }
    
    /**
     * Obtain WSContextService
     * 
     * @return WSContextService
     */
    protected synchronized WSContextService getWSContextServiceInstance() {
        BundleContext bundleContext = FrameworkUtil.getBundle(WSContextService.class).getBundleContext();
        WSContextService retMe = null;
        try {
            Collection<ServiceReference<WSContextService>> srs = bundleContext.getServiceReferences(WSContextService.class, null);
            if (srs.isEmpty()) {
                throw new BatchContainerRuntimeException("Unable to obtain WSContextService");
            }

            ServiceReference<WSContextService> wsContextServiceSR = srs.iterator().next();
            retMe  = bundleContext.getService(wsContextServiceSR);

        } catch (InvalidSyntaxException e) {
            throw new BatchContainerRuntimeException(e);
         }
        return retMe;
    }

    /**
     * Get WSJobRepository service
     * 
     * @return
     */
    protected synchronized WSJobRepository getWSJobRepositoryInstance() {

        BundleContext bundleContext = FrameworkUtil.getBundle(WSJobRepository.class).getBundleContext();
        WSJobRepository retMe = null;
        try {
            Collection<ServiceReference<WSJobRepository>> srs = bundleContext.getServiceReferences(WSJobRepository.class, null);
            if (srs.isEmpty()) {
                throw new BatchContainerRuntimeException("Unable to obtain WSJobRepository");
            }

            ServiceReference<WSJobRepository> wsJobRepositorySR = srs.iterator().next();
            retMe  = bundleContext.getService(wsJobRepositorySR);

        } catch (InvalidSyntaxException e) {
            throw new BatchContainerRuntimeException(e);
         }
        return retMe;
    }

    /**
     * Get WSJobOperator service
     * 
     * @return
     */
    protected synchronized WSJobOperator getWSJobOperatorInstance() {

        BundleContext bundleContext = FrameworkUtil.getBundle(WSJobOperator.class).getBundleContext();
        WSJobOperator retMe = null;
        try {
            Collection<ServiceReference<WSJobOperator>> srs = bundleContext.getServiceReferences(WSJobOperator.class, null);
            if (srs.isEmpty()) {
                throw new BatchContainerRuntimeException("Unable to obtain WSJobOperator");
            }

            ServiceReference<WSJobOperator> wsJobOperatorSR = srs.iterator().next();
            retMe  = bundleContext.getService(wsJobOperatorSR);

        } catch (InvalidSyntaxException e) {
           throw new BatchContainerRuntimeException(e);
        }
        return retMe;
    }
    
  
    /**
     * Get WSJobOperator service
     * 
     * @return
     */
    protected synchronized BatchEventsPublisher getBatchEventsPublisher() {

        BundleContext bundleContext = FrameworkUtil.getBundle(BatchEventsPublisher.class).getBundleContext();
        BatchEventsPublisher retMe = null;
        try {
            Collection<ServiceReference<BatchEventsPublisher>> srs = bundleContext.getServiceReferences(BatchEventsPublisher.class, null);
            if (! srs.isEmpty()) {
                 ServiceReference<BatchEventsPublisher> eventsPublisherSR = srs.iterator().next();
                 retMe  = bundleContext.getService(eventsPublisherSR);
            } 
            //BatchEventsPublisher not configured or unable to obtain it.
      
        } catch (InvalidSyntaxException e) {
           //ffdc
        }
        return retMe;
    }
    
    /**
     * This method is invoked by the messaging provider when there is a suitable
     * message to consume. A suitable message means this message matches the
     * criteria set in the message selector.
     * 
     * Message will be rollback onto the queue by messaging provider if this
     * method encounter any exception
     */
    @Override
    public void onMessage(Message msg) {
        processMessage( new BatchJmsMessage(msg) );
    }

    /**
     * Use the operation type (Start or Restart) to invoke the right handler
     * 
     * JMSException can occur when pulling out data from jms message. This will
     * cause the listener to discard message, and print error message.
     * 
     * Any exceptions related to batch container operation (like unable to get
     * job execution, security,etc.) will cause the listener to mark job
     * instance state to FAILED, print error message. This is done in method
     * handleStartRequest() and handleRestartRequest(). These two methods throws
     * any non-batch container operation exception (all other exeption)
     * 
     * All other exception will be bubble back to onMessage(), to messaging
     * provider, and cause messaging provider to rollback message onto the
     * queue.
     * 
     * @param msg
     */
    private void processMessage(BatchJmsMessage msg) {
       
        try {
            
            //don't remove this trace. there is a fat test looking for this string or
            //if update the string, make sure update the test.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug( BatchJmsEndpointListener.this, tc, 
                         " received message from " + BatchJmsConstants.JBATCH_JMS_LISTENER_CLASS_NAME + " for applicationName: " + msg.getApplicationName() );
            }

            if (msg.getOperation().equalsIgnoreCase(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START)) {
                
                //FOR TESTING ONLY - ACTIVE IF SYSTEM PROPERTY EXIST//
                //throw this exception only once
                //expecting message will be put back on queue
                if (BatchJmsEnvHelper.isTriggerEndpointJmsException() && BatchJmsEnvHelper.getExceptionCount() == 0) {
                    BatchJmsEnvHelper.incrementExceptionCount();
                    throw new JMSException("Throws Jms exception to simulate failure in retreiving data from jms message for instance id " + msg.getInstanceId());             
                } 
                long executionId = NO_EXECUTION_ID;
                if (msg.getMessage().getIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_MINOR_VERSION) > 1) {
                    executionId = msg.getExecutionId();
                }
                handleStartRequest(msg.getInstanceId(), 
                                   executionId,  
                                   msg.getSecurityContext(),
                                   msg.getJobParameters());

            } else if (msg.getOperation().equalsIgnoreCase(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_RESTART)) {
                long instanceId = NO_INSTANCE_ID;
                if (msg.getMessage().getIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_MINOR_VERSION) > 0) {
                    instanceId = msg.getInstanceId();
                }
                handleRestartRequest(instanceId,
                                     msg.getExecutionId(),
                                     msg.getSecurityContext(),
                                     msg.getJobParameters());
                
            }  else if (msg.getOperation().equalsIgnoreCase(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START_PARTITION)) {

                handleStartPartitionRequest( msg );

            } else {
                // Unsupported operation.
                Tr.warning(tc, "warning.unsupported.operation", new Object[] {msg.getOperation(), msg.toString()});
            }

        } catch (JMSException e) {
            // if we get jms error here, don't rethrow exception because this
            // message should not be roll back msg onto the queue.
            // maybe message was corrupted or missing required property.
            Exception ex = e.getLinkedException() != null ? e.getLinkedException() : e;
            Tr.error(tc, "error.endpoint.unable.process.message", new Object[] { msg.toString(), ex });
            
            if ( ! msg.getOperationUnchecked().equalsIgnoreCase(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START_PARTITION)
                 && msg.getInstanceIdUnchecked() >= 0 ) {
               
                try{
                    WSJobRepository jobRepository = getWSJobRepositoryInstance();                
                    jobRepository.updateJobInstanceWithInstanceStateAndBatchStatus(msg.getInstanceIdUnchecked(), InstanceState.FAILED, BatchStatus.FAILED);
                } catch (Exception ue) {
                    //FFDC it
                    //don't re-throw consume it
                }
            }
        }
    }
    
    /**
     * @return a JMS connection, retrieved from BatchJmsDispatcher, which has a ref
     *         to the connectionFactory.
     *         TODO: should the <batchJmsExecutor> config include a ref to the connection factory as well?
     *               it has to be the same connection factory that the BatchJmsDispatcher would use.
     *               Probably doesn't really save us anything... cuz the top-level thread (PartitionedStepController)
     *               needs the BatchJmsDispatcher anyway for startPartition(), so we need BatchJmsDispatcher
     *               in the batch executors anyway.  
     */
    private Connection getJmsConnection() throws JMSException {
        if(connectionFactory == null ){
            throw new IllegalStateException("The replyConnectionFactoryRef reference cannot be located in the <batchJmsExecutor> element.");
        }
        return connectionFactory.createConnection();
    }
    
    /*
     * Process partition message
     * 
     * @param topLevelExecutionId
     * 
     */
    private void handleStartPartitionRequest(BatchJmsMessage batchJmsMessage) {
        Exception savedException = null;
         StartPartitionPayload payload = null;
        PartitionReplyQueueJms partitionReplyQueue = null;
        try {
            
            payload  = batchJmsMessage.getStartPartitionPayload();
            PartitionPlanConfig config = payload.getPartitionPlanConfig();
            long jobExecutionId = config.getTopLevelExecutionId();
            
            byte[] securityContext = payload.getSecurityContext();
            // push the security context on this request because batch operation is guarded with authorization
            WSJobRepository jobRepositoryProxy = createContextualProxy(securityContext, getWSJobRepositoryInstance(), WSJobRepository.class);
            BatchInternalDispatcher dispatcherProxy = createContextualProxy(securityContext, getInternalDispatcherInstance(), BatchInternalDispatcher.class);

            if (!isJobExecutionMostRecent(jobExecutionId)) {
                if(tc.isDebugEnabled()) {
                    Tr.debug(BatchJmsEndpointListener.this, tc, "Exiting since execution Id = " + jobExecutionId + " was not the newest.");
                }
                return;
            }

            RemotablePartitionKey rpKey = new RemotablePartitionKey(jobExecutionId, config.getStepName(), config.getPartitionNumber());
            WSRemotablePartitionState rpState = jobRepositoryProxy.getRemotablePartitionInternalState(rpKey);
            if (rpState == null) {
                if(tc.isDebugEnabled()) {
                    Tr.debug(BatchJmsEndpointListener.this, tc, "Ignore, RP table maybe not created");
                }
            } else if (!rpState.equals(WSRemotablePartitionState.QUEUED)) {
                // It might seem like we should have more locking around the state transition.  However even if we were to let two
                // threads through, the DB constraint would only allow one to create the partition-level STEPTHREADEXECUTION entry.
                // So there would be some noise and possible failure even, but not the double execution of this partition.   So we do
                // this simple check.
                if(tc.isDebugEnabled()) {
                    Tr.debug(BatchJmsEndpointListener.this, tc, "Exiting since WSRemotablePartitionState = " + rpState);
                }
                return;
            }

            // UPDATE: 2019-09-24 - I'm sure this comment below isn't 100% correct or up-to-date, but I'm leaving it
            // because it enumerates a bunch of considerations that we should keep in mind.
            //
            // TODO: what if we can't get a JMS connection?  That means we can't send messages
            //       back to the top-level thread.  Which is waiting infinitely for us.  So we
            //       blow up right away, and rollback the message on the queue?  The switch-block-like
            //       code above does NOT rollback onto the queue.  If we could communicate thru DB
            //       updates would that work?  Just as long as the top-level thread knows to wake up
            //       periodically and check the DB. polling......  It's either that or risk it never
            //       stopping, when it possibly could have if we only checked the DB.
            //       Anyway, on the consumer side, we need to guarantee that we'll make a DB update about 
            //       receiving the partition, or else we rollback the message onto the queue
            //       (which we can't do... or can we?).  So we need to make a DB update here.  What if that fails?
            //       .. then the partition is lost...  There may be occasions where the partition is lost.
            //       How does the top-level thread prevent hang?  When it wakes for polling, it checks the state
            //       of all partitions.  If a partition has not begun anywhere, that could mean it's lost.
            //       If we can comm via the DB, then we can let the partition run, so long as it doesn't
            //       have any collectors.  If collectors, just fail.  If no collectors, run the job,
            //       final status will be sent via DB.
            
            partitionReplyQueue = new PartitionReplyQueueJms( getJmsConnection(), batchJmsMessage.getJmsReplyToQueue() );
            
            Future<?> futureWork = dispatcherProxy.startPartition( config, 
                                            payload.getStep(),
                                            partitionReplyQueue);
            
            //Block until the partition ends.
            waitTillWorkEnds(futureWork);
            
        } catch (Exception e) {
             // exceptions that we want to catch
            if (exceptionWillBeConsumed(e)) {
                savedException = e;
            } else {
                throw new BatchContainerRuntimeException(e.getCause() != null ? e.getCause() : e);
            }
        } finally {
            if ( savedException != null) {
                try{
                    //don't remove this trace. there is a fat test looking for this string or
                    //if update the string, make sure update the test.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug( BatchJmsEndpointListener.this, tc, 
                                 " received exception " + savedException.getClass() + ", sending FAILED status for this partition");
                        
                    }
                    //Send thread complete partition reply
                    partitionReplyQueue.add( new PartitionReplyMsg( PartitionReplyMsgType.PARTITION_FINAL_STATUS)
                                .setBatchStatus(BatchStatus.FAILED )
                                .setExitStatus(BatchStatus.FAILED.toString())
                                .setPartitionPlanConfig( payload.getPartitionPlanConfig() ) );

                    partitionReplyQueue.close();
                }
                catch(Exception e){
                       Tr.error(tc, "error.start.partition.request", new Object[] { e });
                }
            }
        }
        
    }

    /**
     * Process a job restart request.
     * 
     * @param instanceId
     * @param executionId
     * @param securityContext
     * @param jobParameters
     * 
     */
    @FFDCIgnore( JobInstanceNotQueuedException.class )
    private void handleRestartRequest(long jobInstanceId, long executionId, byte[] securityContext, Properties restartParameters) {
    
        WSJobRepository jobRepository = getWSJobRepositoryInstance();
        BatchInternalDispatcher batchDispatcher = getInternalDispatcherInstance();
        String correlationId = this.getCorrelationId(restartParameters);
        long instanceId = jobInstanceId;
        long jobExecutionId = executionId;
        
        Exception savedException = null;
        try {            
            
            WSJobRepository jobRepositoryProxy = createContextualProxy(securityContext, jobRepository, WSJobRepository.class);
            BatchInternalDispatcher dispatcherProxy = createContextualProxy(securityContext, batchDispatcher, BatchInternalDispatcher.class);
            
            // NO_INSTANCE_ID means original version of the restart message was issued.
            if (instanceId == NO_INSTANCE_ID) {
                //get instance id of this execution because we need to update the instance state
                instanceId = jobRepositoryProxy.getJobInstanceFromExecution(executionId).getInstanceId();
                //reset execution id to -1 so that we know this is the old dispatch path and 
                //a new execution has not yet been created. 
                jobExecutionId = NO_EXECUTION_ID;
            }

            if (!isJobExecutionMostRecent(jobExecutionId)) {
                return;
            }

            WSJobInstance jobInstance = null; 

            try {
            	jobInstance = jobRepositoryProxy.updateJobInstanceStateOnConsumed(instanceId);
            } catch (JobInstanceNotQueuedException exc) {
            	// trace and swallow this 
                if(tc.isDebugEnabled()) {
                	// Might be misleading to trace the non-JMS_QUEUED `state we found without locking it down
                    Tr.debug(BatchJmsEndpointListener.this, tc, "Exiting since instanceState isn't equal to JMS_QUEUED");
                }
                return;
            }
            
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_JMS_CONSUMED, correlationId);

            // get the op group names and create a mapping to each for the job instance id
            if (batchOperationGroup != null) {
                int instanceEntityVersion = jobRepositoryProxy.getJobInstanceEntityVersion();
                if (instanceEntityVersion >= 3) {
                    if (jobInstance.getGroupNames() == null || jobInstance.getGroupNames().size() == 0) {
                        if(tc.isDebugEnabled()) {
                            Tr.debug(BatchJmsEndpointListener.this, tc, "On restart, null/empty operation group mapping. Give it another chance.");
                        }
                        jobRepositoryProxy.updateJobInstanceWithGroupNames(instanceId, batchOperationGroup.getGroupNames());
                    } else {
                        if(tc.isDebugEnabled()) {
                            Tr.debug(BatchJmsEndpointListener.this, tc, "On restart, operation group mapping already performed, continuing without reassignment");
                        }
                    }
                } else {
                    if(tc.isDebugEnabled()) {
                        Tr.debug(BatchJmsEndpointListener.this, tc, "Skip group names update because job instance table version = " + instanceEntityVersion); 
                    }
                }
            }

            // invoke operation
            Future<?> futureWork = dispatcherProxy.restartInstance(instanceId, restartParameters, jobExecutionId);
            
            //Block until the job ends.
            waitTillWorkEnds(futureWork);
            
        } catch (Exception e) {
            // exceptions that we want to catch
            if (exceptionWillBeConsumed(e)) {
                savedException = e;
            } else {
                // exception that needs to throw back to caller, so message will
                // be rollback.
                throw new BatchContainerRuntimeException(e.getCause() != null ? e.getCause() : e);
            }
        } finally {
            if ( savedException != null) {
                Tr.error(tc, "error.endpoint.unable.process.restart.request", new Object[] { executionId,
                        savedException.getCause() != null ? savedException.getCause() : savedException });
                
                markInstanceExecutionFailed( instanceId, executionId, correlationId );
            }
        }
    }

    /**
     * Process a job start request
     * 
     * @param instanceId
     * @param securityContext
     * @param jobParameters
     */
    @FFDCIgnore( {JobStoppedOnStartException.class, JobInstanceNotQueuedException.class} )
    private void handleStartRequest(long instanceId, long executionId, byte[] securityContext, Properties jobParameters) {
        Exception savedException = null;
        WSJobRepository jobRepository = getWSJobRepositoryInstance();
        BatchInternalDispatcher batchDispatcher = getInternalDispatcherInstance();
        String correlationId = this.getCorrelationId(jobParameters);
        
        try {
            
            // push the security context on this request because batch operation is guarded with authorization
            WSJobRepository jobRepositoryProxy = createContextualProxy(securityContext, jobRepository, WSJobRepository.class);
            BatchInternalDispatcher dispatcherProxy = createContextualProxy(securityContext, batchDispatcher, BatchInternalDispatcher.class);          
            
            if (!isJobExecutionMostRecent(executionId)) {
                return;
            }
            
            WSJobInstance jobInstance = null; 

            try {
            	jobInstance = jobRepositoryProxy.updateJobInstanceStateOnConsumed(instanceId);
            } catch (JobInstanceNotQueuedException exc) {
            	// trace and swallow this 
                if(tc.isDebugEnabled()) {
                	// Might be misleading to trace the non-JMS_QUEUED `state we found without locking it down
                    Tr.debug(BatchJmsEndpointListener.this, tc, "Exiting since instanceState isn't equal to JMS_QUEUED");
                }
                return;
            }
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_JMS_CONSUMED, correlationId);

            // get the op group names and create a mapping to each for the job instance id
            if (batchOperationGroup != null) {
                int instanceEntityVersion = jobRepositoryProxy.getJobInstanceEntityVersion();
                if (instanceEntityVersion >= 3) {
                    jobRepositoryProxy.updateJobInstanceWithGroupNames(instanceId, batchOperationGroup.getGroupNames());
                } else {
                    if(tc.isDebugEnabled()) {
                        Tr.debug(BatchJmsEndpointListener.this, tc, "Skip group names update because job instance table version = " + instanceEntityVersion); 
                    }
                }
            }

            //FOR TESTING ONLY - ACTIVE IF SYSTEM PROPERTY EXIST//
            //expecting message will be put back on queue
            if (BatchJmsEnvHelper.isTriggerEndpointDbException() && BatchJmsEnvHelper.getExceptionCount() == 0) {
                BatchJmsEnvHelper.incrementExceptionCount();
                throw new PersistenceException(new BatchContainerRuntimeException("Throws persistence exception to simulate failure in db update"));             
            } 
           
            // invoke operation
            Future<?> futureWork = dispatcherProxy.start(jobInstance, jobParameters, executionId);
            
            //Block until the job ends.
            waitTillWorkEnds(futureWork);
          
        } catch (JobStoppedOnStartException js) {
            // Catch this exception so that job does not get put into FAILED state.
            if(tc.isDebugEnabled()) {
                Tr.debug( BatchJmsEndpointListener.this, tc, 
                        "Stop operation was received prior to job being STARTED.");
            }
        } catch (Exception e) {
            // exceptions that we want to catch
            if (exceptionWillBeConsumed(e)) {
                savedException = e;
            } else {
                // exception that needs to throw back to caller, so message will
                // be rollback by the jms provider.
                throw new BatchContainerRuntimeException(e.getCause() != null ? e.getCause() : e);
            }
        } finally {
            if ( savedException != null) {
                Tr.error(tc, "error.endpoint.unable.process.start.request", new Object[] { instanceId,
                        savedException.getCause() != null ? savedException.getCause() : savedException });
                
                markInstanceExecutionFailed( instanceId, executionId, correlationId );
            }
        }
    }
    
    /**
     * If couldn't start instance then mark instance/execution FAILED. 
     * Don't want exceptions to cause a roll back of msg onto the queue.
     * Example: Maybe couldn't locate application contents (JSL).
     */
    private void markInstanceExecutionFailed( long instanceId, long executionId, String correlationId ){

        try {
            getWSJobRepositoryInstance().updateJobInstanceAndExecutionWithInstanceStateAndBatchStatus( instanceId, executionId, InstanceState.FAILED, BatchStatus.FAILED);
            
            //Push to topic for this instance state
            publishEvent(instanceId, BatchEventsPublisher.TOPIC_INSTANCE_FAILED, correlationId);
            publishExecutionEvent( executionId, BatchEventsPublisher.TOPIC_EXECUTION_FAILED, correlationId);
        
        }catch(Throwable t){
            //FFDC it
            //don't re-throw, consume it
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
    
    
    private void waitTillWorkEnds(Future<?> futureWork){
        try {
            futureWork.get();
        } catch (InterruptedException e ) {
            //FFDC it
        } catch ( CancellationException e ){
            //FFDC it
        } catch ( ExecutionException  e ){
            //FFDC it
        }
    }

    /**
     * Publish event
     * @param jobInstance
     * @param topic
     */
    private void publishEvent(WSJobInstance jobInstance, String topic, String correlationId) {
        BatchEventsPublisher publisher = getBatchEventsPublisher();
        
        if (publisher != null) {
            publisher.publishJobInstanceEvent(jobInstance, topic, correlationId);
        }
    }
    
    /**
     * publish event for this instance
     * @param instanceId
     * @param event
     * @param correlationId
     */
    private void publishEvent(long instanceId, String event, String correlationId){
        BatchEventsPublisher publisher = getBatchEventsPublisher();
        WSJobRepository jobRepository = getWSJobRepositoryInstance();
        
        if (publisher != null && jobRepository != null ) {
            WSJobInstance jobInstance = jobRepository.getJobInstance(instanceId);
               publisher.publishJobInstanceEvent(jobInstance, event, correlationId);
        }
    }
    
    
    /**
     * publish event for this execution
     * @param executionId
     * @param event
     * @param correlationId
     */
    private void publishExecutionEvent(long executionId, String event, String correlationId){
        BatchEventsPublisher publisher = getBatchEventsPublisher();
        WSJobRepository jobRepository = getWSJobRepositoryInstance();
        
        if (publisher != null && jobRepository != null ) {
            WSJobExecution jobExecution = jobRepository.getJobExecution(executionId);
               publisher.publishJobExecutionEvent(jobExecution, event, correlationId);
        }
    }

    /**
     * Filter out exception that listener wants to catch.
     * 
     * @param exception
     * @return
     */
    private boolean exceptionWillBeConsumed(Exception exception) {
               
        //Unwrap Persistence exceptions in case exception should be consumed.
        if(exception instanceof PersistenceException){
            exception = unWrapException( exception);
            
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug( BatchJmsEndpointListener.this, tc, " Unwrapped Exception: " + exception.toString());
            }
        }
        
        //191113: added BatchSubmitInvalidParametersException since on submit of an invalid xmlName a never ending loop of JMS messages was being thrown
        if (exception instanceof ClassCastException || exception instanceof ClassNotFoundException || exception instanceof IOException || exception instanceof NoSuchJobExecutionException
                || exception instanceof JobSecurityException || exception instanceof JobStartException
                || exception instanceof BatchContainerAppNotFoundException || exception instanceof JobExecutionNotMostRecentException
                || exception instanceof JobExecutionAlreadyCompleteException || exception instanceof NoSuchJobInstanceException 
                || exception instanceof BatchSubmitInvalidParametersException || exception instanceof BatchIllegalIDPersistedException 
                || exception instanceof BatchIllegalJobStatusTransitionException)  {
            return true;
        }
        return false;
    }
    
    /**
     * Unwrap to find exception that should be handled.
     * 
     * @param exception
     * @return exception
     */
    private Exception unWrapException( Exception e){
        
        return( (e.getCause() != null)? unWrapException((Exception) e.getCause()): e);
        
    }

    /**
     * Deserialize the given serializedContext and create a contextual proxy
     * around the given instance object (typically a Callable or Runnable). When
     * the instance object is invoked it will run with under the given context.
     * 
     * @param serializedContext
     *            from a previous call to captureThreadContext
     * @param instance
     *            to be wrapped with a contextual proxy
     * @param intf
     *            the interface that the contextual proxy will implement
     *            (instance should implement it)
     * 
     * @return contextual proxy
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public <T> T createContextualProxy(byte[] serializedContext, T instance, Class<T> intf) throws IOException, ClassNotFoundException {

        Map<String, String> execProps = new HashMap<String, String>();

        // TaskIdentity identifies the task for the purposes of mgmt/auditing.
        execProps.put(BatchJmsConstants.MANAGEDTASK_IDENTITY_NAME, "batch.job"); // "batch.<jobname>.<instanceid>"

        // TaskOwner identifies the submitter of the task.
        execProps.put(WSContextService.TASK_OWNER, "batch.runtime");

        ThreadContextDescriptor tcDescriptor = ThreadContextDeserializer.deserialize(serializedContext, execProps);

        WSContextService contextService = getWSContextServiceInstance();
        if (contextService == null) {
            throw new RuntimeException("Unable to obtain WSContextService");
        }
        return contextService.createContextualProxy(tcDescriptor, instance, intf);
    }

    /**
     * Creates a filter string that matches an attribute value exactly.
     * Characters in the value with special meaning will be escaped.
     * 
     * @param name
     *            a valid attribute name
     * @param value
     *            the exact attribute value
     */
    public static String createPropertyFilter(String name, String value) {
        assert name.matches("[^=><~()]+");

        StringBuilder builder = new StringBuilder(name.length() + 3 + (value == null ? 0 : value.length() * 2));
        builder.append('(').append(name).append('=');

        int begin = 0;
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                if ("\\*()".indexOf(value.charAt(i)) != -1) {
                    builder.append(value, begin, i).append('\\');
                    begin = i;
                }
            }

            return builder.append(value, begin, value.length()).append(')').toString();
        } else {
            return builder.append(')').toString();
        }
    }


    // Useful since the intermediary of the queue means we could pick up a stale messages on the queue from an earlier start or restart
    // of the TLJ
    private boolean isJobExecutionMostRecent(long jobExecutionId) {
        if (!BatchStatusValidator.isJobExecutionMostRecent(jobExecutionId)) {
            Tr.warning(tc, "warning.endpoint.listener.stale.message", jobExecutionId);

            // Don't remove this trace as there is a FAT test looking for this string.  If removing this info
            // update the FAT case in BatchJmsMultiJVMPartitionsTest.testDiscardMessagesAfterRestart()
            if(tc.isDebugEnabled()) {
                Tr.debug(BatchJmsEndpointListener.this, tc, " discarding message with Top Level Execution Id = " + 
                        jobExecutionId + " since it was not the newest Execution");
            }
            return false;
        } else {
            return true;
        }
    }
}
