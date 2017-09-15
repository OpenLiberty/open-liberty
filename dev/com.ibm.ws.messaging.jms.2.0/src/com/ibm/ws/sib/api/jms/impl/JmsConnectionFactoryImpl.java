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

package com.ibm.ws.sib.api.jms.impl;

import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.naming.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsConnectionFactoryImpl extends JmsManagedConnectionFactoryImpl implements JmsConnectionFactory
{
    private static final long serialVersionUID = -869905873329896171L;

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsConnectionFactoryImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // **************************** STATE VARIABLES ******************************

    /**
     * Reference to the JCA connection factory.<p>
     * 
     * A jms connection factory is associated with two jca objects.
     * - a jca managed connection factory is used to read/write all a connection
     * factory's properties. A reference is held to the jcamcf here as this is
     * a non managed connection factory which is allowed to directly set its
     * properties.
     * - a jca connection factory is used to obtain connections and read some of
     * the connection factory properties. A reference to the jcacf is present
     * in the superclass.
     */

    JmsJcaManagedConnectionFactory jcaManagedConnectionFactory = null;

    // ***************************** CONSTRUCTORS ********************************

    /**
     * Constructor that stores a reference to the associated jca connection
     * factory by delegating to the superclass constructor, then stores a
     * reference to the associated jca managed connection factory.
     */
    JmsConnectionFactoryImpl(JmsJcaConnectionFactory jcaConnectionFactory, JmsJcaManagedConnectionFactory jcaManagedConnectionFactory) {

        super(jcaConnectionFactory);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsConnectionFactoryImpl", new Object[] { jcaConnectionFactory, jcaManagedConnectionFactory });

        this.jcaManagedConnectionFactory = jcaManagedConnectionFactory;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsConnectionFactoryImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /*
     * default bus name is "DEFAULT", null bus name is not valid.
     */
    @Override
    public void setBusName(String busName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBusName", busName);

        if (busName == null) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                           InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0261",
                                                                           new Object[] { "busName", null },
                                                                           tc
                            );
        }

        jcaManagedConnectionFactory.setBusName(busName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBusName");
    }

    /*
     * default client id is null.
     */
    @Override
    public void setClientID(String clientID) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setClientID", clientID);

        // Make empty and null look the same.
        if ("".equals(clientID))
            clientID = null;
        jcaManagedConnectionFactory.setClientID(clientID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setClientID");
    }

    /**
     * default non persistent mapping is EXPRESS_NONPERSISTENT, null non persistent mapping is
     * not valid.
     */
    @Override
    public void setNonPersistentMapping(String nonPersistentMapping) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setNonPersistentMapping", nonPersistentMapping);

        if (nonPersistentMapping == null) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "nonPersistentMapping", null },
                                                            tc
                            );
        }

        if (nonPersistentMapping.equals(ApiJmsConstants.MAPPING_BEST_EFFORT_NONPERSISTENT) ||
            nonPersistentMapping.equals(ApiJmsConstants.MAPPING_EXPRESS_NONPERSISTENT) ||
            nonPersistentMapping.equals(ApiJmsConstants.MAPPING_RELIABLE_NONPERSISTENT) ||
            nonPersistentMapping.equals(ApiJmsConstants.MAPPING_RELIABLE_PERSISTENT) ||
            nonPersistentMapping.equals(ApiJmsConstants.MAPPING_ASSURED_PERSISTENT) ||
            nonPersistentMapping.equals(ApiJmsConstants.MAPPING_NONE)) {
            jcaManagedConnectionFactory.setNonPersistentMapping(nonPersistentMapping);
        }
        else if (nonPersistentMapping.equals(ApiJmsConstants.MAPPING_AS_SIB_DESTINATION)) {
            jcaManagedConnectionFactory.setNonPersistentMapping(ApiJmsConstants.MAPPING_NONE);
        }
        else {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "nonPersistentMapping", nonPersistentMapping },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setNonPersistentMapping");
    }

    /**
     * default persistent mapping is RELIABLE_PERSISTENT, null persistent mapping is
     * not valid.
     */
    @Override
    public void setPersistentMapping(String persistentMapping) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPersistentMapping", persistentMapping);

        if (persistentMapping == null) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "persistentMapping", null },
                                                            tc
                            );
        }

        if (persistentMapping.equals(ApiJmsConstants.MAPPING_BEST_EFFORT_NONPERSISTENT) ||
            persistentMapping.equals(ApiJmsConstants.MAPPING_EXPRESS_NONPERSISTENT) ||
            persistentMapping.equals(ApiJmsConstants.MAPPING_RELIABLE_NONPERSISTENT) ||
            persistentMapping.equals(ApiJmsConstants.MAPPING_RELIABLE_PERSISTENT) ||
            persistentMapping.equals(ApiJmsConstants.MAPPING_ASSURED_PERSISTENT) ||
            persistentMapping.equals(ApiJmsConstants.MAPPING_NONE)) {
            jcaManagedConnectionFactory.setPersistentMapping(persistentMapping);
        }
        else if (persistentMapping.equals(ApiJmsConstants.MAPPING_AS_SIB_DESTINATION)) {
            jcaManagedConnectionFactory.setPersistentMapping(ApiJmsConstants.MAPPING_NONE);
        }
        else {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "persistentMapping", persistentMapping },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPersistentMapping");
    }

    /*
     * default password is null.
     */
    @Override
    public void setPassword(String password) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPassword", (password == null ? "null" : "non-null"));
        jcaManagedConnectionFactory.setPassword(password);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPassword");
    }

    /*
     * default user name is null.
     */
    @Override
    public void setUserName(String userName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setUserName", userName);
        jcaManagedConnectionFactory.setUserName(userName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setUserName");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setReadAhead(java.lang.String)
     */
    @Override
    public void setReadAhead(String value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReadAhead", value);

        // Check for null then ensure that the value is one of the permitted constants.
        if ((value != null)
            && ((ApiJmsConstants.READ_AHEAD_DEFAULT.equals(value))
                || (ApiJmsConstants.READ_AHEAD_ON.equals(value))
                || (ApiJmsConstants.READ_AHEAD_OFF.equals(value))
            )) {
            jcaManagedConnectionFactory.setReadAhead(value);
        }
        else {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "readAhead", value },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReadAhead");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setReadAhead(java.lang.String)
     */
    @Override
    public void setDurableSubscriptionHome(String value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDurableSubscriptionHome", value);
        jcaManagedConnectionFactory.setDurableSubscriptionHome(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDurableSubscriptionHome");
    }

    /*
   *
   */
    @Override
    public Reference getReference() throws NamingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReference");
        Reference reference = jcaConnectionFactory.getReference();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReference", reference);
        return reference;
    }

    /*
   *
   */
    @Override
    public void setReference(Reference reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReference", reference);
        jcaConnectionFactory.setReference(reference);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReference");
    }

    /**
     * Set the connection proximity
     * 181802.2
     * 
     * @param newConnectionProximity The connection proximity to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setConnectionProximity(java.lang.String)
     */
    @Override
    public void setConnectionProximity(String newConnectionProximity) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setConnectionProximity", newConnectionProximity);

        // Check for null and empty string, and set to default value if found
        if ((newConnectionProximity == null) || ("".equals(newConnectionProximity))) {
            jcaManagedConnectionFactory.setConnectionProximity(ApiJmsConstants.CONNECTION_PROXIMITY_BUS);
        }
        //ensure that the value is one of the permitted constants - if not throw an exception
        else if ((ApiJmsConstants.CONNECTION_PROXIMITY_BUS.equals(newConnectionProximity))
                 || (ApiJmsConstants.CONNECTION_PROXIMITY_HOST.equals(newConnectionProximity))
                 || (ApiJmsConstants.CONNECTION_PROXIMITY_CLUSTER.equals(newConnectionProximity))
                 || (ApiJmsConstants.CONNECTION_PROXIMITY_SERVER.equals(newConnectionProximity))) {
            jcaManagedConnectionFactory.setConnectionProximity(newConnectionProximity);
        }
        else {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "connectionProximity", newConnectionProximity },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setConnectionProximity");
    }

    /**
     * Set the provider endpoints.<p>
     * 
     * 181802.2
     * 
     * @param newProviderEndpoints The provider endpoints to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setProviderEndpoints(java.lang.String)
     */
    @Override
    public void setProviderEndpoints(String newProviderEndpoints) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProviderEndpoints", newProviderEndpoints);
        jcaManagedConnectionFactory.setRemoteServerAddress(newProviderEndpoints);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProviderEndpoints");
    }

    /**
     * Set the provider endpoints.<p>
     * 
     * 181802.2
     * 
     * @param newProviderEndpoints The provider endpoints to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setProviderEndpoints(java.lang.String)
     */
    @Override
    public void setTargetTransport(String newTargetTransport) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTargetTransport", newTargetTransport);
        jcaManagedConnectionFactory.setTargetTransport(newTargetTransport);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTargetTransport");
    }

    /**
     * Set the remote protocol
     * 181802.2
     * 
     * @param newTargetTransportChain The remote protocol to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setRemoteProtocol(java.lang.String)
     */
    @Override
    public void setTargetTransportChain(String newTargetTransportChain) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTargetTransportChain", newTargetTransportChain);
        jcaManagedConnectionFactory.setTargetTransportChain(newTargetTransportChain);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTargetTransportChain");
    }

    /**
     * Set the target
     * 181802.2
     * 
     * @param newTargetGroup The target to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setTarget(java.lang.String)
     */
    @Override
    public void setTarget(String newTargetGroup) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTarget", newTargetGroup);
        jcaManagedConnectionFactory.setTarget(newTargetGroup);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTarget");
    }

    /**
     * Set the target type
     * 181802.2
     * 
     * @param newTargetType The target type to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setTargetType(java.lang.String)
     */
    @Override
    public void setTargetType(String newTargetType) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTargetType", newTargetType);

        // Check for null and empty string, and set to default value if found
        if ((newTargetType == null) || ("".equals(newTargetType))) {
            jcaManagedConnectionFactory.setTargetType(ApiJmsConstants.TARGET_TYPE_BUSMEMBER);
        }
        // Ensure that the value is one of the permitted constants - thrown an exception if not
        else if ((ApiJmsConstants.TARGET_TYPE_BUSMEMBER.equals(newTargetType))
                 || (ApiJmsConstants.TARGET_TYPE_CUSTOM.equals(newTargetType))
                 || (ApiJmsConstants.TARGET_TYPE_ME.equals(newTargetType))) {
            jcaManagedConnectionFactory.setTargetType(newTargetType);
        }
        else {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "remoteTargetType", newTargetType },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTargetType");
    }

    /**
     * Set the temp queue name prefix
     * 188482
     * 
     * @param prefix The String prefix to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setTemporaryQueueNamePrefix(java.lang.String)
     */
    @Override
    public void setTemporaryQueueNamePrefix(String prefix) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTemporaryQueueNamePrefix", prefix);

        if (prefix != null && !prefix.equals("")) {
            jcaManagedConnectionFactory.setTemporaryQueueNamePrefix(prefix);
        }
        else {
            jcaManagedConnectionFactory.setTemporaryQueueNamePrefix(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTemporaryQueueNamePrefix");
    }

    /**
     * Set the temp topic name prefix
     * 188482
     * 
     * @param prefix The String prefix to use
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setTemporaryTopicNamePrefix(java.lang.String)
     */
    @Override
    public void setTemporaryTopicNamePrefix(String prefix) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTemporaryTopicNamePrefix", prefix);

        if (prefix != null && !prefix.equals("")) {
            jcaManagedConnectionFactory.setTemporaryTopicNamePrefix(prefix);
        }
        else {
            jcaManagedConnectionFactory.setTemporaryTopicNamePrefix(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTemporaryTopicNamePrefix");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setShareDurableSubscriptions(java.lang.String)
     */
    @Override
    public void setShareDurableSubscriptions(String sharePolicy) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setShareDurableSubscriptions", sharePolicy);

        // First of all, convert null and empty to mean the default.
        if ((sharePolicy == null) || ("".equals(sharePolicy))) {
            jcaManagedConnectionFactory.setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_NEVER);
        }
        // Then check for another legal value
        else if ((ApiJmsConstants.SHARED_DSUBS_IN_CLUSTER.equals(sharePolicy)) ||
                 (ApiJmsConstants.SHARED_DSUBS_ALWAYS.equals(sharePolicy)) ||
                 (ApiJmsConstants.SHARED_DSUBS_NEVER.equals(sharePolicy))) {
            jcaManagedConnectionFactory.setShareDurableSubscriptions(sharePolicy);
        }
        else {
            // This is not a legal value.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "shareDurableSubscriptions", sharePolicy },
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setShareDurableSubscriptions");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#setTargetSignificance(java.lang.String)
     */
    @Override
    public void setTargetSignificance(String value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTargetSignificance", value);

        if (ApiJmsConstants.TARGET_SIGNIFICANCE_PREFERRED.equals(value)
            || ApiJmsConstants.TARGET_SIGNIFICANCE_REQUIRED.equals(value)) {
            jcaManagedConnectionFactory.setTargetSignificance(value);
        }
        else {
            // This is not a legal value.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "targetSignificance", value },
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTargetSignificance");
    }

    /**
     * Set network interface to be used for multicast data.
     * 
     * @param mi The IP address of the network interface.
     * @see ApiJmsConstants.MULTICAST_INTERFACE_NONE
     */
    @Override
    public void setMulticastInterface(String mi) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMulticastInterface", mi);
        jcaManagedConnectionFactory.setMulticastInterface(mi);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMulticastInterface");
    }

    /**
     * Set the subscription protocol to be used for the transmission of
     * message data from the ME to the client.
     * 
     * @param the protocol to use.
     * @see ApiJmsConstants.SUBSCRIPTION_PROTOCOL_UNICAST
     * @see ApiJmsConstants.SUBSCRIPTION_PROTOCOL_MULTICAST
     * @see ApiJmsConstants.SUBSCRIPTION_PROTOCOL_UNICAST_AND_MULTICAST
     */
    @Override
    public void setSubscriptionProtocol(String p) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSubscriptionProtocol", p);

        if (ApiJmsConstants.SUBSCRIPTION_PROTOCOL_UNICAST.equals(p) ||
            ApiJmsConstants.SUBSCRIPTION_PROTOCOL_MULTICAST.equals(p) ||
            ApiJmsConstants.SUBSCRIPTION_PROTOCOL_UNICAST_AND_MULTICAST.equals(p)) {
            jcaManagedConnectionFactory.setSubscriptionProtocol(p);
        }
        else {
            // This is not a legal value.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_VALUE_CWSIA0261", new Object[] { "subscriptionProtocol", p }, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSubscriptionProtocol");
    }

    /**
     * Sets the property that indicates if the producer will modify the payload after setting it.
     * 
     * @param propertyValue containing the property value.
     * @throws JMSException In the event of an invalid value
     */
    @Override
    public void setProducerDoesNotModifyPayloadAfterSet(String propertyValue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerDoesNotModifyPayloadAfterSet", propertyValue);

        // First of all, convert null and empty to mean the default.
        if ((propertyValue == null) || ("".equals(propertyValue))) {
            jcaManagedConnectionFactory.setProducerDoesNotModifyPayloadAfterSet(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
        }
        // Check for valid values
        else if (propertyValue.equalsIgnoreCase(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD) ||
                 propertyValue.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD)) {
            jcaManagedConnectionFactory.setProducerDoesNotModifyPayloadAfterSet(propertyValue);
        }
        else {
            // This is not a legal value -> throw a JMS exception
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class, "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "producerDoesNotModifyPayloadAfterSet", propertyValue },
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerDoesNotModifyPayloadAfterSet");
    }

    /**
     * Sets the property that indicates if the consumer will modify the payload after getting it.
     * 
     * @param propertyValue containing the property value.
     * @throws JMSException In the event of an invalid value
     */
    @Override
    public void setConsumerDoesNotModifyPayloadAfterGet(String propertyValue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setConsumerDoesNotModifyPayloadAfterGet", propertyValue);

        // First of all, convert null and empty to mean the default.
        if ((propertyValue == null) || ("".equals(propertyValue))) {
            jcaManagedConnectionFactory.setConsumerDoesNotModifyPayloadAfterGet(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
        }
        // Check for valid values
        else if (propertyValue.equalsIgnoreCase(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD) ||
                 propertyValue.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD)) {
            jcaManagedConnectionFactory.setConsumerDoesNotModifyPayloadAfterGet(propertyValue);
        }
        else {
            // This is not a legal value -> throw a JMS exception
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class, "INVALID_VALUE_CWSIA0261",
                                                            new Object[] { "consumerDoesNotModifyPayloadAfterGet", propertyValue },
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setConsumerDoesNotModifyPayloadAfterGet");
    }
}
