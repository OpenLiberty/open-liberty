/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

import com.ibm.wsspi.sib.core.DestinationType;

/**
 * This class represents the destination of type Queue or Topic
 *
 */
public interface SIBDestination extends BaseDestination {

	/**
	 * Get the type of destination 
	 * @return DestinationType
	 */
	public DestinationType getDestinationType();

	/**
	 * Set the destination type
	 * @param destinationType
	 */
	public void setDestinationType(DestinationType destinationType);

	/**
	 * Indicates if the Producer can Override the QoS setting defined in the destination
	 * @return boolean
	 */
	public boolean isOverrideOfQOSByProducerAllowed();

	/**
	 * Set the overrideOfQOSByProducerAllowed attribute
	 * @param overrideOfQOSByProducerAllowed
	 */
	public void setOverrideOfQOSByProducerAllowed(
			boolean overrideOfQOSByProducerAllowed);

	/**
	 * Get the default reliability of the destination
	 * Values returned are :
	 * BestEffortNonPersistent
     * ExpressNonPersistent
     * ReliableNonPersistent
     * ReliablePersistent
     * AssuredPersistent
	 * @return String
	 */
	public String getDefaultReliability();

	/**
	 * Set the defaultReliability attribute
	 * @param defaultReliability
	 */
	public void setDefaultReliability(String defaultReliability);

	/**
	 * Get the maximum reliability of a destination
	 * Valid values returned are :
	 * BestEffortNonPersistent
     * ExpressNonPersistent
     * ReliableNonPersistent
     * ReliablePersistent
     * AssuredPersistent 
	 * @return String
	 */
	public String getMaximumReliability();

	/**
	 * Set the maximumReliability attribute
	 * @param maximumReliability
	 */
	public void setMaximumReliability(String maximumReliability);

	/**
	 * Get the default reliability.By default the value is 0
	 * @return int
	 */
	public int getDefaultPriority();

	/**
	 * Get the defaultPriority attribute
	 * @param defaultPriority
	 */
	public void setDefaultPriority(int defaultPriority);

	/**
	 * Get the default exception destination. By default it is _SYSTEM.Exception.Destination
	 * unless its changed
	 * @return
	 */
	public String getExceptionDestination();

	/**
	 * Set the exception destination
	 * @param exceptionDestination
	 */
	public void setExceptionDestination(String exceptionDestination);

	/**
	 * Get blockedRetryTimeout attribute
	 * @return
	 */
	public long getBlockedRetryTimeout();

	/**
	 * Set the blockedRetryTimeout attribute
	 * @param blockedRetryTimeout
	 */
	
	public void setBlockedRetryTimeout(long blockedRetryTimeout);

	/**
	 * Get maxFailedDeliveries attribute
	 * @return
	 */
	public int getMaxFailedDeliveries();

	/**
	 * Sets the maxFailedDeliveries attribute
	 * @param maxFailedDeliveries
	 */
	public void setMaxFailedDeliveries(int maxFailedDeliveries);

	/**
     * Indicates if messages can be sent to the destination
     * @return String
     */
	public boolean isSendAllowed();

	/**
     * Sets the sendAllowed atribute
     * @param sendAllowed
     */
	public void setSendAllowed(boolean sendAllowed);

	 /**
     * Indicates if messages can be received from the destination
     * @return String
     */
	public boolean isReceiveAllowed();

	/**
     * Sets the receiveAllowed attribute
     * @param receiveAllowed
     */
	public void setReceiveAllowed(boolean receiveAllowed);

	/**
	 * Get the receiveExclusive attribute
	 * @return boolean
	 */
	public boolean isReceiveExclusive();

	/**
	 * Set the receiveExclusive attribute
	 * @param receiveExclusive
	 */
	public void setReceiveExclusive(boolean receiveExclusive);

	/**
	 * Indicates if the messages have to maintained in order
	 * @return boolean
	 */
	public boolean isMaintainStrictOrder();

	/**
	 * Get the  maintainStrictOrder attribute
	 * @param maintainStrictOrder
	 */
	public void setMaintainStrictOrder(boolean maintainStrictOrder);

	/**
	 * Get the highMessageThreshold
	 * @return
	 */
	public long getHighMessageThreshold();

	/**
	 * Set highMessageThreshold
	 * @param highMessageThreshold
	 */
	public void setHighMessageThreshold(long highMessageThreshold);

	/**
	 * Indicates if redelivery count have to be persisted
	 * @return
	 */
	public boolean isPersistRedeliveryCount();

	/**
	 * Set the persistRedeliveryCount
	 * @param persistRedeliveryCount
	 */
	public void setPersistRedeliveryCount(boolean persistRedeliveryCount);

	/**
	 * Getter for exceptionDiscardReliability
	 * @return String
	 */
	public String getExceptionDiscardReliability();

	/**
	 * Setter for exceptionDiscardReliability
	 * @param exceptionDiscardReliability
	 */
	public void setExceptionDiscardReliability(
			String exceptionDiscardReliability);

	/**
	 * Indicates if Access Check is required for the Topic.
	 * topicAccessCheckRequired is only valid for destination of type TOPIC
	 * @return
	 */
	public boolean isTopicAccessCheckRequired();

	/**
	 * Setter for topicAccessCheckRequired
	 * @param topicAccessCheckRequired
	 */
	public void setTopicAccessCheckRequired(boolean topicAccessCheckRequired);

	/**
	 * Set default and max reliability
	 * The rule is maxrealibility >= defaultreliability
	 * @param defaultReliability
	 * @param maxReliability
	 */
	public void setDefaultAndMaxReliability(String defaultReliability,
			String maxReliability,JsMEConfig jsmeConfig,boolean modified);
	/**
	 * Set failed delivery policy 	
	 * @param failedDeliveryPolicy values can be SEND_TO_EXCEPTION_DESTINATION, DISCARD , KEEP_TRYING
	 */
	public void setFailedDeliveryPolicy(String failedDeliveryPolicy);
	/**
	 * Get failed delivery policy
	 * the default value is SEND_TO_EXCEPTION_DESTINATION if nothing is set by user
	 */
        public String getFailedDeliveryPolicy();
    
}
