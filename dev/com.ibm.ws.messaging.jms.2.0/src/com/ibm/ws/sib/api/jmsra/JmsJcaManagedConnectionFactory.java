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

package com.ibm.ws.sib.api.jmsra;

import javax.jms.JMSException;
import javax.naming.spi.ObjectFactory;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * The JMS resource adapter factory for creation of JMS connection factories.
 * Adds setters/getters for the properties required by Jetstream and JMS. These
 * properties are still subject to change.
 */
public interface JmsJcaManagedConnectionFactory extends
                ManagedConnectionFactory, ObjectFactory {

    /**
     * Sets the default user name for use when none is provided by the
     * application or container.
     * 
     * @param userName
     *            the default user name
     */
    void setUserName(String userName);

    /**
     * Returns the default user name for use when none is specified by the
     * application or container.
     * 
     * @return the default user name
     */
    String getUserName();

    /**
     * Sets the default password for use when none is specified by the
     * application or container.
     * 
     * @param password
     *            the default password
     */
    void setPassword(String password);

    /**
     * Returns the default password for use when none is specified by the
     * application or container.
     * 
     * @return the default password
     */
    String getPassword();

    /**
     * Sets the client ID for this connection factory. Must be set to use
     * durable subscriptions.
     * 
     * @param clientID
     *            the client ID
     */
    void setClientID(String clientID);

    /**
     * Returns the client ID.
     * 
     * @return the client ID
     */
    String getClientID();

    /**
     * Sets the bus name to connect to.
     * 
     * @param busName
     *            the bus name
     */
    void setBusName(String busName);

    /**
     * Returns the bus name to connect to.
     * 
     * @return the bus name
     */
    String getBusName();

    /**
     * Set the non persistent mapping message reliability to use for
     * non-persistent messages
     * 
     * @param nonPersistentMapping
     *            the non persistent mapping setting
     */
    void setNonPersistentMapping(String nonPersistentMapping);

    /**
     * Set the persistent mapping message reliability to use for persistent
     * messages
     * 
     * @param persistentMapping
     *            the persistent mapping setting
     */
    void setPersistentMapping(String persistentMapping);

    /**
     * Returns the message reliability to use for non-persistent messages.
     * 
     * @return the non persistent mapping
     */
    String getNonPersistentMapping();

    /**
     * Returns the message reliability to use for persistent messages.
     * 
     * @return the persistent mapping
     */
    String getPersistentMapping();

    /**
     * Returns the name of the durable subscription home.
     * 
     * @return the durable subscription home
     */
    String getDurableSubscriptionHome();

    /**
     * Sets the name of the durable subscription home.
     * 
     * @param durableSubscriptionHome
     *            the durable subscription home
     */
    void setDurableSubscriptionHome(String durableSubscriptionHome);

    /**
     * Returns ReadAhead used to control read ahead optimisation during message
     * delivery.
     * 
     * @return ReadAhead
     */
    String getReadAhead();

    /**
     * Sets ReadAhead used to control read ahead optimisation during message
     * delivery.
     * 
     * @param readAhead
     */
    void setReadAhead(String readAhead);

    /**
     * Gets the target.
     * 
     * @return the target
     */
    String getTarget();

    /**
     * Sets the target.
     * 
     * @param target
     *            the target to use
     */
    void setTarget(String target);

    /**
     * Gets the target type.
     * 
     * @return the target type
     */
    String getTargetType();

    /**
     * Set the target type.
     * 
     * @param targetType
     *            the target type to use
     */
    void setTargetType(String targetType);

    /**
     * Gets the target significance.
     * 
     * @return the target significance
     */
    String getTargetSignificance();

    /**
     * Set the target significance.
     * 
     * @param targetSignificance
     *            target significance to use
     */
    void setTargetSignificance(String targetSignificance);

    /**
     * Returns the name of the target inbound transport chain.
     * 
     * @return the target transport chain
     */
    String getTargetTransportChain();

    /**
     * Sets the name of the target inbound transport chain.
     * 
     * @param targetTransportChain
     *            the target transport chain
     */
    void setTargetTransportChain(String targetTransportChain);

    /**
     * Gets the connection name.
     * 
     * @return the connection name
     */
    String getRemoteServerAddress();

    /**
     * Sets the connection Name.
     * 
     * @param conneciton name to use
     */
    void setRemoteServerAddress(String remoteServerAddress);

    /**
     * Gets the target transport
     * 
     * @return the target transport
     */
    public String getTargetTransport();

    /**
     * Sets the target transport
     * 
     * @param targetTransport
     *            the target transport
     */
    public void setTargetTransport(final String targetTransport);

    /**
     * Gets the connection proximity.
     * 
     * @return the connection proximity
     */
    String getConnectionProximity();

    /**
     * Sets the connection proximity.
     * 
     * @param connectionProximity
     *            the connection proximity to use
     */
    void setConnectionProximity(String connectionProximity);

    /**
     * Gets the prefix for temporary queue names.
     * 
     * @return the prefix
     */
    String getTemporaryQueueNamePrefix();

    /**
     * Sets the prefix for temporary queue names.
     * 
     * @param prefix
     *            the prefix
     * @throws JMSException
     *             if the prefix is longer than 12 characters
     */
    void setTemporaryQueueNamePrefix(String prefix) throws JMSException;

    /**
     * Gets the prefix for temporary topic names.
     * 
     * @return the prefix
     */
    String getTemporaryTopicNamePrefix();

    /**
     * Sets the prefix for temporary queue names.
     * 
     * @param prefix
     *            the prefix
     * @throws JMSException
     *             if the prefix is longer than 12 characters
     */
    void setTemporaryTopicNamePrefix(String prefix) throws JMSException;

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
     * Sets the shareDurableSubscription details
     * 
     * @param sharedDurSubs
     *            the new shareDurableSubscription property to use
     */
    void setShareDurableSubscriptions(String sharedDurSubs);

    /**
     * Returns the shareDurableSubscription details
     * 
     * @return the shareDurableSubscription property
     */
    String getShareDurableSubscriptions();

    /**
     * Gets the subscription protocol property.
     * 
     * @return the subscription protocol property
     */
    String getSubscriptionProtocol();

    /**
     * Sets the subscription protocol property.
     * 
     * @param subscriptionProtocol
     *            the subscription protocol property
     */
    void setSubscriptionProtocol(String subscriptionProtocol);

    /**
     * Gets the multicast interface property.
     * 
     * @return the multicast interface property
     */
    String getMulticastInterface();

    /**
     * Sets the multicast interface property.
     * 
     * @param multicastInterface
     *            the multicast interface property
     */
    void setMulticastInterface(String multicastInterface);

    /**
     * Gets the property indicating if the producer will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    String getProducerDoesNotModifyPayloadAfterSet();

    /**
     * Sets the property that indicates if the producer will modify the payload after setting it.
     * 
     * @param propertyValue containing the property value.
     */
    void setProducerDoesNotModifyPayloadAfterSet(String propertyValue);

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
     * Whether to share durable subscription or not
     * 
     * @param shareDurSubs
     */
    void setShareDurableSubscription(Boolean shareDurSubs);
}
