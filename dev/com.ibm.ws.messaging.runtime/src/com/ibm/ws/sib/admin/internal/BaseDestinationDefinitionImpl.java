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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

public class BaseDestinationDefinitionImpl implements BaseDestinationDefinition {


    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.impl.BaseDestinationDefinitionImpl";
    private static final TraceComponent tc =
                    SibTr.register(BaseDestinationDefinitionImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);


    // The actual type of the destination
    protected DestinationConfigType _type = null;
    protected DestinationType _destinationType = null;
    // The following data members are derived from WCCM configuration
    private String _description = null;
    protected Map _destinationContext = new HashMap();
    private String _name = null;
    private SIBUuid12 _uuid;

    /**
     * Constructor: Create a default instance. This constructor should be used where it is required
     * to create a DestinationDefinition which is not formed from WCCM configuration objects.
     * 
     * @param type the type of destination to be created
     * @param name the name of the destination
     */
    BaseDestinationDefinitionImpl(DestinationType type, String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".<init>", type + " " + name);

        _name = name;
        _destinationType=type;
        _uuid = new SIBUuid12();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".<init>");
    }

    /**
     * Constructor: Create an instance from a WCCM configuration object. This constructor is intended
     * to be used by the SM component when creating destinations either during startup or on request.
     * 
     * @param dest
     */
    BaseDestinationDefinitionImpl(LWMConfig dest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".<init>", this);
        
        setConfigType(DestinationConfigType.LOCAL);
        if(((BaseDestination)dest).isAlias()){
        	setConfigType(DestinationConfigType.ALIAS);
        	
        }
        else
        {
            this._destinationType=((SIBDestination)dest).getDestinationType();
        }

        //Venu Liberty change: setFromConfig is already called from DestinationDefintionImpl
        //setFromConfig(dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".<init>");
    }

    /**
     * @return
     */
    protected DestinationConfigType getConfigType() {
        return _type;
    }

    /**
     * @param d
     */
    protected void setConfigType(DestinationConfigType d) {
        _type = d;
    }

    /**
     * @param dest
     */
    protected void setFromConfig(LWMConfig dest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".setFromConfig", dest);
        
          _name = ((BaseDestination) dest).getName();
       // _uuid = new SIBUuid12(((SIBDestination) dest).getUUID());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + ".setFromConfig");
    }

    /**
     * Clones the Object
     * 
     * @return Object
     */
    @Override
    public final Object clone() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, CLASS_NAME + ".clone", this);

        BaseDestinationDefinitionImpl obj = null;
        try {
            obj = (BaseDestinationDefinitionImpl) super.clone();
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#isLocal()
     */
    public boolean isLocal() {
        return (getConfigType() == DestinationConfigType.LOCAL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#isAlias()
     */
    public boolean isAlias() {
        return (getConfigType() == DestinationConfigType.ALIAS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#isForeign()
     */
    public boolean isForeign() {
        return (getConfigType() == DestinationConfigType.FOREIGN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#getDescription()
     */
    public String getDescription() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDescription", this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDescription", _description);
        return _description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#setDescription(java.lang.String)
     */
    public void setDescription(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDescription", value);
        _description = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDescription");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#getName()
     */
    public String getName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getName", this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getName", _name);
        return _name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#getUUID()
     */
    public SIBUuid12 getUUID() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getUUID", this._uuid);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getUUID");
        }
        return _uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#setUUID(com.ibm.ws.sib.utils.SIBUuid12)
     */
    public void setUUID(SIBUuid12 value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setUUID", value);
        }
        _uuid = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setUUID");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#getDestinationContext()
     */
    public Map getDestinationContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationContext", this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationContext", _destinationContext.toString());
        return _destinationContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.BaseDestinationDefinition#setDestinationContext(java.util.Map)
     */
    public void setDestinationContext(Map value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestinationContext", value.toString());
        _destinationContext = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestinationContext");
    }
}
