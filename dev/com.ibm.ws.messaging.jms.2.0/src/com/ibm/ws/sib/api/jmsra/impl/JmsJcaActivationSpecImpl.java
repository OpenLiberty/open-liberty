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
package com.ibm.ws.sib.api.jmsra.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.JmsSharedUtils;
import com.ibm.ws.sib.api.jms.impl.JmsManagedConnectionFactoryImpl;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.ra.inbound.SibRaDurableSubscriptionSharing;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.ra.inbound.SibRaMessageDeletionMode;
import com.ibm.ws.sib.ra.inbound.SibRaReadAhead;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * Implementation class for activation specification for JMS resource adapter.
 */
public final class JmsJcaActivationSpecImpl implements JmsJcaActivationSpec,
                SibRaEndpointConfigurationProvider, FFDCSelfIntrospectable {

    /**
     * The resource adapter instance related to this activation specification.
     */
    private transient ResourceAdapter _resourceAdapter;

    /**
     * JNDI name of the destination for lookup.
     * This property may be used to specify the lookup name of an administratively-defined
     * javax.jms.Queue or javax.jms.Topic object which defines the JMS queue or topic
     * from which the endpoint (message-driven bean) is to receive messages.
     * */
    private String _destinationLookup;

    /**
     * JNDI name of the connection factory for lookup.
     * This property may be used to specify the lookup name of an administratively-defined
     * javax.jms.ConnectionFactory, javax.jms.QueueConnectionFactory or javax.jms.TopicConnectionFactory
     * object that will be used to connect to the JMS provider from which the endpoint (message-driven bean) is to receive messages.
     * */
    private String _connectionFactoryLookup;

    /**
     * The user name to connect to the message engine with.
     */
    private String _userName;

    /**
     * The password to connect to the message engine with.
     */
    private String _password;

    /**
     * The destination to receive messages from.
     */
    private Destination _destination;

    /**
     * The name of the destination to receive messages from.
     */
    private String _destinationName;

    /**
     * The name of the home for durable subscriptions.
     */
    private String _durableSubscriptionHome;

    /**
     * The type of the destination to receive messages from. One of
     * <code>javax.jms.Queue</code> or <code>javax.jms.Topic</code>.
     */
    private String _destinationType;

    /**
     * Name of the topic space to use when the destination is of type topic. A null
     * means use the default topic space.
     */
    private String _topicSpace;

    /**
     * The message selector for filtering messages received.
     */
    private String _messageSelector;

    /**
     * The acknowledge mode. One of <code>Auto-acknowledge</code> or
     * <code>Dups-ok-acknowledge</code>.
     */
    private String _acknowledgeMode = DEFAULT_ACKNOWLEDGE_MODE;

    /**
     * The subsciption durability. One of <code>Durable</code> or
     * <code>NonDurable</code>.
     */
    private String _subscriptionDurability = DEFAULT_DURABILITY;

    /**
     * Indicates when sharing of durable subscriptions is permitted. One of:
     * <ul>
     * <li> {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_IN_CLUSTER}-
     * sharing is only permitted when part of a cluster.
     * <li> {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_ALWAYS}-
     * sharing is always permitted.
     * <li> {@link com.ibm.websphere.sib.api.jms.ApiJmsConstants#SHARED_DSUBS_NEVER}-
     * sharing is never permitted.
     * </ul>
     * <p>
     */
    private String _shareDurableSubscriptions = ApiJmsConstants.SHARED_DSUBS_NEVER;

    /**
     * The client identifier.
     */
    private String _clientId;

    /**
     * The subscription name.
     */
    private String _subscriptionName;

    /**
     * The maximum number of messages delivered in a batch to an MDB.
     */
    private Integer _maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    /**
     * The maximum number of concurrent threads for delivering messages.
     */
    private Integer _maxConcurrency = DEFAULT_MAX_CONCURRENCY;

    /**
     * The name of the bus to connect to.
     */
    private String _busName = JmsraConstants.DEFAULT_BUS_NAME;

    /**
     * Flag indicating whether the connection to the messaging engine database
     * should be shared for container managed persistence.
     */
    private Boolean _shareDataSourceWithCMP = Boolean.FALSE;

    /**
     * The name of the target transport chain to use when connecting to a remote
     * messaging engine.
     */
    private String _targetTransportChain;

    /**
     * Property used to control read ahead optimization during message receipt.
     * One of <code>Default</code>,<code>AlwaysOn</code> or
     * <code>AlwaysOff</code>. The default behaviour is to enable the
     * optimization for non-durable subscriptions and unshared durable
     * subscriptions.
     */
    private String _readAhead = ApiJmsConstants.READ_AHEAD_DEFAULT;

    /**
     * The provider endpoints. This is passed to TRM to enable TRM to connect to an
     * ME outside of the local cell.
     */
    private String _providerEndpoints;

    /**
     * The target transport. This is passed to TRM to select ME it has to connect
     */
    private String _targetTransport;

    /**
     * Whether to use the server subject if no credentials are supplied
     */
    private Boolean _useServerSubject = Boolean.FALSE;

    /**
     * The target properties, these three properties work together to specify a target
     * when the RA has to create a remote connection. These are not used when a local
     * connection is created.
     */
    private String _target;
    //since there is 1 ME per liberty profile, making the changes accordingly
    private String _targetType = JmsraConstants.DEFAULT_TARGET_TYPE;
    private String _targetSignificance = JmsraConstants.DEFAULT_TARGET_SIGNIFICANCE;

    /**
     * Flag to indicate if all MDBs in a cluster bus member should be activated or just the MDBs with a
     * running ME in the same server
     */
    private Boolean _alwaysActivateAllMDBs = Boolean.FALSE;

    /**
     * The retry interval to use
     */
    private Integer _retryInterval = DEFAULT_RETRY_INTERVAL;

    /**
     * The failing message delay to use
     */
    private Long _failingMessageDelay = DEFAULT_FAILING_MESSAGE_DELAY;

    /**
     * Constant representing a desination type for queues.
     */
    static final String QUEUE = "javax.jms.Queue";

    /**
     * Constant representing a destination type for topics.
     */
    static final String TOPIC = "javax.jms.Topic";

    /**
     * Constant representing an acknowledge mode for automatic acknowledgement
     * of messages.
     */
    static final String AUTO_ACKNOWLEDGE = "Auto-acknowledge";

    /**
     * Constant representing an acknowledge mode permitting duplicate message
     * delivery.
     */
    static final String DUPS_OK_ACKNOWLEDGE = "Dups-ok-acknowledge";

    /**
     * Constant representing the default acknowledge mode.
     */
    private static final String DEFAULT_ACKNOWLEDGE_MODE = AUTO_ACKNOWLEDGE;

    /**
     * Constant representing a durable subscription type.
     */
    static final String DURABLE = "Durable";

    /**
     * Constant representing a durable shred subscription type.
     */
    static final String DURABLE_SHARED = "DurableShared";

    /**
     * Constant representing a non-durable subscription type.
     */
    static final String NON_DURABLE = "NonDurable";

    /**
     * Constant representing a non-durable subscription type.
     */
    static final String NON_DURABLE_SHARED = "NonDurableShared";

    /**
     * Constant representing the default subscription durability.
     */
    private static final String DEFAULT_DURABILITY = NON_DURABLE;

    /**
     * Constant representing the default maximum number of messages delivered in
     * a batch to an MDB.
     */
    private static final Integer DEFAULT_MAX_BATCH_SIZE = Integer.valueOf(1);

    /**
     * Constant representing the default maximum number of concurrent threads
     * for delivering messages.
     */

    private static final Integer DEFAULT_MAX_CONCURRENCY = Integer.valueOf(5);

    /**
     * Constant representing the default retry interval
     */
    private static final int DEFAULT_RETRY_INTERVAL = 30;

    /**
     * Constant representing the default failing message delay
     */
    private static final long DEFAULT_FAILING_MESSAGE_DELAY = 0;

    /**
     * The MaxSequentialMessageFailure property
     */
    private Integer _maxSequentialMessageFailure = Integer.valueOf(-1);

    /**
     * The AutoStopSequentialMessageFailure property
     */
    private Integer _autoStopSequentialMessageFailure = Integer.valueOf(0);

    /**
     * Property indicating if the consumer will not modify the payload after getting it
     */
    private String _consumerDoesNotModifyPayloadAfterGet = ApiJmsConstants.MIGHT_MODIFY_PAYLOAD;

    /**
     * Property indicating if the forwarder will not modify the payload after getting it
     */
    private String _forwarderDoesNotModifyPayloadAfterSet = ApiJmsConstants.MIGHT_MODIFY_PAYLOAD;

    private static final long serialVersionUID = -7593258810211123494L;

    private static final TraceComponent TRACE = SibTr.register(
                                                               JmsJcaActivationSpecImpl.class, JmsraConstants.MSG_GROUP,
                                                               JmsraConstants.MSG_BUNDLE);

    private static final TraceNLS NLS = TraceNLS
                    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    private static final String CLASS_NAME = JmsJcaActivationSpecImpl.class
                    .getName();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getUserName()
     */
    @Override
    public String getUserName() {
        return _userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setUserName(java.lang.String)
     */
    @Override
    public void setUserName(final String userName) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setUserName", userName);
        }
        _userName = userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getPassword()
     */
    @Override
    public String getPassword() {
        return _password;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setPassword(java.lang.String)
     */
    @Override
    public void setPassword(final String password) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setPassword", "***");
        }
        _password = password;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getDestination()
     */
    @Override
    public Destination getDestination() {
        return _destination;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setDestination(java.lang.String)
     */
    @Override
    public void setDestination(Destination destination) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setDestination", destination);
        }
        _destination = destination;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getDestinationName()
     */
    @Override
    public String getDestinationName() {
        return _destinationName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setDestinationName(java.lang.String)
     */
    @Override
    public void setDestinationName(String destinationName) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setDestinationName", destinationName);
        }
        _destinationName = destinationName;
        dynamicallyCreateDestination();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getTopicSpace()
     */
    public String getTopicSpace() {
        return _topicSpace;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setTopicSpace(java.lang.String)
     */
    public void setTopicSpace(final String topicSpace) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTopicSpace", topicSpace);
        }
        _topicSpace = topicSpace;
        dynamicallyCreateDestination();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getDestinationType()
     */
    @Override
    public String getDestinationType() {
        return _destinationType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setDesinationType(java.lang.String)
     */
    @Override
    public void setDestinationType(final String destinationType) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setDestinationType", destinationType);
        }
        _destinationType = destinationType;
        dynamicallyCreateDestination();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getMessageSelector()
     */
    @Override
    public String getMessageSelector() {
        return _messageSelector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setMessageSelector(java.lang.String)
     */
    @Override
    public void setMessageSelector(final String messageSelector) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setMessageSelector", messageSelector);
        }
        _messageSelector = messageSelector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getAcknowledgeMode()
     */
    @Override
    public String getAcknowledgeMode() {
        return _acknowledgeMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setAcknowledgeMode(java.lang.String)
     */
    @Override
    public void setAcknowledgeMode(final String acknowledgeMode) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setAcknowledgeMode", acknowledgeMode);
        }
        _acknowledgeMode = acknowledgeMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getSubscriptionDurability()
     */
    @Override
    public String getSubscriptionDurability() {
        return _subscriptionDurability;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setSubscriptionDurability(java.lang.String)
     */
    @Override
    public void setSubscriptionDurability(final String subscriptionDurability) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setSubscriptionDurability",
                        subscriptionDurability);
        }
        _subscriptionDurability = subscriptionDurability;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getDurableSubscriptionHome()
     */
    @Override
    public String getDurableSubscriptionHome() {
        return _durableSubscriptionHome;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setDurableSubscriptionHome(java.lang.String)
     */
    @Override
    public void setDurableSubscriptionHome(final String durableSubscriptionHome) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setDurableSubscriptionHome",
                        durableSubscriptionHome);
        }
        _durableSubscriptionHome = durableSubscriptionHome;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getShareDurableSubscriptions()
     */
    @Override
    public String getShareDurableSubscriptions() {
        return _shareDurableSubscriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setShareDurableSubscriptions(java.lang.Boolean)
     */
    @Override
    public void setShareDurableSubscriptions(
                                             final String shareDurableSubscriptions) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "shareDurableSubscriptions",
                        shareDurableSubscriptions);
        }
        _shareDurableSubscriptions = shareDurableSubscriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setShareDurableSubscription(java.lang.Boolean)
     */
    @Override
    public void setShareDurableSubscription(Boolean shareDurSubs) {
        if (shareDurSubs)
            setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_ALWAYS);
        else
            setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_NEVER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getClientId()
     */
    @Override
    public String getClientId() {
        return _clientId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setClientId(java.lang.String)
     */
    @Override
    public void setClientId(final String clientId) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setClientId", clientId);
        }
        _clientId = clientId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getSubscriptionName()
     */
    @Override
    public String getSubscriptionName() {
        return _subscriptionName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setSubscriptionName(java.lang.String)
     */
    @Override
    public void setSubscriptionName(final String subscriptionName) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setSubscriptionName", subscriptionName);
        }
        _subscriptionName = subscriptionName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getMaxBatchSize()
     */
    @Override
    public Integer getMaxBatchSize() {
        return _maxBatchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setMaxBatchSize(java.lang.Integer)
     */
    @Override
    public void setMaxBatchSize(final Integer maxBatchSize) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setMaxBatchSize", maxBatchSize);
        }
        _maxBatchSize = maxBatchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setMaxBatchSize(java.lang.String)
     */
    @Override
    public void setMaxBatchSize(final String maxBatchSize) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setMaxBatchSize", maxBatchSize);
        }
        _maxBatchSize = (maxBatchSize == null ? null : Integer.valueOf(maxBatchSize));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getMaxConcurrency()
     */
    @Override
    public Integer getMaxConcurrency() {
        return _maxConcurrency;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setMaxConcurrency(java.lang.Integer)
     */
    @Override
    public void setMaxConcurrency(final Integer maxConcurrency) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setMaxConcurrency", maxConcurrency);
        }
        _maxConcurrency = maxConcurrency;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setMaxConcurrency(java.lang.String)
     */
    @Override
    public void setMaxConcurrency(final String maxConcurrency) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setMaxConcurrency", maxConcurrency);
        }
        _maxConcurrency = (maxConcurrency == null ? null : Integer.valueOf(maxConcurrency));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getBusName()
     */
    @Override
    public String getBusName() {
        return _busName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setBusName(java.lang.String)
     */
    @Override
    public void setBusName(final String busName) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setBusName", busName);
        }
        _busName = busName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setShareDataSourceWithCMP(java.lang.Boolean)
     */
    @Override
    public void setShareDataSourceWithCMP(final Boolean sharing) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setShareDataSourceWithCMP", sharing);
        }
        _shareDataSourceWithCMP = sharing;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getShareDataSourceWithCMP()
     */
    @Override
    public Boolean getShareDataSourceWithCMP() {
        return _shareDataSourceWithCMP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setTargetTransportChain(java.lang.String)
     */
    @Override
    public void setTargetTransportChain(final String targetTransportChain) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTargetTransportChain",
                        targetTransportChain);
        }
        _targetTransportChain = targetTransportChain;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getTargetTransportChain()
     */
    @Override
    public String getTargetTransportChain() {
        return _targetTransportChain;
    }

    /**
     * Set the useServerSubject property.
     * 
     * @param useServerSubject
     */
    @Override
    public void setUseServerSubject(Boolean useServerSubject) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setUseServerSubject", useServerSubject);
        }
        _useServerSubject = useServerSubject;

    }

    /**
     * Get the useServerSubject property.
     * 
     * @return the useServerSubject property
     */
    @Override
    public Boolean getUseServerSubject() {

        return _useServerSubject;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setReadAhead(java.lang.String)
     */
    @Override
    public void setReadAhead(final String readAhead) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setReadAhead", readAhead);
        }
        _readAhead = readAhead;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#getReadAhead()
     */
    @Override
    public String getReadAhead() {
        return _readAhead;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ResourceAdapterAssociation#getResourceAdapter()
     */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return _resourceAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ResourceAdapterAssociation#setResourceAdapter(javax.resource.spi.ResourceAdapter)
     */
    @Override
    public void setResourceAdapter(final ResourceAdapter resourceAdapter) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setResourceAdapter", resourceAdapter);
        }
        _resourceAdapter = resourceAdapter;
    }

    /**
     * Set the target property.
     * 
     * @param target
     */
    @Override
    public void setTarget(String target) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTarget", target);
        }
        _target = target;

    }

    /**
     * Get the target property.
     * 
     * @return the target property
     */
    @Override
    public String getTarget() {

        return _target;

    }

    /**
     * Set the target type property.
     * 
     * @param targetType
     */
    @Override
    public void setTargetType(String targetType) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTargetType", targetType);
        }
        _targetType = targetType;

    }

    /**
     * Get the target type property.
     * 
     * @return the target type property
     */
    @Override
    public String getTargetType() {

        return _targetType;

    }

    /**
     * Set the target significance property.
     * 
     * @param target significance
     */
    @Override
    public void setTargetSignificance(String targetSignificance) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTargetSignificance", targetSignificance);
        }
        _targetSignificance = targetSignificance;

    }

    /**
     * Get the target significance property.
     * 
     * @return the target significance property
     */
    @Override
    public String getTargetSignificance() {

        return _targetSignificance;

    }

    /**
     * Gets the provider endpoints property
     * 
     * @return the provider endpoints property
     */
    @Override
    public String getProviderEndpoints() {

        return _providerEndpoints;

    }

    /**
     * Set the provider endpoints property
     * 
     * @param providerEndpoints The provider endpoints property to use
     */
    @Override
    public void setProviderEndpoints(String providerEndpoints) {

        _providerEndpoints = providerEndpoints;

    }

    @Override
    public void setRemoteServerAddress(String remoteServerAddress) {
        setProviderEndpoints(remoteServerAddress);

    }

    @Override
    public String getRemoteServerAddress() {
        return getProviderEndpoints();
    }

    /**
     * Gets the target Transport property
     * 
     * @return the target Transport property
     */
    @Override
    public String getTargetTransport() {

        return _targetTransport;

    }

    /**
     * Set the Target Transport property
     * 
     * @param targetTransport The Target Transport property to use
     */
    @Override
    public void setTargetTransport(String targetTransport) {

        _targetTransport = targetTransport;

    }

    /**
     * Get the Max SequentialMessagefailure property
     */
    @Override
    public Integer getMaxSequentialMessageFailure()
    {
        return _maxSequentialMessageFailure;
    }

    /**
     * Set the MaxSequentialMessageFailure property
     * 
     * @param maxSequentialMessageFailure The maximum number of failed messages
     */
    @Override
    public void setMaxSequentialMessageFailure(final Integer maxSequentialMessageFailure)
    {
        _maxSequentialMessageFailure = maxSequentialMessageFailure;
    }

    /**
     * Set the MaxSequentialMessageFailure property
     * 
     * @param maxSequentialMessageFailure The maximum number of failed messages
     */
    @Override
    public void setMaxSequentialMessageFailure(final String maxSequentialMessageFailure)
    {
        _maxSequentialMessageFailure = (maxSequentialMessageFailure == null ? null : Integer.valueOf(maxSequentialMessageFailure));
    }

    /**
     * Get the AutoStop SequentialMessagefailure property
     */
    @Override
    public Integer getAutoStopSequentialMessageFailure()
    {
        return _autoStopSequentialMessageFailure;
    }

    /**
     * Set the AutoStopSequentialMessageFailure property
     * 
     * @param autoStopSequentialMessageFailure The maximum number of failed messages before stopping the MDB
     */
    @Override
    public void setAutoStopSequentialMessageFailure(final Integer autoStopSequentialMessageFailure)
    {
        _autoStopSequentialMessageFailure = autoStopSequentialMessageFailure;
    }

    /**
     * Set the AutoStopSequentialMessageFailure property
     * 
     * @param autoStopSequentialMessageFailure The maximum number of failed messages before stopping the MDB
     */
    @Override
    public void setAutoStopSequentialMessageFailure(final String autoStopSequentialMessageFailure)
    {
        _autoStopSequentialMessageFailure = (autoStopSequentialMessageFailure == null ? null : Integer.valueOf(autoStopSequentialMessageFailure));
    }

    /**
     * Gets the property indicating if the consumer will modify the payload after getting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getConsumerDoesNotModifyPayloadAfterGet()
    {
        return _consumerDoesNotModifyPayloadAfterGet;
    }

    /**
     * Sets the property that indicates if the consumer will modify the payload after getting it.
     * 
     * @param propertyValue containing the property value.
     */
    @Override
    public void setConsumerDoesNotModifyPayloadAfterGet(String propertyValue)
    {
        _consumerDoesNotModifyPayloadAfterGet = propertyValue;
    }

    /**
     * Gets the property indicating if the forwarder will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getForwarderDoesNotModifyPayloadAfterSet()
    {
        return _forwarderDoesNotModifyPayloadAfterSet;
    }

    /**
     * Sets the property that indicates if the forwarder will modify the payload after setting it.
     * 
     * @param propertyValue containing the property value.
     */
    @Override
    public void setForwarderDoesNotModifyPayloadAfterSet(String propertyValue)
    {
        _forwarderDoesNotModifyPayloadAfterSet = propertyValue;
    }

    /**
     * Sets the activate all MDBs in a cluster bus member property.
     */
    @Override
    public void setAlwaysActivateAllMDBs(Boolean alwaysActivateAllMDBs)
    {
        _alwaysActivateAllMDBs = alwaysActivateAllMDBs;
    }

    /**
     * Gets the activate all MDBs in a cluster bus member property.
     */
    @Override
    public Boolean getAlwaysActivateAllMDBs()
    {
        return _alwaysActivateAllMDBs;
    }

    /**
     * Sets the retry interval
     */
    @Override
    public void setRetryInterval(final Integer retryInterval)
    {
        _retryInterval = retryInterval;
    }

    /**
     * Sets the retry interval
     */
    @Override
    public void setRetryInterval(final String retryInterval)
    {
        _retryInterval = (retryInterval == null ? null : Integer.valueOf(retryInterval));
    }

    /**
     * Gets the retry interval
     */
    @Override
    public Integer getRetryInterval()
    {
        return _retryInterval;
    }

    /**
     * Gets the failing message delay time
     * 
     * @return the failing message delay
     */
    @Override
    public Long getFailingMessageDelay()
    {
        return _failingMessageDelay;
    }

    /**
     * GSts the failing message delay time
     * 
     * @param delay the failing message delay
     */
    @Override
    public void setFailingMessageDelay(Long delay)
    {
        _failingMessageDelay = delay;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ActivationSpec#validate()
     */
    @Override
    public void validate() throws InvalidPropertyException {

        final String methodName = "validate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final List<String> failedPropsMessages = new ArrayList<String>();

        try {
            final List<PropertyDescriptor> invalidProperties = new ArrayList<PropertyDescriptor>();

            // Busname is required
            if ((null == _busName) || ("".equals(_busName))) {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE, "Invalid BusName - BusName was "
                                             + _busName);
                }
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_BUSNAME_CWSJR1187"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor("busName",
                                JmsJcaActivationSpec.class));
            }

            // Destination required
            if (null == _destination) {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Invalid Destination - Destination was null");
                }
                failedPropsMessages.add(NLS.getFormattedMessage("INVALID_PROPERTY_DESTINATION_CWSJR1188", null, null));
                invalidProperties.add(new PropertyDescriptor("destination",
                                JmsJcaActivationSpec.class));
            }

            // Check destination type valid
            if (!(QUEUE.equals(_destinationType) || TOPIC
                            .equals(_destinationType))) {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Invalid Destination Type - We expected a destination type of "
                                                + QUEUE + " or " + TOPIC + " but we got "
                                                + _destinationType);
                }
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_DESTINATION_TYPE_CWSJR1189"),
                                                                new Object[] { QUEUE, TOPIC, _destinationType }, null));
                invalidProperties.add(new PropertyDescriptor("destinationType",
                                JmsJcaActivationSpec.class));
            }

            // Check acknowledge mode valid
            if (!(AUTO_ACKNOWLEDGE.equals(_acknowledgeMode) || DUPS_OK_ACKNOWLEDGE
                            .equals(_acknowledgeMode))) {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Invalid Acknowledge Mode - We expected an acknowledge mode of "
                                                + AUTO_ACKNOWLEDGE + " or "
                                                + DUPS_OK_ACKNOWLEDGE + " but we got "
                                                + _acknowledgeMode);
                }
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_ACKNOWLEDGE_MODE_CWSJR1190"),
                                                                new Object[] { AUTO_ACKNOWLEDGE, DUPS_OK_ACKNOWLEDGE, _acknowledgeMode },
                                                                null));
                invalidProperties.add(new PropertyDescriptor("acknowledgeMode",
                                JmsJcaActivationSpec.class));
            }

            // Check read ahead valid
            if (!(ApiJmsConstants.READ_AHEAD_DEFAULT.equals(_readAhead)
                  || ApiJmsConstants.READ_AHEAD_ON.equals(_readAhead) || ApiJmsConstants.READ_AHEAD_OFF
                            .equals(_readAhead))) {

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Invalid ReadAhead - We expected a read ahead of "
                                                + ApiJmsConstants.READ_AHEAD_DEFAULT + ", "
                                                + ApiJmsConstants.READ_AHEAD_ON + " or "
                                                + ApiJmsConstants.READ_AHEAD_OFF
                                                + " but we got " + _readAhead);
                }
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_READ_AHEAD_CWSJR1191"),
                                                                new Object[] { ApiJmsConstants.READ_AHEAD_DEFAULT,
                                                                              ApiJmsConstants.READ_AHEAD_ON,
                                                                              ApiJmsConstants.READ_AHEAD_OFF,
                                                                              _acknowledgeMode },
                                                                null));
                invalidProperties.add(new PropertyDescriptor("readAhead",
                                JmsJcaActivationSpec.class));
            }

            // For queues ...
            if (QUEUE.equals(_destinationType)) {
                if (!(_destination instanceof JmsQueue)) {
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE,
                                    "Invalid Destination - We expect a destination object to be a "
                                                    + JmsQueue.class.getName()
                                                    + " but we got "
                                                    + _destination == null ? "null" : _destination.getClass().getName());
                    }
                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_QUEUE_CWSJR1192"),
                                                                    new Object[] { JmsQueue.class.getName(), _destination == null ? "null" : _destination.getClass().getName() },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor("destination",
                                    JmsJcaActivationSpec.class));
                }
            }

            // For topics ...
            if (TOPIC.equals(_destinationType)) {

                if (!(_destination instanceof JmsTopic)) {
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE,
                                    "Invalid Destination - We expect a destination object to be a "
                                                    + JmsTopic.class.getName()
                                                    + " but we got "
                                                    + _destination == null ? "null" : _destination.getClass().getName());
                    }
                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_TOPIC_CWSJR1193"),
                                                                    new Object[] { JmsTopic.class.getName(), _destination == null ? "null" : _destination.getClass().getName() },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor("destination",
                                    JmsJcaActivationSpec.class));
                }

                // ... check subscription durability valid
                if (!(DURABLE.equals(_subscriptionDurability) || DURABLE_SHARED.equals(_subscriptionDurability) ||
                      NON_DURABLE.equals(_subscriptionDurability) || NON_DURABLE_SHARED.equals(_subscriptionDurability))) {
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE,
                                    "Invalid subscriptionDurability - We expect a value of "
                                                    + DURABLE + " or " + DURABLE_SHARED + " or " + NON_DURABLE + " or " + NON_DURABLE_SHARED
                                                    + " but we got "
                                                    + _subscriptionDurability);
                    }
                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_SUBDUR_CWSJR1194"),
                                                                    new Object[] { DURABLE, DURABLE_SHARED, NON_DURABLE, NON_DURABLE_SHARED, _subscriptionDurability },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "subscriptionDurability",
                                    JmsJcaActivationSpec.class));
                }

                if (DURABLE.equals(_subscriptionDurability)) {
                    // ... check client ID set
                    if ((null == _clientId) || ("".equals(_clientId))) {
                        failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_CLIENTID_CWSJR1183"),
                                                                        null, null));
                        invalidProperties.add(new PropertyDescriptor(
                                        "clientId", JmsJcaActivationSpec.class));
                    }

                }
                // For durable(and shared non durable) topic subscriptions ...
                if (DURABLE.equals(_subscriptionDurability) ||
                    DURABLE_SHARED.equals(_subscriptionDurability) ||
                    NON_DURABLE_SHARED.equals(_subscriptionDurability)) {
                    // ... check client ID set
                    if ((null == _clientId) || ("".equals(_clientId))) {
                        failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_CLIENTID_CWSJR1183"),
                                                                        null, null));
                        invalidProperties.add(new PropertyDescriptor(
                                        "clientId", JmsJcaActivationSpec.class));
                    }

                    // durableSubscriptionHome is populated at the connectionImpl 
                    // or the ConnectionProxy for liberty.Hence the check is removed

                    // ... check subscription name set
                    if ((null == _subscriptionName)
                        || ("".equals(_subscriptionName))) {

                        failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_SUBNAME_CWSJR1196"),
                                                                        null, null));
                        invalidProperties
                                        .add(new PropertyDescriptor("subscriptionName",
                                                        JmsJcaActivationSpec.class));

                    }

                }
            }

            // Check maximum concurrency greater than zero
            if ((null == _maxConcurrency) || (_maxConcurrency.intValue() < 1)) {
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_MAXCONC_CWSJR1198"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor("maxConcurrency",
                                JmsJcaActivationSpec.class));
            }

            // Check maximum batch size greater than zero
            if ((null == _maxBatchSize) || (_maxBatchSize.intValue() < 1)) {
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_MAXBATCH_CWSJR1199"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor("maxBatchSize",
                                JmsJcaActivationSpec.class));
            }

            // Check share data source with CMP is set - since this defaults to a value at
            // initialisation time this should never be encountered
            if (null == _shareDataSourceWithCMP) {
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_SHARECMP_CWSJR1200"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor(
                                "shareDataSourceWithCMP", JmsJcaActivationSpec.class));
            }

            /**
             * For the target set of properties, use the defaults if none specified
             */
            if ("".equals(_target)) {

                _target = JmsraConstants.DEFAULT_TARGET;

            }

            // Target type must be one of the predefined values
            if ((null == _targetType) || ("".equals(_targetType))) {

                _targetType = JmsraConstants.DEFAULT_TARGET_TYPE;

            } else {

                if (!((ApiJmsConstants.TARGET_TYPE_BUSMEMBER.equals(_targetType)) ||
                      (ApiJmsConstants.TARGET_TYPE_ME.equals(_targetType)) || (ApiJmsConstants.TARGET_TYPE_CUSTOM.equals(_targetType)))) {

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_TARGET_TYPE_CWSJR1180"),
                                                                    new Object[] { ApiJmsConstants.TARGET_TYPE_BUSMEMBER,
                                                                                  ApiJmsConstants.TARGET_TYPE_ME,
                                                                                  ApiJmsConstants.TARGET_TYPE_CUSTOM,
                                                                                  _targetType },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "targetType",
                                    JmsJcaActivationSpec.class));

                }

            }

            // Target significance must be one of the predefined values
            if ((null == _targetSignificance) || ("".equals(_targetSignificance))) {

                _targetSignificance = JmsraConstants.DEFAULT_TARGET_SIGNIFICANCE;

            } else {

                if (!((ApiJmsConstants.TARGET_SIGNIFICANCE_PREFERRED.equals(_targetSignificance)) || (ApiJmsConstants.TARGET_SIGNIFICANCE_REQUIRED.equals(_targetSignificance)))) {

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_TARGET_SIGNIFICANCE_CWSJR1179"),
                                                                    new Object[] { ApiJmsConstants.TARGET_SIGNIFICANCE_PREFERRED,
                                                                                  ApiJmsConstants.TARGET_SIGNIFICANCE_REQUIRED,
                                                                                  _targetSignificance },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "targetSignificance",
                                    JmsJcaActivationSpec.class));

                }

            }

            // If we have any invalid properties, throw an
            // InvalidPropertyException
            if (!invalidProperties.isEmpty()) {

                final PropertyDescriptor[] invalidPropertyArray = invalidProperties
                                .toArray(new PropertyDescriptor[invalidProperties
                                                .size()]);
                // Commented out in 284561 since we are now using failedPropsMessages instead
/*
 * final List propertyNames = new ArrayList();
 * 
 * for (int i = 0; i < invalidPropertyArray.length; i++) {
 * propertyNames.add(invalidPropertyArray[i].getDisplayName());
 * }
 */

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE, "Invalid properties found:",
                                failedPropsMessages);
                }

                final InvalidPropertyException exc = new InvalidPropertyException(
                                NLS.getFormattedMessage(
                                                        ("INVALID_PROPERTIES_CWSJR1181"),
                                                        new Object[] { failedPropsMessages }, null));
                exc.setInvalidPropertyDescriptors(invalidPropertyArray);
                throw exc;
            }

        } catch (final IntrospectionException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, FFDC_PROBE_1, this);

            SibTr.exception(this, TRACE, exception);

            throw new RuntimeException(NLS.getFormattedMessage(
                                                               ("EXCEPTION_RECEIVED_CWSJR1185"),
                                                               new Object[] { exception }, null), exception);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    @Override
    public String toString() {

        final StringBuffer buffer = new StringBuffer("[");
        buffer.append(getClass().getName());
        buffer.append("@");
        buffer.append(System.identityHashCode(this));
        buffer.append(" <userName=");
        buffer.append(_userName);
        buffer.append("> <password=");
        buffer.append((_password == null) ? null : "*****");
        buffer.append("> <destination=");
        buffer.append(_destination);
        buffer.append("> <durableSubscriptionHome=");
        buffer.append(_durableSubscriptionHome);
        buffer.append("> <destinationType=");
        buffer.append(_destinationType);
        buffer.append("> <messageSelector=");
        buffer.append(_messageSelector);
        buffer.append("> <acknowledgeMode=");
        buffer.append(_acknowledgeMode);
        buffer.append("> <subscriptionDurability=");
        buffer.append(_subscriptionDurability);
        buffer.append("> <shareDurableSubscriptions=");
        buffer.append(_shareDurableSubscriptions);
        buffer.append("> <clientId=");
        buffer.append(_clientId);
        buffer.append("> <subscriptionName=");
        buffer.append(_subscriptionName);
        buffer.append("> <maxBatchSize=");
        buffer.append(_maxBatchSize);
        buffer.append("> <maxConcurrency=");
        buffer.append(_maxConcurrency);
        buffer.append("> <busName=");
        buffer.append(_busName);
        buffer.append("> <shareDataSourceWithCMP=");
        buffer.append(_shareDataSourceWithCMP);
        buffer.append("> <targetTransportChain=");
        buffer.append(_targetTransportChain);
        buffer.append("> <readAhead=");
        buffer.append(_readAhead);
        buffer.append("> <target=");
        buffer.append(_target);
        buffer.append("> <targetType=");
        buffer.append(_targetType);
        buffer.append("> <targetSignificance=");
        buffer.append(_targetSignificance);
        buffer.append("> <providerEndpoints=");
        buffer.append(_providerEndpoints);
        buffer.append("> <targetTransport=");
        buffer.append(_targetTransport);
        buffer.append("> <consumerDoesNotModifyPayloadAfterGet=");
        buffer.append(_consumerDoesNotModifyPayloadAfterGet);
        buffer.append("> <forwarderDoesNotModifyPayloadAfterSet=");
        buffer.append(_forwarderDoesNotModifyPayloadAfterSet);
        buffer.append("> <alwaysActivateAllMDBs=");
        buffer.append(_alwaysActivateAllMDBs);
        buffer.append("> <retryInterval=");
        buffer.append(_retryInterval);
        buffer.append("> <failingMessageDelay=");
        buffer.append(_failingMessageDelay);
        buffer.append("> <useServerSubject=");
        buffer.append(_useServerSubject);
        buffer.append("> <topicSpace=");
        buffer.append(_topicSpace);
        buffer.append("> <destinationLookup=");
        buffer.append(_destinationLookup);
        buffer.append("> <connectionFactoryLookup=");
        buffer.append(_connectionFactoryLookup);
        buffer.append(">]");

        return buffer.toString();

    }

    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider#getEndpointInvoker()
     */
    @Override
    public SibRaEndpointInvoker getEndpointInvoker()
                    throws ResourceAdapterInternalException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.entry(this, TRACE, "getEndpointInvoker");

        // Create a Map of properties to pass into the new endpoint invoker. These are admin props that are
        // relevant in other classes so need to be passed through... NOTE: 'forwarder does not modify...'
        // is synonymous with 'producer does not modify...', so translate the forwarder property here to make
        // payload handling for incoming messages simpler
        Map<String, String> passThruProps = new HashMap<String, String>();
        passThruProps.put(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET, getConsumerDoesNotModifyPayloadAfterGet());
        passThruProps.put(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET, getForwarderDoesNotModifyPayloadAfterSet());

        // Create a new endpoint invoker, passing in the pass thru props
        JmsJcaEndpointInvokerImpl epInvoker = new JmsJcaEndpointInvokerImpl(passThruProps);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.exit(this, TRACE, "getEndpointInvoker", epInvoker);

        return epInvoker;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider#getEndpointConfiguration()
     */
    @Override
    public SibRaEndpointConfiguration getEndpointConfiguration()
                    throws InvalidPropertyException, ResourceAdapterInternalException {

        final String methodName = "getEndpointConfiguration";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final SibRaEndpointConfiguration configuration = new JmsJcaEndpointConfigurationImpl();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, configuration);
        }
        return configuration;

    }

    // When a destination name rather than an administered object destination is provided we need to dynamically
    // create an administered object using the destination name and type when known. To cater for subsequent
    // changes in the destinationName or destinationType the destination is always recreated if one of these
    // changes

    private void dynamicallyCreateDestination() {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.entry(this, TRACE, "dynamicallyCreateDestination");

        if (_destination != null)
            return;

        // Can we create a new destination object?

        if (_destinationType != null && _destinationName != null) {
            try {
                if (_destinationType.equals(QUEUE)) {
                    _destination = com.ibm.websphere.sib.api.jms.JmsFactoryFactory.getInstance().createQueue(_destinationName);
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        SibTr.debug(this, TRACE, "Dynamically created queue:" + _destination);
                } else if (_destinationType.equals(TOPIC)) {
                    _destination = com.ibm.websphere.sib.api.jms.JmsFactoryFactory.getInstance().createTopic(_destinationName);
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        SibTr.debug(this, TRACE, "Dynamically created topic:" + _destination);
                }
            } catch (javax.jms.JMSException e) {
                FFDCFilter.processException(e, CLASS_NAME, FFDC_PROBE_4);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                    SibTr.exception(this, TRACE, e);
            }
        }

        // If we have a destination object do we need to set additional properties on it?

        if (_destination != null) {
            try {
                if (_destinationType.equals(TOPIC)) {
                    if (_topicSpace != null) {
                        ((JmsTopic) _destination).setTopicSpace(_topicSpace);
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                            SibTr.debug(this, TRACE, "Set topicSpace on destination");
                    }
                }
            } catch (javax.jms.JMSException e) {
                FFDCFilter.processException(e, CLASS_NAME, FFDC_PROBE_5);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                    SibTr.exception(this, TRACE, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.exit(this, TRACE, "dynamicallyCreateDestination");
    }

    /**
     * Inner class mapping the properties of this activation specification to
     * those required by the <code>SibRaEndpointActivation</code> interface.
     */
    private class JmsJcaEndpointConfigurationImpl implements
                    SibRaEndpointConfiguration {

        private final SIDestinationAddressFactory _destinationAddressFactory;

        private final JmsSharedUtils _jmsUtils;

        /**
         * Constructor. Does everything up front that might throw an exception.
         * 
         * @throws InvalidPropertyException
         *             if the configuration is not valid
         * @throws ResourceAdapterInternalException
         *             if we cannot obtain a destination address factory of JMS
         *             utilities
         */
        private JmsJcaEndpointConfigurationImpl()
            throws InvalidPropertyException,
            ResourceAdapterInternalException {

            validate();

            try {

                _destinationAddressFactory = JmsServiceFacade.getSIDestinationAddressFactory();
                _jmsUtils = JmsInternalsFactory.getSharedUtils();

            } catch (final Exception exception) {

                FFDCFilter
                                .processException(exception, CLASS_NAME
                                                             + ".JmsJcaEndpointConfigurationImpl."
                                                             + "JmsJcaEndpointConfigurationImpl",
                                                  FFDC_PROBE_2, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1182"),
                                                     new Object[] { exception }, null), exception);

            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestination()
         */
        @Override
        public SIDestinationAddress getDestination() {

            final JmsDestination jmsDestination = (JmsDestination) _destination;

            // If we are a JMS Queue then obtain the "scope to local queue point" property
            // and pass that to the create of the SIDestinationAddress, if we are not
            // a JMS queue then use the original method call.
            SIDestinationAddress addr = null;
            if (jmsDestination instanceof JmsQueue)
            {
                JmsQueue q = (JmsQueue) jmsDestination;
                String scopeToLocal = q.getScopeToLocalQP();
                boolean scope = (ApiJmsConstants.SCOPE_TO_LOCAL_QP_ON.equals(scopeToLocal));
                addr = _destinationAddressFactory.createSIDestinationAddress(
                                                                             jmsDestination.getDestName(), scope,
                                                                             jmsDestination.getBusName());
            }
            else
            {
                addr = _destinationAddressFactory.createSIDestinationAddress(
                                                                             jmsDestination.getDestName(), jmsDestination.getBusName());
            }

            return addr;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getAllowMessageGathering()
         */
        @Override
        public String getAllowMessageGathering()
        {
            // if we are a JMS Queue obtain the "gatherMessages" property and return it, if not
            // return null.
            final JmsDestination jmsDestination = (JmsDestination) _destination;
            String allow = null;
            if (jmsDestination instanceof JmsQueue)
            {
                JmsQueue q = (JmsQueue) jmsDestination;
                allow = q.getGatherMessages();
            }
            return allow;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestinationName()
         */
        @Override
        public String getDestinationName()
        {
            return _destinationName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestinationType()
         */
        @Override
        public DestinationType getDestinationType() {

            final DestinationType destinationType;
            if (QUEUE.equals(_destinationType)) {
                destinationType = DestinationType.QUEUE;
            } else {
                destinationType = DestinationType.TOPICSPACE;
            }
            return destinationType;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDiscriminator()
         */
        @Override
        public String getDiscriminator() {

            final JmsDestination jmsDestination = (JmsDestination) _destination;
            return jmsDestination.getDestDiscrim();

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMaxConcurrency()
         */
        @Override
        public int getMaxConcurrency() {
            return _maxConcurrency.intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMaxBatchSize()
         */
        @Override
        public int getMaxBatchSize() {
            return _maxBatchSize.intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getBusName()
         */
        @Override
        public String getBusName() {
            return _busName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getUserName()
         */
        @Override
        public String getUserName() {
            return _userName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getPassword()
         */
        @Override
        public String getPassword() {
            return _password;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMessageSelector()
         */
        @Override
        public String getMessageSelector() {
            return _messageSelector;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDurableSubscriptionName()
         */
        @Override
        public String getDurableSubscriptionName() {
            return _jmsUtils
                            .getCoreDurableSubName(_clientId, _subscriptionName);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDurableSubscriptionHome()
         */
        @Override
        public String getDurableSubscriptionHome() {
            return _durableSubscriptionHome;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getShareDurableSubscriptions()
         */
        @Override
        public SibRaDurableSubscriptionSharing getShareDurableSubscriptions() {

            final SibRaDurableSubscriptionSharing sharing;
            if (ApiJmsConstants.SHARED_DSUBS_ALWAYS
                            .equals(_shareDurableSubscriptions)) {
                sharing = SibRaDurableSubscriptionSharing.ALWAYS;
            } else if (ApiJmsConstants.SHARED_DSUBS_NEVER
                            .equals(_shareDurableSubscriptions)) {
                sharing = SibRaDurableSubscriptionSharing.NEVER;
            } else {
                sharing = SibRaDurableSubscriptionSharing.CLUSTER_ONLY;
            }
            return sharing;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMessageDeletionMode()
         */
        @Override
        public SibRaMessageDeletionMode getMessageDeletionMode() {

            final SibRaMessageDeletionMode messageDeletionMode;
            if (AUTO_ACKNOWLEDGE.equals(_acknowledgeMode)) {
                messageDeletionMode = SibRaMessageDeletionMode.SINGLE;
            } else {
                messageDeletionMode = SibRaMessageDeletionMode.BATCH;
            }
            return messageDeletionMode;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#isDurableSubscription()
         */
        @Override
        public boolean isDurableSubscription() {

            return ((TOPIC.equals(_destinationType)) && (DURABLE.equalsIgnoreCase(_subscriptionDurability) || DURABLE_SHARED.equalsIgnoreCase(_subscriptionDurability)));

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getSubscriptionDurablity()
         */
        @Override
        public String getSubscriptionDurability() {

            return _subscriptionDurability;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getSelectorDomain()
         */
        @Override
        public SelectorDomain getSelectorDomain() {

            return SelectorDomain.JMS;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getShareDataSourceWithCMP()
         */
        @Override
        public boolean getShareDataSourceWithCMP() {
            return _shareDataSourceWithCMP.booleanValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetTransportChain()
         */
        @Override
        public String getTargetTransportChain() {
            return _targetTransportChain;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTarget()
         */
        @Override
        public String getTarget() {
            return _target;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetType()
         */
        @Override
        public String getTargetType() {
            return _targetType;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetSignificance()
         */
        @Override
        public String getTargetSignificance() {
            return _targetSignificance;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getReadAhead()
         */
        @Override
        public SibRaReadAhead getReadAhead() {

            final SibRaReadAhead readAhead;
            if (ApiJmsConstants.READ_AHEAD_ON.equals(_readAhead)) {
                readAhead = SibRaReadAhead.ON;
            } else if (ApiJmsConstants.READ_AHEAD_OFF.equals(_readAhead)) {
                readAhead = SibRaReadAhead.OFF;
            } else {
                readAhead = SibRaReadAhead.DEFAULT;
            }
            return readAhead;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getProviderEndpoints()
         */
        @Override
        public String getProviderEndpoints() {
            return _providerEndpoints;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getActivationSpec()
         */
        @Override
        public ActivationSpec getActivationSpec() {
            return JmsJcaActivationSpecImpl.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getSequentialMessageFailure()
         */
        @Override
        public int getMaxSequentialMessageFailure()
        {
            return _maxSequentialMessageFailure;
        }

        /**
         * Get the AutoStop SequentialMessagefailure property
         */
        @Override
        public int getAutoStopSequentialMessageFailure()
        {
            return _autoStopSequentialMessageFailure;
        }

        @Override
        public String toString() {

            final StringBuffer buffer = new StringBuffer("[");
            buffer.append(getClass().getName());
            buffer.append("@");
            buffer.append(System.identityHashCode(this));
            buffer.append(" <JmsJcaActivationSpecImpl.this=");
            buffer.append(JmsJcaActivationSpecImpl.this);
            buffer.append(">]");

            return buffer.toString();

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#isJMSRa()
         */
        @Override
        public boolean isJMSRa() {

            return true;

        }

        /**
         * Gets the flag indicating if all MDBs in a cluster bus member should be
         * activated
         */
        @Override
        public Boolean getAlwaysActivateAllMDBs()
        {
            return _alwaysActivateAllMDBs;
        }

        /**
         * Gets the retry interval
         */
        @Override
        public int getRetryInterval()
        {
            return _retryInterval;
        }

        /**
         * This parameter is only used for core SPI MDB's, so we return a hardcored value of false.
         * 
         * @return False, this flag is not used for JMS MDBs
         */
        @Override
        public Boolean getUseDestinationWildcard()
        {
            return Boolean.FALSE;
        }

        @Override
        public boolean getUseServerSubject()
        {
            return _useServerSubject;
        }

        /**
         * Gets the failing message delay time
         * 
         * @return the failing message delay
         */
        @Override
        public Long getFailingMessageDelay()
        {
            return _failingMessageDelay;
        }

        /** {@inheritDoc} */
        @Override
        public String getTargetTransport() {
            return _targetTransport;
        }
    }

    /**
     * @return the _destinationLookup - the JNDI name of the Destination which needs to be dynamically looked upon.
     */
    public String getDestinationLookup() {
        return _destinationLookup;
    }

    /**
     * This method does lookup of {@link Destination} based on the given JNDI and sets into this.
     * 
     * @param _destinationLookup - the JNDI name of the Destination which needs to be dynamically looked upon.
     */
    /** {@inheritDoc} */
    @Override
    public void setDestinationLookup(String _destinationLookup) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.entry(this, TRACE, "setDestinationLookup", _destinationLookup);
        }
        if (!isValueCanBeSet(_destinationLookup)) {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, " destinationLookup property value is "
                                         + _destinationLookup + ", so ignoring");
            }

            return;
        }
        this._destinationLookup = _destinationLookup;

        try {
            this._destination = (Destination) new InitialContext().lookup(_destinationLookup);
        } catch (NamingException e) {
            //TODo - Do we need to just log the warning or throw the error to the user?
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Invalid Destination JNDI name "
                                         + _destinationLookup);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.exit(this, TRACE, "setDestinationLookup", _destinationLookup);
        }
    }

    /**
     * 
     * @return the _connectionFactoryLookup - the JNDI name of the Connection Factory which needs to be dynamically looked upon.
     */
    public String getConnectionFactoryLookup() {
        return _connectionFactoryLookup;
    }

    /**
     * 
     * This method gets the properties of administraively configured ConnectionFactory and sets in this object's respective properties.
     * Note: Few setter methods are commented in here as they are unmappable.
     * TODO: Check if the property is non-null/non-empty, then set?
     * 
     * @param _connectionFactoryLookup - the JNDI name of the Connection Factory which needs to be dynamically looked upon.
     */
    /** {@inheritDoc} */
    @Override
    public void setConnectionFactoryLookup(String _connectionFactoryLookup) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.entry(this, TRACE, "setConnectionFactoryLookup", _connectionFactoryLookup);
        }

        this._connectionFactoryLookup = _connectionFactoryLookup;
        JmsManagedConnectionFactoryImpl jmsManagedCF = null;
        try {
            Object object = new InitialContext().lookup(_connectionFactoryLookup);

            if (object instanceof JmsManagedConnectionFactoryImpl) {

                jmsManagedCF = (JmsManagedConnectionFactoryImpl) object;

                if (isValueCanBeSet(jmsManagedCF.getUserName()))
                    this.setUserName(jmsManagedCF.getUserName());

                if (isValueCanBeSet(jmsManagedCF.getPassword()))
                    this.setPassword(jmsManagedCF.getPassword());
                //TODO: Do we need to really set this, since we always use defaultBus?
                if (isValueCanBeSet(jmsManagedCF.getBusName()))
                    this.setBusName(jmsManagedCF.getBusName());

                if (isValueCanBeSet(jmsManagedCF.getClientID()))
                    this.setClientId(jmsManagedCF.getClientID());

                if (isValueCanBeSet(jmsManagedCF.getConsumerDoesNotModifyPayloadAfterGet()))
                    this.setConsumerDoesNotModifyPayloadAfterGet(jmsManagedCF.getConsumerDoesNotModifyPayloadAfterGet());
                //this.setConnectionProximity(jmsManagedConnectioFactory.getConnectionProximity());
                if (isValueCanBeSet(jmsManagedCF.getDurableSubscriptionHome()))
                    this.setDurableSubscriptionHome(jmsManagedCF.getDurableSubscriptionHome());
                //this.setLogWriter(jmsManagedConnectioFactory.getLogWriter());
                //this.setMulticastInterface(jmsManagedConnectioFactory.getMulticastInterface());
                //this.setNonPersistentMapping(jmsManagedConnectioFactory.getNonPersistentMapping());
                //this.setPersistentMapping(jmsManagedConnectioFactory.getPersistentMapping());
                //this.setProducerDoesNotModifyPayloadAfterSet(jmsManagedConnectioFactory.getProducerDoesNotModifyPayloadAfterSet());
                if (isValueCanBeSet(jmsManagedCF.getProviderEndpoints()))
                    this.setProviderEndpoints(jmsManagedCF.getProviderEndpoints());

                if (isValueCanBeSet(jmsManagedCF.getReadAhead()))
                    this.setReadAhead(jmsManagedCF.getReadAhead());

                if (isValueCanBeSet(jmsManagedCF.getShareDurableSubscriptions()))
                    this.setShareDurableSubscriptions(jmsManagedCF.getShareDurableSubscriptions());
                //this.setShareDataSourceWithCMP(jmsManagedConnectioFactory.getShareDataSourceWithCMP());
                //this.setSubscriptionProtocol(jmsManagedConnectioFactory.getSubscriptionProtocol());
                if (isValueCanBeSet(jmsManagedCF.getTarget()))
                    this.setTarget(jmsManagedCF.getTarget());

                if (isValueCanBeSet(jmsManagedCF.getTargetSignificance()))
                    this.setTargetSignificance(jmsManagedCF.getTargetSignificance());
                //this.setTargetTransport(jmsManagedConnectioFactory.getTargetTransport());
                if (isValueCanBeSet(jmsManagedCF.getTargetTransportChain()))
                    this.setTargetTransportChain(jmsManagedCF.getTargetTransportChain());

                if (isValueCanBeSet(jmsManagedCF.getTargetType()))
                    this.setTargetType(jmsManagedCF.getTargetType());
                //this.setTemporaryQueueNamePrefix(jmsManagedConnectioFactory.getTemporaryQueueNamePrefix());
            }

        } catch (NamingException exp) {
            //TODo - Do we need to just log the warning or throw the error to the user?
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Invalid Connection Factory JNDI name "
                                         + _connectionFactoryLookup);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.exit(this, TRACE, "setConnectionFactoryLookup", _connectionFactoryLookup);
        }
    }

    private boolean isValueCanBeSet(String value) {
        return value != null && !value.isEmpty();
    }
}
