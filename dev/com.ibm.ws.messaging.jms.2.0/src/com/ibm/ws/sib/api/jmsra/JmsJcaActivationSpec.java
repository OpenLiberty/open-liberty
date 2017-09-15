/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*****************************************************************************/
/* Note: For all setter methods that accept arguments in a type which is not */
/* String, eg void setMaxBatchSize(Integer maxBatchSize) there should always */
/* be an equivalent setter method that accepts a String argument, eg         */
/* void setMaxBatchSize(String maxBatchSize). This is because non-WAS appl-  */
/* ication servers always look for setters which take String arguments.      */
/*****************************************************************************/

package com.ibm.ws.sib.api.jmsra;

import java.io.Serializable;

import javax.jms.Destination;
import javax.resource.spi.ActivationSpec;

/**
 * Activation spec interface for the JMS resource adapter.
 */
public interface JmsJcaActivationSpec extends ActivationSpec, Serializable {

    /**
     * Sets the user name to connect to the message engine with. Defaults to
     * <code>null</code>.
     * 
     * @param userName
     *            the user name
     */
    void setUserName(String userName);

    /**
     * Gets the user name to connect to the message engine with.
     * 
     * @return the user name
     */
    String getUserName();

    /**
     * Sets the password to connect to the message engine with. Defaults to
     * <code>null</code>.
     * 
     * @param password
     *            the password
     */
    void setPassword(String password);

    /**
     * Gets the password to connect to the message engine with.
     * 
     * @return the password
     */
    String getPassword();

    /**
     * Set the destination to recieve messages from
     * 
     * @param destination
     *            the destination
     */
    void setDestination(Destination destination);

    /**
     * Get the destination to recieve messages from
     * 
     * @return the destination.
     */
    Destination getDestination();

    /**
     * Set the name of the destination to receive messages from
     * 
     * @param destinationName
     *            the name of the destination
     */
    void setDestinationName(String destinationName);

    /**
     * Get the name of the destination to receive messages from
     * 
     * @return the name of the destination
     */
    String getDestinationName();

    /**
     * Sets the type of the destination to receive messages from. One of
     * <code>javax.jms.Queue</code> or <code>javax.jms.Topic</code>.
     * Required property.
     * 
     * @param destinationType
     *            the destination type
     */
    void setDestinationType(String destinationType);

    /**
     * Gets the type of the destination to receive messages from.
     * 
     * @return the destination type
     */
    String getDestinationType();

    /**
     * Sets the message selector for filtering messages received. Defaults to
     * <code>null</code>.
     * 
     * @param messageSelector
     *            the message selector
     */
    void setMessageSelector(String messageSelector);

    /**
     * Gets the message selector for filtering messages received.
     * 
     * @return the message selector
     */
    String getMessageSelector();

    /**
     * Sets the acknowledge mode. One of <code>Auto-acknowledge</code> or
     * <code>Dups-ok-acknowledge</code>. Defaults to
     * <code>Auto-acknowledge</code>.
     * 
     * @param acknowledgeMode
     *            the acknowledge mode
     */
    void setAcknowledgeMode(String acknowledgeMode);

    /**
     * Gets the acknowledge mode.
     * 
     * @return the acknowledge mode
     */
    String getAcknowledgeMode();

    /**
     * Sets the subsciption durability. One of <code>Durable</code> or
     * <code>NonDurable</code>. Default is <code>NonDurable</code>.
     * 
     * @param subscriptionDurability
     *            the subscription durability
     */
    void setSubscriptionDurability(String subscriptionDurability);

    /**
     * Gets the subscription durability.
     * 
     * @return the subscription durability
     */
    String getSubscriptionDurability();

    /**
     * Sets the name of the home for durable subscriptions.
     * 
     * @param durableSubscriptionHome
     *            the name of the home for durable subscriptions
     */
    void setDurableSubscriptionHome(String durableSubscriptionHome);

    /**
     * Gets the name of the home for durable subscriptions.
     * 
     * @return the name of the home for durable subscriptions
     */
    String getDurableSubscriptionHome();

    /**
     * Sets whether durable subscriptions are shared. One of
     * <code>SHARED_DSUBS_IN_CLUSTER</code>,<code>SHARED_DSUBS_ALWAYS</code>
     * or <code>SHARED_DSUBS_NEVER</code>.
     * 
     * @param shareDurableSubscriptions
     *            flag indicating whether durable subscriptions are shared.
     */
    void setShareDurableSubscriptions(String shareDurableSubscriptions);

    /**
     * Returns indication of whether durable subscriptions are shared.
     * 
     * @return indication of whether durable subscriptions are shared
     */
    String getShareDurableSubscriptions();

