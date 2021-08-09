/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.messaging.mbean;

import javax.management.MXBean;

import com.ibm.ws.sib.admin.mxbean.QueuedMessage;
import com.ibm.ws.sib.admin.mxbean.QueuedMessageDetail;

/**
 * <p>
 * The QueueMBean is enabled by the wasJmsServer feature.
 * A QueueMBean is initialized for each Queue defined in the Messaging Engine configuration.
 * Use the MBean programming interface to query runtime information about a Queue.
 * <br><br>
 * JMX clients should use the ObjectName of this MBean to query it
 * <br>
 * Partial Object Name: WebSphere:feature=wasJmsServer, type=Queue,name=* <br>
 * where name is unique for each queue and is equal to the name of the queue defined in messaging engine configuration.
 * </p>
 * 
 * @ibm-api
 */
@MXBean
public interface QueueMBean {

    /**
     * The ID of the Queue represented by this Messaging Engine.
     * 
     * @return ID of the Queue
     */
    public String getId();

    /**
     * The identifier (name) attribute of the Queue represented by this Messaging Engine.
     * by this instance.
     * 
     * @return Name of the Queue
     */
    public String getIdentifier();

    /**
     * Getter for the state attribute for the queue.
     * 
     * @return State of the Queue
     */
    public String getState();

    /**
     * Getter for the depth attribute for the queue.
     * 
     * @return The number of messages queued to the Queue
     */
    public long getDepth();

    /**
     * Getter for the MaxQueueDepth attribute for the queue.
     * 
     * @return The maximum number of messages permitted at the Queue
     */
    public long getMaxQueueDepth();

    /**
     * Checks if sendAllowed attribute is set for the Queue
     * 
     * @return true if send is allowed on the Queue else false
     */
    public boolean isSendAllowed();

    // OPERATIONS

    /**
     * Lists an array of message objects representing the messages on the Queue.
     * 
     * @return Array of messages queued in the Queue
     */
    public QueuedMessage[] listQueuedMessages() throws Exception;

    /**
     * Get an object representing one specific message on the Queue
     * 
     * @param messageId the ID of the message
     * @return Message representing the message id
     */
    public QueuedMessage getQueuedMessage(String messageId) throws Exception;

    /**
     * Get an object containing detailed information on one specific message on the Queue.
     * Exception messages are returned in the server locale.
     * 
     * @param messageId
     * @return Message representing the message id
     * @throws Exception
     */
    public QueuedMessageDetail getQueuedMessageDetail(String messageId) throws Exception;

    /**
     * Get the data content, or payload, of one specific message on the Queue.
     * 
     * @param messageId the ID of the message
     * @param size the number of bytes to return. If the size is specified as zero, or
     *            the size exceeds the length of the data content, then the entire message content
     *            is returned.
     * @return Byte Array representing the message data (Payload)
     */
    public byte[] getMessageData(String messageId, java.lang.Integer size) throws Exception;

    /**
     * Delete a specific message on the Queue.
     * 
     * @param messageId the ID of the message
     * @param move flag indicating whether the message should be discarded. If false, then the message
     *            is physically deleted. If true, the message is moved to the exception destination for this queue,
     *            if one exists.
     * @throws Exception
     */
    public void deleteQueuedMessage(String messageId, java.lang.Boolean move) throws Exception;

    /**
     * Delete all messages on the Queue.
     * 
     * @param move flag indicating whether the message should be moved to the
     *            exception destination or deleted. If true, the message is moved to the
     *            exception destination for this queue, if one exists. If false, then the
     *            message is physically deleted.
     * @throws Exception
     */
    public void deleteAllQueuedMessages(java.lang.Boolean move) throws Exception;

    /**
     * Get an array of objects representing the messages on the Queue starting from fromIndex to toIndex
     * 
     * @param fromIndex Starting Index of the total messages retrieved
     * @param toIndex Last Index of the total messages retrieved
     * @param totalMessages Total number of messages to be retrieved in single invocation of this method
     * @return Array of total number of messages retrieved
     * 
     */
    public QueuedMessage[] getQueuedMessages(java.lang.Integer fromIndex, java.lang.Integer toIndex, java.lang.Integer totalMessages) throws Exception;

}
