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

package com.ibm.ws.sib.ra.inbound.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaDurableSubscriptionSharing;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.ra.inbound.SibRaMessageDeletionMode;
import com.ibm.ws.sib.ra.inbound.SibRaReadAhead;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.ra.SibRaActivationSpec;

/**
 * Activation specification implementation for inbound core SPI resource
 * adapter. Configured with endpoint properties via JCA administration.
 */
public final class SibRaActivationSpecImpl implements SibRaActivationSpec,
                SibRaEndpointConfigurationProvider, FFDCSelfIntrospectable {

    private static final String DEFAULT_SHARE_DURABLE_SUBSCRIPTIONS = SHARED_DSUBS_IN_CLUSTER;

    /**
     * Constant representing the default retry interval
     */
    private static final int DEFAULT_RETRY_INTERVAL = 30;

    /**
     * Constant representing the default failing message delay
     */
    private static final long DEFAULT_FAILING_MESSAGE_DELAY = 0;

    /**
     * The resource adapter associated with this activation specification.
     */
    private transient ResourceAdapter _resourceAdapter;

    /**
     * The name of the bus containing the destinations to receive messages from.
     */
    private String _busName = null;

    /**
     * The user name with which to connect to the messaging engine.
     */
    private String _userName = null;

    /**
     * The password with which to connect to the messaging engine.
     */
    private String _password = null;

    /**
     * The type of the destination to consume messages from.
     */
    private String _destinationType;

    /**
     * The name of the destination on the given bus from which the
     * message-driven bean should receive messages.
     */
    private String _destinationName = null;

    /**
     * A selector string used to filter which messages are received from the
     * destination.
     */
    private String _messageSelector = null;

    /**
     * The discriminator to specify when registering for message consumption.
     */
    private String _discriminator = null;

    /**
     * The name of the durable subscription.
     */
    private String _subscriptionName = null;

    /**
     * The name of the home for durable subscriptions.
     */
    private String _durableSubscriptionHome = null;

    /**
     * A string indicating whether durable subscriptions are shared.
     */
    private String _shareDurableSubscriptions = DEFAULT_SHARE_DURABLE_SUBSCRIPTIONS;

    /**
     * The maximum number of messages delivered in a single batch to an MDB
     * instance.
     */
    private Integer _maxBatchSize = Integer.valueOf(1);

    /**
     * The mode used to delete messages when used with a non-transactional MDB.
     */
    private String _messageDeletionMode = MESSAGE_DELETION_MODE_SINGLE;

    /**
     * The maximum number of threads to be used to deliver messages concurrently
     * to MDB instances.
     */
    private Integer _maxConcurrency = Integer.valueOf(10);

    /**
     * The target properties, these three properties work together to specify a target
     * when the RA has to create a remote connection. These are not used when a local
     * connection is created.
     */
    private String _target;
    private String _targetType;
    private String _targetSignificance;
    private String _targetTransportChain;

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
    private Boolean _useServerSubject = Boolean.TRUE;

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
     * The Max threshold for sequential message failure
     * 
     * Defult is -1
     */
    private Integer _maxSequentialMessageFailure = -1;

    /**
     * The Max threshold for sequential message failure
     * 
     * Defult is 0
     */
    private Integer _autoStopSequentialMessageFailure = 0;

    /**
     * This is set to true if a user wishes to consume from multile destination by specifying a wildcard (SIB0137)
     */
    private Boolean _useDestinationWildcard = Boolean.FALSE;

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String CLASS_NAME = SibRaActivationSpecImpl.class
                    .getName();

    /**
     * Map of valid subscription durabilities to their corresponding endpoint
     * configuration values.
     */
    private static final Map<String, SibRaDurableSubscriptionSharing> SUBSCRIPTION_SHAREABILITY = new HashMap<String, SibRaDurableSubscriptionSharing>(3);

    static {

        SUBSCRIPTION_SHAREABILITY.put(SHARED_DSUBS_IN_CLUSTER,
                                      SibRaDurableSubscriptionSharing.CLUSTER_ONLY);
        SUBSCRIPTION_SHAREABILITY.put(SHARED_DSUBS_ALWAYS,
                                      SibRaDurableSubscriptionSharing.ALWAYS);
        SUBSCRIPTION_SHAREABILITY.put(SHARED_DSUBS_NEVER,
                                      SibRaDurableSubscriptionSharing.NEVER);

    }

    /**
     * Map of valid destination types to their corresponding core SPI values.
     */
    private static final Map<String, DestinationType> DESTINATION_TYPES = new HashMap<String, DestinationType>(4);

    static {

        DESTINATION_TYPES.put(DESTINATION_TYPE_QUEUE, DestinationType.QUEUE);
        DESTINATION_TYPES.put(DESTINATION_TYPE_TOPIC_SPACE,
                              DestinationType.TOPICSPACE);
        DESTINATION_TYPES.put(DESTINATION_TYPE_PORT, DestinationType.PORT);
        DESTINATION_TYPES
                        .put(DESTINATION_TYPE_SERVICE, DestinationType.SERVICE);

    }

    /**
     * Map of valid message deletion modes to their corresponding endpoint
     * configuration values.
     */
    private static final Map<String, SibRaMessageDeletionMode> MESSAGE_DELETION_MODES = new HashMap<String, SibRaMessageDeletionMode>(3);

    static {

        MESSAGE_DELETION_MODES.put(MESSAGE_DELETION_MODE_SINGLE,
                                   SibRaMessageDeletionMode.SINGLE);
        MESSAGE_DELETION_MODES.put(MESSAGE_DELETION_MODE_BATCH,
                                   SibRaMessageDeletionMode.BATCH);
        MESSAGE_DELETION_MODES.put(MESSAGE_DELETION_MODE_APPLICATION,
                                   SibRaMessageDeletionMode.APPLICATION);

    }

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
                    .getTraceComponent(SibRaActivationSpecImpl.class);

    /**
     * The component to use for SibRaEndpointConfigurationImpl trace.
     */
    private static final TraceComponent CONFIG_TRACE = SibRaUtils
                    .getTraceComponent(SibRaEndpointConfigurationImpl.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setBusName(java.lang.String)
     */
    public void setBusName(final String busName) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "BusName", busName);
        }
        _busName = busName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getBusName()
     */
    public String getBusName() {

        return _busName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setUserName(java.lang.String)
     */
    public void setUserName(final String userName) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "UserName", userName);
        }
        _userName = userName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getUserName()
     */
    public String getUserName() {

        return _userName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setPassword(java.lang.String)
     */
    public void setPassword(final String password) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Password", (password == null) ? null
                            : "*****");
        }
        _password = password;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getPassword()
     */
    public String getPassword() {

        return _password;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setDestinationType(java.lang.String)
     */
    public void setDestinationType(final String destinationType) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "DestinationType", destinationType);
        }
        _destinationType = destinationType;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getDestinationType()
     */
    public String getDestinationType() {

        return _destinationType;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setDestinationName(java.lang.String)
     */
    public void setDestinationName(final String destinationName) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "DestinationName", destinationName);
        }
        _destinationName = destinationName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getDestinationName()
     */
    public String getDestinationName() {

        return _destinationName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMessageSelector(java.lang.String)
     */
    public void setMessageSelector(final String messageSelector) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "MessageSelector", messageSelector);
        }
        _messageSelector = messageSelector;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getMessageSelector()
     */
    public String getMessageSelector() {

        return _messageSelector;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setDiscriminator(java.lang.String)
     */
    public void setDiscriminator(final String discriminator) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Discriminator", discriminator);
        }
        _discriminator = discriminator;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getDiscriminator()
     */
    public String getDiscriminator() {

        return _discriminator;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setSubscriptionName(java.lang.String)
     */
    public void setSubscriptionName(final String subscriptionName) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "SubscriptionName", subscriptionName);
        }
        _subscriptionName = subscriptionName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getSubscriptionName()
     */
    public String getSubscriptionName() {

        return _subscriptionName;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setDurableSubscriptionHome(java.lang.String)
     */
    public void setDurableSubscriptionHome(final String durableSubscriptionHome) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "DurableSubscriptionHome",
                        durableSubscriptionHome);
        }
        _durableSubscriptionHome = durableSubscriptionHome;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getDurableSubscriptionHome()
     */
    public String getDurableSubscriptionHome() {

        return _durableSubscriptionHome;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setShareDurableSubscriptions(String)
     */
    public void setShareDurableSubscriptions(String shareDurableSubscriptions) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "ShareDurableSubscriptions",
                        shareDurableSubscriptions);
        }
        _shareDurableSubscriptions = shareDurableSubscriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getShareDurableSubscriptions()
     */
    public String getShareDurableSubscriptions() {
        return _shareDurableSubscriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMaxBatchSize(java.lang.Integer)
     */
    public void setMaxBatchSize(final Integer maxBatchSize) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "MaxBatchSize", maxBatchSize);
        }
        _maxBatchSize = maxBatchSize;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMaxBatchSize(java.lang.String)
     */
    public void setMaxBatchSize(final String maxBatchSize) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "MaxBatchSize", maxBatchSize);
        }
        _maxBatchSize = (maxBatchSize == null ? null : Integer.valueOf(maxBatchSize));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getMaxBatchSize()
     */
    public Integer getMaxBatchSize() {

        return _maxBatchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMessageDeletionMode(java.lang.String)
     */
    public void setMessageDeletionMode(final String messageDeletionMode) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr
                            .debug(this, TRACE, "MessageDeletionMode",
                                   messageDeletionMode);
        }
        _messageDeletionMode = messageDeletionMode;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getMessageDeletionMode()
     */
    public String getMessageDeletionMode() {

        return _messageDeletionMode;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMaxConcurrency(java.lang.Integer)
     */
    public void setMaxConcurrency(final Integer maxConcurrency) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "MaxConcurrency", maxConcurrency);
        }
        _maxConcurrency = maxConcurrency;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#setMaxConcurrency(java.lang.String)
     */
    public void setMaxConcurrency(final String maxConcurrency) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "MaxConcurrency", maxConcurrency);
        }
        _maxConcurrency = (maxConcurrency == null ? null : Integer.valueOf(maxConcurrency));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.ra.SibRaActivationSpec#getMaxConcurrency()
     */
    public Integer getMaxConcurrency() {

        return _maxConcurrency;

    }

    /**
     * Gets the provider endpoints property
     * 
     * @return the provider endpoints property
     */
    public String getProviderEndpoints() {

        return _providerEndpoints;

    }

    /**
     * Set the provider endpoints property
     * 
     * @param providerEndpoints The provider endpoints property to use
     */
    public void setProviderEndpoints(String providerEndpoints) {

        _providerEndpoints = providerEndpoints;
    }

    /**
     * Gets the target Transport property
     * 
     * @return the target Transport property
     */
    public String getTargetTransport() {

        return _targetTransport;

    }

    /**
     * Set the Target Transport property
     * 
     * @param targetTransport The Target Transport property to use
     */
    public void setTargetTransport(String targetTransport) {

        _targetTransport = targetTransport;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ActivationSpec#validate()
     */
    public void validate() throws InvalidPropertyException {

        final String methodName = "validate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final List<String> failedPropsMessages = new ArrayList<String>();
        final List<PropertyDescriptor> invalidProperties = new ArrayList<PropertyDescriptor>();

        try {

            if (isSet(_destinationName))
            {
                // BusName must be set
                if (!isSet(_busName)) {

                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE, "Invalid BusName - BusName was "
                                                 + _busName);
                    }
                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_BUSNAME_CWSIV0504"),
                                                                    null, null));
                    invalidProperties.add(new PropertyDescriptor("BusName",
                                    SibRaActivationSpec.class));

                }

                // DestinationType must be valid
                if (!DESTINATION_TYPES.keySet().contains(_destinationType)) {

                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE,
                                    "Invalid Destination Type - We expected a destination type of "
                                                    + DESTINATION_TYPE_QUEUE + " or " + DESTINATION_TYPE_TOPIC_SPACE + " or "
                                                    + DESTINATION_TYPE_PORT + " or " + DESTINATION_TYPE_SERVICE
                                                    + " but we got "
                                                    + _destinationType);
                    }

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_DESTINATION_TYPE_CWSIV0505"),
                                                                    new Object[] { DESTINATION_TYPE_QUEUE,
                                                                                  DESTINATION_TYPE_TOPIC_SPACE,
                                                                                  DESTINATION_TYPE_PORT,
                                                                                  DESTINATION_TYPE_SERVICE,
                                                                                  _destinationType }, null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "DestinationType", SibRaActivationSpec.class));

                }

                // Make sure we are not using a topic space if wildcarded destinations are used.
                if ((Boolean.TRUE.equals(_useDestinationWildcard))
                    && (DESTINATION_TYPE_TOPIC_SPACE.equals(_destinationType)))
                {
                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_USEDESTWILDCARD_CWSIV0515"),
                                                                    null, null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "UseDestinationWildcard",
                                    SibRaActivationSpec.class));
                }

                // For durable subscriptions...
                if ((DESTINATION_TYPE_TOPIC_SPACE.equals(_destinationType))
                    && (isSet(_subscriptionName))) {

                    // ShareDurableSubscriptions must be valid
                    if (!SUBSCRIPTION_SHAREABILITY.keySet().contains(
                                                                     _shareDurableSubscriptions)) {

                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr.debug(this, TRACE,
                                        "Invalid shareDurableSubscriptions - We expect a value of "
                                                        + SibRaDurableSubscriptionSharing.ALWAYS + ", "
                                                        + SibRaDurableSubscriptionSharing.NEVER + " or "
                                                        + SibRaDurableSubscriptionSharing.CLUSTER_ONLY + " but we got "
                                                        + _shareDurableSubscriptions);
                        }
                        failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_SHARE_DURSUB_CWSIV0506"),
                                                                        new Object[] { SibRaDurableSubscriptionSharing.ALWAYS,
                                                                                      SibRaDurableSubscriptionSharing.NEVER,
                                                                                      SibRaDurableSubscriptionSharing.CLUSTER_ONLY,
                                                                                      _shareDurableSubscriptions },
                                                                        null));

                        invalidProperties.add(new PropertyDescriptor(
                                        "ShareDurableSubscriptions",
                                        SibRaActivationSpec.class));

                    }

                    // DurableSubscriptionHome must be set
                    if (!isSet(_durableSubscriptionHome)) {

                        failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_DURSUBHOME_CWSIV0507"),
                                                                        null, null));
                        invalidProperties.add(new PropertyDescriptor(
                                        "DurableSubscriptionHome",
                                        SibRaActivationSpec.class));

                    }

                }

            } else {

                // DestinationType must be valid and not a topic space
                if (!DESTINATION_TYPES.keySet().contains(_destinationType)
                    || DESTINATION_TYPE_TOPIC_SPACE
                                    .equals(_destinationType)) {

                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE,
                                    "Invalid Destination Type - No destination was specified so we expected a destination type of "
                                                    + DESTINATION_TYPE_QUEUE + " or " + DESTINATION_TYPE_PORT + " or "
                                                    + DESTINATION_TYPE_SERVICE + " but we got "
                                                    + _destinationType);
                    }

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_DESTINATION_TYPE_CWSIV0508"),
                                                                    new Object[] { DESTINATION_TYPE_QUEUE,
                                                                                  DESTINATION_TYPE_PORT,
                                                                                  DESTINATION_TYPE_SERVICE,
                                                                                  _destinationType }, null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "DestinationType", SibRaActivationSpec.class));

                }

            }

            // MaxBatchSize must be an Integer greater than zero
            if ((_maxBatchSize == null) || (_maxBatchSize.intValue() < 1)) {

                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_MAXBATCH_CWSIV0509"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor("MaxBatchSize",
                                SibRaActivationSpec.class));

            }

            // MessageDeletionMode must be valid
            if (!MESSAGE_DELETION_MODES.keySet().contains(_messageDeletionMode)) {

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Invalid Message Deletion Mode we expected a mode of "
                                                + SibRaMessageDeletionMode.SINGLE + " or "
                                                + SibRaMessageDeletionMode.BATCH + " or "
                                                + SibRaMessageDeletionMode.APPLICATION + " but we got "
                                                + _messageDeletionMode);
                }

                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_DELETION_MODE_CWSIV0510"),
                                                                new Object[] { SibRaMessageDeletionMode.SINGLE,
                                                                              SibRaMessageDeletionMode.BATCH,
                                                                              SibRaMessageDeletionMode.APPLICATION,
                                                                              _messageDeletionMode }, null));
                invalidProperties.add(new PropertyDescriptor(
                                "MessageDeletionMode", SibRaActivationSpec.class));

            }

            // MaxConcurrency must be an Integer greater than zero
            if ((_maxConcurrency == null) || (_maxConcurrency.intValue() < 1)) {

                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_MAXCONC_CWSIV0511"),
                                                                null, null));
                invalidProperties.add(new PropertyDescriptor("MaxConcurrency",
                                SibRaActivationSpec.class));

            }

            /**
             * For the target set of properties, use the defaults if none specified
             */

            if ("".equals(_targetType)) {

                _targetType = JmsraConstants.DEFAULT_TARGET_TRANSPORT;

            }

            if ("".equals(_target)) {

                _target = JmsraConstants.DEFAULT_TARGET;

            }

            // Target type must be one of the predefined values
            if ((null == _targetType) || ("".equals(_targetType))) {

                _targetType = JmsraConstants.DEFAULT_TARGET_TYPE;

            } else {

                if (!((ApiJmsConstants.TARGET_TYPE_BUSMEMBER.equals(_targetType)) ||
                      (ApiJmsConstants.TARGET_TYPE_ME.equals(_targetType)) || (ApiJmsConstants.TARGET_TYPE_CUSTOM.equals(_targetType)))) {

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_TARGET_TYPE_CWSIV0512"),
                                                                    new Object[] { ApiJmsConstants.TARGET_TYPE_BUSMEMBER,
                                                                                  ApiJmsConstants.TARGET_TYPE_ME,
                                                                                  ApiJmsConstants.TARGET_TYPE_CUSTOM,
                                                                                  _targetType },
                                                                    null));

                    invalidProperties.add(new PropertyDescriptor(
                                    "targetType",
                                    SibRaActivationSpec.class));

                }

            }

            // Target significance must be one of the predefined values
            if ((null == _targetSignificance) || ("".equals(_targetSignificance))) {

                _targetSignificance = JmsraConstants.DEFAULT_TARGET_SIGNIFICANCE;

            } else {

                if (!((ApiJmsConstants.TARGET_SIGNIFICANCE_PREFERRED.equals(_targetSignificance)) || (ApiJmsConstants.TARGET_SIGNIFICANCE_REQUIRED.equals(_targetSignificance)))) {

                    failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_TARGET_SIGNIFICANCE_CWSIV0513"),
                                                                    new Object[] { ApiJmsConstants.TARGET_SIGNIFICANCE_PREFERRED,
                                                                                  ApiJmsConstants.TARGET_SIGNIFICANCE_REQUIRED,
                                                                                  _targetSignificance },
                                                                    null));
                    invalidProperties.add(new PropertyDescriptor(
                                    "targetSignificance",
                                    SibRaActivationSpec.class));

                }

            }

            if ((_useServerSubject.booleanValue()) && (isSet(_providerEndpoints)))
            {
                failedPropsMessages.add(NLS.getFormattedMessage(("INVALID_PROPERTY_PROVIDER_ENDPOINTS_CWSIV0514"),
                                                                new Object[] { _providerEndpoints },
                                                                null));
                invalidProperties.add(new PropertyDescriptor(
                                "providerEndpoints",
                                SibRaActivationSpec.class));
            }

        } catch (final IntrospectionException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, FFDC_PROBE_1, this);
            // Should never happen

        }

        // If we have any invalid properties, throw an
        // InvalidPropertyException
        if (!invalidProperties.isEmpty()) {

            final PropertyDescriptor[] invalidPropertyArray = (PropertyDescriptor[]) invalidProperties
                            .toArray(new PropertyDescriptor[invalidProperties
                                            .size()]);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Invalid properties found:",
                            failedPropsMessages);
            }

            final InvalidPropertyException exc = new InvalidPropertyException(
                            NLS.getFormattedMessage(
                                                    ("INVALID_PROPERTIES_CWSIV0501"),
                                                    new Object[] { this, failedPropsMessages }, null));
            exc.setInvalidPropertyDescriptors(invalidPropertyArray);
            throw exc;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ResourceAdapterAssociation#getResourceAdapter()
     */
    public ResourceAdapter getResourceAdapter() {

        return _resourceAdapter;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.resource.spi.ResourceAdapterAssociation#setResourceAdapter(javax.resource.spi.ResourceAdapter)
     */
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
    public String getTarget() {

        return _target;

    }

    /**
     * Set the target type property.
     * 
     * @param targetType
     */
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
    public String getTargetType() {

        return _targetType;

    }

    /**
     * Set the target significance property.
     * 
     * @param target significance
     */
    public void setTargetSignificance(String targetSignificance) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTargetSignificance", targetSignificance);
        }
        _targetSignificance = targetSignificance;

    }

    /**
     * Set the target transport chain property.
     * 
     * @param target transport chain
     */
    public void setTargetTransportChain(String targetTransportChain) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "setTargetTransportChain", targetTransportChain);
        }
        _targetTransportChain = targetTransportChain;

    }

    /**
     * Get the target significance property.
     * 
     * @return the target significance property
     */
    public String getTargetSignificance() {

        return _targetSignificance;

    }

    /**
     * Get the target transport chain property.
     * 
     * @return the target transport chain property
     */
    public String getTargetTransportChain() {

        return _targetTransportChain;

    }

    /**
     * Set the useServerSubject property.
     * 
     * @param useServerSubject
     */
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
    public Boolean getUseServerSubject() {

        return _useServerSubject;

    }

    /**
     * Get the MaxSequentialMessageFailure property
     * 
     * @return
     */
    public Integer getMaxSequentialMessageFailure()
    {
        return _maxSequentialMessageFailure;
    }

    /**
     * Set the MaxSequentialMessageFailure
     * 
     * @param MaxSequentialMessageFailure
     */
    public void setMaxSequentialMessageFailure(Integer maxSequentialMessageFailure)
    {
        _maxSequentialMessageFailure = maxSequentialMessageFailure;
    }

    /**
     * Set the MaxSequentialMessageFailure
     * 
     * @param MaxSequentialMessageFailure
     */
    public void setMaxSequentialMessageFailure(final String maxSequentialMessageFailure)
    {
        _maxSequentialMessageFailure = (maxSequentialMessageFailure == null ? null : Integer.valueOf(maxSequentialMessageFailure));
    }

    /**
     * Get the AutoStopSequentialMessageFailure property
     * 
     * @return
     */
    public Integer getAutoStopSequentialMessageFailure()
    {
        return _autoStopSequentialMessageFailure;
    }

    /**
     * Set the AutoStopSequentialMessageFailure
     * 
     * @param AutoStopSequentialMessageFailure
     */
    public void setAutoStopSequentialMessageFailure(Integer autoStopSequentialMessageFailure)
    {
        _autoStopSequentialMessageFailure = autoStopSequentialMessageFailure;
    }

    /**
     * Set the AutoStopSequentialMessageFailure
     * 
     * @param AutoStopSequentialMessageFailure
     */
    public void setAutoStopSequentialMessageFailure(final String autoStopSequentialMessageFailure)
    {
        _autoStopSequentialMessageFailure = (autoStopSequentialMessageFailure == null ? null : Integer.valueOf(autoStopSequentialMessageFailure));
    }

    /**
     * Sets the activate all MDBs in a cluster bus member property.
     */
    public void setAlwaysActivateAllMDBs(Boolean alwaysActivateAllMDBs)
    {
        _alwaysActivateAllMDBs = alwaysActivateAllMDBs;
    }

    /**
     * Gets the activate all MDBs in a cluster bus member property.
     */
    public Boolean getAlwaysActivateAllMDBs()
    {
        return _alwaysActivateAllMDBs;
    }

    /**
     * Sets the retry interval
     */
    public void setRetryInterval(Integer retryInterval)
    {
        _retryInterval = retryInterval;
    }

    /**
     * Sets the retry interval
     */
    public void setRetryInterval(String retryInterval)
    {
        _retryInterval = (retryInterval == null ? null : Integer.valueOf(retryInterval));
    }

    /**
     * Gets the retry interval
     */
    public Integer getRetryInterval()
    {
        return _retryInterval;
    }

    /**
     * Gets the failing message delay time
     * 
     * @return the failing message delay
     */
    public Long getFailingMessageDelay()
    {
        return _failingMessageDelay;
    }

    /**
     * GSts the failing message delay time
     * 
     * @param delay the failing message delay
     */
    public void setFailingMessageDelay(Long delay)
    {
        _failingMessageDelay = delay;
    }

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
     *            should be treated as a wildcard expression or not.
     */
    public void setUseDestinationWildcard(Boolean useDestinationWildcard)
    {
        _useDestinationWildcard = useDestinationWildcard;
    }

    /**
     * Retrieve the variable indicating whether the destination name property should be treated
     * as a wildcard expression.
     * 
     * @return Boolean that indicates whether the destination name should be treated as a
     *         wildcard expression
     */
    public Boolean getUseDestinationWildcard()
    {
        return _useDestinationWildcard;
    }

    /**
     * Returns a string represenation of this object.
     * 
     * @return the string representation
     */
    public String toString() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);

        generator.addParent("resourceAdapter", _resourceAdapter);
        generator.addField("busName", _busName);
        generator.addField("userName", _userName);
        generator.addPasswordField("password", _password);
        generator.addField("destinationType", _destinationType);
        generator.addField("destinationName", _destinationName);
        generator.addField("messageSelector", _messageSelector);
        generator.addField("discriminators", _discriminator);
        generator.addField("subscriptionName", _subscriptionName);
        generator.addField("durableSubscriptionHome", _durableSubscriptionHome);
        generator.addField("shareDurableSubscriptions",
                           _shareDurableSubscriptions);
        generator.addField("maxBatchSize", _maxBatchSize);
        generator.addField("messageDeletionMode", _messageDeletionMode);
        generator.addField("maxConcurrency", _maxConcurrency);
        generator.addField("target", _target);
        generator.addField("targetType", _targetType);
        generator.addField("targetSignificance", _targetSignificance);
        generator.addField("useServerSubject", _useServerSubject);
        generator.addField("providerEndpoints", _providerEndpoints);
        generator.addField("alwaysActivateAllMDBs", _alwaysActivateAllMDBs);
        generator.addField("retryInterval", _retryInterval);
        generator.addField("useServerSubject", _useServerSubject);
        generator.addField("failingMessageDelay", _failingMessageDelay);
        generator.addField("useDestinationWildcard", _useDestinationWildcard);

        return generator.getStringRepresentation();

    }

    /**
     * Returns a list of fields for use when introspecting for FFDC
     * 
     * @return An array of strings for use with FFDC
     */
    public String[] introspectSelf()
    {
        return new String[] { toString() };
    }

    /**
     * Helper method returns <code>true</code> if the given string has been
     * set i.e. it is not null or the empty string.
     * 
     * @param string
     *            the string to check
     * @return <code>true</code> if the given string has been set
     */
    private static boolean isSet(final String string) {

        return (string != null) && !string.equals("");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider#getEndpointInvoker()
     */
    public SibRaEndpointInvoker getEndpointInvoker()
                    throws ResourceAdapterInternalException {

        return new SibRaEndpointInvokerImpl();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider#getEndpointConfiguration()
     */
    public SibRaEndpointConfiguration getEndpointConfiguration()
                    throws InvalidPropertyException, ResourceAdapterInternalException {

        final String methodName = "getEndpointConfiguration";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final SibRaEndpointConfiguration configuration = new SibRaEndpointConfigurationImpl();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, configuration);
        }
        return configuration;

    }

    /**
     * Inner class mapping the properties of this activation specification to
     * those required by the <code>SibRaEndpointConfiguration</code>
     * interface.
     */
    private class SibRaEndpointConfigurationImpl implements
                    SibRaEndpointConfiguration {

        private final SIDestinationAddressFactory _destinationAddressFactory;

        /**
         * Constructor. Does everything up front that might throw an exception.
         * 
         * @throws InvalidPropertyException
         *             if the configuration is not valid
         * @throws ResourceAdapterInternalException
         *             if we cannot obtain a destination address factory of JMS
         *             utilities
         */
        private SibRaEndpointConfigurationImpl()
            throws InvalidPropertyException,
            ResourceAdapterInternalException {

            final String methodName = "SibRaEndpointConfigurationImpl";
            if (TraceComponent.isAnyTracingEnabled() && CONFIG_TRACE.isEntryEnabled()) {
                SibTr.entry(this, CONFIG_TRACE, methodName);
            }

            validate();

            try {

                _destinationAddressFactory = JmsServiceFacade.getSIDestinationAddressFactory();

            } catch (final Exception exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + ".SibRaEndpointConfigurationImpl." + methodName,
                                            FFDC_PROBE_2, this);
                if (TraceComponent.isAnyTracingEnabled() && CONFIG_TRACE.isEventEnabled()) {
                    SibTr.exception(this, CONFIG_TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("DEST_FACTORY_CWSIV0502"),
                                                     new Object[] { exception }, null), exception);

            }

            if (TraceComponent.isAnyTracingEnabled() && CONFIG_TRACE.isEntryEnabled()) {
                SibTr.exit(this, CONFIG_TRACE, methodName);
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestination()
         */
        public SIDestinationAddress getDestination() {

            final String methodName = "getDestination";
            if (TraceComponent.isAnyTracingEnabled() && CONFIG_TRACE.isEntryEnabled()) {
                SibTr.entry(this, CONFIG_TRACE, methodName);
            }

            final SIDestinationAddress destination;
            if (_destinationName == null) {
                destination = null;
            } else {
                destination = _destinationAddressFactory
                                .createSIDestinationAddress(_destinationName, false);
            }

            if (TraceComponent.isAnyTracingEnabled() && CONFIG_TRACE.isEntryEnabled()) {
                SibTr.exit(this, CONFIG_TRACE, methodName, destination);
            }
            return destination;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getAllowMessageGathering()
         */
        public String getAllowMessageGathering()
        {
            // This is a JMS only property configured on a JMS Destination
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestinationName()
         */
        public String getDestinationName()
        {
            return _destinationName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDestinationType()
         */
        public DestinationType getDestinationType() {

            return (DestinationType) DESTINATION_TYPES.get(_destinationType);

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDiscriminator()
         */
        public String getDiscriminator() {

            return _discriminator;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMaxConcurrency()
         */
        public int getMaxConcurrency() {
            return _maxConcurrency.intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMaxBatchSize()
         */
        public int getMaxBatchSize() {
            return _maxBatchSize.intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getBusName()
         */
        public String getBusName() {
            return _busName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getUserName()
         */
        public String getUserName() {
            return _userName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getPassword()
         */
        public String getPassword() {
            return _password;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMessageSelector()
         */
        public String getMessageSelector() {
            return _messageSelector;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDurableSubscriptionName()
         */
        public String getDurableSubscriptionName() {
            return _subscriptionName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getDurableSubscriptionHome()
         */
        public String getDurableSubscriptionHome() {
            return _durableSubscriptionHome;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getShareDurableSubscriptions()
         */
        public SibRaDurableSubscriptionSharing getShareDurableSubscriptions() {

            return (SibRaDurableSubscriptionSharing) SUBSCRIPTION_SHAREABILITY
                            .get(_shareDurableSubscriptions);

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getMessageDeletionMode()
         */
        public SibRaMessageDeletionMode getMessageDeletionMode() {

            return (SibRaMessageDeletionMode) MESSAGE_DELETION_MODES
                            .get(_messageDeletionMode);

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#isDurableSubscription()
         */
        public boolean isDurableSubscription() {

            return ((DESTINATION_TYPE_TOPIC_SPACE.equals(_destinationType)) && (isSet(_subscriptionName)));

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getSelectorDomain()
         */
        public SelectorDomain getSelectorDomain() {

            return SelectorDomain.SIMESSAGE;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getShareDataSourceWithCMP()
         */
        public boolean getShareDataSourceWithCMP() {
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetTransportChain()
         */
        public String getTargetTransportChain() {
            return _targetTransportChain;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getReadAhead()
         */
        public SibRaReadAhead getReadAhead() {
            return SibRaReadAhead.DEFAULT;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTarget()
         */
        public String getTarget() {
            return _target;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetType()
         */
        public String getTargetType() {
            return _targetType;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getTargetSignificance()
         */
        public String getTargetSignificance() {
            return _targetSignificance;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getActivationSpec()
         */
        public ActivationSpec getActivationSpec() {
            return SibRaActivationSpecImpl.this;
        }

        /**
         * Get the MaxSequentialMessageFailure property
         * 
         * @return
         */
        public int getMaxSequentialMessageFailure()
        {
            return _maxSequentialMessageFailure;
        }

        /**
         * Get the AutoStopSequentialMessageFailure property
         * 
         * @return
         */
        public int getAutoStopSequentialMessageFailure()
        {
            return _autoStopSequentialMessageFailure;
        }

        /**
         * Returns a string represenation of this object.
         * 
         * @return the string representation
         */
        public String toString() {

            final SibRaStringGenerator generator = new SibRaStringGenerator(
                            this);

            generator.addField("SibRaActivationSpecImpl.this",
                               SibRaActivationSpecImpl.this);

            return generator.getStringRepresentation();

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#isJMSRa()
         */
        public boolean isJMSRa() {

            return false;

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getProviderEndpoints()
         */
        public String getProviderEndpoints() {
            return _providerEndpoints;
        }

        /**
         * Gets the flag indicating if all MDBs in a cluster bus member should be
         * activated
         */
        public Boolean getAlwaysActivateAllMDBs()
        {
            return _alwaysActivateAllMDBs;
        }

        /**
         * Gets the retry interval
         */
        public int getRetryInterval()
        {
            return _retryInterval;
        }

        /**
         * Gets the flag indicating if destination wildcard is being used.
         * 
         * @return True if a destation wildcard is being used
         */
        public Boolean getUseDestinationWildcard()
        {
            return _useDestinationWildcard;
        }

        public boolean getUseServerSubject()
        {
            return _useServerSubject;
        }

        /**
         * Gets the failing message delay time
         * 
         * @return the failing message delay
         */
        public Long getFailingMessageDelay()
        {
            return _failingMessageDelay;
        }

        /** {@inheritDoc} */
        @Override
        public String getTargetTransport() {
            return _targetTransport;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration#getSubscriptionDurablity()
         */
        @Override
        public String getSubscriptionDurability() {

            return null; //Venu JMS 2.0 TODO . This function would not be used.

        }
    }
}
