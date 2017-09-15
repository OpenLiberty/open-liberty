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

/**
 * This class is designed to allow MDB to cross attach to remote MEs if the local MEs are not currently running
 * which differs from SibRaColocatingEndpointActivation which will wait for the local ME to start and will not attempt
 * to connect remotely all the while there is a local ME defined.
 */

package com.ibm.ws.sib.ra.inbound.impl;

import com.ibm.websphere.ras.TraceComponent;
//Sanjay Liberty Changes
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;

public class SibRaScalableEndpointActivation extends SibRaCommonEndpointActivation 
{

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaScalableEndpointActivation.class);
    
    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaScalableEndpointActivation.class.getName();
    
    public SibRaScalableEndpointActivation (SibRaResourceAdapterImpl resourceAdapter,
                                            MessageEndpointFactory messageEndpointFactory,
                                            SibRaEndpointConfiguration endpointConfiguration,
                                            SibRaEndpointInvoker endpointInvoker) throws ResourceException 
    {
        super(resourceAdapter, messageEndpointFactory, endpointConfiguration,
                endpointInvoker);
        final String methodName = "SibRaScalableEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * It is best to connect directly to the DSH when in scalable mode.
     */
    public boolean onlyConnectToDSH ()
    {
    	return true;
    }

    /**
     * This method gets a list of all local running MEs
     * @return Array of all the local running MEs
     */
    JsMessagingEngine [] getMEsToCheck() 
    {
        final String methodName = "getMEsToCheck";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.entry(this, TRACE, methodName);
        }
        
        JsMessagingEngine[] retVal = SibRaEngineComponent.getActiveMessagingEngines(_endpointConfiguration.getBusName ());
        
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.exit(this, TRACE, methodName, retVal);
        }
        return retVal;
    }

}