    /**
     * Sets the client identifier. Required for durable topic subscriptions.
     * 
     * @param clientId
     *            the client identifier
     */
    void setClientId(String clientId);

    /**
     * Gets the client identifier.
     * 
     * @return the client identifier
     */
    String getClientId();

    /**
     * Sets the subscription name. Required for durable topic destinations.
     * 
     * @param subscriptionName
     *            the subscription name
     */
    void setSubscriptionName(String subscriptionName);

    /**
     * Gets the subscription name.
     * 
     * @return the subscription name
     */
    String getSubscriptionName();

    /**
     * Sets the maximum number of messages delivered in a batch to an MDB.
     * Defaults to <code>1</code>.
     * 
     * @param maxBatchSize
     *            the maximum number of messages in a batch
     */
    void setMaxBatchSize(Integer maxBatchSize);

    /**
     * Sets the maximum number of messages delivered in a batch to an MDB.
     * Defaults to <code>1</code>.
     * 
     * @param maxBatchSize
     *            the maximum number of messages in a batch
     */
    void setMaxBatchSize(String maxBatchSize);

    /**
     * Gets the maximum number of messages delivered in a batch to an MDB.
     * 
     * @return the maximum number of messages in a batch
     */
    Integer getMaxBatchSize();

    /**
     * Sets the maximum number of concurrent threads for delivering messages.
     * Defaults to <code>10</code>.
     * 
     * @param maxConcurrency
     *            the maximum concurrency
     */
    void setMaxConcurrency(Integer maxConcurrency);

    /**
     * Sets the maximum number of concurrent threads for delivering messages.
     * Defaults to <code>10</code>.
     * 
     * @param maxConcurrency
     *            the maximum concurrency
     */
    void setMaxConcurrency(String maxConcurrency);

    /**
     * Gets the maximum number of concurrent threads for delivering messages.
     * 
     * @return the maximum concurrency
     */
    Integer getMaxConcurrency();

    /**
     * Sets the name of the bus to connect to.
     * 
     * @param busName
     *            the bus name
     */
    void setBusName(String busName);

    /**
     * Gets the name of the bus to connect to.
     * 
     * @return the bus name
     */
    String getBusName();

    /**
     * Sets the flag indicating whether the connection to the messaging engine
     * database should be shared with that use for container-managed
     * persistence.
     * 
     * @param sharing
     *            <code>true</code> if the connection should be shared,
     *            otherwise <code>false</code>
     */
    void setShareDataSourceWithCMP(Boolean sharing);

    /**
     * Returns the flag indicating whether the connection to the messaging
     * engine database should be shared with that use for container-managed
     * persistence.
     * 
     * @return <code>true</code> if the connection should be shared, otherwise
     *         <code>false</code>
     */
    Boolean getShareDataSourceWithCMP();

    /**
     * Sets the name of the target transport chain used when connecting to a
     * remote messaging engine.
     * 
     * @param targetTransportChain
     *            the target transport chain name
     */
    void setTargetTransportChain(String targetTransportChain);

    /**
     * Returns the name of the target transport chain used when connecting to a
     * remote messaging engine.
     * 
     * @return the target transport chain name
     */
    String getTargetTransportChain();

    /**
     * Set the useServerSubject property.
     * 
     * @param useServerSubject
     */
    void setUseServerSubject(Boolean useServerSubject);

    /**
     * Get the useServerSubject property.
     * 
     * @return the useServerSubject property
     */
    Boolean getUseServerSubject();

    /**
     * Sets the property used to control read ahead optimization during message
     * receipt. One of <code>Default</code>,<code>AlwaysOn</code> or
     * <code>AlwaysOff</code>. The default behaviour is to enable the
     * optimization for non-durable subscriptions and unshared durable
     * subscriptions.
     * 
     * @param readAhead
     *            the read ahead property
     */
    void setReadAhead(String readAhead);

    /**
     * Returns the property used to control read ahead optimization during
     * message receipt.
     * 
     * @return the read ahead property
     */
    String getReadAhead();

    /**
     * Set the target property.
     * 
     * @param target
     */
    void setTarget(String target);

    /**
     * Get the target property.
     * 
     * @return the target property
     */
    String getTarget();

    /**
     * Set the target type property.
     * 
     * @param targetType
     */
    void setTargetType(String targetType);

    /**
     * Get the target type property.
     * 
     * @return the target type property
     */
    String getTargetType();

    /**
     * Set the target significance property.
     * 
     * @param target significance
     */
    void setTargetSignificance(String targetSignificance);

    /**
     * Get the target significance property.
     * 
     * @return the target significance property
     */
    String getTargetSignificance();

