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

import com.ibm.ws.sib.admin.exception.ControllableNotFoundException;
import com.ibm.ws.sib.admin.exception.InvalidArgumentException;
import com.ibm.ws.sib.admin.exception.RuntimeOperationFailedException;
import com.ibm.ws.sib.admin.mxbean.MessagingSubscription;
import com.ibm.ws.sib.admin.mxbean.QueuedMessage;
import com.ibm.ws.sib.admin.mxbean.QueuedMessageDetail;

/**
 * <p>
 * The TopicMBean is enabled by the wasJmsServer feature.
 * A TopicMBean is initialized for each Topic existing on the messaging engine.
 * Use the MBean programming interface to query runtime information about a Topic.
 * <br><br>
 * JMX clients should use the ObjectName of this MBean to query it
 * <br>
 * Partial Object Name: WebSphere:feature=wasJmsServer, type=Topic,name=* <br>
 * where name is unique for each Topic and is equal to the name of the Topic.
 * </p>
 * 
 * @ibm-api
 */
@MXBean
public interface TopicMBean {

    /**
     * The UUID of the Topic represented by this instance.
     * 
     * @return ID of the Topic
     */
    public String getId();

    /**
     * The identifier (name) attribute of the Topic represented
     * by this instance.
     * 
     * @return Name of the Topic
     */
    public String getIdentifier();

    /**
     * The maximum number of messages permitted at the Topic
     * 
     * @return Maximum number of messages permitted for the Topic
     */
    public long getMaxQueueSize();

    /**
     * Can producers send to this Topic?
     * 
     * @return true if send is allowed on the Topic else false
     */
    public boolean isSendAllowed();

    /**
     * Total number of unique messages on this Topic which are not yet passed to all subscribers
     * 
     * @return Number of messages present on the Topic
     */
    public long getDepth();

    // OPERATIONS

    /**
     * List an array of objects representing all Subscriptions at this Topic
     * 
     * @return Array of Subscriptions for the Topic
     */
    public MessagingSubscription[] listSubscriptions() throws Exception;

    /**
     * Get an object representing a specified Subscription
     * 
     * @param subId the ID of the subscription
     * @return Subscription represented by the Subscription ID
     * @throws Exception
     */
    public MessagingSubscription getSubscription(String subId) throws Exception;

    /**
     * Get an object representing a specified Subscription
     * 
     * @param subName the name of the subscription( name is represented as clientId##subscription name)
     * @return Subscription represented by the Subscription name
     * @throws Exception
     */
    public MessagingSubscription getSubscriptionByName(String subName) throws Exception;

    /**
     * Delete a subscription
     * 
     * @param subId
     * @throws InvalidArgumentException
     * @throws ControllableNotFoundException
     * @throws RuntimeOperationFailedException
     */
    public void deleteSubscription(String subId)
                    throws Exception;

    /**
     * Get an array of message objects representing the messages on a specified Subscription.
     * 
     * @return Array of messages ready to be subscribed by the Subscription representing Subscription ID
     */
    public QueuedMessage[] getSubscriptionMessages(String subId) throws Exception;

    /**
     * Get a message object representing one specific message on a specified Subscription.
     * 
     * @param subId the ID of the subscription
     * @param messageId the ID of the message
     * @return Message representing the Message ID and ready to be consumed by Subscription representing Subscription ID
     */
    public QueuedMessage getSubscriptionMessage(String subId, String messageId) throws Exception;

    /**
     * Get a message object containing detailed information on one specific message on a specified Subscription.
     * 
     * @param subId the ID of the subscription
     * @param messageId the ID of the message
     * @return Message representing the Message ID and ready to be consumed by Subscription representing Subscription ID
     * @throws Exception
     */
    public QueuedMessageDetail getSubscriptionMessageDetail(String subId, String messageId) throws Exception;

    /**
     * Get the data content, or payload, of one specific message on the Subscription.
     * 
     * @param subId the ID of the subscription
     * @param messageId the ID of the message
     * @param size the number of bytes to return. If the size is specified as zero, or
     *            the size exceeds the length of the data content, then the entire message content
     *            is returned.
     * @return Byte Array representing the Message Payload
     */
    public byte[] getSubscriptionMessageData(String subId, String messageId, java.lang.Integer size) throws Exception;

    /**
     * Delete a specific message on the Subscription.
     * 
     * @param subId the ID of the subscription
     * @param messageId the ID of the message
     * @param move flag indicating whether the message should be discarded. If false, then the message
     *            is physically deleted. If true, the message is moved to the exception destination for this queue,
     *            if one exists.
     * @throws Exception
     */
    public void deleteSubscriptionMessage(String subId, String messageId, java.lang.Boolean move) throws Exception;

}
