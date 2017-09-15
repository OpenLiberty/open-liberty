package com.ibm.tx.ltc.embeddable.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;

public final class LTCCallbacks
{
    //
    // Static initialisation for
    // singleton instance.
    //
    static private LTCCallbacks _instance = new LTCCallbacks();

    /**
     * Collection of interested components.
     */
    private ArrayList<UOWCallback> _callbacks = new ArrayList<UOWCallback>();

    /**
     * Reference to the <code>UOWCurrent</code> implementation.
     */
    private final static UOWCurrent _uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();

    private static final TraceComponent tc = Tr.register(LTCCallbacks.class, TranConstants.TRACE_GROUP, TranConstants.LTC_NLS_FILE);

    private LTCCallbacks(){}

    static public LTCCallbacks instance()
    {
        return _instance;
    }

    /**
     * Register a <code>UOWCallback</code> for LTC notifications.
     * 
     * @param callback The UOWCallback object to register with the LocalTransaction service
     */
    public void registerCallback(UOWCallback callback)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "registerCallback", callback);

        if (!_callbacks.contains(callback))
        {
            _callbacks.add(callback);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "Number of registered Callbacks: "+_callbacks.size());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "registerCallback");
    }

    /**
     * Notify registered callbacks of context change.
     */
    public void contextChange(int typeOfChange) throws IllegalStateException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            String type = "UNKNOWN";
            switch (typeOfChange)
            {
            case UOWCallback.PRE_BEGIN:
                type = "PRE_BEGIN";
                break;
            case UOWCallback.POST_BEGIN:
                type = "POST_BEGIN";
                break;
            case UOWCallback.PRE_END:
                type = "PRE_END";
                break;
            case UOWCallback.POST_END:
                type = "POST_END";
                break;
            }

            Tr.entry(tc, "contextChange", type);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "UOWCurrent", _uowCurrent);

        //
        // Need to get the current UOWCoordinator if 
        // we are in POST_BEGIN or PRE_END
        //
        UOWCoordinator coord = null;

        if ((typeOfChange == UOWCallback.POST_BEGIN) || (typeOfChange == UOWCallback.PRE_END))
        {
            coord = _uowCurrent.getUOWCoord();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "Coordinator="+coord);

        IllegalStateException ex = null;

        //
        // Inform the registered callbacks
        //
        for (int i = 0; i < _callbacks.size(); i++)
        {
            UOWCallback callback = _callbacks.get(i);

            try
            {
                callback.contextChange(typeOfChange, coord);
            }
            catch (IllegalStateException ise)
            {   
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "Exception caught during UOW callback at context change", ise);
                ex = ise;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "contextChange");

        //
        // If one of the callbacks threw an exception 
        // then rethrow here. In practice the last exception 
        // to be thrown will be re-thrown but as long as 
        // we throw it doesn't matter.
        //
        if (ex != null)
        {
            throw ex;
        }
    }
}
