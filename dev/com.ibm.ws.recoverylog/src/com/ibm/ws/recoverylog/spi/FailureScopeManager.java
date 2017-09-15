/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import java.util.HashMap;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public class FailureScopeManager
{
    private static final TraceComponent tc = Tr.register(FailureScopeManager.class, TraceConstants.TRACE_GROUP, null);       
    
    private static HashMap _failureScopeFactoriesById = new HashMap();
    private static HashMap _failureScopeFactoriesByClass = new HashMap();
    
    public static void registerFailureScopeFactory(Byte id, Class failureScopeClass, FailureScopeFactory factory)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "registerFailureScopeFactory", new Object[] {id, failureScopeClass, factory});
        
        _failureScopeFactoriesById.put(id, factory);
        _failureScopeFactoriesByClass.put(failureScopeClass, factory);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "registerFailureScopeFactory");
    }
    
    public static byte[] toByteArray(FailureScope failureScope)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "toByteArray", failureScope);
        
        byte[] bytes = null;
        
        final Class failureScopeClass = failureScope.getClass();
        
        if (tc.isDebugEnabled()) Tr.debug(tc, "FailureScope class: " + failureScopeClass);
               
        final FailureScopeFactory failureScopeFactory = (FailureScopeFactory)_failureScopeFactoriesByClass.get(failureScopeClass);
        
        if (failureScopeFactory != null)
        {
            bytes = failureScopeFactory.toByteArray(failureScope);
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "toByteArray");
        return bytes;
    }
    
    public static FailureScope toFailureScope(byte[] bytes)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "toFailureScope", bytes);
        
        // The first byte in the given array is the failure scope factory's
        // identifier. Extract it from the array and use it to lookup the
        // appropriate factory.
        final Byte factoryId = new Byte(bytes[0]);
        final FailureScopeFactory failureScopeFactory = (FailureScopeFactory)_failureScopeFactoriesById.get(factoryId);
        
        FailureScope failureScope = null;
        
        if (failureScopeFactory != null)
        {
            failureScope = failureScopeFactory.toFailureScope(bytes);
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "toFailureScope", failureScope);
        return failureScope;
    }
}
