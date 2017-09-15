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

package com.ibm.ws.uow.embeddable;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;

public class UOWCallbackManager
{
    private static final TraceComponent tc = Tr.register(UOWCallbackManager.class, TranConstants.TRACE_GROUP, null);
    
    // A list of callbacks that are interested in
    // UOW context changes 
    private ArrayList<UOWCallback> _callbacks;
    
    public void addCallback(UOWCallback callback)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "addCallback", callback);

        if (_callbacks == null)
        {
            _callbacks = new ArrayList<UOWCallback>();
        }
        
        _callbacks.add(callback);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "addCallback");
    }
       
    public void notifyCallbacks(int contextChangeType, UOWCoordinator coord)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "notifyCallbacks", new Object[]{contextChangeType, coord});       
        
        if (_callbacks != null)
        {
            final Iterator callbacks = _callbacks.iterator();
            
            while (callbacks.hasNext())
            {
                ((UOWCallback)callbacks.next()).contextChange(contextChangeType, coord);
            }       
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "notifyCallbacks");
    }
}
