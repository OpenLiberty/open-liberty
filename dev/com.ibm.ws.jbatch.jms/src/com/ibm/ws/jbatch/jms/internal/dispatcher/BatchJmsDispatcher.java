/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.jms.internal.dispatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;

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
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.jbatch.jms.internal.BatchJmsDispatcherException;
import com.ibm.ws.jbatch.jms.internal.BatchJmsEnvHelper;
import com.ibm.ws.jbatch.jms.internal.BatchJmsMessage;
import com.ibm.ws.jbatch.jms.internal.listener.BatchJmsEndpointListener;
import com.ibm.ws.jbatch.rest.BatchManager;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializer;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Service implementation of Batch Dispatcher using Jms.
 * 
 * This service is in an auto-provision feature where the bundle will be activated
 * when batchManagement feature and either wasJmsClient or wasJmsServer are
 * configured.
 * 
 * In addition, this service will be activated when its configuration element
 * exists in the server.xml.
 * 
 * Both BatchLocalDispatcher and BatchJmsDispatcher will need to co-exist, and
 * we'll need to be able to DS-inject one or the other, since some consumers of
 * the services will need one or the other. E.g: - BatchManagerImpl will use
 * BatchJmsDispatcher over BatchLocalDispatcher if it's active. -
 * BatchJmsDispatcher will delegate stop() calls for locally running jobs to
 * BatchLocalDispatcher. - The BatchJmsEndpointListener will use
 * BatchLocalDispatcher always, even if BatchJmsDispatcher is also active.
 * (jbatch utility -> rest -> batch manager -> batch jms dispatcher endpoint
 * listener -> batch local dispatcher -> run job)
 * 
 */
@Component(configurationPid = "com.ibm.ws.jbatch.jms.dispatcher", 
           service = BatchDispatcher.class, 
           configurationPolicy = ConfigurationPolicy.REQUIRE, 
           property = { "service.vendor=IBM", "service.ranking:Integer=10", "type=JMS" })
public class BatchJmsDispatcher implements BatchDispatcher {

    private static final TraceComponent tc = Tr.register(BatchJmsDispatcher.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");


    /**
     * The batch persistence layer.
     */
    private WSJobRepository jobRepository;
    
    /**
     * The collection of contexts to capture under captureThreadContext.
     * Currently only security context is captured.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] CapturedContexts = new Map[] { Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.security.context.provider"), };

    /**
     * For capturing thread context.
     */
    private WSContextService contextService;

    /**
     * Local batch dispatcher to delegate task.
     */
    private BatchDispatcher localBatchDispatcher = null;

    /**
     * For starting transaction in start and restart
     */
    private EmbeddableWebSphereTransactionManager tranMgr;

    /**
     * Service that controls local transactions.
     */
    private LocalTransactionCurrent localTranCurrent;

    /**
     * For creating J2EENames.
     */
    private J2EENameFactory j2eeNameFactory;

    /**
     * For creating jms dispatcher queue
     */
    private ResourceFactory jmsQueueFactory;

    /**
     * For creating jms dispatcher connnection factory
     */
    private ResourceFactory jmsConnectionFactory;

    /**
     * Resource configuration factory used to create a resource info object.
     */
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * For publishing job event
     */
    private BatchEventsPublisher eventsPublisher; 
    
    @Reference(service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) {
        resourceConfigFactory = svc;
    }

    @Reference(target = "(id=unbound)")
    protected void setJMSQueueFactory(ResourceFactory factory, Map<String, String> serviceProps) {
        jmsQueueFactory = factory;
    }

    @Reference(target = "(id=unbound)")
    protected void setJMSConnectionFactory(ResourceFactory factory, Map<String, String> serviceProps) {
        jmsConnectionFactory = factory;
    }

    protected void unsetJmsQueueFactory(ResourceFactory svc) {
        if (svc == jmsQueueFactory) {
            jmsQueueFactory = null;
        }
    }

    protected void unsetJmsConnectionFactory(ResourceFactory svc) {
        if (svc == jmsConnectionFactory) {
            jmsConnectionFactory = null;
        }
    }

    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
        if (svc == resourceConfigFactory) {
            resourceConfigFactory = null;
        }
    }

