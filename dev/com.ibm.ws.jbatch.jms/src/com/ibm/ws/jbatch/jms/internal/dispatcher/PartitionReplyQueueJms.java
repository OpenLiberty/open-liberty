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

import javax.batch.operations.BatchRuntimeException;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.PartitionReplyTimeoutConstants;
import com.ibm.ws.jbatch.jms.internal.BatchJmsMessage;

/**
 * The PartitionReplyQueue for multi-jvm partition threads that communicate
 * with the top-level thread (in a possibly different server) over JMS.
 * 
 */
public class PartitionReplyQueueJms implements PartitionReplyQueue {
    
    private Session jmsSession;
    private Connection jmsConn;
    private Destination jmsReplyToQueue;
    
    /**
     * CTOR. 
     * 
     * This one is used by the top-level thread (PartitionedStepControllerImpl, via BatchJmsDispatcher).
     * It creates the JMS reply-to queue up front.  The JMS reply-to queue will be passed to the
     * partitions (via the JMS Message).  The partitions use the reply-to queue to send data back
     * to the top-level thread.
     * 
     * Note: the connection must stay open for the life of the job, since the temporary reply-to
     * queue is scoped to the life of the connection.
     */
    public PartitionReplyQueueJms(Connection jmsConn) {
        this.jmsConn = jmsConn;
        
        try {
            this.jmsSession = this.jmsConn.createSession(false, Session.AUTO_ACKNOWLEDGE);   // TODO: correct settings?
            this.jmsReplyToQueue = this.jmsSession.createTemporaryQueue();
            jmsConn.start();
            
        } catch (JMSException je) {
            //TODO: include jobInstanceId/jobExecutionId in msg
            throw new BatchRuntimeException("Top-level thread for partitioned step could not open JMS session or create temporary reply-to queue", je);
        }
    }
        
    /**
     * CTOR. This one is used by the partitioned threads.
     * 
     * @param jmsReplyToQueue - the replyTo queue for communicating back to the top-level thread
     */
    public PartitionReplyQueueJms(Connection jmsConn, Destination jmsReplyToQueue) {
        this.jmsConn = jmsConn;
        this.jmsReplyToQueue = jmsReplyToQueue;
        
        try {
            this.jmsSession = this.jmsConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
        } catch (JMSException je) {
            // TODO: include jobInstanceId/jobExecutionId in msg
            throw new BatchRuntimeException("Sub-job partition thread could not open JMS session", je);
        }
    }

    /**
     * @return the JMS session.
     */
    public Session getSession() {
        return jmsSession;
    }

    /**
     * @return the JMS reply-to queue
     */
    public Destination getJmsReplyToQueue() {
        return jmsReplyToQueue;
    }
    
    /**
     * This method is called only by sub-job partition threads, to send data 
     * back to the top-level thread.
     * 
     * Send the message on the JMS reply-to queue.
     */
    @Override
    public boolean add(PartitionReplyMsg partitionReplyMsg) {
        
        try {
            MessageProducer messageProducer = getSession().createProducer(getJmsReplyToQueue());

            BatchJmsMessage jmsMsg = new BatchJmsMessage( getSession().createObjectMessage() )
                                                .setVersion()
                                                // TODO? .setOperation(BatchJmsConstants.PROPERTY_VALUE_JOB_OPERATION_START_PARTITION)
                                                // TODO? .setExecutionId( partitionPlanConfig.getTopLevelExecutionId() ) 
                                                // TODO? .setPartitionNumber( partitionPlanConfig.getPartitionNumber() )
                                                .setPartitionReplyMsgPayload( partitionReplyMsg );
  
            messageProducer.send(jmsMsg.getMessage());
            return true;
            
        } catch (Exception je) {
            throw new BatchRuntimeException("Partition thread could not send data back to top-level thread over JMS reply-to queue", je);
        }
    }
    
    /**
     * This method is called only by the top-level thread, to receive data sent back 
     * from the partition threads.
     * 
     * TODO: timeout?
     */
    @Override
    public PartitionReplyMsg take() {

        try {
            MessageConsumer jmsConsumer = jmsSession.createConsumer(getJmsReplyToQueue());
            ObjectMessage response = (ObjectMessage) jmsConsumer.receive(PartitionReplyTimeoutConstants.BATCH_REPLY_MSG_WAIT_TIMEOUT);
            return response == null ? null : new BatchJmsMessage( response ).getPartitionReplyMsgPayload();
        
        } catch (Exception je) {
            // TODO: include jobexecid in msg
            throw new BatchRuntimeException("Top-level thread for partitioned step could not receive msg from sub-job partitioned thread over JMS reply-to queue", je);
        }
    }
    
    /**
     * This method is called only by the top-level thread, to receive data sent back 
     * from the partition threads.
     * 
     * @returns PartitionReplyMsg if its there or null 
     */
	@Override
	public PartitionReplyMsg takeWithoutWaiting() {
		 try {
	            MessageConsumer jmsConsumer = jmsSession.createConsumer(getJmsReplyToQueue());
	            ObjectMessage response = (ObjectMessage) jmsConsumer.receiveNoWait();
	            return response == null ? null : new BatchJmsMessage( response ).getPartitionReplyMsgPayload();
	        
	        } catch (Exception je) {
	            // TODO: include jobexecid in msg
	            throw new BatchRuntimeException("Top-level thread for partitioned step could not receive msg from sub-job partitioned thread over JMS reply-to queue", je);
	        }
	}
    
    
    /**
     * Called by the top-level thread at end-of-job.
     * Close the JMS connection (which also shuts down the reply-to queue).
     */
    @Override
    public void close() {
        try {
            this.jmsConn.close();
        } catch (JMSException je) {
            // just FFDC it.  No need to throw since the job is stopping anyway.
        }
    }
    
    /**
     * 
     */
    @Override
    public String toString() {
        return "PartitionReplyQueueJms:jmsConn=" + jmsConn + ",jmsReplyToQueue:" + jmsReplyToQueue;
    }
}