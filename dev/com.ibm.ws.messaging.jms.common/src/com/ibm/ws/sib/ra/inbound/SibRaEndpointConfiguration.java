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

package com.ibm.ws.sib.ra.inbound;

import javax.resource.spi.ActivationSpec;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * Interface implemented by resource adapters to provide API agnostic endpoint
 * configuration information.
 */
public interface SibRaEndpointConfiguration {

    /**
     * Returns the destination from which messages should be received.
     * 
     * @return the destination
     */
    SIDestinationAddress getDestination();

    /**
     * Returns the destination name.
     * 
     * @return The destination name
     */
    String getDestinationName();

    /**
     * Returns the type of the destination from which messages should be
     * received.
     * 
     * @return the destination
     */
    DestinationType getDestinationType();

    /**
     * Gets the destination discriminator.
     * 
     * @return the discriminator
     */
    String getDiscriminator();

    /**
     * Returns the maximum concurrency for dispatchers on this endpoint.
     * 
     * @return the maximum concurrency
     */
    int getMaxConcurrency();

    /**
     * Returns the maximum size for batches of messages.
     * 
     * @return the maximum batch size
     */
    int getMaxBatchSize();

    /**
     * Returns the name of the bus to connect to.
     * 
     * @return the bus name
     */
    String getBusName();

    /**
     * Returns the user name with which to connect to the messaging engine.
     * 
     * @return the user name
     */
    String getUserName();

    /**
     * Returns the password with which to connect to the messaging engine.
     * 
     * @return the password
     */
    String getPassword();

    /**
     * Returns the message selector.
     * 
     * @return the message selector
     */
    String getMessageSelector();

    /**
     * Gets the name of a durable subscription.
     * 
     * @return the name
     */
    String getDurableSubscriptionName();

    /**
     * Returns the home for a durable subscription.
     * 
     * @return the home
     */
    String getDurableSubscriptionHome();

    /**
     * Gets whether durable subscriptions should be shared.
     * 
     * @return whether durable subscriptions should be shared
     */
    SibRaDurableSubscriptionSharing getShareDurableSubscriptions();

    /**
     * Returns the message deletion mode.
     * 
     * @return the message deletion mode
     */
    SibRaMessageDeletionMode getMessageDeletionMode();

    /**
     * Returns <code>true</code> if this configuration represents a durable
     * subscription.
     * 
     * @return <code>true</code> if this configuration represents a durable
     *         subscription
     */
    boolean isDurableSubscription();

    /**
     * Returns <code>true</code> if this configuration represents a durable
     * subscription.
     * 
     * @return <code>true</code> if this configuration represents a durable
     *         subscription
     */
    String getSubscriptionDurability();

    /**
     * Returns the <code>SelectorDomain</code>.
     * 
     * @return the domain
     */
    SelectorDomain getSelectorDomain();

    /**
     * Returns <code>true</code> if the message store data source should be
     * shared with the container managed persistence data source to achieve a
     * one-phase commit optimization.
     * 
     * @return <code>true</code> if the data source should be shared
     */
    boolean getShareDataSourceWithCMP();

    /**
     * Returns the activation specification from which this configuration was
     * obtained. Used by the handler exit point framework.
     * 
     * @return the activation specification
     */
    ActivationSpec getActivationSpec();

    /**
     * Returns the name of the target transport chain used when connecting to a
     * remote messaging engine.
     * 
     * @return the target transport chain name
     */
    String getTargetTransportChain();

    /**
     * Returns the property used to control read ahead optimization during
     * message receipt.
     * 
     * @return the read ahead property
     */
    SibRaReadAhead getReadAhead();

    /**
     * Returns the target property, this meaning of this value depends on the value
     * that target type has been set to
     * 
     * @return The target property
     */
    String getTarget();

    /**
     * Returns the target type, this is used in conjunction with target and target significance
     * to select a target whenever the RA has to perform a remote connection.
     * 
     * @return The target type
     */
    String getTargetType();

    /**
     * Returns the target significance, this is used in conjunction with target and target type
     * to select a target whenever the RA has to perform a remote connection
     * 
     * @return the target significance
     */
    String getTargetSignificance();

    /**
     * This method checks to see if the endpoint configuration represents a JMSRa or the core
     * spi ra.
     * 
     * @return true is this configuration represents the JMS Ra and false otherwise
     */
    boolean isJMSRa();

    /**
     * This method returns the provider endpoints configured in the activation
     * specification.
     * 
     * @return
     */
    String getProviderEndpoints();

    /**
     * Get the useServerSubject property.
     * 
     * @return the useServerSubject property
     */
    boolean getUseServerSubject();

    /**
     * Get the Max Sequential Message Failure property
     * 
     * @return
     */
    int getMaxSequentialMessageFailure();

    /**
     * Get the Auto Stop Sequential Message Failure property
     * 
     * @return
     */
    int getAutoStopSequentialMessageFailure();

    /**
     * Gets the activate all MDBs in a cluster bus member property.
     */
    Boolean getAlwaysActivateAllMDBs();

    /**
     * Get the RetryInterval property
     * 
     * @return
     */
    int getRetryInterval();

    /**
     * Gets the flag indicating if destination wildcard is being used.
     * 
     * @return True if a destation wildcard is being used
     */
    Boolean getUseDestinationWildcard();

    /**
     * Get the flag indicating if message gathering is allowed
     */
    String getAllowMessageGathering();

    /**
     * Gets the failing message delay time
     * 
     * @return the failing message delay
     */
    Long getFailingMessageDelay();

    /**
     * Get the target transport.Possible Values :
     * BINDING
     * CLIENT
     * BINDING_THEN_CLIENT
     * 
     * @return the target transport
     */
    String getTargetTransport();
}
