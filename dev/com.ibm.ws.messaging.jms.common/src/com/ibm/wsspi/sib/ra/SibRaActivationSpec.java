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

package com.ibm.wsspi.sib.ra;

import javax.resource.spi.ActivationSpec;

/**
 * Activation specification interface for inbound core SPI resource adapter.
 * Describes properties that may be configured via JCA administration.
 */
public interface SibRaActivationSpec extends ActivationSpec {

    /**
     * Message deletion mode indicating that each message should be deleted
     * immediately after MDB processing completes.
     */
    static String MESSAGE_DELETION_MODE_SINGLE = "Single";

    /**
     * Message deletion mode indicating that, if messages are received in a
     * batch, then the batch of messages will be deleted together after they
     * have all been delivered. This increases the window in which the server
     * may crash leading to the duplicate processing of messages but may result
     * in a performance improvement.
     */
    static String MESSAGE_DELETION_MODE_BATCH = "Batch";

    /**
     * Message deletion mode indicating that, if the MDB completes successfully,
     * the resource adapter will neither delete nor unlock the message. This
     * permits the MDB to either perform the delete itself or use the bifurcated
     * consumer support so that it may perform the deletion at some later time.
     * If the MDB throws an exception then the message will be unlocked.
     */
    static String MESSAGE_DELETION_MODE_APPLICATION = "Application";

    /**
     * Destination type representing a queue.
     */
    static String DESTINATION_TYPE_QUEUE = "Queue";

    /**
     * Destination type representing a topic space.
     */
    static String DESTINATION_TYPE_TOPIC_SPACE = "TopicSpace";

    /**
     * Destination type representing a port.
     */
    static String DESTINATION_TYPE_PORT = "Port";

    /**
     * Destination type representing a service.
     */
    static String DESTINATION_TYPE_SERVICE = "Service";

    /**
     * Indicates that a durable subscription may only be shared when running
     * inside a cluster.
     *
     * @see #SHARED_DSUBS_NEVER
     * @see #SHARED_DSUBS_ALWAYS
     */
    public static final String SHARED_DSUBS_IN_CLUSTER = "InCluster";

    /**
     * Indicates that a durable subscription may always be shared.
     *
     * @see #SHARED_DSUBS_IN_CLUSTER
     * @see #SHARED_DSUBS_NEVER
     */
    public static final String SHARED_DSUBS_ALWAYS = "AlwaysShared";

    /**
     * Indicates that a durable subscription may never be shared.
     *
     * @see #SHARED_DSUBS_IN_CLUSTER
     * @see #SHARED_DSUBS_ALWAYS
     */
    public static final String SHARED_DSUBS_NEVER = "NeverShared";

    /**
     * If a destination name is given this property is required in order to
     * specify the bus on which that destination is located. If a destination
     * name is not given then this property may be optionally specified to
     * restrict the destinations from which messages will be receive to those on
     * the given bus.
     *
     * @param busName
     *            the bus name
     */
    void setBusName(String busName);

    /**
     * Returns the name of the bus containing the destination(s) to receive
     * messages from.
     *
     * @return the bus name
     */
    String getBusName();

    /**
     * Sets the user name with which to connect to the messaging engine.
     * Defaults to <code>null</code>.
     *
     * @param userName
     *            the user name
     */
    void setUserName(String userName);

    /**
     * Returns the user name with which to connect to the messaging engine.
     *
     * @return the user name
     */
    String getUserName();

    /**
     * Sets the password with which to connect to the messaging engine. Defaults
     * to <code>null</code>.
     *
     * @param password
     *            the password
     */
    void setPassword(String password);

    /**
     * Returns the password with which to connect to the messaging engine.
     *
     * @return the password
     */
    String getPassword();

    /**
     * Sets the type of the destination to consume messages from. One of
     * <code>DESTINATION_TYPE_QUEUE</code>,
     * <code>DESTINATION_TYPE_TOPIC_SPACE</code>,
     * <code>DESTINATION_TYPE_PORT</code> or
     * <code>DESTINATION_TYPE_SERVICE</code>. Required property.
     *
     * @param destinationType
     *            the destination type
     */
    void setDestinationType(String destinationType);

    /**
     * Returns the type of the destination to consume messages from.
     *
     * @return the destination type
     */
    String getDestinationType();

