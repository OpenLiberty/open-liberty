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

package com.ibm.ws.sib.api.jms.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.JMSSecurityException;
import javax.jms.JMSSecurityRuntimeException;
import javax.resource.ResourceException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

public class JmsManagedConnectionFactoryImpl implements JmsManagedConnectionFactory, ApiJmsConstants
{
    private static final long serialVersionUID = 2796080016458361701L;

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsManagedConnectionFactoryImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // **************************** STATE VARIABLES ******************************

    /**
     * Reference to the JCA connection factory object.<p>
     * 
     * A jms connection factory is associated with two jca objects.
     * - a jca managed connection factory is used to read/write all a connection
     * factory's properties. No reference is held to the jcamcf here as this is
     * a managed connection factory which is not allowed to directly set its
     * properties.
     * - a jca connection factory is used to obtain connections and read some of
     * the connection factory properties.
     */
    JmsJcaConnectionFactory jcaConnectionFactory = null;

    // ***************************** CONSTRUCTORS ********************************

    /**
     * Constructor that stores a reference to the associated jca connection
     * factory.
     */
    JmsManagedConnectionFactoryImpl(JmsJcaConnectionFactory jcaConnectionFactory) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsManagedConnectionFactoryImpl", jcaConnectionFactory);
        this.jcaConnectionFactory = jcaConnectionFactory;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsManagedConnectionFactoryImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * @see javax.jms.ConnectionFactory#createConnection()
     */
    @Override
    public Connection createConnection() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection");
        Connection connection = createConnection(null, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConnection", connection);
        return connection;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see javax.jms.ConnectionFactory#createConnection(String, String)
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection", new Object[] { userName, (password == null ? "<null>" : "<non-null>") });
        JmsConnectionImpl connection = null;

        JmsJcaConnection jcaConnection = null;

        // create the JCA connection
        try {
            if ((userName == null) && (password == null)) {
                jcaConnection = jcaConnectionFactory.createConnection();
            }
            else {
                jcaConnection = jcaConnectionFactory.createConnection(userName, password);
            }
        } catch (SIAuthenticationException siae) {
            // No FFDC code needed
            // d238447 FFDC review. Not an internal error, no FFDC required.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "AUTHENTICATION_FAILED_CWSIA0009",
                                                                    null,
                                                                    siae,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc
                            );
        } catch (SINotAuthorizedException sinae) {
            // No FFDC code needed
            // d238447 FFDC review. Not an internal error, no FFDC required.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "AUTHORIZATION_FAILED_CWSIA0006",
                                                                    null,
                                                                    sinae,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc
                            );
        } catch (SIException sice) {
            // No FFDC code needed
            // d222942 review - default message ok. Generate FFDC in this case.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0241",
                                                            new Object[] { sice, "JmsManagedConnectionFactoryImpl.createConnection" },
                                                            sice,
                                                            "JmsManagedConnectionFactoryImpl.createConnection#3",
                                                            this,
                                                            tc
                            );
        } catch (ResourceException re) {
            // No FFDC code needed
            // d238447 FFDC review. This should already have generated an FFDC in the lower levels,
            //   so don't generate another one here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "JCA_RESOURCE_EXC_CWSIA0005",
                                                            null,
                                                            re,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        }

        // Create the map containing information that needs to be passed down to
        // the producer and consumer objects, potentially for overriding by the
        // Destination objects.
        // Note that this Map is owned by the connection to which it is about to
        // be passed.
        Map<String, String> passThruProps = new HashMap<String, String>();

        // Properties to pass through are;
        //   ClientID
        //   NonPersistentMapping
        //   PersistentMapping
        //   DurableSubHome
        //   ReadAhead
        //   TempQueueNamePrefix
        //   TempTopicNamePrefix
        //   ShareDurableSubscriptions
        //   ProducerDoesNotModifyPayloadAfterSet (SIB0121)
        //   ConsumerDoesNotModifyPayloadAfterGet (SIB0121)
        //   BusName
        passThruProps.put(JmsraConstants.CLIENT_ID, getClientID());
        passThruProps.put(JmsraConstants.NON_PERSISTENT_MAP, getNonPersistentMapping());
        passThruProps.put(JmsraConstants.PERSISTENT_MAP, getPersistentMapping());
        passThruProps.put(JmsraConstants.READ_AHEAD, getReadAhead());
        passThruProps.put(JmsraConstants.DURABLE_SUB_HOME, getDurableSubscriptionHome());
        passThruProps.put(JmsraConstants.TEMP_QUEUE_NAME_PREFIX, getTemporaryQueueNamePrefix());
        passThruProps.put(JmsraConstants.TEMP_TOPIC_NAME_PREFIX, getTemporaryTopicNamePrefix());
        passThruProps.put(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET, getProducerDoesNotModifyPayloadAfterSet());
        passThruProps.put(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET, getConsumerDoesNotModifyPayloadAfterGet());
        passThruProps.put(JmsInternalConstants.SHARE_DSUBS, getShareDurableSubscriptions());
        passThruProps.put(JmsInternalConstants.BUS_NAME, getBusName());

        // create the JMS connection
        connection = instantiateConnection(jcaConnection, passThruProps);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConnection", connection);
        return connection;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getClientID()
     */
    @Override
    public String getClientID() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getClientID");

        String clientID = jcaConnectionFactory.getClientID();

        // In a managed environment the preceeding get call is likely to return empty string
        // when it actually meant null because of the way it is retrieved from JNDI. This
        // is a particular problem since we will fix the clientID if it is not null.
        if ((clientID != null) && ("".equals(clientID))) {
            // This is empty string - make it null instead.
            clientID = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getClientID", clientID);
        return clientID;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getNonPersistentMapping()
     */
    @Override
    public String getNonPersistentMapping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getNonPersistentMapping");
        String nonPersistentMapping = jcaConnectionFactory.getNonPersistentMapping();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getNonPersistentMapping", nonPersistentMapping);
        return nonPersistentMapping;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#isManaged()
     */
    @Override
    public boolean isManaged() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isManaged");
        boolean isManaged;
        String imP = PrivHelper.getProperty("com.ibm.ws.sib.api.isManaged");

        if (imP == null) {
            // no property available, so take the value from JCA
            isManaged = jcaConnectionFactory.isManaged();
        }
        else {
            // use the property to set a value for isManaged
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "isManaged being overridden by system property: " + imP);
            imP = imP.toUpperCase();
            if (imP.equals("TRUE") || imP.equals("YES")) {
                isManaged = true;
            }
            else {
                isManaged = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isManaged", isManaged);
        return isManaged;
    }

    // ---------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getReadAhead()
     */
    @Override
    public String getReadAhead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReadAhead");
        String ra = jcaConnectionFactory.getReadAhead();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReadAhead", ra);
        return ra;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getDurableSUbscriptionHome()
     */
    @Override
    public String getDurableSubscriptionHome() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDurableSubscriptionHome");
        String dsh = jcaConnectionFactory.getDurableSubscriptionHome();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDurableSubscriptionHome", dsh);
        return dsh;
    }

    /**
     * Get the temp queue name prefix
     * 
     * @return prefix The String prefix set
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getTemporaryQueueNamePrefix
     */
    @Override
    public String getTemporaryQueueNamePrefix() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTemporaryQueueNamePrefix");
        String prefix = jcaConnectionFactory.getTemporaryQueueNamePrefix();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTemporaryQueueNamePrefix", prefix);
        return prefix;
    }

    /**
     * Get the temp topic name prefix
     * 
     * @return prefix The String prefix set
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getTemporaryTopicNamePrefix
     */
    @Override
    public String getTemporaryTopicNamePrefix() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTemporaryTopicNamePrefix");
        String prefix = jcaConnectionFactory.getTemporaryTopicNamePrefix();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTemporaryTopicNamePrefix", prefix);
        return prefix;
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * This method is used to create a JMS Connection object.<p>
     * 
     * This method is overriden by subclasses so that the appropriate connection
     * type is returned whenever this method is called
     * (e.g. from within createConnection()).
     */
    JmsConnectionImpl instantiateConnection(JmsJcaConnection jcaConnection, Map<String, String> _passThruProps) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateConnection", jcaConnection);
        JmsConnectionImpl jmsConnection = new JmsConnectionImpl(jcaConnection, isManaged(), _passThruProps);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateConnection", jmsConnection);
        return jmsConnection;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getPersistentMapping()
     */
    @Override
    public String getPersistentMapping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPersistentMapping");
        String persistentMapping = jcaConnectionFactory.getPersistentMapping();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPersistentMapping", persistentMapping);
        return persistentMapping;
    }

    // ---------------------------------------------------------------------------

    /*
     * default bus name is "DEFAULT", null bus name is not valid.
     */
    @Override
    public String getBusName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBusName");
        String busName = jcaConnectionFactory.getBusName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getBusName", busName);
        return busName;
    }

    // ---------------------------------------------------------------------------

    /*
     * default user name is null.
     */
    @Override
    public String getUserName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUserName");
        String userName = jcaConnectionFactory.getUserName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getUserName", userName);
        return userName;
    }

    /**
     * This method is not added in the interface JmsManagedConnectionFactory since it's for internal use only.
     * 
     * @return
     */
    public String getPassword() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPassword");
        String password = jcaConnectionFactory.getPassword();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPassword");
        return password;
    }

    // ---------------------------------------------------------------------------

    /**
     * Gets the connection proximity
     * 
     * @return The connection proximity
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getConnectionProximity
     */
    @Override
    public String getConnectionProximity() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectionProximity");
        String connectionProximity = jcaConnectionFactory.getConnectionProximity();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnectionProximity", connectionProximity);
        return connectionProximity;
    }

    // ---------------------------------------------------------------------------

    /**
     * Gets the provider endpoints
     * 181802.2
     * 
     * @return The provider endpoints
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getProviderEndpoints
     */
    @Override
    public String getProviderEndpoints() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProviderEndpoints");
        String providerEndpoints = jcaConnectionFactory.getProviderEndpoints();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProviderEndpoints", providerEndpoints);
        return providerEndpoints;
    }

    // ---------------------------------------------------------------------------

    /**
     * Gets the remote protocol
     * 
     * @return The remote protocol
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getRemoteProtocol
     */
    @Override
    public String getTargetTransportChain() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTargetTransportChain");
        String targetTransportChain = jcaConnectionFactory.getTargetTransportChain();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTargetTransportChain", targetTransportChain);
        return targetTransportChain;
    }

    // ---------------------------------------------------------------------------

    /**
     * Gets the target
     * 
     * @return The target
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getTarget
     */
    @Override
    public String getTarget() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTarget");
        String remoteTargetGroup = jcaConnectionFactory.getTarget();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTarget", remoteTargetGroup);
        return remoteTargetGroup;
    }

    // ---------------------------------------------------------------------------

    /**
     * Gets the target type
     * 
     * @return The target type
     * @see com.ibm.websphere.sib.api.jms.JmsConnectionFactory#getTargetType
     */
    @Override
    public String getTargetType() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTargetType");
        String remoteTargetType = jcaConnectionFactory.getTargetType();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTargetType", remoteTargetType);
        return remoteTargetType;
    }

    // ---------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getShareDurableSubscriptions()
     */
    @Override
    public String getShareDurableSubscriptions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getShareDurableSubscriptions");
        String val = jcaConnectionFactory.getShareDurableSubscriptions();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getShareDurableSubscriptions", val);
        return val;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object that) {

        if (this == that)
            return true;
        if (that == null)
            return false;

        // ensure this works for subclasses by using getClass()
        if (this.getClass() != that.getClass())
            return false;

        JmsJcaConnectionFactory thatJcaConnectionFactory = ((JmsManagedConnectionFactoryImpl) that).jcaConnectionFactory;
        if (this.jcaConnectionFactory == thatJcaConnectionFactory)
            return true;

        if (this.jcaConnectionFactory != null)
            return this.jcaConnectionFactory.equals(thatJcaConnectionFactory);

        return false;
    }

    // ---------------------------------------------------------------------------

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return jcaConnectionFactory.hashCode();
    }

    // ---------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsManagedConnectionFactory#getTargetSignificnce()
     */
    @Override
    public String getTargetSignificance() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTargetSignificance");
        String result = jcaConnectionFactory.getTargetSignificance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTargetSignificance", result);
        return result;
    }

    @Override
    public String getMulticastInterface() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMulticastInterface");
        String result = jcaConnectionFactory.getMulticastInterface();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMulticastInterface", result);
        return result;
    }

    @Override
    public String getSubscriptionProtocol() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSubscriptionProtocol");
        String result = jcaConnectionFactory.getSubscriptionProtocol();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSubscriptionProtocol", result);
        return result;
    }

    /**
     * Returns the property indicating if the producer will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getProducerDoesNotModifyPayloadAfterSet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerDoesNotModifyPayloadAfterSet");
        String result = jcaConnectionFactory.getProducerDoesNotModifyPayloadAfterSet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerDoesNotModifyPayloadAfterSet", result);
        return result;
    }

    /**
     * Gets the property indicating if the consumer will modify the payload after getting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getConsumerDoesNotModifyPayloadAfterGet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerDoesNotModifyPayloadAfterGet");
        String result = jcaConnectionFactory.getConsumerDoesNotModifyPayloadAfterGet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerDoesNotModifyPayloadAfterGet", result);
        return result;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createContext()
     */
    @Override
    public JMSContext createContext() throws JMSRuntimeException, JMSSecurityRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createContext");

        JMSContext jmsContext = null;
        try {

            // Pass sessionMode as AUTO_ACKNOWLEDGE. The sessionMode is used to create a session.In simplified
            // API(JMS 2.0), AUTO_ACKNOWLEDGE is the default sessionMode  
            jmsContext = createContext(null, null, JMSContext.AUTO_ACKNOWLEDGE);

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createContext");
        }
        return jmsContext;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createContext(int)
     */
    @Override
    public JMSContext createContext(int sessionMode) throws JMSRuntimeException, JMSSecurityRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createContext", new Object[] { sessionMode });

        JMSContext jmsContext = null;
        try {
            jmsContext = createContext(null, null, sessionMode);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createContext");
        }

        return jmsContext;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createContext(java.lang.String, java.lang.String)
     */
    @Override
    public JMSContext createContext(String userName, String password) throws JMSRuntimeException, JMSSecurityRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createContext", new Object[] { userName, password });

        JMSContext jmsContext = null;
        try {
            // Pass sessionMode as AUTO_ACKNOWLEDGE. The sessionMode is used to create a session.In simplified
            // API(JMS 2.0), AUTO_ACKNOWLEDGE is the default sessionMode 
            jmsContext = createContext(userName, password, JMSContext.AUTO_ACKNOWLEDGE);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createContext");
        }
        return jmsContext;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createContext(java.lang.String, java.lang.String, int)
     */
    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) throws JMSRuntimeException, JMSSecurityRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createContext", new Object[] { userName, password, sessionMode });

        JmsConnectionImpl jmsConnection = null;
        JMSContext jmsContext = null;
        try {

            //CTS test failure was reported when the session mode was passed as -1,  so adding the validation
            switch (sessionMode)
            {
                case JMSContext.AUTO_ACKNOWLEDGE:
                case JMSContext.CLIENT_ACKNOWLEDGE:
                case JMSContext.DUPS_OK_ACKNOWLEDGE:
                case JMSContext.SESSION_TRANSACTED: {
                    break;
                }
                default:
                    throw (JMSRuntimeException) JmsErrorUtils.newThrowable(JMSRuntimeException.class,
                                                                           "INVALID_ACKNOWLEDGE_MODE_CWSIA0514",
                                                                           new Object[] { sessionMode },
                                                                           tc
                                    );
            }

            // Create JMSConnection to pass into JMSContext
            jmsConnection = (JmsConnectionImpl) createConnection(userName, password);
            jmsContext = new JmsJMSContextImpl(jmsConnection, sessionMode, true);
        } catch (JMSSecurityException jmsse) {
            throw (JMSSecurityRuntimeException) JmsErrorUtils.getJMS2Exception(jmsse, JMSSecurityRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createContext", jmsContext);
        }
        return jmsContext;

    }
}
