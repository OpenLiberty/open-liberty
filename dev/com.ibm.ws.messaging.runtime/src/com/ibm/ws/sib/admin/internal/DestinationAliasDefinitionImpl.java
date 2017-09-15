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
package com.ibm.ws.sib.admin.internal;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.ExtendedBoolean;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.QualifiedDestinationName;
import com.ibm.ws.sib.admin.SIBDestinationReliabilityInheritType;
import com.ibm.ws.sib.admin.SIBDestinationReliabilityType;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * @author philip
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DestinationAliasDefinitionImpl extends BaseDestinationDefinitionImpl implements DestinationAliasDefinition {
  
    private static final TraceComponent tc = SibTr.register(DestinationAliasDefinitionImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    // The following data members are derived from ME configuration.

    private String _bus = null;
    private int _defaultPriority = DestinationAliasDefinition.DEFAULT_DEFAULTPRIORITY;
    private boolean _delegateAuthorizationCheckToTarget = true;
    private QualifiedDestinationName[] _forwardRoutingPath = null;
    private Reliability _maxReliability = Reliability.NONE;
    private ExtendedBoolean _producerQOSOverrideEnabled = ExtendedBoolean.NONE;
    private ExtendedBoolean _receiveAllowed = ExtendedBoolean.NONE;
    private Reliability _reliability = Reliability.NONE;
    private QualifiedDestinationName _replyDestination = null;
    private ExtendedBoolean _sendAllowed = ExtendedBoolean.NONE;
    private String _targetBus = null;
    private String _targetName = null;

    private SIBUuid8[] scopedQueuePointMEs = null;

//   private SIBUuid8[] scopedMediationPointMEs = null;

    /**
     * Constructor: Create a default instance. This constructor should be used
     * where it is required to create a DestinationDefinition which is not formed
     * from WCCM configuration objects.
     * 
     * @param type
     *            the type of destination to be created
     * @param name
     *            the name of the destination
     */
    DestinationAliasDefinitionImpl(DestinationType type, String name) {
        super(type, name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "DestinationAliasDefinitionImpl", new Object[] { type, name });
        }

        setConfigType(DestinationConfigType.ALIAS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "DestinationAliasDefinitionImpl", this);
        }
    }

    /**
     * Constructor: Create an instance from a WCCM configuration object. This
     * constructor is intended to be used by the SM component when creating
     * destinations either during startup or on request.
     * 
     * @param dest
     */
    DestinationAliasDefinitionImpl(LWMConfig dest) {
        super(dest);
        setUUID(new SIBUuid12());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "DestinationAliasDefinitionImpl", dest);
        }

        setConfigType(DestinationConfigType.ALIAS);

        setFromConfig(dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "DestinationAliasDefinitionImpl", this);
        }
    }

    /**
     * @param dest
     */
    @Override
    protected void setFromConfig(LWMConfig dest) {
        super.setFromConfig(dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setFromConfig", dest);
        }
        AliasDestination destConfig = (AliasDestination) dest;
       
        _targetBus = null; // This value would be null for liberty release as,there is no concept of multiple SIBus in liberty
        _targetName = destConfig.getTargetDestination();
        _delegateAuthorizationCheckToTarget = destConfig.getDelegateAuthCheckToTargetDestination();
        

        String b = destConfig.isOverrideOfQOSByProducerAllowed();//.getString(CT_SIBDestinationAlias.OVERRIDEOFQOSBYPRODUCERALLOWED_NAME, CT_SIBDestinationAlias.OVERRIDEOFQOSBYPRODUCERALLOWED_DEFAULT);
        if (b.equalsIgnoreCase("TRUE")) {
            _producerQOSOverrideEnabled = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {

            _producerQOSOverrideEnabled = ExtendedBoolean.FALSE;
        }else{
        	_producerQOSOverrideEnabled= ExtendedBoolean.NONE;
        }

        b = destConfig.isReceiveAllowed();

        if (b.equalsIgnoreCase("TRUE")) {
            _receiveAllowed = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {
            _receiveAllowed = ExtendedBoolean.FALSE;
        }else{
        	_receiveAllowed=ExtendedBoolean.NONE;
        }

        // Map the Reliability from WCCM to Core SPI.

        String _rs = destConfig.getDefaultReliability();// dest.getString(CT_SIBDestinationAlias.RELIABILITY_NAME, CT_SIBDestinationAlias.RELIABILITY_DEFAULT);

        if (_rs.equals(SIBDestinationReliabilityInheritType.BEST_EFFORT_NONPERSISTENT)) {
            _reliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.ASSURED_PERSISTENT)) {
            _reliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.EXPRESS_NONPERSISTENT)) {
            _reliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.RELIABLE_NONPERSISTENT)) {
            _reliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.RELIABLE_PERSISTENT)) {
            _reliability = Reliability.RELIABLE_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.INHERIT)) {
            _reliability = Reliability.NONE;
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
        } else if (_rs.equals(SIBDestinationReliabilityInheritType.INHERIT)) {
            _maxReliability = Reliability.NONE;
        }

        b = destConfig.isSendAllowed();

        if (b.equalsIgnoreCase("TRUE")) {
            _sendAllowed = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {
            _sendAllowed = ExtendedBoolean.FALSE;
        }else if(b.equalsIgnoreCase("INHERIT")){
        	_sendAllowed = ExtendedBoolean.NONE;
        }
    }

    public String getBus() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getBus", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getBus", _bus);
        }

        return _bus;
    }

    public void setBus(String busName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setBus", busName);
        }

        _bus = busName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setBus");
        }

        return;
    }

    public String getTargetName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getTargetName", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getTargetName", _targetName);
        }

        return _targetName;
    }

    public void setTargetName(String targetName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setTargetName", targetName);
        _targetName = targetName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setTargetName");
        return;
    }

    public String getTargetBus() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getTargetBus", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getTargetBus", _targetBus);
        }

        return _targetBus;
    }

    public void setTargetBus(String targetBus) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setTargetBus", targetBus);
        }

        _targetBus = targetBus;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setTargetBus");
        }

        return;
    }

    public int getDefaultPriority() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDefaultPriority", null);
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

    public Reliability getMaxReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMaxReliability", null);
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

    public ExtendedBoolean isOverrideOfQOSByProducerAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed", _producerQOSOverrideEnabled.toString());
        }

        return _producerQOSOverrideEnabled;
    }

    public void setOverrideOfQOSByProducerAllowed(ExtendedBoolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setOverrideOfQOSByProducerAllowed", value.toString());
        }

        _producerQOSOverrideEnabled = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setOverrideOfQOSByProducerAllowed");
        }
    }

    public ExtendedBoolean isReceiveAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isReceiveAllowed", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "isReceiveAllowed", _receiveAllowed.toString());
        }

        return _receiveAllowed;
    }

    public void setReceiveAllowed(ExtendedBoolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setReceiveAllowed", value.toString());
        }

        _receiveAllowed = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setReceiveAllowed");
        }
    }

    public Reliability getDefaultReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDefaultReliability", null);
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

    public ExtendedBoolean isSendAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isSendAllowed", null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "isSendAllowed", _sendAllowed.toString());
        }

        return _sendAllowed;
    }

    public void setSendAllowed(ExtendedBoolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setSendAllowed", value.toString());
        }

        _sendAllowed = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setSendAllowed");
        }
    }

    public QualifiedDestinationName getReplyDestination() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getReplyDestination", _replyDestination);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            if (_replyDestination != null) {
                SibTr.exit(tc, "getReplyDestination", _replyDestination.toString());
            } else {
                SibTr.exit(tc, "getReplyDestination", "null");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getReplyDestination", _replyDestination);
        }

        return _replyDestination;
    }

    public void setReplyDestination(QualifiedDestinationName value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setReplyDestination", value.toString());
        }

        _replyDestination = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setReplyDestination");
        }
    }

    public QualifiedDestinationName[] getForwardRoutingPath() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getForwardRoutingPath", _forwardRoutingPath);
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

    public boolean getDelegateAuthorizationCheckToTarget() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDelegateAuthorizationCheckToTarget", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getDelegateAuthorizationCheckToTarget");
        }

        return _delegateAuthorizationCheckToTarget;
    }

    public void setDelegateAuthorizationCheckToTarget(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setDelegateAuthorizationCheckToTarget", value);
        }

        _delegateAuthorizationCheckToTarget = value;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setDelegateAuthorizationCheckToTarget");
        }
    }

    /**
     * 
     * @return SIBUuid8[]
     */
    public SIBUuid8[] getScopedQueuePointMEs() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getScopedQueuePointMEs", this);
        }

        SIBUuid8[] returnValue = null;

        if (scopedQueuePointMEs != null) {
            returnValue = new SIBUuid8[scopedQueuePointMEs.length];
            System.arraycopy(scopedQueuePointMEs, 0, returnValue, 0, scopedQueuePointMEs.length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getScopedQueuePointMEs", returnValue);
        }

        return returnValue;
    }

    /**
     * 
     * @param newValue
     */
    public void setScopedQueuePointMEs(SIBUuid8[] newValue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setScopedQueuePointMEs", newValue);
        }

        if (newValue == null) {
            scopedQueuePointMEs = null;
        } else {
            scopedQueuePointMEs = new SIBUuid8[newValue.length];
            System.arraycopy(newValue, 0, scopedQueuePointMEs, 0, newValue.length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "setScopedQueuePointMEs");
        }
    }

}