    /**
     * Sets the name of the destination on the given bus from which the
     * message-driven bean should receive messages. If not set then the MDB will
     * receive messages from all destinations of the given type localized by
     * messaging engines on the given bus in the same server as the MDB. This
     * property is required for a destination type of
     * <code>DESTINATION_TYPE_TOPIC_SPACE</code>.
     *
     * @param destinationName
     *            the destination name
     */
    void setDestinationName(String destinationName);

    /**
     * The name of the destination on the given bus from which the
     * message-driven bean should receive messages.
     *
     * @return the destination name
     */
    String getDestinationName();

    /**
     * Sets a selector string used to filter which messages are received from
     * the destination(s). Defaults to <code>null</code>.
     *
     * @param messageSelector
     *            the message selector
     */
    void setMessageSelector(String messageSelector);

    /**
     * Returns a selector string used to filter which messages are received from
     * the destination(s).
     *
     * @return the message selector
     */
    String getMessageSelector();

    /**
     * Sets the discriminator to specify when registering for message
     * consumption. Defaults to <code>null</code>.
     *
     * @param discriminator
     *            the discriminator
     */
    void setDiscriminator(String discriminator);

    /**
     * Returns the discriminator to specify when registering for message
     * consumption.
     *
     * @return the discriminator
     */
    String getDiscriminator();

    /**
     * For a destination type of <code>DESTINATION_TYPE_TOPIC_SPACE</code>
     * indicates that a durable subscription should be created with the given
     * name. Defaults to <code>null</code> indicating the subscription should
     * not be durable.
     *
     * @param subscriptionName
     *            the subscription name
     */
    void setSubscriptionName(String subscriptionName);

    /**
     * Returns the name of the durable subscription for a destination type of
     * <code>DESTINATION_TYPE_TOPIC_SPACE</code> or <code>null</code> if the
     * subscription is not durable.
     *
     * @return the subscription name
     */
    String getSubscriptionName();

    /**
     * Sets the name of the messaging engine on which durable subscriptions will
     * be created.
     *
     * @param durableSubscriptionHome
     *            the durable subscription home
     */
    void setDurableSubscriptionHome(String durableSubscriptionHome);

    /**
     * Returns the name of the messaging engine on which durable subscriptions
     * will be created.
     *
     * @return the durable subscription home
     */
    String getDurableSubscriptionHome();

    /**
     * Sets whether durable subscriptions are shared. One of
     * <code>SHARED_DSUBS_AS_CLUSTER</code>,<code>SHARED_DSUBS_ALWAYS</code>
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
     * Sets the maximum number of messages delivered in a single batch (serially
     * on the same thread) to an MDB instance. Defaults to 1.
     *
     * @param maxBatchSize
     *            the maximum batch size
     */
    void setMaxBatchSize(Integer maxBatchSize);

    /**
     * Sets the maximum number of messages delivered in a single batch (serially
     * on the same thread) to an MDB instance. Defaults to 1.
     *
     * @param maxBatchSize
     *            the maximum batch size
     */
    void setMaxBatchSize(String maxBatchSize);

    /**
     * Returns the maximum number of messages delivered in a single batch to an
     * MDB instance.
     *
     * @return the maximum batch size
     */
    Integer getMaxBatchSize();

    /**
     * Sets the mode used to delete messages when used with a non-transactional
     * MDB. One of <code>MESSAGE_DELETION_MODE_SINGLE</code> (default),
     * <code>MESSAGE_DELETION_MODE_BATCH</code> or
     * <code>MESSAGE_DELETION_MODE_APPLICATION</code>. If the MDB is
     * transactional then this mode is ignored and the message will always be
     * delivered as part of the transaction.
     *
     * @param messageDeletionMode
     *            the message deletion mode
     */
    void setMessageDeletionMode(String messageDeletionMode);

    /**
     * Returns the mode used to delete messages when used with a
     * non-transactional MDB.
     *
     * @return the message deletion mode
     */
    String getMessageDeletionMode();

    /**
     * Sets the maximum number of messages that will be delivered concurrently
     * from a single destination. May be set to 1 to ensure message ordering is
     * retained. Defaults to 10.
     *
     * @param maxConcurrency
     *            the maximum concurrency
     */
    void setMaxConcurrency(Integer maxConcurrency);

