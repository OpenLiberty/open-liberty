

package com.ibm.ws.sib.admin.internal;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
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
public class LocalizationDefinitionImpl implements LocalizationDefinition {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.impl.LocalizationDefinitionImpl";
    private static final TraceComponent tc =
                    SibTr.register(LocalizationDefinitionImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);


    private final String _configId = null;
    private long _destinationHighMsgs = Long.MAX_VALUE;
    private long _destinationLowMsgs = Long.MAX_VALUE;
    private String _identifier = null;
    private boolean _sendAllowed = true;
    private SIBUuid12 _targetUuid = null;
    private SIBUuid12 _uuid = null;

    // The following data members are dynamic and are not directly part of WCCM configuration
    private long _alterationTime;

    LocalizationDefinitionImpl(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".<init>", name);

        _identifier = name;
        _uuid = new SIBUuid12();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".<init>");
    }

    LocalizationDefinitionImpl(SIBUuid12 targetUuid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".<init>", targetUuid);

        _identifier = targetUuid.toString();
        _targetUuid = targetUuid;
        _uuid = new SIBUuid12();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".<init>");
    }

    LocalizationDefinitionImpl(LWMConfig lp) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".<init>", lp);
        SIBLocalizationPoint lpConfig = ((SIBLocalizationPoint) lp);
        _identifier = lpConfig.getIdentifier();
        _destinationHighMsgs = lpConfig.getHighMsgThreshold();
        _destinationLowMsgs = (_destinationHighMsgs * 8) / 10;
        _sendAllowed = lpConfig.isSendAllowed();
        _uuid = new SIBUuid12();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".<init>");
    }

    public String getUuid() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getUuid", this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getUuid", _uuid);
        }
        return _uuid.toString();
    }

    public String getName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getName", this._identifier);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getName", _identifier);
        }
        return _identifier;
    }

//    public String getConfigId() {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//            SibTr.entry(tc, "getConfigId");
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.exit(tc, "getConfigId", _configId);
//        }
//        return _configId;
//    }

    public long getAlterationTime() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAlterationTime", this._alterationTime);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(_alterationTime);
            SibTr.exit(tc, "getAlterationTime", l.toString());
        }
        return _alterationTime;
    }

    public void setAlterationTime(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(value);
            SibTr.entry(tc, "setAlterationTime", l);
        }
        _alterationTime = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setAlterationTime");
    }

    public long getDestinationHighMsgs() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationHighMsgs", this._destinationHighMsgs);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(_destinationHighMsgs);
            SibTr.exit(tc, "getDestinationHighMsgs", l.toString());
        }
        return _destinationHighMsgs;
    }

    public void setDestinationHighMsgs(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(value);
            SibTr.entry(tc, "setDestinationHighMsgs", l);
        }
        _destinationHighMsgs = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestinationHighMsgs");
    }

    public long getDestinationLowMsgs() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationLowMsgs", this._destinationLowMsgs);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(_destinationLowMsgs);
            SibTr.exit(tc, "getDestinationLowMsgs", l.toString());
        }
        return _destinationLowMsgs;
    }

    public void setDestinationLowMsgs(long value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Long l = new Long(value);
            SibTr.entry(tc, "setDestinationLowMsgs", l);
        }
        _destinationLowMsgs = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestinationLowMsgs");
    }

    public boolean isSendAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSendAllowed", this._sendAllowed);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = new Boolean(_sendAllowed);
            SibTr.exit(tc, "isSendAllowed", b);
        }
        return _sendAllowed;
    }

    public void setSendAllowed(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Boolean b = new Boolean(value);
            SibTr.entry(tc, "setSendAllowed", b.toString());
        }
        _sendAllowed = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setSendAllowed");
    }

    /**
     * Clones the Object
     * 
     * @return Object
     */
    @Override
    public Object clone() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".clone", this);

        LocalizationDefinitionImpl obj = null;
        try {
            obj = (LocalizationDefinitionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            FFDCFilter.processException(e, CLASS_NAME, "PROBE_ID_10", this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "clone", e);

            // Cannot happen
            InternalError ie = new InternalError(e.toString());

            throw ie;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "clone");

        return obj;
    }

    /**
     * @see java.lang.Object#toString()
     * @returns String toString result
     */
    @Override
    public String toString() {
        // use a StringBuffer to save creating many String objects
        StringBuffer result = new StringBuffer("");

        // construct the result by appending each variable
        result.append("uuid=");
        result.append(_uuid);
        result.append(":targetUuid=");
        result.append(_targetUuid);
        result.append(":configId=");
        result.append(_configId);
        result.append(":sendAllowed=");
        result.append(_sendAllowed);
        result.append(":identifier=");
        result.append(_identifier);
        result.append(":destinationHighMsgs=");
        result.append(_destinationHighMsgs);
        result.append(":destinationLowMsgs=");
        result.append(_destinationLowMsgs);

        // return the String held in the String Buffer
        return result.toString();
    }

	@Override
	public void setUUID(SIBUuid8 uuid) {
		// TODO Auto-generated method stub
		
	}

}
