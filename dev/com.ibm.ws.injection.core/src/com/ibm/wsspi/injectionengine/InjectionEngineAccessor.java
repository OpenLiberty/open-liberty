/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.injectionengine.InternalInjectionEngine;

/**
 * Accessor for InjectionEngine. <p>
 *
 * It follows a singleton pattern and provides a static getInstance methods for easy access.
 */
public class InjectionEngineAccessor
{
    private static final TraceComponent tc = Tr.register(InjectionEngineAccessor.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private static InternalInjectionEngine svInstance = null;
    private static MessageDestinationLinkFactory svMDLFactory = null; //d493167

    /**
     * Creates an InjectionEngine
     */
    private InjectionEngineAccessor()
    {
        //Private constructor to follow the singleton pattern
    }

    /**
     * Returns the single instance of the InjectionEngine for the
     * current process.
     */
    public final static InjectionEngine getInstance()
    {
        if (svInstance == null)
        {
            Tr.error(tc, "INJECTION_ENGINE_SERVICE_UNAVAILABLE_CWNEN0005E");
        }
        return svInstance;
    }

    //d493167
    /**
     * Returns the single instance of the MessageDestinationLinkFactory
     * for the current process.
     */
    public synchronized final static MessageDestinationLinkFactory getMessageDestinationLinkInstance()
    {
        if (svMDLFactory == null) {
            svMDLFactory = new MessageDestinationLinkFactory();
        }
        return svMDLFactory;
    }

    /**
     * Internal mechanism to support providing a server type specific
     * implementation of the InjectionEngine. <p>
     */
    // F46994.2
    static void setInjectionEngine(InternalInjectionEngine ie)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInjectionEngine : " + ie);
        svInstance = ie;
    }

    /**
     * Returns the single instance of the InjecctionEngine for internal use. <p>
     */
    // F46994.2
    static InternalInjectionEngine getInternalInstance()
    {
        return svInstance;
    }
}