    /**
     * Sets the maximum number of messages that will be delivered concurrently
     * from a single destination. May be set to 1 to ensure message ordering is
     * retained. Defaults to 10.
     *
     * @param maxConcurrency
     *            the maximum concurrency
     */
    void setMaxConcurrency(String maxConcurrency);

    /**
     * Returns the maximum number of messages that will be delivered
     * concurrently from a single destination.
     *
     * @return the maximum concurrency
     */
    Integer getMaxConcurrency();

    /**
     * Set the target property.
     * @param target
     */
    void setTarget (String target);

    /**
     * Get the target property.
     * @return the target property
     */
    String getTarget ();

    /**
     * Set the target type property.
     * @param targetType
     */
    void setTargetType (String targetType);

    /**
     * Get the target type property.
     * @return the target type property
     */
    String getTargetType ();

    /**
     * Set the target significance property.
     * @param target significance
     */
    void setTargetSignificance (String targetSignificance);

    /**
     * Get the target significance property.
     * @return the target significance property
     */
    String getTargetSignificance ();
    /**
     * Set the target transport chain property.
     * @param target transport chain
     */
    void setTargetTransportChain (String targetTransportChain);

    /**
     * Get the target transport chain property.
     * @return the target transport chain property
     */
    String getTargetTransportChain ();

    /**
     * Set the useServerSubject property.
     * @param useServerSubject
     */
    void setUseServerSubject (Boolean useServerSubject);

    /**
     * Get the useServerSubject property.
     * @return the useServerSubject property
     */
    Boolean getUseServerSubject ();

    /**
     * Set the provider endpoints property.
     * @param providerEndpoints The new provider endpoints
     */
    void setProviderEndpoints (String providerEndpoints);

    /**
     * Get the provider endpoints property.
     * @return the provider endpoints property
     */
    String getProviderEndpoints ();

    /**
     * Gets the target Transport  property
     * @return the target Transport property
     */
    public String getTargetTransport();


    /**
     * Set the Target Transport property
     * @param targetTransport The Target Transport property to use
     */
    public void setTargetTransport(String targetTransport);
    
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
     * Get the MaxSequentialMessageFailure property
     * @return
     */
    Integer getMaxSequentialMessageFailure();

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
     * @return
     */
    Integer getAutoStopSequentialMessageFailure();

    /**
     * Sets the activate all MDBs in a cluster bus member property.
     */
    void setAlwaysActivateAllMDBs (Boolean activateAllMDBs);

    /**
     * Gets the activate all MDBs in a cluster bus member property.
     */
    Boolean getAlwaysActivateAllMDBs ();

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
     * @return
     */
    Integer getRetryInterval();

    /**
     * Used to indicate whether the destination name property should be treated as a
     * wildcard expression.
     * 
     * The default value for this property is False, which indicates that the destination name
     * describes a single destination. When set to True, this property will cause the MDB application
     * to simultaneously listen to all destinations local to the connected messaging engine that
     * match the specified wildcard expression.
     * 
     * For example, when this property is set to True, the destination name sca/moduleName//* matches
     * destinations with names like sca/moduleName/one, sca/moduleName/two etc (but not sca/moduleName).
     *
     * When this property is set to true, the patterns of supported wildcards in the destination name
     * are the same as those specified in the InfoCenter topic on "Topic names and use of wildcard
     * characters in topic expressions" (rjo0002_.html)
     * 
     * @param usesDestinationWildcard Boolean True/False to indicate whether the destination name
     *        should be treated as a wildcard expression or not.
     */
    void setUseDestinationWildcard (Boolean usesDestinationWildcard);

    /**
     * Retrieve the variable indicating whether the destination name property should be treated
     * as a wildcard expression.
     * 
     * @return Boolean that indicates whether the destination name should be treated as a
     *         wildcard expression
     */
    Boolean getUseDestinationWildcard ();

    /**
     * Gets the failing message delay time
     * @return the failing message delay
     */
    Long getFailingMessageDelay ();
    
    /**
     * Sets the failing message delay time
     * @param set the failing message delay
     */
    void setFailingMessageDelay (Long delay);
    
}