    /**
     * DS injection
     */
    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }

    /**
     * DS injection
     */
    @Reference
    protected void setLocalTransactionCurrent(LocalTransactionCurrent ltc) {
        localTranCurrent = ltc;
    }
    /**
     * DS injection.
     */
    @Reference(target = "(service.pid=com.ibm.ws.context.manager)")
    protected void setContextService(WSContextService contextService) {
        this.contextService = contextService;
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
    @Reference(target = "(type=Local)")
    protected void setLocalBatchDispatcher(BatchDispatcher ref) {
        this.localBatchDispatcher = ref;
    }
     
    /**
     * DS injection
     */
    @Reference
    protected void setTransactionManager(EmbeddableWebSphereTransactionManager svc) {
        tranMgr = svc;
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

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) throws Exception {
        //
        // Do nothing.   Need to lazy init because we have "optional-but-sometimes-required" reference
        // downstream to a RRS-related resource manager type of class.
        // 
        // If we are in BINDINGS-mode, the RRS thing is required, and we should wait for it to get set up
        // before we initialize.   However, we can't make a required dependency since if we are on Distributed or
        // if zosTransaction isn't enable, this dependency will remain unsatisified.
        //
    }
    
    // Use this to blow up if we are being called on the way down (since our clients are not all DS instances)
    private boolean deactivated=false;
    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> config) throws Exception {
        deactivated = true;
    }
    
    private ConnectionFactory getConnectionFactory() {
        if (deactivated) {
            throw new IllegalStateException("BatchJmsDispatchImpl component instance = " + this.toString() +", has been deactivated");
        }
        return getInitHelper().jmsCf;
    }

    private Queue getQueue() {
        if (deactivated) {
            throw new IllegalStateException("BatchJmsDispatchImpl component instance = " + this.toString() +", has been deactivated");
        }
        return getInitHelper().jmsQ;
    }
    
    private byte[] initHelperLock = new byte[0];

    // double-checked locking
    // http://www.oracle.com/technetwork/articles/javase/bloch-effective-08-qa-140880.html
    private volatile InitHelper initHelper;
    InitHelper getInitHelper() {
        InitHelper result = initHelper;
        if (result == null) { // First check (no locking)
            synchronized(initHelperLock) {
                result = initHelper;
                if (result == null) { // Second check (with locking)
                    initHelper = result =  new InitHelper();
                }
            }
        }
         return result;
    }

    private class InitHelper {
        
        private InitHelper() {
            initJMSResources();
        }

        private void initJMSResources() {
            try {
                ResourceConfig resourceConfig = resourceConfigFactory.createResourceConfig(Queue.class.getName());
                jmsQ = (Queue) jmsQueueFactory.createResource(resourceConfig);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "        jmsQ = " + jmsQ.getQueueName() + jmsQ.toString());
                }

                ResourceConfig cfResourceConfig = resourceConfigFactory.createResourceConfig(ConnectionFactory.class.getName());
                cfResourceConfig.setResAuthType(ResourceInfo.AUTH_CONTAINER);
                jmsCf = (ConnectionFactory) jmsConnectionFactory.createResource(cfResourceConfig);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "        jmsCf = " + jmsCf.toString());
                }
            } catch (Exception e) {
                Tr.error(tc, "error.batch.dispatcher.jms.resource.activate", new Object[] { e });
                throw new RuntimeException(e);
            }
        }

        /**
         * Connection factory for dispatch queue
         */
        private ConnectionFactory jmsCf = null;

        /**
         * Dispatch queue
         */
        private Queue jmsQ = null;

    }


    /**
     * Encapsulate job instance data and job parameters data and security
     * context into jms message, and put it onto jms dispatcher queue. Return
     * BatchManagerImpl.REMOTE_DISPATCH_NO_EXECUTION_ID to caller. The
     * BatchDispatcher interface is expecting an execution id, however, we don't
     * have it yet at this stage.
     */
    @Override
    public void start(final WSJobInstance jobInstance, Properties jobParameters, long executionId) throws BatchJmsDispatcherException {

        Exception savedException = null;
        long instanceId = jobInstance.getInstanceId();
        String correlationId = this.getCorrelationId(jobParameters);

        // Serialize security context
        // this call does not need to be in the transaction.
        byte[] securityContext = captureThreadContext(instanceId, BatchManager.REMOTE_DISPATCH_NO_EXECUTION_ID);

        TransactionHelper tranHelper = new TransactionHelper();
        tranHelper.preInvoke(instanceId);
        try {

            // Though it might seem backwards to change the DB status to queued BEFORE putting the message to the queue
            // (rather than after), this way we can leverage the status transition validation within the update method.
            // Plus, remember this is all part of a single global transaction, so it's not as if we can ever persist the
            // update to JMS_QUEUED without sending the JMS message successfully.
            WSJobInstance updatedJobInstance = jobRepository.updateJobInstanceStateOnQueued(instanceId);
            if (updatedJobInstance.getInstanceState() != InstanceState.JMS_QUEUED) {
                if(tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exiting since instanceState isn't equal to JMS_QUEUED, instead is: " + updatedJobInstance.getInstanceState());
                }
                return;
            }
            
            sendStartMessage(jobInstance, jobParameters, securityContext, executionId);
            
            // FOR TESTING ONLY - ACTIVE IF SYSTEM PROPERTY EXIST//
            if (BatchJmsEnvHelper.isTriggerDispatcherQueueException()) {
                throw new JMSException("Throws jms exception to simulate failure in queue jms message");             
            } 
            // FOR TESTING ONLY - ACTIVE IF SYSTEM PROPERTY EXIST//
            if (BatchJmsEnvHelper.isTriggerDispatcherDbException()) {
                throw new PersistenceException(new BatchContainerRuntimeException("Throws persistence exception to simulate failure in db update"));
            }
        } catch (JMSException jmsException) {
            savedException = jmsException.getLinkedException() != null ? jmsException.getLinkedException() : jmsException;
            tranHelper.setTranRollback();
        } catch (Exception allOtherException) {
            savedException = allOtherException;
            tranHelper.setTranRollback();
        } finally {
            tranHelper.postInvoke(instanceId, BatchManager.REMOTE_DISPATCH_NO_EXECUTION_ID);
            // no error from rollback and commit, but we have an exception,
            // so it must be error that caused the rollback. rethrow it.
            if (savedException != null) {
                
                markInstanceExecutionFailed( instanceId, executionId, correlationId );
                
                throw new BatchJmsDispatcherException(savedException, instanceId, BatchManager.REMOTE_DISPATCH_NO_EXECUTION_ID);
            }
        }

        // We want to publish outside of the dispatch tran, since we don't want a publish failure to roll back the dispatch.  
        publishEvent(instanceId, BatchEventsPublisher.TOPIC_INSTANCE_JMS_QUEUED, correlationId);

        return;
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
    
    /**
     * If couldn't start instance then mark instance/execution FAILED. 
     * Example: Maybe couldn't locate application contents (JSL).
     */
    @Override
    public void markInstanceExecutionFailed( long instanceId, long executionId, String correlationId ){

        jobRepository.updateJobInstanceAndExecutionWithInstanceStateAndBatchStatus( instanceId, executionId, InstanceState.FAILED, BatchStatus.FAILED);
        
        //Push to topic for this instance state
        publishEvent(instanceId, BatchEventsPublisher.TOPIC_INSTANCE_FAILED, correlationId);
        publishExecutionEvent( executionId, BatchEventsPublisher.TOPIC_EXECUTION_FAILED, correlationId);
    
    } 

    /**
     * Return new execution id which has already been created.
     */
    @Override    
    public void restartInstance(long instanceId, Properties restartParameters, long executionId) throws BatchJmsDispatcherException {
        Exception savedException = null;
        String correlationId = this.getCorrelationId(restartParameters);
        
        J2EEName j2eeName = createJ2EEName(jobRepository.getJobInstance(instanceId).getAmcName());
               
        byte[] securityContext = captureThreadContext(instanceId, executionId);

        TransactionHelper tranHelper = new TransactionHelper();
        tranHelper.preInvoke(instanceId);

        try {
            // Though it might seem backwards to change the DB status to queued BEFORE putting the message to the queue
            // (rather than after), this way we can leverage the status transition validation within the update method.
            // Plus, remember this is all part of a single global transaction, so it's not as if we can ever persist the
            // update to JMS_QUEUED without sending the JMS message successfully.
            WSJobInstance updatedJobInstance = jobRepository.updateJobInstanceStateOnQueued(instanceId);
            if (updatedJobInstance.getInstanceState() != InstanceState.JMS_QUEUED) {
                if(tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exiting since instanceState isn't equal to JMS_QUEUED, instead is: " + updatedJobInstance.getInstanceState());
                }
                return;
            }
            
            sendRestartMessage(instanceId, executionId, j2eeName, restartParameters, securityContext);

        } catch (JMSException e) {
            // extract the linked exception from JMSException
            savedException = e.getLinkedException() != null ? e.getLinkedException() : e;
            tranHelper.setTranRollback();
        } catch (Exception allOtherException) {
            savedException = allOtherException;
            tranHelper.setTranRollback();
        } finally {
            tranHelper.postInvoke(instanceId, executionId);
            // no error from rollback and commit, but we have an exception,
            // so it must be the error that caused the rollback. rethrow it.
            if (savedException != null) {
                //jobRepository.updateJobInstanceWithInstanceStateAndBatchStatus(instanceId, InstanceState.FAILED, BatchStatus.FAILED);
                markInstanceExecutionFailed( instanceId, executionId, correlationId );
                throw new BatchJmsDispatcherException(savedException, instanceId, executionId);
            }
        }

        // We want to publish outside of the dispatch tran, since we don't want a publish failure to roll back the dispatch.  
        publishEvent(instanceId, BatchEventsPublisher.TOPIC_INSTANCE_JMS_QUEUED, correlationId);
        return;
    }

    /**
     * publish event for this instance
     * @param instanceId
     * @param event
     */
    //private void publishEvent(long instanceId, String event ){
    //    publishEvent(instanceId, event, null);
    //}
    
    /**
     * publish event for this instance
     * @param instanceId
     * @param event
     * @param correlationId
     */
    private void publishEvent(long instanceId, String event, String correlationId ){
        if (eventsPublisher != null) {
            WSJobInstance updatedInstance = jobRepository.getJobInstance(instanceId);
            if(updatedInstance != null){
                eventsPublisher.publishJobInstanceEvent(updatedInstance, event, correlationId);
            }
        }
    }
    /**
     * publish event for this execution
     * @param executionId
     * @param event
     * @param correlationId
     */
    private void publishExecutionEvent(long executionId, String event, String correlationId){
        if (eventsPublisher != null) {
            WSJobExecution updatedExecution = jobRepository.getJobExecution(executionId);
            if(updatedExecution != null){
                eventsPublisher.publishJobExecutionEvent(updatedExecution, event, correlationId);
            }
        }
    }

    /**
     * Forwards the request directly to the BatchLocalDispatcher.
     * If the jobexecution is not local (i.e did not run in this server), 
     * then an exception will be raised.
     * @throws BatchJobNotLocalException 
     * @throws BatchDispatcherException 
     */
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {
        localBatchDispatcher.stop(executionId);
    }

    /**
     * Set up jms objects, create jms message, send, and clean up jms objects
     * for start request
     * 
     */
    private void sendStartMessage(WSJobInstance jobInstance, 
                                  Properties jobParameters, 
                                  byte[] securityContext,
                                  long executionId) throws JMSException {

        Connection jmsConnection = null;
        try {

            // Connection, Session and MesageProducer should not be cached.
            jmsConnection = getConnectionFactory().createConnection();
            Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = jmsSession.createProducer(getQueue());

            setMessageProperties(jobInstance.getInstanceId(),jobParameters,false,messageProducer);
            
            BatchJmsMessage jmsMsg = new BatchJmsMessage( jmsSession.createMapMessage() )
                                                    .setVersion()
                                                    .setJ2eeName( createJ2EEName(jobInstance.getAmcName()) )
                                                    .setInstanceId( jobInstance.getInstanceId() )
                                                    .setExecutionId( executionId) 
                                                    .setOperation( BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START )
                                                    .setJobParameters( jobParameters )
                                                    .setSecurityContext( securityContext )
                                                    .setBatchWorkType(BatchJmsConstants.PROPERTY_VALUE_WORK_TYPE_JOB);

            messageProducer.send(jmsMsg.getMessage());
            
        } finally {
            closeJmsConnection(jmsConnection);
        }
    }

    /**
     * Set message properties based on input job parameters
     */
    private void setMessageProperties(long instanceId,
                                      Properties jobParameters, 
                                      Boolean isPartition,
                                      MessageProducer messageProducer) throws JMSException,BatchJmsDispatcherException {
        
        String priorityS=jobParameters.getProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_PRIORITY);
        
        if (priorityS!=null) {
            try {
                int priority = Integer.parseInt(priorityS);
                //valid priority values are 0-9?  Could validate, but let JMS do it
                messageProducer.setPriority(priority);
                } catch(NumberFormatException nfe) {
                     throw new BatchJmsDispatcherException(nfe, instanceId, BatchManager.REMOTE_DISPATCH_NO_EXECUTION_ID);
            }            
        }

        // Delivery delay applies to job starts/restarts, not partitions
        if (!isPartition) {
            String delayS=jobParameters.getProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_DELIVERYDELAY);
            
            if (delayS!=null) {
                try {
                    long delay = Long.parseLong(delayS);
                    // there's a max, but let JMS enforce it
                    messageProducer.setDeliveryDelay(delay);
                    } catch(NumberFormatException nfe) {
                        throw new BatchJmsDispatcherException(nfe, instanceId, BatchManager.REMOTE_DISPATCH_NO_EXECUTION_ID);
                }                
             }
            
        }
    }
    
    /**
     * Close jms session. According to api doc, there is no need to close the
     * sessions, producers, and consumers of a closed connection.
     * 
     * @param jmsSession
     */
    private void closeJmsConnection(Connection jmsConnection) {
        try {
            if (jmsConnection != null) {
                jmsConnection.close();
            }
        } catch (JMSException e) {
            // don't care if there is error when closing session
        }
    }

    /**
     * Set up jms objects, create jms message, send, and clean up jms objects
     * for restart request
     *
     */
    private void sendRestartMessage(long instanceId,
                                    long executionId, 
                                    J2EEName j2eeName,       
                                    Properties restartParameters, 
                                    byte[] securityContext) throws JMSException {

        Connection jmsConnection = null;
        try {

            // Connection, Session and MesageProducer should not be cached.
            jmsConnection = getConnectionFactory().createConnection();
            Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = jmsSession.createProducer(getQueue());

            setMessageProperties(instanceId,restartParameters,false,messageProducer);
            
            BatchJmsMessage jmsMsg = new BatchJmsMessage( jmsSession.createMapMessage() )
                                            .setVersion()
                                            .setJ2eeName( j2eeName )                                            
                                            .setOperation(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_RESTART)
                                            .setInstanceId( instanceId )
                                            .setExecutionId( executionId) 
                                            .setJobParameters( restartParameters )
                                            .setSecurityContext( securityContext )
                                            .setBatchWorkType(BatchJmsConstants.PROPERTY_VALUE_WORK_TYPE_JOB);

            messageProducer.send(jmsMsg.getMessage());   
            
        } finally {
            closeJmsConnection(jmsConnection);
        }
    }

    /**
     * 
     * This method is called by the top-level thread (PartitionedStepControllerImpl) to create
     * a JMS version of the PartitionReplyQueue.  
     * 
     * On the other side, the sub-job partition thread creates its own instance of the JmsPartitionReplyQueue,
     * using the JMSReplyToQueue from the JMS message.  Messages sent by the sub-job partition thread
     * are queued to the JMSReplyToQueue (in JmsPartitionReplyQueue.add) and received by the top-level 
     * thread under PartitionReplyQueueJms.take().
     * 
     * The JMS specifics/objects are encapsulated and hidden behind the PartitionReplyQueueJms object.
     * This simplifies things for the top-level and partitioned threads, since they don't really need 
     * to know how the comm is handled between them.  Plus it saves the jbatch.container from 
     * having to establish a static dependency on JMS packages.
     * 
     * @return a PartitionReplyQueueJms, which can be used by the top-level thread to communicate
     *         with sub-job partitions over JMS.
     */
    @Override
    public PartitionReplyQueue createPartitionReplyQueue() throws BatchJmsDispatcherException {
        
        try {
            return new PartitionReplyQueueJms( getConnectionFactory().createConnection() );
        } catch (JMSException je) {
            throw new BatchJmsDispatcherException("Could not create JmsPartitionReplyQueue.", je);
        }
    }
    
    /**
     * Called by BatchJmsEndpointListener, on receipt of a "startPartition" message.  A JMS
     * connection is needed by the sub-job partition thread in order to send JMS messages back
     * to the top-level thread via the JMSReplyToQueue.
     */
    public Connection getJmsConnection() throws JMSException {
        return getConnectionFactory().createConnection();
    }
    
    /**
     * Send a "startPartition" JMS message to the queue.  It'll get picked up by a
     * batch executor/endpoint, who then creates and runs the partition.
     */
    @Override
    public void startPartition(PartitionPlanConfig partitionPlanConfig, 
                               Step step,
                               PartitionReplyQueue partitionReplyQueue ) throws BatchJmsDispatcherException {
        Exception savedException = null;

        byte[] securityContext = captureThreadContext(partitionPlanConfig.getTopLevelInstanceId(), 
                                                      partitionPlanConfig.getTopLevelExecutionId() );
        
        J2EEName j2eeName = createJ2EEName(jobRepository.getBatchAppNameFromExecution(partitionPlanConfig.getTopLevelExecutionId()));

        RemotablePartitionKey partitionKey = new RemotablePartitionKey(partitionPlanConfig.getTopLevelExecutionId(), partitionPlanConfig.getStepName(), partitionPlanConfig.getPartitionNumber());
        TransactionHelper tranHelper = new TransactionHelper();
        
        tranHelper.partitionPreInvoke(partitionKey);
        
        try {
            sendStartPartitionMessage(partitionPlanConfig,
                                      step,
                                      j2eeName,
                                      securityContext,
                                      (PartitionReplyQueueJms)partitionReplyQueue);
            
            jobRepository.createRemotablePartition(partitionKey);
            
            // TODO: publish partition event?

        //} catch (Exception je) {
        //    throw new BatchJmsDispatcherException(je, partitionPlanConfig.getTopLevelInstanceId(), partitionPlanConfig.getTopLevelExecutionId());
        } catch (JMSException jmsException) {
            savedException = jmsException.getLinkedException() != null ? jmsException.getLinkedException() : jmsException;
            tranHelper.setTranRollback();
        } catch (Exception allOtherException) {
            savedException = allOtherException;
            tranHelper.setTranRollback();
        } finally {
            tranHelper.partitionPostInvoke(partitionKey);
            // no error from rollback and commit, but we have an exception,
            // so it must be error that caused the rollback. rethrow it.
            if (savedException != null) {
        		String errorMessage = "Unable to Dispatch the partition " + partitionKey.toString();
                throw new BatchJmsDispatcherException(errorMessage);
            } 

        } 

    }
    
    /**
     * Open a JMS connection and send a 'start partition' message.
     */
    private void sendStartPartitionMessage(PartitionPlanConfig partitionPlanConfig,
                                           Step step,
                                           J2EEName j2eeName,
                                           byte[] securityContext,
                                           PartitionReplyQueueJms partitionReplyQueue) throws JMSException, IOException {
        Connection jmsConnection = null;
        try {

            // Connection, Session and MesageProducer should not be cached.
            jmsConnection = getConnectionFactory().createConnection();
            Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = jmsSession.createProducer(getQueue());
            
            Properties jobParameters =
                    jobRepository.getJobExecution(partitionPlanConfig.getTopLevelExecutionId()).getJobParameters();
            
            // Shouldn't get parse error exceptions from this because the priority should already be valid or 
            // we wouldn't have gotten past the job submit with the value
            setMessageProperties(partitionPlanConfig.getTopLevelInstanceId(),jobParameters,true,messageProducer);
            
            BatchJmsMessage jmsMsg = new BatchJmsMessage( jmsSession.createObjectMessage() )
                                                .setVersion()
                                                .setJ2eeName( j2eeName )
                                                .setBatchWorkType(BatchJmsConstants.PROPERTY_VALUE_WORK_TYPE_PARTITION    )
                                                .setOperation(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START_PARTITION)
                                                .setStepName(partitionPlanConfig.getStepName())
                                                .setJmsReplyToQueue( partitionReplyQueue.getJmsReplyToQueue() )
                                                .setExecutionId( partitionPlanConfig.getTopLevelExecutionId() ) 
                                                .setPartitionNumber( partitionPlanConfig.getPartitionNumber() )
                                                .setJobParameters(jobParameters)
                                                .setStartPartitionPayload( new StartPartitionPayload( partitionPlanConfig,
                                                                                                      step,
                                                                                                      securityContext ) );
            messageProducer.send(jmsMsg.getMessage());
            
        } finally {
            closeJmsConnection(jmsConnection);
        }
       
    }

    /**
     * 
     * Serialize the current thread context, for propagating over JMS to the
     * endpoint. At the endpoint, the thread context is deserialized and applied
     * so the job thread runs under the same context.
     * 
     * Currently only security context is captured.
     * 
     * @param executionId
     * @param instanceId
     * 
     * @return serialized thread context
     * 
     * @throws BatchContainerRuntimeException
     */
    public byte[] captureThreadContext(long instanceId, long executionId) {
        // Note: execProps are NOT serialized... so... skip 'em
        try {
            return contextService.captureThreadContext(new HashMap<String, String>(), CapturedContexts).serialize();
        } catch (IOException ioe) {
            throw new BatchJmsDispatcherException(ioe, instanceId, executionId);
        }
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
        // ?

        // TaskOwner identifies the submitter of the task.
        execProps.put(WSContextService.TASK_OWNER, "batch.runtime");

        ThreadContextDescriptor tcDescriptor = ThreadContextDeserializer.deserialize(serializedContext, execProps);

        return contextService.createContextualProxy(tcDescriptor, instance, intf);
    }


    /**
     * @return a J2EEName for the given app / module / comp.
     */
    protected J2EEName createJ2EEName(String j2eeName) {
        return j2eeNameFactory.create( j2eeName.getBytes(StandardCharsets.UTF_8) );
    }


    class TransactionHelper {

        private LocalTransactionCurrent ltcCurrent;
        private LocalTransactionCoordinator suspendedLTC;

        /**
         * Suspend LTC and start global transaction
         * 
         * @param instanceId
         */
        private void preInvoke(long instanceId) {
            try {
                suspendExistingLTC();
                startTransaction();
            } catch (Exception txException) {
                throw new BatchJmsDispatcherException(txException, instanceId);
            }
        }
        
        /**
         * Suspend LTC and start global transaction
         * 
         * @param rpk
         */
        private void partitionPreInvoke(RemotablePartitionKey rpk) {
        	try {
        		suspendExistingLTC();
        		startTransaction();
        	} catch (Exception txException) {
        		String errorMessage = "Unable to Dispatch the partition " + rpk.toString();
        		throw new BatchJmsDispatcherException(errorMessage, txException);
        	}
        }
        
        /**
         * Tidy up transaction
         * 
         * @param rpk
         */
        private void partitionPostInvoke(RemotablePartitionKey rpk) {
            try {
                rollbackOrCommitTransactionAsRequired();
            } catch (Exception txException) {
        		String errorMessage = "Unable to Dispatch the partition " + rpk.toString();
                throw new BatchJmsDispatcherException(errorMessage, txException);
            } finally {
                resumeExistingLTC();
            }
        }
        
        /**
         * Tidy up transaction
         * 
         * @param instanceId
         * @param executionId
         */
        private void postInvoke(long instanceId, long executionId) {
            try {
                rollbackOrCommitTransactionAsRequired();
            } catch (Exception txException) {
                throw new BatchJmsDispatcherException(txException, instanceId, executionId);
            } finally {
                resumeExistingLTC();
            }
        }


        /**
         * Suspend existing LTC
         * 
         * @throws SystemException
         */
        private void suspendExistingLTC() throws SystemException {
            int tranStatus = tranMgr.getStatus();
            ltcCurrent = tranStatus == Status.STATUS_NO_TRANSACTION ? localTranCurrent : null;
            suspendedLTC = ltcCurrent == null ? null : ltcCurrent.suspend();

        }

        /**
         * Resume suspended LTC
         */
        private void resumeExistingLTC() {
            if (suspendedLTC != null)
                ltcCurrent.resume(suspendedLTC);
        }

        /**
         * Start a global transaction
         * 
         * @param instanceId
         */
        private void startTransaction() throws SystemException, NotSupportedException {
            tranMgr.begin();
        }

        /**
         * Rollback or commit the global transaction
         * 
         * @param instanceId
         * @throws HeuristicRollbackException
         * @throws HeuristicMixedException
         * @throws RollbackException
         * @throws SystemException
         * @throws SecurityException
         * @throws IllegalStateException
         */
        private void rollbackOrCommitTransactionAsRequired() throws IllegalStateException, SecurityException, SystemException, RollbackException, HeuristicMixedException,
                HeuristicRollbackException {

            if (tranMgr.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                tranMgr.rollback();
            } else {
                tranMgr.commit();
            }
        }

        /**
         * Mark the transaction to roll back
         */
        private void setTranRollback() {
            try {
                tranMgr.setRollbackOnly();
            } catch (Exception setRbException) {
                // ffdc
            }
        }
    }

}
