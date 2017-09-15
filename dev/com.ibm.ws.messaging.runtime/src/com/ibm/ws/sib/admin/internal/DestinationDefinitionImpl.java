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
package com.ibm.ws.sib.admin.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIBDestinationReliabilityType;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.QualifiedDestinationName;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

public class DestinationDefinitionImpl extends BaseDestinationDefinitionImpl implements DestinationDefinition {

    private static final TraceComponent tc = SibTr.register(DestinationDefinitionImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

 
    // The following data members are derived from WCCM configuration.

    private int _defaultPriority = 0;
    private Map _destinationContext = new HashMap();
    private String _exceptionDestination = null;
    private QualifiedDestinationName[] _forwardRoutingPath = null;
    private int _maxFailedDeliveries = 5;
    private boolean _isRedeliveryCountPersisted = false;
    private Reliability _maxReliability;
    private boolean _producerQOSOverrideEnabled = true;
    private boolean _receiveAllowed = true;
    private boolean _receiveExclusive = false;
    private Reliability _reliability;
    private QualifiedDestinationName _replyDestination = null;
    private boolean _sendAllowed = true;
    private boolean _topicAccessCheckRequired = true;
    private boolean _isOrderingRequired = false;
    private boolean _isAuditAllowed = true;
    private long _blockedRetryTimeout;
    private Reliability _exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;

    // The following data members are dynamic and are not directly part of WCCM
    // configuration.

    public long _alterationTime;

    /**
     * Constructor: Create a default instance. This constructor should be used
     * where it is required to create a DestinationDefinition which is not formed
     * from WCCM configuration objects. Examples of this would be for the
     * creation of dynamic destinations and for use in unit testcases.
     * 
     * @param type
     *            the type of destination to be created
     * @param name
     *            the name of the destination
     */
    public DestinationDefinitionImpl(DestinationType type, String name) // F008622 changed this constructor to public
    {
        super(type, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "DestinationDefinitionImpl", new Object[] { type, name });
        }
     

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "DestinationDefinitionImpl", this);
        }
    }

    /**
     * Constructor: Create an instance from a WCCM configuration object. This
     * constructor is intended to be used by the SM component when creating
     * destinations either during startup or on request.
     * 
     * @param dest
     */
    DestinationDefinitionImpl(LWMConfig dest) {
        super(dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "DestinationDefinitionImpl", dest);
        }

        setConfigType(DestinationConfigType.LOCAL);
        _destinationType = ((SIBDestination) dest).getDestinationType();

        setFromConfig(dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "DestinationDefinitionImpl", this);
        }
    }

    /**
     * @return
     */
    @Override
    protected DestinationConfigType getConfigType() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getConfigType", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getConfigType", _type);
        }

        return _type;
    }

    /**
     * @param d
     */
    @Override
    protected void setConfigType(DestinationConfigType d) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setConfigType", d);
        }

        _type = d;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setConfigType");
        }
    }

    /**
     * @param dest
     */
    @Override
    protected void setFromConfig(LWMConfig dest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setFromConfig", dest);
        }
        super.setFromConfig(dest);
        SIBDestination destConfig = ((SIBDestination) dest);
        _defaultPriority = destConfig.getDefaultPriority();

        _exceptionDestination = destConfig.getExceptionDestination();
        _maxFailedDeliveries = destConfig.getMaxFailedDeliveries();
        _isRedeliveryCountPersisted = destConfig.isPersistRedeliveryCount();

        _producerQOSOverrideEnabled = destConfig.isOverrideOfQOSByProducerAllowed();
        _receiveAllowed = destConfig.isReceiveAllowed();

        if (_destinationType == DestinationType.QUEUE) {
            _receiveExclusive = destConfig.isReceiveExclusive();
        }

        // Map the Reliability from WCCM to internal.

        String _rs = destConfig.getDefaultReliability();

        if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
            _reliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
            _reliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
            _reliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
            _reliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
            _reliability = Reliability.RELIABLE_PERSISTENT;
        }

        // Map the maxReliability from WCCM to Core SPI.

        _rs = destConfig.getMaximumReliability();

        if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
            _maxReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
            _maxReliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
            _maxReliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
            _maxReliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
            _maxReliability = Reliability.RELIABLE_PERSISTENT;
        }

        // Map the exceptionDiscardReliability from WCCM to Core SPI.

        _rs = destConfig.getExceptionDiscardReliability();

        if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
            _exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
            _exceptionDiscardReliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
            _exceptionDiscardReliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
            _exceptionDiscardReliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
            _exceptionDiscardReliability = Reliability.RELIABLE_PERSISTENT;
        }

        _sendAllowed = destConfig.isSendAllowed();

        if (_destinationType == DestinationType.TOPICSPACE) {
            _topicAccessCheckRequired = destConfig.isTopicAccessCheckRequired();
        }

        _isOrderingRequired = destConfig.isMaintainStrictOrder();

        if (_isOrderingRequired) {
            if (!_receiveExclusive) {
                setReceiveExclusive(true);
                // Override and warn if ordered destination.
                SibTr.debug(tc, "RECEIVE_EXCLUSIVE_OVERRIDE_WARNING_SIAS0048", new Object[] { destConfig.getName() });
            }
        }
        _blockedRetryTimeout = destConfig.getBlockedRetryTimeout();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setFromConfig");
        }
    }

    public int getDefaultPriority() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDefaultPriority", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(_defaultPriority);
            SibTr.exit(tc, "getDefaultPriority", i);
        }

        return _defaultPriority;
    }

    public void setDefaultPriority(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(value);
            SibTr.entry(tc, "setDefaultPriority", i);
        }

        _defaultPriority = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setDefaultPriority");
        }
    }

    public Reliability getDefaultReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDefaultReliability", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(_reliability.toInt());
            SibTr.exit(tc, "getDefaultReliability", i + " " + _reliability.toString());
        }

        return _reliability;
    }

    public void setDefaultReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(value.toInt());
            SibTr.entry(tc, "setDefaultReliability", i + " " + value.toString());
        }

        _reliability = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setDefaultReliability");
        }
    }

    @Override
    public Map getDestinationContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDestinationContext", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getDestinationContext", _destinationContext.toString());
        }

        return _destinationContext;
    }

    @Override
    public void setDestinationContext(Map value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setDestinationContext", value.toString());
        }

        _destinationContext = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setDestinationContext");
        }
    }

    public DestinationType getDestinationType() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDestinationType", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getDestinationType", _destinationType.toString());
        }

        return _destinationType;
    }

    public String getExceptionDestination() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getExceptionDestination", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getExceptionDestination", _exceptionDestination);
        }

        return _exceptionDestination;
    }

    public void setExceptionDestination(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setExceptionDestination", value);
        }

        _exceptionDestination = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setExceptionDestination");
        }
    }

    public Reliability getExceptionDiscardReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getExceptionDiscardReliability", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getExceptionDiscardReliability", _exceptionDiscardReliability);
        }

        return _exceptionDiscardReliability;
    }

    public void setExceptionDiscardReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setExceptionDiscardReliability", value.toString());
        }

        _exceptionDiscardReliability = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setExceptionDiscardReliability");
        }
    }

    public int getMaxFailedDeliveries() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMaxFailedDeliveries", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(_maxFailedDeliveries);
            SibTr.exit(tc, "getMaxFailedDeliveries", i);
        }

        return _maxFailedDeliveries;
    }

    public boolean isRedeliveryCountPersisted() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isRedeliveryCountPersisted", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean i = Boolean.valueOf(_isRedeliveryCountPersisted);
            SibTr.exit(tc, "isRedeliveryCountPersisted", i);
        }

        return _isRedeliveryCountPersisted;

    }

    public void setMaxFailedDeliveries(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = Integer.valueOf(value);
            SibTr.entry(tc, "setMaxFailedDeliveries", i);
        }

        _maxFailedDeliveries = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setMaxFailedDeliveries");
        }
    }

    public void setRedeliveryCountPersisted(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean i = Boolean.valueOf(value);
            SibTr.entry(tc, "setRedeliveryCountPersisted", i);
        }

        _isRedeliveryCountPersisted = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setRedeliveryCountPersisted");
        }
    }

    public boolean isReceiveAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isReceiveAllowed", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_receiveAllowed);
            SibTr.exit(tc, "isReceiveAllowed", b);
        }

        return _receiveAllowed;
    }

    public void setReceiveAllowed(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setReceiveAllowed", b.toString());
        }

        _receiveAllowed = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setReceiveAllowed");
        }
    }

    public Reliability getMaxReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMaxReliability", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getMaxReliability", _maxReliability.toString());
        }

        return _maxReliability;
    }

    public void setMaxReliability(Reliability value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setMaxReliability", value.toString());
        }

        _maxReliability = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setMaxReliability");
        }
    }

    public boolean isOverrideOfQOSByProducerAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_producerQOSOverrideEnabled);
            SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed", b);
        }

        return _producerQOSOverrideEnabled;
    }

    public void setOverrideOfQOSByProducerAllowed(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setOverrideOfQOSByProducerAllowed", b.toString());
        }

        _producerQOSOverrideEnabled = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setOverrideOfQOSByProducerAllowed");
        }
    }

    public boolean isProducerQOSOverrideEnabled() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isProducerQOSOverrideEnabled", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_producerQOSOverrideEnabled);
            SibTr.exit(tc, "isProducerQOSOverrideEnabled", b);
        }

        return _producerQOSOverrideEnabled;
    }

    public void setProducerQOSOverrideEnabled(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setProducerQOSOverrideEnabled", b.toString());
        }

        _producerQOSOverrideEnabled = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setProducerQOSOverrideEnabled");
        }
    }

    public boolean isReceiveExclusive() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isReceiveExclusive", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_receiveExclusive);
            SibTr.exit(tc, "isReceiveExclusive", b);
        }

        return _receiveExclusive;
    }

    public void setReceiveExclusive(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setReceiveExclusive", b.toString());
        }

        _receiveExclusive = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setReceiveExclusive");
        }
    }

    public boolean isSendAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isSendAllowed", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_sendAllowed);
            SibTr.exit(tc, "isSendAllowed", b);
        }

        return _sendAllowed;
    }

    public void setSendAllowed(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setSendAllowed", b.toString());
        }

        _sendAllowed = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setSendAllowed");
        }
    }

    public QualifiedDestinationName getReplyDestination() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getReplyDestination", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getReplyDestination", _replyDestination);
        }

        return _replyDestination;
    }

    public void setReplyDestination(QualifiedDestinationName value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setReplyDestination", value);
        }

        _replyDestination = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setReplyDestination");
        }
    }

    public QualifiedDestinationName[] getForwardRoutingPath() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getForwardRoutingPath", this);
        }

        QualifiedDestinationName[] returnValue = null;

        if (_forwardRoutingPath != null) {
            returnValue = new QualifiedDestinationName[_forwardRoutingPath.length];
            System.arraycopy(_forwardRoutingPath, 0, returnValue, 0, _forwardRoutingPath.length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getForwardRoutingPath", returnValue);
        }

        return returnValue;
    }

    public void setForwardRoutingPath(QualifiedDestinationName[] newValue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setForwardRoutingPath", newValue);
        }

        if (newValue == null) {
            _forwardRoutingPath = null;
        } else {
            _forwardRoutingPath = new QualifiedDestinationName[newValue.length];
            System.arraycopy(newValue, 0, _forwardRoutingPath, 0, newValue.length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setForwardRoutingPath");
        }
    }

    public boolean isTopicAccessCheckRequired() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isTopicAccessCheckRequired", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(_topicAccessCheckRequired);
            SibTr.exit(tc, "isTopicAccessCheckRequired", b);
        }

        return _topicAccessCheckRequired;
    }

    public void setTopicAccessCheckRequired(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "setTopicAccessCheckRequired", b.toString());
        }

        _topicAccessCheckRequired = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setTopicAccessCheckRequired");
        }
    }

    public long getAlterationTime() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getAlterationTime", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getAlterationTime", Long.valueOf(_alterationTime));
        }

        return _alterationTime;
    }

    public void setAlterationTime(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = Long.valueOf(value);
            SibTr.entry(tc, "setAlterationTime", l);
        }

        _alterationTime = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setAlterationTime");
        }
    }

    public long getBlockedRetryTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getBlockedRetryTimeout", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getBlockedRetryTimeout", Long.valueOf(_blockedRetryTimeout));
        }

        return _blockedRetryTimeout;
    }

    public void setBlockedRetryTimeout(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = Long.valueOf(value);
            SibTr.entry(tc, "setBlockedRetryTimeout", l);
        }

        _blockedRetryTimeout = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setBlockedRetryTimeout");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.DestinationDefinition#isOrderingRequired()
     */
    public boolean isOrderingRequired() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isOrderingRequired", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "isOrderingRequired", Boolean.valueOf(_isOrderingRequired));
        }

        return _isOrderingRequired;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.DestinationDefinition#maintainMsgOrder(boolean)
     */
    public void maintainMsgOrder(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = Boolean.valueOf(value);
            SibTr.entry(tc, "maintainMsgOrder", b.toString());
        }

        _isOrderingRequired = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "maintainMsgOrder");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.DestinationDefinition#isAuditAllowed()
     */
    public boolean isAuditAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isAuditAllowed", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "isAuditAllowed", Boolean.valueOf(_isAuditAllowed));
        }

        return _isAuditAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.DestinationDefinition#setAuditAllowed(boolean
     * value)
     */
    void setAuditAllowed(boolean auditAllowed) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setAuditAllowed", Boolean.valueOf(auditAllowed));
        }

        _isAuditAllowed = auditAllowed;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setAuditAllowed");
        }
    }

    /**
     * @param sibdd
     */
    public void reset(LWMConfig sibdd) {
        setFromConfig(sibdd);

    }

}