    /**
     * Set the provider endpoints property.
     * 
     * @param providerEndpoints The new provider endpoints
     */
    void setProviderEndpoints(String providerEndpoints);

    /**
     * Get the provider endpoints property.
     * 
     * @return the provider endpoints property
     */
    String getProviderEndpoints();

    /**
     * Set the provider endpoints property.Which is
     * nothing but the provider endpoint.This change
     * is specific to liberty as the name is changed
     * to remoteServerAddress instead of providerEndpoints.
     * But internally we will set provider endpoints so that
     * code will not be altered everywhere else.
     * 
     * @param remoteServerAddress The new connection name
     */
    void setRemoteServerAddress(String remoteServerAddress);

    /**
     * Get the connection name property, which is
     * nothing but the provider endpoints
     * 
     * @return the connection name property
     */
    String getRemoteServerAddress();

    /**
     * Sets the target transport
     * 
     * @param targetTransport
     *            the target transport
     */
    public void setTargetTransport(String targetTransport);

    /**
     * Get the target transport property.
     * 
     * @return the target transport property
     */
    String getTargetTransport();

    /**
     * Set the MaxSequentialMessageFailure property
     * 
     * @param maxSequentialMessageFailure
     */
    void setMaxSequentialMessageFailure(Integer maxSequentialMessageFailure);

    /**
     * Set the MaxSequentialMessageFailure property
     * 
     * @param maxSequentialMessageFailure
     */
    void setMaxSequentialMessageFailure(String maxSequentialMessageFailure);

    /**
     * Get the AutoStopSequentialMessageFailure property
     * 
     * @return
     */
    Integer getAutoStopSequentialMessageFailure();

    /**
     * Set the AutoStopSequentialMessageFailure property
     * 
     * @param autoStopSequentialMessageFailure
     */
    void setAutoStopSequentialMessageFailure(Integer autoStopSequentialMessageFailure);

    /**
     * Set the AutoStopSequentialMessageFailure property
     * 
     * @param autoStopSequentialMessageFailure
     */
    void setAutoStopSequentialMessageFailure(String autoStopSequentialMessageFailure);

    /**
     * Get the AutoStopSequentialMessageFailure property
     * 
     * @return
     */
    Integer getMaxSequentialMessageFailure();

    /**
     * Gets the property indicating if the consumer will modify the payload after getting it.
     * 
     * @return String containing the property value.
     */
    String getConsumerDoesNotModifyPayloadAfterGet();

    /**
     * Sets the property that indicates if the consumer will modify the payload after getting it.
     * 
     * @param propertyValue containing the property value.
     */
    void setConsumerDoesNotModifyPayloadAfterGet(String propertyValue);

    /**
     * Gets the property indicating if the forwarder will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    String getForwarderDoesNotModifyPayloadAfterSet();

    /**
     * Sets the property that indicates if the forwarder will modify the payload after setting it.
     * 
     * @param propertyValue containing the property value.
     */
    void setForwarderDoesNotModifyPayloadAfterSet(String propertyValue);

    /**
     * Sets the activate all MDBs in a cluster bus member property.
     */
    void setAlwaysActivateAllMDBs(Boolean activateAllMDBs);

    /**
     * Gets the activate all MDBs in a cluster bus member property.
     */
    Boolean getAlwaysActivateAllMDBs();

    /**
     * Set the RetryInterval property
     * 
     * @param retryInterval
     */
    void setRetryInterval(Integer retryInterval);

    /**
     * Set the RetryInterval property
     * 
     * @param retryInterval
     */
    void setRetryInterval(String retryInterval);

    /**
     * Get the RetryInterval property
     * 
     * @return
     */
    Integer getRetryInterval();

    /**
     * Gets the failing message delay time
     * 
     * @return the Failing message delay
     */
    Long getFailingMessageDelay();

    /**
     * GSts the failing message delay time
     * 
     * @param delay the failing message delay
     */
    void setFailingMessageDelay(Long delay);

    /**
     * Whether to share durable subscription or not
     * 
     * @param shareDurSubs
     */
    void setShareDurableSubscription(Boolean shareDurSubs);

    /**
     * This method does lookup of {@link Destination} based on the given JNDI and sets into this.
     * 
     * @param _destinationLookup - the JNDI name of the Destination which needs to be dynamically looked upon.
     */
    void setDestinationLookup(String _destinationLookup);

    /**
     * 
     * This method gets the properties of administratively configured ConnectionFactory and sets in this object's respective properties.
     * 
     * @param _connectionFactoryLookup - the JNDI name of the Connection Factory which needs to be dynamically looked upon.
     */
    void setConnectionFactoryLookup(String _connectionFactoryLookup);
}
